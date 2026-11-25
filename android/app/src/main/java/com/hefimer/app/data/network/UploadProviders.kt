package com.hefimer.app.data.network

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import androidx.documentfile.provider.DocumentFile
import com.hefimer.app.data.local.HefimerStorage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.longOrNull
import okhttp3.Call
import okhttp3.FormBody
import okhttp3.MediaType
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.Request
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import okio.BufferedSink
import okio.source
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.InputStream
import java.net.URLEncoder
import java.util.UUID
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicReference
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

data class ExpireOption(val label: String, val value: String)

data class UploadProviderConfig(
    val id: String,
    val name: String,
    val maxSizeBytes: Long,
    val maxSizeLabel: String,
    val expiry: String,
    val speed: String,
    val features: String,
    val isLargeFile: Boolean,
    val defaultExpire: String,
    val expireOptions: List<ExpireOption>,
)

data class FallbackUploadResult(
    val fileUrl: String,
    val providerId: String,
    val providerName: String,
)

data class LocalPickedFile(
    val uri: Uri,
    val name: String,
    val size: Long,
    val mimeType: String,
    val tempFile: File? = null,
)

class UploadCancellationToken {
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

object UploadProviders {
    private const val MB = 1024L * 1024L
    private const val GB = 1024L * MB
    private const val WORKER_PROXY_SAFE_MAX = 95L * MB

    val STANDARD_EXPIRE_OPTIONS = listOf(
        ExpireOption("30 sec", "30s"),
        ExpireOption("5 min", "5p"),
        ExpireOption("30 min", "30p"),
        ExpireOption("1 hour", "1h"),
        ExpireOption("12 hours", "12h"),
        ExpireOption("24 hours", "24h"),
        ExpireOption("48 hours", "48h"),
    )

    val ALL: List<UploadProviderConfig> = listOf(
        UploadProviderConfig(
            id = "stocare",
            name = "sto.care",
            maxSizeBytes = 99L * MB,
            maxSizeLabel = "100 MB",
            expiry = "Deletes after 72 hours (fixed)",
            speed = "Very fast direct stream upload & clean raw download",
            features = "Default fast route up to 100 MB via trystocare.com. Files > 100 MB auto-route to storage.to / Gofile.",
            isLargeFile = false,
            defaultExpire = "72h",
            expireOptions = listOf(ExpireOption("72 hours (Fixed)", "72h")),
        ),
        UploadProviderConfig(
            id = "storageto",
            name = "storage.to",
            maxSizeBytes = 25L * GB,
            maxSizeLabel = "25 GB",
            expiry = "Deletes after 3 days (1-7 days configurable)",
            speed = "Fast, powered by Cloudflare R2 multipart",
            features = "Direct R2 presigned multipart upload up to 25 GB. Falls back automatically if blocked.",
            isLargeFile = true,
            defaultExpire = "72h",
            expireOptions = listOf(
                ExpireOption("1 day", "24h"),
                ExpireOption("2 days", "48h"),
                ExpireOption("3 days (Default)", "72h"),
                ExpireOption("5 days", "120h"),
                ExpireOption("7 days", "168h"),
            ),
        ),
        UploadProviderConfig(
            id = "gofile",
            name = "Gofile",
            maxSizeBytes = Long.MAX_VALUE,
            maxSizeLabel = "No stated limit",
            expiry = "Deletes after 10 days of inactivity",
            speed = "Fast multi-server direct upload",
            features = "Large-file external route with multi-server retry. Downloads open Gofile page.",
            isLargeFile = true,
            defaultExpire = "48h",
            expireOptions = STANDARD_EXPIRE_OPTIONS,
        ),
        UploadProviderConfig(
            id = "litterbox",
            name = "Litterbox",
            maxSizeBytes = 1L * GB,
            maxSizeLabel = "1 GB",
            expiry = "Deletes after 1, 12, 24, or 72 hours",
            speed = "Fast direct Catbox CDN",
            features = "Temporary Catbox host up to 1 GB with configurable retention and direct downloads.",
            isLargeFile = true,
            defaultExpire = "24h",
            expireOptions = listOf(
                ExpireOption("1 hour", "1h"),
                ExpireOption("12 hours", "12h"),
                ExpireOption("24 hours", "24h"),
                ExpireOption("72 hours", "72h"),
            ),
        ),
        UploadProviderConfig(
            id = "vikingfile",
            name = "VikingFile",
            maxSizeBytes = Long.MAX_VALUE,
            maxSizeLabel = "No stated limit",
            expiry = "Inactive cleanup (long retention)",
            speed = "Fast S3 multipart direct upload",
            features = "Large-file S3 multipart fallback route. Downloads open VikingFile page.",
            isLargeFile = true,
            defaultExpire = "48h",
            expireOptions = STANDARD_EXPIRE_OPTIONS,
        ),
        UploadProviderConfig(
            id = "tmpfilelink",
            name = "tfLink",
            maxSizeBytes = WORKER_PROXY_SAFE_MAX,
            maxSizeLabel = "95 MB",
            expiry = "Deletes after 7 days (fixed)",
            speed = "Global CDN delivery",
            features = "tmpfile.link temporary hosting with 7-day retention and direct CDN downloads.",
            isLargeFile = false,
            defaultExpire = "168h",
            expireOptions = listOf(ExpireOption("7 days (Fixed)", "168h")),
        ),
        UploadProviderConfig(
            id = "tempfile",
            name = "TempFile",
            maxSizeBytes = 100L * MB,
            maxSizeLabel = "100 MB",
            expiry = "Deletes after 1, 6, 24, or 48 hours",
            speed = "Fast direct upload & CDN delivery",
            features = "Public API with direct downloads and configurable temporary retention.",
            isLargeFile = false,
            defaultExpire = "24h",
            expireOptions = listOf(
                ExpireOption("1 hour", "1h"),
                ExpireOption("6 hours", "6h"),
                ExpireOption("24 hours", "24h"),
                ExpireOption("48 hours", "48h"),
            ),
        ),
        UploadProviderConfig(
            id = "filebin",
            name = "Filebin",
            maxSizeBytes = WORKER_PROXY_SAFE_MAX,
            maxSizeLabel = "95 MB",
            expiry = "Deletes after 6 days (fixed)",
            speed = "Standard streaming speed",
            features = "One private bin per share with verified direct file downloads.",
            isLargeFile = false,
            defaultExpire = "144h",
            expireOptions = listOf(ExpireOption("6 days (Fixed)", "144h")),
        ),
        UploadProviderConfig(
            id = "tempsh",
            name = "temp.sh",
            maxSizeBytes = WORKER_PROXY_SAFE_MAX,
            maxSizeLabel = "95 MB",
            expiry = "Deletes after 3 days (fixed)",
            speed = "Standard speed",
            features = "Minimalist temporary host with 3-day retention and proxied byte downloads.",
            isLargeFile = false,
            defaultExpire = "72h",
            expireOptions = listOf(ExpireOption("3 days (Fixed)", "72h")),
        ),
        UploadProviderConfig(
            id = "tmpfiles",
            name = "tmpfiles",
            maxSizeBytes = 100L * MB,
            maxSizeLabel = "100 MB",
            expiry = "Deletes after 60 minutes",
            speed = "Fast direct CORS upload",
            features = "Short-lived temporary hosting. Automatically deleted after 1 hour.",
            isLargeFile = false,
            defaultExpire = "1h",
            expireOptions = listOf(ExpireOption("1 hour (Fixed)", "1h")),
        ),
        UploadProviderConfig(
            id = "uguu",
            name = "Uguu.se",
            maxSizeBytes = WORKER_PROXY_SAFE_MAX,
            maxSizeLabel = "95 MB",
            expiry = "Deletes after 3 hours (fixed)",
            speed = "Fast lightweight host",
            features = "Pomf-style ephemeral file host with direct d.uguu.se downloads.",
            isLargeFile = false,
            defaultExpire = "3h",
            expireOptions = listOf(ExpireOption("3 hours (Fixed)", "3h")),
        ),
        UploadProviderConfig(
            id = "puticu",
            name = "Put.icu",
            maxSizeBytes = WORKER_PROXY_SAFE_MAX,
            maxSizeLabel = "95 MB",
            expiry = "Deletes after 5 min to 24 hours",
            speed = "Standard Linx server",
            features = "Configurable short-lived retention (5m, 1h, 12h, 24h) with direct /s/ download.",
            isLargeFile = false,
            defaultExpire = "24h",
            expireOptions = listOf(
                ExpireOption("5 minutes", "5p"),
                ExpireOption("1 hour", "1h"),
                ExpireOption("12 hours", "12h"),
                ExpireOption("24 hours", "24h"),
            ),
        ),
    )

