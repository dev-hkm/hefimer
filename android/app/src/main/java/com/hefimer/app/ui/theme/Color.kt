package com.hefimer.app.ui.theme

import androidx.compose.ui.graphics.Color

object HefimerColors {
    // Pure OLED monochrome base matching web index.css & App.tsx
    val Black = Color(0xFF000000)
    val SurfaceElevated = Color(0xFF070707)
    val SurfaceCard = Color(0xFF0A0A0A)
    val SurfaceCardAlt = Color(0xFF111111)
    val SurfaceSheet = Color(0xFF090909)

    val White = Color(0xFFFFFFFF)
    val White90 = Color(0xE6FFFFFF)
    val White80 = Color(0xCCFFFFFF)
    val White70 = Color(0xB3FFFFFF)
    val White60 = Color(0x99FFFFFF)
    val White50 = Color(0x80FFFFFF)
    val White40 = Color(0x66FFFFFF)
    val White30 = Color(0x4DFFFFFF)
    val White20 = Color(0x33FFFFFF)
    val White15 = Color(0x26FFFFFF)
    val White12 = Color(0x1FFFFFFF)
    val White10 = Color(0x1AFFFFFF)
    val White08 = Color(0x14FFFFFF)
    val White06 = Color(0x0FFFFFFF)
    val White04 = Color(0x0AFFFFFF)
    val White025 = Color(0x06FFFFFF)

    // Secret R2 & live accents (Emerald 400 / 500)
    val Emerald300 = Color(0xFF6EE7B7)
    val Emerald400 = Color(0xFF34D399)
    val Emerald500 = Color(0xFF10B981)
    val Emerald700 = Color(0xFF047857)
    val EmeraldBg = Color(0x0A10B981)
    val EmeraldBorder = Color(0x3310B981)

    // Semantic accents
    val Red400 = Color(0xFFF87171)
    val Red500 = Color(0xFFEF4444)
    val RedBg = Color(0x1AEF4444)
    val RedBorder = Color(0x4DEF4444)

    val Blue400 = Color(0xFF60A5FA)
    val Purple400 = Color(0xFFC084FC)
    val Pink400 = Color(0xFFF472B6)
    val Amber400 = Color(0xFFFBBF24)
    val Sky400 = Color(0xFF38BDF8)
    val Rose400 = Color(0xFFFB7185)
    val Violet400 = Color(0xFFA78BFA)
    val Cyan400 = Color(0xFF22D3EE)

    // Dracula syntax highlighting tokens (exact match to src/index.css)
    val SyntaxComment = Color(0xFF6272A4)
    val SyntaxPunctuation = Color(0xFFF8F8F2)
    val SyntaxKeyword = Color(0xFFFF79C6)
    val SyntaxNumber = Color(0xFFBD93F9)
    val SyntaxFunction = Color(0xFF50FA7B)
    val SyntaxString = Color(0xFFF1FA8C)
    val SyntaxType = Color(0xFF8BE9FD)
    val SyntaxDecorator = Color(0xFFFFB86C)

    // Space sticky note palettes
    val NoteZincBg = Color(0xE618181B)
    val NoteZincBorder = Color(0x26FFFFFF)
    val NoteEmeraldBg = Color(0xBF022C22)
    val NoteEmeraldBorder = Color(0x4D34D399)
    val NoteAmberBg = Color(0xBF451A03)
    val NoteAmberBorder = Color(0x4DFBBF24)
    val NoteSkyBg = Color(0xBF082F49)
    val NoteSkyBorder = Color(0x4D38BDF8)
    val NoteRoseBg = Color(0xBF4C0519)
    val NoteRoseBorder = Color(0x4DFB7185)
    val NoteVioletBg = Color(0xBF2E1065)
    val NoteVioletBorder = Color(0x4DA78BFA)
}
