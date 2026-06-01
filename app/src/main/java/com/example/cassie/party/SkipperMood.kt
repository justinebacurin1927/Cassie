package com.example.cassie.party

/**
 * Skipper's OWN emotional state. Independent of the user's behavior
 * patterns — this is the mascot's mood, which evolves over time via
 * the [EmotionalArc] layer.
 *
 * Why a separate state from [UserPattern]? Because the same loop
 * count can feel different at 9am vs 2am, and the user's patterns
 * only describe the *behavior*, not how the mascot *reacts* to it.
 */
enum class SkipperMood {
    /** Default. Nothing interesting happening. The chill baseline. */
    WHATEVER,

    /** Caught the user doing something. A little flustered attention. */
    NOSY,

    /** Energy is up. Detected LOOPER, PARTIER, EXPLORER, or just woken up. */
    HYPED,

    /** The user's behavior is making the mascot introspective.
     *  Late night, deep listening, repeated sad playlists. */
    MUSED,

    /** User is just vibing. Slow tempo, no skips, normal session. */
    CHILL,

    /** Something dramatic happened. A big skip spree, or a song on
     *  repeat for an absurdly long time. */
    DRAMATIC,

    /** Mock-exhausted. User has been at it forever. */
    EEPY,

    /** Sharp, witty, a little mean. Used during ROAST-heavy streaks. */
    WITTY,

    /** Full unhinged energy. The "you've looped this 47 times" moment. */
    CHAOTIC,
}
