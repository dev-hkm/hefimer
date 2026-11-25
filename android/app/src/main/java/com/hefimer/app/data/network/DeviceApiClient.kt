package com.hefimer.app.data.network

import com.hefimer.app.data.local.HefimerStorage
import com.hefimer.app.data.model.DeviceSync
import com.hefimer.app.data.model.DropCreatedOffer
import com.hefimer.app.data.model.InviteResponse
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException

/**
 * Complete Hefimer Link API client communicating with https://hefimer.qzz.io/api/devices endpoints
 * using signed ECDSA P-256 requests.
 */
class DeviceApiClient(private val storage: HefimerStorage) {
    private val apiRoot = "${HefimerApiConfig.SITE_ORIGIN}/api/devices"
    private val client = HefimerApiConfig.apiClient
    private val json = HefimerApiConfig.json
    private val jsonMedia = "application/json; charset=utf-8".toMediaType()
    private val registerMutex = Mutex()
    private var registered = false

    suspend fun ensureRegistered(): LocalDeviceIdentity = registerMutex.withLock {
        val identity = DeviceCrypto.getOrCreateIdentity(storage)
        if (!registered) {
            val payload = buildJsonObject {
                put("id", JsonPrimitive(identity.id))
                put("publicKey", JsonPrimitive(identity.publicKeySpkiBase64Url))
                put("name", JsonPrimitive(identity.name))
                put("platform", JsonPrimitive(identity.platform))
            }.toString()
            requestRaw("POST", "/register", payload, signed = false)
            registered = true
        }
        identity
    }

    private suspend fun requestRaw(
        method: String,
        path: String,
        body: String = "",
        signed: Boolean = true,
        allowAuthRetry: Boolean = true,
    ): String = withContext(Dispatchers.IO) {
        val fullUrl = "$apiRoot$path"
        val reqBuilder = Request.Builder().url(fullUrl)
            .header("User-Agent", HefimerApiConfig.USER_AGENT)

        if (signed) {
            val identity = ensureRegistered()
            val timestamp = System.currentTimeMillis().toString()
            val nonce = DeviceCrypto.randomToken(18)
            val pathAndQuery = "/api/devices$path"
            val bodyHash = DeviceCrypto.sha256Base64Url(body)
            val signPayload = DeviceCrypto.buildSignaturePayload(
                timestamp = timestamp,
                nonce = nonce,
                method = method,
                pathAndQuery = pathAndQuery,
                bodyHash = bodyHash,
            )
            val signature = DeviceCrypto.signP1363Base64Url(identity.privateKey, signPayload)
            reqBuilder
                .header("X-Hefimer-Device", identity.id)
                .header("X-Hefimer-Time", timestamp)
                .header("X-Hefimer-Nonce", nonce)
                .header("X-Hefimer-Signature", signature)
        }

        val reqBody = if (method == "GET") null else body.toRequestBody(jsonMedia)
        reqBuilder.method(method, reqBody)

        var shouldRetryAuth = false
        val responseText = client.newCall(reqBuilder.build()).execute().use { resp ->
            val text = resp.body?.string().orEmpty()
            if (!resp.isSuccessful) {
                if (resp.code == 401 && signed && allowAuthRetry) {
                    registered = false
                    shouldRetryAuth = true
                    ""
                } else {
                    val msg = runCatching {
                        json.parseToJsonElement(text).jsonObject["error"]?.jsonPrimitive?.contentOrNull
                    }.getOrNull()
                    if (resp.code == 401) registered = false
                    throw IOException(
                        msg ?: com.hefimer.app.ui.theme.tr(
                            "Device service returned ${resp.code}",
                            "Dịch vụ thiết bị phản hồi mã ${resp.code}",
                        ),
                    )
                }
            } else {
                text
            }
        }
        if (shouldRetryAuth) {
            ensureRegistered()
            requestRaw(method, path, body, signed = true, allowAuthRetry = false)
        } else {
            responseText
        }
    }

    suspend fun sync(): DeviceSync {
        val text = requestRaw("GET", "/sync")
        return json.decodeFromString(DeviceSync.serializer(), text)
    }

    suspend fun rename(name: String): String {
        val payload = buildJsonObject { put("name", JsonPrimitive(name.trim())) }.toString()
        val text = requestRaw("PATCH", "/device", payload)
        val resultName = runCatching {
            json.parseToJsonElement(text).jsonObject["name"]?.jsonPrimitive?.contentOrNull
        }.getOrNull() ?: name.trim()
        storage.setDeviceName(resultName)
        registered = false
        return resultName
    }

    suspend fun createGroup(name: String, duration: String) {
        val payload = buildJsonObject {
            put("name", JsonPrimitive(name.trim()))
            put("duration", JsonPrimitive(duration))
        }.toString()
        requestRaw("POST", "/groups", payload)
    }

    suspend fun createInvite(): InviteResponse {
        val text = requestRaw("POST", "/groups/invite", "{}")
        return json.decodeFromString(InviteResponse.serializer(), text)
    }

    suspend fun joinGroup(token: String) {
        val payload = buildJsonObject { put("token", JsonPrimitive(token.trim())) }.toString()
        requestRaw("POST", "/groups/join", payload)
    }

    suspend fun setAutoApprove(autoApprove: Boolean) {
        val payload = buildJsonObject { put("autoApprove", JsonPrimitive(autoApprove)) }.toString()
        requestRaw("PATCH", "/groups/settings", payload)
    }

    suspend fun removeDevice(deviceId: String) {
        val payload = buildJsonObject { put("deviceId", JsonPrimitive(deviceId)) }.toString()
        requestRaw("POST", "/groups/remove", payload)
    }

    suspend fun leaveGroup() {
        requestRaw("POST", "/groups/leave", "{}")
    }

    suspend fun createTransfer(offer: DropCreatedOffer): String {
        val payload = buildJsonObject {
            put("kind", JsonPrimitive(offer.kind))
            put("name", JsonPrimitive(offer.name))
            put("provider", JsonPrimitive(offer.provider))
            put("sizeBytes", JsonPrimitive(offer.sizeBytes))
            put("dropCode", JsonPrimitive(offer.dropCode))
            put("expiresAt", JsonPrimitive(offer.expiresAt))
            put("payload", offer.payload)
        }.toString()
        val text = requestRaw("POST", "/transfers", payload)
        return runCatching {
            json.parseToJsonElement(text).jsonObject["transferId"]?.jsonPrimitive?.contentOrNull.orEmpty()
        }.getOrDefault("")
    }

    suspend fun decideTransfer(id: String, decision: String) {
        val payload = buildJsonObject { put("decision", JsonPrimitive(decision)) }.toString()
        requestRaw("POST", "/transfers/$id/decision", payload)
    }

    suspend fun updateTransferStatus(id: String, status: String) {
        val payload = buildJsonObject { put("status", JsonPrimitive(status)) }.toString()
        requestRaw("POST", "/transfers/$id/status", payload)
    }

    suspend fun cancelTransfer(id: String) {
        requestRaw("POST", "/transfers/$id/cancel", "{}")
    }
}
