package com.hefimer.app.data.network

import kotlinx.serialization.json.Json
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import java.util.concurrent.TimeUnit

object HefimerApiConfig {
    const val SITE_HOST = "hefimer.qzz.io"
    const val SITE_ORIGIN = "https://hefimer.qzz.io"
    const val RTDB_BASE_URL = "https://hefimer-default-rtdb.asia-southeast1.firebasedatabase.app"
    const val USER_AGENT =
        "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/138.0.0.0 Safari/537.36"

    val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        coerceInputValues = true
        encodeDefaults = true
    }

    /**
     * Automatically attaches trusted same-origin headers ONLY for hefimer.qzz.io requests
     * so Pages Functions (`isTrustedBrowserRequest`) accept R2, proxy uploads, and /api/download
     * without leaking Origin/Referer to third-party presigned storage hosts.
     */
    private val trustedOriginInterceptor = Interceptor { chain ->
        val original = chain.request()
        val isHefimerHost = original.url.host.equals(SITE_HOST, ignoreCase = true)
        val builder = original.newBuilder()
        if (original.header("User-Agent") == null) {
            builder.header("User-Agent", USER_AGENT)
        }
        if (isHefimerHost) {
            if (original.header("Origin") == null) builder.header("Origin", SITE_ORIGIN)
            if (original.header("Referer") == null) builder.header("Referer", "$SITE_ORIGIN/")
            if (original.header("Sec-Fetch-Site") == null) builder.header("Sec-Fetch-Site", "same-origin")
            if (original.header("Sec-Fetch-Mode") == null) builder.header("Sec-Fetch-Mode", "cors")
            if (original.header("Accept-Language") == null) builder.header("Accept-Language", "en-US,en;q=0.9")
        }
        chain.proceed(builder.build())
    }

    val apiClient: OkHttpClient = OkHttpClient.Builder()
        .addInterceptor(trustedOriginInterceptor)
        .connectTimeout(25, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .followRedirects(true)
        .build()

    val uploadClient: OkHttpClient = OkHttpClient.Builder()
        .addInterceptor(trustedOriginInterceptor)
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.MINUTES)
        .writeTimeout(20, TimeUnit.MINUTES)
        .followRedirects(true)
        .retryOnConnectionFailure(true)
        .build()

    val sseClient: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(0, TimeUnit.MILLISECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .pingInterval(30, TimeUnit.SECONDS)
        .build()
}
