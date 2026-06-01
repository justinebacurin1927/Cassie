package com.example.cassie.party.grammar

/**
 * A [com.example.cassie.party.GrammarRule] template is mostly literal
 * text, but can contain `{slot}` placeholders. A [SlotFiller] decides
 * what goes in each one.
 *
 * Why a sealed class and not a function? Because the rules database
 * is just a list of data. We want to declare ~60 rules in a single
 * Kotlin file without writing a function for every slot, and we want
 * the slot logic to be inspectable from the anti-repetition layer
 * ("did we just say {N}?").
 */
sealed class SlotFiller {
    /** Always emits the same text. */
    data class Literal(val value: String) : SlotFiller()

    /** Picks one at random from the list. */
    data class OneOf(val options: List<String>) : SlotFiller()

    /**
     * Filled in at generation time from the live [SlotContext].
     * The [key] must be a field name on [SlotContext] (looked up
     * reflectively OR via a small switch in [com.example.cassie.party.LineGenerator]).
     */
    data class Dynamic(val key: String) : SlotFiller()

    /**
     * Filled in by combining a number from [SlotContext] with a
     * singular/plural template pair. Used for "you've looped this
     * {N|1=time|2=times}".
     */
    data class Count(
        val source: String,         // a number from SlotContext
        val singular: String,       // text when count == 1
        val plural: String,         // text when count > 1
    ) : SlotFiller()
}
