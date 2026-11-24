package com.hefimer.app.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.hefimer.app.ui.theme.HefimerColors
import com.hefimer.app.ui.theme.rememberReduceMotion

private const val ORBIT_BLADE_PATH =
    "M64 15c25.4 0 46 20.6 46 46 0 7.8-2 15.4-5.7 22.1L84.7 71.7c1.1-2.6 1.7-5.4 1.7-8.3 0-12.3-10-22.3-22.3-22.3V15Z"

/**
 * Exact 3-blade Hefimer aperture orbit mark matching public/hefimer-orbit-clean.svg.
 * Linear easing is intentionally used exclusively for continuous orbital rotation (never UI springs).
 */
@Composable
fun HefimerOrbitLogo(
    modifier: Modifier = Modifier,
    size: Dp = 32.dp,
    tint: Color = HefimerColors.White,
    spinning: Boolean = false,
    spinDurationMs: Int = 25_000,
) {
    val reduceMotion = rememberReduceMotion()
    val bladePath = remember {
        Path().apply {
            PathParser().parsePathString(ORBIT_BLADE_PATH).toPath(this)
        }
    }

    val angle = if (spinning && !reduceMotion) {
        val transition = rememberInfiniteTransition(label = "hefimer_orbit")
        val anim by transition.animateFloat(
            initialValue = 0f,
            targetValue = 360f,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = spinDurationMs, easing = LinearEasing),
                repeatMode = RepeatMode.Restart,
            ),
            label = "orbit_angle",
        )
        anim
    } else {
        0f
    }

    Canvas(
        modifier = modifier
            .size(size)
            .graphicsLayer { rotationZ = angle },
    ) {
        val factor = minOf(this.size.width, this.size.height) / 128f
        scale(scaleX = factor, scaleY = factor, pivot = androidx.compose.ui.geometry.Offset.Zero) {
            for (deg in listOf(0f, 120f, 240f)) {
                rotate(degrees = deg, pivot = androidx.compose.ui.geometry.Offset(64f, 64f)) {
                    drawPath(path = bladePath, color = tint)
                }
            }
        }
    }
}
