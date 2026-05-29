package com.example.cassie.data.media

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.URL

data class LyricsResult(
    val plainLyrics: String,
    val syncedLyrics: String?,
)

/**
 * Fetches lyrics from LRCLIB (free, no API key needed).
 * https://lrclib.net
 */
object LyricsRepository {

    suspend fun fetchLyrics(artist: String, title: String): LyricsResult? =
        withContext(Dispatchers.IO) {
            try {
                val url = URL(
                    "https://lrclib.net/api/get?artist_name=${artist.urlEncode()}&track_name=${title.urlEncode()}"
                )
                val json = url.readText()
                // parse JSON manually to avoid extra dependencies
                val plainLyrics = extractJsonString(json, "plainLyrics") ?: return@withContext null
                val syncedLyrics = extractJsonString(json, "syncedLyrics")
                LyricsResult(plainLyrics, syncedLyrics)
            } catch (_: Exception) {
                null
            }
        }

    private fun extractJsonString(json: String, key: String): String? {
        val search = "\"$key\":\""
        val start = json.indexOf(search)
        if (start < 0) return null
        val valueStart = start + search.length
        val valueEnd = json.indexOf('"', valueStart)
        if (valueEnd < 0) return null
        return json.substring(valueStart, valueEnd)
            .replace("\\n", "\n")
            .replace("\\\"", "\"")
            .replace("\\\\", "\\")
    }

    private fun String.urlEncode(): String =
        java.net.URLEncoder.encode(this, "UTF-8")
}
