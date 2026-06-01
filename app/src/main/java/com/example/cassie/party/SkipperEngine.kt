package com.example.cassie.party

import android.app.Application
import android.content.Context
import com.example.cassie.data.media.PersistenceManager
import com.example.cassie.party.grammar.SlotContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.Calendar

/**
 * The top-level orchestrator for Skipper.
 *
 * Wires three layers together:
 *  1. [UserEventStream]      — every behavior signal in the app
 *  2. [PatternRecognizer]    — events → [BehaviorStats] → [UserPattern]s
 *  3. [LineGenerator]        — current state → [SkipperLine]
 *
 * Responsibilities:
 *  - Subscribe to the event stream and fold every event into stats.
 *  - Persist stats via [UserPatternStore] (throttled).
 *  - Generate a new line when something interesting happens
 *    (throttled to one line per [LINE_COOLDOWN_MS] ms to avoid spam).
 *  - Expose live state to the UI: current patterns, recent events,
 *    and the line Skipper is currently saying.
 *
 * This is a process-wide singleton; skip DI for v1.
 */
object SkipperEngine {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    private lateinit var store: UserPatternStore
    private lateinit var recognizer: PatternRecognizer
    private lateinit var minutesTracker: MinutesListenedTracker
    private lateinit var lineGenerator: LineGenerator

    private var stats: BehaviorStats = BehaviorStats()
    private var lastSaveAt: Long = 0L
    private var lastLineAt: Long = 0L
    private var lastSignature: String = "" // pattern-types-sorted for change detection

    private val _currentPatterns = MutableStateFlow<List<UserPattern>>(emptyList())
    val currentPatterns: StateFlow<List<UserPattern>> = _currentPatterns.asStateFlow()

    private val _recentEvents = MutableStateFlow<List<UserEvent>>(emptyList())
    /** Most-recent 30 events, newest first. */
    val recentEvents: StateFlow<List<UserEvent>> = _recentEvents.asStateFlow()

    private val _currentLine = MutableStateFlow<SkipperLine?>(null)
    /**
     * The line Skipper is currently "saying". Null until the first
     * line is generated (which happens on init, with the initial
     * patterns detected).
     */
    val currentLine: StateFlow<SkipperLine?> = _currentLine.asStateFlow()

    /** Source of truth for whether party mode is active. */
    @Volatile var isPartyMode: Boolean = false
        private set

    @Volatile private var initialized = false

    fun init(context: Context) {
        if (initialized) return
        synchronized(this) {
            if (initialized) return
            val app = context.applicationContext as Application
            store = UserPatternStore(PersistenceManager(app))
            recognizer = PatternRecognizer()
            minutesTracker = MinutesListenedTracker()
            lineGenerator = LineGenerator()
            stats = store.load()
            val initialPatterns = recognizer.detect(stats)
            _currentPatterns.value = initialPatterns
            lastSignature = signatureOf(initialPatterns)
            initialized = true

            // Kick off a first line so the card isn't blank on open.
            regenerateLine(force = true, reason = "init")

            scope.launch {
                UserEventStream.events.collect { event ->
                    handleEvent(event)
                }
            }
        }
    }

    private fun handleEvent(event: UserEvent) {
        // 1. ring buffer of recent events (for debug UI)
        val updated = (listOf(event) + _recentEvents.value).take(30)
        _recentEvents.value = updated

        // 2. side-effects on the minutes tracker
        when (event) {
            is UserEvent.SongStarted -> {
                minutesTracker.onSongStarted(event.songId)
            }
            is UserEvent.SongPaused,
            is UserEvent.AppBackgrounded -> {
                minutesTracker.onPlaybackStopped()
            }
            is UserEvent.SongSkipped,
            is UserEvent.SongCompleted -> {
                minutesTracker.onSongEnded()
            }
            else -> { /* no tracker side-effect */ }
        }

        // 3. party-mode flag tracking (for the SlotContext)
        if (event is UserEvent.PartyModeToggled) {
            isPartyMode = event.enabled
        }

        // 4. fold into running stats
        recognizer.applyEvent(stats, event)

        // 5. re-detect patterns
        val newPatterns = recognizer.detect(stats)
        _currentPatterns.value = newPatterns

        // 6. throttled persist
        val now = System.currentTimeMillis()
        if (now - lastSaveAt > 10_000L) {
            store.save(stats)
            lastSaveAt = now
        }

        // 7. maybe regenerate a line
        val newSig = signatureOf(newPatterns)
        val sigChanged = newSig != lastSignature
        if (sigChanged) lastSignature = newSig
        if (shouldRegenerateOn(event, sigChanged, now)) {
            regenerateLine(force = false, reason = "event:${event::class.simpleName}")
        }
    }

