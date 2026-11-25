package com.hefimer.app.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Custom-drawn monochrome geometric reaction glyphs (zero Unicode emojis).
 * Preserves the 10 canonical reaction IDs for Firebase RTDB & Web compatibility.
 */
@Composable
fun HefimerReactionGlyph(
    reactionId: String,
    tint: Color,
    size: Dp = 18.dp,
    modifier: Modifier = Modifier,
) {
    Canvas(modifier = modifier.size(size)) {
        val w = this.size.width
        val h = this.size.height
        val strokeW = (w * 0.105f).coerceAtLeast(1.4f)
        val stroke = Stroke(width = strokeW, cap = StrokeCap.Round, join = StrokeJoin.Round)
        val softFill = tint.copy(alpha = 0.22f)

        when (reactionId) {
            "heart" -> {
                val p = Path().apply {
                    moveTo(w * 0.50f, h * 0.84f)
                    cubicTo(w * 0.16f, h * 0.62f, w * 0.10f, h * 0.36f, w * 0.24f, h * 0.23f)
                    cubicTo(w * 0.34f, h * 0.14f, w * 0.46f, h * 0.20f, w * 0.50f, h * 0.30f)
                    cubicTo(w * 0.54f, h * 0.20f, w * 0.66f, h * 0.14f, w * 0.76f, h * 0.23f)
                    cubicTo(w * 0.90f, h * 0.36f, w * 0.84f, h * 0.62f, w * 0.50f, h * 0.84f)
                    close()
                }
                drawPath(p, color = softFill, style = Fill)
                drawPath(p, color = tint, style = stroke)
            }

            "thumb" -> {
                // Cuff
                drawRoundRect(
                    color = softFill,
                    topLeft = Offset(w * 0.15f, h * 0.46f),
                    size = Size(w * 0.18f, h * 0.36f),
                    cornerRadius = CornerRadius(w * 0.05f, w * 0.05f),
                )
                drawRoundRect(
                    color = tint,
                    topLeft = Offset(w * 0.15f, h * 0.46f),
                    size = Size(w * 0.18f, h * 0.36f),
                    cornerRadius = CornerRadius(w * 0.05f, w * 0.05f),
                    style = stroke,
                )
                val hand = Path().apply {
                    moveTo(w * 0.33f, h * 0.50f)
                    lineTo(w * 0.47f, h * 0.20f)
                    cubicTo(w * 0.50f, h * 0.14f, w * 0.59f, h * 0.16f, w * 0.58f, h * 0.25f)
                    lineTo(w * 0.55f, h * 0.41f)
                    lineTo(w * 0.77f, h * 0.41f)
                    cubicTo(w * 0.85f, h * 0.41f, w * 0.87f, h * 0.50f, w * 0.83f, h * 0.57f)
                    lineTo(w * 0.76f, h * 0.76f)
                    cubicTo(w * 0.74f, h * 0.80f, w * 0.70f, h * 0.82f, w * 0.65f, h * 0.82f)
                    lineTo(w * 0.33f, h * 0.82f)
                    close()
                }
                drawPath(hand, color = softFill, style = Fill)
                drawPath(hand, color = tint, style = stroke)
            }

            "smile" -> {
                drawCircle(color = softFill, radius = w * 0.37f, center = Offset(w * 0.5f, h * 0.5f))
                drawCircle(color = tint, radius = w * 0.37f, center = Offset(w * 0.5f, h * 0.5f), style = stroke)
                drawCircle(color = tint, radius = w * 0.048f, center = Offset(w * 0.37f, h * 0.41f))
                drawCircle(color = tint, radius = w * 0.048f, center = Offset(w * 0.63f, h * 0.41f))
                drawArc(
                    color = tint,
                    startAngle = 22f,
                    sweepAngle = 136f,
                    useCenter = false,
                    topLeft = Offset(w * 0.31f, h * 0.40f),
                    size = Size(w * 0.38f, h * 0.28f),
                    style = stroke,
                )
            }

            "frown" -> {
                drawCircle(color = softFill, radius = w * 0.37f, center = Offset(w * 0.5f, h * 0.5f))
                drawCircle(color = tint, radius = w * 0.37f, center = Offset(w * 0.5f, h * 0.5f), style = stroke)
                drawCircle(color = tint, radius = w * 0.048f, center = Offset(w * 0.37f, h * 0.41f))
                drawCircle(color = tint, radius = w * 0.048f, center = Offset(w * 0.63f, h * 0.41f))
                drawArc(
                    color = tint,
                    startAngle = 205f,
                    sweepAngle = 130f,
                    useCenter = false,
                    topLeft = Offset(w * 0.33f, h * 0.56f),
                    size = Size(w * 0.34f, h * 0.22f),
                    style = stroke,
                )
                // Tear dot
                drawCircle(color = tint, radius = w * 0.042f, center = Offset(w * 0.66f, h * 0.54f))
            }

            "fire" -> {
                val outer = Path().apply {
                    moveTo(w * 0.50f, h * 0.14f)
                    cubicTo(w * 0.66f, h * 0.30f, w * 0.80f, h * 0.45f, w * 0.77f, h * 0.65f)
                    cubicTo(w * 0.75f, h * 0.78f, w * 0.64f, h * 0.86f, w * 0.50f, h * 0.86f)
                    cubicTo(w * 0.34f, h * 0.86f, w * 0.23f, h * 0.76f, w * 0.23f, h * 0.61f)
                    cubicTo(w * 0.23f, h * 0.47f, w * 0.33f, h * 0.38f, w * 0.37f, h * 0.32f)
                    cubicTo(w * 0.39f, h * 0.40f, w * 0.44f, h * 0.42f, w * 0.46f, h * 0.36f)
                    cubicTo(w * 0.49f, h * 0.29f, w * 0.48f, h * 0.20f, w * 0.50f, h * 0.14f)
                    close()
                }
                drawPath(outer, color = softFill, style = Fill)
                drawPath(outer, color = tint, style = stroke)
                val inner = Path().apply {
                    moveTo(w * 0.50f, h * 0.53f)
                    cubicTo(w * 0.58f, h * 0.60f, w * 0.60f, h * 0.73f, w * 0.50f, h * 0.76f)
                    cubicTo(w * 0.40f, h * 0.73f, w * 0.42f, h * 0.60f, w * 0.50f, h * 0.53f)
                    close()
                }
                drawPath(inner, color = tint, style = Stroke(width = strokeW * 0.85f, cap = StrokeCap.Round))
            }

            "party" -> {
                val cone = Path().apply {
                    moveTo(w * 0.20f, h * 0.80f)
                    lineTo(w * 0.37f, h * 0.36f)
                    lineTo(w * 0.64f, h * 0.63f)
                    close()
                }
                drawPath(cone, color = softFill, style = Fill)
                drawPath(cone, color = tint, style = stroke)
                // Burst rays
                drawLine(tint, Offset(w * 0.54f, h * 0.36f), Offset(w * 0.66f, h * 0.20f), strokeWidth = strokeW, cap = StrokeCap.Round)
                drawLine(tint, Offset(w * 0.64f, h * 0.46f), Offset(w * 0.82f, h * 0.36f), strokeWidth = strokeW, cap = StrokeCap.Round)
                drawCircle(tint, radius = w * 0.05f, center = Offset(w * 0.50f, h * 0.20f))
                drawCircle(tint, radius = w * 0.05f, center = Offset(w * 0.80f, h * 0.56f))
            }

            "ghost" -> {
                val ghost = Path().apply {
                    moveTo(w * 0.24f, h * 0.80f)
                    lineTo(w * 0.24f, h * 0.46f)
                    cubicTo(w * 0.24f, h * 0.24f, w * 0.36f, h * 0.16f, w * 0.50f, h * 0.16f)
                    cubicTo(w * 0.64f, h * 0.16f, w * 0.76f, h * 0.24f, w * 0.76f, h * 0.46f)
                    lineTo(w * 0.76f, h * 0.80f)
                    lineTo(w * 0.64f, h * 0.71f)
                    lineTo(w * 0.50f, h * 0.81f)
                    lineTo(w * 0.36f, h * 0.71f)
                    close()
                }
                drawPath(ghost, color = softFill, style = Fill)
                drawPath(ghost, color = tint, style = stroke)
                drawCircle(tint, radius = w * 0.052f, center = Offset(w * 0.41f, h * 0.45f))
                drawCircle(tint, radius = w * 0.052f, center = Offset(w * 0.59f, h * 0.45f))
            }

            "rocket" -> {
                val body = Path().apply {
                    moveTo(w * 0.76f, h * 0.20f)
                    cubicTo(w * 0.56f, h * 0.22f, w * 0.40f, h * 0.35f, w * 0.33f, h * 0.54f)
                    lineTo(w * 0.46f, h * 0.67f)
                    cubicTo(w * 0.65f, h * 0.60f, w * 0.78f, h * 0.44f, w * 0.76f, h * 0.20f)
                    close()
                }
                drawPath(body, color = softFill, style = Fill)
                drawPath(body, color = tint, style = stroke)
                // Fins + exhaust
                drawLine(tint, Offset(w * 0.36f, h * 0.48f), Offset(w * 0.22f, h * 0.52f), strokeWidth = strokeW, cap = StrokeCap.Round)
                drawLine(tint, Offset(w * 0.52f, h * 0.64f), Offset(w * 0.48f, h * 0.78f), strokeWidth = strokeW, cap = StrokeCap.Round)
                drawLine(tint, Offset(w * 0.32f, h * 0.68f), Offset(w * 0.21f, h * 0.79f), strokeWidth = strokeW, cap = StrokeCap.Round)
                drawCircle(tint, radius = w * 0.055f, center = Offset(w * 0.58f, h * 0.40f))
            }

            "zap" -> {
                val bolt = Path().apply {
                    moveTo(w * 0.56f, h * 0.14f)
                    lineTo(w * 0.26f, h * 0.54f)
                    lineTo(w * 0.49f, h * 0.54f)
                    lineTo(w * 0.43f, h * 0.86f)
                    lineTo(w * 0.74f, h * 0.46f)
                    lineTo(w * 0.51f, h * 0.46f)
                    close()
                }
                drawPath(bolt, color = softFill, style = Fill)
                drawPath(bolt, color = tint, style = stroke)
            }

            else -> {
                // "star" (4-point crisp sparkle + accent dot)
                val star = Path().apply {
                    moveTo(w * 0.50f, h * 0.14f)
                    quadraticTo(w * 0.53f, h * 0.47f, w * 0.86f, h * 0.50f)
                    quadraticTo(w * 0.53f, h * 0.53f, w * 0.50f, h * 0.86f)
                    quadraticTo(w * 0.47f, h * 0.53f, w * 0.14f, h * 0.50f)
                    quadraticTo(w * 0.47f, h * 0.47f, w * 0.50f, h * 0.14f)
                    close()
                }
                drawPath(star, color = softFill, style = Fill)
                drawPath(star, color = tint, style = stroke)
                drawCircle(tint, radius = w * 0.045f, center = Offset(w * 0.77f, h * 0.23f))
            }
        }
    }
}
