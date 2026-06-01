package com.example.cassie.party.grammar

import com.example.cassie.party.MascotIntent
import com.example.cassie.party.SkipperMood
import com.example.cassie.party.UserPatternType

/**
 * A [GrammarRule] is a single template that Skipper can say, plus
 * the conditions under which it's allowed to be picked.
 *
 * The LineGenerator flow is:
 *  1. ContextFilter.isAllowed  → narrows the ruleset
 *  2. weight + dynamic weighting → probability distribution
 *  3. weighted random pick → one rule
 *  4. fill slots using SlotContext → final string
 *  5. anti-repetition check → reject if recently said
 *
 * Slot syntax inside [template]:
 *  - {slotName}     looks up `slots[slotName]`
 *  - {slotName|opt1|opt2|opt3}  one-of filler with inline options
 */
data class GrammarRule(
    /** Stable identifier for anti-repetition and debugging. */
    val id: String,
    /** What is this line trying to do? */
    val intent: MascotIntent,
    /** The sentence template, with `{name}` placeholders. */
    val template: String,
    /** Slot definitions. Keys are the names used in [template]. */
    val slots: Map<String, SlotFiller> = emptyMap(),
    /** Conditions for this rule to be a candidate. If null, always. */
    val contextFilter: ContextFilter? = null,
    /** Multiplier on the base selection probability. Default 1.0. */
    val weight: Float = 1f,
    /**
     * Optional list of mood states that boost this rule's weight.
     * If empty, the rule is mood-agnostic.
     */
    val moodAffinity: List<SkipperMood> = emptyList(),
) {
    /**
     * If a pattern requirement is set, this rule only applies when
     * the user currently has that pattern active with confidence
     * above the threshold.
     */
    val requiresPattern: UserPatternType? get() = contextFilter?.requiresPattern
    val minPatternConfidence: Float get() = contextFilter?.minConfidence ?: 0f
}

/**
 * A set of "this rule is only valid if..." checks. All non-null
 * fields must pass for the rule to be a candidate.
 */
data class ContextFilter(
    /** Only fire if the user currently has this pattern active. */
    val requiresPattern: UserPatternType? = null,
    /** Minimum confidence (0..1) of [requiresPattern]. */
    val minConfidence: Float = 0f,
    /** Only fire if the current Skipper mood is in this set. */
    val requireMoodIn: Set<SkipperMood> = emptySet(),
    /** Only fire outside these moods (anti-mood). */
    val excludeMoodIn: Set<SkipperMood> = emptySet(),
    /** Hour-of-day window, inclusive. */
    val hourRange: IntRange? = null,
    /** Only fire if user is currently skipping a lot. */
    val requiresHighSkipRate: Boolean = false,
    /** Only fire if in party mode. */
    val requiresPartyMode: Boolean = false,
    /** Only fire if the current song is being looped. */
    val requiresLooping: Boolean = false,
    /** Only fire if currentMinutesListened >= this. */
    val minMinutesOnCurrentSong: Int = 0,
)