    val FALLBACK_PRIORITY = listOf(
        "stocare",
        "storageto",
        "litterbox",
        "tmpfilelink",
        "tempfile",
        "tmpfiles",
        "tempsh",
        "filebin",
        "uguu",
        "puticu",
        "gofile",
        "vikingfile",
    )

    fun getConfig(id: String): UploadProviderConfig = ALL.firstOrNull { it.id == id } ?: ALL.first()

    fun normalizeExpire(providerId: String, currentExpire: String): String {
        val cfg = getConfig(providerId)
        return if (cfg.expireOptions.any { it.value == currentExpire }) currentExpire else cfg.defaultExpire
    }

    fun getProviderNameFromUrl(url: String, hasObjectKey: Boolean = false): String {
        if (url.isBlank()) return "File"
        if (hasObjectKey) return "Cloudflare R2"
        return when {
            url.contains("gofile.io") -> "Gofile"
            url.contains("catbox.moe") -> "Litterbox"
            url.contains("pixeldrain.com") -> "Pixeldrain"
            url.contains("storage.to") -> "storage.to"
            url.contains("trystocare.com") || url.contains("sto.care") -> "sto.care"
            url.contains("tmpfile.link") -> "tfLink"
            url.contains("tmpfiles.org") -> "tmpfiles.org"
            url.contains("filebin.net") -> "Filebin"
            url.contains("tempfile.org") -> "TempFile"
            url.contains("temp.sh") -> "temp.sh"
            url.contains("uguu.se") -> "Uguu.se"
            url.contains("put.icu") -> "Put.icu"
            url.contains("vikingfile.com") -> "VikingFile"
            else -> runCatching {
                java.net.URI(url).host.orEmpty().removePrefix("www.").substringBefore(".")
                    .replaceFirstChar { it.uppercase() }
            }.getOrDefault("Host")
        }
    }
}

class FileUploadEngine(
    private val context: Context,
    private val storage: HefimerStorage,
) {
    private val client = HefimerApiConfig.uploadClient
    private val json = HefimerApiConfig.json
    private val origin = HefimerApiConfig.SITE_ORIGIN
    private var cachedGofileServer: String? = null

    fun resolvePickedFile(uri: Uri): LocalPickedFile {
        var name = "file_${System.currentTimeMillis()}"
        var size = -1L
        context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
            if (cursor.moveToFirst()) {
                val nameIdx = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                val sizeIdx = cursor.getColumnIndex(OpenableColumns.SIZE)
                if (nameIdx >= 0) cursor.getString(nameIdx)?.takeIf { it.isNotBlank() }?.let { name = it }
                if (sizeIdx >= 0 && !cursor.isNull(sizeIdx)) size = cursor.getLong(sizeIdx)
            }
        }
        val mime = context.contentResolver.getType(uri) ?: "application/octet-stream"
        if (size < 0L) {
            size = runCatching {
                context.contentResolver.openAssetFileDescriptor(uri, "r")?.use { it.length } ?: 0L
            }.getOrDefault(0L)
        }
        return LocalPickedFile(uri = uri, name = name, size = size.coerceAtLeast(0L), mimeType = mime)
    }

