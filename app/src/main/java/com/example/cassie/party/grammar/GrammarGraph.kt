package com.example.cassie.party.grammar

import com.example.cassie.party.grammar.rules.ambientRules
import com.example.cassie.party.grammar.rules.explorerRules
import com.example.cassie.party.grammar.rules.favoriteHoarderRules
import com.example.cassie.party.grammar.rules.looperRules
import com.example.cassie.party.grammar.rules.lyricsLoverRules
import com.example.cassie.party.grammar.rules.marathonerRules
import com.example.cassie.party.grammar.rules.nightOwlRules
import com.example.cassie.party.grammar.rules.partierRules
import com.example.cassie.party.grammar.rules.repeaterRules
import com.example.cassie.party.grammar.rules.skipperRules

/**
 * The Skipper rules database, aggregated from one file per
 * pattern in the `rules/` subpackage.
 *
 * Voice rules (locked in based on user feedback):
 *  - NO emoji, ever. (Per user requirement.)
 *  - Lines are about the USER'S MUSIC LIFE, not the penguin's life.
 *    Skipper is a sharp observer with a personality, not the topic
 *    of its own lines. Most lines address "you" or "bestie" and
 *    describe behavior. Self-aware penguin flavor is allowed as
 *    seasoning (max ~5% of the database) but never the main course.
 *  - No music-judgment ("this song is great", "great choice").
 *    Every line describes what the user DID, not what the music IS.
 *  - "Quiet" / "idle" lines must require !isActivelyPlaying, so
 *    they never fire when the user is actively listening.
 *  - Tweet-length, max ~120 chars.
 *
 * The split: one file per pattern in `rules/`. `allRules` flattens
 * them with `by lazy` so we don't pay construction cost until the
 * LineGenerator actually needs them.
 */
object GrammarGraph {

    val allRules: List<GrammarRule> by lazy {
        skipperRules() +
            looperRules() +
            repeaterRules() +
            marathonerRules() +
            partierRules() +
            explorerRules() +
            nightOwlRules() +
            favoriteHoarderRules() +
            lyricsLoverRules() +
            ambientRules()
    }

    // ════════════════════════════════════════════════════════════════
    // Filtering — used by the LineGenerator before weighting/picking
    // ════════════════════════════════════════════════════════════════

    fun rulesForContext(ctx: SlotContext): List<GrammarRule> {
        return allRules.filter { rule ->
            val f = rule.contextFilter ?: return@filter true
            // 1. pattern requirement
            if (f.requiresPattern != null) {
                val match = ctx.activePatterns.firstOrNull {
                    it.type == f.requiresPattern && it.confidence >= f.minConfidence
                }
                if (match == null) return@filter false
            }
            // 2. skip-rate
            if (f.requiresHighSkipRate && ctx.recentSkipRate < 0.6f) {
                return@filter false
            }
            // 3. party mode
            if (f.requiresPartyMode && !ctx.isPartyMode) {
                return@filter false
            }
            // 4. looping
            if (f.requiresLooping && ctx.currentLoopCount < 3) {
                return@filter false
            }
            // 5. minutes on current song
            if (ctx.currentMinutesListened < f.minMinutesOnCurrentSong) {
                return@filter false
            }
            // 6. hour range
            if (f.hourRange != null && ctx.hourOfDay !in f.hourRange) {
                return@filter false
            }
            // 7. active playback requirement
            if (f.requiresActivePlayback == true && !ctx.isActivelyPlaying) {
                return@filter false
            }
            if (f.requiresActivePlayback == false && ctx.isActivelyPlaying) {
                return@filter false
            }
            true
        }
    }
}
