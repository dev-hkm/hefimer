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
 * Pure Apple-grade launch intro featuring ONLY the large circular 3-blade Hefimer logo
 * (zero outer rings, zero text):
 * 1. Silky aperture emerge (scale 0.42 -> 1.0) paired with fluid decelerated orbital spin (-180° -> 360°).
 * 2. Subtle elastic breath settle (1.0 -> 1.06 -> 1.0).
 * 3. Unhurried iris dissolve (scale 1.0 -> 1.32, trailing glide to 420°) seamlessly revealing the app.
 */
@Composable
fun HefimerLaunchIntro(
    onFinished: () -> Unit,
) {
    val reduceMotion = rememberReduceMotion()
    val backdropAlpha = remember { Animatable(1f) }
    val logoAlpha = remember { Animatable(0f) }
    val logoScale = remember { Animatable(0.42f) }
    val logoRotation = remember { Animatable(-180f) }

    LaunchedEffect(Unit) {
        if (reduceMotion) {
            logoAlpha.snapTo(1f)
            logoScale.snapTo(1f)
            logoRotation.snapTo(0f)
            delay(220L)
            backdropAlpha.animateTo(0f, tween(200, easing = AppMotion.AppleEase))
            onFinished()
            return@LaunchedEffect
        }

        // Phase 1: Pure logo emerges with fluid orbital momentum
        val enterFade = launch {
            logoAlpha.animateTo(1f, tween(420, easing = AppMotion.AppleSilk))
        }
        val enterScale = launch {
            logoScale.animateTo(1.06f, tween(620, easing = AppMotion.AppleSheet))
            logoScale.animateTo(1f, AppMotion.navigationSpring())
        }
        val enterSpin = launch {
            logoRotation.animateTo(360f, tween(1020, easing = AppMotion.AppleSheet))
        }

        enterFade.join()
        enterScale.join()
        enterSpin.join()

        delay(70L)

        // Phase 2: Velvety iris zoom-out into the live workspace
        val exitScale = launch {
            logoScale.animateTo(1.32f, tween(480, easing = AppMotion.AppleSheet))
        }
        val exitSpin = launch {
            logoRotation.animateTo(420f, tween(480, easing = AppMotion.AppleSilk))
        }
        val exitLogoFade = launch {
            logoAlpha.animateTo(0f, tween(340, easing = AppMotion.AppleEase))
        }
        val exitBgFade = launch {
            delay(60L)
            backdropAlpha.animateTo(0f, tween(440, easing = AppMotion.AppleSilk))
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
            size = 136.dp,
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
