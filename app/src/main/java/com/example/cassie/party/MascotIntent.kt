package com.example.cassie.party

/**
 * The 5 "stances" Skipper takes when it speaks.
 *
 * An intent is the *purpose* of a line, not its *content*. A ROAST
 * line and a PRAISE line can both be about the same behavior — they
 * just take opposite sides on it. Mixing intents is what keeps Skipper
 * from sounding one-note.
 */
enum class MascotIntent {
    /** Neutral observation of what the user just did. The default. */
    OBSERVE,

    /** Playful teasing. The whole point of Skipper. Must stay loving. */
    ROAST,

    /** Hype, celebration, validation. Used sparingly so it lands. */
    PRAISE,

    /** Skipper talking about ITSELF. Builds the feeling that the
     *  penguin is a character with its own life. */
    CONFESS,

    /** A direct or rhetorical question. Pulls the user in. */
    QUESTION,
}
