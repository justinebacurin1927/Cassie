package com.example.cassie.party.grammar.rules

import com.example.cassie.party.MascotIntent
import com.example.cassie.party.SkipperMood
import com.example.cassie.party.UserPatternType
import com.example.cassie.party.grammar.ContextFilter
import com.example.cassie.party.grammar.GrammarRule
import com.example.cassie.party.grammar.SlotFiller

/**
 * NIGHT_OWL pattern rules — app opens cluster between 22:00–04:00.
 *
 * Voice: roast (gentle) + confess + question. Always about the
 * user's sleep schedule, never about the music.
 */
internal fun nightOwlRules(): List<GrammarRule> = listOf(
    // ROAST
    GrammarRule("roast_night_1", MascotIntent.ROAST, "bestie it's {time}",
        contextFilter = ContextFilter(requiresPattern = UserPatternType.NIGHT_OWL, minConfidence = 0.5f),
        slots = mapOf("time" to SlotFiller.Dynamic("nightOwlHourLabel"))),
    GrammarRule("roast_night_2", MascotIntent.ROAST, "the walls are closing in, you should sleep",
        contextFilter = ContextFilter(requiresPattern = UserPatternType.NIGHT_OWL, minConfidence = 0.6f),
        moodAffinity = listOf(SkipperMood.EEPY)),
    GrammarRule("roast_night_3", MascotIntent.ROAST, "tomorrow you is going to be so mad at you you",
        contextFilter = ContextFilter(requiresPattern = UserPatternType.NIGHT_OWL, minConfidence = 0.6f), weight = 1.2f),
    GrammarRule("roast_night_4", MascotIntent.ROAST, "the {time} panic is real",
        contextFilter = ContextFilter(requiresPattern = UserPatternType.NIGHT_OWL, minConfidence = 0.6f),
        slots = mapOf("time" to SlotFiller.Dynamic("nightOwlHourLabel"))),
    GrammarRule("roast_night_5", MascotIntent.ROAST, "horizontal scrolling through your library is not a sleep aid",
        contextFilter = ContextFilter(requiresPattern = UserPatternType.NIGHT_OWL, minConfidence = 0.7f),
        weight = 1.1f, moodAffinity = listOf(SkipperMood.EEPY)),
    GrammarRule("roast_night_6", MascotIntent.ROAST, "your sleep schedule is a work of fiction",
        contextFilter = ContextFilter(requiresPattern = UserPatternType.NIGHT_OWL, minConfidence = 0.7f)),

    // CONFESS
    GrammarRule("confess_night_1", MascotIntent.CONFESS, "your late night listening sessions are a whole thing",
        contextFilter = ContextFilter(requiresPattern = UserPatternType.NIGHT_OWL, minConfidence = 0.5f)),
    GrammarRule("confess_night_2", MascotIntent.CONFESS, "the {time} energy is real",
        contextFilter = ContextFilter(requiresPattern = UserPatternType.NIGHT_OWL, minConfidence = 0.6f),
        slots = mapOf("time" to SlotFiller.Dynamic("nightOwlHourLabel"))),

    // QUESTION
    GrammarRule("question_night_1", MascotIntent.QUESTION, "bestie is there a reason you're still up",
        contextFilter = ContextFilter(requiresPattern = UserPatternType.NIGHT_OWL, minConfidence = 0.5f)),
    GrammarRule("question_night_2", MascotIntent.QUESTION, "do you need company or do you need a bed",
        contextFilter = ContextFilter(requiresPattern = UserPatternType.NIGHT_OWL, minConfidence = 0.6f),
        moodAffinity = listOf(SkipperMood.MUSED)),
    GrammarRule("question_night_3", MascotIntent.QUESTION, "have you tried the lying down position",
        contextFilter = ContextFilter(requiresPattern = UserPatternType.NIGHT_OWL, minConfidence = 0.7f),
        weight = 1.1f, moodAffinity = listOf(SkipperMood.WITTY)),
)
