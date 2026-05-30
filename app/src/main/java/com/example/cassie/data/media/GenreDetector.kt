package com.example.cassie.data.media

import android.content.Context
import android.media.MediaMetadataRetriever
import android.net.Uri
import androidx.compose.runtime.Stable

/**
 * Reads embedded genre and mood metadata from audio files using
 * [MediaMetadataRetriever]. This catches ID3 tags that are often
 * richer than the MediaStore genre column.
 *
 * Results are cached in [PersistenceManager] so we only hit the
 * file system once per song.
 */
@Stable
class GenreDetector(private val context: Context) {

    private val cache = mutableMapOf<Long, String>()

    /**
     * Returns a genre/mood string for the given song.
     * Priority: cached → embedded ID3 tag → MediaStore genre.
     */
    fun getGenre(song: Song): String {
        // 1. Check in-memory cache
        cache[song.id]?.let { return it }

        // 2. Try embedded ID3 tags via MediaMetadataRetriever
        val uri = Uri.withAppendedPath(
            android.provider.MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
            song.id.toString()
        )
        try {
            val mmr = MediaMetadataRetriever()
            mmr.setDataSource(context, uri)
            val embedded = mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_GENRE)
            mmr.release()
            if (!embedded.isNullOrBlank()) {
                cache[song.id] = embedded
                return embedded
            }
        } catch (_: Exception) {
            // file may be inaccessible — fall through
        }

        // 3. Fall back to MediaStore genre (already on Song)
        if (song.genre.isNotBlank()) {
            cache[song.id] = song.genre
            return song.genre
        }

        // 4. Nothing found — mark as empty so we don't retry
        cache[song.id] = ""
        return ""
    }

    /** Clear the cache (e.g. if library rescan happens). */
    fun clearCache() = cache.clear()
}
