package com.example.cassie.party

import com.example.cassie.data.media.PersistenceManager
import org.json.JSONArray
import org.json.JSONObject

/**
 * Persists the user's [BehaviorStats] across app restarts.
 *
 * Storage layout (one JSON object in SharedPreferences under key
 * "skipper_behavior_stats"):
 *
 * {
 *   "totalSongsStarted":   42,
 *   "totalSongsSkipped":   11,
 *   ...
 *   "minutesPerSong":      { "123": 7, "456": 3, ... },
 *   "loopsPerSong":        { "123": 4 },
 *   "recentSkips":         [true, false, false, true, ...],   // last 20
 *   "currentSongId":       123,
 *   "currentSongLoopCount": 2,
 *   "foregroundsByHour":   { "8": 5, "22": 12, "23": 9, ... },
 *   "lastUpdatedMs":       1717200000000
 * }
 *
 * The store is the only thing that survives process death; everything
 * else (event stream, recognizer) is in-memory and rebuilt on init.
 */
class UserPatternStore(private val pm: PersistenceManager) {

    private val key = "skipper_behavior_stats"

    /** Load the saved stats, or return a fresh empty [BehaviorStats]. */
    fun load(): BehaviorStats {
        val raw = pm.getString(key) ?: return BehaviorStats()
        return try {
            val obj = JSONObject(raw)
            BehaviorStats(
                totalSongsStarted = obj.optInt("totalSongsStarted"),
                totalSongsSkipped = obj.optInt("totalSongsSkipped"),
                totalSkipsBefore30s = obj.optInt("totalSkipsBefore30s"),
                totalSongsCompleted = obj.optInt("totalSongsCompleted"),
                totalSongsReplayed = obj.optInt("totalSongsReplayed"),
                totalSongLoops = obj.optInt("totalSongLoops"),
                totalPartyModeToggles = obj.optInt("totalPartyModeToggles"),
                totalShuffleToggles = obj.optInt("totalShuffleToggles"),
                totalSleepTimerSets = obj.optInt("totalSleepTimerSets"),
                totalFavoriteToggles = obj.optInt("totalFavoriteToggles"),
                totalLyricsOpens = obj.optInt("totalLyricsOpens"),
                totalAppForegrounds = obj.optInt("totalAppForegrounds"),
                totalAppBackgrounds = obj.optInt("totalAppBackgrounds"),
                totalMinutesListened = obj.optDouble("totalMinutesListened", 0.0).toFloat(),
                minutesPerSong = obj.optJSONObject("minutesPerSong")?.let(::jsonToLongFloatMap) ?: emptyMap(),
                loopsPerSong = obj.optJSONObject("loopsPerSong")?.let(::jsonToLongIntMap) ?: emptyMap(),
                recentSkips = obj.optJSONArray("recentSkips")?.let(::jsonToBooleanList) ?: emptyList(),
                currentSongId = obj.optLong("currentSongId", -1L).takeIf { it >= 0 },
                currentSongStartedAt = obj.optLong("currentSongStartedAt", -1L).takeIf { it >= 0 },
                currentSongLoopCount = obj.optInt("currentSongLoopCount"),
                currentSessionStartMs = obj.optLong("currentSessionStartMs", System.currentTimeMillis()),
                lastUpdatedMs = obj.optLong("lastUpdatedMs", System.currentTimeMillis()),
                foregroundsByHour = obj.optJSONObject("foregroundsByHour")?.let(::jsonToIntIntMap) ?: emptyMap(),
            )
        } catch (_: Exception) {
            BehaviorStats()
        }
    }

    /** Persist the current stats. Safe to call from any thread. */
    fun save(stats: BehaviorStats) {
        try {
            val obj = JSONObject()
            obj.put("totalSongsStarted", stats.totalSongsStarted)
            obj.put("totalSongsSkipped", stats.totalSongsSkipped)
            obj.put("totalSkipsBefore30s", stats.totalSkipsBefore30s)
            obj.put("totalSongsCompleted", stats.totalSongsCompleted)
            obj.put("totalSongsReplayed", stats.totalSongsReplayed)
            obj.put("totalSongLoops", stats.totalSongLoops)
            obj.put("totalPartyModeToggles", stats.totalPartyModeToggles)
            obj.put("totalShuffleToggles", stats.totalShuffleToggles)
            obj.put("totalSleepTimerSets", stats.totalSleepTimerSets)
            obj.put("totalFavoriteToggles", stats.totalFavoriteToggles)
            obj.put("totalLyricsOpens", stats.totalLyricsOpens)
            obj.put("totalAppForegrounds", stats.totalAppForegrounds)
            obj.put("totalAppBackgrounds", stats.totalAppBackgrounds)
            obj.put("totalMinutesListened", stats.totalMinutesListened.toDouble())
            obj.put("minutesPerSong", floatMapToJson(stats.minutesPerSong))
            obj.put("loopsPerSong", JSONObject(stats.loopsPerSong))
            obj.put("recentSkips", JSONArray(stats.recentSkips))
            stats.currentSongId?.let { obj.put("currentSongId", it) }
            stats.currentSongStartedAt?.let { obj.put("currentSongStartedAt", it) }
            obj.put("currentSongLoopCount", stats.currentSongLoopCount)
            obj.put("currentSessionStartMs", stats.currentSessionStartMs)
            obj.put("lastUpdatedMs", stats.lastUpdatedMs)
            obj.put("foregroundsByHour", JSONObject(stats.foregroundsByHour))
            pm.putString(key, obj.toString())
        } catch (_: Exception) {
            // Persistence is best-effort. If SharedPreferences is
            // unavailable we lose this update but the in-memory state
            // is still valid for the rest of the session.
        }
    }

    // ── JSON helpers ───────────────────────────────────────────────

    private fun jsonToLongIntMap(obj: JSONObject): Map<Long, Int> {
        val out = mutableMapOf<Long, Int>()
        obj.keys().forEach { k ->
            val v = obj.optInt(k, 0)
            k.toLongOrNull()?.let { out[it] = v }
        }
        return out
    }

    private fun jsonToLongFloatMap(obj: JSONObject): Map<Long, Float> {
        val out = mutableMapOf<Long, Float>()
        obj.keys().forEach { k ->
            val v = obj.optDouble(k, 0.0).toFloat()
            k.toLongOrNull()?.let { out[it] = v }
        }
        return out
    }

    private fun floatMapToJson(map: Map<Long, Float>): JSONObject {
        val obj = JSONObject()
        map.forEach { (k, v) -> obj.put(k.toString(), v.toDouble()) }
        return obj
    }

    private fun jsonToIntIntMap(obj: JSONObject): Map<Int, Int> {
        val out = mutableMapOf<Int, Int>()
        obj.keys().forEach { k ->
            val v = obj.optInt(k, 0)
            k.toIntOrNull()?.let { out[it] = v }
        }
        return out
    }

    private fun jsonToBooleanList(arr: JSONArray): List<Boolean> {
        return (0 until arr.length()).map { arr.getBoolean(it) }
    }
}
