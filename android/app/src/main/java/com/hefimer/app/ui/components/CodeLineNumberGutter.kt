package com.hefimer.app.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hefimer.app.ui.theme.HefimerColors
import com.hefimer.app.ui.theme.JetBrainsMonoFamily
import kotlin.math.max

internal fun computeLineStartOffsets(text: String): List<Int> {
    val list = ArrayList<Int>(max(16, text.length / 32))
    list.add(0)
    for (i in text.indices) {
        if (text[i] == '\n') list.add(i + 1)
    }
    return list
}

/**
 * Pixel-exact line-number gutter driven by the target Text / BasicTextField's [TextLayoutResult].
 * Guarantees every logical line 1..N stays aligned to its exact baseline even with soft-wrapping
 * or 10,000+ line documents (zero truncation or cumulative drift).
 */
@Composable
fun CodeLineNumberGutter(
    text: String,
    textLayoutResult: TextLayoutResult?,
    modifier: Modifier = Modifier,
    color: Color = HefimerColors.White30,
) {
    val lineStartOffsets = remember(text) { computeLineStartOffsets(text) }
    val totalLines = lineStartOffsets.size
    val digits = totalLines.toString().length.coerceAtLeast(2)
    val gutterWidth = (digits * 8 + 10).dp
    val density = LocalDensity.current
    val textMeasurer = rememberTextMeasurer(cacheSize = 256)
    val numStyle = remember(color) {
        TextStyle(
            fontFamily = JetBrainsMonoFamily,
            fontSize = 11.5.sp,
            color = color,
            fontFeatureSettings = "tnum",
        )
    }

    val contentHeightDp = with(density) {
        val px = textLayoutResult?.size?.height?.toFloat() ?: (totalLines * 20.sp.toPx())
        px.toDp().coerceAtLeast(20.dp)
    }

    Canvas(
        modifier = modifier
            .padding(end = 10.dp)
            .width(gutterWidth)
            .height(contentHeightDp),
    ) {
        val layout = textLayoutResult
        if (layout == null) {
            val fallbackLineHeight = 20.sp.toPx()
            for (i in 0 until totalLines) {
                val measured = textMeasurer.measure((i + 1).toString(), numStyle)
                val x = (size.width - measured.size.width).coerceAtLeast(0f)
                val y = i * fallbackLineHeight + (fallbackLineHeight - measured.size.height) / 2f
                if (y <= size.height) {
                    drawText(measured, topLeft = Offset(x, y))
                }
            }
            return@Canvas
        }

        val maxOffset = layout.layoutInput.text.length
        for (idx in lineStartOffsets.indices) {
            val offset = lineStartOffsets[idx].coerceIn(0, maxOffset)
            val visualLine = layout.getLineForOffset(offset)
            val top = layout.getLineTop(visualLine)
            val bottom = layout.getLineBottom(visualLine)
            val measured = textMeasurer.measure((idx + 1).toString(), numStyle)
            val x = (size.width - measured.size.width).coerceAtLeast(0f)
            val y = top + ((bottom - top) - measured.size.height) / 2f
            drawText(measured, topLeft = Offset(x, y))
        }
    }
}
