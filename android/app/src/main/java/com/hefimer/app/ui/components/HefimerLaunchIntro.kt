package com.hefimer.app.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import com.hefimer.app.ui.theme.AppMotion
import com.hefimer.app.ui.theme.HefimerColors
import com.hefimer.app.ui.theme.rememberReduceMotion
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Seamless immediate-frame-1 Apple-grade launch intro featuring ONLY the large circular
 * 3-blade Hefimer logo (176dp, zero outer rings, zero blank flash).
 */
@Composable
fun HefimerLaunchIntro(
    onFinished: () -> Unit,
) {
    val reduceMotion = rememberReduceMotion()
    val backdropAlpha = remember { Animatable(1f) }
    val logoAlpha = remember { Animatable(1f) }
    val logoScale = remember { Animatable(0.84f) }
    val logoRotation = remember { Animatable(0f) }

    LaunchedEffect(Unit) {
        if (reduceMotion) {
            logoScale.snapTo(1f)
            delay(220L)
            backdropAlpha.animateTo(0f, tween(220, easing = AppMotion.AppleEase))
            onFinished()
            return@LaunchedEffect
        }

        // Phase 1: Immediate visible large logo smoothly expands & sweeps one calm orbital turn
        val scaleJob = launch {
            logoScale.animateTo(1.03f, tween(920, easing = AppMotion.AppleSheet))
            logoScale.animateTo(1f, AppMotion.gentleSpring())
        }
        val spinJob = launch {
            logoRotation.animateTo(360f, tween(1480, easing = AppMotion.AppleSheet))
        }

        scaleJob.join()
        spinJob.join()

        delay(120L)

        // Phase 2: Slow, velvety iris zoom-dissolve into the live app
        val exitScale = launch {
            logoScale.animateTo(1.28f, tween(680, easing = AppMotion.AppleSheet))
        }
        val exitSpin = launch {
            logoRotation.animateTo(418f, tween(680, easing = AppMotion.AppleSilk))
        }
        val exitLogoFade = launch {
            logoAlpha.animateTo(0f, tween(500, easing = AppMotion.AppleSilk))
        }
        val exitBgFade = launch {
            delay(90L)
            backdropAlpha.animateTo(0f, tween(580, easing = AppMotion.AppleSilk))
        }

        exitScale.join()
        exitSpin.join()
        exitLogoFade.join()
        exitBgFade.join()
        onFinished()
    }

    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .fillMaxSize()
            .graphicsLayer { alpha = backdropAlpha.value }
            .background(HefimerColors.Black),
    ) {
        HefimerOrbitLogo(
            size = 176.dp,
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
