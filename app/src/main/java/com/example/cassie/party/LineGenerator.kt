package com.example.cassie.party

import com.example.cassie.party.grammar.GrammarGraph
import com.example.cassie.party.grammar.GrammarRule
import com.example.cassie.party.grammar.SlotContext
import com.example.cassie.party.grammar.SlotFiller
import java.util.Calendar
import kotlin.random.Random

/**
 * The Skipper line generator.
 *
 * Runs the five algorithm layers in order, every time a new line is
 * requested. The layers are NOT independent — they read each other's
 * state. The order is:
 *
 *   1. EMOTIONAL ARC      — decay tick, then propose a mood shift
 *                            from the current top pattern.
 *   2. GRAMMAR GRAPH      — filter all ~80 rules to the ones that
 *                            match the current [SlotContext].
 *   3. CONTEXT-WEIGHTED   — boost rules whose moodAffinity matches
 *                            the new mood, whose intent matches the
 *                            current event, etc.
 *   4. ANTI-REPETITION    — dampen any rule that was used very
 *                            recently (or hard-block exact text dups).
 *   5. WEIGHTED RANDOM    — pick one rule from the survivors, fill
 *                            its slots, and return the [SkipperLine].
 *
 * This class is stateless apart from its collaborators ([EmotionalArc],
 * [AntiRepetition]). It is safe to call [generate] from any thread.
 */
class LineGenerator {

    private val emotionalArc = EmotionalArc()
    private val antiRepetition = AntiRepetition(windowSize = 8)
    private val random = Random.Default

    /** Read-only access to the current mascot mood. */
    val currentMood: SkipperMood get() = emotionalArc.mood

    /**
     * Build a [SkipperLine] for the current moment.
     *
     * @param ctx          the live behavior snapshot
     * @param triggerTags  short tags describing what triggered this
     *                     generation (e.g. "pattern:looper" or
     *                     "event:song_started"). Stored on the line.
     */
    fun generate(
        ctx: SlotContext,
        triggerTags: List<String> = emptyList(),
    ): SkipperLine {
        // ── Layer 1: emotional arc ─────────────────────────────────
        emotionalArc.tick()
        val topPatternType = ctx.activePatterns.firstOrNull()?.type
        val targetMood = emotionalArc.suggestFromContext(topPatternType, ctx.hourOfDay)
        emotionalArc.shift(targetMood)
        val currentMood = emotionalArc.mood

        // ── Layer 2: grammar graph filter ───────────────────────────
        val candidates = GrammarGraph.rulesForContext(ctx)
        if (candidates.isEmpty()) {
            // Should never happen — we have ambient rules. But if it
            // does, fall back to the most generic ambient line.
            return fallbackLine(ctx, triggerTags)
        }

        // ── Layers 3+4: weighted by context, dampened by repetition ─
        val weighted = candidates.mapNotNull { rule ->
            val text = previewTemplate(rule, ctx)
            val moodBoost = if (rule.moodAffinity.isEmpty() ||
                rule.moodAffinity.contains(currentMood)
            ) 1.0f else 0.4f
            val repeatDampen = antiRepetition.dampenWeight(rule.id, text)
            if (repeatDampen == 0f) {
                null // hard reject exact duplicate
            } else {
                Weighted(rule, rule.weight * moodBoost * repeatDampen, text)
            }
        }

        if (weighted.isEmpty()) {
            // All candidates were exact-dup. Pick any rule and
            // regenerate a fresh text version if possible.
            val pick = candidates.random(random)
            val text = previewTemplate(pick, ctx)
            return recordAndReturn(pick, text, currentMood, ctx, triggerTags)
        }

        // ── Layer 5: weighted random pick ──────────────────────────
        val pick = weightedPick(weighted)
        return recordAndReturn(pick.rule, pick.preview, currentMood, ctx, triggerTags)
    }

    // ── Helpers ─────────────────────────────────────────────────────

    private fun recordAndReturn(
        rule: GrammarRule,
        text: String,
        mood: SkipperMood,
        ctx: SlotContext,
        triggerTags: List<String>,
    ): SkipperLine {
        val line = SkipperLine(
            text = text,
            intent = rule.intent,
            mood = mood,
            sourceRuleId = rule.id,
            triggerTags = triggerTags,
        )
        antiRepetition.record(line)
        return line
    }

    /**
     * Render a rule template to a concrete string. This is the same
     * pass that the final generation will run, so we can use it to
     * detect exact duplicates BEFORE the final pick.
     */
    private fun previewTemplate(rule: GrammarRule, ctx: SlotContext): String =
        fillTemplate(rule.template, rule.slots, ctx)