    suspend fun zipFolderTree(
        treeUri: Uri,
        onProgress: (Int) -> Unit,
        cancelToken: UploadCancellationToken,
    ): LocalPickedFile = withContext(Dispatchers.IO) {
        val root = DocumentFile.fromTreeUri(context, treeUri)
            ?: throw IOException("Could not open selected folder")
        val folderName = (root.name?.takeIf { it.isNotBlank() } ?: "folder").removeSuffix(".zip")
        val files = mutableListOf<Pair<String, DocumentFile>>()

        fun walk(doc: DocumentFile, prefix: String) {
            if (cancelToken.isCancelled()) throw IOException("UPLOAD_CANCELED")
            for (child in doc.listFiles()) {
                val childName = child.name ?: continue
                val rel = if (prefix.isEmpty()) childName else "$prefix/$childName"
                if (child.isDirectory) walk(child, rel) else files.add(rel to child)
            }
        }
        walk(root, "")
        if (files.isEmpty()) throw IOException("Selected folder is empty")

        val outFile = File(context.cacheDir, "${folderName}_${System.currentTimeMillis()}.zip")
        val totalBytes = files.sumOf { it.second.length().coerceAtLeast(1L) }.coerceAtLeast(1L)
        var writtenBytes = 0L

        ZipOutputStream(FileOutputStream(outFile)).use { zos ->
            val buf = ByteArray(16 * 1024)
            for ((relPath, doc) in files) {
                if (cancelToken.isCancelled()) {
                    outFile.delete()
                    throw IOException("UPLOAD_CANCELED")
                }
                zos.putNextEntry(ZipEntry(relPath))
                context.contentResolver.openInputStream(doc.uri)?.use { input ->
                    while (true) {
                        val read = input.read(buf)
                        if (read <= 0) break
                        zos.write(buf, 0, read)
                        writtenBytes += read
                        onProgress(((writtenBytes * 100) / totalBytes).toInt().coerceIn(0, 100))
                    }
                }
                zos.closeEntry()
            }
        }

        LocalPickedFile(
            uri = Uri.fromFile(outFile),
            name = "$folderName.zip",
            size = outFile.length(),
            mimeType = "application/zip",
            tempFile = outFile,
        )
    }

    private fun asciiSafeWireName(raw: String): String {
        val ext = raw.substringAfterLast('.', "")
            .filter { it.isLetterOrDigit() }
            .take(12)
        val base = raw.substringBeforeLast('.', raw)
        val decomposed = java.text.Normalizer.normalize(base, java.text.Normalizer.Form.NFD)
            .replace(Regex("""\p{InCombiningDiacriticalMarks}+"""), "")
            .replace('đ', 'd').replace('Đ', 'D')
            .replace(Regex("""[^A-Za-z0-9._-]+"""), "_")
            .trim('_', '.')
            .ifBlank { "file_${System.currentTimeMillis()}" }
            .take(80)
        return if (ext.isNotEmpty() && raw.contains('.')) "$decomposed.$ext" else decomposed
    }

    suspend fun uploadWithFallback(
        primaryProviderId: String,
        picked: LocalPickedFile,
        expire: String,
        cancelToken: UploadCancellationToken,
        onProgress: (Int) -> Unit,
        onBytesProgress: (loadedBytes: Long, totalBytes: Long) -> Unit = { _, _ -> },
        onStatus: (String) -> Unit,
        onFallback: (String, String) -> Unit,
    ): FallbackUploadResult = withContext(Dispatchers.IO) {
        val orderedIds = listOf(primaryProviderId) +
            UploadProviders.FALLBACK_PRIORITY.filter { it != primaryProviderId }
        val eligibleIds = orderedIds.filter { id ->
            picked.size <= UploadProviders.getConfig(id).maxSizeBytes
        }
        if (eligibleIds.isEmpty()) {
            val primary = UploadProviders.getConfig(primaryProviderId)
            throw IOException("File size exceeds limit of ${primary.maxSizeLabel} for ${primary.name}")
        }

        var lastError: Throwable? = null
        for (i in eligibleIds.indices) {
            if (cancelToken.isCancelled()) throw IOException("UPLOAD_CANCELED")
            val providerId = eligibleIds[i]
            val cfg = UploadProviders.getConfig(providerId)
            if (i > 0) {
                val prevCfg = UploadProviders.getConfig(eligibleIds[i - 1])
                onFallback(prevCfg.name, cfg.name)
                onProgress(0)
                onBytesProgress(0L, picked.size)
            }
            try {
                val effectiveExpire = if (i == 0) expire else cfg.defaultExpire
                val url = uploadSingle(
                    providerId = providerId,
                    picked = picked,
                    expire = effectiveExpire,
                    cancelToken = cancelToken,
                    onProgress = onProgress,
                    onBytesProgress = onBytesProgress,
                    onStatus = onStatus,
                )
                return@withContext FallbackUploadResult(
                    fileUrl = url,
                    providerId = providerId,
                    providerName = cfg.name,
                )
            } catch (e: Throwable) {
                if (cancelToken.isCancelled() || e.message == "UPLOAD_CANCELED") {
                    throw IOException("UPLOAD_CANCELED")
                }
                lastError = e
            }
        }
        throw lastError ?: IOException("All upload providers failed")
    }

    private fun uploadSingle(
        providerId: String,
        picked: LocalPickedFile,
        expire: String,
        cancelToken: UploadCancellationToken,
        onProgress: (Int) -> Unit,
        onBytesProgress: (Long, Long) -> Unit,
        onStatus: (String) -> Unit,
    ): String = when (providerId) {
        "stocare" -> uploadStoCare(picked, cancelToken, onProgress, onBytesProgress, onStatus)
        "storageto" -> uploadStorageTo(picked, expire, cancelToken, onProgress, onBytesProgress, onStatus)
        "gofile" -> uploadGofile(picked, expire, cancelToken, onProgress, onBytesProgress, onStatus)
        "litterbox" -> uploadLitterbox(picked, expire, cancelToken, onProgress, onBytesProgress, onStatus)
        "vikingfile" -> uploadVikingFile(picked, cancelToken, onProgress, onBytesProgress, onStatus)
        "tmpfilelink" -> uploadTmpfileLink(picked, cancelToken, onProgress, onBytesProgress, onStatus)
        "tempfile" -> uploadTempFile(picked, expire, cancelToken, onProgress, onBytesProgress, onStatus)
        "filebin" -> uploadFilebin(picked, cancelToken, onProgress, onBytesProgress, onStatus)
        "tempsh" -> uploadTempSh(picked, cancelToken, onProgress, onBytesProgress, onStatus)
        "tmpfiles" -> uploadTmpfiles(picked, cancelToken, onProgress, onBytesProgress, onStatus)
        "uguu" -> uploadUguu(picked, cancelToken, onProgress, onBytesProgress, onStatus)
        "puticu" -> uploadPutIcu(picked, expire, cancelToken, onProgress, onBytesProgress, onStatus)
        else -> throw IOException("Unsupported upload provider: $providerId")
    }

