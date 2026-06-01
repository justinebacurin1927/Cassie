package com.example.cassie

import android.app.Activity
import android.app.Application
import android.os.Bundle
import coil.Coil
import coil.ImageLoader
import coil.disk.DiskCache
import coil.request.CachePolicy
import com.example.cassie.party.SkipperEngine
import com.example.cassie.party.UserEvent
import com.example.cassie.party.UserEventStream

class CassieApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        Coil.setImageLoader(
            ImageLoader.Builder(this)
                .diskCache {
                    DiskCache.Builder()
                        .directory(cacheDir.resolve("coil_cache"))
                        .maxSizeBytes(250L * 1024 * 1024) // 250 MB
                        .build()
                }
                .memoryCachePolicy(CachePolicy.ENABLED)
                .diskCachePolicy(CachePolicy.ENABLED)
                .build()
        )

        // ── Skipper (Party Mode mascot) ───────────────────────────
        // Initialize the pattern-recognition engine. Safe to call
        // multiple times — the engine itself guards against
        // re-initialization.
        SkipperEngine.init(this)

        // Wire app foreground / background into the event stream so
        // the NIGHT_OWL pattern can fire. We track started-activity
        // count via ActivityLifecycleCallbacks — the standard way to
        // get whole-app lifecycle without pulling in
        // lifecycle-process.
        var startedActivities = 0
        registerActivityLifecycleCallbacks(
            object : ActivityLifecycleCallbacks {
                override fun onActivityStarted(activity: Activity) {
                    startedActivities += 1
                    if (startedActivities == 1) {
                        // First activity of the process is now visible:
                        // app came to the foreground.
                        UserEventStream.emit(
                            UserEvent.AppForegrounded(
                                timestamp = System.currentTimeMillis(),
                                sessionId = UserEventStream.currentSessionId,
                            )
                        )
                    }
                }

                override fun onActivityStopped(activity: Activity) {
                    startedActivities = (startedActivities - 1).coerceAtLeast(0)
                    if (startedActivities == 0) {
                        // Last activity just stopped: app is now
                        // backgrounded (or killed by the OS).
                        UserEventStream.emit(
                            UserEvent.AppBackgrounded(
                                timestamp = System.currentTimeMillis(),
                                sessionId = UserEventStream.currentSessionId,
                            )
                        )
                    }
                }

                override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {}
                override fun onActivityResumed(activity: Activity) {}
                override fun onActivityPaused(activity: Activity) {}
                override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) {}
                override fun onActivityDestroyed(activity: Activity) {}
            }
        )
    }
}
