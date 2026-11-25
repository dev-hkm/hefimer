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
import android.webkit.MimeTypeMap
import androidx.core.content.FileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import okhttp3.Call
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.OutputStream
import java.net.URI
import java.net.URLEncoder
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicReference

enum class DirectDownloadMode { PROXY, FETCH, TAB }

data class DownloadOutcome(
    val mode: String, // "download" or "tab"
    val savedUri: Uri? = null,
    val fileName: String = "",
    val mimeType: String = "application/octet-stream",
    val sizeBytes: Long = 0L,
)

class DownloadCancellationToken {
    private val cancelled = AtomicBoolean(false)
    private val activeCall = AtomicReference<Call?>(null)

    fun isCancelled(): Boolean = cancelled.get()
    fun bindCall(call: Call) {
        activeCall.set(call)
        if (cancelled.get()) call.cancel()
    }

    fun clearCall(call: Call? = null) {
        if (call == null) activeCall.set(null) else activeCall.compareAndSet(call, null)
    }

    fun cancel() {
        cancelled.set(true)
        activeCall.getAndSet(null)?.cancel()
    }
}

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
        "buzzheavier.com",
        "fileditch.com",
        "new.fileditch.com",
        "itsfileditchfiles.st",
    )

    private val SKIP_INLINE_PREVIEW_HOSTS = setOf(
        "gofile.io",
        "vikingfile.com",
        "storage.to",
        "buzzheavier.com",
        "fileditch.com",
        "new.fileditch.com",
        "itsfileditchfiles.st",
        "qu.ax",
        "temp.sh",
    )

    private val DIRECT_INLINE_PREVIEW_HOSTS = setOf(
        "x0.at",
        "pomf2.lain.la",
        "pomf.lain.la",
        "files.catbox.moe",
        "d.tmpfile.link",
        "n.uguu.se",
    )

    fun getDirectDownloadMode(url: String): DirectDownloadMode {
        val host = runCatching { URI(url).host?.lowercase().orEmpty() }.getOrDefault("")
        return when {
            host in PROXIED_HOSTS || host.endsWith(".uguu.se") -> DirectDownloadMode.PROXY
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
        if (host in DIRECT_INLINE_PREVIEW_HOSTS) return url
        return buildProxyUrl(url, "preview", inline = true)
    }

    private fun safeDownloadName(raw: String): String {
        val cleaned = raw.trim()
            .replace(Regex("""[/\\:*?"<>|\r\n\u0000-\u001f]"""), "_")
            .trim('.')
            .ifBlank { "hefimer_download" }
        return if (cleaned.length <= 160) cleaned else {
            val ext = cleaned.substringAfterLast('.', "").take(16)
            if (ext.isNotEmpty() && cleaned.contains('.')) {
                cleaned.take(158 - ext.length) + "." + ext
            } else {
                cleaned.take(160)
            }
        }
    }

    /**
     * Native OkHttp streaming downloader that writes directly to Android Downloads (MediaStore)
     * with live byte progress, trusted same-origin headers for hefimer.qzz.io, full Unicode filename
     * support, and smart mobile-IP resolution for storage.to.
     */
    suspend fun downloadFile(
        context: Context,
        url: String,
        fileName: String,
        cancelToken: DownloadCancellationToken = DownloadCancellationToken(),
        onProgress: (loadedBytes: Long, totalBytes: Long, percent: Int) -> Unit = { _, _, _ -> },
    ): DownloadOutcome = withContext(Dispatchers.IO) {
        val safeName = safeDownloadName(fileName)
        val mode = getDirectDownloadMode(url)
        if (mode == DirectDownloadMode.TAB) {
            openExternalUrl(context, url)
            return@withContext DownloadOutcome(mode = "tab", fileName = safeName)
        }

        val host = runCatching { URI(url).host?.lowercase().orEmpty() }.getOrDefault("")

        // 1. For storage.to: try mobile-IP direct resolution FIRST, then proxy, then browser fallback
        if (host == "storage.to") {
            val directResolved = runCatching { resolveStorageToOnDevice(url) }.getOrNull()
            if (!directResolved.isNullOrBlank()) {
                val res = runCatching {
                    streamToDownloads(context, directResolved, safeName, cancelToken, onProgress)
                }.getOrNull()
                if (res != null) return@withContext res
            }
            val proxyUrl = buildProxyUrl(url, safeName)
            val proxyRes = runCatching {
                streamToDownloads(context, proxyUrl, safeName, cancelToken, onProgress)
            }.getOrNull()
            if (proxyRes != null) return@withContext proxyRes

            if (cancelToken.isCancelled()) throw IOException("DOWNLOAD_CANCELED")
            openExternalUrl(context, url)
            return@withContext DownloadOutcome(mode = "tab", fileName = safeName)
        }

        // 2. For qu.ax: resolve /x/<file> raw link from page HTML and stream directly with Referer
        if (host == "qu.ax" || host.endsWith(".qu.ax")) {
            val resolved = runCatching { resolveQuAxOnDevice(url) }.getOrNull()
            if (!resolved.isNullOrBlank()) {
                val res = runCatching {
                    streamToDownloads(context, resolved, safeName, cancelToken, onProgress, referer = url)
                }.getOrNull()
                if (res != null) return@withContext res
            }
            if (cancelToken.isCancelled()) throw IOException("DOWNLOAD_CANCELED")
            openExternalUrl(context, url)
            return@withContext DownloadOutcome(mode = "tab", fileName = safeName)
        }

        // 3. For proxied hosts (sto.care, tmpfile.link, tempfile, temp.sh, tmpfiles, put.icu, litterbox, filebin, uguu):
        //    Stream through /api/download with trusted same-origin headers, with direct fallback if proxy fails.
        if (mode == DirectDownloadMode.PROXY) {
            val proxyUrl = buildProxyUrl(url, safeName)
            try {
                return@withContext streamToDownloads(context, proxyUrl, safeName, cancelToken, onProgress)
            } catch (e: Throwable) {
                if (cancelToken.isCancelled() || e.message == "DOWNLOAD_CANCELED") throw e
                // Fallback: try direct download from phone's own IP
                try {
                    val directTarget = when {
                        host == "put.icu" && !url.contains("/s/") ->
                            url.replaceFirst("put.icu/", "put.icu/s/")
                        host == "tmpfiles.org" ->
                            resolveTmpfilesDirectUrl(url) ?: url.replaceFirst("tmpfiles.org/", "tmpfiles.org/dl/")
                        else -> url
                    }
                    return@withContext streamToDownloads(context, directTarget, safeName, cancelToken, onProgress)
                } catch (directErr: Throwable) {
                    if (cancelToken.isCancelled() || directErr.message == "DOWNLOAD_CANCELED") throw directErr
                    openExternalUrl(context, url)
                    return@withContext DownloadOutcome(mode = "tab", fileName = safeName)
                }
            }
        }

        // 4. Direct URLs (x0.at, pomf2.lain.la, files.catbox.moe, Cloudflare R2 presigned URLs)
        streamToDownloads(context, url, safeName, cancelToken, onProgress)
    }

    private fun resolveQuAxOnDevice(shareUrl: String): String? {
        val req = Request.Builder().url(shareUrl).get().build()
        val html = HefimerApiConfig.apiClient.newCall(req).execute().use { resp ->
            if (!resp.isSuccessful) return null
            val ct = resp.header("Content-Type").orEmpty()
            if (!ct.contains("text/html", ignoreCase = true)) return shareUrl
            resp.body?.string().orEmpty()
        }
        val rel = Regex("""href=["'](/x/[^"']+)["']""", RegexOption.IGNORE_CASE).find(html)?.groupValues?.get(1)
            ?: return null
        return "https://qu.ax$rel"
    }

    private fun resolveTmpfilesDirectUrl(url: String): String? {
        val dlPage = if (url.contains("/dl/")) url else url.replaceFirst("tmpfiles.org/", "tmpfiles.org/dl/")
        val req = Request.Builder().url(dlPage).get().build()
        val body = HefimerApiConfig.apiClient.newCall(req).execute().use { resp ->
            if (!resp.isSuccessful) return null
            val ct = resp.header("Content-Type").orEmpty()
            if (!ct.contains("text/html", ignoreCase = true)) return dlPage
            resp.body?.string().orEmpty()
        }
        return Regex("""https?://tmpfiles\.org/dl/[0-9]+(?:\.[0-9a-f]+)+/[^"'\s<>]+""")
            .find(body)?.value
    }

    /**
     * Resolves a storage.to share URL directly using the phone's own mobile IP.
     */
    private fun resolveStorageToOnDevice(shareUrl: String): String? {
        val uri = URI(shareUrl)
        val shareId = Regex("""^/([A-Za-z0-9_-]{5,64})/?$""").find(uri.path.orEmpty())?.groupValues?.get(1)
            ?: return shareUrl
        val pageReq = Request.Builder()
            .url(shareUrl)
            .header("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8")
            .header("Cache-Control", "no-cache")
            .get()
            .build()
        val html = HefimerApiConfig.apiClient.newCall(pageReq).execute().use { resp ->
            if (!resp.isSuccessful) return null
            val ct = resp.header("Content-Type").orEmpty()
            if (!ct.contains("text/html", ignoreCase = true)) return shareUrl
            resp.body?.string().orEmpty()
        }
        val escapedId = Regex.escape(shareId)
        val signedMatch = Regex(
            """(?:https://storage\.to)?/$escapedId/download\?expires=\d+(?:&amp;|&)signature=[A-Za-z0-9_-]+""",
        ).find(html)
        if (signedMatch != null) {
            val raw = signedMatch.value.replace("&amp;", "&")
            return if (raw.startsWith("http")) raw else "https://storage.to$raw"
        }
        val proofMatch = Regex("""\\?["']mint_proof\\?["']\s*,\s*\\?["']([0-9]+\.[a-f0-9]{32,128})\\?["']""", RegexOption.IGNORE_CASE).find(html)
        val proof = proofMatch?.groupValues?.get(1) ?: return null
        val dlReq = Request.Builder()
            .url("https://storage.to/$shareId/download")
            .header("Accept", "application/json")
            .header("x-mint-proof", proof)
            .get()
            .build()
        return HefimerApiConfig.apiClient.newCall(dlReq).execute().use { resp ->
            if (!resp.isSuccessful) return null
            val obj = HefimerApiConfig.json.parseToJsonElement(resp.body?.string().orEmpty()).jsonObject
            obj["url"]?.jsonPrimitive?.contentOrNull
        }
    }

    suspend fun streamDirectUrlToDownloads(
        context: Context,
        url: String,
        fileName: String,
        cancelToken: DownloadCancellationToken = DownloadCancellationToken(),
        onProgress: (loadedBytes: Long, totalBytes: Long, percent: Int) -> Unit = { _, _, _ -> },
    ): DownloadOutcome = withContext(Dispatchers.IO) {
        streamToDownloads(context, url, safeDownloadName(fileName), cancelToken, onProgress)
    }

    private fun resolveMimeType(fileName: String, serverCt: String): String {
        val clean = serverCt.substringBefore(';').trim()
        if (clean.isNotBlank() && !clean.equals("application/octet-stream", ignoreCase = true)) {
            return clean
        }
        val ext = fileName.substringAfterLast('.', "").lowercase()
        return if (ext.isNotBlank()) {
            MimeTypeMap.getSingleton().getMimeTypeFromExtension(ext) ?: "application/octet-stream"
        } else {
            "application/octet-stream"
        }
    }

    private fun streamToDownloads(
        context: Context,
        url: String,
        safeName: String,
        cancelToken: DownloadCancellationToken,
        onProgress: (loadedBytes: Long, totalBytes: Long, percent: Int) -> Unit,
        referer: String? = null,
    ): DownloadOutcome {
        if (cancelToken.isCancelled()) throw IOException("DOWNLOAD_CANCELED")
        val reqBuilder = Request.Builder()
            .url(url)
            .get()
        if (!referer.isNullOrBlank()) {
            reqBuilder.header("Referer", referer)
        }
        val req = reqBuilder.build()
        val call = HefimerApiConfig.uploadClient.newCall(req)
        cancelToken.bindCall(call)
        try {
            call.execute().use { resp ->
                if (!resp.isSuccessful) {
                    throw IOException(
                        com.hefimer.app.ui.theme.tr(
                            "Download failed with HTTP ${resp.code}",
                            "Tải xuống thất bại với mã HTTP ${resp.code}",
                        ),
                    )
                }
                val body = resp.body ?: throw IOException(
                    com.hefimer.app.ui.theme.tr("Empty download response", "Phản hồi tải xuống trống"),
                )
                val rawCt = resp.header("Content-Type")?.substringBefore(';')?.trim() ?: "application/octet-stream"
                // Guard against accidentally saving a provider HTML error page as a binary file
                if (rawCt.equals("text/html", ignoreCase = true) && !safeName.endsWith(".html", true) && !safeName.endsWith(".htm", true)) {
                    throw IOException(
                        com.hefimer.app.ui.theme.tr(
                            "Upstream returned an HTML page instead of file bytes",
                            "Máy chủ trả về trang HTML thay vì dữ liệu tệp",
                        ),
                    )
                }
                val ct = resolveMimeType(safeName, rawCt)
                val totalBytes = body.contentLength()
                onProgress(0L, totalBytes, 0)

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    val values = ContentValues().apply {
                        put(MediaStore.Downloads.DISPLAY_NAME, safeName)
                        put(MediaStore.Downloads.MIME_TYPE, ct)
                        put(MediaStore.Downloads.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
                        put(MediaStore.Downloads.IS_PENDING, 1)
                    }
                    val resolver = context.contentResolver
                    val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values)
                        ?: throw IOException(
                            com.hefimer.app.ui.theme.tr(
                                "Could not create file in Downloads",
                                "Không thể tạo tệp trong thư mục Tải xuống",
                            ),
                        )
                    var written = 0L
                    try {
                        resolver.openOutputStream(uri)?.use { out ->
                            written = copyWithProgress(body.byteStream(), out, totalBytes, cancelToken, onProgress)
                        } ?: throw IOException(
                            com.hefimer.app.ui.theme.tr(
                                "Cannot open Downloads output stream",
                                "Không thể mở luồng ghi vào thư mục Tải xuống",
                            ),
                        )
                        val finish = ContentValues().apply { put(MediaStore.Downloads.IS_PENDING, 0) }
                        resolver.update(uri, finish, null, null)
                        return DownloadOutcome(
                            mode = "download",
                            savedUri = uri,
                            fileName = safeName,
                            mimeType = ct,
                            sizeBytes = written.coerceAtLeast(totalBytes.coerceAtLeast(0L)),
                        )
                    } catch (e: Throwable) {
                        runCatching { resolver.delete(uri, null, null) }
                        throw e
                    }
                } else {
                    val dir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                    dir.mkdirs()
                    val dest = uniqueFile(dir, safeName)
                    var written = 0L
                    try {
                        FileOutputStream(dest).use { out ->
                            written = copyWithProgress(body.byteStream(), out, totalBytes, cancelToken, onProgress)
                        }
                        val uri = runCatching {
                            FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", dest)
                        }.getOrElse { Uri.fromFile(dest) }
                        return DownloadOutcome(
                            mode = "download",
                            savedUri = uri,
                            fileName = dest.name,
                            mimeType = ct,
                            sizeBytes = written.coerceAtLeast(dest.length()),
                        )
                    } catch (e: Throwable) {
                        dest.delete()
                        throw e
                    }
                }
            }
        } finally {
            cancelToken.clearCall(call)
        }
    }

    private fun uniqueFile(dir: File, name: String): File {
        var candidate = File(dir, name)
        if (!candidate.exists()) return candidate
        val base = name.substringBeforeLast('.', name)
        val ext = if (name.contains('.')) ".${name.substringAfterLast('.')}" else ""
        var i = 1
        while (candidate.exists()) {
            candidate = File(dir, "${base}_$i$ext")
            i++
        }
        return candidate
    }

    private fun copyWithProgress(
        input: java.io.InputStream,
        output: OutputStream,
        totalBytes: Long,
        cancelToken: DownloadCancellationToken,
        onProgress: (loadedBytes: Long, totalBytes: Long, percent: Int) -> Unit,
    ): Long {
        val buf = ByteArray(64 * 1024)
        var loaded = 0L
        var lastEmitNs = 0L
        while (true) {
            if (cancelToken.isCancelled()) throw IOException("DOWNLOAD_CANCELED")
            val read = input.read(buf)
            if (read <= 0) break
            output.write(buf, 0, read)
            loaded += read
            val now = System.nanoTime()
            if (now - lastEmitNs >= 32_000_000L) {
                lastEmitNs = now
                val pct = if (totalBytes > 0L) ((loaded * 100L) / totalBytes).toInt().coerceIn(0, 100) else -1
                onProgress(loaded, totalBytes, pct)
            }
        }
        output.flush()
        onProgress(loaded, if (totalBytes > 0L) totalBytes else loaded, 100)
        return loaded
    }

    fun openExternalUrl(context: Context, url: String) {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    }

    fun isUriReadable(context: Context, uriString: String): Boolean {
        if (uriString.isBlank()) return false
        return runCatching {
            val uri = Uri.parse(uriString)
            context.contentResolver.openAssetFileDescriptor(uri, "r")?.use { true } ?: false
        }.getOrDefault(false)
    }

    fun openSavedFileWithChooser(context: Context, uriString: String, mimeType: String, fileName: String): Boolean {
        val uri = runCatching { Uri.parse(uriString) }.getOrNull() ?: return false
        val resolvedMime = resolveMimeType(fileName, mimeType)
        val viewIntent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, resolvedMime)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        val chooser = Intent.createChooser(
            viewIntent,
            com.hefimer.app.ui.theme.tr("Open $fileName with", "Mở $fileName bằng"),
        ).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        return runCatching {
            context.startActivity(chooser)
            true
        }.getOrElse {
            // Fallback with wildcard mime type if exact mime has no chooser
            runCatching {
                val fallback = Intent(Intent.ACTION_VIEW).apply {
                    setDataAndType(uri, "*/*")
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(
                    Intent.createChooser(fallback, com.hefimer.app.ui.theme.tr("Open with", "Mở bằng"))
                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
                )
                true
            }.getOrDefault(false)
        }
    }

    fun shareSavedFile(context: Context, uriString: String, mimeType: String, fileName: String): Boolean {
        val uri = runCatching { Uri.parse(uriString) }.getOrNull() ?: return false
        val resolvedMime = resolveMimeType(fileName, mimeType)
        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = resolvedMime
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        return runCatching {
            context.startActivity(
                Intent.createChooser(shareIntent, fileName)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION),
            )
            true
        }.getOrDefault(false)
    }

    fun openDownloadsFolder(context: Context): Boolean = runCatching {
        val intent = Intent(DownloadManager.ACTION_VIEW_DOWNLOADS).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
        true
    }.getOrDefault(false)

    suspend fun saveTextToDownloads(context: Context, fileName: String, content: String): DownloadOutcome =
        withContext(Dispatchers.IO) {
            val safeName = safeDownloadName(fileName.ifBlank { "hefimer_text.txt" })
            val bytes = content.toByteArray()
            val mime = resolveMimeType(safeName, "text/plain")
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val values = ContentValues().apply {
                    put(MediaStore.Downloads.DISPLAY_NAME, safeName)
                    put(MediaStore.Downloads.MIME_TYPE, mime)
                    put(MediaStore.Downloads.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
                }
                val uri = context.contentResolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values)
                    ?: throw IllegalStateException(
                        com.hefimer.app.ui.theme.tr("Could not create download entry", "Không thể tạo mục tải xuống"),
                    )
                context.contentResolver.openOutputStream(uri)?.use { it.write(bytes) }
                DownloadOutcome(mode = "download", savedUri = uri, fileName = safeName, mimeType = mime, sizeBytes = bytes.size.toLong())
            } else {
                val dir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                dir.mkdirs()
                val dest = uniqueFile(dir, safeName)
                dest.writeBytes(bytes)
                val uri = runCatching {
                    FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", dest)
                }.getOrElse { Uri.fromFile(dest) }
                DownloadOutcome(mode = "download", savedUri = uri, fileName = dest.name, mimeType = mime, sizeBytes = bytes.size.toLong())
            }
        }

    suspend fun saveBitmapToGallery(context: Context, bitmap: Bitmap, fileName: String): DownloadOutcome =
        withContext(Dispatchers.IO) {
            val safeName = safeDownloadName(fileName.ifBlank { "hefimer_${System.currentTimeMillis()}.png" })
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val values = ContentValues().apply {
                    put(MediaStore.Images.Media.DISPLAY_NAME, safeName)
                    put(MediaStore.Images.Media.MIME_TYPE, "image/png")
                    put(MediaStore.Images.Media.RELATIVE_PATH, "${Environment.DIRECTORY_PICTURES}/Hefimer")
                }
                val uri = context.contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
                    ?: throw IllegalStateException(
                        com.hefimer.app.ui.theme.tr("Could not save image", "Không thể lưu ảnh"),
                    )
                context.contentResolver.openOutputStream(uri)?.use { out ->
                    bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
                }
                DownloadOutcome(mode = "download", savedUri = uri, fileName = safeName, mimeType = "image/png")
            } else {
                val dir = File(
                    Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES),
                    "Hefimer",
                )
                dir.mkdirs()
                val dest = uniqueFile(dir, safeName)
                FileOutputStream(dest).use { out ->
                    bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
                }
                val uri = runCatching {
                    FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", dest)
                }.getOrElse { Uri.fromFile(dest) }
                DownloadOutcome(mode = "download", savedUri = uri, fileName = dest.name, mimeType = "image/png", sizeBytes = dest.length())
            }
        }
}