    private fun openStream(picked: LocalPickedFile): InputStream {
        if (picked.tempFile != null && picked.tempFile.exists()) {
            return picked.tempFile.inputStream()
        }
        return context.contentResolver.openInputStream(picked.uri)
            ?: throw IOException("Cannot read selected file")
    }

    private fun streamingBody(
        picked: LocalPickedFile,
        onProgress: (Int) -> Unit,
        cancelToken: UploadCancellationToken,
        onBytesProgress: (Long, Long) -> Unit = { _, _ -> },
        rangeStart: Long = 0L,
        rangeLength: Long = picked.size,
        overrideMediaType: MediaType? = picked.mimeType.substringBefore(';').trim().toMediaTypeOrNull(),
        omitContentType: Boolean = false,
    ): RequestBody = object : RequestBody() {
        override fun contentType(): MediaType? = if (omitContentType) null else overrideMediaType
        override fun contentLength(): Long = if (rangeLength > 0L) rangeLength else -1L

        override fun writeTo(sink: BufferedSink) {
            val total = rangeLength.coerceAtLeast(1L)
            var uploaded = 0L
            var lastEmitNs = 0L
            openStream(picked).use { input ->
                val buf = ByteArray(64 * 1024)
                if (rangeStart > 0L) {
                    var skipped = 0L
                    while (skipped < rangeStart) {
                        if (cancelToken.isCancelled()) throw IOException("UPLOAD_CANCELED")
                        val remainingSkip = rangeStart - skipped
                        val s = input.skip(remainingSkip)
                        if (s > 0L) {
                            skipped += s
                        } else {
                            // InputStream.skip() can return 0 on ContentResolver pipes before EOF;
                            // read & discard explicitly until exact rangeStart is reached.
                            val discarded = input.read(buf, 0, minOf(buf.size.toLong(), remainingSkip).toInt())
                            if (discarded <= 0) break
                            skipped += discarded
                        }
                    }
                }
                var remaining = if (rangeLength > 0L) rangeLength else Long.MAX_VALUE
                while (remaining > 0L) {
                    if (cancelToken.isCancelled()) throw IOException("UPLOAD_CANCELED")
                    val toRead = minOf(buf.size.toLong(), remaining).toInt()
                    val read = input.read(buf, 0, toRead)
                    if (read <= 0) break
                    sink.write(buf, 0, read)
                    uploaded += read
                    if (rangeLength > 0L) remaining -= read
                    val now = System.nanoTime()
                    if (now - lastEmitNs >= 32_000_000L || uploaded >= total) {
                        lastEmitNs = now
                        val pct = ((uploaded * 100L) / total).toInt().coerceIn(0, 100)
                        onProgress(pct)
                        onBytesProgress(rangeStart + uploaded, picked.size.coerceAtLeast(1L))
                    }
                }
            }
            onProgress(100)
            onBytesProgress((rangeStart + uploaded).coerceAtMost(picked.size), picked.size.coerceAtLeast(1L))
        }
    }

    private fun execute(
        request: Request,
        cancelToken: UploadCancellationToken,
    ): Triple<Int, String, okhttp3.Headers> {
        if (cancelToken.isCancelled()) throw IOException("UPLOAD_CANCELED")
        val call = client.newCall(request)
        cancelToken.bindCall(call)
        try {
            call.execute().use { resp ->
                val text = resp.body?.string().orEmpty()
                if (!resp.isSuccessful) {
                    val parsedMsg = runCatching {
                        val obj = json.parseToJsonElement(text).jsonObject
                        obj["error"]?.jsonPrimitive?.contentOrNull
                            ?: obj["message"]?.jsonPrimitive?.contentOrNull
                    }.getOrNull()
                    throw IOException(parsedMsg ?: "Upload failed with status ${resp.code}")
                }
                return Triple(resp.code, text, resp.headers)
            }
        } finally {
            cancelToken.clearCall(call)
        }
    }

    private fun encodePathSeg(value: String): String =
        URLEncoder.encode(value, "UTF-8").replace("+", "%20")

    private fun uploadStoCare(
        picked: LocalPickedFile,
        cancelToken: UploadCancellationToken,
        onProgress: (Int) -> Unit,
        onBytesProgress: (Long, Long) -> Unit,
        onStatus: (String) -> Unit,
    ): String {
        if (picked.size > 99L * 1024L * 1024L) {
            throw IOException("sto.care API limit is 100 MB")
        }
        onStatus("Uploading via sto.care...")
        val safeWireName = asciiSafeWireName(picked.name)
        val req = Request.Builder()
            .url("https://ul.sto.care/${encodePathSeg(safeWireName)}")
            .put(streamingBody(picked, onProgress, cancelToken, onBytesProgress))
            .build()
        val (_, text, _) = execute(req, cancelToken)
        val obj = json.parseToJsonElement(text).jsonObject
        return obj["url"]?.jsonPrimitive?.contentOrNull
            ?: throw IOException(obj["error"]?.jsonPrimitive?.contentOrNull ?: "sto.care did not return a URL")
    }

