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
    fun uploadProvidersHaveAll18UniqueIdsAndValidFallback() {
        assertEquals(18, UploadProviders.ALL.size)
        assertEquals(18, UploadProviders.ALL.map { it.id }.toSet().size)
        assertEquals("storageto", UploadProviders.ALL.first().id)
        assertEquals("storageto", UploadProviders.FALLBACK_PRIORITY.first())
        assertEquals(18, UploadProviders.FALLBACK_PRIORITY.toSet().size)
        assertEquals("72h", UploadProviders.normalizeExpire("storageto", "invalid"))
        assertEquals("24h", UploadProviders.normalizeExpire("storageto", "24h"))
        assertEquals("storage.to", UploadProviders.getProviderNameFromUrl("https://storage.to/f/abc"))
        assertEquals("sto.care", UploadProviders.getProviderNameFromUrl("https://trystocare.com/f/abc"))
        assertEquals("x0.at", UploadProviders.getProviderNameFromUrl("https://x0.at/abc.png"))
        assertEquals("pomf.lain.la", UploadProviders.getProviderNameFromUrl("https://pomf2.lain.la/f/abc.bin"))
        assertEquals("qu.ax", UploadProviders.getProviderNameFromUrl("https://qu.ax/abc.png"))
        assertEquals("Catbox", UploadProviders.getProviderNameFromUrl("https://files.catbox.moe/abc.png"))
        assertEquals("Buzzheavier", UploadProviders.getProviderNameFromUrl("https://buzzheavier.com/abc"))
        assertEquals("Fileditch", UploadProviders.getProviderNameFromUrl("https://itsfileditchfiles.st/f/abc"))
        assertEquals("Cloudflare R2", UploadProviders.getProviderNameFromUrl("https://example.com", hasObjectKey = true))
    }

    @Test
    fun downloadHelperRoutesProvidersCorrectly() {
        assertEquals(DirectDownloadMode.PROXY, DownloadHelper.getDirectDownloadMode("https://storage.to/abc"))
        assertEquals(DirectDownloadMode.PROXY, DownloadHelper.getDirectDownloadMode("https://trystocare.com/abc"))
        assertEquals(DirectDownloadMode.PROXY, DownloadHelper.getDirectDownloadMode("https://h.uguu.se/abc.txt"))
        assertEquals(DirectDownloadMode.PROXY, DownloadHelper.getDirectDownloadMode("https://litter.catbox.moe/abc.txt"))
        assertEquals(DirectDownloadMode.FETCH, DownloadHelper.getDirectDownloadMode("https://files.catbox.moe/abc.txt"))
        assertEquals(DirectDownloadMode.FETCH, DownloadHelper.getDirectDownloadMode("https://x0.at/abc.txt"))
        assertEquals(DirectDownloadMode.FETCH, DownloadHelper.getDirectDownloadMode("https://pomf2.lain.la/f/abc.txt"))
        assertEquals(DirectDownloadMode.FETCH, DownloadHelper.getDirectDownloadMode("https://qu.ax/abc.txt"))
        assertEquals(DirectDownloadMode.TAB, DownloadHelper.getDirectDownloadMode("https://gofile.io/d/xyz"))
        assertEquals(DirectDownloadMode.TAB, DownloadHelper.getDirectDownloadMode("https://vikingfile.com/f/xyz"))
        assertEquals(DirectDownloadMode.TAB, DownloadHelper.getDirectDownloadMode("https://buzzheavier.com/xyz"))
        assertEquals(DirectDownloadMode.TAB, DownloadHelper.getDirectDownloadMode("https://itsfileditchfiles.st/f/xyz"))
        assertNull(DownloadHelper.getMediaPreviewUrl("https://storage.to/a.png", "image/png"))
        assertNull(DownloadHelper.getMediaPreviewUrl("https://buzzheavier.com/a.png", "image/png"))
        assertNotNull(DownloadHelper.getMediaPreviewUrl("https://trystocare.com/a.png", "image/png"))
        assertEquals(
            "https://files.catbox.moe/a.png",
            DownloadHelper.getMediaPreviewUrl("https://files.catbox.moe/a.png", "image/png"),
        )
        assertEquals(
            "https://x0.at/a.png",
            DownloadHelper.getMediaPreviewUrl("https://x0.at/a.png", "image/png"),
        )
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

    @Test
    fun codeHighlighterHighlightsFullLargeFileAndMultipleLanguages() {
        // Verify 45,000+ char file highlights all the way to the last line (no early cutoff)
        val unit = "fun compute(value: Int): String {\n    // comment\n    return \"item_\${value + 42}\"\n}\n"
        val bigCode = unit.repeat(550) // ~45,650 chars
        assertTrue(bigCode.length > 40_000)
        val highlighted = CodeHighlighter.highlight(bigCode, "kotlin")
        assertEquals(bigCode.length, highlighted.length)
        assertTrue("Expected spans near the end of large file", highlighted.spanStyles.any { it.start > 40_000 })

        // Verify HTML, JSON, CSS, SQL all emit syntax spans
        assertTrue(CodeHighlighter.highlight("<div class=\"card\">Hello</div>", "markup").spanStyles.size >= 3)
        assertTrue(CodeHighlighter.highlight("{\"key\": 123, \"ok\": true}", "json").spanStyles.size >= 4)
        assertTrue(CodeHighlighter.highlight(".btn { color: #ff79c6; padding: 12px; }", "css").spanStyles.size >= 4)
        assertTrue(CodeHighlighter.highlight("select id from users where active = true -- ok", "sql").spanStyles.size >= 5)

        // Verify CodeSyntaxVisualTransformation preserves exact text length & Identity mapping
        val vt = com.hefimer.app.ui.components.CodeSyntaxVisualTransformation("kotlin")
        val transformed = vt.filter(androidx.compose.ui.text.AnnotatedString(unit))
        assertEquals(unit, transformed.text.text)
        assertTrue(transformed.text.spanStyles.isNotEmpty())
    }

    @Test
    fun hefimerLangAutoDetectsRegionAndLocale() {
        val viLocale = java.util.Locale("vi", "VN")
        val enVnLocale = java.util.Locale("en", "VN")
        val enUsLocale = java.util.Locale("en", "US")

        assertTrue(com.hefimer.app.ui.theme.HefimerLang.resolveIsVietnamese(com.hefimer.app.ui.theme.AppLanguage.AUTO, viLocale))
        assertTrue(com.hefimer.app.ui.theme.HefimerLang.resolveIsVietnamese(com.hefimer.app.ui.theme.AppLanguage.AUTO, enVnLocale))
        assertEquals(false, com.hefimer.app.ui.theme.HefimerLang.resolveIsVietnamese(com.hefimer.app.ui.theme.AppLanguage.AUTO, enUsLocale))
        assertTrue(com.hefimer.app.ui.theme.HefimerLang.resolveIsVietnamese(com.hefimer.app.ui.theme.AppLanguage.VI, enUsLocale))
        assertEquals(false, com.hefimer.app.ui.theme.HefimerLang.resolveIsVietnamese(com.hefimer.app.ui.theme.AppLanguage.EN, viLocale))
    }

    @Test
    fun vietnameseLocalizationCoversFormatsProvidersAndCodeHighlighter() {
        val prev = com.hefimer.app.ui.theme.HefimerLang.mode
        try {
            com.hefimer.app.ui.theme.HefimerLang.update(com.hefimer.app.ui.theme.AppLanguage.VI)
            assertTrue(com.hefimer.app.ui.theme.HefimerLang.isVi)
            assertEquals("Đã hết hạn", com.hefimer.app.data.model.formatCountdown(System.currentTimeMillis() - 10_000L))
            assertEquals("Không hết hạn", com.hefimer.app.data.model.formatRemainingCompact(null))
            assertEquals("Văn bản thuần", CodeHighlighter.detectLanguage("hello world").localizedLabel())
            assertEquals("Không giới hạn", UploadProviders.getConfig("gofile").localizedMaxSize())
            assertEquals("30 giây", UploadProviders.STANDARD_EXPIRE_OPTIONS.first().localizedLabel())
            assertEquals("5 phút", UploadProviders.STANDARD_EXPIRE_OPTIONS[1].localizedLabel())
            assertEquals("Gửi Email", CodeHighlighter.getSmartAction("dev@hefimer.qzz.io")?.label)
            UploadProviders.ALL.forEach { cfg ->
                assertTrue("${cfg.id} expiryVi missing", cfg.localizedExpiry().isNotBlank() && cfg.localizedExpiry() != cfg.expiry)
                assertTrue("${cfg.id} speedVi missing", cfg.localizedSpeed().isNotBlank() && cfg.localizedSpeed() != cfg.speed)
                assertTrue("${cfg.id} featuresVi missing", cfg.localizedFeatures().isNotBlank() && cfg.localizedFeatures() != cfg.features)
                cfg.expireOptions.forEach { opt ->
                    assertTrue("${cfg.id}/${opt.value} label missing", opt.localizedLabel().isNotBlank())
                }
            }
            com.hefimer.app.ui.theme.HefimerLang.update(com.hefimer.app.ui.theme.AppLanguage.EN)
            assertEquals("My Hefimer Group", com.hefimer.app.ui.theme.tr("My Hefimer Group", "Nhóm Hefimer của tôi"))
            com.hefimer.app.ui.theme.HefimerLang.update(com.hefimer.app.ui.theme.AppLanguage.VI)
            assertEquals("Nhóm Hefimer của tôi", com.hefimer.app.ui.theme.tr("My Hefimer Group", "Nhóm Hefimer của tôi"))
        } finally {
            com.hefimer.app.ui.theme.HefimerLang.update(prev)
        }
    }

    @Test
    fun computeLineStartOffsetsHandlesLongMultilineDocumentsWithoutTruncation() {
        assertEquals(listOf(0), com.hefimer.app.ui.components.computeLineStartOffsets(""))
        assertEquals(listOf(0, 2, 4), com.hefimer.app.ui.components.computeLineStartOffsets("a\nb\n"))

        val lines = (1..420).joinToString("\n") { idx ->
            if (idx % 25 == 0) "line_$idx " + "wrapped_token_".repeat(12) else "const val line$idx = $idx"
        }
        val offsets = com.hefimer.app.ui.components.computeLineStartOffsets(lines)
        assertEquals(420, offsets.size)
        assertEquals(0, offsets.first())
        assertTrue(offsets.last() < lines.length)
        assertTrue(offsets.zipWithNext().all { (a, b) -> b > a })
    }

    @Test
    fun pcSyncedBoardAndReleaseV230ModelsHaveExpectedDefaults() {
        val rooms = com.hefimer.app.ui.RoomsUiState()
        assertEquals(1280f, rooms.boardWidth, 0.001f)
        assertEquals(720f, rooms.boardHeight, 0.001f)
        assertEquals(16f / 9f, rooms.boardWidth / rooms.boardHeight, 0.001f)

        val link = com.hefimer.app.ui.LinkUiState()
        assertEquals("", link.inviteShortCode)
        assertEquals(false, link.showPairModal)

        val outcome = com.hefimer.app.data.network.DownloadOutcome(
            mode = "download",
            fileName = "demo.pdf",
            mimeType = "application/pdf",
            sizeBytes = 2048L,
        )
        assertEquals("demo.pdf", outcome.fileName)
        assertEquals(2048L, outcome.sizeBytes)
        assertEquals("application/pdf", outcome.mimeType)
    }
}
