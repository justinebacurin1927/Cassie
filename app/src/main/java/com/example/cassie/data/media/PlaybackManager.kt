package com.example.cassie.data.media

import android.app.Application
import android.app.PendingIntent
import android.content.Intent
import android.net.Uri
import android.os.Handler
import android.os.Looper
import androidx.compose.runtime.Stable
import androidx.core.net.toUri
import androidx.lifecycle.AndroidViewModel
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.MediaSession
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
)

class PlaybackManager(app: Application) : AndroidViewModel(app) {

    private val _playerState = MutableStateFlow(PlayerState())
    val playerState: StateFlow<PlayerState> = _playerState.asStateFlow()

    val listeningCounter = ListeningCounter()

    private var player: ExoPlayer? = null
    private var mediaSession: MediaSession? = null
    private var currentSongIndex: Int = -1

    init {
        initPlayer()
    }

    private fun initPlayer() {
        val ctx = getApplication<Application>()
        player = ExoPlayer.Builder(ctx).build().apply {
            addListener(object : Player.Listener {
                override fun onIsPlayingChanged(isPlaying: Boolean) {
                    _playerState.update { it.copy(isPlaying = isPlaying) }
                }

                override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                    val idx = player?.currentMediaItemIndex ?: -1
                    updateCurrentItem(idx)
                    val song = _playerState.value.queue.getOrNull(idx)
                    if (song != null) {
                        listeningCounter.recordPlay(song.id)
                    }
                }

                override fun onPlaybackStateChanged(state: Int) {
                    if (state == Player.STATE_READY) {
                        _playerState.update { it.copy(duration = duration) }
                    }
                }
            })
        }
    }

    /** Wire up MediaSession so Android shows notification controls. */
    fun initMediaSession(activity: android.app.Activity) {
        if (mediaSession != null) return
        val intent = activity.packageManager?.getLaunchIntentForPackage(activity.packageName)
        val pi = PendingIntent.getActivity(activity, 0, intent, PendingIntent.FLAG_IMMUTABLE)
        mediaSession = MediaSession.Builder(activity, player!!)
            .setSessionActivity(pi)
            .build()
    }

    private fun updateCurrentItem(index: Int) {
        if (index < 0 || index >= _playerState.value.queue.size) return
        currentSongIndex = index
        val song = _playerState.value.queue[index]
        _playerState.update {
            it.copy(
                currentSong = song,
                duration = player?.duration ?: 0,
                currentPosition = 0,
            )
        }
    }

    /** Return the actual live playback position from ExoPlayer. */
    fun getCurrentPosition(): Long = player?.currentPosition ?: 0

    /** Return the audio session ID needed for equalizer. */
    fun getAudioSessionId(): Int = player?.audioSessionId ?: -1

    fun play(song: Song) {
        val queue = _playerState.value.queue
        val index = queue.indexOfFirst { it.id == song.id }
        if (index >= 0) {
            player?.seekTo(index, 0)
        } else {
            playQueue(listOf(song) + queue, 0)
        }
        player?.play()
    }

    fun playQueue(songs: List<Song>, startIndex: Int = 0) {
        val mediaItems = songs.map { buildMediaItem(it) }

        player?.apply {
            setMediaItems(mediaItems, startIndex, 0)
            prepare()
            play()
        }

        _playerState.update {
            it.copy(queue = songs, shuffleMode = false)
        }
        updateCurrentItem(startIndex)
    }

    fun togglePlayPause() {
        player?.let {
            if (it.isPlaying) it.pause() else it.play()
        }
    }

    fun seekTo(position: Long) {
        player?.seekTo(position)
        _playerState.update { it.copy(currentPosition = position) }
    }

    fun skipToNext() {
        player?.seekToNextMediaItem()
    }

    fun skipToPrevious() {
        player?.seekToPreviousMediaItem()
    }

    fun toggleShuffle() {
        player?.apply {
            shuffleModeEnabled = !shuffleModeEnabled
            _playerState.update { it.copy(shuffleMode = shuffleModeEnabled) }
        }
    }

    fun cycleRepeat() {
        player?.apply {
            val next = when (repeatMode) {
                Player.REPEAT_MODE_OFF -> Player.REPEAT_MODE_ONE
                Player.REPEAT_MODE_ONE -> Player.REPEAT_MODE_ALL
                else -> Player.REPEAT_MODE_OFF
            }
            repeatMode = next
            _playerState.update { it.copy(repeatMode = next) }
        }
    }

    // ── sleep timer ─────────────────────────────────────────────────
    private var sleepHandler: Handler? = null
    private var sleepRunnable: Runnable? = null

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
                    player?.pause()
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

    // ── queue management ─────────────────────────────────────────────
    fun removeFromQueue(index: Int) {
        val old = _playerState.value.queue
        if (index < 0 || index >= old.size) return
        val newQueue = old.toMutableList().apply { removeAt(index) }
        player?.let { p ->
            val mediaItems = newQueue.map { buildMediaItem(it) }
            p.setMediaItems(mediaItems, p.currentMediaItemIndex.coerceAtMost(mediaItems.size - 1).coerceAtLeast(0), 0)
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
        player?.let { p ->
            val mediaItems = newQueue.map { buildMediaItem(it) }
            p.setMediaItems(mediaItems, p.currentMediaItemIndex.coerceAtMost(mediaItems.size - 1).coerceAtLeast(0), 0)
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
        mediaSession?.release()
        player?.release()
        player = null
    }
}
