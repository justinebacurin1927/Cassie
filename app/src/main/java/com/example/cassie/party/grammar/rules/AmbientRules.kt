package com.example.cassie.party.grammar.rules

import com.example.cassie.party.MascotIntent
import com.example.cassie.party.SkipperMood
import com.example.cassie.party.grammar.ContextFilter
import com.example.cassie.party.grammar.GrammarRule
import com.example.cassie.party.grammar.SlotFiller

/**
 * AMBIENT rules — no specific pattern required, fired as filler.
 *
 * BIG POOL on purpose. With ~45 ambient rules and the variety
 * from slot fillers + weighted random, the user shouldn't see
 * the same line twice in a session.
 *
 * Two safety invariants the LineGenerator relies on:
 *  - "Quiet" / "idle" lines carry `requiresActivePlayback = false`
 *    so they NEVER fire while music is playing.
 *  - The most common trigger (a song starting) carries
 *    `requiresActivePlayback = true` so the "ok we are doing
 *    this" lines don't fire on app open before the user has
 *    actually pressed play.
 */
internal fun ambientRules(): List<GrammarRule> = listOf(

    // ── SONG JUST STARTED (most common trigger) ──
    GrammarRule("ambient_started_1", MascotIntent.OBSERVE, "ok we are doing this",
        weight = 0.7f, contextFilter = ContextFilter(requiresActivePlayback = true)),
    GrammarRule("ambient_started_2", MascotIntent.OBSERVE, "different one, let's see",
        weight = 0.6f, contextFilter = ContextFilter(requiresActivePlayback = true)),
    GrammarRule("ambient_started_3", MascotIntent.OBSERVE, "the rotation continues",
        weight = 0.6f, contextFilter = ContextFilter(requiresActivePlayback = true)),
    GrammarRule("ambient_started_4", MascotIntent.OBSERVE, "we're on something new",
        weight = 0.6f, contextFilter = ContextFilter(requiresActivePlayback = true)),
    GrammarRule("ambient_started_5", MascotIntent.OBSERVE, "we're on song {N} of the session",
        weight = 0.7f,
        slots = mapOf("N" to SlotFiller.Dynamic("sessionSongNumber")),
        contextFilter = ContextFilter(requiresActivePlayback = true)),
    GrammarRule("ambient_started_6", MascotIntent.OBSERVE, "{N} deep now, how are we feeling",
        weight = 0.6f,
        slots = mapOf("N" to SlotFiller.Dynamic("sessionSongNumber")),
        contextFilter = ContextFilter(requiresActivePlayback = true)),
    GrammarRule("ambient_started_7", MascotIntent.OBSERVE, "the queue is moving",
        weight = 0.5f, contextFilter = ContextFilter(requiresActivePlayback = true)),
    GrammarRule("ambient_started_8", MascotIntent.OBSERVE, "switching things up, noted",
        weight = 0.5f, contextFilter = ContextFilter(requiresActivePlayback = true)),
    GrammarRule("ambient_started_9", MascotIntent.OBSERVE, "we're back at it",
        weight = 0.5f, contextFilter = ContextFilter(requiresActivePlayback = true)),
    GrammarRule("ambient_started_10", MascotIntent.OBSERVE, "we changed it up",
        weight = 0.5f, contextFilter = ContextFilter(requiresActivePlayback = true)),

    // ── SESSION-WIDE observations (about the user) ──
    GrammarRule("ambient_session_1", MascotIntent.OBSERVE, "you've put in {N} of music today",
        weight = 0.6f,
        slots = mapOf("N" to SlotFiller.Dynamic("totalMinutesLabel")),
        contextFilter = ContextFilter(requiresActivePlayback = true)),
    GrammarRule("ambient_session_2", MascotIntent.OBSERVE, "{N} of your life on music today, bestie",
        weight = 0.5f,
        slots = mapOf("N" to SlotFiller.Dynamic("totalMinutesLabel")),
        contextFilter = ContextFilter(requiresActivePlayback = true)),
    GrammarRule("ambient_session_3", MascotIntent.OBSERVE, "you've gone through {N} different songs today",
        weight = 0.6f,
        slots = mapOf("N" to SlotFiller.Count("uniqueSongsListened", "song", "songs")),
        contextFilter = ContextFilter(requiresActivePlayback = true)),
    GrammarRule("ambient_session_4", MascotIntent.OBSERVE, "your listening is {N} minutes strong",
        weight = 0.5f,
        slots = mapOf("N" to SlotFiller.Dynamic("totalMinutesLabel")),
        contextFilter = ContextFilter(requiresActivePlayback = true)),
    GrammarRule("ambient_session_5", MascotIntent.OBSERVE, "we've been at this for {N} now",
        weight = 0.5f,
        slots = mapOf("N" to SlotFiller.Dynamic("currentMinutesLabel")),
        contextFilter = ContextFilter(requiresActivePlayback = true)),
    GrammarRule("ambient_session_6", MascotIntent.OBSERVE, "the variety in your queue is showing",
        weight = 0.5f, contextFilter = ContextFilter(requiresActivePlayback = true)),

    // ── TASTE / PERSONALITY observations ──
    GrammarRule("ambient_taste_1", MascotIntent.OBSERVE, "your music taste is showing and i like it",
        weight = 0.6f, moodAffinity = listOf(SkipperMood.CHILL, SkipperMood.MUSED),
        contextFilter = ContextFilter(requiresActivePlayback = true)),
    GrammarRule("ambient_taste_2", MascotIntent.OBSERVE, "your listening choices are a whole personality",
        weight = 0.5f, contextFilter = ContextFilter(requiresActivePlayback = true)),
    GrammarRule("ambient_taste_3", MascotIntent.OBSERVE, "you have a type and you stick to it",
        weight = 0.4f, contextFilter = ContextFilter(requiresActivePlayback = true)),
    GrammarRule("ambient_taste_4", MascotIntent.PRAISE, "your music taste is unhinged and i respect it",
        weight = 0.4f, moodAffinity = listOf(SkipperMood.CHAOTIC),
        contextFilter = ContextFilter(requiresActivePlayback = true)),
    GrammarRule("ambient_taste_5", MascotIntent.OBSERVE, "i'm clocking your listening pattern",
        weight = 0.4f, moodAffinity = listOf(SkipperMood.NOSY),
        contextFilter = ContextFilter(requiresActivePlayback = true)),

    // ── VIBE observations ──
    GrammarRule("ambient_vibe_1", MascotIntent.PRAISE, "the vibes are immaculate rn",
        weight = 0.5f, moodAffinity = listOf(SkipperMood.CHILL),
        contextFilter = ContextFilter(requiresActivePlayback = true)),
    GrammarRule("ambient_vibe_2", MascotIntent.OBSERVE, "your queue is the main character right now",
        weight = 0.4f, moodAffinity = listOf(SkipperMood.NOSY),
        contextFilter = ContextFilter(requiresActivePlayback = true)),
    GrammarRule("ambient_vibe_3", MascotIntent.OBSERVE, "we are doing this",
        weight = 0.3f, moodAffinity = listOf(SkipperMood.CHILL, SkipperMood.WHATEVER),
        contextFilter = ContextFilter(requiresActivePlayback = true)),
    GrammarRule("ambient_vibe_4", MascotIntent.OBSERVE, "vibing, acknowledged",
        weight = 0.3f, moodAffinity = listOf(SkipperMood.CHILL),
        contextFilter = ContextFilter(requiresActivePlayback = true)),
    GrammarRule("ambient_vibe_5", MascotIntent.OBSERVE, "this is a whole mood",
        weight = 0.4f, moodAffinity = listOf(SkipperMood.CHILL),
        contextFilter = ContextFilter(requiresActivePlayback = true)),
    GrammarRule("ambient_vibe_6", MascotIntent.PRAISE, "elite listening behavior",
        weight = 0.4f, moodAffinity = listOf(SkipperMood.HYPED),
        contextFilter = ContextFilter(requiresActivePlayback = true)),
    GrammarRule("ambient_vibe_7", MascotIntent.OBSERVE, "you are locked in and it shows",
        weight = 0.4f, moodAffinity = listOf(SkipperMood.MUSED),
        contextFilter = ContextFilter(requiresActivePlayback = true)),
    GrammarRule("ambient_vibe_8", MascotIntent.OBSERVE, "the energy is steady",
        weight = 0.3f, moodAffinity = listOf(SkipperMood.CHILL),
        contextFilter = ContextFilter(requiresActivePlayback = true)),

    // ── TIME-OF-DAY observations ──
    GrammarRule("ambient_time_1", MascotIntent.OBSERVE, "it's {time}, we are doing music",
        weight = 0.4f,
        slots = mapOf("time" to SlotFiller.Dynamic("currentHourLabel")),
        contextFilter = ContextFilter(requiresActivePlayback = true)),
    GrammarRule("ambient_time_2", MascotIntent.OBSERVE, "{time} and you are in here vibing",
        weight = 0.3f,
        slots = mapOf("time" to SlotFiller.Dynamic("currentHourLabel")),
        contextFilter = ContextFilter(requiresActivePlayback = true)),

    // ── ROASTS (mild ambient, about the user) ──
    GrammarRule("ambient_roast_1", MascotIntent.ROAST, "i'm clocking your listening habits",
        weight = 0.3f, moodAffinity = listOf(SkipperMood.NOSY)),
    GrammarRule("ambient_roast_2", MascotIntent.ROAST, "your music choices are a ride",
        weight = 0.3f, moodAffinity = listOf(SkipperMood.WITTY)),
    GrammarRule("ambient_roast_3", MascotIntent.ROAST, "every song you pick is a story",
        weight = 0.3f, moodAffinity = listOf(SkipperMood.DRAMATIC)),

    // ── QUESTIONS ──
    GrammarRule("ambient_question_1", MascotIntent.QUESTION, "what's the vibe today",
        weight = 0.4f, moodAffinity = listOf(SkipperMood.NOSY, SkipperMood.WHATEVER)),
    GrammarRule("ambient_question_2", MascotIntent.QUESTION, "where are we going with this queue",
        weight = 0.3f, moodAffinity = listOf(SkipperMood.NOSY)),
    GrammarRule("ambient_question_3", MascotIntent.QUESTION, "are we feeling anything specific",
        weight = 0.3f, moodAffinity = listOf(SkipperMood.MUSED)),

    // ── PRAISE (sparse) ──
    GrammarRule("ambient_praise_1", MascotIntent.PRAISE, "your music taste is a journey and i'm here for it",
        weight = 0.3f, moodAffinity = listOf(SkipperMood.MUSED)),

    // ── IDLE / QUIET lines — ONLY fire when NOT playing ──
    GrammarRule("ambient_idle_1", MascotIntent.OBSERVE, "you've been quiet for {N}, everything ok",
        weight = 0.6f,
        slots = mapOf("N" to SlotFiller.Dynamic("quietMinutesLabel")),
        contextFilter = ContextFilter(requiresActivePlayback = false)),
    GrammarRule("ambient_idle_2", MascotIntent.OBSERVE, "the app is open but the music isn't, just saying",
        weight = 0.4f,
        contextFilter = ContextFilter(requiresActivePlayback = false)),
    GrammarRule("ambient_idle_3", MascotIntent.QUESTION, "are you picking something or just vibing in silence",
        weight = 0.5f,
        contextFilter = ContextFilter(requiresActivePlayback = false)),
    GrammarRule("ambient_idle_4", MascotIntent.ROAST, "bestie press play, the queue misses you",
        weight = 0.5f, moodAffinity = listOf(SkipperMood.WITTY),
        contextFilter = ContextFilter(requiresActivePlayback = false)),
    GrammarRule("ambient_idle_5", MascotIntent.OBSERVE, "no music for {N}, very on brand for you probably",
        weight = 0.4f,
        slots = mapOf("N" to SlotFiller.Dynamic("quietMinutesLabel")),
        contextFilter = ContextFilter(requiresActivePlayback = false)),
)
