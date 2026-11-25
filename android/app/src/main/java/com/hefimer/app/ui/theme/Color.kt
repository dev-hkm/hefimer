package com.hefimer.app.ui.theme

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color

object HefimerColors {
    var isLightMode by mutableStateOf(false)

    // Canvas & Surface hierarchy (OLED Dark <-> Apple Light)
    val Black: Color get() = if (isLightMode) Color(0xFFF5F5F7) else Color(0xFF000000)
    val SurfaceElevated: Color get() = if (isLightMode) Color(0xFFFFFFFF) else Color(0xFF070707)
    val SurfaceCard: Color get() = if (isLightMode) Color(0xFFFFFFFF) else Color(0xFF0A0A0A)
    val SurfaceCardAlt: Color get() = if (isLightMode) Color(0xFFEDEDF0) else Color(0xFF111111)
    val SurfaceSheet: Color get() = if (isLightMode) Color(0xFFFFFFFF) else Color(0xFF090909)
    val DockBg: Color get() = if (isLightMode) Color(0xF0FFFFFF) else Color(0xEB0D0D0D)
    val BannerBg: Color get() = if (isLightMode) Color(0xF5FFFFFF) else Color(0xF2101010)

    // Ink & hairline opacity scale (automatically inverted for crisp contrast in Light Mode)
    val White: Color get() = if (isLightMode) Color(0xFF0B0B0C) else Color(0xFFFFFFFF)
    val White90: Color get() = if (isLightMode) Color(0xE60B0B0C) else Color(0xE6FFFFFF)
    val White85: Color get() = if (isLightMode) Color(0xD90B0B0C) else Color(0xD9FFFFFF)
    val White80: Color get() = if (isLightMode) Color(0xCC0B0B0C) else Color(0xCCFFFFFF)
    val White75: Color get() = if (isLightMode) Color(0xBF0B0B0C) else Color(0xBFFFFFFF)
    val White70: Color get() = if (isLightMode) Color(0xB30B0B0C) else Color(0xB3FFFFFF)
    val White60: Color get() = if (isLightMode) Color(0x990B0B0C) else Color(0x99FFFFFF)
    val White55: Color get() = if (isLightMode) Color(0x8C0B0B0C) else Color(0x8CFFFFFF)
    val White50: Color get() = if (isLightMode) Color(0x800B0B0C) else Color(0x80FFFFFF)
    val White45: Color get() = if (isLightMode) Color(0x730B0B0C) else Color(0x73FFFFFF)
    val White40: Color get() = if (isLightMode) Color(0x660B0B0C) else Color(0x66FFFFFF)
    val White30: Color get() = if (isLightMode) Color(0x4D0B0B0C) else Color(0x4DFFFFFF)
    val White20: Color get() = if (isLightMode) Color(0x2E0B0B0C) else Color(0x33FFFFFF)
    val White15: Color get() = if (isLightMode) Color(0x220B0B0C) else Color(0x26FFFFFF)
    val White12: Color get() = if (isLightMode) Color(0x1A0B0B0C) else Color(0x1FFFFFFF)
    val White10: Color get() = if (isLightMode) Color(0x140B0B0C) else Color(0x1AFFFFFF)
    val White08: Color get() = if (isLightMode) Color(0x100B0B0C) else Color(0x14FFFFFF)
    val White06: Color get() = if (isLightMode) Color(0x0B0B0B0C) else Color(0x0FFFFFFF)
    val White04: Color get() = if (isLightMode) Color(0x080B0B0C) else Color(0x0AFFFFFF)
    val White03: Color get() = if (isLightMode) Color(0x060B0B0C) else Color(0x08FFFFFF)
    val White025: Color get() = if (isLightMode) Color(0x050B0B0C) else Color(0x06FFFFFF)

    // Secret R2 & live accents (Emerald)
    val Emerald300: Color get() = if (isLightMode) Color(0xFF047857) else Color(0xFF6EE7B7)
    val Emerald400: Color get() = if (isLightMode) Color(0xFF059669) else Color(0xFF34D399)
    val Emerald500: Color get() = Color(0xFF10B981)
    val Emerald700: Color get() = Color(0xFF047857)
    val EmeraldBg: Color get() = if (isLightMode) Color(0x14059669) else Color(0x0A10B981)
    val EmeraldBorder: Color get() = if (isLightMode) Color(0x40059669) else Color(0x3310B981)

