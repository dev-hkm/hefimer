package com.hefimer.app

import android.app.Application
import coil.ImageLoader
import coil.ImageLoaderFactory
import coil.disk.DiskCache
import coil.memory.MemoryCache
import com.hefimer.app.data.network.HefimerApiConfig
import java.io.File
import java.util.concurrent.Executors

class HefimerApp : Application(), ImageLoaderFactory {

    override fun onCreate() {
        super.onCreate()
        pruneStaleCacheFiles()
    }

    override fun newImageLoader(): ImageLoader =
        ImageLoader.Builder(this)
            .okHttpClient(HefimerApiConfig.apiClient)
            .memoryCache {
                MemoryCache.Builder(this)
                    .maxSizePercent(0.20)
                    .build()
            }
            .diskCache {
                DiskCache.Builder()
                    .directory(File(cacheDir, "image_cache"))
                    .maxSizeBytes(64L * 1024L * 1024L)
                    .build()
            }
            .respectCacheHeaders(false)
            .crossfade(true)
            .build()

    private fun pruneStaleCacheFiles() {
        Executors.newSingleThreadExecutor().execute {
            runCatching {
                val cutoff = System.currentTimeMillis() - 6L * 3_600_000L
                cacheDir.listFiles()?.forEach { f ->
                    if (f.isFile && f.lastModified() < cutoff) {
                        val n = f.name
                        if (n.endsWith(".zip", ignoreCase = true) ||
                            n.startsWith("hef_") ||
                            n.startsWith("picked_")
                        ) {
                            f.delete()
                        }
                    }
                }
            }
        }
    }
}
