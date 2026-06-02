package com.example.cassie.data.media

import android.app.Application
import android.content.ComponentName
import android.content.Intent
import android.net.Uri
import android.os.Handler
import android.os.Looper
import androidx.compose.runtime.Stable
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.example.cassie.party.StartSource
import com.example.cassie.party.UserEvent
import com.example.cassie.party.UserEventStream
import com.google.common.util.concurrent.FutureCallback
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.MoreExecutors
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

@Stable
data class PlayerState(
    val isPlaying: Boolean = false,
    val currentSong: Song? = null,
    val currentPosition: Long = 0,
    val duration: Long = 0,
    val queue: List<Song> = emptyList(),
    val shuffleMode: Boolean = false,
    val repeatMode: Int = Player.REPEAT_MODE_OFF,
    val sleepTimerRemainingSec: Int = 0,
    val audioSessionId: Int = -1,
    val partyMode: Boolean = false,
    /**
     * Lifetime seconds of real playback per song. Updated by a 1Hz
     * coroutine loop while playing — the "for loop counting" the user
     * asked for. Kept in PlayerState even though the current UI uses
     * play counts (ListeningCounter) for Vibe/Top 50 — the user wants
     * this data still ticking in the background for future use.
     */
    val listenedTimeSecBySong: Map<Long, Long> = emptyMap(),
)

class PlaybackManager(app: Application) : AndroidViewModel(app) {

    private val _playerState = MutableStateFlow(PlayerState(listenedTimeSecBySong = loadTimeTracker()))
    val playerState: StateFlow<PlayerState> = _playerState.asStateFlow()

    val listeningCounter = ListeningCounter(PersistenceManager(getApplication()))

    private var controller: MediaController? = null
    private var currentSongIndex: Int = -1

    // ── direct time-per-song tracker ────────────────────────────────
    // A 1Hz coroutine loop that ticks while the player is actively
    // playing, adding one second of listening time to the current
    // song. The user explicitly asked for a "for loop counting each
    // song" — this is it. Bypasses the event-based minutes tracker
    // in SkipperEngine for the simple case (UI stats) and remains
    // accurate even if the event stream drops a tick.
    private var listenedTimeSecBySong: Map<Long, Long> = loadTimeTracker()
    private var tickingSongId: Long? = null
    private var tickingShouldRun: Boolean = false

    // ── previous-song tracking (for skip vs complete detection) ──
    private var prevSongId: Long? = null
    private var prevSongStartedAt: Long = 0L
    private var prevSongDurationMs: Long = 0L
    private var lastEmittedPlayingState: Boolean = false

    // ── Media3 transition reason constants (stable ints across versions) ──
    // We use raw ints instead of Player.MEDIA_ITEM_TRANSITION_REASON_*
    // because the static-field resolution from Kotlin varies across
    // Media3 minor versions. The values themselves are stable.
    private companion object {
        const val TRANSITION_REASON_AUTO = 0
        const val TRANSITION_REASON_USER = 3
    }

    /** Queue of actions that execute once the controller connects. */
    private val pendingActions = mutableListOf<((MediaController) -> Unit)>()
    private val pm = PersistenceManager(getApplication())
    private val persistenceManager: PersistenceManager get() = pm

    /** Execute action immediately if controller is ready, otherwise queue it. */
    private fun withController(action: (MediaController) -> Unit) {
        val c = controller
        if (c != null) {
            action(c)
        } else {
            pendingActions.add(action)
        }
    }

    private fun flushPendingActions() {
        val c = controller ?: return
        val copy = pendingActions.toList()
        pendingActions.clear()
        copy.forEach { it(c) }
    }

    // ── sleep timer ─────────────────────────────────────────────────
    private var sleepHandler: Handler? = null
    private var sleepRunnable: Runnable? = null

    // ── service + controller initialization ──────────────────────────

