package com.example.cassie.data.media

import android.content.Context
import android.net.Uri
import android.provider.MediaStore
import androidx.compose.runtime.Stable
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

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
                            // Placeholder — resolved below after Albums query
                            albumArtUri = null,
                            genre = if (genreIdx >= 0) c.getString(genreIdx) ?: "" else "",
                            filePath = if (dataIdx >= 0) c.getString(dataIdx) else null,
                        ))
                    } catch (_: Exception) {
                        // skip this row, keep scanning
                    }
                }

                // ── Resolve album art via Albums table ────────────────
                // Some OEM devices return a generic image from the standard
                // content://media/external/audio/albumart/{id} URI even
                // when the album has real art. By reading the ALBUM_ART
                // file path from the Albums table and copying the art into
                // the app's private cache directory, we bypass this issue
                // entirely and get the actual image.
                val seenIds = songs.map { it.albumId }.filter { it > 0L }.distinct()
                if (seenIds.isNotEmpty()) {
                    resolveAlbumArt(seenIds).forEach { (aid, cachedUri) ->
                        if (cachedUri != null) {
                            for (i in songs.indices) {
                                if (songs[i].albumId == aid) {
                                    songs[i] = songs[i].copy(albumArtUri = cachedUri)
                                }
                            }
                        }
                    }
                }

                // Final fallback: any song that still has null albumArtUri
                // but a valid albumId gets the standard content URI.
                // This covers albums the Albums query didn't return (e.g.
                // very new albums not yet indexed) and devices where the
                // content URI actually works correctly.
                for (i in songs.indices) {
                    if (songs[i].albumArtUri == null && songs[i].albumId > 0L) {
                        songs[i] = songs[i].copy(
                            albumArtUri = "content://media/external/audio/albumart/${songs[i].albumId}"
                        )
                    }
                }
            }
        } catch (_: SecurityException) {
            // permission got revoked — return what we have
        } catch (_: Exception) {
            // other query failure — return empty so UI can recover
        }
        songs
    }

    /**
     * Queries the Albums table for ALBUM_ART file paths, copies each file
     * into the app's private cache directory, and returns a map of
     * albumId → cached file:// URI. Albums without real art get null.
     *
     * Using cached files is the ONLY reliable way to get correct album art
     * on devices where MediaStore's content:// album art URIs return a
     * generic placeholder (common on Xiaomi, Vivo, Samsung, and other OEMs).
     * Once cached, Coil can load file:// URIs from the app's private
     * directory without any scoped-storage issues.
     */
    private fun resolveAlbumArt(albumIds: List<Long>): Map<Long, String?> {
        val result = mutableMapOf<Long, String?>()
        albumIds.forEach { result[it] = null }

        if (albumIds.isEmpty()) return result

        val placeholders = albumIds.joinToString(",") { "?" }
        val selection = "${MediaStore.Audio.Albums._ID} IN ($placeholders)"
        val args = albumIds.map { it.toString() }.toTypedArray()

        try {
            context.contentResolver.query(
                MediaStore.Audio.Albums.EXTERNAL_CONTENT_URI,
                arrayOf(MediaStore.Audio.Albums._ID, MediaStore.Audio.Albums.ALBUM_ART),
                selection, args, null
            )?.use { cursor ->
                val idCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Albums._ID)
                val artCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Albums.ALBUM_ART)
                val cacheDir = File(context.cacheDir, "album_art").also { it.mkdirs() }

                while (cursor.moveToNext()) {
                    val aid = cursor.getLong(idCol)
                    val artPath = cursor.getString(artCol)
                    if (artPath.isNullOrEmpty()) continue

                    // Copy to app-private cache using the albumId as name.
                    // Coil can freely read app-private files.
                    val cacheFile = File(cacheDir, "$aid.jpg")
                    // Use the standard content URI to open the image through
                    // MediaStore's content provider — this works even when
                    // direct File I/O would be blocked by scoped storage.
                    val contentUri = Uri.parse("content://media/external/audio/albumart/$aid")
                    if (!cacheFile.exists()) {
                        try {
                            context.contentResolver.openInputStream(contentUri)?.use { inp ->
                                cacheFile.outputStream().use { out ->
                                    inp.copyTo(out, bufferSize = 8192)
                                }
                            }
                        } catch (_: Exception) {
                            continue // skip this one
                        }
                    }
                    result[aid] = Uri.fromFile(cacheFile).toString()
                }
            }
        } catch (_: Exception) {
            // Query failed — fallback below handles these
        }
        return result
    }
}
