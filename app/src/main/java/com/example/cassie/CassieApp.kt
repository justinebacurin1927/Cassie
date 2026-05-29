package com.example.cassie

import android.app.Application
import coil.Coil
import coil.ImageLoader
import coil.disk.DiskCache
import coil.request.CachePolicy

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
    }
}
