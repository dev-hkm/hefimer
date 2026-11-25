package com.hefimer.app.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Devices
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.Forum
import androidx.compose.material.icons.rounded.GridView
import androidx.compose.material.icons.rounded.Upload
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hefimer.app.ui.theme.AppMotion
import com.hefimer.app.ui.theme.HefimerColors

enum class MainTab(val label: String, val icon: ImageVector) {
    SEND("Send", Icons.Rounded.Upload),
    RECEIVE("Receive", Icons.Rounded.Download),
    SPACE("Space", Icons.Rounded.GridView),
    ROOMS("Rooms", Icons.Rounded.Forum),
    LINK("Link", Icons.Rounded.Devices),
}

/**
 * Compact floating round pill bottom navigation dock harmonized seamlessly
 * with the transparent Android navigation bar.
 * Features a sleek concentric pill capsule inside that glides fluidly between tabs
 * driven by AppMotion.layoutSpring() with tactile press compression and zero Material ripples.
 */
@Composable
fun HefimerFloatingBottomDock(
    selectedTab: MainTab,
    onSelectTab: (MainTab) -> Unit,
    linkBadgeCount: Int,
    linkGroupActive: Boolean,
    isR2Mode: Boolean,
    modifier: Modifier = Modifier,
) {
    val navBottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
    val haptic = LocalHapticFeedback.current
    val tabs = MainTab.entries
    val selectedIndex = tabs.indexOf(selectedTab).coerceAtLeast(0)
    val activeAccent = if (isR2Mode && selectedTab == MainTab.SEND) HefimerColors.Emerald400 else HefimerColors.White

    Box(
        modifier = modifier
            .fillMaxWidth()
            .background(
                Brush.verticalGradient(
                    0f to Color.Transparent,
                    0.5f to HefimerColors.Black.copy(alpha = 0.65f),
                    1f to HefimerColors.Black.copy(alpha = 0.92f),
                ),
            )
            .padding(top = 14.dp, bottom = navBottom + 10.dp, start = 24.dp, end = 24.dp),
        contentAlignment = Alignment.Center,
    ) {
        val pillHeight = 48.dp
        val innerPadding = 3.5.dp
        val thumbHeight = pillHeight - innerPadding * 2

        BoxWithConstraints(
            modifier = Modifier
                .width(244.dp)
                .height(pillHeight)
                .clip(CircleShape)
                .background(HefimerColors.DockBg)
                .border(1.dp, HefimerColors.White15, CircleShape)
                .padding(innerPadding),
        ) {
            val density = androidx.compose.ui.platform.LocalDensity.current
            val slotWidth = maxWidth / tabs.size
            val slotWidthPx = with(density) { slotWidth.toPx() }
            val thumbOffsetPx by animateFloatAsState(
                targetValue = slotWidthPx * selectedIndex,
                animationSpec = AppMotion.layoutSpring(),
                label = "pill_thumb_x",
            )
            val thumbColor by animateColorAsState(
                targetValue = activeAccent,
                animationSpec = tween(AppMotion.Fast, easing = AppMotion.AppleSilk),
                label = "pill_thumb_color",
            )

            // Gliding round pill capsule inside the outer pill (GPU-only translationX)
            Box(
                modifier = Modifier
                    .graphicsLayer { translationX = thumbOffsetPx }
                    .width(slotWidth)
                    .height(thumbHeight)
                    .clip(CircleShape)
                    .background(thumbColor),
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                tabs.forEach { tab ->
                    val isSelected = tab == selectedTab
                    val interactionSource = remember { MutableInteractionSource() }
                    val isPressed by interactionSource.collectIsPressedAsState()
                    val pressScale by animateFloatAsState(
                        targetValue = if (isPressed) 0.88f else 1f,
                        animationSpec = if (isPressed) AppMotion.pressDownSpring() else AppMotion.pressReleaseSpring(),
                        label = "dock_press",
                    )
                    val iconTint by animateColorAsState(
                        targetValue = if (isSelected) HefimerColors.Black else HefimerColors.White50,
                        animationSpec = tween(AppMotion.Micro, easing = AppMotion.AppleSilk),
                        label = "dock_tint",
                    )

                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .weight(1f)
                            .height(thumbHeight)
                            .graphicsLayer {
                                scaleX = pressScale
                                scaleY = pressScale
                            }
                            .clickable(
                                interactionSource = interactionSource,
                                indication = null,
                            ) {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                onSelectTab(tab)
                            },
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = tab.icon,
                                contentDescription = tab.label,
                                tint = iconTint,
                                modifier = Modifier.size(21.dp),
                            )
                            if (tab == MainTab.LINK) {
                                if (linkGroupActive && linkBadgeCount == 0) {
                                    Box(
                                        modifier = Modifier
                                            .align(Alignment.TopEnd)
                                            .offset(x = 4.dp, y = (-3).dp)
                                            .size(6.dp)
                                            .clip(CircleShape)
                                            .background(if (isSelected) HefimerColors.Black else HefimerColors.Emerald400),
                                    )
                                }
                                androidx.compose.animation.AnimatedVisibility(
                                    visible = linkBadgeCount > 0,
                                    enter = scaleIn(AppMotion.softPopSpring(), initialScale = 0.7f) +
                                        fadeIn(tween(150, easing = AppMotion.AppleSilk)),
                                    exit = scaleOut(tween(140, easing = AppMotion.AppleEase), targetScale = 0.7f) +
                                        fadeOut(tween(120, easing = AppMotion.AppleEase)),
                                    modifier = Modifier
                                        .align(Alignment.TopEnd)
                                        .offset(x = 8.dp, y = (-5).dp),
                                ) {
                                    Box(
                                        contentAlignment = Alignment.Center,
                                        modifier = Modifier
                                            .size(16.dp)
                                            .clip(CircleShape)
                                            .background(if (isSelected) HefimerColors.Black else HefimerColors.Emerald400),
                                    ) {
                                        Text(
                                            text = if (linkBadgeCount > 9) "9+" else linkBadgeCount.toString(),
                                            color = if (isSelected) HefimerColors.White else HefimerColors.Black,
                                            fontSize = 8.sp,
                                            fontWeight = FontWeight.Bold,
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
