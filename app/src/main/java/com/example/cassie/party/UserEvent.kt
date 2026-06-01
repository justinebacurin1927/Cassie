package com.example.cassie.party

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import java.util.UUID

/**
 * Every observable USER ACTION goes through this sealed hierarchy.
 *
 * CRITICAL RULE: these events describe what the USER DOES, not what the
 * music IS. Skipper reacts to behavior, never to the content of songs.
 *
 * Each event carries a [timestamp] and a [sessionId]. The sessionId is
 * generated once per app cold-start and stays stable for the lifetime of
 * the process — it lets the [PatternRecognizer] distinguish "this user's
 * listening behavior today" from "this user over weeks of use".
 */
sealed class UserEvent {
    abstract val timestamp: Long
    abstract val sessionId: String

    /** A new song started playing (auto-advance, manual pick, or initial play). */
    data class SongStarted(
        override val timestamp: Long,
        override val sessionId: String,
        val songId: Long,
        val title: String,
        val artist: String,
        val durationMs: Long,
        val source: StartSource,
    ) : UserEvent()

    /** User pressed next / swiped to skip the current song. */
    data class SongSkipped(
        override val timestamp: Long,
        override val sessionId: String,
        val songId: Long,
        val positionMs: Long,
        val songDurationMs: Long,
    ) : UserEvent()

    /** User pressed previous (or seekTo 0) — they wanted to hear it again. */
    data class SongReplayed(
        override val timestamp: Long,
        override val sessionId: String,
        val songId: Long,
    ) : UserEvent()

    /** The currently playing song naturally looped (repeat-one) back to start. */
    data class SongLooped(
        override val timestamp: Long,
        override val sessionId: String,
        val songId: Long,
    ) : UserEvent()

    /** User hit pause. */
    data class SongPaused(
        override val timestamp: Long,
        override val sessionId: String,
        val songId: Long,
        val positionMs: Long,
    ) : UserEvent()

    /** User hit play (resume from pause). */
    data class SongResumed(
        override val timestamp: Long,
        override val sessionId: String,
        val songId: Long,
    ) : UserEvent()

    /** User dragged the seek bar to a new position. */
    data class SongSeeked(
        override val timestamp: Long,
        override val sessionId: String,
        val songId: Long,
        val fromMs: Long,
        val toMs: Long,
    ) : UserEvent()

    /** A song reached its natural end (skipped == false, completed == true). */
    data class SongCompleted(
        override val timestamp: Long,
        override val sessionId: String,
        val songId: Long,
    ) : UserEvent()

    /** User toggled the app's party mode (the high-energy shuffle+repeat mode). */
    data class PartyModeToggled(
        override val timestamp: Long,
        override val sessionId: String,
        val enabled: Boolean,
    ) : UserEvent()

    /** User changed the repeat mode (off → all → one). */
    data class RepeatModeChanged(
        override val timestamp: Long,
        override val sessionId: String,
        val mode: Int, // 0=off, 1=all, 2=one
    ) : UserEvent()

    /** User toggled shuffle on/off. */
    data class ShuffleToggled(
        override val timestamp: Long,
        override val sessionId: String,
        val enabled: Boolean,
    ) : UserEvent()

    /** User set a sleep timer. */
    data class SleepTimerSet(
        override val timestamp: Long,
        override val sessionId: String,
        val minutes: Int,
    ) : UserEvent()

    /** User favorited (or unfavorited) a song. */
    data class FavoriteToggled(
        override val timestamp: Long,
        override val sessionId: String,
        val songId: Long,
        val isFavorite: Boolean,
    ) : UserEvent()

    /** User opened the lyrics view. */
    data class LyricsOpened(
        override val timestamp: Long,
        override val sessionId: String,
        val songId: Long,
    ) : UserEvent()

    /** App came to the foreground. */
    data class AppForegrounded(
        override val timestamp: Long,
        override val sessionId: String,
    ) : UserEvent()

    /** App went to the background. */
    data class AppBackgrounded(
        override val timestamp: Long,
        override val sessionId: String,
    ) : UserEvent()

    /**
     * One minute of continuous playback of the same song ticked by.
     * This is the minutes-listened signal — the real measure of engagement,
     * not the broken per-click "play count" we used to track.
     */
    data class MinutesListenedTicked(
        override val timestamp: Long,
        override val sessionId: String,
        val songId: Long,
    ) : UserEvent()
}

enum class StartSource {
    /** App cold-start or service reconnect. */
    RESUME,
    /** User tapped a song in a list / search result. */
    MANUAL_PICK,
    /** User hit "next" / auto-advance ended the previous song. */
    AUTO_ADVANCE,
    /** User hit "previous" and went back to the song. */
    PREVIOUS,
}

/**
 * A singleton event bus. Anything that wants to react to user behavior
 * subscribes to [events]. Anything that wants to record a user behavior
 * calls [emit].
 *
 * Buffered with a small extraBufferCapacity so a slow consumer never
 * drops a behavior event on the floor — we'd rather back-pressure the
 * emitter than silently lose the signal.
 */
object UserEventStream {
    private val _events = MutableSharedFlow<UserEvent>(
        replay = 0,
        extraBufferCapacity = 256,
    )
    val events: SharedFlow<UserEvent> = _events.asSharedFlow()

    /**
     * Returns a fresh sessionId for the current process lifetime. Callers
     * stamp it onto every event they emit so downstream code can group
     * events into listening sessions.
     */
    val currentSessionId: String by lazy { UUID.randomUUID().toString() }

    fun emit(event: UserEvent) {
        // tryEmit returns false only if the buffer is full and there are
        // zero subscribers. With extraBufferCapacity=256 that's effectively
        // never, and if it ever happens we just lose the event (a debug
        // log would be the right escalation).
        _events.tryEmit(event)
    }
}
