package com.example.cassie.party.grammar

import com.example.cassie.party.MascotIntent
import com.example.cassie.party.SkipperMood
import com.example.cassie.party.UserPatternType

/**
 * The complete Skipper rules database.
 *
 * This is the personality. ~80 grammar rules, each one a tiny piece
 * of Skipper's voice. The [LineGenerator] filters this list by
 * context, weights the survivors, picks one, fills the slots, and
 * runs anti-repetition.
 *
 * Organization (in source order, mirrored in [baseRules] list):
 *   1. SKIPPER  rules
 *   2. LOOPER   rules
 *   3. REPEATER rules
 *   4. MARATHONER rules
 *   5. PARTIER rules
 *   6. EXPLORER rules
 *   7. NIGHT_OWL rules
 *   8. FAVORITE_HOARDER rules
 *   9. LYRICS_LOVER rules
 *  10. AMBIENT rules (no specific pattern required)
 *
 * Editing rules:
 *  - No emoji in any template (per user requirement).
 *  - Keep lines short — tweet-length, max ~120 chars. The card
 *    only has so much room.
 *  - Every line must be SOMETHING the user did, not something
 *    the music IS. Even the praise lines are about the user's
 *    behavior ("we love to see it", not "this song is great").
 *  - Tag [moodAffinity] loosely — multiple rules can share a mood.
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

    // ── Builder helper to keep the list below readable ─────────────
    private fun r(
        id: String,
        intent: MascotIntent,
        template: String,
        contextFilter: ContextFilter? = null,
        weight: Float = 1f,
        moodAffinity: List<SkipperMood> = emptyList(),
        slots: Map<String, SlotFiller> = emptyMap(),
    ) = GrammarRule(id, intent, template, slots, contextFilter, weight, moodAffinity)

    // ── Common context filters ─────────────────────────────────────
    private fun pat(
        type: UserPatternType,
        minConf: Float = 0.5f,
        extra: ContextFilter.() -> ContextFilter = { this },
    ) = ContextFilter(requiresPattern = type, minConfidence = minConf).extra()

    // ════════════════════════════════════════════════════════════════
    // 1. SKIPPER — user is skipping songs fast
    // ════════════════════════════════════════════════════════════════
    private fun skipperRules(): List<GrammarRule> = listOf(
        // ROAST
        r("roast_skip_1", MascotIntent.ROAST, "bestie the skip button is not a personality trait",
            pat(UserPatternType.SKIPPER, 0.7f), weight = 1.2f),
        r("roast_skip_2", MascotIntent.ROAST, "ok but you're skipping faster than i can flap",
            pat(UserPatternType.SKIPPER, 0.7f), moodAffinity = listOf(SkipperMood.WITTY)),
        r("roast_skip_3", MascotIntent.ROAST, "the song didn't even get to say hi and you already left",
            pat(UserPatternType.SKIPPER, 0.7f)),
        r("roast_skip_4", MascotIntent.ROAST, "we love speedrunning music",
            pat(UserPatternType.SKIPPER, 0.8f)),
        r("roast_skip_5", MascotIntent.ROAST, "you have the attention span of a squirrel on espresso",
            pat(UserPatternType.SKIPPER, 0.7f)),
        r("roast_skip_6", MascotIntent.ROAST, "every skipped song writes a letter to me btw",
            pat(UserPatternType.SKIPPER, 0.8f)),
        r("roast_skip_7", MascotIntent.ROAST, "this is musical speed dating and you keep ghosting",
            pat(UserPatternType.SKIPPER, 0.8f)),
        r("roast_skip_8", MascotIntent.ROAST, "the song is literally crying",
            pat(UserPatternType.SKIPPER, 0.8f), moodAffinity = listOf(SkipperMood.DRAMATIC)),
        r("roast_skip_9", MascotIntent.ROAST, "commitment issues much",
            pat(UserPatternType.SKIPPER, 0.8f)),
        r("roast_skip_10", MascotIntent.ROAST, "do you actually like any of these songs or",
            pat(UserPatternType.SKIPPER, 0.9f)),
        r("roast_skip_11", MascotIntent.ROAST, "the songs are not the problem bestie",
            pat(UserPatternType.SKIPPER, 0.8f)),
        r("roast_skip_12", MascotIntent.ROAST, "this is a cry for help and i'm here for it",
            pat(UserPatternType.SKIPPER, 0.9f), weight = 1.3f),

        // OBSERVE
        r("observe_skip_1", MascotIntent.OBSERVE, "interesting strategy",
            pat(UserPatternType.SKIPPER, 0.6f)),
        r("observe_skip_2", MascotIntent.OBSERVE, "you've been through {N} songs in a row and skipped most of them",
            pat(UserPatternType.SKIPPER, 0.7f),
            slots = mapOf("N" to SlotFiller.Dynamic("recentSkipCount"))),
        r("observe_skip_3", MascotIntent.OBSERVE, "skipped {N} in a row, the streak is real",
            pat(UserPatternType.SKIPPER, 0.8f),
            slots = mapOf("N" to SlotFiller.Count("totalSongsSkipped", "song", "songs"))),

        // CONFESS
        r("confess_skip_1", MascotIntent.CONFESS, "i'm getting whiplash from your queue",
            pat(UserPatternType.SKIPPER, 0.7f)),
        r("confess_skip_2", MascotIntent.CONFESS, "as the local penguin, i need you to know i'm tired",
            pat(UserPatternType.SKIPPER, 0.8f), moodAffinity = listOf(SkipperMood.EEPY)),
        r("confess_skip_3", MascotIntent.CONFESS, "lowkey worried about your taste rn",
            pat(UserPatternType.SKIPPER, 0.9f), moodAffinity = listOf(SkipperMood.MUSED)),

        // QUESTION
        r("question_skip_1", MascotIntent.QUESTION, "is there a song you wouldn't skip",
            pat(UserPatternType.SKIPPER, 0.7f)),
        r("question_skip_2", MascotIntent.QUESTION, "do you even like this playlist or are you just pressing play",
            pat(UserPatternType.SKIPPER, 0.8f)),
    )

    // ════════════════════════════════════════════════════════════════
    // 2. LOOPER — same song on repeat
    // ════════════════════════════════════════════════════════════════
    private fun looperRules(): List<GrammarRule> = listOf(
        // ROAST
        r("roast_loop_1", MascotIntent.ROAST, "ok stalker",
            pat(UserPatternType.LOOPER, 0.5f), weight = 1.2f, moodAffinity = listOf(SkipperMood.WITTY)),
        r("roast_loop_2", MascotIntent.ROAST, "the song is not going anywhere i promise",
            pat(UserPatternType.LOOPER, 0.5f)),
        r("roast_loop_3", MascotIntent.ROAST, "we get it you like it",
            pat(UserPatternType.LOOPER, 0.5f)),
        r("roast_loop_4", MascotIntent.ROAST, "this is a parasocial relationship with a song",
            pat(UserPatternType.LOOPER, 0.7f), weight = 1.3f),
        r("roast_loop_5", MascotIntent.ROAST, "is the song a person because you're in a situationship",
            pat(UserPatternType.LOOPER, 0.7f)),
        r("roast_loop_6", MascotIntent.ROAST, "the song would like a break please",
            pat(UserPatternType.LOOPER, 0.7f), moodAffinity = listOf(SkipperMood.WITTY)),
        r("roast_loop_7", MascotIntent.ROAST, "ok {N} loops, do you need me to call someone",
            pat(UserPatternType.LOOPER, 0.6f),
            slots = mapOf("N" to SlotFiller.Count("currentLoopCount", "loop", "loops"))),
        r("roast_loop_8", MascotIntent.ROAST, "bestie you've looped this more than i've flapped",
            pat(UserPatternType.LOOPER, 0.7f)),
        r("roast_loop_9", MascotIntent.ROAST, "this is between us and god",
            pat(UserPatternType.LOOPER, 0.8f), moodAffinity = listOf(SkipperMood.DRAMATIC)),

        // OBSERVE
        r("observe_loop_1", MascotIntent.OBSERVE, "you've looped this {N} times now",
            pat(UserPatternType.LOOPER, 0.5f),
            slots = mapOf("N" to SlotFiller.Count("currentLoopCount", "time", "times"))),
        r("observe_loop_2", MascotIntent.OBSERVE, "loop count: {N}, status: obsessive",
            pat(UserPatternType.LOOPER, 0.6f),
            slots = mapOf("N" to SlotFiller.Count("currentLoopCount", "loop", "loops"))),
        r("observe_loop_3", MascotIntent.OBSERVE, "we are on loop {N} of the same song",
            pat(UserPatternType.LOOPER, 0.6f),
            slots = mapOf("N" to SlotFiller.Count("currentLoopCount", "", ""))),
        r("observe_loop_4", MascotIntent.OBSERVE, "current loop count: {N} and rising",
            pat(UserPatternType.LOOPER, 0.7f),
            slots = mapOf("N" to SlotFiller.Count("currentLoopCount", "", ""))),
        r("observe_loop_5", MascotIntent.OBSERVE, "still on this one huh",
            pat(UserPatternType.LOOPER, 0.5f), moodAffinity = listOf(SkipperMood.NOSY)),
        r("observe_loop_6", MascotIntent.OBSERVE, "this is your {N}th loop btw",
            pat(UserPatternType.LOOPER, 0.6f),
            slots = mapOf("N" to SlotFiller.Count("currentLoopCount", "", ""))),

        // CONFESS
        r("confess_loop_1", MascotIntent.CONFESS, "the song is stuck in my head too now thanks",
            pat(UserPatternType.LOOPER, 0.6f)),
        r("confess_loop_2", MascotIntent.CONFESS, "i'm starting to know this one by heart",
            pat(UserPatternType.LOOPER, 0.7f)),
        r("confess_loop_3", MascotIntent.CONFESS, "the dedication is unmatched honestly",
            pat(UserPatternType.LOOPER, 0.7f), moodAffinity = listOf(SkipperMood.MUSED)),
        r("confess_loop_4", MascotIntent.CONFESS, "even i am getting tired of it and i'm a penguin",
            pat(UserPatternType.LOOPER, 0.8f), weight = 1.2f),
        r("confess_loop_5", MascotIntent.CONFESS, "i'm going to dream about this song tonight",
            pat(UserPatternType.LOOPER, 0.9f), moodAffinity = listOf(SkipperMood.EEPY)),

        // PRAISE (rare)
        r("praise_loop_1", MascotIntent.PRAISE, "absolute focus",
            pat(UserPatternType.LOOPER, 0.5f), weight = 0.6f, moodAffinity = listOf(SkipperMood.HYPED)),
        r("praise_loop_2", MascotIntent.PRAISE, "the loyalty",
            pat(UserPatternType.LOOPER, 0.6f), weight = 0.6f, moodAffinity = listOf(SkipperMood.HYPED)),
        r("praise_loop_3", MascotIntent.PRAISE, "we love to see it",
            pat(UserPatternType.LOOPER, 0.5f), weight = 0.5f, moodAffinity = listOf(SkipperMood.HYPED)),
    )

    // ════════════════════════════════════════════════════════════════
    // 3. REPEATER — one song dominates recent listening
    // ════════════════════════════════════════════════════════════════
    private fun repeaterRules(): List<GrammarRule> = listOf(
        r("roast_repeat_1", MascotIntent.ROAST, "this song gets more play than your ex's voicemail",
            pat(UserPatternType.REPEATER, 0.5f)),
        r("roast_repeat_2", MascotIntent.ROAST, "this song is your whole personality",
            pat(UserPatternType.REPEATER, 0.6f), weight = 1.3f, moodAffinity = listOf(SkipperMood.WITTY)),
        r("roast_repeat_3", MascotIntent.ROAST, "the algorithm is confused because of you",
            pat(UserPatternType.REPEATER, 0.6f)),
        r("roast_repeat_4", MascotIntent.ROAST, "ok one song for a week is a lifestyle",
            pat(UserPatternType.REPEATER, 0.7f), moodAffinity = listOf(SkipperMood.DRAMATIC)),

        r("observe_repeat_1", MascotIntent.OBSERVE, "this song is {share}% of your recent listening",
            pat(UserPatternType.REPEATER, 0.5f),
            slots = mapOf("share" to SlotFiller.Dynamic("topSharePercent"))),
        r("observe_repeat_2", MascotIntent.OBSERVE, "you're the reason streaming services exist",
            pat(UserPatternType.REPEATER, 0.6f)),
    )

    // ════════════════════════════════════════════════════════════════
    // 4. MARATHONER — long continuous listen on one song
    // ════════════════════════════════════════════════════════════════
    private fun marathonerRules(): List<GrammarRule> = listOf(
        r("roast_marathon_1", MascotIntent.ROAST, "you've been on this song for {N}, the song has a family",
            pat(UserPatternType.MARATHONER, 0.5f),
            slots = mapOf("N" to SlotFiller.Dynamic("currentMinutesLabel"))),
        r("roast_marathon_2", MascotIntent.ROAST, "the song didn't sign up for this",
            pat(UserPatternType.MARATHONER, 0.6f)),
        r("roast_marathon_3", MascotIntent.ROAST, "this song is doing overtime and not getting paid",
            pat(UserPatternType.MARATHONER, 0.6f), weight = 1.2f),
        r("roast_marathon_4", MascotIntent.ROAST, "you're a one-song-and-dedicated type huh",
            pat(UserPatternType.MARATHONER, 0.7f), moodAffinity = listOf(SkipperMood.WITTY)),
        r("roast_marathon_5", MascotIntent.ROAST, "bestie this is a {N}-minute commitment to a single song",
            pat(UserPatternType.MARATHONER, 0.5f),
            slots = mapOf("N" to SlotFiller.Dynamic("currentMinutesLabel"))),

        r("confess_marathon_1", MascotIntent.CONFESS, "the song is on its {N}-minute mark and so am i",
            pat(UserPatternType.MARATHONER, 0.5f),
            slots = mapOf("N" to SlotFiller.Dynamic("currentMinutesLabel"))),
        r("confess_marathon_2", MascotIntent.CONFESS, "we are both locked in",
            pat(UserPatternType.MARATHONER, 0.6f), moodAffinity = listOf(SkipperMood.MUSED)),
        r("confess_marathon_3", MascotIntent.CONFESS, "the song should get a job with these hours",
            pat(UserPatternType.MARATHONER, 0.7f)),
        r("confess_marathon_4", MascotIntent.CONFESS, "i respect the marathon honestly",
            pat(UserPatternType.MARATHONER, 0.7f), moodAffinity = listOf(SkipperMood.MUSED)),

        r("praise_marathon_1", MascotIntent.PRAISE, "the dedication is unmatched",
            pat(UserPatternType.MARATHONER, 0.5f), weight = 0.6f),
    )

    // ════════════════════════════════════════════════════════════════
    // 5. PARTIER — party mode active or used a lot
    // ════════════════════════════════════════════════════════════════
    private fun partierRules(): List<GrammarRule> = listOf(
        r("praise_party_1", MascotIntent.PRAISE, "party mode activated, the penguin is hyped",
            pat(UserPatternType.PARTIER, 0.5f), weight = 1.3f, moodAffinity = listOf(SkipperMood.HYPED, SkipperMood.CHAOTIC)),
        r("praise_party_2", MascotIntent.PRAISE, "we are so locked in rn",
            pat(UserPatternType.PARTIER, 0.5f), weight = 1.2f, moodAffinity = listOf(SkipperMood.HYPED)),
        r("praise_party_3", MascotIntent.PRAISE, "the energy is unmatched",
            pat(UserPatternType.PARTIER, 0.6f), moodAffinity = listOf(SkipperMood.HYPED, SkipperMood.CHAOTIC)),
        r("praise_party_4", MascotIntent.PRAISE, "this is main character hours",
            pat(UserPatternType.PARTIER, 0.6f), weight = 1.1f, moodAffinity = listOf(SkipperMood.HYPED)),
        r("praise_party_5", MascotIntent.PRAISE, "iconic",
            pat(UserPatternType.PARTIER, 0.5f), weight = 0.7f, moodAffinity = listOf(SkipperMood.HYPED)),

        r("observe_party_1", MascotIntent.OBSERVE, "party mode on, queue is on shuffle",
            pat(UserPatternType.PARTIER, 0.5f)),
        r("observe_party_2", MascotIntent.OBSERVE, "every song is a surprise, this is chaos",
            pat(UserPatternType.PARTIER, 0.6f), moodAffinity = listOf(SkipperMood.CHAOTIC)),

        r("confess_party_1", MascotIntent.CONFESS, "even the penguin is hyped",
            pat(UserPatternType.PARTIER, 0.5f), moodAffinity = listOf(SkipperMood.HYPED)),
        r("confess_party_2", MascotIntent.CONFESS, "i'm doing little spins on the ice to this",
            pat(UserPatternType.PARTIER, 0.6f), weight = 1.1f, moodAffinity = listOf(SkipperMood.HYPED, SkipperMood.CHAOTIC)),
        r("confess_party_3", MascotIntent.CONFESS, "my wings are moving, this is the one",
            pat(UserPatternType.PARTIER, 0.7f), moodAffinity = listOf(SkipperMood.HYPED)),
    )

    // ════════════════════════════════════════════════════════════════
    // 6. EXPLORER — wide variety, low skip rate
    // ════════════════════════════════════════════════════════════════
    private fun explorerRules(): List<GrammarRule> = listOf(
        r("praise_explore_1", MascotIntent.PRAISE, "we love a music explorer",
            pat(UserPatternType.EXPLORER, 0.5f), weight = 1.2f),
        r("praise_explore_2", MascotIntent.PRAISE, "the variety is the whole point honestly",
            pat(UserPatternType.EXPLORER, 0.5f)),
        r("praise_explore_3", MascotIntent.PRAISE, "this is elite music citizen behavior",
            pat(UserPatternType.EXPLORER, 0.5f), weight = 1.1f, moodAffinity = listOf(SkipperMood.MUSED)),
        r("praise_explore_4", MascotIntent.PRAISE, "the algorithm is learning from you",
            pat(UserPatternType.EXPLORER, 0.6f)),
        r("praise_explore_5", MascotIntent.PRAISE, "no notes",
            pat(UserPatternType.EXPLORER, 0.5f), weight = 0.7f),

        r("observe_explore_1", MascotIntent.OBSERVE, "no song is safe from your queue",
            pat(UserPatternType.EXPLORER, 0.5f)),
        r("observe_explore_2", MascotIntent.OBSERVE, "every song gets its moment",
            pat(UserPatternType.EXPLORER, 0.6f), moodAffinity = listOf(SkipperMood.MUSED)),
        r("observe_explore_3", MascotIntent.OBSERVE, "{N} unique songs so far today, the range",
            pat(UserPatternType.EXPLORER, 0.5f),
            slots = mapOf("N" to SlotFiller.Count("uniqueSongsListened", "song", "songs"))),

        r("confess_explore_1", MascotIntent.CONFESS, "i'm learning so much about you rn",
            pat(UserPatternType.EXPLORER, 0.5f), moodAffinity = listOf(SkipperMood.MUSED)),
    )

    // ════════════════════════════════════════════════════════════════
    // 7. NIGHT_OWL — late night listening
    // ════════════════════════════════════════════════════════════════
    private fun nightOwlRules(): List<GrammarRule> = listOf(
        r("roast_night_1", MascotIntent.ROAST, "bestie it's {time}",
            pat(UserPatternType.NIGHT_OWL, 0.5f),
            slots = mapOf("time" to SlotFiller.Dynamic("nightOwlHourLabel"))),
        r("roast_night_2", MascotIntent.ROAST, "the walls are closing in, you should sleep",
            pat(UserPatternType.NIGHT_OWL, 0.6f), moodAffinity = listOf(SkipperMood.EEPY)),
        r("roast_night_3", MascotIntent.ROAST, "tomorrow you is going to be so mad at you you",
            pat(UserPatternType.NIGHT_OWL, 0.6f), weight = 1.2f),
        r("roast_night_4", MascotIntent.ROAST, "the {time} panic is real",
            pat(UserPatternType.NIGHT_OWL, 0.6f),
            slots = mapOf("time" to SlotFiller.Dynamic("nightOwlHourLabel"))),
        r("roast_night_5", MascotIntent.ROAST, "horizontal scrolling through your library is not a sleep aid",
            pat(UserPatternType.NIGHT_OWL, 0.7f), weight = 1.1f, moodAffinity = listOf(SkipperMood.EEPY)),
        r("roast_night_6", MascotIntent.ROAST, "i'm watching you in the dark, respectfully",
            pat(UserPatternType.NIGHT_OWL, 0.7f), moodAffinity = listOf(SkipperMood.NOSY)),

        r("confess_night_1", MascotIntent.CONFESS, "the penguin is also up btw, solidarity",
            pat(UserPatternType.NIGHT_OWL, 0.5f)),
        r("confess_night_2", MascotIntent.CONFESS, "we're both in the late night hours huh",
            pat(UserPatternType.NIGHT_OWL, 0.6f), moodAffinity = listOf(SkipperMood.MUSED)),
        r("confess_night_3", MascotIntent.CONFESS, "the {time} energy is real",
            pat(UserPatternType.NIGHT_OWL, 0.6f),
            slots = mapOf("time" to SlotFiller.Dynamic("nightOwlHourLabel"))),

        r("question_night_1", MascotIntent.QUESTION, "bestie is there a reason you're still up",
            pat(UserPatternType.NIGHT_OWL, 0.5f)),
        r("question_night_2", MascotIntent.QUESTION, "do you need company or do you need a bed",
            pat(UserPatternType.NIGHT_OWL, 0.6f), moodAffinity = listOf(SkipperMood.MUSED)),
        r("question_night_3", MascotIntent.QUESTION, "have you tried the lying down position",
            pat(UserPatternType.NIGHT_OWL, 0.7f), weight = 1.1f, moodAffinity = listOf(SkipperMood.WITTY)),
    )

    // ════════════════════════════════════════════════════════════════
    // 8. FAVORITE_HOARDER — favorited a lot
    // ════════════════════════════════════════════════════════════════
    private fun favoriteHoarderRules(): List<GrammarRule> = listOf(
        r("roast_fav_1", MascotIntent.ROAST, "you've favorited {N} songs, that is a lot of favorites",
            pat(UserPatternType.FAVORITE_HOARDER, 0.5f),
            slots = mapOf("N" to SlotFiller.Count("totalFavorites", "song", "songs"))),
        r("roast_fav_2", MascotIntent.ROAST, "bestie the heart button is not a hobby",
            pat(UserPatternType.FAVORITE_HOARDER, 0.6f), weight = 1.2f),
        r("roast_fav_3", MascotIntent.ROAST, "you have more favorites than a pinterest board from 2014",
            pat(UserPatternType.FAVORITE_HOARDER, 0.6f), moodAffinity = listOf(SkipperMood.WITTY)),
        r("roast_fav_4", MascotIntent.ROAST, "the favorite button is not a personality trait",
            pat(UserPatternType.FAVORITE_HOARDER, 0.7f), weight = 1.2f, moodAffinity = listOf(SkipperMood.WITTY)),
        r("roast_fav_5", MascotIntent.ROAST, "this is hoarding, respectfully",
            pat(UserPatternType.FAVORITE_HOARDER, 0.7f)),

        r("observe_fav_1", MascotIntent.OBSERVE, "{N} favorites, the love is real",
            pat(UserPatternType.FAVORITE_HOARDER, 0.5f),
            slots = mapOf("N" to SlotFiller.Count("totalFavorites", "favorite", "favorites"))),
        r("observe_fav_2", MascotIntent.OBSERVE, "you've tapped that heart a lot today",
            pat(UserPatternType.FAVORITE_HOARDER, 0.6f)),
    )

    // ════════════════════════════════════════════════════════════════
    // 9. LYRICS_LOVER — opens lyrics often
    // ════════════════════════════════════════════════════════════════
    private fun lyricsLoverRules(): List<GrammarRule> = listOf(
        r("observe_lyr_1", MascotIntent.OBSERVE, "lyrics open count: {N}, we love a reader",
            pat(UserPatternType.LYRICS_LOVER, 0.5f),
            slots = mapOf("N" to SlotFiller.Count("totalLyricsOpens", "time", "times"))),
        r("observe_lyr_2", MascotIntent.OBSERVE, "you've opened lyrics {N}, the dedication",
            pat(UserPatternType.LYRICS_LOVER, 0.6f),
            slots = mapOf("N" to SlotFiller.Count("totalLyricsOpens", "once", "many times"))),
        r("observe_lyr_3", MascotIntent.OBSERVE, "lyrics mode, i see you",
            pat(UserPatternType.LYRICS_LOVER, 0.5f), moodAffinity = listOf(SkipperMood.MUSED)),

        r("confess_lyr_1", MascotIntent.CONFESS, "i pretend i understand the lyrics but i'm a penguin",
            pat(UserPatternType.LYRICS_LOVER, 0.5f), weight = 1.2f, moodAffinity = listOf(SkipperMood.WITTY)),
        r("confess_lyr_2", MascotIntent.CONFESS, "lyrics are a whole art form and you know it",
            pat(UserPatternType.LYRICS_LOVER, 0.6f), moodAffinity = listOf(SkipperMood.MUSED)),
        r("confess_lyr_3", MascotIntent.CONFESS, "i'm also looking at the lyrics, the song is deep",
            pat(UserPatternType.LYRICS_LOVER, 0.7f), moodAffinity = listOf(SkipperMood.MUSED)),
    )

    // ════════════════════════════════════════════════════════════════
    // 10. AMBIENT — no specific pattern required
    // ════════════════════════════════════════════════════════════════
    private fun ambientRules(): List<GrammarRule> = listOf(
        // PRAISE
        r("ambient_praise_1", MascotIntent.PRAISE, "the vibes are immaculate",
            weight = 0.7f, moodAffinity = listOf(SkipperMood.CHILL, SkipperMood.WHATEVER)),
        r("ambient_praise_2", MascotIntent.PRAISE, "this is a whole mood",
            weight = 0.6f, moodAffinity = listOf(SkipperMood.CHILL)),
        r("ambient_praise_3", MascotIntent.PRAISE, "elite behavior",
            weight = 0.5f, moodAffinity = listOf(SkipperMood.HYPED)),

        // OBSERVE
        r("ambient_observe_1", MascotIntent.OBSERVE, "still here, still watching",
            weight = 0.5f, moodAffinity = listOf(SkipperMood.NOSY, SkipperMood.WHATEVER)),
        r("ambient_observe_2", MascotIntent.OBSERVE, "vibing",
            weight = 0.4f, moodAffinity = listOf(SkipperMood.CHILL)),
        r("ambient_observe_3", MascotIntent.OBSERVE, "we are doing this",
            weight = 0.4f, moodAffinity = listOf(SkipperMood.CHILL, SkipperMood.WHATEVER)),
        r("ambient_observe_4", MascotIntent.OBSERVE, "what are we doing today",
            weight = 0.5f, moodAffinity = listOf(SkipperMood.NOSY)),
        r("ambient_observe_5", MascotIntent.OBSERVE, "okay so",
            weight = 0.3f, moodAffinity = listOf(SkipperMood.WHATEVER)),

        // CONFESS
        r("ambient_confess_1", MascotIntent.CONFESS, "the penguin is also just vibing",
            weight = 0.6f, moodAffinity = listOf(SkipperMood.CHILL, SkipperMood.WHATEVER)),
        r("ambient_confess_2", MascotIntent.CONFESS, "honestly just happy to be here",
            weight = 0.5f, moodAffinity = listOf(SkipperMood.CHILL)),
        r("ambient_confess_3", MascotIntent.CONFESS, "we have been quiet for {N} and i respect that",
            weight = 0.6f,
            slots = mapOf("N" to SlotFiller.Dynamic("quietMinutesLabel"))),
        r("ambient_confess_4", MascotIntent.CONFESS, "{self} enjoys this very much",
            weight = 0.5f,
            slots = mapOf("self" to SlotFiller.OneOf(Lexicon.penguinSelf))),

        // QUESTION
        r("ambient_question_1", MascotIntent.QUESTION, "what's the vibe today",
            weight = 0.5f, moodAffinity = listOf(SkipperMood.NOSY, SkipperMood.WHATEVER)),
        r("ambient_question_2", MascotIntent.QUESTION, "where are we going with this",
            weight = 0.4f, moodAffinity = listOf(SkipperMood.NOSY)),

        // ROAST (mild ambient)
        r("ambient_roast_1", MascotIntent.ROAST, "i'm watching",
            weight = 0.3f, moodAffinity = listOf(SkipperMood.NOSY)),
        r("ambient_roast_2", MascotIntent.ROAST, "i'm clocking this",
            weight = 0.3f, moodAffinity = listOf(SkipperMood.NOSY, SkipperMood.WITTY)),
    )

    // ════════════════════════════════════════════════════════════════
    // Filtering — used by the LineGenerator before weighting/picking
    // ════════════════════════════════════════════════════════════════

    /**
     * Return only the rules whose [GrammarRule.contextFilter] is
     * satisfied by the given [SlotContext].
     */
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
            // 2. mood whitelist
            if (f.requireMoodIn.isNotEmpty()) {
                // We don't have a mood here in SlotContext; treat
                // this as a no-op (the LineGenerator uses its own
                // mood state for this check). The filter is a hint
                // that gets applied later.
            }
            // 3. skip-rate
            if (f.requiresHighSkipRate && ctx.recentSkipRate < 0.6f) {
                return@filter false
            }
            // 4. party mode
            if (f.requiresPartyMode && !ctx.isPartyMode) {
                return@filter false
            }
            // 5. looping
            if (f.requiresLooping && ctx.currentLoopCount < 3) {
                return@filter false
            }
            // 6. minutes on current song
            if (ctx.currentMinutesListened < f.minMinutesOnCurrentSong) {
                return@filter false
            }
            // 7. hour range
            if (f.hourRange != null && ctx.hourOfDay !in f.hourRange) {
                return@filter false
            }
            true
        }
    }
}
