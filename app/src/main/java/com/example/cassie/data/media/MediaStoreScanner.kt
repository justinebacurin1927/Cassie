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
    val mimeType: String,
    val albumArtUri: String?,
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
            MediaStore.Audio.Media.MIME_TYPE,
        )
        val where = mimeTypes.joinToString(" OR ") { "${MediaStore.Audio.Media.MIME_TYPE} = ?" }
        val sel = "$where AND ${MediaStore.Audio.Media.DURATION} > 30000"

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
            val mime = c.getColumnIndexOrThrow(MediaStore.Audio.Media.MIME_TYPE)

            while (c.moveToNext()) {
                val aid = c.getLong(albumId)
                songs.add(Song(
                    id = c.getLong(id),
                    title = c.getString(title) ?: "Unknown",
                    artist = c.getString(artist) ?: "Unknown Artist",
                    album = c.getString(album) ?: "Unknown Album",
                    albumId = aid,
                    duration = c.getLong(dur),
                    mimeType = c.getString(mime) ?: "audio/mpeg",
                    albumArtUri = Uri.parse("content://media/external/audio/albumart/$aid").toString(),
                ))
            }
        }
        songs
    }
}
