package com.example.cassie.party

/**
 * One fully-rendered Skipper line, ready to display.
 *
 * Carries more than the text so the UI can render different visual
 * treatments for different intents (e.g. a WITTY line might use a
 * different accent color than a CHILL line) and so we can later
 * support speech, animations, or notifications from the same source.
 */
data class SkipperLine(
    val text: String,
    val intent: MascotIntent,
    val mood: SkipperMood,
    /** ID of the [GrammarRule] that produced this line, for debugging
     *  and for the anti-repetition layer. */
    val sourceRuleId: String,
    /** Tags that describe *what triggered* this line — e.g. a pattern
     *  type, or a song event. Useful for the UI and for analytics. */
    val triggerTags: List<String> = emptyList(),
    val timestampMs: Long = System.currentTimeMillis(),
)
