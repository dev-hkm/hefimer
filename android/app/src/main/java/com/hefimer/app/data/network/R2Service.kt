package com.hefimer.app.data.network

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import okhttp3.MediaType
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.Request
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import okio.BufferedSink
import java.io.IOException
import java.net.URLEncoder
import java.text.Normalizer

class R2Service(private val context: Context) {
    private val origin = HefimerApiConfig.SITE_ORIGIN
    private val apiClient = HefimerApiConfig.apiClient
    private val uploadClient = HefimerApiConfig.uploadClient
    private val json = HefimerApiConfig.json

    private val safeContentTypeRegex =
        Regex("""^[a-z][a-z0-9!#$&^_.+-]{0,126}/[a-z0-9!#$&^_.+-]+$""", RegexOption.IGNORE_CASE)

    private fun sanitizeR2Filename(raw: String): String {
        val normalized = Normalizer.normalize(raw, Normalizer.Form.NFKC).trim()
            .replace("..", "_")
            .replace(Regex("""[\\/\u0000-\u001f\u007f]"""), "_")
            .ifBlank { "file_${System.currentTimeMillis()}" }
        return if (normalized.length <= 160) normalized else {
            val ext = normalized.substringAfterLast('.', "").take(16)
            if (ext.isNotEmpty() && normalized.contains('.')) {
                normalized.take(158 - ext.length) + "." + ext
            } else {
                normalized.take(160)
            }
        }
    }

    private fun sanitizeR2ContentType(raw: String?): String {
        val base = raw.orEmpty().substringBefore(';').trim()
        return if (safeContentTypeRegex.matches(base)) base else "application/octet-stream"
    }

    suspend fun uploadToR2(
        picked: LocalPickedFile,
        customName: String? = null,
        cancelToken: UploadCancellationToken,
        onProgress: (loadedBytes: Long, totalBytes: Long, percent: Int) -> Unit,
    ): String = withContext(Dispatchers.IO) {
        val finalName = sanitizeR2Filename(customName?.takeIf { it.isNotBlank() } ?: picked.name)
        val finalType = sanitizeR2ContentType(picked.mimeType)
        val encodedName = URLEncoder.encode(finalName, "UTF-8")
        val encodedType = URLEncoder.encode(finalType, "UTF-8")

        val presignReq = Request.Builder()
            .url("$origin/api/r2/upload-url?filename=$encodedName&contentType=$encodedType")
            .header("Cache-Control", "no-store")
            .get()
            .build()

        val (uploadUrl, objectKey) = apiClient.newCall(presignReq).execute().use { resp ->
            val text = resp.body?.string().orEmpty()
            if (!resp.isSuccessful) {
                val msg = runCatching {
                    json.parseToJsonElement(text).jsonObject["error"]?.jsonPrimitive?.contentOrNull
                }.getOrNull()
                throw IOException(msg ?: "Failed to prepare Secret R2 upload (${resp.code})")
            }
            val obj = json.parseToJsonElement(text).jsonObject
            val url = obj["url"]?.jsonPrimitive?.contentOrNull
                ?: throw IOException("Missing presigned R2 URL")
            val key = obj["objectKey"]?.jsonPrimitive?.contentOrNull
                ?: throw IOException("Missing R2 objectKey")
            url to key
        }

        // Exact MediaType matching SigV4 signed Content-Type without extra charset parameter
        val mediaType = finalType.toMediaTypeOrNull()

        val body = object : RequestBody() {
            override fun contentType(): MediaType? = mediaType
            override fun contentLength(): Long = if (picked.size > 0L) picked.size else -1L
            override fun writeTo(sink: BufferedSink) {
                val total = picked.size.coerceAtLeast(1L)
                var uploaded = 0L
                var lastEmitNs = 0L
                val stream = if (picked.tempFile != null && picked.tempFile.exists()) {
                    picked.tempFile.inputStream()
                } else {
                    context.contentResolver.openInputStream(picked.uri)
                        ?: throw IOException("Cannot read selected file")
                }
                stream.use { input ->
                    val buf = ByteArray(64 * 1024)
                    while (true) {
                        if (cancelToken.isCancelled()) throw IOException("UPLOAD_CANCELED")
                        val read = input.read(buf)
                        if (read <= 0) break
                        sink.write(buf, 0, read)
                        uploaded += read
                        val now = System.nanoTime()
                        if (now - lastEmitNs >= 32_000_000L || uploaded >= total) {
                            lastEmitNs = now
                            val pct = ((uploaded * 100L) / total).toInt().coerceIn(0, 100)
                            onProgress(uploaded, total, pct)
                        }
                    }
                }
            }
        }

        val putReq = Request.Builder()
            .url(uploadUrl)
            .put(body)
            .build()
        val call = uploadClient.newCall(putReq)
        cancelToken.bindCall(call)
        try {
            call.execute().use { resp ->
                if (!resp.isSuccessful) {
                    val err = resp.body?.string().orEmpty().take(160)
                    throw IOException("R2 returned status ${resp.code}${if (err.isNotBlank()) ": $err" else ""}")
                }
            }
        } finally {
            cancelToken.clearCall(call)
        }
        onProgress(picked.size, picked.size.coerceAtLeast(1L), 100)
        objectKey
    }

    suspend fun getDownloadUrl(objectKey: String): String = withContext(Dispatchers.IO) {
        val encoded = URLEncoder.encode(objectKey, "UTF-8")
        val apiUrl = "$origin/api/r2/download-url?objectKey=$encoded"
        val req = Request.Builder().url(apiUrl).get().build()
        apiClient.newCall(req).execute().use { resp ->
            val ct = resp.header("Content-Type").orEmpty()
            if (ct.contains("application/json", ignoreCase = true)) {
                val text = resp.body?.string().orEmpty()
                val obj = json.parseToJsonElement(text).jsonObject
                if (!resp.isSuccessful) {
                    throw IOException(obj["error"]?.jsonPrimitive?.contentOrNull ?: "Server returned ${resp.code}")
                }
                obj["url"]?.jsonPrimitive?.contentOrNull
                    ?: throw IOException("No R2 download URL returned")
            } else {
                if (!resp.isSuccessful) throw IOException("Server returned ${resp.code}")
                apiUrl
            }
        }
    }

    suspend fun deleteObject(objectKey: String) = withContext(Dispatchers.IO) {
        if (objectKey.isBlank()) return@withContext
        val payload = """{"objectKey":"${objectKey.replace("\"", "\\\"")}"}"""
        val req = Request.Builder()
            .url("$origin/api/r2/delete")
            .post(payload.toRequestBody("application/json; charset=utf-8".toMediaTypeOrNull()))
            .build()
        apiClient.newCall(req).execute().close()
    }
}
