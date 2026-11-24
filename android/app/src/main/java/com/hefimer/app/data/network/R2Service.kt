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

class R2Service(private val context: Context) {
    private val origin = HefimerApiConfig.SITE_ORIGIN
    private val apiClient = HefimerApiConfig.apiClient
    private val uploadClient = HefimerApiConfig.uploadClient
    private val json = HefimerApiConfig.json

    suspend fun uploadToR2(
        picked: LocalPickedFile,
        customName: String? = null,
        cancelToken: UploadCancellationToken,
        onProgress: (Int) -> Unit,
    ): String = withContext(Dispatchers.IO) {
        val finalName = customName?.takeIf { it.isNotBlank() } ?: picked.name
        val finalType = picked.mimeType.ifBlank { "application/octet-stream" }
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

        val body = object : RequestBody() {
            override fun contentType(): MediaType? = finalType.toMediaTypeOrNull()
            override fun contentLength(): Long = if (picked.size > 0L) picked.size else -1L
            override fun writeTo(sink: BufferedSink) {
                val total = picked.size.coerceAtLeast(1L)
                var uploaded = 0L
                var lastPct = -1
                val stream = if (picked.tempFile != null && picked.tempFile.exists()) {
                    picked.tempFile.inputStream()
                } else {
                    context.contentResolver.openInputStream(picked.uri)
                        ?: throw IOException("Cannot read selected file")
                }
                stream.use { input ->
                    val buf = ByteArray(32 * 1024)
                    while (true) {
                        if (cancelToken.isCancelled()) throw IOException("UPLOAD_CANCELED")
                        val read = input.read(buf)
                        if (read <= 0) break
                        sink.write(buf, 0, read)
                        uploaded += read
                        val pct = ((uploaded * 100L) / total).toInt().coerceIn(0, 100)
                        if (pct != lastPct) {
                            lastPct = pct
                            onProgress(pct)
                        }
                    }
                }
            }
        }

        val putReq = Request.Builder()
            .url(uploadUrl)
            .header("Content-Type", finalType)
            .put(body)
            .build()
        val call = uploadClient.newCall(putReq)
        cancelToken.bindCall(call)
        try {
            call.execute().use { resp ->
                if (!resp.isSuccessful) {
                    throw IOException("R2 returned status ${resp.code}")
                }
            }
        } finally {
            cancelToken.clearCall(call)
        }
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
