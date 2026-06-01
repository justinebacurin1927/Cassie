package com.example.cassie.party.grammar.rules

import com.example.cassie.party.MascotIntent
import com.example.cassie.party.SkipperMood
import com.example.cassie.party.UserPatternType
import com.example.cassie.party.grammar.ContextFilter
import com.example.cassie.party.grammar.GrammarRule
import com.example.cassie.party.grammar.SlotFiller

/**
 * FAVORITE_HOARDER pattern rules — user has favorited 10+ songs
 * across the lifetime. Roast-heavy, mild observe for variety.
 */
internal fun favoriteHoarderRules(): List<GrammarRule> = listOf(
    // ROAST
    GrammarRule("roast_fav_1", MascotIntent.ROAST, "you've favorited {N} songs, that is a lot of favorites",
        contextFilter = ContextFilter(requiresPattern = UserPatternType.FAVORITE_HOARDER, minConfidence = 0.5f),
        slots = mapOf("N" to SlotFiller.Count("totalFavorites", "song", "songs"))),
    GrammarRule("roast_fav_2", MascotIntent.ROAST, "bestie the heart button is not a hobby",
        contextFilter = ContextFilter(requiresPattern = UserPatternType.FAVORITE_HOARDER, minConfidence = 0.6f), weight = 1.2f),
    GrammarRule("roast_fav_3", MascotIntent.ROAST, "you have more favorites than a 2014 pinterest board",
        contextFilter = ContextFilter(requiresPattern = UserPatternType.FAVORITE_HOARDER, minConfidence = 0.6f),
        moodAffinity = listOf(SkipperMood.WITTY)),
    GrammarRule("roast_fav_4", MascotIntent.ROAST, "the favorite button is not a personality trait",
        contextFilter = ContextFilter(requiresPattern = UserPatternType.FAVORITE_HOARDER, minConfidence = 0.7f),
        weight = 1.2f, moodAffinity = listOf(SkipperMood.WITTY)),
    GrammarRule("roast_fav_5", MascotIntent.ROAST, "this is hoarding, respectfully",
        contextFilter = ContextFilter(requiresPattern = UserPatternType.FAVORITE_HOARDER, minConfidence = 0.7f)),

    // OBSERVE
    GrammarRule("observe_fav_1", MascotIntent.OBSERVE, "{N} favorites, the love is real",
        contextFilter = ContextFilter(requiresPattern = UserPatternType.FAVORITE_HOARDER, minConfidence = 0.5f),
        slots = mapOf("N" to SlotFiller.Count("totalFavorites", "favorite", "favorites"))),
    GrammarRule("observe_fav_2", MascotIntent.OBSERVE, "you've tapped that heart a lot today",
        contextFilter = ContextFilter(requiresPattern = UserPatternType.FAVORITE_HOARDER, minConfidence = 0.6f)),
)
