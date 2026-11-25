package com.hefimer.app.data.network

import com.hefimer.app.data.model.CustomS3Provider
import java.net.URI
import java.net.URLEncoder
import java.security.MessageDigest
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

/**
 * Pure Kotlin AWS Signature Version 4 presigner for Cloudflare R2, AWS S3,
 * Backblaze B2, MinIO, Wasabi, DigitalOcean Spaces, etc.
 * Runs 100% on-device so user-owned credentials never leave the phone.
 */
object S3SigV4Signer {
    private const val ALGORITHM = "AWS4-HMAC-SHA256"

    fun normalizeProvider(provider: CustomS3Provider): CustomS3Provider {
        val bucket = provider.bucket.trim()
        val rawEp = provider.endpoint.trim().trimEnd('/')
        val withScheme = if (rawEp.startsWith("http://", ignoreCase = true) || rawEp.startsWith("https://", ignoreCase = true)) {
            rawEp
        } else {
            "https://$rawEp"
        }
        val normalizedEndpoint = runCatching {
            val uri = URI(withScheme)
            val scheme = uri.scheme ?: "https"
            val authority = uri.authority ?: return@runCatching withScheme
            var path = uri.path?.trimEnd('/').orEmpty()
            // Users often paste R2's "S3 API" URL (https://<acc>.r2.cloudflarestorage.com/<bucket>);
            // strip trailing /<bucket> so presignUrl does not duplicate the bucket segment.
            if (bucket.isNotEmpty() && path.equals("/$bucket", ignoreCase = true)) {
                path = ""
            } else if (bucket.isNotEmpty() && path.endsWith("/$bucket", ignoreCase = true)) {
                path = path.dropLast(bucket.length + 1)
            }
            "$scheme://$authority$path"
        }.getOrDefault(withScheme)

        return provider.copy(
            name = provider.name.trim().ifBlank { bucket },
            endpoint = normalizedEndpoint,
            region = provider.region.trim().ifBlank { "auto" },
            bucket = bucket,
            accessKeyId = provider.accessKeyId.trim(),
            secretAccessKey = provider.secretAccessKey.trim(),
            publicBaseUrl = provider.publicBaseUrl.trim().trimEnd('/'),
        )
    }

    fun parseS3ErrorXml(xml: String): String? {
        if (xml.isBlank()) return null
        val code = Regex("<Code>([^<]+)</Code>", RegexOption.IGNORE_CASE).find(xml)?.groupValues?.getOrNull(1)?.trim()
        val msg = Regex("<Message>([^<]+)</Message>", RegexOption.IGNORE_CASE).find(xml)?.groupValues?.getOrNull(1)?.trim()
        return when {
            !code.isNullOrBlank() && !msg.isNullOrBlank() -> "$code: $msg"
            !code.isNullOrBlank() -> code
            !msg.isNullOrBlank() -> msg
            else -> null
        }
    }

    fun presignUrl(
        provider: CustomS3Provider,
        method: String,
        objectKey: String,
        expiresSeconds: Long,
        nowMs: Long = System.currentTimeMillis(),
    ): String {
        val norm = normalizeProvider(provider)
        val parsed = URI(norm.endpoint)
        val scheme = parsed.scheme ?: "https"
        val host = parsed.authority ?: throw IllegalArgumentException("Invalid S3/R2 endpoint host")
        val basePath = parsed.path?.trimEnd('/').orEmpty()
        val region = norm.region
        val bucket = norm.bucket
        val cleanKey = objectKey.trimStart('/')
        val isVirtualHosted = bucket.isNotEmpty() &&
            host.lowercase(Locale.US).startsWith("${bucket.lowercase(Locale.US)}.")

        val canonicalPath = buildString {
            if (basePath.isNotEmpty()) append(basePath)
            if (!isVirtualHosted && bucket.isNotEmpty()) {
                append('/')
                append(encodePathSegment(bucket))
            }
            cleanKey.split('/').filter { it.isNotEmpty() }.forEach { seg ->
                append('/')
                append(encodePathSegment(seg))
            }
            if (isEmpty()) append('/')
        }

        val amzDate = formatAmzDate(nowMs)
        val dateStamp = amzDate.substring(0, 8)
        val credentialScope = "$dateStamp/$region/s3/aws4_request"
        val credential = "${norm.accessKeyId}/$credentialScope"

        val queryParams = linkedMapOf(
            "X-Amz-Algorithm" to ALGORITHM,
            "X-Amz-Credential" to credential,
            "X-Amz-Date" to amzDate,
            "X-Amz-Expires" to expiresSeconds.coerceIn(60L, 604_800L).toString(),
            "X-Amz-SignedHeaders" to "host",
        )
        val canonicalQuery = queryParams.entries
            .sortedBy { it.key }
            .joinToString("&") { "${awsUrlEncode(it.key)}=${awsUrlEncode(it.value)}" }

        val canonicalHeaders = "host:${host.lowercase(Locale.US)}\n"
        val canonicalRequest = listOf(
            method.uppercase(Locale.US),
            canonicalPath,
            canonicalQuery,
            canonicalHeaders,
            "host",
            "UNSIGNED-PAYLOAD",
        ).joinToString("\n")

        val stringToSign = listOf(
            ALGORITHM,
            amzDate,
            credentialScope,
            sha256Hex(canonicalRequest),
        ).joinToString("\n")

        val signingKey = deriveSigningKey(norm.secretAccessKey, dateStamp, region, "s3")
        val signature = hmacSha256(signingKey, stringToSign).toHex()

        return "$scheme://$host$canonicalPath?$canonicalQuery&X-Amz-Signature=$signature"
    }

    fun buildPublicUrl(provider: CustomS3Provider, objectKey: String): String? {
        val base = provider.publicBaseUrl.trim().trimEnd('/')
        if (base.isBlank()) return null
        val normalizedBase = if (base.startsWith("http://") || base.startsWith("https://")) base else "https://$base"
        val encodedKey = objectKey.trimStart('/')
            .split('/')
            .filter { it.isNotEmpty() }
            .joinToString("/") { encodePathSegment(it) }
        return "$normalizedBase/$encodedKey"
    }

    private fun deriveSigningKey(secretKey: String, dateStamp: String, region: String, service: String): ByteArray {
        val kSecret = ("AWS4$secretKey").toByteArray(Charsets.UTF_8)
        val kDate = hmacSha256(kSecret, dateStamp)
        val kRegion = hmacSha256(kDate, region)
        val kService = hmacSha256(kRegion, service)
        return hmacSha256(kService, "aws4_request")
    }

    private fun hmacSha256(key: ByteArray, data: String): ByteArray {
        val mac = Mac.getInstance("HmacSHA256")
        mac.init(SecretKeySpec(key, "HmacSHA256"))
        return mac.doFinal(data.toByteArray(Charsets.UTF_8))
    }

    private fun sha256Hex(input: String): String =
        MessageDigest.getInstance("SHA-256").digest(input.toByteArray(Charsets.UTF_8)).toHex()

    private fun ByteArray.toHex(): String = joinToString("") { "%02x".format(it) }

    private fun formatAmzDate(nowMs: Long): String {
        val sdf = SimpleDateFormat("yyyyMMdd'T'HHmmss'Z'", Locale.US)
        sdf.timeZone = TimeZone.getTimeZone("UTC")
        return sdf.format(Date(nowMs))
    }

    private fun encodePathSegment(segment: String): String = awsUrlEncode(segment)

    private fun awsUrlEncode(value: String): String =
        URLEncoder.encode(value, "UTF-8")
            .replace("+", "%20")
            .replace("*", "%2A")
            .replace("%7E", "~")
}
