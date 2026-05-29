package com.example.cassie.data.media

import android.app.PendingIntent
import android.content.Intent
import android.os.Build
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import java.lang.ref.WeakReference

/**
 * Foreground service that owns the ExoPlayer and MediaSession.
 * This keeps music playing in the background and shows a persistent
 * notification with playback controls (play/pause/prev/next) via Media3.
 */
class CassiePlaybackService : MediaSessionService() {

    private var mediaSession: MediaSession? = null

    lateinit var player: ExoPlayer
        private set

    override fun onCreate() {
        super.onCreate()
        player = ExoPlayer.Builder(this).build()
        companionPlayerRef = WeakReference(player)

        val intent = packageManager?.getLaunchIntentForPackage(packageName)
        val pi = PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_IMMUTABLE)
        mediaSession = MediaSession.Builder(this, player)
            .setSessionActivity(pi)
            .build()
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? = mediaSession

    override fun onTaskRemoved(rootIntent: Intent?) {
        val p = player
        if (!p.playWhenReady || p.mediaItemCount == 0) {
            stopSelf()
        }
    }

    override fun onDestroy() {
        mediaSession?.release()
        player.release()
        companionPlayerRef?.clear()
        companionPlayerRef = null
        super.onDestroy()
    }

    companion object {
        private var companionPlayerRef: WeakReference<ExoPlayer>? = null

        /** Access the player for features like the equalizer that need audioSessionId. */
        fun getPlayer(): ExoPlayer? = companionPlayerRef?.get()
    }
}
