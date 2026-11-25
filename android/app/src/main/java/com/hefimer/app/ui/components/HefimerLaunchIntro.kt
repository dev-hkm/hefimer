package com.hefimer.app.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import com.hefimer.app.ui.theme.AppMotion
import com.hefimer.app.ui.theme.HefimerColors
import com.hefimer.app.ui.theme.rememberReduceMotion
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Minimalist Apple-grade launch intro using exclusively the circular 3-blade Hefimer aperture mark.
 * Choreography:
 * 1. Aperture breathes in softly (scale 0.68 -> 1.0) while sweeping a decelerated orbital rotation (-110° -> 360°).
 * 2. Concentric subtle orbital halo traces around the circular logo and dissolves softly.
 * 3. Unhurried iris exit: logo expands gently (1.0 -> 1.16) as the OLED canvas fades into the live app.
 */
@Composable
fun HefimerLaunchIntro(
    onFinished: () -> Unit,
) {
    val reduceMotion = rememberReduceMotion()
    val backdropAlpha = remember { Animatable(1f) }
    val logoAlpha = remember { Animatable(0f) }
    val logoScale = remember { Animatable(0.68f) }
    val logoRotation = remember { Animatable(-110f) }
    val ringSweep = remember { Animatable(0f) }
    val ringAlpha = remember { Animatable(0f) }

    LaunchedEffect(Unit) {
        if (reduceMotion) {
            logoAlpha.snapTo(1f)
            logoScale.snapTo(1f)
            logoRotation.snapTo(0f)
            delay(260L)
            backdropAlpha.animateTo(0f, tween(200, easing = AppMotion.AppleEase))
            onFinished()
            return@LaunchedEffect
        }

        // Phase 1: Silky entrance + decelerated orbital spin + orbital halo sweep
        val enterAlphaJob = launch {
            logoAlpha.animateTo(1f, tween(460, easing = AppMotion.AppleSilk))
        }
        val enterScaleJob = launch {
            logoScale.animateTo(1f, AppMotion.navigationSpring())
        }
        val spinJob = launch {
            logoRotation.animateTo(360f, tween(1050, easing = AppMotion.AppleSheet))
        }
        val ringJob = launch {
            delay(90L)
            launch { ringAlpha.animateTo(1f, tween(280, easing = AppMotion.AppleSilk)) }
            ringSweep.animateTo(360f, tween(760, easing = AppMotion.AppleSheet))
            ringAlpha.animateTo(0f, tween(340, easing = AppMotion.AppleEase))
        }

        enterAlphaJob.join()
        enterScaleJob.join()
        spinJob.join()
        ringJob.join()

        delay(80L)

        // Phase 2: Unhurried Apple iris dissolve into main workspace
        val exitScaleJob = launch {
            logoScale.animateTo(1.16f, tween(480, easing = AppMotion.AppleSheet))
        }
        val exitSpinJob = launch {
            logoRotation.animateTo(405f, tween(480, easing = AppMotion.AppleSilk))
        }
        val exitLogoFadeJob = launch {
            logoAlpha.animateTo(0f, tween(360, easing = AppMotion.AppleEase))
        }
        val exitBgFadeJob = launch {
            delay(70L)
            backdropAlpha.animateTo(0f, tween(440, easing = AppMotion.AppleSilk))
        }

        exitScaleJob.join()
        exitSpinJob.join()
        exitLogoFadeJob.join()
        exitBgFadeJob.join()
        onFinished()
    }

    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .fillMaxSize()
            .graphicsLayer { alpha = backdropAlpha.value }
            .background(HefimerColors.Black),
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.size(164.dp),
        ) {
            // Subtle concentric orbital halo ring tracing around the circular Hefimer logo
            Canvas(
                modifier = Modifier
                    .size(144.dp)
                    .graphicsLayer {
                        alpha = ringAlpha.value
                        scaleX = 0.92f + 0.08f * logoScale.value
                        scaleY = 0.92f + 0.08f * logoScale.value
                    },
            ) {
                val strokeW = 1.6.dp.toPx()
                val inset = strokeW / 2f + 4.dp.toPx()
                val arcSize = Size(size.width - inset * 2f, size.height - inset * 2f)
                val topLeft = Offset(inset, inset)

                // Soft ambient radial glow behind logo
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(Color.White.copy(alpha = 0.11f), Color.Transparent),
                        center = center,
                        radius = size.minDimension * 0.5f,
                    ),
                )

                // Sweeping orbital hairline
                if (ringSweep.value > 0.5f) {
                    drawArc(
                        color = Color.White.copy(alpha = 0.42f),
                        startAngle = -90f,
                        sweepAngle = ringSweep.value,
                        useCenter = false,
                        topLeft = topLeft,
                        size = arcSize,
                        style = Stroke(width = strokeW, cap = StrokeCap.Round),
                    )
                }
            }

            // Pure circular 3-blade Hefimer Orbit Mark
            HefimerOrbitLogo(
                size = 86.dp,
                tint = HefimerColors.White,
                spinning = false,
                modifier = Modifier.graphicsLayer {
                    alpha = logoAlpha.value
                    scaleX = logoScale.value
                    scaleY = logoScale.value
                    rotationZ = logoRotation.value
                },
            )
        }
    }
}
