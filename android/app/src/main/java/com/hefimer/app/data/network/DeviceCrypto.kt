package com.hefimer.app.data.network

import com.hefimer.app.data.local.HefimerStorage
import java.security.KeyFactory
import java.security.KeyPairGenerator
import java.security.MessageDigest
import java.security.PrivateKey
import java.security.SecureRandom
import java.security.Signature
import java.security.spec.ECGenParameterSpec
import java.security.spec.PKCS8EncodedKeySpec
import java.util.Base64

data class LocalDeviceIdentity(
    val id: String,
    val name: String,
    val platform: String,
    val publicKeySpkiBase64Url: String,
    val privateKey: PrivateKey,
)

/**
 * Accountless P-256 ECDSA identity for Hefimer Link, producing SPKI public keys and
 * exact IEEE P1363 64-byte (r || s) signatures verified by Cloudflare Workers WebCrypto.
 */
object DeviceCrypto {
    private val secureRandom = SecureRandom()

    fun toBase64Url(bytes: ByteArray): String =
        Base64.getUrlEncoder().withoutPadding().encodeToString(bytes)

    fun fromBase64Url(value: String): ByteArray =
        Base64.getUrlDecoder().decode(value)

    fun sha256Base64Url(value: String): String {
        val digest = MessageDigest.getInstance("SHA-256").digest(value.toByteArray(Charsets.UTF_8))
        return toBase64Url(digest)
    }

    fun randomToken(byteCount: Int = 24): String {
        val bytes = ByteArray(byteCount)
        secureRandom.nextBytes(bytes)
        return toBase64Url(bytes)
    }

    fun buildSignaturePayload(
        timestamp: String,
        nonce: String,
        method: String,
        pathAndQuery: String,
        bodyHash: String,
    ): String = listOf(timestamp, nonce, method.uppercase(), pathAndQuery, bodyHash).joinToString("\n")

    fun getOrCreateIdentity(storage: HefimerStorage): LocalDeviceIdentity {
        val existingId = storage.getStoredString("hefimer_dev_id")
        val existingPub = storage.getStoredString("hefimer_dev_pub")
        val existingPriv = storage.getStoredString("hefimer_dev_priv")

        if (!existingId.isNullOrBlank() && !existingPub.isNullOrBlank() && !existingPriv.isNullOrBlank()) {
            val privKey = runCatching {
                val kf = KeyFactory.getInstance("EC")
                kf.generatePrivate(PKCS8EncodedKeySpec(fromBase64Url(existingPriv)))
            }.getOrNull()
            if (privKey != null) {
                return LocalDeviceIdentity(
                    id = existingId,
                    name = storage.getDeviceName(),
                    platform = "Android",
                    publicKeySpkiBase64Url = existingPub,
                    privateKey = privKey,
                )
            }
        }

        val kpg = KeyPairGenerator.getInstance("EC")
        kpg.initialize(ECGenParameterSpec("secp256r1"), secureRandom)
        val kp = kpg.generateKeyPair()
        val id = randomToken(24)
        val pubB64 = toBase64Url(kp.public.encoded) // X.509 SPKI format — matches WebCrypto exportKey("spki")
        val privB64 = toBase64Url(kp.private.encoded) // PKCS#8

        storage.putStoredString("hefimer_dev_id", id)
        storage.putStoredString("hefimer_dev_pub", pubB64)
        storage.putStoredString("hefimer_dev_priv", privB64)

        return LocalDeviceIdentity(
            id = id,
            name = storage.getDeviceName(),
            platform = "Android",
            publicKeySpkiBase64Url = pubB64,
            privateKey = kp.private,
        )
    }

    /**
     * Signs payload with ECDSA P-256 SHA-256 and outputs 64-byte IEEE P1363 (r || s) Base64Url
     * matching WebCrypto's `crypto.subtle.verify({ name: "ECDSA", hash: "SHA-256" })`.
     */
    fun signP1363Base64Url(privateKey: PrivateKey, payload: String): String {
        val signer = Signature.getInstance("SHA256withECDSA")
        signer.initSign(privateKey)
        signer.update(payload.toByteArray(Charsets.UTF_8))
        val der = signer.sign()
        val p1363 = derToP1363(der, 32)
        return toBase64Url(p1363)
    }

    /**
     * Converts an ASN.1 DER-encoded ECDSA signature to fixed-length IEEE P1363 (r || s).
     */
    fun derToP1363(der: ByteArray, componentSize: Int = 32): ByteArray {
        require(der.size >= 8 && der[0] == 0x30.toByte()) { "Invalid DER signature" }
        var offset = 2
        if ((der[1].toInt() and 0x80) != 0) {
            offset += (der[1].toInt() and 0x7F)
        }
        require(der[offset] == 0x02.toByte()) { "Expected INTEGER for r" }
        val rLen = der[offset + 1].toInt() and 0xFF
        val rStart = offset + 2
        offset = rStart + rLen

        require(der[offset] == 0x02.toByte()) { "Expected INTEGER for s" }
        val sLen = der[offset + 1].toInt() and 0xFF
        val sStart = offset + 2

        val output = ByteArray(componentSize * 2)
        copyUnsignedFixed(der, rStart, rLen, output, 0, componentSize)
        copyUnsignedFixed(der, sStart, sLen, output, componentSize, componentSize)
        return output
    }

    private fun copyUnsignedFixed(
        src: ByteArray,
        srcOffset: Int,
        srcLen: Int,
        dst: ByteArray,
        dstOffset: Int,
        fixedLen: Int,
    ) {
        var start = srcOffset
        var len = srcLen
        while (len > 0 && src[start] == 0.toByte()) {
            start++
            len--
        }
        require(len <= fixedLen) { "ECDSA component overflow ($len > $fixedLen)" }
        System.arraycopy(src, start, dst, dstOffset + (fixedLen - len), len)
    }
}
