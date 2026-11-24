package com.hefimer.app.data.network

import android.app.DownloadManager
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream
import java.net.URI
import java.net.URLEncoder

enum class DirectDownloadMode { PROXY, FETCH, TAB }

object DownloadHelper {
    private val PROXIED_HOSTS = setOf(
        "storage.to",
        "tmpfiles.org",
        "litter.catbox.moe",
        "litterbox.catbox.moe",
        "filebin.net",
        "trystocare.com",
        "d.tmpfile.link",
        "tempfile.org",
        "temp.sh",
        "d.uguu.se",
        "put.icu",
    )

    private val TAB_HOSTS = setOf(
        "gofile.io",
        "vikingfile.com",
    )

    private val SKIP_INLINE_PREVIEW_HOSTS = setOf(
        "gofile.io",
        "vikingfile.com",
        "storage.to",
    )

    fun getDirectDownloadMode(url: String): DirectDownloadMode {
        val host = runCatching { URI(url).host?.lowercase().orEmpty() }.getOrDefault("")
        return when {
            host in PROXIED_HOSTS -> DirectDownloadMode.PROXY
            host in TAB_HOSTS -> DirectDownloadMode.TAB
            else -> DirectDownloadMode.FETCH
        }
    }

    fun buildProxyUrl(url: String, fileName: String, inline: Boolean = false): String {
        val encUrl = URLEncoder.encode(url, "UTF-8")
        val encName = URLEncoder.encode(fileName.ifBlank { "download" }, "UTF-8")
        val suffix = if (inline) "&inline=1" else ""
        return "${HefimerApiConfig.SITE_ORIGIN}/api/download?url=$encUrl&filename=$encName$suffix"
    }

    fun getMediaPreviewUrl(url: String?, mimeType: String?): String? {
        if (url.isNullOrBlank()) return null
        val mime = mimeType.orEmpty().lowercase()
        val isPreviewable = mime.startsWith("image/") || mime.startsWith("video/") || mime.startsWith("audio/")
        if (!isPreviewable || mime.contains("svg")) return null
        val parsed = runCatching { URI(url) }.getOrNull() ?: return null
        if (parsed.scheme?.lowercase() != "https") return null
        val host = parsed.host?.lowercase().orEmpty()
        if (host in SKIP_INLINE_PREVIEW_HOSTS) return null
        return buildProxyUrl(url, "preview", inline = true)
    }

    suspend fun downloadFile(context: Context, url: String, fileName: String): String {
        val safeName = fileName.ifBlank { "hefimer_download" }
            .replace(Regex("""[/\\:*?"<>|]"""), "_")
        val mode = getDirectDownloadMode(url)
        if (mode == DirectDownloadMode.TAB) {
            openExternalUrl(context, url)
            return "tab"
        }

        var targetUrl = if (mode == DirectDownloadMode.PROXY) buildProxyUrl(url, safeName) else url

        // Probe storage.to through proxy just like web src/downloads.ts
        val host = runCatching { URI(url).host?.lowercase().orEmpty() }.getOrDefault("")
        if (host == "storage.to") {
            val probeOk = withContext(Dispatchers.IO) {
                runCatching {
                    val req = Request.Builder().url("$targetUrl&probe=1").get().build()
                    HefimerApiConfig.apiClient.newCall(req).execute().use { it.isSuccessful }
                }.getOrDefault(false)
            }
            if (!probeOk) {
                openExternalUrl(context, url)
                return "tab"
            }
        }

        enqueueSystemDownload(context, targetUrl, safeName)
        return "download"
    }

    fun enqueueSystemDownload(context: Context, url: String, fileName: String) {
        val safeName = fileName.ifBlank { "hefimer_download" }
            .replace(Regex("""[/\\:*?"<>|]"""), "_")
        val dm = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
        val request = DownloadManager.Request(Uri.parse(url))
            .setTitle(safeName)
            .setDescription("Downloading from Hefimer")
            .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
            .setAllowedOverMetered(true)
            .setAllowedOverRoaming(true)
            .addRequestHeader("User-Agent", HefimerApiConfig.USER_AGENT)
            .setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, safeName)
        dm.enqueue(request)
    }

    fun openExternalUrl(context: Context, url: String) {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    }

    suspend fun saveTextToDownloads(context: Context, fileName: String, content: String): String =
        withContext(Dispatchers.IO) {
            val safeName = fileName.ifBlank { "hefimer_text.txt" }
                .replace(Regex("""[/\\:*?"<>|]"""), "_")
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val values = ContentValues().apply {
                    put(MediaStore.Downloads.DISPLAY_NAME, safeName)
                    put(MediaStore.Downloads.MIME_TYPE, "text/plain")
                    put(MediaStore.Downloads.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
                }
                val uri = context.contentResolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values)
                    ?: throw IllegalStateException("Could not create download entry")
                context.contentResolver.openOutputStream(uri)?.use { it.write(content.toByteArray()) }
            } else {
                val dir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                dir.mkdirs()
                File(dir, safeName).writeText(content)
            }
            safeName
        }

    suspend fun saveBitmapToGallery(context: Context, bitmap: Bitmap, fileName: String): String =
        withContext(Dispatchers.IO) {
            val safeName = fileName.ifBlank { "hefimer_${System.currentTimeMillis()}.png" }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val values = ContentValues().apply {
                    put(MediaStore.Images.Media.DISPLAY_NAME, safeName)
                    put(MediaStore.Images.Media.MIME_TYPE, "image/png")
                    put(MediaStore.Images.Media.RELATIVE_PATH, "${Environment.DIRECTORY_PICTURES}/Hefimer")
                }
                val uri = context.contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
                    ?: throw IllegalStateException("Could not save image")
                context.contentResolver.openOutputStream(uri)?.use { out ->
                    bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
                }
            } else {
                val dir = File(
                    Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES),
                    "Hefimer",
                )
                dir.mkdirs()
                FileOutputStream(File(dir, safeName)).use { out ->
                    bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
                }
            }
            safeName
        }
}
