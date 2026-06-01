package com.example.cassie.party.grammar.rules

import com.example.cassie.party.MascotIntent
import com.example.cassie.party.SkipperMood
import com.example.cassie.party.UserPatternType
import com.example.cassie.party.grammar.ContextFilter
import com.example.cassie.party.grammar.GrammarRule
import com.example.cassie.party.grammar.SlotFiller

/**
 * SKIPPER pattern rules — user is skipping songs fast.
 *
 * Voice: roast, observe, confess, question. All about the user's
 * habit, never the music's quality. Lines address the user directly.
 */
internal fun skipperRules(): List<GrammarRule> = listOf(
    // ROAST
    GrammarRule("roast_skip_1", MascotIntent.ROAST, "bestie the skip button is not a personality trait",
        contextFilter = ContextFilter(requiresPattern = UserPatternType.SKIPPER, minConfidence = 0.7f), weight = 1.2f),
    GrammarRule("roast_skip_2", MascotIntent.ROAST, "the songs are not the problem bestie, just saying",
        contextFilter = ContextFilter(requiresPattern = UserPatternType.SKIPPER, minConfidence = 0.7f)),
    GrammarRule("roast_skip_3", MascotIntent.ROAST, "the song didn't even get to say hi and you already left",
        contextFilter = ContextFilter(requiresPattern = UserPatternType.SKIPPER, minConfidence = 0.7f)),
    GrammarRule("roast_skip_4", MascotIntent.ROAST, "we love speedrunning music",
        contextFilter = ContextFilter(requiresPattern = UserPatternType.SKIPPER, minConfidence = 0.8f)),
    GrammarRule("roast_skip_5", MascotIntent.ROAST, "you have the attention span of a squirrel on espresso",
        contextFilter = ContextFilter(requiresPattern = UserPatternType.SKIPPER, minConfidence = 0.7f)),
    GrammarRule("roast_skip_6", MascotIntent.ROAST, "every skipped song writes a letter to me btw",
        contextFilter = ContextFilter(requiresPattern = UserPatternType.SKIPPER, minConfidence = 0.8f)),
    GrammarRule("roast_skip_7", MascotIntent.ROAST, "this is musical speed dating and you keep ghosting",
        contextFilter = ContextFilter(requiresPattern = UserPatternType.SKIPPER, minConfidence = 0.8f)),
    GrammarRule("roast_skip_8", MascotIntent.ROAST, "the song is literally crying",
        contextFilter = ContextFilter(requiresPattern = UserPatternType.SKIPPER, minConfidence = 0.8f),
        moodAffinity = listOf(SkipperMood.DRAMATIC)),
    GrammarRule("roast_skip_9", MascotIntent.ROAST, "commitment issues much",
        contextFilter = ContextFilter(requiresPattern = UserPatternType.SKIPPER, minConfidence = 0.8f)),
    GrammarRule("roast_skip_10", MascotIntent.ROAST, "is there a song you wouldn't skip",
        contextFilter = ContextFilter(requiresPattern = UserPatternType.SKIPPER, minConfidence = 0.8f), weight = 1.1f),
    GrammarRule("roast_skip_11", MascotIntent.ROAST, "you've skipped more songs than you've finished today",
        contextFilter = ContextFilter(requiresPattern = UserPatternType.SKIPPER, minConfidence = 0.9f)),
    GrammarRule("roast_skip_12", MascotIntent.ROAST, "this is a cry for help and i'm here for it",
        contextFilter = ContextFilter(requiresPattern = UserPatternType.SKIPPER, minConfidence = 0.9f), weight = 1.3f),

    // OBSERVE
    GrammarRule("observe_skip_1", MascotIntent.OBSERVE, "interesting skip pattern you have there",
        contextFilter = ContextFilter(requiresPattern = UserPatternType.SKIPPER, minConfidence = 0.6f)),
    GrammarRule("observe_skip_2", MascotIntent.OBSERVE, "you've been through {N} songs in a row and skipped most of them",
        contextFilter = ContextFilter(requiresPattern = UserPatternType.SKIPPER, minConfidence = 0.7f),
        slots = mapOf("N" to SlotFiller.Count("recentSkipsCount", "song", "songs"))),
    GrammarRule("observe_skip_3", MascotIntent.OBSERVE, "skipped {N} in a row, the streak is real",
        contextFilter = ContextFilter(requiresPattern = UserPatternType.SKIPPER, minConfidence = 0.8f),
        slots = mapOf("N" to SlotFiller.Count("totalSongsSkipped", "song", "songs"))),

    // CONFESS
    GrammarRule("confess_skip_1", MascotIntent.CONFESS, "your skip rate is breaking records today",
        contextFilter = ContextFilter(requiresPattern = UserPatternType.SKIPPER, minConfidence = 0.8f)),
    GrammarRule("confess_skip_2", MascotIntent.CONFESS, "lowkey worried about your taste rn",
        contextFilter = ContextFilter(requiresPattern = UserPatternType.SKIPPER, minConfidence = 0.9f),
        moodAffinity = listOf(SkipperMood.MUSED)),

    // QUESTION
    GrammarRule("question_skip_1", MascotIntent.QUESTION, "do you even like this playlist or are you just pressing play",
        contextFilter = ContextFilter(requiresPattern = UserPatternType.SKIPPER, minConfidence = 0.8f), weight = 1.1f),
)
