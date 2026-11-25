package com.hefimer.app.ui.components

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.qrcode.QRCodeWriter
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel
import com.hefimer.app.ui.theme.AppleGlassVariant
import com.hefimer.app.ui.theme.HefimerColors
import com.hefimer.app.ui.theme.appleGlassSurface
import com.hefimer.app.ui.theme.tr

/**
 * Exact native port of web's BeautifulQRCode:
 * crisp pixel-aligned modules + smooth rounded-corner finder patterns + monochrome & Emerald themes.
 */
object BeautifulQrRenderer {
    fun renderBitmap(
        value: String,
        sizePx: Int = 720,
        emeraldTheme: Boolean = false,
    ): Bitmap {
        val hints = mapOf(
            EncodeHintType.ERROR_CORRECTION to ErrorCorrectionLevel.L,
            EncodeHintType.MARGIN to 0,
        )
        val matrix = QRCodeWriter().encode(value, BarcodeFormat.QR_CODE, 0, 0, hints)
        val qrSize = matrix.width
        val bitmap = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        val bgColor = if (emeraldTheme) 0xFFF0FDF4.toInt() else 0xFFFFFFFF.toInt()
        val fgColor = if (emeraldTheme) 0xFF047857.toInt() else 0xFF000000.toInt()

        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        paint.color = bgColor
        canvas.drawRect(0f, 0f, sizePx.toFloat(), sizePx.toFloat(), paint)

        val baseMargin = (sizePx * 0.065f).toInt()
        val cellSize = ((sizePx - baseMargin * 2) / qrSize).coerceAtLeast(1)
        val margin = (sizePx - cellSize * qrSize) / 2

        fun isFinder(r: Int, c: Int): Boolean {
            if (r < 7 && c < 7) return true
            if (r < 7 && c >= qrSize - 7) return true
            if (r >= qrSize - 7 && c < 7) return true
            return false
        }

        paint.color = fgColor
        paint.isAntiAlias = false
        for (r in 0 until qrSize) {
            for (c in 0 until qrSize) {
                if (isFinder(r, c)) continue
                if (matrix.get(c, r)) {
                    val x = (margin + c * cellSize).toFloat()
                    val y = (margin + r * cellSize).toFloat()
                    canvas.drawRect(x, y, x + cellSize, y + cellSize, paint)
                }
            }
        }

        paint.isAntiAlias = true
        fun drawFinder(x: Float, y: Float) {
            val cs = cellSize.toFloat()
            paint.color = fgColor
            canvas.drawRoundRect(RectF(x, y, x + cs * 7, y + cs * 7), cs * 1.6f, cs * 1.6f, paint)
            paint.color = bgColor
            canvas.drawRoundRect(RectF(x + cs, y + cs, x + cs * 6, y + cs * 6), cs * 1.1f, cs * 1.1f, paint)
            paint.color = fgColor
            canvas.drawRoundRect(RectF(x + cs * 2, y + cs * 2, x + cs * 5, y + cs * 5), cs * 0.6f, cs * 0.6f, paint)
        }

        drawFinder(margin.toFloat(), margin.toFloat())
        drawFinder((margin + (qrSize - 7) * cellSize).toFloat(), margin.toFloat())
        drawFinder(margin.toFloat(), (margin + (qrSize - 7) * cellSize).toFloat())

        return bitmap
    }
}

@Composable
fun BeautifulQrCodeView(
    value: String,
    size: Dp = 200.dp,
    emeraldTheme: Boolean = false,
    modifier: Modifier = Modifier,
) {
    val bitmap = remember(value, emeraldTheme) {
        BeautifulQrRenderer.renderBitmap(value = value, sizePx = 720, emeraldTheme = emeraldTheme)
    }

    Box(
        modifier = modifier
            .appleGlassSurface(
                shape = RoundedCornerShape(28.dp),
                variant = AppleGlassVariant.Clear,
                elevation = 4.dp,
            )
            .padding(8.dp),
    ) {
        Image(
            bitmap = bitmap.asImageBitmap(),
            contentDescription = tr("QR Code", "Mã QR"),
            modifier = Modifier
                .size(size)
                .clip(RoundedCornerShape(20.dp)),
        )
    }
}