    private fun shouldRegenerateOn(
        event: UserEvent,
        signatureChanged: Boolean,
        now: Long,
    ): Boolean {
        // Cool down is the strongest gate — no more than one new
        // line every few seconds even during a skip spree.
        if (now - lastLineAt < LINE_COOLDOWN_MS) return false
        // Pattern signature changes always justify a new line.
        if (signatureChanged) return true
        // First song of a session is worth commenting on.
        if (event is UserEvent.SongStarted &&
            stats.totalSongsStarted in 1..3
        ) return true
        // Looping is newsworthy.
        if (event is UserEvent.SongLooped) return true
        // Party mode toggled is newsworthy.
        if (event is UserEvent.PartyModeToggled && event.enabled) return true
        // Sleep timer set is newsworthy.
        if (event is UserEvent.SleepTimerSet) return true
        return false
    }

    /**
     * Force a fresh line (e.g. when the user taps the card). Bypasses
     * the cooldown and the pattern-change requirement.
     */
    fun refreshLine() {
        if (!initialized) return
        regenerateLine(force = true, reason = "user_tap")
    }

    private fun regenerateLine(force: Boolean, reason: String) {
        val ctx = buildSlotContext()
        // If we're forcing a refresh, we want a different line than
        // the last one. The LineGenerator's anti-repetition layer
        // handles the hard-dup case; for force, we just ask again and
        // trust the randomness + anti-rep.
        val line = lineGenerator.generate(
            ctx = ctx,
            triggerTags = listOf(reason),
        )
        if (!force && line.text == _currentLine.value?.text) {
            // No change — don't bump the cooldown or the displayed
            // timestamp; this was a "nothing interesting changed" event.
            return
        }
        _currentLine.value = line
        lastLineAt = System.currentTimeMillis()
    }

    private fun buildSlotContext(): SlotContext {
        val topSongId = stats.topReplaySongId
        val topMinutes = topSongId?.let { stats.minutesPerSong[it] } ?: 0
        val topShare = if (stats.totalMinutesListened > 0)
            topMinutes.toFloat() / stats.totalMinutesListened else 0f
        val now = System.currentTimeMillis()
        return SlotContext(
            currentLoopCount = stats.currentSongLoopCount,
            currentMinutesListened = stats.currentSongId?.let {
                stats.minutesPerSong[it] ?: 0
            } ?: 0,
            totalMinutesListened = stats.totalMinutesListened,
            totalSongsSkipped = stats.totalSongsSkipped,
            totalSongsCompleted = stats.totalSongsCompleted,
            totalFavorites = stats.totalFavoriteToggles,
            totalLyricsOpens = stats.totalLyricsOpens,
            uniqueSongsListened = stats.uniqueSongsListenedTo,
            sessionMinutes = ((now - stats.currentSessionStartMs) / 60_000L).toInt(),
            hourOfDay = Calendar.getInstance().get(Calendar.HOUR_OF_DAY),
            isPartyMode = isPartyMode,
            recentSkipRate = stats.recentSkipRate,
            recentSkips = stats.recentSkips,
            activePatterns = _currentPatterns.value,
            topMinutesShare = topShare,
        )
    }

    private fun signatureOf(patterns: List<UserPattern>): String =
        patterns.sortedBy { it.type.name }.joinToString(",") { it.type.name }

    /**
     * Wipe all stored behavior stats. Used by the debug card's reset
     * button. Does not clear the recent events ring (that's debug).
     */
    fun resetStats() {
        stats = BehaviorStats()
        _currentPatterns.value = recognizer.detect(stats)
        store.save(stats)
        lastSignature = signatureOf(_currentPatterns.value)
        regenerateLine(force = true, reason = "reset")
    }

    /** Force a final save (debug helpers, lifecycle hooks). */
    fun flush() {
        if (initialized) store.save(stats)
    }

    private const val LINE_COOLDOWN_MS = 3_000L
}