    private fun uploadStorageTo(
        picked: LocalPickedFile,
        expire: String,
        cancelToken: UploadCancellationToken,
        onProgress: (Int) -> Unit,
        onBytesProgress: (Long, Long) -> Unit,
        onStatus: (String) -> Unit,
    ): String {
        onStatus("Initializing upload with storage.to...")
        val visitorToken = storage.getVisitorToken()
        val jsonMedia = "application/json; charset=utf-8".toMediaTypeOrNull()
        val cleanMime = picked.mimeType.substringBefore(';').trim().ifBlank { "application/octet-stream" }

        val initPayload = buildJsonObject {
            put("filename", JsonPrimitive(picked.name))
            put("content_type", JsonPrimitive(cleanMime))
            put("size", JsonPrimitive(picked.size))
        }.toString()

        val initReq = Request.Builder()
            .url("$origin/api/proxy/storageto/upload/init")
            .header("X-Visitor-Token", visitorToken)
            .post(initPayload.toRequestBody(jsonMedia))
            .build()
        val (_, initText, _) = execute(initReq, cancelToken)
        val initObj = json.parseToJsonElement(initText).jsonObject
        if (initObj["success"]?.jsonPrimitive?.content != "true") {
            throw IOException(initObj["error"]?.jsonPrimitive?.contentOrNull ?: "storage.to init failed")
        }

        val r2Key = initObj["r2_key"]?.jsonPrimitive?.contentOrNull.orEmpty()
        var ownerToken = initObj["owner_token"]?.jsonPrimitive?.contentOrNull.orEmpty()
        val uploadType = initObj["type"]?.jsonPrimitive?.contentOrNull.orEmpty()

        if (uploadType == "single") {
            val uploadUrl = initObj["upload_url"]?.jsonPrimitive?.contentOrNull
                ?: throw IOException("Missing storage.to upload URL")
            onStatus("Uploading bytes to storage.to...")
            val putReq = Request.Builder()
                .url(uploadUrl)
                .put(streamingBody(picked, onProgress, cancelToken, onBytesProgress, overrideMediaType = cleanMime.toMediaTypeOrNull()))
                .build()
            execute(putReq, cancelToken)
        } else if (uploadType == "multipart") {
            val uploadId = initObj["upload_id"]?.jsonPrimitive?.contentOrNull.orEmpty()
            val partSize = initObj["part_size"]?.jsonPrimitive?.longOrNull ?: (10L * 1024L * 1024L)
            val totalParts = initObj["total_parts"]?.jsonPrimitive?.intOrNull ?: 1
            val initialUrls = initObj["initial_urls"] as? JsonObject
            onStatus("Uploading $totalParts parts to storage.to...")
            val completedParts = mutableListOf<Pair<Int, String>>()

            for (partNum in 1..totalParts) {
                if (cancelToken.isCancelled()) throw IOException("UPLOAD_CANCELED")
                var partUrl = initialUrls?.get(partNum.toString())?.jsonPrimitive?.contentOrNull
                if (partUrl.isNullOrBlank()) {
                    val partReqBody = buildJsonObject {
                        put("upload_id", JsonPrimitive(uploadId))
                        put("part_numbers", buildJsonArray { add(JsonPrimitive(partNum)) })
                    }.toString()
                    val partUrlReq = Request.Builder()
                        .url("$origin/api/proxy/storageto/upload/parts")
                        .header("Authorization", "Owner $ownerToken")
                        .post(partReqBody.toRequestBody(jsonMedia))
                        .build()
                    val (_, partUrlText, _) = execute(partUrlReq, cancelToken)
                    val arr = json.parseToJsonElement(partUrlText).jsonObject["part_urls"]?.jsonArray
                    partUrl = arr?.firstOrNull {
                        it.jsonObject["partNumber"]?.jsonPrimitive?.intOrNull == partNum
                    }?.jsonObject?.get("url")?.jsonPrimitive?.contentOrNull
                }
                if (partUrl.isNullOrBlank()) throw IOException("Missing URL for part $partNum")

                val start = (partNum - 1L) * partSize
                val len = minOf(partSize, picked.size - start).coerceAtLeast(0L)
                val putPartReq = Request.Builder()
                    .url(partUrl)
                    .put(
                        streamingBody(
                            picked = picked,
                            onProgress = { pct ->
                                val overall = (((partNum - 1.0) / totalParts) * 100.0 + pct.toDouble() / totalParts).toInt()
                                onProgress(overall.coerceIn(0, 100))
                            },
                            cancelToken = cancelToken,
                            onBytesProgress = onBytesProgress,
                            rangeStart = start,
                            rangeLength = len,
                            omitContentType = true,
                        ),
                    )
                    .build()
                val (_, _, headers) = execute(putPartReq, cancelToken)
                val etag = headers["ETag"] ?: headers["etag"]
                    ?: throw IOException("Missing ETag for part $partNum")
                completedParts.add(partNum to etag)
            }

            onStatus("Assembling parts...")
            val completeBody = buildJsonObject {
                put("upload_id", JsonPrimitive(uploadId))
                put(
                    "parts",
                    JsonArray(
                        completedParts.map { (num, etag) ->
                            buildJsonObject {
                                put("partNumber", JsonPrimitive(num))
                                put("etag", JsonPrimitive(etag))
                            }
                        },
                    ),
                )
            }.toString()
            val completeReq = Request.Builder()
                .url("$origin/api/proxy/storageto/upload/complete-multipart")
                .header("Authorization", "Owner $ownerToken")
                .post(completeBody.toRequestBody(jsonMedia))
                .build()
            execute(completeReq, cancelToken)
        } else {
            throw IOException("Unsupported storage.to upload type: $uploadType")
        }

        onStatus("Finalizing upload with storage.to...")
        val confirmBody = buildJsonObject {
            put("filename", JsonPrimitive(picked.name))
            put("size", JsonPrimitive(picked.size))
            put("content_type", JsonPrimitive(cleanMime))
            put("r2_key", JsonPrimitive(r2Key))
        }.toString()
        val confirmReq = Request.Builder()
            .url("$origin/api/proxy/storageto/upload/confirm")
            .header("X-Visitor-Token", visitorToken)
            .post(confirmBody.toRequestBody(jsonMedia))
            .build()
        val (_, confirmText, _) = execute(confirmReq, cancelToken)
        val confirmObj = json.parseToJsonElement(confirmText).jsonObject
        val fileObj = confirmObj["file"]?.jsonObject
        val fileUrl = fileObj?.get("raw_url")?.jsonPrimitive?.contentOrNull
            ?: fileObj?.get("url")?.jsonPrimitive?.contentOrNull
            ?: throw IOException(confirmObj["error"]?.jsonPrimitive?.contentOrNull ?: "storage.to confirm failed")

        val fileId = fileObj?.get("id")?.jsonPrimitive?.contentOrNull
        ownerToken = confirmObj["owner_token"]?.jsonPrimitive?.contentOrNull ?: ownerToken
        val days = when (expire) {
            "24h" -> 1
            "48h" -> 2
            "72h" -> 3
            "120h" -> 5
            "168h" -> 7
            else -> 3
        }
        if (!fileId.isNullOrBlank() && ownerToken.isNotBlank() && days != 3) {
            runCatching {
                val expReq = Request.Builder()
                    .url("$origin/api/proxy/storageto/file/$fileId/expiry")
                    .header("Authorization", "Owner $ownerToken")
                    .post("""{"days":$days}""".toRequestBody(jsonMedia))
                    .build()
                HefimerApiConfig.apiClient.newCall(expReq).execute().close()
            }
        }
        return fileUrl
    }

