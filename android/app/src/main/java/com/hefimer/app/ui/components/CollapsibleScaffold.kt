package com.hefimer.app.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hefimer.app.ui.theme.AppMotion
import com.hefimer.app.ui.theme.HefimerColors
import com.hefimer.app.ui.theme.UbuntuFontFamily
import com.hefimer.app.ui.theme.applePressable

/**
 * Edge-to-edge collapsible scrolling header scaffold used across EVERY Hefimer screen.
 * - Extends seamlessly behind the transparent status bar and navigation bar.
 * - Large editorial header smoothly collapses into a compact translucent top bar via GPU graphicsLayer.
 * - Uses coarsened derivedStateOf threshold for compact bar visibility (zero 120Hz recomposition jitter).
 * - Content glides seamlessly underneath both the translucent top bar and bottom floating dock.
 */
@Composable
fun HefimerCollapsibleScaffold(
    kicker: String,
    title: String,
    subtitle: String,
    modifier: Modifier = Modifier,
    scrollState: ScrollState = rememberScrollState(),
    accentColor: Color = HefimerColors.White,
    onTitleClick: (() -> Unit)? = null,
    showOrbitMark: Boolean = false,
    bottomDockClearance: Dp = 108.dp,
    headerActions: @Composable RowScope.() -> Unit = {},
    content: @Composable ColumnScope.() -> Unit,
) {
    val density = LocalDensity.current
    val statusTop = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    val navBottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
    val collapseThresholdPx = with(density) { 86.dp.toPx() }

    val isCollapsed by remember(scrollState) {
        derivedStateOf { scrollState.value > collapseThresholdPx }
    }

    val compactAlpha by animateFloatAsState(
        targetValue = if (isCollapsed) 1f else 0f,
        animationSpec = tween(AppMotion.Fast, easing = AppMotion.AppleSilk),
        label = "compact_header_alpha",
    )
    val compactSlideY by animateFloatAsState(
        targetValue = if (isCollapsed) 0f else 10f,
        animationSpec = tween(AppMotion.Fast, easing = AppMotion.AppleSheet),
        label = "compact_header_slide",
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(HefimerColors.Black),
    ) {
        // Scrollable full-bleed content flowing behind transparent status bar & floating bottom dock
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(horizontal = 20.dp),
        ) {
            // Top clearance for status bar + compact action bar
            Spacer(Modifier.height(statusTop + 56.dp))

            // Large collapsible editorial header driven strictly inside GPU graphicsLayer
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .graphicsLayer {
                        val p = (scrollState.value / collapseThresholdPx).coerceIn(0f, 1f)
                        alpha = 1f - p * 0.92f
                        translationY = -p * 18f
                        val s = 1f - p * 0.05f
                        scaleX = s
                        scaleY = s
                    }
                    .padding(bottom = 22.dp),
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    if (showOrbitMark) {
                        HefimerOrbitLogo(
                            size = 14.dp,
                            tint = accentColor.copy(alpha = 0.72f),
                            spinning = true,
                            spinDurationMs = 28_000,
                        )
                    }
                    Text(
                        text = kicker.uppercase(),
                        fontFamily = UbuntuFontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 10.sp,
                        letterSpacing = 2.4.sp,
                        color = if (accentColor == HefimerColors.White) HefimerColors.White30 else accentColor.copy(alpha = 0.7f),
                    )
                }

                Spacer(Modifier.height(8.dp))

                val titleModifier = if (onTitleClick != null) {
                    Modifier.applePressable(pressedScale = 0.98f, onClick = onTitleClick)
                } else {
                    Modifier
                }

                Text(
                    text = title,
                    fontFamily = UbuntuFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 40.sp,
                    lineHeight = 44.sp,
                    letterSpacing = (-1.8).sp,
                    color = HefimerColors.White,
                    modifier = titleModifier,
                )

                if (subtitle.isNotBlank()) {
                    Spacer(Modifier.height(6.dp))
                    Text(
                        text = subtitle,
                        fontFamily = UbuntuFontFamily,
                        fontWeight = FontWeight.Normal,
                        fontSize = 13.5.sp,
                        lineHeight = 19.sp,
                        color = HefimerColors.White40,
                    )
                }
            }

            content()

            // Generous bottom clearance so last card scrolls comfortably above floating bottom dock
            Spacer(Modifier.height(navBottom + bottomDockClearance))
        }

        // Translucent pinned top chrome extending seamlessly behind transparent status bar
        Column(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth()
                .background(
                    Brush.verticalGradient(
                        0f to HefimerColors.Black.copy(alpha = 0.86f + 0.10f * compactAlpha),
                        0.72f to HefimerColors.Black.copy(alpha = 0.62f + 0.30f * compactAlpha),
                        1f to HefimerColors.Black.copy(alpha = 0.0f + 0.84f * compactAlpha),
                    ),
                ),
        ) {
            Spacer(Modifier.height(statusTop))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .padding(horizontal = 20.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.weight(1f),
                ) {
                    HefimerOrbitLogo(
                        size = 22.dp,
                        tint = accentColor,
                        spinning = false,
                        modifier = if (onTitleClick != null) {
                            Modifier.applePressable(pressedScale = 0.92f, onClick = onTitleClick)
                        } else {
                            Modifier
                        },
                    )
                    Text(
                        text = title,
                        fontFamily = UbuntuFontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 17.sp,
                        letterSpacing = (-0.4).sp,
                        color = HefimerColors.White,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.graphicsLayer {
                            alpha = compactAlpha
                            translationY = compactSlideY
                        },
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    content = headerActions,
                )
            }

            // Crisp hairline border fades in smoothly when collapsed
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(0.5.dp)
                    .graphicsLayer { alpha = compactAlpha }
                    .background(HefimerColors.White12),
            )
        }
    }
}
