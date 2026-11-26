package com.hefimer.app.data.network

import android.content.Context
import android.util.Base64
import java.io.File
import java.io.FilterInputStream
import java.io.IOException
import java.io.InputStream
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.CipherInputStream
import javax.crypto.CipherOutputStream
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec

/**
 * On-device AES-256-GCM streaming file encryption/decryption for public file hosts.
 * Wire format: [12-byte random IV] || [AES-256-GCM ciphertext + 128-bit auth tag].
 */
object FileE2eeCrypto {
    private const val IV_LEN = 12
    private const val TAG_BITS = 128
    private const val KEY_BYTES = 32
    const val ALGO_LABEL = "AES-256-GCM"

    fun generateKeyBase64(): String {
        val raw = ByteArray(KEY_BYTES)
        SecureRandom().nextBytes(raw)
        return Base64.encodeToString(raw, Base64.URL_SAFE or Base64.NO_WRAP or Base64.NO_PADDING)
    }

    private fun decodeKey(keyBase64: String): SecretKeySpec {
        val raw = Base64.decode(keyBase64.trim(), Base64.URL_SAFE or Base64.NO_WRAP or Base64.NO_PADDING)
        if (raw.size != KEY_BYTES) {
            throw IOException("Invalid E2EE key length")
        }
        return SecretKeySpec(raw, "AES")
    }

    fun encryptToTempFile(
        context: Context,
        picked: LocalPickedFile,
        keyBase64: String,
    ): LocalPickedFile {
        val key = decodeKey(keyBase64)
        val iv = ByteArray(IV_LEN)
        SecureRandom().nextBytes(iv)
        val cipher = Cipher.getInstance("AES/GCM/NoPadding").apply {
            init(Cipher.ENCRYPT_MODE, key, GCMParameterSpec(TAG_BITS, iv))
        }
        val outFile = File(context.cacheDir, "e2ee_${System.currentTimeMillis()}_${picked.name.hashCode()}.bin")
        val input = if (picked.tempFile != null && picked.tempFile.exists()) {
            picked.tempFile.inputStream()
        } else {
            context.contentResolver.openInputStream(picked.uri)
                ?: throw IOException("Cannot open source file for encryption")
        }
        input.use { src ->
            outFile.outputStream().use { fos ->
                fos.write(iv)
                CipherOutputStream(fos, cipher).use { cos ->
                    val buf = ByteArray(64 * 1024)
                    while (true) {
                        val read = src.read(buf)
                        if (read <= 0) break
                        cos.write(buf, 0, read)
                    }
                }
            }
        }
        return LocalPickedFile(
            uri = android.net.Uri.fromFile(outFile),
            name = "hefimer_enc_${System.currentTimeMillis()}.bin",
            size = outFile.length(),
            mimeType = "application/octet-stream",
            tempFile = outFile,
        )
    }

    /**
     * Wraps an encrypted HTTP InputStream: reads the leading 12-byte IV, then streams decrypted
     * plaintext bytes via AES-256-GCM. Authentication tag is verified automatically on EOF.
     */
    fun decryptInputStream(rawStream: InputStream, keyBase64: String): InputStream {
        val key = decodeKey(keyBase64)
        val iv = ByteArray(IV_LEN)
        var offset = 0
        while (offset < IV_LEN) {
            val n = rawStream.read(iv, offset, IV_LEN - offset)
            if (n <= 0) throw IOException("Corrupted encrypted stream (missing IV)")
            offset += n
        }
        val cipher = Cipher.getInstance("AES/GCM/NoPadding").apply {
            init(Cipher.DECRYPT_MODE, key, GCMParameterSpec(TAG_BITS, iv))
        }
        val cis = CipherInputStream(rawStream, cipher)
        return object : FilterInputStream(cis) {}
    }
}