    private fun uploadGofile(
        picked: LocalPickedFile,
        expire: String,
        cancelToken: UploadCancellationToken,
        onProgress: (Int) -> Unit,
        onBytesProgress: (Long, Long) -> Unit,
        onStatus: (String) -> Unit,
    ): String {
        onStatus("Getting Gofile server...")
        val servers = mutableListOf<String>()
        runCatching {
            val req = Request.Builder().url("https://api.gofile.io/servers").get().build()
            HefimerApiConfig.apiClient.newCall(req).execute().use { resp ->
                if (resp.isSuccessful) {
                    val obj = json.parseToJsonElement(resp.body?.string().orEmpty()).jsonObject
                    val list = obj["data"]?.jsonObject?.get("servers")?.jsonArray
                    list?.forEach { elem ->
                        elem.jsonObject["name"]?.jsonPrimitive?.contentOrNull?.let { servers.add(it) }
                    }
                }
            }
        }
        cachedGofileServer?.let { if (!servers.contains(it)) servers.add(0, it) }
        listOf("store-eu-par-3", "store1", "store3").forEach { if (!servers.contains(it)) servers.add(it) }

        onStatus("Uploading via Gofile.io...")
        var lastErr: Throwable? = null
        for (server in servers.take(3)) {
            if (cancelToken.isCancelled()) throw IOException("UPLOAD_CANCELED")
            try {
                val body = MultipartBody.Builder()
                    .setType(MultipartBody.FORM)
                    .addFormDataPart("file", picked.name, streamingBody(picked, onProgress, cancelToken, onBytesProgress))
                    .build()
                val req = Request.Builder()
                    .url("https://$server.gofile.io/contents/uploadfile")
                    .post(body)
                    .build()
                val (_, text, _) = execute(req, cancelToken)
                val obj = json.parseToJsonElement(text).jsonObject
                if (obj["status"]?.jsonPrimitive?.contentOrNull != "ok") {
                    throw IOException("Gofile upload failed")
                }
                cachedGofileServer = server
                val data = obj["data"]?.jsonObject
                val code = data?.get("parentFolderCode")?.jsonPrimitive?.contentOrNull
                    ?: data?.get("id")?.jsonPrimitive?.contentOrNull
                    ?: throw IOException("Gofile missing folder code")
                val guestToken = data?.get("guestToken")?.jsonPrimitive?.contentOrNull
                if (!guestToken.isNullOrBlank()) {
                    val expSec = (System.currentTimeMillis() + com.hefimer.app.data.model.parseExpireDurationMs(expire)) / 1000L
                    runCatching {
                        val expBody = buildJsonObject {
                            put("contentId", JsonPrimitive(code))
                            put("attribute", JsonPrimitive("expire"))
                            put("attributeValue", JsonPrimitive(expSec))
                        }.toString().toRequestBody("application/json".toMediaTypeOrNull())
                        val expReq = Request.Builder()
                            .url("https://api.gofile.io/contents")
                            .header("Authorization", "Bearer $guestToken")
                            .put(expBody)
                            .build()
                        HefimerApiConfig.apiClient.newCall(expReq).execute().close()
                    }
                }
                return "https://gofile.io/d/$code"
            } catch (e: Throwable) {
                if (cancelToken.isCancelled() || e.message == "UPLOAD_CANCELED") throw e
                lastErr = e
            }
        }
        throw lastErr ?: IOException("Gofile upload failed")
    }

    private fun uploadLitterbox(
        picked: LocalPickedFile,
        expire: String,
        cancelToken: UploadCancellationToken,
        onProgress: (Int) -> Unit,
        onBytesProgress: (Long, Long) -> Unit,
        onStatus: (String) -> Unit,
    ): String {
        onStatus("Uploading via Litterbox...")
        val allowed = setOf("1h", "12h", "24h", "72h")
        val timeVal = if (expire in allowed) expire else "24h"
        val wireName = asciiSafeWireName(picked.name)
        fun makeBody() = MultipartBody.Builder()
            .setType(MultipartBody.FORM)
            .addFormDataPart("reqtype", "fileupload")
            .addFormDataPart("time", timeVal)
            .addFormDataPart("fileToUpload", wireName, streamingBody(picked, onProgress, cancelToken, onBytesProgress))
            .build()

        val text = try {
            val req = Request.Builder()
                .url("https://litterbox.catbox.moe/resources/internals/api.php")
                .header("Accept", "*/*")
                .post(makeBody())
                .build()
            execute(req, cancelToken).second.trim()
        } catch (e: Throwable) {
            if (cancelToken.isCancelled() || e.message == "UPLOAD_CANCELED" || picked.size > 95L * 1024L * 1024L) {
                throw e
            }
            val proxyReq = Request.Builder()
                .url("$origin/api/proxy/litterbox")
                .post(makeBody())
                .build()
            execute(proxyReq, cancelToken).second.trim()
        }
        if (!text.startsWith("https://")) throw IOException(text.ifBlank { "Litterbox upload failed" })
        return text
    }

