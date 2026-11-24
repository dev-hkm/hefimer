package com.hefimer.app

import com.hefimer.app.data.model.parseExpireDurationMs
import com.hefimer.app.data.network.DeviceCrypto
import com.hefimer.app.data.network.DirectDownloadMode
import com.hefimer.app.data.network.DownloadHelper
import com.hefimer.app.data.network.FirebaseRtdbClient
import com.hefimer.app.data.network.UploadProviders
import com.hefimer.app.ui.components.CodeHighlighter
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.security.KeyPairGenerator
import java.security.SecureRandom
import java.security.Signature
import java.security.spec.ECGenParameterSpec

class HefimerCoreTests {

    @Test
    fun ecdsaP256DerToP1363ProducesValid64ByteSignature() {
        val kpg = KeyPairGenerator.getInstance("EC")
        kpg.initialize(ECGenParameterSpec("secp256r1"), SecureRandom())
        val kp = kpg.generateKeyPair()

        val payload = DeviceCrypto.buildSignaturePayload(
            timestamp = "1700000000000",
            nonce = "abc12345678901234567",
            method = "POST",
            pathAndQuery = "/api/devices/sync",
            bodyHash = DeviceCrypto.sha256Base64Url("{}"),
        )
        val sigB64 = DeviceCrypto.signP1363Base64Url(kp.private, payload)
        val raw = DeviceCrypto.fromBase64Url(sigB64)
        assertEquals("P-256 IEEE P1363 signature must be exactly 64 bytes", 64, raw.size)

        // Verify round-trip via JDK SHA256withECDSAinP1363Format
        val verifier = Signature.getInstance("SHA256withECDSAinP1363Format")
        verifier.initVerify(kp.public)
        verifier.update(payload.toByteArray(Charsets.UTF_8))
        assertTrue("P1363 signature must verify cleanly against SPKI public key", verifier.verify(raw))
    }

    @Test
    fun firebaseRtdbSsePatchAndPutMergeCorrectly() {
        val initial = buildJsonObject {
            put("name", JsonPrimitive("Design Room"))
            put("members", buildJsonObject { put("u1", buildJsonObject { put("name", JsonPrimitive("Alice")) }) })
        }
        val afterPut = FirebaseRtdbClient.applyPut(
            initial,
            "/members/u2",
            buildJsonObject { put("name", JsonPrimitive("Bob")) },
        )
        assertNotNull(afterPut)
        assertEquals("Bob", afterPut!!.jsonObject["members"]!!.jsonObject["u2"]!!.jsonObject["name"]!!.jsonPrimitive.content)

        val afterDelete = FirebaseRtdbClient.applyPut(afterPut, "/members/u1", JsonNull)
        assertNull(afterDelete!!.jsonObject["members"]!!.jsonObject["u1"])
    }

    @Test
    fun uploadProvidersHaveAll12UniqueIdsAndValidFallback() {
        assertEquals(12, UploadProviders.ALL.size)
        assertEquals(12, UploadProviders.ALL.map { it.id }.toSet().size)
        assertEquals("72h", UploadProviders.normalizeExpire("storageto", "invalid"))
        assertEquals("24h", UploadProviders.normalizeExpire("storageto", "24h"))
        assertEquals("storage.to", UploadProviders.getProviderNameFromUrl("https://storage.to/f/abc"))
        assertEquals("sto.care", UploadProviders.getProviderNameFromUrl("https://trystocare.com/f/abc"))
        assertEquals("Cloudflare R2", UploadProviders.getProviderNameFromUrl("https://example.com", hasObjectKey = true))
    }

    @Test
    fun downloadHelperRoutesProvidersCorrectly() {
        assertEquals(DirectDownloadMode.PROXY, DownloadHelper.getDirectDownloadMode("https://storage.to/abc"))
        assertEquals(DirectDownloadMode.PROXY, DownloadHelper.getDirectDownloadMode("https://trystocare.com/abc"))
        assertEquals(DirectDownloadMode.TAB, DownloadHelper.getDirectDownloadMode("https://gofile.io/d/xyz"))
        assertEquals(DirectDownloadMode.TAB, DownloadHelper.getDirectDownloadMode("https://vikingfile.com/f/xyz"))
        assertNull(DownloadHelper.getMediaPreviewUrl("https://storage.to/a.png", "image/png"))
        assertNotNull(DownloadHelper.getMediaPreviewUrl("https://trystocare.com/a.png", "image/png"))
    }

    @Test
    fun codeHighlighterDetectsLanguagesAndSmartActions() {
        assertEquals("kotlin", CodeHighlighter.detectLanguage("fun main() {\n  val x = 1\n}").id)
        assertEquals("python", CodeHighlighter.detectLanguage("def hello():\n    print('hi')").id)
        assertEquals("sql", CodeHighlighter.detectLanguage("SELECT * FROM users WHERE id = 1").id)
        assertEquals("email", CodeHighlighter.getSmartAction("dev@hefimer.qzz.io")?.type)
        assertEquals("link", CodeHighlighter.getSmartAction("https://hefimer.qzz.io")?.type)
        assertEquals(3_600_000L, parseExpireDurationMs("1h"))
        assertEquals(300_000L, parseExpireDurationMs("5p"))
    }
}
