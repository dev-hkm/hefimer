package com.hefimer.app.data.network

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import okhttp3.sse.EventSource
import okhttp3.sse.EventSourceListener
import okhttp3.sse.EventSources
import java.io.IOException

/**
 * Lightweight, full-featured Firebase Realtime Database REST + Server-Sent Events (SSE) client.
 * Connects directly to Hefimer's live production RTDB without Play Services dependency.
 */
class FirebaseRtdbClient(
    private val baseUrl: String = HefimerApiConfig.RTDB_BASE_URL,
    private val apiClient: OkHttpClient = HefimerApiConfig.apiClient,
    private val sseClient: OkHttpClient = HefimerApiConfig.sseClient,
    private val json: Json = HefimerApiConfig.json,
) {
    private val jsonMediaType = "application/json; charset=utf-8".toMediaType()

    private fun pathUrl(path: String, query: String? = null): String {
        val clean = path.trim('/')
        val base = "$baseUrl/$clean.json"
        return if (query.isNullOrBlank()) base else "$base?$query"
    }

    suspend fun get(path: String, query: String? = null): JsonElement? = withContext(Dispatchers.IO) {
        val req = Request.Builder()
            .url(pathUrl(path, query))
            .get()
            .build()
        apiClient.newCall(req).execute().use { resp ->
            val body = resp.body?.string().orEmpty()
            if (!resp.isSuccessful) {
                throw IOException(
                    com.hefimer.app.ui.theme.tr(
                        "RTDB GET failed (${resp.code}): ${body.take(120)}",
                        "Lỗi tải dữ liệu RTDB (${resp.code}): ${body.take(120)}",
                    ),
                )
            }
            if (body.isBlank() || body == "null") return@withContext null
            val parsed = json.parseToJsonElement(body)
            if (parsed is JsonNull) null else parsed
        }
    }

    suspend fun set(path: String, payload: JsonElement): JsonElement? = withContext(Dispatchers.IO) {
        val bodyStr = json.encodeToString(JsonElement.serializer(), payload)
        val req = Request.Builder()
            .url(pathUrl(path))
            .put(bodyStr.toRequestBody(jsonMediaType))
            .build()
        apiClient.newCall(req).execute().use { resp ->
            val body = resp.body?.string().orEmpty()
            if (!resp.isSuccessful) {
                throw IOException(
                    com.hefimer.app.ui.theme.tr(
                        "RTDB PUT failed (${resp.code}): ${body.take(120)}",
                        "Lỗi ghi dữ liệu RTDB (${resp.code}): ${body.take(120)}",
                    ),
                )
            }
            if (body.isBlank() || body == "null") null else json.parseToJsonElement(body)
        }
    }

    suspend fun update(path: String, patch: JsonObject): JsonElement? = withContext(Dispatchers.IO) {
        val bodyStr = json.encodeToString(JsonObject.serializer(), patch)
        val req = Request.Builder()
            .url(pathUrl(path))
            .patch(bodyStr.toRequestBody(jsonMediaType))
            .build()
        apiClient.newCall(req).execute().use { resp ->
            val body = resp.body?.string().orEmpty()
            if (!resp.isSuccessful) {
                throw IOException(
                    com.hefimer.app.ui.theme.tr(
                        "RTDB PATCH failed (${resp.code}): ${body.take(120)}",
                        "Lỗi cập nhật RTDB (${resp.code}): ${body.take(120)}",
                    ),
                )
            }
            if (body.isBlank() || body == "null") null else json.parseToJsonElement(body)
        }
    }

    suspend fun push(path: String, payload: JsonElement): String = withContext(Dispatchers.IO) {
        val bodyStr = json.encodeToString(JsonElement.serializer(), payload)
        val req = Request.Builder()
            .url(pathUrl(path))
            .post(bodyStr.toRequestBody(jsonMediaType))
            .build()
        apiClient.newCall(req).execute().use { resp ->
            val body = resp.body?.string().orEmpty()
            if (!resp.isSuccessful) {
                throw IOException(
                    com.hefimer.app.ui.theme.tr(
                        "RTDB POST failed (${resp.code}): ${body.take(120)}",
                        "Lỗi gửi dữ liệu RTDB (${resp.code}): ${body.take(120)}",
                    ),
                )
            }
            val parsed = json.parseToJsonElement(body)
            parsed.jsonObject["name"]?.jsonPrimitive?.content.orEmpty()
        }
    }

    suspend fun remove(path: String) = withContext(Dispatchers.IO) {
        val req = Request.Builder()
            .url(pathUrl(path))
            .delete()
            .build()
        apiClient.newCall(req).execute().use { resp ->
            if (!resp.isSuccessful) {
                val body = resp.body?.string().orEmpty()
                throw IOException(
                    com.hefimer.app.ui.theme.tr(
                        "RTDB DELETE failed (${resp.code}): ${body.take(120)}",
                        "Lỗi xóa dữ liệu RTDB (${resp.code}): ${body.take(120)}",
                    ),
                )
            }
        }
    }

    suspend fun incrementInt(path: String, delta: Int = 1): Int = withContext(Dispatchers.IO) {
        val url = pathUrl(path)
        val maxAttempts = 5
        for (attempt in 1..maxAttempts) {
            val getReq = Request.Builder()
                .url(url)
                .header("X-Firebase-ETag", "true")
                .get()
                .build()
            val (current, etag) = apiClient.newCall(getReq).execute().use { resp ->
                val body = resp.body?.string().orEmpty()
                if (!resp.isSuccessful) {
                    throw IOException(
                        com.hefimer.app.ui.theme.tr(
                            "RTDB GET failed (${resp.code}): ${body.take(120)}",
                            "Lỗi tải dữ liệu RTDB (${resp.code}): ${body.take(120)}",
                        ),
                    )
                }
                val value = if (body.isBlank() || body == "null") {
                    0
                } else {
                    runCatching { json.parseToJsonElement(body).jsonPrimitive.content.toIntOrNull() ?: 0 }
                        .getOrDefault(0)
                }
                value to resp.header("ETag")
            }
            val next = current + delta
            val putBody = json.encodeToString(JsonElement.serializer(), JsonPrimitive(next))
            val putReqBuilder = Request.Builder()
                .url(url)
                .put(putBody.toRequestBody(jsonMediaType))
            if (!etag.isNullOrBlank()) {
                putReqBuilder.header("If-Match", etag)
            }
            apiClient.newCall(putReqBuilder.build()).execute().use { resp ->
                if (resp.code == 412) {
                    if (attempt == maxAttempts) {
                        throw IOException(
                            com.hefimer.app.ui.theme.tr(
                                "RTDB atomic increment failed after $maxAttempts retries",
                                "Cập nhật bộ đếm RTDB thất bại sau $maxAttempts lần thử",
                            ),
                        )
                    }
                    return@use
                }
                val body = resp.body?.string().orEmpty()
                if (!resp.isSuccessful) {
                    throw IOException(
                        com.hefimer.app.ui.theme.tr(
                            "RTDB PUT failed (${resp.code}): ${body.take(120)}",
                            "Lỗi ghi dữ liệu RTDB (${resp.code}): ${body.take(120)}",
                        ),
                    )
                }
                return@withContext next
            }
        }
        throw IOException(
            com.hefimer.app.ui.theme.tr(
                "RTDB atomic increment failed",
                "Cập nhật bộ đếm RTDB thất bại",
            ),
        )
    }

    /**
     * Real-time Firebase SSE observer that maintains an exact local mirror of the RTDB subtree,
     * applying `put` and `patch` events incrementally and auto-reconnecting on drop.
     */
    fun observe(path: String): Flow<JsonElement?> = callbackFlow {
        val factory = EventSources.createFactory(sseClient)
        var currentTree: JsonElement? = null
        var activeSource: EventSource? = null
        var reconnectDelayMs = 2_000L
        var closed = false

        fun startStream() {
            if (closed || !isActive) return
            activeSource?.cancel()
            val request = Request.Builder()
                .url(pathUrl(path))
                .header("Accept", "text/event-stream")
                .header("Cache-Control", "no-cache")
                .build()

            activeSource = factory.newEventSource(
                request,
                object : EventSourceListener() {
                    override fun onOpen(eventSource: EventSource, response: Response) {
                        if (eventSource === activeSource) {
                            reconnectDelayMs = 2_000L
                        }
                    }

                    override fun onEvent(
                        eventSource: EventSource,
                        id: String?,
                        type: String?,
                        data: String,
                    ) {
                        if (closed) return
                        when (type) {
                            "put" -> {
                                runCatching {
                                    val obj = json.parseToJsonElement(data).jsonObject
                                    val relPath = obj["path"]?.jsonPrimitive?.content ?: "/"
                                    val value = obj["data"] ?: JsonNull
                                    currentTree = applyPut(currentTree, relPath, value)
                                    trySend(currentTree)
                                }
                            }
                            "patch" -> {
                                runCatching {
                                    val obj = json.parseToJsonElement(data).jsonObject
                                    val relPath = obj["path"]?.jsonPrimitive?.content ?: "/"
                                    val patchObj = obj["data"]?.jsonObject ?: return
                                    currentTree = applyPatch(currentTree, relPath, patchObj)
                                    trySend(currentTree)
                                }
                            }
                            "cancel", "auth_revoked" -> {
                                eventSource.cancel()
                            }
                        }
                    }

                    override fun onClosed(eventSource: EventSource) {
                        if (!closed && isActive && eventSource === activeSource) {
                            val waitMs = reconnectDelayMs
                            reconnectDelayMs = (reconnectDelayMs * 2L).coerceAtMost(16_000L)
                            launch(Dispatchers.IO) {
                                delay(waitMs)
                                startStream()
                            }
                        }
                    }

                    override fun onFailure(
                        eventSource: EventSource,
                        t: Throwable?,
                        response: Response?,
                    ) {
                        if (!closed && isActive && eventSource === activeSource) {
                            val waitMs = reconnectDelayMs
                            reconnectDelayMs = (reconnectDelayMs * 2L).coerceAtMost(16_000L)
                            launch(Dispatchers.IO) {
                                delay(waitMs)
                                startStream()
                            }
                        }
                    }
                },
            )
        }

        launch(Dispatchers.IO) {
            // Immediate initial snapshot fallback before SSE delivers if slow
            runCatching {
                val initial = get(path)
                if (currentTree == null) {
                    currentTree = initial
                    trySend(currentTree)
                }
            }
            startStream()
        }

        awaitClose {
            closed = true
            activeSource?.cancel()
        }
    }

    companion object {
        fun serverTimestamp(): JsonObject = buildJsonObject {
            put(".sv", JsonPrimitive("timestamp"))
        }

        internal fun applyPut(root: JsonElement?, path: String, value: JsonElement): JsonElement? {
            val segments = path.split('/').filter { it.isNotEmpty() }
            if (segments.isEmpty()) {
                return if (value is JsonNull) null else value
            }
            return putAtPath(root, segments, 0, value)
        }

        private fun putAtPath(
            node: JsonElement?,
            segments: List<String>,
            index: Int,
            value: JsonElement,
        ): JsonElement {
            val map = (node as? JsonObject)?.toMutableMap() ?: mutableMapOf()
            val key = segments[index]
            if (index == segments.lastIndex) {
                if (value is JsonNull) {
                    map.remove(key)
                } else {
                    map[key] = value
                }
            } else {
                map[key] = putAtPath(map[key], segments, index + 1, value)
            }
            return JsonObject(map)
        }

        internal fun applyPatch(root: JsonElement?, path: String, patch: JsonObject): JsonElement? {
            var current = root
            val base = path.trimEnd('/')
            for ((k, v) in patch) {
                val full = if (base.isEmpty()) "/$k" else "$base/$k"
                current = applyPut(current, full, v)
            }
            return current
        }
    }
}
