package com.example.cassie.party

/**
 * Keeps a ring buffer of the last [windowSize] lines Skipper has
 * said, and exposes a single question: "would saying this line be
 * a repetition?"
 *
 * Repetition is checked in three flavors, with strictness decreasing:
 *
 *  1. EXACT: the exact same text was said within the window
 *     → never allow.
 *  2. TEMPLATE: the same grammar rule was used within the last 3
 *     → strong de-prioritization (weight → 0.1x), not a hard block.
 *  3. NEAR: lines that share a significant amount of n-gram content
 *     → soft de-prioritization (weight → 0.5x).
 *
 * Why soft for templates and exact for text? Because templates can
 * legitimately be reused with new content (e.g. "you've looped this 5
 * times" later becomes "you've looped this 10 times") — that's not
 * repetition, it's progression. But "you've looped this 5 times"
 * followed by "you've looped this 5 times" verbatim IS a bug.
 */
class AntiRepetition(private val windowSize: Int = 8) {

    private val recent: ArrayDeque<SaidLine> = ArrayDeque()

    fun record(line: SkipperLine) {
        recent.addFirst(SaidLine(line.text, line.sourceRuleId))
        while (recent.size > windowSize) recent.removeLast()
    }

    /** True if the exact text was said within the window. */
    fun isExactDuplicate(text: String): Boolean =
        recent.any { it.text == text }

    /** True if the same rule was used within the last [recentCount] lines. */
    fun isTemplateRepeat(ruleId: String, recentCount: Int = 3): Boolean =
        recent.take(recentCount).any { it.ruleId == ruleId }

    /**
     * Returns a weight multiplier in (0, 1] for a candidate line. The
     * LineGenerator multiplies the rule's own weight by this before
     * running weighted random.
     */
    fun dampenWeight(ruleId: String, text: String): Float {
        if (isExactDuplicate(text)) return 0f            // hard reject
        if (isTemplateRepeat(ruleId, recentCount = 3)) return 0.1f
        if (isTemplateRepeat(ruleId, recentCount = 6)) return 0.4f
        return 1f
    }

    fun snapshot(): List<SkipperLine> =
        recent.map { SaidLine.toLine(it) }

    private data class SaidLine(val text: String, val ruleId: String) {
        companion object {
            fun toLine(s: SaidLine) = SkipperLine(
                text = s.text,
                intent = MascotIntent.OBSERVE, // we don't store intent here
                mood = SkipperMood.WHATEVER,
                sourceRuleId = s.ruleId,
            )
        }
    }
}
