package com.example.cassie.data.media

import android.content.Context
import android.net.Uri
import android.provider.MediaStore
import androidx.compose.runtime.Stable
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
@Stable
data class Song(
    val id: Long,
    val title: String,
    val artist: String,
    val album: String,
    val albumId: Long,
    val duration: Long,
    val dateAdded: Long = 0,
    val mimeType: String,
    val albumArtUri: String?,
    val genre: String = "",
    val filePath: String? = null, // for companion .lrc lookup
)

class MediaStoreScanner(private val context: Context) {

    private val mimeTypes = arrayOf(
        "audio/mpeg", "audio/flac", "audio/ogg", "audio/opus",
        "audio/x-wav", "audio/wav", "audio/aac", "audio/mp4a-latm",
        "audio/x-m4a", "audio/mp4",
    )

    suspend fun scan(): List<Song> = withContext(Dispatchers.IO) {
        val songs = mutableListOf<Song>()
        val projection = arrayOf(
            MediaStore.Audio.Media._ID,
            MediaStore.Audio.Media.TITLE,
            MediaStore.Audio.Media.ARTIST,
            MediaStore.Audio.Media.ALBUM,
            MediaStore.Audio.Media.ALBUM_ID,
            MediaStore.Audio.Media.DURATION,
            MediaStore.Audio.Media.DATE_ADDED,
            MediaStore.Audio.Media.MIME_TYPE,
            MediaStore.Audio.AudioColumns.GENRE,
            MediaStore.Audio.Media.DATA,
        )
        val where = mimeTypes.joinToString(" OR ") { "${MediaStore.Audio.Media.MIME_TYPE} = ?" }
        val sel = "$where AND ${MediaStore.Audio.Media.DURATION} > 30000"

        // Some OEM devices (Vivo/Xiaomi on Android 10-12) throw
        // SecurityException even after the runtime permission is
        // granted. Wrap the whole query so the coroutine doesn't
        // crash and leave the caller in an infinite-loading state.
        try {
            context.contentResolver.query(
                MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                projection, sel, mimeTypes,
                "${MediaStore.Audio.Media.TITLE} ASC"
            )?.use { c ->
                val id = c.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
                val title = c.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE)
                val artist = c.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST)
                val album = c.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM)
                val albumId = c.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM_ID)
                val dur = c.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION)
                val dateCol = c.getColumnIndexOrThrow(MediaStore.Audio.Media.DATE_ADDED)
                val mime = c.getColumnIndexOrThrow(MediaStore.Audio.Media.MIME_TYPE)
                val genreIdx = c.getColumnIndex(MediaStore.Audio.AudioColumns.GENRE)
                val dataIdx = c.getColumnIndex(MediaStore.Audio.Media.DATA)

                while (c.moveToNext()) {
                    try {
                        val aid = c.getLong(albumId)
                        songs.add(Song(
                            id = c.getLong(id),
                            title = c.getString(title) ?: "Unknown",
                            artist = c.getString(artist) ?: "Unknown Artist",
                            album = c.getString(album) ?: "Unknown Album",
                            albumId = aid,
                            duration = c.getLong(dur),
                            dateAdded = c.getLong(dateCol),
                            mimeType = c.getString(mime) ?: "audio/mpeg",
                            // Build album art URI from albumId.
                            // Songs with albumId > 0 get the standard MediaStore
                            // content URI which Coil can load. Untagged songs
                            // (albumId <= 0) get null → fallback icon.
                            albumArtUri = if (aid > 0L)
                                "content://media/external/audio/albumart/$aid" else null,
                            genre = if (genreIdx >= 0) c.getString(genreIdx) ?: "" else "",
                            filePath = if (dataIdx >= 0) c.getString(dataIdx) else null,
                        ))
                    } catch (_: Exception) {
                        // skip this row, keep scanning
                    }
                }
            }
        } catch (_: SecurityException) {
            // permission got revoked between grant and query — return
            // what we have so the caller can show an empty/permission UI
        } catch (_: Exception) {
            // other query failure — return empty so the UI can recover
        }
        songs
    }
}