    private fun uploadVikingFile(
        picked: LocalPickedFile,
        cancelToken: UploadCancellationToken,
        onProgress: (Int) -> Unit,
        onBytesProgress: (Long, Long) -> Unit,
        onStatus: (String) -> Unit,
    ): String {
        onStatus("Initializing upload with VikingFile...")
        val initReq = Request.Builder()
            .url("$origin/api/proxy/vikingfile/get-upload-url")
            .post(FormBody.Builder().add("size", picked.size.toString()).build())
            .build()
        val (_, initText, _) = execute(initReq, cancelToken)
        val initObj = json.parseToJsonElement(initText).jsonObject
        val uploadId = initObj["uploadId"]?.jsonPrimitive?.contentOrNull.orEmpty()
        val key = initObj["key"]?.jsonPrimitive?.contentOrNull.orEmpty()
        val partSize = initObj["partSize"]?.jsonPrimitive?.longOrNull ?: picked.size
        val urls = initObj["urls"]?.jsonArray?.mapNotNull { it.jsonPrimitive.contentOrNull }.orEmpty()
        if (uploadId.isBlank() || key.isBlank() || urls.isEmpty()) {
            throw IOException("VikingFile returned invalid multipart metadata")
        }

        onStatus(if (urls.size > 1) "Uploading ${urls.size} parts to VikingFile..." else "Uploading via VikingFile...")
        val parts = mutableListOf<Pair<Int, String>>()
        urls.forEachIndexed { idx, partUrl ->
            if (cancelToken.isCancelled()) throw IOException("UPLOAD_CANCELED")
            val partNum = idx + 1
            val start = idx.toLong() * partSize
            val len = minOf(partSize, picked.size - start).coerceAtLeast(0L)
            val putReq = Request.Builder()
                .url(partUrl)
                .put(
                    streamingBody(
                        picked = picked,
                        onProgress = { pct ->
                            val overall = (((idx.toDouble()) / urls.size) * 100.0 + pct.toDouble() / urls.size).toInt()
                            onProgress(overall.coerceIn(0, 100))
                        },
                        cancelToken = cancelToken,
                        onBytesProgress = onBytesProgress,
                        rangeStart = start,
                        rangeLength = len,
                        omitContentType = true,
                    ),
                )
                .build()
            val (_, _, headers) = execute(putReq, cancelToken)
            val etag = (headers["ETag"] ?: headers["etag"])?.replace("\"", "")
                ?: throw IOException("VikingFile part $partNum missing ETag")
            parts.add(partNum to etag)
        }

        onStatus("Finalizing VikingFile upload...")
        val form = FormBody.Builder()
            .add("key", key)
            .add("uploadId", uploadId)
            .add("name", picked.name)
            .add("user", "")
        parts.forEachIndexed { idx, (num, etag) ->
            form.add("parts[$idx][PartNumber]", num.toString())
            form.add("parts[$idx][ETag]", etag)
        }
        val completeReq = Request.Builder()
            .url("$origin/api/proxy/vikingfile/complete-upload")
            .post(form.build())
            .build()
        val (_, completeText, _) = execute(completeReq, cancelToken)
        val obj = json.parseToJsonElement(completeText).jsonObject
        return obj["url"]?.jsonPrimitive?.contentOrNull
            ?: throw IOException(obj["error"]?.jsonPrimitive?.contentOrNull ?: "VikingFile complete failed")
    }

    private fun uploadTmpfileLink(
        picked: LocalPickedFile,
        cancelToken: UploadCancellationToken,
        onProgress: (Int) -> Unit,
        onBytesProgress: (Long, Long) -> Unit,
        onStatus: (String) -> Unit,
    ): String {
        onStatus("Uploading via tfLink...")
        val safeName = asciiSafeWireName(picked.name)
        fun makeBody() = MultipartBody.Builder()
            .setType(MultipartBody.FORM)
            .addFormDataPart("file", safeName, streamingBody(picked, onProgress, cancelToken, onBytesProgress))
            .build()
        val text = try {
            execute(Request.Builder().url("https://tmpfile.link/api/upload").post(makeBody()).build(), cancelToken).second
        } catch (e: Throwable) {
            if (cancelToken.isCancelled() || e.message == "UPLOAD_CANCELED") throw e
            execute(Request.Builder().url("$origin/api/proxy/tmpfilelink").post(makeBody()).build(), cancelToken).second
        }
        val obj = json.parseToJsonElement(text).jsonObject
        return obj["downloadLink"]?.jsonPrimitive?.contentOrNull
            ?: obj["downloadLinkEncoded"]?.jsonPrimitive?.contentOrNull
            ?: throw IOException("tfLink upload failed")
    }

    private fun uploadTempFile(
        picked: LocalPickedFile,
        expire: String,
        cancelToken: UploadCancellationToken,
        onProgress: (Int) -> Unit,
        onBytesProgress: (Long, Long) -> Unit,
        onStatus: (String) -> Unit,
    ): String {
        onStatus("Uploading via TempFile...")
        val hours = Regex("""\d+""").find(expire)?.value?.toIntOrNull() ?: 24
        val validHours = if (hours in listOf(1, 6, 24, 48)) hours else 24
        val safeName = asciiSafeWireName(picked.name)
        fun makeBody() = MultipartBody.Builder()
            .setType(MultipartBody.FORM)
            .addFormDataPart("files", safeName, streamingBody(picked, onProgress, cancelToken, onBytesProgress))
            .addFormDataPart("expiryHours", validHours.toString())
            .build()

        val text = try {
            execute(Request.Builder().url("https://tempfile.org/api/upload/local").post(makeBody()).build(), cancelToken).second
        } catch (e: Throwable) {
            if (cancelToken.isCancelled() || e.message == "UPLOAD_CANCELED") throw e
            execute(Request.Builder().url("$origin/api/proxy/tempfile").post(makeBody()).build(), cancelToken).second
        }
        val obj = json.parseToJsonElement(text).jsonObject
        val url = obj["files"]?.jsonArray?.firstOrNull()?.jsonObject?.get("url")?.jsonPrimitive?.contentOrNull
        if (obj["success"]?.jsonPrimitive?.content != "true" || url.isNullOrBlank()) {
            throw IOException(obj["message"]?.jsonPrimitive?.contentOrNull ?: "TempFile upload failed")
        }
        return "${url}download"
    }

