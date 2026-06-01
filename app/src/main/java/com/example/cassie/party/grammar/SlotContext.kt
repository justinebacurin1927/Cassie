package com.example.cassie.party.grammar

/**
 * Live data passed to [SlotFiller.Dynamic] and [SlotFiller.Count]
 * when rendering a [com.example.cassie.party.GrammarRule] template.
 *
 * CRITICAL: this struct contains ZERO song metadata (no title,
 * artist, album, genre, file path, URI). Skipper never judges the
 * music — it only knows what the USER has done. If you find yourself
 * wanting to add a "songTitle" field here, you're about to break
 * the design rule. Add a behavior-derived field instead.
 */
data class SlotContext(
    // ── Current song (behavior only) ──
    /** How many times the current song has been repeat-one'd. */
    val currentLoopCount: Int = 0,
    /** How many minutes the user has spent on the current song. */
    val currentMinutesListened: Float = 0f,

    // ── Lifetime / aggregate (behavior only) ──
    val totalSongsStarted: Int = 0,
    val totalMinutesListened: Float = 0f,
    val totalSongsSkipped: Int = 0,
    val totalSongsCompleted: Int = 0,
    val totalFavorites: Int = 0,
    val totalLyricsOpens: Int = 0,
    /** Songs the user has spent at least 1 minute on. */
    val uniqueSongsListened: Int = 0,

    // ── Session / time ──
    /** How many minutes since this app session started. */
    val sessionMinutes: Int = 0,
    /** 0..23, local time. */
    val hourOfDay: Int = 12,
    val isPartyMode: Boolean = false,
    /**
     * True if the user is currently playing music (not paused, not
     * stopped, app likely foregrounded). Rules that reference
     * "listening" should use this. Rules about being "quiet" or
     * "idle" should require it to be FALSE.
     */
    val isActivelyPlaying: Boolean = false,

    // ── Detection (from the recognizer) ──
    /** Recent-window skip rate, 0.0..1.0. */
    val recentSkipRate: Float = 0f,
    /** Currently-active patterns, highest-confidence first. */
    val activePatterns: List<com.example.cassie.party.UserPattern> = emptyList(),
    /** Most-recent 20 skip outcomes (true=skipped, false=completed),
     *  oldest first. Useful for count-flavoured templates. */
    val recentSkips: List<Boolean> = emptyList(),

    // ── Derived (computed once per generation, not stored) ──
    /** What fraction of total listening the user's top song represents, 0..1. */
    val topMinutesShare: Float = 0f,
)
