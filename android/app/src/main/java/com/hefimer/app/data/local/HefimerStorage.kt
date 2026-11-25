package com.hefimer.app.data.local

import android.content.Context
import android.os.Build
import com.hefimer.app.data.model.DownloadedFileRecord
import com.hefimer.app.data.model.HistoryItem
import com.hefimer.app.data.model.RecentSpaceEntry
import com.hefimer.app.data.model.UploadStats
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.util.UUID

class HefimerStorage(context: Context) {
    private val prefs = context.getSharedPreferences("hefimer_native_prefs", Context.MODE_PRIVATE)
    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    init {
        // Ensure v2.4.0 upgrade sets storage.to as default for users who previously had the old stocare default
        if (!prefs.getBoolean("hefimer_migrated_default_storageto_v1", false)) {
            prefs.edit().apply {
                if (prefs.getString("hefimer_default_provider", null) == "stocare") {
                    putString("hefimer_default_provider", "storageto")
                }
                if (prefs.getString("hefimer_space_provider", null) == "stocare") {
                    putString("hefimer_space_provider", "storageto")
                }
                putBoolean("hefimer_migrated_default_storageto_v1", true)
            }.apply()
        }
    }

    fun loadHistory(): List<HistoryItem> {
        val raw = prefs.getString("hefimer_history", null) ?: return emptyList()
        return runCatching { json.decodeFromString<List<HistoryItem>>(raw) }.getOrDefault(emptyList())
    }

    fun addHistory(item: HistoryItem): List<HistoryItem> {
        val current = loadHistory()
        if (current.any { it.code == item.code && it.action == item.action }) return current
        val updated = (listOf(item) + current).take(25)
        prefs.edit().putString("hefimer_history", json.encodeToString(updated)).apply()
        return updated
    }

    fun clearHistory() {
        prefs.edit().remove("hefimer_history").apply()
    }

    fun loadUploadStats(isR2: Boolean): UploadStats {
        val key = if (isR2) "hefimer_r2_stats" else "hefimer_standard_stats"
        val raw = prefs.getString(key, null) ?: return UploadStats()
        val parsed = runCatching { json.decodeFromString<UploadStats>(raw) }.getOrDefault(UploadStats())
        val now = System.currentTimeMillis()
        if (parsed.firstUploadTime != 0L && now - parsed.firstUploadTime > 24L * 3_600_000L) {
            prefs.edit().remove(key).apply()
            return UploadStats()
        }
        return parsed
    }

    fun recordUploadStats(sizeBytes: Long, isR2: Boolean): UploadStats {
        val key = if (isR2) "hefimer_r2_stats" else "hefimer_standard_stats"
        val prev = loadUploadStats(isR2)
        val now = System.currentTimeMillis()
        val next = if (prev.firstUploadTime == 0L || now - prev.firstUploadTime > 24L * 3_600_000L) {
            UploadStats(firstUploadTime = now, totalSize = sizeBytes)
        } else {
            prev.copy(totalSize = prev.totalSize + sizeBytes)
        }
        prefs.edit().putString(key, json.encodeToString(next)).apply()
        return next
    }

    fun loadRecentSpaces(): List<RecentSpaceEntry> {
        val raw = prefs.getString("hefimer_recent_spaces", null) ?: return emptyList()
        return runCatching { json.decodeFromString<List<RecentSpaceEntry>>(raw) }
            .getOrDefault(emptyList())
            .filter { it.code.length == 5 }
            .take(5)
    }

    fun saveRecentSpace(code: String, name: String): List<RecentSpaceEntry> {
        val updated = (listOf(RecentSpaceEntry(code, name.ifBlank { "Space $code" })) +
            loadRecentSpaces().filter { it.code != code }).take(5)
        prefs.edit().putString("hefimer_recent_spaces", json.encodeToString(updated)).apply()
        return updated
    }

    fun getUserName(): String = prefs.getString("hefimer_username", "") ?: ""
    fun setUserName(name: String) {
        prefs.edit().putString("hefimer_username", name.trim()).apply()
    }

    fun getSpaceProvider(): String = prefs.getString("hefimer_space_provider", "storageto") ?: "storageto"
    fun setSpaceProvider(id: String) {
        prefs.edit().putString("hefimer_space_provider", id).apply()
    }

    fun getDefaultUploadProvider(): String = prefs.getString("hefimer_default_provider", "storageto") ?: "storageto"
    fun setDefaultUploadProvider(id: String) {
        prefs.edit().putString("hefimer_default_provider", id).apply()
    }

    fun isLightMode(): Boolean = prefs.getBoolean("hefimer_light_mode", false)
    fun setLightMode(enabled: Boolean) {
        prefs.edit().putBoolean("hefimer_light_mode", enabled).apply()
    }

    fun hasCompletedOnboarding(): Boolean = prefs.getBoolean("hefimer_onboarding_done_v1", false)
    fun setOnboardingCompleted(completed: Boolean) {
        prefs.edit().putBoolean("hefimer_onboarding_done_v1", completed).apply()
    }

