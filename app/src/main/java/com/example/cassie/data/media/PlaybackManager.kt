package com.example.cassie.data.media

import android.app.Application
import android.content.ComponentName
import android.content.Intent
import android.net.Uri
import android.os.Handler
import android.os.Looper
import androidx.compose.runtime.Stable
import androidx.lifecycle.AndroidViewModel
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.google.common.util.concurrent.FutureCallback
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.MoreExecutors
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

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
)

class PlaybackManager(app: Application) : AndroidViewModel(app) {

    private val _playerState = MutableStateFlow(PlayerState())
    val playerState: StateFlow<PlayerState> = _playerState.asStateFlow()

    val listeningCounter = ListeningCounter(PersistenceManager(getApplication()))

    private var controller: MediaController? = null
    private var currentSongIndex: Int = -1

    /** Queue of actions that execute once the controller connects. */
    private val pendingActions = mutableListOf<((MediaController) -> Unit)>()
    private val pm = PersistenceManager(getApplication())

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
        }

        override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
            val c = controller ?: return
            val idx = c.currentMediaItemIndex
            currentSongIndex = idx
            val song = _playerState.value.queue.getOrNull(idx)
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
                // Refresh audioSessionId for equalizer
                val sid = CassiePlaybackService.getPlayer()?.audioSessionId ?: -1
                if (sid != _playerState.value.audioSessionId) {
                    _playerState.update { it.copy(audioSessionId = sid) }
                }
            }
        }

        override fun onPlaybackStateChanged(state: Int) {
            if (state == Player.STATE_READY) {
                _playerState.update { it.copy(duration = controller?.duration ?: 0) }
            }
        }

        override fun onShuffleModeEnabledChanged(enabled: Boolean) {
            _playerState.update { it.copy(shuffleMode = enabled) }
        }

        override fun onRepeatModeChanged(mode: Int) {
            _playerState.update { it.copy(repeatMode = mode) }
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
        }
        listeningCounter.recordPlay(songs.getOrNull(startIndex)?.id ?: -1)
    }

    fun togglePlayPause() {
        withController {
            if (it.isPlaying) it.pause() else it.play()
        }
    }

    fun seekTo(position: Long) {
        withController { it.seekTo(position) }
        _playerState.update { it.copy(currentPosition = position) }
    }

    fun skipToNext() {
        withController { it.seekToNextMediaItem() }
    }

    fun skipToPrevious() {
        withController { it.seekToPreviousMediaItem() }
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
