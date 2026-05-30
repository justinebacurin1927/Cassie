package com.example.cassie.data.media

import android.util.Log
import com.example.cassie.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

/**
 * Fetches mood/genre tags for a song using the Last.fm API.
 * Maps returned tags to a [PenguinMood] (defined in MascotMoodCard).
 *
 * Results are cached in-memory so we don't re-fetch on every play.
 *
 * API key is loaded from BuildConfig.LASTFM_API_KEY (set via local.properties).
 * See local.properties for the key — that file is gitignored and never committed.
 */
class OnlineMoodDetector {

    companion object {
        private const val BASE_URL = "https://ws.audioscrobbler.com/2.0/"
        private const val TAG = "OnlineMoodDetector"
    }

    // Cache: songKey -> moodIndex (0=GREETING,1=EXCITED,2=CHILL,3=SLEEPY,4=LOVE,5=SAD)
    private val cache = mutableMapOf<String, Int>()

    /** Map Last.fm tags to our mood enum */
    private fun moodFromTags(tags: List<String>): Int {
        val lowerTags = tags.map { it.lowercase().trim() }

        var sad = 0
        var excited = 0
        var chill = 0
        var sleepy = 0
        var love = 0

        for (tag in lowerTags) {
            when {
                tag in listOf("sad", "melancholy", "depressing", "emotional", "melancholic",
                    "heartbreak", "heartbroken", "breakup", "cry", "crying", "lonely",
                    "blues", "gloomy", "dark", "piano", "rainy", "grief") -> sad++

                tag in listOf("happy", "party", "upbeat", "dance", "energetic", "fun",
                    "pop", "electronic", "edm", "festival", "nightclub", "workout",
                    "exciting", "joyful", "celebration", "power", "anthem") -> excited++

                tag in listOf("chill", "chillout", "ambient", "relaxing", "lounge",
                    "downtempo", "lo-fi", "lofi", "study", "background", "mellow",
                    "smooth jazz", "easy listening", "trip-hop", "trip hop") -> chill++

                tag in listOf("sleep", "sleepy", "dream", "dreamy", "peaceful",
                    "calm", "meditation", "ethereal", "ambient", "atmospheric",
                    "nighttime", "lullaby", "serene", "tranquil", "quiet") -> sleepy++

                tag in listOf("love", "romantic", "romance", "sexy", "sensual",
                    "ballad", "slow dance", "wedding", "couple", "passion",
                    "intimate", "sweet", "heart", "valentine") -> love++
            }
        }

        val maxScore = maxOf(sad, excited, chill, sleepy, love)
        if (maxScore <= 0) return 0 // GREETING

        return when (maxScore) {
            love -> 4
            sad -> 5
            excited -> 1
            sleepy -> 3
            chill -> 2
            else -> 0
        }
    }

    /**
     * Fetch mood from Last.fm for the given song.
     * Returns mood index: 0=GREETING, 1=EXCITED, 2=CHILL, 3=SLEEPY, 4=LOVE, 5=SAD
     */
    suspend fun detectMood(song: Song): Int = withContext(Dispatchers.IO) {
        val songKey = "${song.artist.lowercase()}_${song.title.lowercase()}"

        // Check cache first
        cache[songKey]?.let {
            Log.d(TAG, "CACHE HIT: $songKey -> mood=$it")
            return@withContext it
        }

        val apiKey = BuildConfig.LASTFM_API_KEY
        Log.d(TAG, "API key loaded: '${apiKey.take(4)}...' (blank=${apiKey.isBlank()})")
        if (apiKey.isBlank()) {
            Log.w(TAG, "No API key in BuildConfig — set lastfm.api.key in local.properties")
            return@withContext 0 // No API key configured in local.properties
        }

        try {
            val encodedArtist = URLEncoder.encode(song.artist, "UTF-8")
            val encodedTrack = URLEncoder.encode(song.title, "UTF-8")
            val url = "${BASE_URL}?method=track.getInfo&api_key=$apiKey&artist=$encodedArtist&track=$encodedTrack&format=json"

            Log.d(TAG, "Fetching: artist='${song.artist}' track='${song.title}'")

            val connection = URL(url).openConnection() as HttpURLConnection
            connection.connectTimeout = 10000
            connection.readTimeout = 10000

            val response = connection.inputStream.bufferedReader().readText()
            connection.disconnect()

            Log.d(TAG, "Response (first 200): ${response.take(200)}")

            val json = JSONObject(response)

            // Check for error response
            if (json.has("error")) {
                val errMsg = json.optString("message", "unknown")
                Log.w(TAG, "API error for '${song.title}': $errMsg")
                return@withContext 0
            }

            val track = json.optJSONObject("track") ?: run {
                Log.w(TAG, "No 'track' object in response for '${song.title}'")
                return@withContext 0
            }
            val toptags = track.optJSONObject("toptags") ?: run {
                Log.w(TAG, "No 'toptags' object for '${song.title}'")
                return@withContext 0
            }
            val tagArray = toptags.optJSONArray("tag") ?: run {
                Log.d(TAG, "No tags found for '${song.title}' — GREETING fallback")
                return@withContext 0
            }

            val tags = mutableListOf<String>()
            for (i in 0 until tagArray.length()) {
                val tagObj = tagArray.getJSONObject(i)
                val name = tagObj.optString("name", "")
                if (name.isNotBlank()) tags.add(name)
            }

            Log.d(TAG, "Tags for '${song.title}': $tags")

            val moodIdx = moodFromTags(tags)
            Log.d(TAG, "Mood determined: $moodIdx for '${song.title}'")
            cache[songKey] = moodIdx
            moodIdx
        } catch (e: Exception) {
            Log.e(TAG, "Exception for '${song.title}': ${e.message}", e)
            0
        }
    }

    fun clearCache() = cache.clear()
}
