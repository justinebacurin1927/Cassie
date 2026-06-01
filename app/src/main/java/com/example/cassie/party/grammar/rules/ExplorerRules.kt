package com.example.cassie.party.grammar.rules

import com.example.cassie.party.MascotIntent
import com.example.cassie.party.SkipperMood
import com.example.cassie.party.UserPatternType
import com.example.cassie.party.grammar.ContextFilter
import com.example.cassie.party.grammar.GrammarRule
import com.example.cassie.party.grammar.SlotFiller

/**
 * EXPLORER pattern rules — wide variety (8+ unique songs) with
 * a low skip rate (<50% in the recent window).
 */
internal fun explorerRules(): List<GrammarRule> = listOf(
    GrammarRule("praise_explore_1", MascotIntent.PRAISE, "we love a music explorer",
        contextFilter = ContextFilter(requiresPattern = UserPatternType.EXPLORER, minConfidence = 0.5f), weight = 1.2f),
    GrammarRule("praise_explore_2", MascotIntent.PRAISE, "the variety is the whole point honestly",
        contextFilter = ContextFilter(requiresPattern = UserPatternType.EXPLORER, minConfidence = 0.5f)),
    GrammarRule("praise_explore_3", MascotIntent.PRAISE, "this is elite music citizen behavior",
        contextFilter = ContextFilter(requiresPattern = UserPatternType.EXPLORER, minConfidence = 0.5f),
        weight = 1.1f, moodAffinity = listOf(SkipperMood.MUSED)),
    GrammarRule("praise_explore_4", MascotIntent.PRAISE, "the algorithm is learning from you",
        contextFilter = ContextFilter(requiresPattern = UserPatternType.EXPLORER, minConfidence = 0.6f)),

    GrammarRule("observe_explore_1", MascotIntent.OBSERVE, "no song is safe from your queue",
        contextFilter = ContextFilter(requiresPattern = UserPatternType.EXPLORER, minConfidence = 0.5f)),
    GrammarRule("observe_explore_2", MascotIntent.OBSERVE, "every song gets its moment in your rotation",
        contextFilter = ContextFilter(requiresPattern = UserPatternType.EXPLORER, minConfidence = 0.6f),
        moodAffinity = listOf(SkipperMood.MUSED)),
    GrammarRule("observe_explore_3", MascotIntent.OBSERVE, "{N} unique songs so far today, the range is real",
        contextFilter = ContextFilter(requiresPattern = UserPatternType.EXPLORER, minConfidence = 0.5f),
        slots = mapOf("N" to SlotFiller.Count("uniqueSongsListened", "song", "songs"))),

    GrammarRule("confess_explore_1", MascotIntent.CONFESS, "your taste is all over the place and i respect it",
        contextFilter = ContextFilter(requiresPattern = UserPatternType.EXPLORER, minConfidence = 0.5f),
        moodAffinity = listOf(SkipperMood.MUSED)),
)
