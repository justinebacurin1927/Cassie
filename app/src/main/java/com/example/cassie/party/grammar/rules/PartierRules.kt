package com.example.cassie.party.grammar.rules

import com.example.cassie.party.MascotIntent
import com.example.cassie.party.SkipperMood
import com.example.cassie.party.UserPatternType
import com.example.cassie.party.grammar.ContextFilter
import com.example.cassie.party.grammar.GrammarRule

/**
 * PARTIER pattern rules — user toggled party mode on.
 *
 * Voice: hype praise + light chaos observation. Lower-stakes than
 * the roast lanes; party mode is a positive signal so the line
 * should land positive too.
 */
internal fun partierRules(): List<GrammarRule> = listOf(
    GrammarRule("praise_party_1", MascotIntent.PRAISE, "party mode activated, we are so locked in",
        contextFilter = ContextFilter(requiresPattern = UserPatternType.PARTIER, minConfidence = 0.5f),
        weight = 1.3f, moodAffinity = listOf(SkipperMood.HYPED, SkipperMood.CHAOTIC)),
    GrammarRule("praise_party_2", MascotIntent.PRAISE, "the energy is unmatched rn",
        contextFilter = ContextFilter(requiresPattern = UserPatternType.PARTIER, minConfidence = 0.5f),
        weight = 1.2f, moodAffinity = listOf(SkipperMood.HYPED)),
    GrammarRule("praise_party_3", MascotIntent.PRAISE, "this is main character hours",
        contextFilter = ContextFilter(requiresPattern = UserPatternType.PARTIER, minConfidence = 0.6f),
        weight = 1.1f, moodAffinity = listOf(SkipperMood.HYPED)),
    GrammarRule("praise_party_4", MascotIntent.PRAISE, "your shuffle game is iconic",
        contextFilter = ContextFilter(requiresPattern = UserPatternType.PARTIER, minConfidence = 0.5f),
        moodAffinity = listOf(SkipperMood.HYPED)),

    GrammarRule("observe_party_1", MascotIntent.OBSERVE, "party mode on, queue is on shuffle",
        contextFilter = ContextFilter(requiresPattern = UserPatternType.PARTIER, minConfidence = 0.5f)),
    GrammarRule("observe_party_2", MascotIntent.OBSERVE, "every song is a surprise now, this is chaos",
        contextFilter = ContextFilter(requiresPattern = UserPatternType.PARTIER, minConfidence = 0.6f),
        moodAffinity = listOf(SkipperMood.CHAOTIC)),

    GrammarRule("confess_party_1", MascotIntent.CONFESS, "your music taste under party mode is unhinged and i respect it",
        contextFilter = ContextFilter(requiresPattern = UserPatternType.PARTIER, minConfidence = 0.6f),
        weight = 1.1f, moodAffinity = listOf(SkipperMood.CHAOTIC)),
    GrammarRule("confess_party_2", MascotIntent.CONFESS, "you are the human aux cord rn",
        contextFilter = ContextFilter(requiresPattern = UserPatternType.PARTIER, minConfidence = 0.7f),
        weight = 1.1f, moodAffinity = listOf(SkipperMood.HYPED)),
)