    init {
        val ctx = getApplication<Application>()
        // Restore last played song so MiniPlayer shows immediately on reopen
        restoreLastSong()
        // Start the foreground service for background playback
        ctx.startForegroundService(Intent(ctx, CassiePlaybackService::class.java))
        // Connect to the service's MediaSession asynchronously
        connectToController(0)

        // ── 1Hz time-per-song loop ─────────────────────────────────
        // Runs while tickingShouldRun is true. Each second that the
        // song is the same as the last tick, add 1 second to its
        // lifetime listening time. Stops automatically when the
        // song changes or playback pauses.
        //
        // Persistence: save on EVERY tick (not throttled). SharedPreferences
        // apply() is async and cheap, and the user reported the per-minute
        // Top 50 was "resets when the app is closed" — that's unacceptable
        // for a "critical" stat. Saving every second guarantees that at
        // most 1 second of listening time is lost on a force-kill.
        // Also flushes on pause for the same reason.
        viewModelScope.launch {
            while (isActive) {
                delay(1_000L)
                if (tickingShouldRun) {
                    val songId = tickingSongId
                    if (songId != null) {
                        val prev = listenedTimeSecBySong[songId] ?: 0L
                        listenedTimeSecBySong = listenedTimeSecBySong + (songId to (prev + 1L))
                        _playerState.update { it.copy(listenedTimeSecBySong = listenedTimeSecBySong) }
                        // Persist on every tick — apply() is async so
                        // this doesn't block the coroutine.
                        saveTimeTracker()
                    }
                } else if (listenedTimeSecBySong.isNotEmpty()) {
                    // Ticker paused — flush to disk so a force-close
                    // or OOM kill doesn't lose any in-flight time.
                    saveTimeTracker()
                }
            }
        }
    }

    /**
     * Persist the current [listenedTimeSecBySong] map to SharedPreferences
     * so the per-minute Top 50 survives app restarts. Storage key
     * `listened_time_v1` in the `cassie_data` prefs file.
     */
    private fun saveTimeTracker() {
        val pm = persistenceManager ?: return
        val obj = org.json.JSONObject()
        for ((id, sec) in listenedTimeSecBySong) {
            obj.put(id.toString(), sec)
        }
        pm.putString("listened_time_v1", obj.toString())
    }

    /**
     * Load [listenedTimeSecBySong] from SharedPreferences. Called from
     * init so the per-minute Top 50 still has data after the user
     * closes and reopens the app.
     */
    private fun loadTimeTracker(): Map<Long, Long> {
        val raw = persistenceManager?.getString("listened_time_v1") ?: return emptyMap()
        return try {
            val obj = org.json.JSONObject(raw)
            val map = mutableMapOf<Long, Long>()
            obj.keys().forEach { key ->
                val sec = obj.optLong(key, 0L)
                if (sec > 0L) map[key.toLong()] = sec
            }
            map
        } catch (_: Exception) { emptyMap() }
    }

    /** Persist current song so it survives app restarts */
    private fun saveLastSong(song: Song) {
        try {
            val json = org.json.JSONObject()
            json.put("id", song.id)
            json.put("title", song.title)
            json.put("artist", song.artist)
            json.put("album", song.album)
            json.put("albumId", song.albumId)
            json.put("duration", song.duration)
            json.put("dateAdded", song.dateAdded)
            json.put("mimeType", song.mimeType)
            json.put("albumArtUri", song.albumArtUri ?: "")
            pm.putString("last_song", json.toString())
        } catch (_: Exception) {}
    }

    /** Load last played song into playerState immediately */
    private fun restoreLastSong() {
        try {
            val raw = pm.getString("last_song") ?: return
            val json = org.json.JSONObject(raw)
            val song = Song(
                id = json.getLong("id"),
                title = json.getString("title"),
                artist = json.getString("artist"),
                album = json.getString("album"),
                albumId = json.getLong("albumId"),
                duration = json.getLong("duration"),
                dateAdded = json.getLong("dateAdded"),
                mimeType = json.getString("mimeType"),
                albumArtUri = json.optString("albumArtUri", "").ifEmpty { null },
            )
            _playerState.update { it.copy(currentSong = song) }
        } catch (_: Exception) {}
    }

    private fun connectToController(tryCount: Int = 0) {
        if (controller != null || tryCount > 5) return
        val ctx = getApplication<Application>()
        try {
            val token = SessionToken(ctx, ComponentName(ctx, CassiePlaybackService::class.java))
            val future = MediaController.Builder(ctx, token).buildAsync()
            Futures.addCallback(
                future,
                object : FutureCallback<MediaController> {
                    override fun onSuccess(result: MediaController) {
                        controller = result
                        result.addListener(controllerListener)
                        syncStateFromController(result)
                        flushPendingActions()
                    }

                    override fun onFailure(t: Throwable) {
                        t.printStackTrace()
                        Handler(Looper.getMainLooper()).postDelayed({
                            connectToController(tryCount + 1)
                        }, 500)
                    }
                },
                MoreExecutors.directExecutor()
            )
        } catch (e: Exception) {
            e.printStackTrace()
            Handler(Looper.getMainLooper()).postDelayed({
                connectToController(tryCount + 1)
            }, 500)
        }
    }