    fun getAppLanguageCode(): String = prefs.getString("hefimer_app_language", "auto") ?: "auto"
    fun setAppLanguageCode(code: String) {
        prefs.edit().putString("hefimer_app_language", code).apply()
    }

    fun getDefaultStartTab(): String = prefs.getString("hefimer_default_start_tab", "SEND") ?: "SEND"
    fun setDefaultStartTab(tabName: String) {
        prefs.edit().putString("hefimer_default_start_tab", tabName).apply()
    }

    fun isPromptOpenAfterDownload(): Boolean = prefs.getBoolean("hefimer_prompt_open_after_download", true)
    fun setPromptOpenAfterDownload(enabled: Boolean) {
        prefs.edit().putBoolean("hefimer_prompt_open_after_download", enabled).apply()
    }

    fun isSmartClipboardEnabled(): Boolean = prefs.getBoolean("hefimer_smart_clipboard", true)
    fun setSmartClipboardEnabled(enabled: Boolean) {
        prefs.edit().putBoolean("hefimer_smart_clipboard", enabled).apply()
    }

    fun isHapticEnabled(): Boolean = prefs.getBoolean("hefimer_haptic_enabled", true)
    fun setHapticEnabled(enabled: Boolean) {
        prefs.edit().putBoolean("hefimer_haptic_enabled", enabled).apply()
    }

    fun isConfirmRedownloadEnabled(): Boolean = prefs.getBoolean("hefimer_confirm_redownload", true)
    fun setConfirmRedownloadEnabled(enabled: Boolean) {
        prefs.edit().putBoolean("hefimer_confirm_redownload", enabled).apply()
    }

    fun loadDownloadedFiles(): List<DownloadedFileRecord> {
        val raw = prefs.getString("hefimer_downloaded_files_v1", null) ?: return emptyList()
        return runCatching { json.decodeFromString<List<DownloadedFileRecord>>(raw) }.getOrDefault(emptyList())
    }

    fun findDownloadedFile(key: String, fileName: String, sizeBytes: Long): DownloadedFileRecord? {
        val list = loadDownloadedFiles()
        return list.firstOrNull { rec ->
            (key.isNotBlank() && rec.key == key) ||
                (rec.fileName.equals(fileName, ignoreCase = true) && (sizeBytes <= 0L || rec.fileSize == sizeBytes))
        }
    }

    fun recordDownloadedFile(record: DownloadedFileRecord) {
        val updated = (listOf(record) + loadDownloadedFiles().filterNot {
            (record.key.isNotBlank() && it.key == record.key) ||
                (it.fileName.equals(record.fileName, ignoreCase = true) && it.fileSize == record.fileSize)
        }).take(100)
        prefs.edit().putString("hefimer_downloaded_files_v1", json.encodeToString(updated)).apply()
    }

    fun getVisitorToken(): String {
        val existing = prefs.getString("hefimer_storageto_visitor_token", null)
        if (!existing.isNullOrBlank()) return existing
        val created = UUID.randomUUID().toString().replace("-", "")
        prefs.edit().putString("hefimer_storageto_visitor_token", created).apply()
        return created
    }

    fun getStorageToApiToken(): String = prefs.getString("hefimer_storageto_api_token", "")?.trim().orEmpty()
    fun setStorageToApiToken(token: String) {
        val clean = token.trim()
        prefs.edit().apply {
            if (clean.isEmpty()) remove("hefimer_storageto_api_token")
            else putString("hefimer_storageto_api_token", clean)
        }.apply()
    }

    fun getDeviceName(): String {
        val saved = prefs.getString("hefimer_device_name", null)
        if (!saved.isNullOrBlank()) return saved
        val model = Build.MODEL?.takeIf { it.isNotBlank() } ?: "Android"
        val brand = Build.MANUFACTURER?.replaceFirstChar { it.uppercase() }.orEmpty()
        val combined = if (brand.isNotBlank() && !model.startsWith(brand, ignoreCase = true)) {
            "$brand $model"
        } else {
            model
        }
        return if (com.hefimer.app.ui.theme.HefimerLang.isVi) "$combined (Ứng dụng)" else "$combined (App)"
    }

    fun setDeviceName(name: String) {
        prefs.edit().putString("hefimer_device_name", name.trim()).apply()
    }

    fun isDeviceLinkEnabled(): Boolean = prefs.getBoolean("hefimer_device_enabled", false)
    fun setDeviceLinkEnabled(enabled: Boolean) {
        prefs.edit().putBoolean("hefimer_device_enabled", enabled).apply()
    }

    fun getStoredString(key: String): String? = prefs.getString(key, null)
    fun putStoredString(key: String, value: String) {
        prefs.edit().putString(key, value).apply()
    }

    fun getOrCreateClientId(): String {
        val existing = prefs.getString("hefimer_client_id", null)
        if (!existing.isNullOrBlank()) return existing
        val created = "sp_${UUID.randomUUID().toString().replace("-", "").take(14)}"
        prefs.edit().putString("hefimer_client_id", created).apply()
        return created
    }
}
