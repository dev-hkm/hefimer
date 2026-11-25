package com.hefimer.app.data.network

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import com.hefimer.app.data.local.HefimerStorage
import java.security.KeyFactory
import java.security.KeyPairGenerator
import java.security.KeyStore
import java.security.MessageDigest
import java.security.PrivateKey
import java.security.SecureRandom
import java.security.Signature
import java.security.spec.ECGenParameterSpec
import java.security.spec.PKCS8EncodedKeySpec
import java.util.Base64
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

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
 * Persists the EC private key encrypted via hardware/OS-backed AndroidKeyStore AES-256-GCM.
 */
object DeviceCrypto {
    private val secureRandom = SecureRandom()
    private const val ANDROID_KEYSTORE = "AndroidKeyStore"
    private const val WRAP_KEY_ALIAS = "hefimer_ecdsa_wrap_v1"
    private const val WRAPPED_PREFIX = "aks1:"
    private const val GCM_TAG_BITS = 128

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

    internal fun wrapWithAesGcmKey(plainBytes: ByteArray, key: SecretKey): String {
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, key)
        val iv = cipher.iv
        val ciphertext = cipher.doFinal(plainBytes)
        return "$WRAPPED_PREFIX${toBase64Url(iv)}.${toBase64Url(ciphertext)}"
    }

    internal fun unwrapWithAesGcmKey(encoded: String, key: SecretKey): ByteArray? {
        if (!encoded.startsWith(WRAPPED_PREFIX)) return null
        val parts = encoded.removePrefix(WRAPPED_PREFIX).split('.')
        if (parts.size != 2) return null
        val iv = fromBase64Url(parts[0])
        val ciphertext = fromBase64Url(parts[1])
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.DECRYPT_MODE, key, GCMParameterSpec(GCM_TAG_BITS, iv))
        return cipher.doFinal(ciphertext)
    }

    private fun getOrCreateWrapKey(): SecretKey? = runCatching {
        val ks = KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }
        (ks.getKey(WRAP_KEY_ALIAS, null) as? SecretKey)?.let { return@runCatching it }
        val gen = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, ANDROID_KEYSTORE)
        val spec = KeyGenParameterSpec.Builder(
            WRAP_KEY_ALIAS,
            KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT,
        )
            .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
            .setKeySize(256)
            .build()
        gen.init(spec)
        gen.generateKey()
    }.getOrNull()

    private fun encodePrivateKeyForStorage(pkcs8Bytes: ByteArray): String {
        val wrapKey = getOrCreateWrapKey()
        if (wrapKey != null) {
            runCatching { wrapWithAesGcmKey(pkcs8Bytes, wrapKey) }.getOrNull()?.let { return it }
        }
        return toBase64Url(pkcs8Bytes)
    }

    private fun decodePrivateKeyFromStorage(stored: String): Pair<ByteArray, Boolean>? {
        if (stored.startsWith(WRAPPED_PREFIX)) {
            val wrapKey = getOrCreateWrapKey() ?: return null
            val raw = runCatching { unwrapWithAesGcmKey(stored, wrapKey) }.getOrNull() ?: return null
            return raw to false
        }
        val legacyRaw = runCatching { fromBase64Url(stored) }.getOrNull() ?: return null
        return legacyRaw to true
    }

    fun getOrCreateIdentity(storage: HefimerStorage): LocalDeviceIdentity {
        val existingId = storage.getStoredString("hefimer_dev_id")
        val existingPub = storage.getStoredString("hefimer_dev_pub")
        val existingPriv = storage.getStoredString("hefimer_dev_priv")

        if (!existingId.isNullOrBlank() && !existingPub.isNullOrBlank() && !existingPriv.isNullOrBlank()) {
            val decoded = decodePrivateKeyFromStorage(existingPriv)
            if (decoded != null) {
                val (pkcs8Bytes, needsUpgradeToKeystore) = decoded
                val privKey = runCatching {
                    KeyFactory.getInstance("EC").generatePrivate(PKCS8EncodedKeySpec(pkcs8Bytes))
                }.getOrNull()
                if (privKey != null) {
                    if (needsUpgradeToKeystore) {
                        val wrapped = encodePrivateKeyForStorage(pkcs8Bytes)
                        if (wrapped.startsWith(WRAPPED_PREFIX)) {
                            storage.putStoredString("hefimer_dev_priv", wrapped)
                        }
                    }
                    return LocalDeviceIdentity(
                        id = existingId,
                        name = storage.getDeviceName(),
                        platform = "Android",
                        publicKeySpkiBase64Url = existingPub,
                        privateKey = privKey,
                    )
                }
            }
        }

        val kpg = KeyPairGenerator.getInstance("EC")
        kpg.initialize(ECGenParameterSpec("secp256r1"), secureRandom)
        val kp = kpg.generateKeyPair()
        val id = randomToken(24)
        val pubB64 = toBase64Url(kp.public.encoded) // X.509 SPKI format — matches WebCrypto exportKey("spki")
        val privStored = encodePrivateKeyForStorage(kp.private.encoded) // Hardware/OS Keystore wrapped PKCS#8

        storage.putStoredString("hefimer_dev_id", id)
        storage.putStoredString("hefimer_dev_pub", pubB64)
        storage.putStoredString("hefimer_dev_priv", privStored)

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
