package com.example.cassie.data.media

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.net.URL

data class LyricsResult(
    val plainLyrics: String,
    val syncedLyrics: String?,
    val source: String = "online", // "online" | "lrc_file" | "cache"
)

/**
 * Fetches lyrics from LRCLIB (free, no API key needed).
 * Falls back to companion .lrc files and cached results for offline.
 * https://lrclib.net
 */
object LyricsRepository {

    /**
     * Fetch lyrics with offline fallback:
     * 1. Companion .lrc file next to the audio file
     * 2. SharedPreferences cache (from previous online fetch)
     * 3. LRCLIB online (and cache result for next time)
     */
    suspend fun fetchLyrics(
        artist: String,
        title: String,
        songFilePath: String? = null,
        prefs: PersistenceManager? = null,
    ): LyricsResult? = withContext(Dispatchers.IO) {
        // ── 1. Companion .lrc file ──
        if (songFilePath != null) {
            val lrcFile = File(songFilePath).resolveSibling(
                File(songFilePath).nameWithoutExtension + ".lrc"
            )
            if (lrcFile.exists()) {
                val content = lrcFile.readText()
                val plain = if (content.contains("[")) extractPlainFromLrc(content) else content
                return@withContext LyricsResult(
                    plainLyrics = plain,
                    syncedLyrics = if (content.contains("[")) content else null,
                    source = "lrc_file",
                )
            }
        }

        // ── 2. SharedPreferences cache (offline) ──
        val cacheKey = "lyrics_cache_${artist}_${title}"
            .replace(" ", "_")
            .replace(Regex("[^a-zA-Z0-9_]"), "")
        if (prefs != null) {
            val cached = prefs.getString(cacheKey)
            if (cached != null) {
                val parts = cached.split("|||", limit = 2)
                if (parts.size == 2) {
                    return@withContext LyricsResult(
                        plainLyrics = parts[0],
                        syncedLyrics = parts[1].ifBlank { null },
                        source = "cache",
                    )
                }
            }
        }

        // ── 3. LRCLIB online ──
        try {
            val url = URL(
                "https://lrclib.net/api/get?artist_name=${artist.urlEncode()}&track_name=${title.urlEncode()}"
            )
            val json = url.readText()
            val plainLyrics = extractJsonString(json, "plainLyrics") ?: return@withContext null
            val syncedLyrics = extractJsonString(json, "syncedLyrics")

            // Cache for offline
            if (prefs != null) {
                prefs.putString(cacheKey, "$plainLyrics|||${syncedLyrics ?: ""}")
            }

            LyricsResult(plainLyrics, syncedLyrics, source = "online")
        } catch (_: Exception) {
            null
        }
    }

    /** Extract plain text from an LRC file (strip timestamps). */
    private fun extractPlainFromLrc(lrc: String): String {
        return lrc.lines()
            .map { it.replaceFirst(Regex("""\[\d+:\d+(?:\.\d+)?\]"""), "").trim() }
            .filter { it.isNotBlank() }
            .joinToString("\n")
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
