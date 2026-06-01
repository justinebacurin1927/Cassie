package com.example.cassie.party.grammar.rules

import com.example.cassie.party.MascotIntent
import com.example.cassie.party.SkipperMood
import com.example.cassie.party.UserPatternType
import com.example.cassie.party.grammar.ContextFilter
import com.example.cassie.party.grammar.GrammarRule
import com.example.cassie.party.grammar.SlotFiller

/**
 * MARATHONER pattern rules — user is locked in on one song for
 * 5+ minutes straight (or has a 20m+ lifetime max).
 */
internal fun marathonerRules(): List<GrammarRule> = listOf(
    GrammarRule("roast_marathon_1", MascotIntent.ROAST, "you've been on this song for {N}, the song has a family",
        contextFilter = ContextFilter(requiresPattern = UserPatternType.MARATHONER, minConfidence = 0.5f),
        slots = mapOf("N" to SlotFiller.Dynamic("currentMinutesLabel"))),
    GrammarRule("roast_marathon_2", MascotIntent.ROAST, "the song didn't sign up for this",
        contextFilter = ContextFilter(requiresPattern = UserPatternType.MARATHONER, minConfidence = 0.6f)),
    GrammarRule("roast_marathon_3", MascotIntent.ROAST, "this song is doing overtime and not getting paid",
        contextFilter = ContextFilter(requiresPattern = UserPatternType.MARATHONER, minConfidence = 0.6f), weight = 1.2f),
    GrammarRule("roast_marathon_4", MascotIntent.ROAST, "you're a one-song-and-dedicated type huh",
        contextFilter = ContextFilter(requiresPattern = UserPatternType.MARATHONER, minConfidence = 0.7f),
        moodAffinity = listOf(SkipperMood.WITTY)),
    GrammarRule("roast_marathon_5", MascotIntent.ROAST, "bestie this is a {N}-minute commitment to a single song",
        contextFilter = ContextFilter(requiresPattern = UserPatternType.MARATHONER, minConfidence = 0.5f),
        slots = mapOf("N" to SlotFiller.Dynamic("currentMinutesLabel"))),

    GrammarRule("confess_marathon_1", MascotIntent.CONFESS, "you are locked in on this one",
        contextFilter = ContextFilter(requiresPattern = UserPatternType.MARATHONER, minConfidence = 0.5f),
        moodAffinity = listOf(SkipperMood.MUSED)),
    GrammarRule("confess_marathon_2", MascotIntent.CONFESS, "your marathon listening is honestly impressive",
        contextFilter = ContextFilter(requiresPattern = UserPatternType.MARATHONER, minConfidence = 0.7f),
        moodAffinity = listOf(SkipperMood.MUSED)),
    GrammarRule("confess_marathon_3", MascotIntent.CONFESS, "the song should get a job with these hours",
        contextFilter = ContextFilter(requiresPattern = UserPatternType.MARATHONER, minConfidence = 0.7f)),

    GrammarRule("praise_marathon_1", MascotIntent.PRAISE, "the dedication is unmatched",
        contextFilter = ContextFilter(requiresPattern = UserPatternType.MARATHONER, minConfidence = 0.5f), weight = 0.6f),
)
