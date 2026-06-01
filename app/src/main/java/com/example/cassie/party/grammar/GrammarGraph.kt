package com.example.cassie.party.grammar

import com.example.cassie.party.MascotIntent
import com.example.cassie.party.SkipperMood
import com.example.cassie.party.UserPatternType

/**
 * The Skipper rules database.
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

    // ── Builder helper ─────────────────────────────────────────────
    private fun r(
        id: String,
        intent: MascotIntent,
        template: String,
        contextFilter: ContextFilter? = null,
        weight: Float = 1f,
        moodAffinity: List<SkipperMood> = emptyList(),
        slots: Map<String, SlotFiller> = emptyMap(),
    ) = GrammarRule(id, intent, template, slots, contextFilter, weight, moodAffinity)

    private fun pat(
        type: UserPatternType,
        minConf: Float = 0.5f,
    ) = ContextFilter(requiresPattern = type, minConfidence = minConf)

    // ════════════════════════════════════════════════════════════════
    // 1. SKIPPER — user is skipping songs fast
    // ════════════════════════════════════════════════════════════════
    private fun skipperRules(): List<GrammarRule> = listOf(
        // ROAST — about the user's habit, not the penguin
        r("roast_skip_1", MascotIntent.ROAST, "bestie the skip button is not a personality trait",
            pat(UserPatternType.SKIPPER, 0.7f), weight = 1.2f),
        r("roast_skip_2", MascotIntent.ROAST, "the songs are not the problem bestie, just saying",
            pat(UserPatternType.SKIPPER, 0.7f)),
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
        r("roast_skip_10", MascotIntent.ROAST, "is there a song you wouldn't skip",
            pat(UserPatternType.SKIPPER, 0.8f), weight = 1.1f),
        r("roast_skip_11", MascotIntent.ROAST, "you've skipped more songs than you've finished today",
            pat(UserPatternType.SKIPPER, 0.9f)),
        r("roast_skip_12", MascotIntent.ROAST, "this is a cry for help and i'm here for it",
            pat(UserPatternType.SKIPPER, 0.9f), weight = 1.3f),

        // OBSERVE — about the user's stat
        r("observe_skip_1", MascotIntent.OBSERVE, "interesting skip pattern you have there",
            pat(UserPatternType.SKIPPER, 0.6f)),
        r("observe_skip_2", MascotIntent.OBSERVE, "you've been through {N} songs in a row and skipped most of them",
            pat(UserPatternType.SKIPPER, 0.7f),
            slots = mapOf("N" to SlotFiller.Count("recentSkipsCount", "song", "songs"))),
        r("observe_skip_3", MascotIntent.OBSERVE, "skipped {N} in a row, the streak is real",
            pat(UserPatternType.SKIPPER, 0.8f),
            slots = mapOf("N" to SlotFiller.Count("totalSongsSkipped", "song", "songs"))),

        // CONFESS — about the user, not the penguin's life
        r("confess_skip_1", MascotIntent.CONFESS, "your skip rate is breaking records today",
            pat(UserPatternType.SKIPPER, 0.8f)),
        r("confess_skip_2", MascotIntent.CONFESS, "lowkey worried about your taste rn",
            pat(UserPatternType.SKIPPER, 0.9f), moodAffinity = listOf(SkipperMood.MUSED)),

        // QUESTION
        r("question_skip_1", MascotIntent.QUESTION, "do you even like this playlist or are you just pressing play",
            pat(UserPatternType.SKIPPER, 0.8f), weight = 1.1f),
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
        r("roast_loop_8", MascotIntent.ROAST, "this is between us and god",
            pat(UserPatternType.LOOPER, 0.8f), moodAffinity = listOf(SkipperMood.DRAMATIC)),

        // OBSERVE
        r("observe_loop_1", MascotIntent.OBSERVE, "you've looped this {N} times now",
            pat(UserPatternType.LOOPER, 0.5f),
            slots = mapOf("N" to SlotFiller.Count("currentLoopCount", "time", "times"))),
        r("observe_loop_2", MascotIntent.OBSERVE, "loop count: {N}, status: obsessed",
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

        // CONFESS — about the user
        r("confess_loop_1", MascotIntent.CONFESS, "you've found your song and you're not letting go",
            pat(UserPatternType.LOOPER, 0.6f), weight = 1.1f),
        r("confess_loop_2", MascotIntent.CONFESS, "the dedication is unmatched honestly",
            pat(UserPatternType.LOOPER, 0.7f), moodAffinity = listOf(SkipperMood.MUSED)),
        r("confess_loop_3", MascotIntent.CONFESS, "your focus on this one song is impressive",
            pat(UserPatternType.LOOPER, 0.7f), moodAffinity = listOf(SkipperMood.MUSED)),
        r("confess_loop_4", MascotIntent.CONFESS, "you're going to dream about this song tonight",
            pat(UserPatternType.LOOPER, 0.9f), moodAffinity = listOf(SkipperMood.EEPY)),

        // PRAISE (rare, but about the user)
        r("praise_loop_1", MascotIntent.PRAISE, "absolute focus, the loyalty",
            pat(UserPatternType.LOOPER, 0.5f), weight = 0.6f, moodAffinity = listOf(SkipperMood.HYPED)),
    )

    // ════════════════════════════════════════════════════════════════
    // 3. REPEATER — one song dominates recent listening
    // ════════════════════════════════════════════════════════════════
    private fun repeaterRules(): List<GrammarRule> = listOf(
        r("roast_repeat_1", MascotIntent.ROAST, "this song gets more play than your ex's voicemail",
            pat(UserPatternType.REPEATER, 0.5f)),
        r("roast_repeat_2", MascotIntent.ROAST, "this song is your whole personality at this point",
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

        r("confess_marathon_1", MascotIntent.CONFESS, "you are locked in on this one",
            pat(UserPatternType.MARATHONER, 0.5f), moodAffinity = listOf(SkipperMood.MUSED)),
        r("confess_marathon_2", MascotIntent.CONFESS, "your marathon listening is honestly impressive",
            pat(UserPatternType.MARATHONER, 0.7f), moodAffinity = listOf(SkipperMood.MUSED)),
        r("confess_marathon_3", MascotIntent.CONFESS, "the song should get a job with these hours",
            pat(UserPatternType.MARATHONER, 0.7f)),

        r("praise_marathon_1", MascotIntent.PRAISE, "the dedication is unmatched",
            pat(UserPatternType.MARATHONER, 0.5f), weight = 0.6f),
    )

    // ════════════════════════════════════════════════════════════════
    // 5. PARTIER — party mode active
    // ════════════════════════════════════════════════════════════════
    private fun partierRules(): List<GrammarRule> = listOf(
        r("praise_party_1", MascotIntent.PRAISE, "party mode activated, we are so locked in",
            pat(UserPatternType.PARTIER, 0.5f), weight = 1.3f, moodAffinity = listOf(SkipperMood.HYPED, SkipperMood.CHAOTIC)),
        r("praise_party_2", MascotIntent.PRAISE, "the energy is unmatched rn",
            pat(UserPatternType.PARTIER, 0.5f), weight = 1.2f, moodAffinity = listOf(SkipperMood.HYPED)),
        r("praise_party_3", MascotIntent.PRAISE, "this is main character hours",
            pat(UserPatternType.PARTIER, 0.6f), weight = 1.1f, moodAffinity = listOf(SkipperMood.HYPED)),
        r("praise_party_4", MascotIntent.PRAISE, "your shuffle game is iconic",
            pat(UserPatternType.PARTIER, 0.5f), moodAffinity = listOf(SkipperMood.HYPED)),

        r("observe_party_1", MascotIntent.OBSERVE, "party mode on, queue is on shuffle",
            pat(UserPatternType.PARTIER, 0.5f)),
        r("observe_party_2", MascotIntent.OBSERVE, "every song is a surprise now, this is chaos",
            pat(UserPatternType.PARTIER, 0.6f), moodAffinity = listOf(SkipperMood.CHAOTIC)),

        r("confess_party_1", MascotIntent.CONFESS, "your music taste under party mode is unhinged and i respect it",
            pat(UserPatternType.PARTIER, 0.6f), weight = 1.1f, moodAffinity = listOf(SkipperMood.CHAOTIC)),
        r("confess_party_2", MascotIntent.CONFESS, "you are the human aux cord rn",
            pat(UserPatternType.PARTIER, 0.7f), weight = 1.1f, moodAffinity = listOf(SkipperMood.HYPED)),
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

        r("observe_explore_1", MascotIntent.OBSERVE, "no song is safe from your queue",
            pat(UserPatternType.EXPLORER, 0.5f)),
        r("observe_explore_2", MascotIntent.OBSERVE, "every song gets its moment in your rotation",
            pat(UserPatternType.EXPLORER, 0.6f), moodAffinity = listOf(SkipperMood.MUSED)),
        r("observe_explore_3", MascotIntent.OBSERVE, "{N} unique songs so far today, the range is real",
            pat(UserPatternType.EXPLORER, 0.5f),
            slots = mapOf("N" to SlotFiller.Count("uniqueSongsListened", "song", "songs"))),

        r("confess_explore_1", MascotIntent.CONFESS, "your taste is all over the place and i respect it",
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
        r("roast_night_6", MascotIntent.ROAST, "your sleep schedule is a work of fiction",
            pat(UserPatternType.NIGHT_OWL, 0.7f)),

        r("confess_night_1", MascotIntent.CONFESS, "your late night listening sessions are a whole thing",
            pat(UserPatternType.NIGHT_OWL, 0.5f)),
        r("confess_night_2", MascotIntent.CONFESS, "the {time} energy is real",
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
        r("roast_fav_3", MascotIntent.ROAST, "you have more favorites than a 2014 pinterest board",
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

        r("confess_lyr_1", MascotIntent.CONFESS, "your lyrics reading habit is honestly endearing",
            pat(UserPatternType.LYRICS_LOVER, 0.5f), moodAffinity = listOf(SkipperMood.MUSED)),
        r("confess_lyr_2", MascotIntent.CONFESS, "you actually care about the words, that's rare",
            pat(UserPatternType.LYRICS_LOVER, 0.6f), moodAffinity = listOf(SkipperMood.MUSED)),
    )

    // ════════════════════════════════════════════════════════════════
    // 10. AMBIENT — no specific pattern required
    // CRITICAL: rules about "quiet" or "still" must require
    // !isActivelyPlaying. Otherwise they fire while music is on,
    // which is a bug ("playing music and it says I'm still quiet").
    // ════════════════════════════════════════════════════════════════
    private fun ambientRules(): List<GrammarRule> = listOf(
        // PRAISE — about the user
        r("ambient_praise_1", MascotIntent.PRAISE, "your music taste is showing and i like it",
            weight = 0.7f, moodAffinity = listOf(SkipperMood.CHILL, SkipperMood.WHATEVER)),
        r("ambient_praise_2", MascotIntent.PRAISE, "the vibes are immaculate rn",
            weight = 0.6f, moodAffinity = listOf(SkipperMood.CHILL)),
        r("ambient_praise_3", MascotIntent.PRAISE, "elite listening behavior",
            weight = 0.5f, moodAffinity = listOf(SkipperMood.HYPED)),

        // OBSERVE — about the user, when they're actually playing
        r("ambient_observe_1", MascotIntent.OBSERVE, "still vibing, still watching",
            weight = 0.5f, moodAffinity = listOf(SkipperMood.NOSY, SkipperMood.WHATEVER),
            contextFilter = ContextFilter(requiresActivePlayback = true)),
        r("ambient_observe_2", MascotIntent.OBSERVE, "vibing",
            weight = 0.4f, moodAffinity = listOf(SkipperMood.CHILL),
            contextFilter = ContextFilter(requiresActivePlayback = true)),
        r("ambient_observe_3", MascotIntent.OBSERVE, "we are doing this",
            weight = 0.4f, moodAffinity = listOf(SkipperMood.CHILL, SkipperMood.WHATEVER),
            contextFilter = ContextFilter(requiresActivePlayback = true)),
        r("ambient_observe_4", MascotIntent.OBSERVE, "what are we listening to today",
            weight = 0.5f, moodAffinity = listOf(SkipperMood.NOSY),
            contextFilter = ContextFilter(requiresActivePlayback = true)),
        r("ambient_observe_5", MascotIntent.OBSERVE, "your queue is the main character right now",
            weight = 0.5f, moodAffinity = listOf(SkipperMood.NOSY),
            contextFilter = ContextFilter(requiresActivePlayback = true)),

        // CONFESS — about the user
        r("ambient_confess_1", MascotIntent.CONFESS, "you've been on the same vibe for {N} and i respect that",
            weight = 0.6f,
            slots = mapOf("N" to SlotFiller.Dynamic("currentMinutesLabel")),
            contextFilter = ContextFilter(requiresActivePlayback = true)),
        r("ambient_confess_2", MascotIntent.CONFESS, "your listening session is going strong",
            weight = 0.5f, contextFilter = ContextFilter(requiresActivePlayback = true)),

        // IDLE / QUIET lines — ONLY fire when NOT playing (the fix)
        r("ambient_idle_1", MascotIntent.OBSERVE, "you've been quiet for {N}, everything ok",
            weight = 0.6f,
            slots = mapOf("N" to SlotFiller.Dynamic("quietMinutesLabel")),
            contextFilter = ContextFilter(requiresActivePlayback = false)),
        r("ambient_idle_2", MascotIntent.OBSERVE, "the app is open but the music isn't, just saying",
            weight = 0.4f,
            contextFilter = ContextFilter(requiresActivePlayback = false)),
        r("ambient_idle_3", MascotIntent.QUESTION, "are you picking something or just vibing in silence",
            weight = 0.5f,
            contextFilter = ContextFilter(requiresActivePlayback = false)),
        r("ambient_idle_4", MascotIntent.ROAST, "bestie press play, the queue misses you",
            weight = 0.5f, moodAffinity = listOf(SkipperMood.WITTY),
            contextFilter = ContextFilter(requiresActivePlayback = false)),

        // QUESTION — generic
        r("ambient_question_1", MascotIntent.QUESTION, "what's the vibe today",
            weight = 0.5f, moodAffinity = listOf(SkipperMood.NOSY, SkipperMood.WHATEVER)),

        // ROAST (mild ambient)
        r("ambient_roast_1", MascotIntent.ROAST, "i'm clocking your listening habits",
            weight = 0.3f, moodAffinity = listOf(SkipperMood.NOSY)),
    )

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
