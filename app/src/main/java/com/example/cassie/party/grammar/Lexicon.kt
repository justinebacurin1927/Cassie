package com.example.cassie.party.grammar

/**
 * Skipper's vocabulary database.
 *
 * This is data, not behavior. Every word/phrase here is a seed for
 * grammar rules — the rules themselves decide when to use what.
 *
 * TONE RULES for adding to this file:
 *  - Internet-fluent but not cringe. If it sounds like a 2016 meme
 *    account, don't add it.
 *  - Self-aware. Skipper knows it's a penguin mascot on a music app.
 *  - Loving roasts only. Never cruel.
 *  - No emoji, ever. (Per user requirement.)
 */
object Lexicon {

    // ── Slang seeds ─────────────────────────────────────────────
    // Short interjections Skipper sprinkles in. Always lowercase, no
    // punctuation at the end (the rule template adds it).
    val slangOpeners: List<String> = listOf(
        "bestie",
        "ok but",
        "the way",
        "no bc",
        "lowkey",
        "highkey",
        "be so fr",
        "real talk",
        "not me",
        "we need to talk",
        "okay so",
        "be so for real",
        "listen",
        "hear me out",
        "ngl",
        "i fear",
        "sir/ma'am",
    )

    val slangMid: List<String> = listOf(
        "i fear",
        "lowkey",
        "highkey",
        "no cap",
        "fr fr",
        "on god",
        "for real",
        "deadass",
        "bestie",
        "the way",
        "it's giving",
        "the vibes are",
        "we are so",
        "this is so",
        "literally me",
    )

    val slangClosers: List<String> = listOf(
        "bestie",
        "i fear",
        "bestie please",
        "with love",
        "as a treat",
        "from me to you",
        "that's the post",
        "end of story",
    )

    // ── Penguin-isms ────────────────────────────────────────────
    // Words that ground Skipper in the fact that it's a penguin.
    val penguinSounds: List<String> = listOf(
        "*flaps wings*",
        "*honks*",
        "*slides across ice*",
        "*tilts head*",
        "*preens*",
        "*pats your back with one wing*",
        "*stares*",
        "*does a small bow*",
    )

    val penguinSelf: List<String> = listOf(
        "this penguin",
        "your local flightless bird",
        "your favorite penguin",
        "a humble penguin",
        "this little guy",
        "your music penguin",
    )

    // ── Reactions (no emoji — text only) ────────────────────────
    val reactions: List<String> = listOf(
        "i'm screaming",
        "i'm deceased",
        "send help",
        "i need a moment",
        "let me sit down",
        "i have notes",
        "noted",
        "this is a lot",
        "i'm watching",
        "i'm clocking this",
        "say less",
        "respectfully",
        "with my whole chest",
    )

    // ── Hype / praise reactions ─────────────────────────────────
    val hype: List<String> = listOf(
        "we are so locked in",
        "the dedication is unmatched",
        "we love to see it",
        "absolute focus",
        "main character hours",
        "this is the version of you i stan",
        "elite behavior",
        "iconic",
        "this is art",
        "the vibes are immaculate",
        "no notes",
    )

    // ── Roasts (loving only) ────────────────────────────────────
    val roasts: List<String> = listOf(
        "this is a cry for help and i'm here for it",
        "you need a shower, a nap, and a snack",
        "touch grass bestie",
        "the bar is in hell and you limbo'd under it",
        "this is not a personality trait",
        "bestie the skip button is not a hobby",
        "this is between us and god",
        "i'm not mad i'm just disappointed (i am a little mad)",
        "we are not the same",
        "i fear you need intervention",
    )

    // ── Looper / obsession reactions ────────────────────────────
    val loop: List<String> = listOf(
        "this is your 8th loop btw",
        "we get it you like it",
        "the song is not going anywhere i promise",
        "is the song a person because you're in a situationship with it",
        "ok stalker",
        "this is a parasocial relationship with a song",
        "you've looped this more than i've ever flapped",
        "the song would like a break please",
    )

    // ── Late-night / sleep-deprived reactions ───────────────────
    val lateNight: List<String> = listOf(
        "bestie it's late",
        "the sun is gone and so should you be",
        "your sleep schedule is a work of fiction",
        "this is your 5th hour btw",
        "tomorrow you is going to be so mad at you you",
        "touch grass. then touch your bed.",
        "the walls are closing in aren't they",
        "horizontal scrolling through spotify is not a sleep aid",
    )

    // ── Skipping / indecisive reactions ─────────────────────────
    val skipper: List<String> = listOf(
        "goldfish behavior honestly",
        "you just skipped 4 songs in 2 minutes",
        "the song didn't even get to say hi",
        "commitment issues much",
        "the song was JUST about to get good",
        "you have the attention span of a squirrel on espresso",
        "the song is crying btw",
        "every skipped song writes a letter to me",
        "this is musical speed dating and you keep ghosting",
    )

    // ── Time-of-day phrases ─────────────────────────────────────
    fun nightPhrase(hour: Int): String = when (hour) {
        in 0..4 -> "it is genuinely concerning o'clock"
        in 5..6 -> "the sun is barely up and you're already in here"
        in 7..10 -> "morning behavior"
        in 11..13 -> "lunch break behavior"
        in 14..16 -> "afternoon delusion hours"
        in 17..19 -> "golden hour listening"
        in 20..22 -> "evening main character"
        in 23..23 -> "it's late and you know it"
        else -> "anyway"
    }
}
