package com.example.cassie.party

import android.app.Application
import android.content.Context
import com.example.cassie.data.media.PersistenceManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * The top-level orchestrator for the user-pattern-recognition layer
 * of Skipper (the Party Mode mascot).
 *
 * Responsibilities:
 *  1. Hold the in-memory [BehaviorStats] snapshot.
 *  2. Subscribe to [UserEventStream] and fold every event into the stats.
 *  3. Run [PatternRecognizer] after every event and publish the new
 *     [UserPattern] list to [currentPatterns].
 *  4. Persist stats periodically (and on shutdown) via [UserPatternStore].
 *  5. Own the [MinutesListenedTracker] so the playback layer doesn't
 *     have to know about the party package.
 *
 * What it does NOT do (yet — those are later sprints):
 *  - Generate any actual Skipper lines. That's Sprint 2.
 *  - Read song titles, artists, or genres. Forbidden signal.
 *
 * The engine is a process-wide singleton because every emitter and
 * every consumer lives in the same process. No DI framework needed.
 */
object SkipperEngine {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    private lateinit var store: UserPatternStore
    private lateinit var recognizer: PatternRecognizer
    private lateinit var minutesTracker: MinutesListenedTracker

    private var stats: BehaviorStats = BehaviorStats()
    private var lastSaveAt: Long = 0L

    private val _currentPatterns = MutableStateFlow<List<UserPattern>>(emptyList())
    val currentPatterns: StateFlow<List<UserPattern>> = _currentPatterns.asStateFlow()

    private val _recentEvents = MutableStateFlow<List<UserEvent>>(emptyList())
    /** Most-recent 30 events, newest first. Useful for the debug UI. */
    val recentEvents: StateFlow<List<UserEvent>> = _recentEvents.asStateFlow()

    @Volatile private var initialized = false

    /**
     * Wire up the engine. Safe to call multiple times — only the first
     * call has any effect. Must be called from the main thread (it
     * owns a Handler-bound [MinutesListenedTracker]).
     */
    fun init(context: Context) {
        if (initialized) return
        synchronized(this) {
            if (initialized) return
            val app = context.applicationContext as Application
            store = UserPatternStore(PersistenceManager(app))
            recognizer = PatternRecognizer()
            minutesTracker = MinutesListenedTracker()
            stats = store.load()
            // Re-evaluate patterns immediately so the UI shows real
            // data the moment it subscribes, not only after the first
            // event of the new session.
            _currentPatterns.value = recognizer.detect(stats)
            initialized = true

            scope.launch {
                UserEventStream.events.collect { event ->
                    handleEvent(event)
                }
            }
        }
    }

    private fun handleEvent(event: UserEvent) {
        // 1. Keep the recent-events ring buffer (newest first, max 30)
        val updated = (listOf(event) + _recentEvents.value).take(30)
        _recentEvents.value = updated

        // 2. Translate playback-lifecycle events into tracker commands
        when (event) {
            is UserEvent.SongStarted -> {
                minutesTracker.onSongStarted(event.songId)
            }
            is UserEvent.SongPaused,
            is UserEvent.AppBackgrounded -> {
                minutesTracker.onPlaybackStopped()
            }
            is UserEvent.SongResumed,
            is UserEvent.AppForegrounded -> {
                // We don't restart the minute-counter on resume;
                // a brief pause shouldn't reset the per-song minute count.
                // The SongStarted event is the only thing that (re)starts it.
            }
            is UserEvent.SongSkipped,
            is UserEvent.SongCompleted -> {
                minutesTracker.onSongEnded()
            }
            else -> { /* no tracker side-effect */ }
        }

        // 3. Fold the event into the running stats
        recognizer.applyEvent(stats, event)

        // 4. Re-detect patterns
        _currentPatterns.value = recognizer.detect(stats)

        // 5. Persist (throttled to once every 10 seconds to avoid
        // hammering SharedPreferences on a busy skipper)
        val now = System.currentTimeMillis()
        if (now - lastSaveAt > 10_000L) {
            store.save(stats)
            lastSaveAt = now
        }
    }

    // ── Test / debug helpers ───────────────────────────────────────

    /**
     * Wipe all stored behavior stats and re-detect patterns. Used by
     * the debug card's "reset" button. Does NOT affect the in-memory
     * event ring (that's debug info, not user data).
     */
    fun resetStats() {
        stats = BehaviorStats()
        _currentPatterns.value = recognizer.detect(stats)
        store.save(stats)
    }

    /**
     * Force an immediate save. Call from [Application.onTerminate] in
     * debug builds and from the service's onDestroy so we don't lose
     * the last few events of a session.
     */
    fun flush() {
        if (initialized) store.save(stats)
    }
}
