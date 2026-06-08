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

                // Collect unique album IDs during scan so we can resolve
                // actual album art from the Albums table in one pass.
                val seenAlbumIds = mutableSetOf<Long>()

                while (c.moveToNext()) {
                    try {
                        val aid = c.getLong(albumId)
                        seenAlbumIds.add(aid)
                        songs.add(Song(
                            id = c.getLong(id),
                            title = c.getString(title) ?: "Unknown",
                            artist = c.getString(artist) ?: "Unknown Artist",
                            album = c.getString(album) ?: "Unknown Album",
                            albumId = aid,
                            duration = c.getLong(dur),
                            dateAdded = c.getLong(dateCol),
                            mimeType = c.getString(mime) ?: "audio/mpeg",
                            // Set placeholder — will be resolved below
                            albumArtUri = null,
                            genre = if (genreIdx >= 0) c.getString(genreIdx) ?: "" else "",
                            filePath = if (dataIdx >= 0) c.getString(dataIdx) else null,
                        ))
                    } catch (_: Exception) {
                        // skip this row, keep scanning
                    }
                }

                // ── Resolve actual album art from Albums table ──
                // Querying the Albums table returns the REAL file path
                // for album art (ALBUM_ART column). If it's null/empty
                // the album has NO art — even if albumId > 0.
                // This prevents all untagged songs from sharing the
                // same generic MediaStore image.
                if (seenAlbumIds.isNotEmpty()) {
                    val artMap = resolveAlbumArt(seenAlbumIds)
                    val hasAnyArt = artMap.values.any { it != null }
                    if (hasAnyArt) {
                        // Albums table returned real art — use it.
                        songs.replaceAll { it.copy(albumArtUri = artMap[it.albumId]) }
                    } else {
                        // Fallback: if the Albums query found NO art
                        // (table missing, permission denied, or this
                        // device just doesn't populate ALBUM_ART),
                        // construct URIs the old way using albumId.
                        songs.replaceAll { song ->
                            song.copy(albumArtUri = if (song.albumId > 0L)
                                "content://media/external/audio/albumart/${song.albumId}" else null)
                        }
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

    /**
     * Queries the [MediaStore.Audio.Albums] table for all given [albumIds]
     * and returns a map: albumId → album art content URI (or null if no
     * art exists). Uses a single IN query so it scales to any library size.
     *
     * When the album truly has art, we return the standard content URI
     * (`content://media/external/audio/albumart/{id}`) which Coil can
     * load on all API levels. Albums without real art get null so the
     * UI shows the fallback icon instead of a generic MediaStore image.
     */
    private fun resolveAlbumArt(albumIds: Set<Long>): Map<Long, String?> {
        val validIds = albumIds.filter { it > 0L }
        if (validIds.isEmpty()) return emptyMap()

        val result = mutableMapOf<Long, String?>()
        validIds.forEach { result[it] = null }

        val placeholders = validIds.joinToString(",") { "?" }
        val selection = "${MediaStore.Audio.Albums._ID} IN ($placeholders)"
        val args = validIds.map { it.toString() }.toTypedArray()

        try {
            context.contentResolver.query(
                MediaStore.Audio.Albums.EXTERNAL_CONTENT_URI,
                arrayOf(MediaStore.Audio.Albums._ID, MediaStore.Audio.Albums.ALBUM_ART),
                selection, args, null
            )?.use { cursor ->
                val idCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Albums._ID)
                val artCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Albums.ALBUM_ART)
                while (cursor.moveToNext()) {
                    val aid = cursor.getLong(idCol)
                    val artPath = cursor.getString(artCol)
                    if (!artPath.isNullOrEmpty()) {
                        // Real album art exists — use the standard
                        // content URI format that Coil can load.
                        result[aid] = "content://media/external/audio/albumart/$aid"
                    }
                }
            }
        } catch (_: Exception) {
            // Query failed — will trigger the fallback in scan()
        }
        return result
    }
}