    private val controllerListener = object : Player.Listener {
        override fun onIsPlayingChanged(isPlaying: Boolean) {
            _playerState.update { it.copy(isPlaying = isPlaying) }
            // Drive the direct time-per-song tracker. Resume ticking
            // on play; stop on pause. The song id itself is set in
            // onMediaItemTransition, so we only flip the run flag here.
            tickingShouldRun = isPlaying
            // Translate playing-state edges into Skipper pause/resume events.
            val c = controller ?: return
            val song = _playerState.value.currentSong
            if (isPlaying && !lastEmittedPlayingState && song != null) {
                UserEventStream.emit(
                    UserEvent.SongResumed(
                        timestamp = System.currentTimeMillis(),
                        sessionId = UserEventStream.currentSessionId,
                        songId = song.id,
                    )
                )
            } else if (!isPlaying && lastEmittedPlayingState && song != null) {
                UserEventStream.emit(
                    UserEvent.SongPaused(
                        timestamp = System.currentTimeMillis(),
                        sessionId = UserEventStream.currentSessionId,
                        songId = song.id,
                        positionMs = c.currentPosition,
                    )
                )
            }
            lastEmittedPlayingState = isPlaying
        }

        override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
            val c = controller ?: return
            val now = System.currentTimeMillis()
            val idx = c.currentMediaItemIndex
            currentSongIndex = idx
            val song = _playerState.value.queue.getOrNull(idx)

            // ── 1. emit the OUTGOING song's terminal event ───────────
            val outgoingId = prevSongId
            val outgoingDuration = prevSongDurationMs
            if (outgoingId != null && song != null && outgoingId != song.id) {
                val outgoingPosition = (now - prevSongStartedAt).coerceAtLeast(0L)
                when (reason) {
                    TRANSITION_REASON_AUTO -> {
                        // Natural transition — previous song completed.
                        UserEventStream.emit(
                            UserEvent.SongCompleted(
                                timestamp = now,
                                sessionId = UserEventStream.currentSessionId,
                                songId = outgoingId,
                            )
                        )
                    }
                    TRANSITION_REASON_USER -> {
                        // User pressed next/previous — treat as a skip
                        // (whether they wanted to hear the next song or
                        // went back to the previous one, the previous
                        // song's playback ended by user action).
                        UserEventStream.emit(
                            UserEvent.SongSkipped(
                                timestamp = now,
                                sessionId = UserEventStream.currentSessionId,
                                songId = outgoingId,
                                positionMs = outgoingPosition,
                                songDurationMs = outgoingDuration,
                            )
                        )
                    }
                    else -> {
                        // PLAYLIST_CHANGED / SEEK — ambiguous. If we
                        // spent <80% of the song, treat as a skip;
                        // otherwise let it fall through silently (the
                        // recognizer doesn't punish queue edits).
                        if (outgoingDuration > 0 &&
                            outgoingPosition < (outgoingDuration * 0.8f).toLong()
                        ) {
                            UserEventStream.emit(
                                UserEvent.SongSkipped(
                                    timestamp = now,
                                    sessionId = UserEventStream.currentSessionId,
                                    songId = outgoingId,
                                    positionMs = outgoingPosition,
                                    songDurationMs = outgoingDuration,
                                )
                            )
                        }
                    }
                }
            }

            // ── 2. emit the INCOMING song's start event ──────────────
            if (idx >= 0 && song != null) {
                val source = when (reason) {
                    TRANSITION_REASON_AUTO -> StartSource.AUTO_ADVANCE
                    TRANSITION_REASON_USER -> StartSource.MANUAL_PICK
                    else -> StartSource.MANUAL_PICK
                }
                UserEventStream.emit(
                    UserEvent.SongStarted(
                        timestamp = now,
                        sessionId = UserEventStream.currentSessionId,
                        songId = song.id,
                        title = song.title,
                        artist = song.artist,
                        durationMs = song.duration,
                        source = source,
                    )
                )
                // Remember this song so the NEXT transition can describe it.
                prevSongId = song.id
                prevSongStartedAt = now
                prevSongDurationMs = song.duration
                // Drive the direct time-per-song tracker: switch the
                // tick target to the new song. The loop will start
                // counting on the next tick if tickingShouldRun is on.
                tickingSongId = song.id
            }

            // ── 3. existing player-state update (unchanged) ─────────
            if (idx >= 0) {
                _playerState.update {
                    it.copy(
                        currentSong = song,
                        duration = c.duration,
                        currentPosition = 0,
                    )
                }
                if (song != null) {
                    listeningCounter.recordPlay(song.id)
                    saveLastSong(song)
                }
                val sid = CassiePlaybackService.getPlayer()?.audioSessionId ?: -1
                if (sid != _playerState.value.audioSessionId) {
                    _playerState.update { it.copy(audioSessionId = sid) }
                }
            }
        }