    private fun weightedPick(pool: List<Weighted>): Weighted {
        val total = pool.sumOf { it.weight.toDouble() }
        if (total <= 0.0) return pool.random(random)
        var roll = random.nextDouble() * total
        for (w in pool) {
            roll -= w.weight
            if (roll <= 0.0) return w
        }
        return pool.last()
    }

    private fun fallbackLine(ctx: SlotContext, triggerTags: List<String>): SkipperLine {
        val text = "still here, still watching"
        val line = SkipperLine(
            text = text,
            intent = MascotIntent.OBSERVE,
            mood = SkipperMood.WHATEVER,
            sourceRuleId = "fallback_ambient",
            triggerTags = triggerTags,
        )
        antiRepetition.record(line)
        return line
    }

    private data class Weighted(val rule: GrammarRule, val weight: Float, val preview: String)

    // ════════════════════════════════════════════════════════════════
    // Slot template rendering
    // ════════════════════════════════════════════════════════════════

    private val slotRegex = Regex("""\{([^{}]+)\}""")

    private fun fillTemplate(
        template: String,
        slots: Map<String, SlotFiller>,
        ctx: SlotContext,
    ): String = slotRegex.replace(template) { match ->
        val key = match.groupValues[1]
        val filler = slots[key] ?: return@replace match.value
        fillSlot(filler, ctx)
    }

    private fun fillSlot(filler: SlotFiller, ctx: SlotContext): String = when (filler) {
        is SlotFiller.Literal -> filler.value
        is SlotFiller.OneOf -> filler.options.random(random)
        is SlotFiller.Dynamic -> dynamicValue(filler.key, ctx)
        is SlotFiller.Count -> renderCount(filler.source, filler.singular, filler.plural, ctx)
    }

    /**
     * Look up a named value on [SlotContext]. Most are direct field
     * reads; a few (currentMinutesLabel, topSharePercent, etc.) are
     * computed on the fly to keep SlotContext small.
     */
    private fun dynamicValue(key: String, ctx: SlotContext): String = when (key) {
        "currentMinutesLabel" -> formatMinutes(ctx.currentMinutesListened)
        "totalMinutesLabel" -> formatMinutes(ctx.totalMinutesListened)
        "quietMinutesLabel" -> formatMinutes(ctx.sessionMinutes.toFloat())
        "topSharePercent" -> "${(ctx.topMinutesShare * 100).toInt()}%"
        "nightOwlHourLabel" -> formatHour(ctx.hourOfDay)
        "currentHourLabel" -> formatHour(ctx.hourOfDay)
        "sessionSongNumber" -> "#${ctx.totalSongsStarted}"
        "recentSkipCount" -> ctx.recentSkips.count { it }.toString()
        "totalSongsSkipped" -> ctx.totalSongsSkipped.toString()
        else -> "?"
    }

    private fun renderCount(
        sourceKey: String,
        singular: String,
        plural: String,
        ctx: SlotContext,
    ): String {
        val n = numericValue(sourceKey, ctx)
        val word = if (n == 1L) singular else plural
        return if (word.isBlank()) n.toString() else "$n $word"
    }

    private fun numericValue(key: String, ctx: SlotContext): Long = when (key) {
        "currentLoopCount" -> ctx.currentLoopCount.toLong()
        "currentMinutesListened" -> ctx.currentMinutesListened.toLong()
        "totalMinutesListened" -> ctx.totalMinutesListened.toLong()
        "totalSongsSkipped" -> ctx.totalSongsSkipped.toLong()
        "totalFavorites" -> ctx.totalFavorites.toLong()
        "totalLyricsOpens" -> ctx.totalLyricsOpens.toLong()
        "uniqueSongsListened" -> ctx.uniqueSongsListened.toLong()
        "recentSkipsCount" -> ctx.recentSkips.count { it }.toLong()
        else -> 0L
    }

    // ── Time / duration formatters ─────────────────────────────────

    private fun formatMinutes(m: Float): String = when {
        m <= 0f -> "0m"
        m < 1f -> "${(m * 60).toInt()}s"
        m < 60f -> "${m.toInt()}m"
        else -> {
            val total = m.toInt()
            val h = total / 60
            val rem = total % 60
            if (rem == 0) "${h}h" else "${h}h ${rem}m"
        }
    }

    private fun formatHour(h: Int): String {
        val safe = ((h % 24) + 24) % 24
        return when {
            safe == 0 -> "12am"
            safe < 12 -> "${safe}am"
            safe == 12 -> "12pm"
            else -> "${safe - 12}pm"
        }
    }
}
