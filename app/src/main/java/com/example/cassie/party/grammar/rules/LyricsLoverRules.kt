package com.example.cassie.party.grammar.rules

import com.example.cassie.party.MascotIntent
import com.example.cassie.party.SkipperMood
import com.example.cassie.party.UserPatternType
import com.example.cassie.party.grammar.ContextFilter
import com.example.cassie.party.grammar.GrammarRule
import com.example.cassie.party.grammar.SlotFiller

/**
 * LYRICS_LOVER pattern rules — user has opened lyrics 2+ times.
 *
 * Voice: warm observe + confess. Lyrics reading is a "rare care"
 * signal so the lines land positive.
 */
internal fun lyricsLoverRules(): List<GrammarRule> = listOf(
    GrammarRule("observe_lyr_1", MascotIntent.OBSERVE, "lyrics open count: {N}, we love a reader",
        contextFilter = ContextFilter(requiresPattern = UserPatternType.LYRICS_LOVER, minConfidence = 0.5f),
        slots = mapOf("N" to SlotFiller.Count("totalLyricsOpens", "time", "times"))),
    GrammarRule("observe_lyr_2", MascotIntent.OBSERVE, "you've opened lyrics {N}, the dedication",
        contextFilter = ContextFilter(requiresPattern = UserPatternType.LYRICS_LOVER, minConfidence = 0.6f),
        slots = mapOf("N" to SlotFiller.Count("totalLyricsOpens", "once", "many times"))),
    GrammarRule("observe_lyr_3", MascotIntent.OBSERVE, "lyrics mode, i see you",
        contextFilter = ContextFilter(requiresPattern = UserPatternType.LYRICS_LOVER, minConfidence = 0.5f),
        moodAffinity = listOf(SkipperMood.MUSED)),

    GrammarRule("confess_lyr_1", MascotIntent.CONFESS, "your lyrics reading habit is honestly endearing",
        contextFilter = ContextFilter(requiresPattern = UserPatternType.LYRICS_LOVER, minConfidence = 0.5f),
        moodAffinity = listOf(SkipperMood.MUSED)),
    GrammarRule("confess_lyr_2", MascotIntent.CONFESS, "you actually care about the words, that's rare",
        contextFilter = ContextFilter(requiresPattern = UserPatternType.LYRICS_LOVER, minConfidence = 0.6f),
        moodAffinity = listOf(SkipperMood.MUSED)),
)