        /**
         * Detects repeat-one loops. When the player is in REPEAT_MODE_ONE
         * and the position resets to ~0 because of an automatic
         * transition, that's a "song looped" event.
         */
        override fun onPositionDiscontinuity(
            oldPosition: Player.PositionInfo,
            newPosition: Player.PositionInfo,
            reason: Int,
        ) {
            if (reason == Player.DISCONTINUITY_REASON_AUTO_TRANSITION) {
                val c = controller ?: return
                if (c.repeatMode == Player.REPEAT_MODE_ONE) {
                    val song = _playerState.value.currentSong ?: return
                    UserEventStream.emit(
                        UserEvent.SongLooped(
                            timestamp = System.currentTimeMillis(),
                            sessionId = UserEventStream.currentSessionId,
                            songId = song.id,
                        )
                    )
                    // The "new" song is the same song — keep prevSongId
                    // pointing at it but reset the start timestamp so
                    // the next loop's elapsed time is correct.
                    prevSongStartedAt = System.currentTimeMillis()
                }
            }
        }

        override fun onPlaybackStateChanged(state: Int) {
            if (state == Player.STATE_READY) {
                _playerState.update { it.copy(duration = controller?.duration ?: 0) }
            }
            // Stop the time-per-song tick when playback truly stops
            // (end of queue, error, etc.) — onIsPlayingChanged already
            // handles pause/resume, but a STATE_IDLE/STATE_ENDED edge
            // should also halt the tick.
            if (state == Player.STATE_IDLE || state == Player.STATE_ENDED) {
                tickingShouldRun = false
                // Flush any in-flight listening time to disk NOW.
                // The 1Hz loop also flushes in its else-branch, but
                // by the time the loop's next tick runs, the
                // viewModelScope may already be cancelled (this
                // callback can fire on service destruction). Saving
                // here is the "last chance" to persist before the
                // process dies.
                saveTimeTracker()
            }
        }

        override fun onShuffleModeEnabledChanged(enabled: Boolean) {
            _playerState.update { it.copy(shuffleMode = enabled) }
            UserEventStream.emit(
                UserEvent.ShuffleToggled(
                    timestamp = System.currentTimeMillis(),
                    sessionId = UserEventStream.currentSessionId,
                    enabled = enabled,
                )
            )
        }

