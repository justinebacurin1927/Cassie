package com.example.cassie.party

/**
 * The fifth algorithm layer: Skipper's own internal emotional state.
 *
 * This is independent of the user's [UserPattern]s. Patterns describe
 * the user; this describes the mascot. The two interact: a strong
 * user pattern can pull Skipper's mood toward a related state
 * (e.g. a SKIPPER pattern nudges Skipper toward WITTY or NOSY), but
 * over time the mood decays back toward [SkipperMood.WHATEVER] if
 * no new stimulation comes in.
 *
 * The LineGenerator reads the current mood and boosts rules whose
 * [GrammarRule.moodAffinity] matches. This is what keeps Skipper
 * from sounding the same in every session — same pattern, different
 * time of day, different mood, different line.
 */
class EmotionalArc(private val decayMs: Long = 5 * 60_000L) {

    @Volatile private var current: SkipperMood = SkipperMood.WHATEVER
    @Volatile private var lastShiftAt: Long = System.currentTimeMillis()

    val mood: SkipperMood get() = current

    /**
     * Apply one tick of time passage. If enough time has gone by
     * since the last shift, the mood decays toward WHATEVER.
     */
    fun tick(now: Long = System.currentTimeMillis()) {
        if (now - lastShiftAt > decayMs && current != SkipperMood.WHATEVER) {
            // Step down one rung toward WHATEVER.
            current = decayOneStep(current)
            lastShiftAt = now
        }
    }

    /**
     * Force a mood shift in response to a detected user pattern
     * (or a song event). The new mood sticks for at least
     * [decayMs] before decay resumes.
     */
    fun shift(target: SkipperMood, now: Long = System.currentTimeMillis()) {
        if (target == current) return
        current = target
        lastShiftAt = now
    }

    /**
     * Compute the mood shift to apply given the current user's
     * top pattern (if any) and the time of day. Called by the
     * LineGenerator before each line is generated.
     */
    fun suggestFromContext(
        topPattern: UserPatternType?,
        hourOfDay: Int,
    ): SkipperMood {
        // Pattern-based shifts win over time-based ones.
        val patternMood = when (topPattern) {
            UserPatternType.SKIPPER -> SkipperMood.WITTY
            UserPatternType.LOOPER -> SkipperMood.DRAMATIC
            UserPatternType.REPEATER -> SkipperMood.NOSY
            UserPatternType.MARATHONER -> SkipperMood.MUSED
            UserPatternType.PARTIER -> SkipperMood.HYPED
            UserPatternType.EXPLORER -> SkipperMood.MUSED
            UserPatternType.NIGHT_OWL -> SkipperMood.EEPY
            UserPatternType.FAVORITE_HOARDER -> SkipperMood.WITTY
            UserPatternType.LYRICS_LOVER -> SkipperMood.MUSED
            null -> null
        }
        if (patternMood != null) return patternMood

        // Time-based fallback (only if no pattern is firing).
        return when (hourOfDay) {
            in 0..5 -> SkipperMood.EEPY
            in 6..10 -> SkipperMood.CHILL
            in 11..14 -> SkipperMood.WHATEVER
            in 15..19 -> SkipperMood.CHILL
            in 20..23 -> SkipperMood.MUSED
            else -> SkipperMood.WHATEVER
        }
    }

    /**
     * Walking the mood state machine one step toward the center.
     * The "depth" of each mood is implicit in the order below.
     */
    private fun decayOneStep(m: SkipperMood): SkipperMood = when (m) {
        SkipperMood.CHAOTIC -> SkipperMood.DRAMATIC
        SkipperMood.DRAMATIC -> SkipperMood.NOSY
        SkipperMood.NOSY -> SkipperMood.WHATEVER
        SkipperMood.WITTY -> SkipperMood.WHATEVER
        SkipperMood.HYPED -> SkipperMood.CHILL
        SkipperMood.CHILL -> SkipperMood.WHATEVER
        SkipperMood.MUSED -> SkipperMood.WHATEVER
        SkipperMood.EEPY -> SkipperMood.WHATEVER
        SkipperMood.WHATEVER -> SkipperMood.WHATEVER
    }
}
