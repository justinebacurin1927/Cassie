package com.example.cassie.party.grammar.rules

import com.example.cassie.party.MascotIntent
import com.example.cassie.party.SkipperMood
import com.example.cassie.party.UserPatternType
import com.example.cassie.party.grammar.ContextFilter
import com.example.cassie.party.grammar.GrammarRule
import com.example.cassie.party.grammar.SlotFiller

/**
 * REPEATER pattern rules — one song dominates the user's
 * listening share (>= 5 minutes AND >= 30% of total).
 */
internal fun repeaterRules(): List<GrammarRule> = listOf(
    GrammarRule("roast_repeat_1", MascotIntent.ROAST, "this song gets more play than your ex's voicemail",
        contextFilter = ContextFilter(requiresPattern = UserPatternType.REPEATER, minConfidence = 0.5f)),
    GrammarRule("roast_repeat_2", MascotIntent.ROAST, "this song is your whole personality at this point",
        contextFilter = ContextFilter(requiresPattern = UserPatternType.REPEATER, minConfidence = 0.6f),
        weight = 1.3f, moodAffinity = listOf(SkipperMood.WITTY)),
    GrammarRule("roast_repeat_3", MascotIntent.ROAST, "the algorithm is confused because of you",
        contextFilter = ContextFilter(requiresPattern = UserPatternType.REPEATER, minConfidence = 0.6f)),
    GrammarRule("roast_repeat_4", MascotIntent.ROAST, "ok one song for a week is a lifestyle",
        contextFilter = ContextFilter(requiresPattern = UserPatternType.REPEATER, minConfidence = 0.7f),
        moodAffinity = listOf(SkipperMood.DRAMATIC)),

    GrammarRule("observe_repeat_1", MascotIntent.OBSERVE, "this song is {share}% of your recent listening",
        contextFilter = ContextFilter(requiresPattern = UserPatternType.REPEATER, minConfidence = 0.5f),
        slots = mapOf("share" to SlotFiller.Dynamic("topSharePercent"))),
    GrammarRule("observe_repeat_2", MascotIntent.OBSERVE, "you're the reason streaming services exist",
        contextFilter = ContextFilter(requiresPattern = UserPatternType.REPEATER, minConfidence = 0.6f)),
)