    // Semantic accents
    val Red400: Color get() = if (isLightMode) Color(0xFFDC2626) else Color(0xFFF87171)
    val Red500: Color get() = Color(0xFFEF4444)
    val RedBg: Color get() = if (isLightMode) Color(0x14DC2626) else Color(0x1AEF4444)
    val RedBorder: Color get() = if (isLightMode) Color(0x40DC2626) else Color(0x4DEF4444)

    val Blue400: Color get() = if (isLightMode) Color(0xFF2563EB) else Color(0xFF60A5FA)
    val Purple400: Color get() = if (isLightMode) Color(0xFF7C3AED) else Color(0xFFC084FC)
    val Pink400: Color get() = if (isLightMode) Color(0xFFDB2777) else Color(0xFFF472B6)
    val Amber400: Color get() = if (isLightMode) Color(0xFFD97706) else Color(0xFFFBBF24)
    val Sky400: Color get() = if (isLightMode) Color(0xFF0284C7) else Color(0xFF38BDF8)
    val Rose400: Color get() = if (isLightMode) Color(0xFFE11D48) else Color(0xFFFB7185)
    val Violet400: Color get() = if (isLightMode) Color(0xFF7C3AED) else Color(0xFFA78BFA)
    val Cyan400: Color get() = if (isLightMode) Color(0xFF0891B2) else Color(0xFF22D3EE)

    // Dracula syntax highlighting tokens (adapted for readability in Light Mode)
    val SyntaxComment: Color get() = if (isLightMode) Color(0xFF6B7280) else Color(0xFF6272A4)
    val SyntaxPunctuation: Color get() = if (isLightMode) Color(0xFF1F2937) else Color(0xFFF8F8F2)
    val SyntaxKeyword: Color get() = if (isLightMode) Color(0xFFDB2777) else Color(0xFFFF79C6)
    val SyntaxNumber: Color get() = if (isLightMode) Color(0xFF7C3AED) else Color(0xFFBD93F9)
    val SyntaxFunction: Color get() = if (isLightMode) Color(0xFF059669) else Color(0xFF50FA7B)
    val SyntaxString: Color get() = if (isLightMode) Color(0xFFB45309) else Color(0xFFF1FA8C)
    val SyntaxType: Color get() = if (isLightMode) Color(0xFF0284C7) else Color(0xFF8BE9FD)
    val SyntaxDecorator: Color get() = if (isLightMode) Color(0xFFEA580C) else Color(0xFFFFB86C)

    // Space sticky note palettes
    val NoteZincBg: Color get() = if (isLightMode) Color(0xF2F4F4F5) else Color(0xE618181B)
    val NoteZincBorder: Color get() = if (isLightMode) Color(0x26000000) else Color(0x26FFFFFF)
    val NoteEmeraldBg: Color get() = if (isLightMode) Color(0xF2ECFDF5) else Color(0xBF022C22)
    val NoteEmeraldBorder: Color get() = if (isLightMode) Color(0x55059669) else Color(0x4D34D399)
    val NoteAmberBg: Color get() = if (isLightMode) Color(0xF2FFFBEB) else Color(0xBF451A03)
    val NoteAmberBorder: Color get() = if (isLightMode) Color(0x55D97706) else Color(0x4DFBBF24)
    val NoteSkyBg: Color get() = if (isLightMode) Color(0xF2F0F9FF) else Color(0xBF082F49)
    val NoteSkyBorder: Color get() = if (isLightMode) Color(0x550284C7) else Color(0x4D38BDF8)
    val NoteRoseBg: Color get() = if (isLightMode) Color(0xF2FFF1F2) else Color(0xBF4C0519)
    val NoteRoseBorder: Color get() = if (isLightMode) Color(0x55E11D48) else Color(0x4DFB7185)
    val NoteVioletBg: Color get() = if (isLightMode) Color(0xF2F5F3FF) else Color(0xBF2E1065)
    val NoteVioletBorder: Color get() = if (isLightMode) Color(0x557C3AED) else Color(0x4DA78BFA)
}