        override fun onRepeatModeChanged(mode: Int) {
            _playerState.update { it.copy(repeatMode = mode) }
            UserEventStream.emit(
                UserEvent.RepeatModeChanged(
                    timestamp = System.currentTimeMillis(),
                    sessionId = UserEventStream.currentSessionId,
                    mode = mode,
                )
            )
        }
    }

    private fun syncStateFromController(c: MediaController) {
        _playerState.update {
            it.copy(
                isPlaying = c.isPlaying,
                duration = c.duration,
                repeatMode = c.repeatMode,
                shuffleMode = c.shuffleModeEnabled,
                currentPosition = c.currentPosition,
                currentSong = _playerState.value.currentSong,
            )
        }
        val idx = c.currentMediaItemIndex
        if (idx >= 0 && idx < _playerState.value.queue.size) {
            currentSongIndex = idx
            _playerState.update { it.copy(currentSong = _playerState.value.queue[idx]) }
        }
        // Grab the audio session ID from the service
        val sid = CassiePlaybackService.getPlayer()?.audioSessionId ?: -1
        if (sid != _playerState.value.audioSessionId) {
            _playerState.update { it.copy(audioSessionId = sid) }
        }
    }

    // ── public API ───────────────────────────────────────────────────

    fun getCurrentPosition(): Long = controller?.currentPosition ?: 0

    fun getAudioSessionId(): Int = _playerState.value.audioSessionId

    /** Play a single song (queue = that song only — skip/next won't work). */
    fun play(song: Song) {
        playInContext(song, listOf(song))
    }

    /**
     * Play a song within its context list.
     * Queue = the full [context] list starting at [song], so skip/next/shuffle
     * work naturally because there are more items in the queue.
     */
    fun playInContext(song: Song, context: List<Song>) {
        val index = context.indexOfFirst { it.id == song.id }.coerceAtLeast(0)
        playQueue(context, index)
    }

    fun playQueue(songs: List<Song>, startIndex: Int = 0) {
        val mediaItems = songs.map { buildMediaItem(it) }
        withController { c ->
            c.setMediaItems(mediaItems, startIndex, 0)
            c.prepare()
            c.play()
        }
        _playerState.update {
            it.copy(queue = songs, shuffleMode = false)
        }
        currentSongIndex = startIndex
        if (startIndex in songs.indices) {
            val song = songs[startIndex]
            _playerState.update { it.copy(currentSong = song) }
            saveLastSong(song)
            // Prime the 1Hz time tracker with the starting song.
            // onMediaItemTransition doesn't always fire for the FIRST
            // item in a freshly-set queue (it fires on TRANSITIONS,
            // and the very first song is just "appearing", not
            // "transitioning from another"). Without this prime,
            // the first song in a queue would get 0 seconds even
            // if the user listened to it for an hour — and a later
            // song they only previewed for 10 seconds would leap
            // past it in the Top 50 ranking. This bug is what the
            // user flagged as "per minute top 50 isn't accurate".
            tickingSongId = song.id
        }
        listeningCounter.recordPlay(songs.getOrNull(startIndex)?.id ?: -1)
    }

    /**
     * Play the entire given song list in shuffle mode. Used by the
     * "Shuffle All" button on Home — gives the user a single tap
     * path to "start the music and surprise me". The songs list is
     * the in-memory library, not a re-fetched one, so the user
     * sees what they tapped on.
     */
    fun playAllShuffled(songs: List<Song>) {
        if (songs.isEmpty()) return
        // Shuffle deterministically each tap so two consecutive
        // presses of "Shuffle All" feel different.
        val shuffled = songs.shuffled()
        val mediaItems = shuffled.map { buildMediaItem(it) }
        withController { c ->
            c.setMediaItems(mediaItems, 0, 0)
            c.setShuffleModeEnabled(true)
            c.repeatMode = Player.REPEAT_MODE_ALL
            c.prepare()
            c.play()
        }
        _playerState.update {
            it.copy(
                queue = shuffled,
                shuffleMode = true,
                repeatMode = Player.REPEAT_MODE_ALL,
            )
        }
        currentSongIndex = 0
        val firstSong = shuffled[0]
        _playerState.update { it.copy(currentSong = firstSong) }
        saveLastSong(firstSong)
        // Prime the 1Hz time tracker (same fix as playQueue — see
        // comment there for why this is needed).
        tickingSongId = firstSong.id
        listeningCounter.recordPlay(firstSong.id)
        UserEventStream.emit(
            UserEvent.PartyModeToggled(
                timestamp = System.currentTimeMillis(),
                sessionId = UserEventStream.currentSessionId,
                enabled = true,
            )
        )
    }

    fun togglePlayPause() {
        withController {
            if (it.isPlaying) it.pause() else it.play()
        }
    }

    fun seekTo(position: Long) {
        val c = controller
        val fromMs = c?.currentPosition ?: _playerState.value.currentPosition
        val songId = _playerState.value.currentSong?.id
        withController { it.seekTo(position) }
        _playerState.update { it.copy(currentPosition = position) }
        if (songId != null) {
            UserEventStream.emit(
                UserEvent.SongSeeked(
                    timestamp = System.currentTimeMillis(),
                    sessionId = UserEventStream.currentSessionId,
                    songId = songId,
                    fromMs = fromMs,
                    toMs = position,
                )
            )
        }
    }

    fun skipToNext() {
        withController { it.seekToNextMediaItem() }
    }

    fun skipToPrevious() {
        withController { it.seekToPreviousMediaItem() }
        val song = _playerState.value.currentSong
        if (song != null) {
            UserEventStream.emit(
                UserEvent.SongReplayed(
                    timestamp = System.currentTimeMillis(),
                    sessionId = UserEventStream.currentSessionId,
                    songId = song.id,
                )
            )
        }
    }

    fun toggleShuffle() {
        withController { c ->
            val newState = !c.shuffleModeEnabled
            c.setShuffleModeEnabled(newState)
            _playerState.update { it.copy(shuffleMode = newState) }
        }
    }

    fun cycleRepeat() {
        withController { c ->
            val next = when (c.repeatMode) {
                Player.REPEAT_MODE_OFF -> Player.REPEAT_MODE_ONE
                Player.REPEAT_MODE_ONE -> Player.REPEAT_MODE_ALL
                else -> Player.REPEAT_MODE_OFF
            }
            c.repeatMode = next
            _playerState.update { it.copy(repeatMode = next) }
        }
    }

    // ── sleep timer ─────────────────────────────────────────────────

    fun setSleepTimer(minutes: Int) {
        cancelSleepTimer()
        val sec = minutes * 60
        UserEventStream.emit(
            UserEvent.SleepTimerSet(
                timestamp = System.currentTimeMillis(),
                sessionId = UserEventStream.currentSessionId,
                minutes = minutes,
            )
        )
        _playerState.update { it.copy(sleepTimerRemainingSec = sec) }
        sleepHandler = Handler(Looper.getMainLooper())
        sleepRunnable = object : Runnable {
            var remaining = sec
            override fun run() {
                remaining -= 1
                if (remaining <= 0) {
                    withController { it.pause() }
                    _playerState.update { it.copy(sleepTimerRemainingSec = 0) }
                } else {
                    _playerState.update { it.copy(sleepTimerRemainingSec = remaining) }
                    sleepHandler?.postDelayed(this, 1000)
                }
            }
        }.also { sleepHandler?.postDelayed(it, 1000) }
    }

    fun cancelSleepTimer() {
        sleepRunnable?.let { sleepHandler?.removeCallbacks(it) }
        sleepHandler = null
        sleepRunnable = null
        _playerState.update { it.copy(sleepTimerRemainingSec = 0) }
    }

    // ── party mode ────────────────────────────────────────────────────
    fun togglePartyMode() {
        val newState = !_playerState.value.partyMode
        _playerState.update { it.copy(partyMode = newState) }
        UserEventStream.emit(
            UserEvent.PartyModeToggled(
                timestamp = System.currentTimeMillis(),
                sessionId = UserEventStream.currentSessionId,
                enabled = newState,
            )
        )
        if (newState) {
            // PARTY HARD: shuffle + repeat all + max energy
            withController { c ->
                c.setShuffleModeEnabled(true)
                c.repeatMode = Player.REPEAT_MODE_ALL
            }
            _playerState.update {
                it.copy(shuffleMode = true, repeatMode = Player.REPEAT_MODE_ALL)
            }
        }
    }

    // ── queue management ─────────────────────────────────────────────

    fun removeFromQueue(index: Int) {
        val old = _playerState.value.queue
        if (index < 0 || index >= old.size) return
        val newQueue = old.toMutableList().apply { removeAt(index) }
        val mediaItems = newQueue.map { buildMediaItem(it) }
        withController { c ->
            c.setMediaItems(
                mediaItems,
                c.currentMediaItemIndex.coerceAtMost(mediaItems.size - 1).coerceAtLeast(0),
                0
            )
        }
        _playerState.update { it.copy(queue = newQueue) }
    }

    fun moveInQueue(fromIndex: Int, toIndex: Int) {
        val old = _playerState.value.queue
        if (fromIndex < 0 || fromIndex >= old.size || toIndex < 0 || toIndex >= old.size) return
        val newQueue = old.toMutableList().apply {
            val item = removeAt(fromIndex)
            add(toIndex, item)
        }
        val mediaItems = newQueue.map { buildMediaItem(it) }
        withController { c ->
            c.setMediaItems(
                mediaItems,
                c.currentMediaItemIndex.coerceAtMost(mediaItems.size - 1).coerceAtLeast(0),
                0
            )
        }
        _playerState.update { it.copy(queue = newQueue) }
    }

    private fun buildMediaItem(song: Song): MediaItem {
        return MediaItem.Builder()
            .setMediaId(song.id.toString())
            .setUri(Uri.parse("content://media/external/audio/media/${song.id}"))
            .setMediaMetadata(
                MediaMetadata.Builder()
                    .setTitle(song.title)
                    .setArtist(song.artist)
                    .setAlbumTitle(song.album)
                    .setArtworkUri(song.albumArtUri?.let { Uri.parse(it) })
                    .build()
            )
            .build()
    }

    override fun onCleared() {
        super.onCleared()
        cancelSleepTimer()
        controller?.release()
        controller = null
        // Note: service is NOT stopped here — it continues playing in background.
        // Service stops itself via onTaskRemoved when playback ends or user swipes away.
    }
}
