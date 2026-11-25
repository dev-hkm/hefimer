package com.hefimer.app.data.network

import android.net.Uri
import android.util.Log
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.hefimer.app.data.local.HefimerStorage
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

@RunWith(AndroidJUnit4::class)
class ProviderE2EInstrumentedTest {

    private fun makeTestMp4(dir: File): Pair<File, ByteArray> {
        val bytes = ByteArray(32 * 1024)
        val header = byteArrayOf(
            0x00, 0x00, 0x00, 0x18,
            'f'.code.toByte(), 't'.code.toByte(), 'y'.code.toByte(), 'p'.code.toByte(),
            'm'.code.toByte(), 'p'.code.toByte(), '4'.code.toByte(), '2'.code.toByte(),
            0x00, 0x00, 0x00, 0x00,
            'm'.code.toByte(), 'p'.code.toByte(), '4'.code.toByte(), '2'.code.toByte(),
            'i'.code.toByte(), 's'.code.toByte(), 'o'.code.toByte(), 'm'.code.toByte(),
        )
        System.arraycopy(header, 0, bytes, 0, header.size)
        for (i in header.size until bytes.size) {
            bytes[i] = ((i * 31 + 7) and 0xFF).toByte()
        }
        val file = File(dir, "hefimer_e2e_test.mp4")
        file.writeBytes(bytes)
        return file to bytes
    }

    @Test
    fun all18ProvidersUploadAndDownloadEndToEndOnEmulator() = runBlocking {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val storage = HefimerStorage(context)
        val engine = FileUploadEngine(context, storage)

        val (mp4File, expectedMp4) = makeTestMp4(context.cacheDir)
        val pickedMp4 = LocalPickedFile(
            uri = Uri.fromFile(mp4File),
            name = mp4File.name,
            size = expectedMp4.size.toLong(),
            mimeType = "video/mp4",
            tempFile = mp4File,
        )

        val expectedTabProviders = setOf("gofile", "vikingfile", "buzzheavier", "fileditch")
        val failures = mutableListOf<String>()

        try {
            for (cfg in UploadProviders.ALL) {
                try {
                    val uploadedUrl = engine.uploadForProvider(cfg.id, pickedMp4, cfg.defaultExpire)
                    assertTrue("${cfg.id}: empty/invalid URL '$uploadedUrl'", uploadedUrl.startsWith("https://"))

                    val displayName = UploadProviders.getProviderNameFromUrl(uploadedUrl)
                    assertEquals("${cfg.id}: provider name mismatch for $uploadedUrl", cfg.name, displayName)

                    val mode = DownloadHelper.getDirectDownloadMode(uploadedUrl)
                    if (cfg.id in expectedTabProviders) {
                        assertEquals("${cfg.id}: expected TAB mode for $uploadedUrl", DirectDownloadMode.TAB, mode)
                    } else {
                        val outcome = DownloadHelper.downloadFile(
                            context = context,
                            url = uploadedUrl,
                            fileName = "${cfg.id}_e2e.mp4",
                        )
                        try {
                            assertEquals(
                                "${cfg.id}: expected direct file download, got mode=${outcome.mode} for $uploadedUrl",
                                "download",
                                outcome.mode,
                            )
                            val savedUri = outcome.savedUri
                            assertNotNull("${cfg.id}: missing savedUri", savedUri)
                            val actualBytes = context.contentResolver.openInputStream(savedUri!!)?.use { it.readBytes() }
                            assertNotNull("${cfg.id}: failed reading savedUri", actualBytes)
                            assertArrayEquals(
                                "${cfg.id}: downloaded byte mismatch from $uploadedUrl",
                                expectedMp4,
                                actualBytes,
                            )
                        } finally {
                            outcome.savedUri?.let { runCatching { context.contentResolver.delete(it, null, null) } }
                        }
                    }
                    Log.i("HefimerE2E", "PASS [${cfg.id}] -> $uploadedUrl (mode=$mode, name=$displayName)")
                } catch (t: Throwable) {
                    val msg = "${cfg.id}: ${t.javaClass.simpleName}: ${t.message}"
                    Log.e("HefimerE2E", "FAIL $msg", t)
                    failures.add(msg)
                }
            }

            // Also specifically verify Fileditch TXT vs MP4 host routing + DownloadHelper TAB handoff
            val txtFile = File(context.cacheDir, "hefimer_e2e_note.txt").apply {
                writeText("Hefimer E2E Fileditch txt test")
            }
            try {
                val pickedTxt = LocalPickedFile(
                    uri = Uri.fromFile(txtFile),
                    name = txtFile.name,
                    size = txtFile.length(),
                    mimeType = "text/plain",
                    tempFile = txtFile,
                )
                val fdTxtUrl = engine.uploadForProvider("fileditch", pickedTxt, "unlimited")
                assertEquals("Fileditch", UploadProviders.getProviderNameFromUrl(fdTxtUrl))
                assertEquals(DirectDownloadMode.TAB, DownloadHelper.getDirectDownloadMode(fdTxtUrl))
                Log.i("HefimerE2E", "PASS [fileditch-txt] -> $fdTxtUrl")
            } finally {
                txtFile.delete()
            }
        } finally {
            mp4File.delete()
        }

        // Verify AndroidKeyStore private-key wrapping & seamless plaintext migration
        val freshIdentity = DeviceCrypto.getOrCreateIdentity(storage)
        val wrappedPriv = storage.getStoredString("hefimer_dev_priv").orEmpty()
        assertTrue("Expected Keystore AES-GCM wrapped private key, got $wrappedPriv", wrappedPriv.startsWith("aks1:"))
        val sigAfterWrap = DeviceCrypto.signP1363Base64Url(freshIdentity.privateKey, "hefimer-keystore-check")
        assertTrue(sigAfterWrap.isNotBlank())

        // Simulate legacy plaintext key and ensure reload upgrades transparently preserving identity
        val kpg = java.security.KeyPairGenerator.getInstance("EC")
        kpg.initialize(java.security.spec.ECGenParameterSpec("secp256r1"))
        val kp = kpg.generateKeyPair()
        val legacyPub = DeviceCrypto.toBase64Url(kp.public.encoded)
        val legacyPriv = DeviceCrypto.toBase64Url(kp.private.encoded)
        storage.putStoredString("hefimer_dev_id", "legacy-dev-id-test")
        storage.putStoredString("hefimer_dev_pub", legacyPub)
        storage.putStoredString("hefimer_dev_priv", legacyPriv)

        val migrated = DeviceCrypto.getOrCreateIdentity(storage)
        assertEquals("legacy-dev-id-test", migrated.id)
        assertEquals(legacyPub, migrated.publicKeySpkiBase64Url)
        assertTrue(storage.getStoredString("hefimer_dev_priv").orEmpty().startsWith("aks1:"))

        assertTrue(
            "Provider E2E failures (${failures.size}):\n${failures.joinToString("\n")}",
            failures.isEmpty(),
        )
    }
}
