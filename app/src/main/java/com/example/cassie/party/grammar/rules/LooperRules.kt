package com.example.cassie.party.grammar.rules

import com.example.cassie.party.MascotIntent
import com.example.cassie.party.SkipperMood
import com.example.cassie.party.UserPatternType
import com.example.cassie.party.grammar.ContextFilter
import com.example.cassie.party.grammar.GrammarRule
import com.example.cassie.party.grammar.SlotFiller

/**
 * LOOPER pattern rules — user is on repeat-one for the same song.
 *
 * Voice: roast (the "ok stalker" lane), observe, confess, praise.
 * All lines address the user's behavior, not the song itself.
 */
internal fun looperRules(): List<GrammarRule> = listOf(
    // ROAST
    GrammarRule("roast_loop_1", MascotIntent.ROAST, "ok stalker",
        contextFilter = ContextFilter(requiresPattern = UserPatternType.LOOPER, minConfidence = 0.5f),
        weight = 1.2f, moodAffinity = listOf(SkipperMood.WITTY)),
    GrammarRule("roast_loop_2", MascotIntent.ROAST, "the song is not going anywhere i promise",
        contextFilter = ContextFilter(requiresPattern = UserPatternType.LOOPER, minConfidence = 0.5f)),
    GrammarRule("roast_loop_3", MascotIntent.ROAST, "we get it you like it",
        contextFilter = ContextFilter(requiresPattern = UserPatternType.LOOPER, minConfidence = 0.5f)),
    GrammarRule("roast_loop_4", MascotIntent.ROAST, "this is a parasocial relationship with a song",
        contextFilter = ContextFilter(requiresPattern = UserPatternType.LOOPER, minConfidence = 0.7f), weight = 1.3f),
    GrammarRule("roast_loop_5", MascotIntent.ROAST, "is the song a person because you're in a situationship",
        contextFilter = ContextFilter(requiresPattern = UserPatternType.LOOPER, minConfidence = 0.7f)),
    GrammarRule("roast_loop_6", MascotIntent.ROAST, "the song would like a break please",
        contextFilter = ContextFilter(requiresPattern = UserPatternType.LOOPER, minConfidence = 0.7f),
        moodAffinity = listOf(SkipperMood.WITTY)),
    GrammarRule("roast_loop_7", MascotIntent.ROAST, "ok {N} loops, do you need me to call someone",
        contextFilter = ContextFilter(requiresPattern = UserPatternType.LOOPER, minConfidence = 0.6f),
        slots = mapOf("N" to SlotFiller.Count("currentLoopCount", "loop", "loops"))),
    GrammarRule("roast_loop_8", MascotIntent.ROAST, "this is between us and god",
        contextFilter = ContextFilter(requiresPattern = UserPatternType.LOOPER, minConfidence = 0.8f),
        moodAffinity = listOf(SkipperMood.DRAMATIC)),

    // OBSERVE
    GrammarRule("observe_loop_1", MascotIntent.OBSERVE, "you've looped this {N} times now",
        contextFilter = ContextFilter(requiresPattern = UserPatternType.LOOPER, minConfidence = 0.5f),
        slots = mapOf("N" to SlotFiller.Count("currentLoopCount", "time", "times"))),
    GrammarRule("observe_loop_2", MascotIntent.OBSERVE, "loop count: {N}, status: obsessed",
        contextFilter = ContextFilter(requiresPattern = UserPatternType.LOOPER, minConfidence = 0.6f),
        slots = mapOf("N" to SlotFiller.Count("currentLoopCount", "loop", "loops"))),
    GrammarRule("observe_loop_3", MascotIntent.OBSERVE, "we are on loop {N} of the same song",
        contextFilter = ContextFilter(requiresPattern = UserPatternType.LOOPER, minConfidence = 0.6f),
        slots = mapOf("N" to SlotFiller.Count("currentLoopCount", "", ""))),
    GrammarRule("observe_loop_4", MascotIntent.OBSERVE, "current loop count: {N} and rising",
        contextFilter = ContextFilter(requiresPattern = UserPatternType.LOOPER, minConfidence = 0.7f),
        slots = mapOf("N" to SlotFiller.Count("currentLoopCount", "", ""))),
    GrammarRule("observe_loop_5", MascotIntent.OBSERVE, "still on this one huh",
        contextFilter = ContextFilter(requiresPattern = UserPatternType.LOOPER, minConfidence = 0.5f),
        moodAffinity = listOf(SkipperMood.NOSY)),
    GrammarRule("observe_loop_6", MascotIntent.OBSERVE, "this is your {N}th loop btw",
        contextFilter = ContextFilter(requiresPattern = UserPatternType.LOOPER, minConfidence = 0.6f),
        slots = mapOf("N" to SlotFiller.Count("currentLoopCount", "", ""))),

    // CONFESS
    GrammarRule("confess_loop_1", MascotIntent.CONFESS, "you've found your song and you're not letting go",
        contextFilter = ContextFilter(requiresPattern = UserPatternType.LOOPER, minConfidence = 0.6f), weight = 1.1f),
    GrammarRule("confess_loop_2", MascotIntent.CONFESS, "the dedication is unmatched honestly",
        contextFilter = ContextFilter(requiresPattern = UserPatternType.LOOPER, minConfidence = 0.7f),
        moodAffinity = listOf(SkipperMood.MUSED)),
    GrammarRule("confess_loop_3", MascotIntent.CONFESS, "your focus on this one song is impressive",
        contextFilter = ContextFilter(requiresPattern = UserPatternType.LOOPER, minConfidence = 0.7f),
        moodAffinity = listOf(SkipperMood.MUSED)),
    GrammarRule("confess_loop_4", MascotIntent.CONFESS, "you're going to dream about this song tonight",
        contextFilter = ContextFilter(requiresPattern = UserPatternType.LOOPER, minConfidence = 0.9f),
        moodAffinity = listOf(SkipperMood.EEPY)),

    // PRAISE
    GrammarRule("praise_loop_1", MascotIntent.PRAISE, "absolute focus, the loyalty",
        contextFilter = ContextFilter(requiresPattern = UserPatternType.LOOPER, minConfidence = 0.5f),
        weight = 0.6f, moodAffinity = listOf(SkipperMood.HYPED)),
)