    private fun uploadFilebin(
        picked: LocalPickedFile,
        cancelToken: UploadCancellationToken,
        onProgress: (Int) -> Unit,
        onBytesProgress: (Long, Long) -> Unit,
        onStatus: (String) -> Unit,
    ): String {
        onStatus("Uploading via Filebin...")
        val bin = "hefimer-${UUID.randomUUID().toString().replace("-", "").take(20)}"
        val safeWireName = asciiSafeWireName(picked.name)
        val encodedName = encodePathSeg(safeWireName)
        val text = try {
            val directReq = Request.Builder()
                .url("https://filebin.net/$bin/$encodedName")
                .header("Accept", "application/json")
                .post(streamingBody(picked, onProgress, cancelToken, onBytesProgress))
                .build()
            execute(directReq, cancelToken).second
        } catch (e: Throwable) {
            if (cancelToken.isCancelled() || e.message == "UPLOAD_CANCELED") throw e
            val proxyReq = Request.Builder()
                .url("$origin/api/proxy/filebin/$bin/$encodedName")
                .post(streamingBody(picked, onProgress, cancelToken, onBytesProgress))
                .build()
            execute(proxyReq, cancelToken).second
        }
        val returnedName = runCatching {
            json.parseToJsonElement(text).jsonObject["file"]?.jsonObject?.get("filename")?.jsonPrimitive?.contentOrNull
        }.getOrNull() ?: safeWireName
        return "https://filebin.net/$bin/${encodePathSeg(returnedName)}"
    }

    private fun uploadTempSh(
        picked: LocalPickedFile,
        cancelToken: UploadCancellationToken,
        onProgress: (Int) -> Unit,
        onBytesProgress: (Long, Long) -> Unit,
        onStatus: (String) -> Unit,
    ): String {
        onStatus("Uploading via temp.sh...")
        val safeName = asciiSafeWireName(picked.name)
        fun makeBody() = MultipartBody.Builder()
            .setType(MultipartBody.FORM)
            .addFormDataPart("file", safeName, streamingBody(picked, onProgress, cancelToken, onBytesProgress))
            .build()
        val text = try {
            execute(Request.Builder().url("https://temp.sh/upload").post(makeBody()).build(), cancelToken).second
        } catch (e: Throwable) {
            if (cancelToken.isCancelled() || e.message == "UPLOAD_CANCELED") throw e
            execute(Request.Builder().url("$origin/api/proxy/tempsh").post(makeBody()).build(), cancelToken).second
        }
        val trimmed = text.trim()
        if (!trimmed.startsWith("https://temp.sh/")) throw IOException("temp.sh upload failed")
        return trimmed
    }

    private fun uploadTmpfiles(
        picked: LocalPickedFile,
        cancelToken: UploadCancellationToken,
        onProgress: (Int) -> Unit,
        onBytesProgress: (Long, Long) -> Unit,
        onStatus: (String) -> Unit,
    ): String {
        onStatus("Uploading via tmpfiles.org...")
        val safeName = asciiSafeWireName(picked.name)
        fun makeBody() = MultipartBody.Builder()
            .setType(MultipartBody.FORM)
            .addFormDataPart("file", safeName, streamingBody(picked, onProgress, cancelToken, onBytesProgress))
            .build()
        val text = try {
            execute(Request.Builder().url("https://tmpfiles.org/api/v1/upload").post(makeBody()).build(), cancelToken).second
        } catch (e: Throwable) {
            if (cancelToken.isCancelled() || e.message == "UPLOAD_CANCELED") throw e
            execute(Request.Builder().url("$origin/api/proxy/tmpfiles").post(makeBody()).build(), cancelToken).second
        }
        val obj = json.parseToJsonElement(text).jsonObject
        val url = obj["data"]?.jsonObject?.get("url")?.jsonPrimitive?.contentOrNull
        if (obj["status"]?.jsonPrimitive?.contentOrNull == "success" && !url.isNullOrBlank()) return url
        throw IOException(obj["error"]?.jsonPrimitive?.contentOrNull ?: "tmpfiles upload failed")
    }

    private fun uploadUguu(
        picked: LocalPickedFile,
        cancelToken: UploadCancellationToken,
        onProgress: (Int) -> Unit,
        onBytesProgress: (Long, Long) -> Unit,
        onStatus: (String) -> Unit,
    ): String {
        onStatus("Uploading via Uguu.se...")
        val safeName = asciiSafeWireName(picked.name)
        fun makeBody() = MultipartBody.Builder()
            .setType(MultipartBody.FORM)
            .addFormDataPart("files[]", safeName, streamingBody(picked, onProgress, cancelToken, onBytesProgress))
            .build()
        val text = try {
            execute(Request.Builder().url("https://uguu.se/upload").post(makeBody()).build(), cancelToken).second
        } catch (e: Throwable) {
            if (cancelToken.isCancelled() || e.message == "UPLOAD_CANCELED") throw e
            execute(Request.Builder().url("$origin/api/proxy/uguu").post(makeBody()).build(), cancelToken).second
        }
        val obj = json.parseToJsonElement(text).jsonObject
        val url = obj["files"]?.jsonArray?.firstOrNull()?.jsonObject?.get("url")?.jsonPrimitive?.contentOrNull
        if (obj["success"]?.jsonPrimitive?.content == "true" && !url.isNullOrBlank()) return url
        throw IOException("Uguu.se upload failed")
    }

    private fun uploadPutIcu(
        picked: LocalPickedFile,
        expire: String,
        cancelToken: UploadCancellationToken,
        onProgress: (Int) -> Unit,
        onBytesProgress: (Long, Long) -> Unit,
        onStatus: (String) -> Unit,
    ): String {
        onStatus("Uploading via Put.icu...")
        val sec = (com.hefimer.app.data.model.parseExpireDurationMs(expire) / 1000L).coerceIn(60L, 86400L)
        val req = Request.Builder()
            .url("$origin/api/proxy/puticu/${encodePathSeg(asciiSafeWireName(picked.name))}")
            .header("Accept", "application/json")
            .header("Linx-Expiry", sec.toString())
            .put(streamingBody(picked, onProgress, cancelToken, onBytesProgress))
            .build()
        val (_, text, _) = execute(req, cancelToken)
        val obj = json.parseToJsonElement(text).jsonObject
        return obj["direct_url"]?.jsonPrimitive?.contentOrNull
            ?: obj["url"]?.jsonPrimitive?.contentOrNull
            ?: throw IOException("Put.icu upload failed")
    }
}
