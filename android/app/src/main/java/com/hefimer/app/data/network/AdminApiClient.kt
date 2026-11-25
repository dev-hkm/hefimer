package com.hefimer.app.data.network

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException

data class AdminR2ConfigDto(
    val accountId: String = "",
    val accessKeyId: String = "",
    val secretAccessKey: String = "",
    val bucketName: String = "",
    val source: String = "env",
    val configured: Boolean = false,
)

data class AdminStatsDto(
    val isFrozen: Boolean = false,
    val developer: String = "Khanh Minh",
    val likes: Int = 0,
    val notice: String = "",
)

data class AdminOverviewDto(
    val stats: AdminStatsDto = AdminStatsDto(),
    val r2: AdminR2ConfigDto = AdminR2ConfigDto(),
)

class AdminApiClient {
    private val origin = HefimerApiConfig.SITE_ORIGIN
    private val client = HefimerApiConfig.apiClient
    private val json = HefimerApiConfig.json
    private val jsonMedia = "application/json; charset=utf-8".toMediaType()

    private fun postJson(path: String, body: JsonObject, bearerToken: String? = null): JsonObject {
        val builder = Request.Builder()
            .url("$origin/api/admin/$path")
            .post(body.toString().toRequestBody(jsonMedia))
        if (!bearerToken.isNullOrBlank()) {
            builder.header("Authorization", "Bearer $bearerToken")
        }
        client.newCall(builder.build()).execute().use { resp ->
            val text = resp.body?.string().orEmpty()
            val obj = runCatching { json.parseToJsonElement(text).jsonObject }.getOrNull() ?: buildJsonObject {}
            if (!resp.isSuccessful || obj["ok"]?.jsonPrimitive?.booleanOrNull == false) {
                throw IOException(obj["error"]?.jsonPrimitive?.contentOrNull ?: "Admin API error (${resp.code})")
            }
            return obj
        }
    }

    private fun getJson(path: String, bearerToken: String? = null): JsonObject {
        val builder = Request.Builder()
            .url("$origin/api/admin/$path")
            .get()
        if (!bearerToken.isNullOrBlank()) {
            builder.header("Authorization", "Bearer $bearerToken")
        }
        client.newCall(builder.build()).execute().use { resp ->
            val text = resp.body?.string().orEmpty()
            val obj = runCatching { json.parseToJsonElement(text).jsonObject }.getOrNull() ?: buildJsonObject {}
            if (!resp.isSuccessful || obj["ok"]?.jsonPrimitive?.booleanOrNull == false) {
                throw IOException(obj["error"]?.jsonPrimitive?.contentOrNull ?: "Admin API error (${resp.code})")
            }
            return obj
        }
    }

    suspend fun login(password: String): String = withContext(Dispatchers.IO) {
        val obj = postJson("login", buildJsonObject { put("password", JsonPrimitive(password)) })
        obj["token"]?.jsonPrimitive?.contentOrNull
            ?: throw IOException("Missing admin session token")
    }

    suspend fun verifySecretPasscode(passcode: String): Boolean = withContext(Dispatchers.IO) {
        runCatching {
            val obj = postJson("verify-secret", buildJsonObject { put("passcode", JsonPrimitive(passcode)) })
            obj["ok"]?.jsonPrimitive?.booleanOrNull == true
        }.getOrDefault(false)
    }

    suspend fun fetchPublicStats(): AdminStatsDto? = withContext(Dispatchers.IO) {
        runCatching {
            val obj = getJson("public-stats")
            val stats = obj["stats"]?.jsonObject ?: return@runCatching null
            AdminStatsDto(
                isFrozen = stats["isFrozen"]?.jsonPrimitive?.booleanOrNull ?: false,
                developer = stats["developer"]?.jsonPrimitive?.contentOrNull ?: "Khanh Minh",
                likes = stats["likes"]?.jsonPrimitive?.intOrNull ?: 0,
                notice = stats["notice"]?.jsonPrimitive?.contentOrNull.orEmpty(),
            )
        }.getOrNull()
    }

