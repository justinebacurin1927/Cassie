package com.example.cassie.party

import java.util.Calendar
import java.util.TimeZone

/**
 * Aggregated numeric view of the user's listening behavior.
 *
 * Everything here is derived from [UserEvent]s. The [PatternRecognizer]
 * reads this snapshot to decide which [UserPattern]s apply. Nothing in
 * this file should ever inspect song titles, artists, genres, or album
 * art — those are forbidden signals (we don't want to "judge the music").
 */
data class BehaviorStats(
    // ── Lifetime counters ─────────────────────────────────────────
    var totalSongsStarted: Int = 0,
    var totalSongsSkipped: Int = 0,
    var totalSkipsBefore30s: Int = 0,
    var totalSongsCompleted: Int = 0,
    var totalSongsReplayed: Int = 0,
    var totalSongLoops: Int = 0,
    var totalPartyModeToggles: Int = 0,
    var totalShuffleToggles: Int = 0,
    var totalSleepTimerSets: Int = 0,
    var totalFavoriteToggles: Int = 0,
    var totalLyricsOpens: Int = 0,
    var totalAppForegrounds: Int = 0,
    var totalAppBackgrounds: Int = 0,
    /**
     * Lifetime minutes listened (float, 1/60 per 1s tick). Replaces
     * the old int-per-minute model which dropped everything under 1
     * minute.
     */
    var totalMinutesListened: Float = 0f,

    // ── Per-song aggregates ───────────────────────────────────────
    /** songId -> lifetime minutes listened (float) */
    var minutesPerSong: Map<Long, Float> = emptyMap(),
    /** songId -> lifetime loop count (repeat-one wraps) */
    var loopsPerSong: Map<Long, Int> = emptyMap(),

    // ── Recent rolling window (last 20 song-starts) ───────────────
    /** Most-recent 20 (wasSkipped=true) booleans, oldest first. */
    var recentSkips: List<Boolean> = emptyList(),

    // ── Session state ─────────────────────────────────────────────
    var currentSongId: Long? = null,
    var currentSongStartedAt: Long? = null,
    var currentSongLoopCount: Int = 0,
    var currentSessionStartMs: Long = System.currentTimeMillis(),
    var lastUpdatedMs: Long = System.currentTimeMillis(),

    // ── Time-of-day profile (hour 0..23 -> count of app-foreground events) ──
    var foregroundsByHour: Map<Int, Int> = emptyMap(),
) {

    // ── Derived signals (computed each time, no caching needed) ──

    /** Fraction of the recent 20 songs that were skipped, 0.0..1.0. */
    val recentSkipRate: Float
        get() = if (recentSkips.isEmpty()) 0f
        else recentSkips.count { it }.toFloat() / recentSkips.size

    /** Fraction of total skips that happened within the first 30 seconds. */
    val prematureSkipRate: Float
        get() = if (totalSongsSkipped == 0) 0f
        else totalSkipsBefore30s.toFloat() / totalSongsSkipped

    /** Highest play count for any single song (the user's "favorite replay"). */
    val maxMinutesOnOneSong: Float
        get() = minutesPerSong.values.maxOrNull() ?: 0f

    /** songId of the most-replayed song, or null if no data. */
    val topReplaySongId: Long?
        get() = minutesPerSong.maxByOrNull { it.value }?.key

    /** Number of songs the user has actually spent at least 0.1 minute on. */
    val uniqueSongsListenedTo: Int
        get() = minutesPerSong.count { it.value >= 0.1f }

    /** Fraction of all app-foregrounds that happened between 22:00 and 04:00. */
    val nightOwlRate: Float
        get() {
            if (foregroundsByHour.isEmpty()) return 0f
            val total = foregroundsByHour.values.sum()
            if (total == 0) return 0f
            val night = (22..23).sumOf { foregroundsByHour[it] ?: 0 } +
                (0..4).sumOf { foregroundsByHour[it] ?: 0 }
            return night.toFloat() / total
        }
}

/**
 * The detected "vibe" the user is in, inferred only from [BehaviorStats].
 *
 * Patterns are not mutually exclusive — a single user can be a LOOPER
 * on one song and a PARTIER in general, and Skipper will reference both.
 */
enum class UserPatternType {
    /** User skips 70%+ of songs within 30 seconds. */
    SKIPPER,

    /** User has looped the current song 3+ times. */
    LOOPER,

    /** User replays one specific song far more than anything else. */
    REPEATER,

    /** User listens to one song for 10+ minutes straight. */
    MARATHONER,

    /** User toggled party mode on (high-energy shuffle + repeat-all). */
    PARTIER,

    /** User plays 20+ different songs per day with a low skip rate. */
    EXPLORER,

    /** App foregrounds happen mostly between 22:00 and 04:00. */
    NIGHT_OWL,

    /** User has favorited 50+ songs. */
    FAVORITE_HOARDER,

    /** User opens the lyrics view 10+ times across the lifetime. */
    LYRICS_LOVER,
}

/**
 * A single detected pattern, with a 0..1 confidence score and a
 * human-readable [evidence] string Skipper can later speak out loud.
 */
data class UserPattern(
    val type: UserPatternType,
    val confidence: Float,        // 0.0..1.0
    val evidence: String,         // "skipped 18/20 songs within 30s"
    val detectedAtMs: Long,
)

/** Calendar helper for time-of-day bucketing. */
internal object Clock {
    fun hourOfDay(epochMs: Long, tz: TimeZone = TimeZone.getDefault()): Int {
        val cal = Calendar.getInstance(tz)
        cal.timeInMillis = epochMs
        return cal.get(Calendar.HOUR_OF_DAY)
    }
}
