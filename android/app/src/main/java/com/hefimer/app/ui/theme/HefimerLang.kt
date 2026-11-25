package com.hefimer.app.ui.theme

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import java.util.Locale

enum class AppLanguage(val code: String) {
    AUTO("auto"),
    VI("vi"),
    EN("en"),
    ;

    companion object {
        fun fromCode(code: String?): AppLanguage =
            entries.firstOrNull { it.code.equals(code, ignoreCase = true) } ?: AUTO
    }
}

/**
 * Reactive app-wide language state (mirrors HefimerColors.isLightMode pattern).
 * Automatically detects Vietnamese language or VN region when set to AppLanguage.AUTO,
 * and recomposes every Compose screen immediately when switched in Settings.
 */
object HefimerLang {
    var mode by mutableStateOf(AppLanguage.AUTO)
        private set

    var isVi by mutableStateOf(detectSystemVietnamese())
        private set

    fun detectSystemVietnamese(locale: Locale = Locale.getDefault()): Boolean =
        locale.language.equals("vi", ignoreCase = true) ||
            locale.country.equals("VN", ignoreCase = true)

    fun resolveIsVietnamese(language: AppLanguage, locale: Locale = Locale.getDefault()): Boolean =
        when (language) {
            AppLanguage.VI -> true
            AppLanguage.EN -> false
            AppLanguage.AUTO -> detectSystemVietnamese(locale)
        }

    fun update(language: AppLanguage, locale: Locale = Locale.getDefault()) {
        mode = language
        isVi = resolveIsVietnamese(language, locale)
    }
}

/**
 * Concise bilingual helper: returns Vietnamese when active, English otherwise.
 * Reading [HefimerLang.isVi] inside any @Composable automatically subscribes it to live language switches.
 */
inline fun tr(en: String, vi: String): String = if (HefimerLang.isVi) vi else en