    suspend fun fetchOverview(token: String): AdminOverviewDto = withContext(Dispatchers.IO) {
        val obj = getJson("overview", token)
        val stats = obj["stats"]?.jsonObject
        val r2 = obj["r2"]?.jsonObject
        AdminOverviewDto(
            stats = AdminStatsDto(
                isFrozen = stats?.get("isFrozen")?.jsonPrimitive?.booleanOrNull ?: false,
                developer = stats?.get("developer")?.jsonPrimitive?.contentOrNull ?: "Khanh Minh",
                likes = stats?.get("likes")?.jsonPrimitive?.intOrNull ?: 0,
                notice = stats?.get("notice")?.jsonPrimitive?.contentOrNull.orEmpty(),
            ),
            r2 = AdminR2ConfigDto(
                accountId = r2?.get("accountId")?.jsonPrimitive?.contentOrNull.orEmpty(),
                accessKeyId = r2?.get("accessKeyId")?.jsonPrimitive?.contentOrNull.orEmpty(),
                secretAccessKey = r2?.get("secretAccessKey")?.jsonPrimitive?.contentOrNull.orEmpty(),
                bucketName = r2?.get("bucketName")?.jsonPrimitive?.contentOrNull.orEmpty(),
                source = r2?.get("source")?.jsonPrimitive?.contentOrNull ?: "env",
                configured = r2?.get("configured")?.jsonPrimitive?.booleanOrNull ?: false,
            ),
        )
    }

    suspend fun updateStats(
        token: String,
        isFrozen: Boolean? = null,
        developer: String? = null,
        likes: Int? = null,
        broadcast: JsonObject? = null,
        clearBroadcast: Boolean = false,
    ): AdminStatsDto = withContext(Dispatchers.IO) {
        val body = buildJsonObject {
            if (isFrozen != null) put("isFrozen", JsonPrimitive(isFrozen))
            if (developer != null) put("developer", JsonPrimitive(developer))
            if (likes != null) put("likes", JsonPrimitive(likes))
            if (clearBroadcast) {
                put("broadcast", kotlinx.serialization.json.JsonNull)
            } else if (broadcast != null) {
                put("broadcast", broadcast)
            }
        }
        val obj = postJson("stats", body, token)
        val stats = obj["stats"]?.jsonObject
        AdminStatsDto(
            isFrozen = stats?.get("isFrozen")?.jsonPrimitive?.booleanOrNull ?: false,
            developer = stats?.get("developer")?.jsonPrimitive?.contentOrNull ?: "Khanh Minh",
            likes = stats?.get("likes")?.jsonPrimitive?.intOrNull ?: 0,
            notice = stats?.get("notice")?.jsonPrimitive?.contentOrNull.orEmpty(),
        )
    }

    suspend fun updatePasswords(token: String, adminPass: String, secretPass: String) = withContext(Dispatchers.IO) {
        postJson(
            "passwords",
            buildJsonObject {
                if (adminPass.isNotBlank()) put("adminPass", JsonPrimitive(adminPass.trim()))
                if (secretPass.isNotBlank()) put("secretPass", JsonPrimitive(secretPass.trim()))
            },
            token,
        )
        Unit
    }

    suspend fun updateR2Config(
        token: String,
        accountId: String,
        accessKeyId: String,
        secretAccessKey: String,
        bucketName: String,
    ): AdminR2ConfigDto = withContext(Dispatchers.IO) {
        val obj = postJson(
            "r2-config",
            buildJsonObject {
                put("accountId", JsonPrimitive(accountId.trim()))
                put("accessKeyId", JsonPrimitive(accessKeyId.trim()))
                put("secretAccessKey", JsonPrimitive(secretAccessKey.trim()))
                put("bucketName", JsonPrimitive(bucketName.trim()))
            },
            token,
        )
        val r2 = obj["r2"]?.jsonObject
        AdminR2ConfigDto(
            accountId = r2?.get("accountId")?.jsonPrimitive?.contentOrNull.orEmpty(),
            accessKeyId = r2?.get("accessKeyId")?.jsonPrimitive?.contentOrNull.orEmpty(),
            secretAccessKey = r2?.get("secretAccessKey")?.jsonPrimitive?.contentOrNull.orEmpty(),
            bucketName = r2?.get("bucketName")?.jsonPrimitive?.contentOrNull.orEmpty(),
            source = r2?.get("source")?.jsonPrimitive?.contentOrNull ?: "kv",
            configured = r2?.get("configured")?.jsonPrimitive?.booleanOrNull ?: true,
        )
    }
}
