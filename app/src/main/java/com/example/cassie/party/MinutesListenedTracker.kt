package com.example.cassie.party

import android.os.Handler
import android.os.Looper

/**
 * Emits a [UserEvent.MinutesListenedTicked] once per minute for the
 * currently playing song.
 *
 * The previous per-click "play count" in [com.example.cassie.data.media.ListeningCounter]
 * is broken — it conflates "user touched the song" with "user actually
 * listened to the song". This tracker is the real engagement signal:
 * one minute of continuous playback = one real unit of listening.
 *
 * Lifecycle:
 *  - [onSongStarted] starts a 60-second tick cycle for [songId].
 *  - [onPlaybackStopped] cancels the cycle (pause, app backgrounded).
 *  - [onSongEnded] also cancels (skip, song completed).
 *
 * The tracker is deliberately stateful and Handler-based. Coroutines
 * would be cleaner, but skipping the lifecycle plumbing keeps the v1
 * footprint small and the failure modes obvious.
 */
class MinutesListenedTracker {

    private val handler = Handler(Looper.getMainLooper())
    private var tickRunnable: Runnable? = null
    private var currentSongId: Long? = null

    fun onSongStarted(songId: Long) {
        cancel()
        currentSongId = songId
        scheduleNextTick()
    }

    fun onPlaybackStopped() {
        // Pause / app background / lock screen. We don't tick while
        // music isn't actually playing.
        cancel()
    }

    fun onSongEnded() {
        cancel()
    }

    private fun scheduleNextTick() {
        val songId = currentSongId ?: return
        val r = object : Runnable {
            override fun run() {
                UserEventStream.emit(
                    UserEvent.MinutesListenedTicked(
                        timestamp = System.currentTimeMillis(),
                        sessionId = UserEventStream.currentSessionId,
                        songId = songId,
                    )
                )
                if (currentSongId == songId) scheduleNextTick()
            }
        }
        tickRunnable = r
        handler.postDelayed(r, TICK_INTERVAL_MS)
    }

    private fun cancel() {
        tickRunnable?.let { handler.removeCallbacks(it) }
        tickRunnable = null
    }

    companion object {
        // 1 second — we accumulate fractional minutes in the
        // recognizer (1/60 per tick). Previously 60_000L meant a
        // 30-second skip registered 0 minutes. Now a 30s skip
        // registers 0.5m.
        const val TICK_INTERVAL_MS = 1_000L
    }
}
