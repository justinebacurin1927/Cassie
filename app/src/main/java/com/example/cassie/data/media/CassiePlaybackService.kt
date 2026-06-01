package com.example.cassie.data.media

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.os.Build
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
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

    companion object {
        private const val CHANNEL_ID = "cassie_playback"
        private const val NOTIFICATION_ID = 1
        private var companionPlayerRef: WeakReference<ExoPlayer>? = null

        /** Access the player for features like the equalizer that need audioSessionId. */
        fun getPlayer(): ExoPlayer? = companionPlayerRef?.get()
    }

    private var mediaSession: MediaSession? = null

    lateinit var player: ExoPlayer
        private set

    override fun onCreate() {
        super.onCreate()

        // ── Post foreground notification IMMEDIATELY to avoid ANR on API 35+ ──
        // The short timeout window on Android 15+ means we must call
        // startForeground() before Media3 sets up its own notification.
        val channelId = CHANNEL_ID
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val ch = NotificationChannel(
                channelId, "Music Playback",
                NotificationManager.IMPORTANCE_LOW
            ).apply { setShowBadge(false) }
            val nm = getSystemService(NotificationManager::class.java)
            nm?.createNotificationChannel(ch)
        }

        val pendingIntent = packageManager?.getLaunchIntentForPackage(packageName)?.let {
            PendingIntent.getActivity(this, 0, it, PendingIntent.FLAG_IMMUTABLE)
        }

        val notification = Notification.Builder(this, channelId)
            .setContentTitle("Cassie")
            .setContentText("Starting playback...")
            .setSmallIcon(android.R.drawable.ic_media_play)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .build()

        startForeground(NOTIFICATION_ID, notification)

        // ── Now set up ExoPlayer + MediaSession ──
        // AudioAttributes + handleAudioFocus = true so a phone call
        // (or other music app) pauses us cleanly instead of fighting
        // for the audio output.
        val audioAttrs = AudioAttributes.Builder()
            .setUsage(C.USAGE_MEDIA)
            .setContentType(C.CONTENT_TYPE_MUSIC)
            .build()
        player = ExoPlayer.Builder(this)
            .setAudioAttributes(audioAttrs, /* handleAudioFocus = */ true)
            .setHandleAudioBecomingNoisy(true)
            .build()
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
}
