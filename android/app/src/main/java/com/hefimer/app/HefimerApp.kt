package com.hefimer.app

import android.app.Application
import coil.ImageLoader
import coil.ImageLoaderFactory
import com.hefimer.app.data.network.HefimerApiConfig

class HefimerApp : Application(), ImageLoaderFactory {
    override fun newImageLoader(): ImageLoader =
        ImageLoader.Builder(this)
            .okHttpClient(HefimerApiConfig.apiClient)
            .crossfade(true)
            .build()
}
