package com.hefimer.app.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.hefimer.app.R

val UrbanistFontFamily = FontFamily(
    Font(R.font.urbanist_regular, FontWeight.Normal),
    Font(R.font.urbanist_medium, FontWeight.Medium),
    Font(R.font.urbanist_semibold, FontWeight.SemiBold),
    Font(R.font.urbanist_bold, FontWeight.Bold),
)

val BalooChettan2FontFamily = FontFamily(
    Font(R.font.baloo_chettan_2_regular, FontWeight.Normal),
    Font(R.font.baloo_chettan_2_medium, FontWeight.Medium),
    Font(R.font.baloo_chettan_2_semibold, FontWeight.SemiBold),
    Font(R.font.baloo_chettan_2_bold, FontWeight.Bold),
)

/**
 * Dynamic primary font: Baloo Chettan 2 for Vietnamese, Urbanist for English.
 */
val UbuntuFontFamily: FontFamily
    get() = if (HefimerLang.isVi) BalooChettan2FontFamily else UrbanistFontFamily

val JetBrainsMonoFamily = FontFamily(
    Font(R.font.jetbrains_mono_regular, FontWeight.Normal),
    Font(R.font.jetbrains_mono_medium, FontWeight.Medium),
)

fun hefimerTypography(isVi: Boolean = HefimerLang.isVi): Typography {
    val primary = if (isVi) BalooChettan2FontFamily else UrbanistFontFamily
    return Typography(
        displayLarge = TextStyle(
            fontFamily = primary,
            fontWeight = FontWeight.Bold,
            fontSize = 38.sp,
            lineHeight = 42.sp,
            letterSpacing = (-1.4).sp,
        ),
        displayMedium = TextStyle(
            fontFamily = primary,
            fontWeight = FontWeight.Bold,
            fontSize = 32.sp,
            lineHeight = 36.sp,
            letterSpacing = (-1.0).sp,
        ),
        headlineLarge = TextStyle(
            fontFamily = primary,
            fontWeight = FontWeight.Bold,
            fontSize = 26.sp,
            lineHeight = 30.sp,
            letterSpacing = (-0.7).sp,
        ),
        headlineMedium = TextStyle(
            fontFamily = primary,
            fontWeight = FontWeight.Bold,
            fontSize = 20.sp,
            lineHeight = 25.sp,
            letterSpacing = (-0.3).sp,
        ),
        titleLarge = TextStyle(
            fontFamily = primary,
            fontWeight = FontWeight.Bold,
            fontSize = 18.sp,
            lineHeight = 22.sp,
            letterSpacing = (-0.2).sp,
        ),
        titleMedium = TextStyle(
            fontFamily = primary,
            fontWeight = FontWeight.Bold,
            fontSize = 15.sp,
            lineHeight = 20.sp,
        ),
        bodyLarge = TextStyle(
            fontFamily = primary,
            fontWeight = FontWeight.Normal,
            fontSize = 15.sp,
            lineHeight = 22.sp,
        ),
        bodyMedium = TextStyle(
            fontFamily = primary,
            fontWeight = FontWeight.Normal,
            fontSize = 13.sp,
            lineHeight = 19.sp,
        ),
        bodySmall = TextStyle(
            fontFamily = primary,
            fontWeight = FontWeight.Normal,
            fontSize = 12.sp,
            lineHeight = 17.sp,
        ),
        labelLarge = TextStyle(
            fontFamily = primary,
            fontWeight = FontWeight.Bold,
            fontSize = 13.sp,
            lineHeight = 16.sp,
        ),
        labelMedium = TextStyle(
            fontFamily = primary,
            fontWeight = FontWeight.Bold,
            fontSize = 11.sp,
            lineHeight = 14.sp,
            letterSpacing = 0.8.sp,
        ),
        labelSmall = TextStyle(
            fontFamily = primary,
            fontWeight = FontWeight.Bold,
            fontSize = 10.sp,
            lineHeight = 12.sp,
            letterSpacing = 0.6.sp,
        ),
    )
}
