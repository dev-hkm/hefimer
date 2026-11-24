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
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import com.hefimer.app.ui.theme.UbuntuFontFamily

enum class MainTab(val label: String, val icon: ImageVector) {
    SEND("Send", Icons.Rounded.Upload),
    RECEIVE("Receive", Icons.Rounded.Download),
    SPACE("Space", Icons.Rounded.GridView),
    ROOMS("Rooms", Icons.Rounded.Forum),
    LINK("Link", Icons.Rounded.Devices),
}

/**
 * Floating translucent monochrome bottom navigation dock harmonized seamlessly
 * with the transparent Android system navigation bar (edge-to-edge, zero hard cut).
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
                    0.45f to HefimerColors.Black.copy(alpha = 0.72f),
                    1f to HefimerColors.Black.copy(alpha = 0.94f),
                ),
            )
            .padding(top = 18.dp, bottom = navBottom + 10.dp, start = 16.dp, end = 16.dp),
        contentAlignment = Alignment.Center,
    ) {
        val outerRadius = 28.dp
        val innerPadding = 5.dp
        val innerRadius = outerRadius - innerPadding // Concentric: 23.dp

        BoxWithConstraints(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(outerRadius))
                .background(Color(0xE60B0B0B))
                .border(1.dp, HefimerColors.White12, RoundedCornerShape(outerRadius))
                .padding(innerPadding),
        ) {
            val itemWidth = maxWidth / tabs.size
            val pillOffsetX by animateDpAsState(
                targetValue = itemWidth * selectedIndex,
                animationSpec = AppMotion.layoutSpring(),
                label = "dock_pill_x",
            )

            // Gliding concentric active capsule
            Box(
                modifier = Modifier
                    .offset(x = pillOffsetX)
                    .width(itemWidth)
                    .height(54.dp)
                    .clip(RoundedCornerShape(innerRadius))
                    .background(activeAccent),
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                tabs.forEach { tab ->
                    val isSelected = tab == selectedTab
                    val interactionSource = remember { MutableInteractionSource() }
                    val isPressed by interactionSource.collectIsPressedAsState()
                    val pressScale by animateFloatAsState(
                        targetValue = if (isPressed) 0.92f else 1f,
                        animationSpec = if (isPressed) AppMotion.pressDownSpring() else AppMotion.pressReleaseSpring(),
                        label = "dock_item_press",
                    )
                    val contentColor by animateColorAsState(
                        targetValue = if (isSelected) HefimerColors.Black else HefimerColors.White50,
                        animationSpec = tween(AppMotion.Micro, easing = AppMotion.AppleSilk),
                        label = "dock_item_color",
                    )

                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .weight(1f)
                            .height(54.dp)
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
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center,
                        ) {
                            Box {
                                Icon(
                                    imageVector = tab.icon,
                                    contentDescription = tab.label,
                                    tint = contentColor,
                                    modifier = Modifier.size(20.dp),
                                )
                                if (tab == MainTab.LINK) {
                                    // Online paired indicator dot
                                    if (linkGroupActive && linkBadgeCount == 0) {
                                        Box(
                                            modifier = Modifier
                                                .align(Alignment.TopEnd)
                                                .offset(x = 4.dp, y = (-2).dp)
                                                .size(6.dp)
                                                .clip(CircleShape)
                                                .background(if (isSelected) HefimerColors.Black else HefimerColors.Emerald400),
                                        )
                                    }
                                    // Incoming transfers badge
                                    androidx.compose.animation.AnimatedVisibility(
                                        visible = linkBadgeCount > 0,
                                        enter = scaleIn(AppMotion.softPopSpring(), initialScale = 0.7f) + fadeIn(tween(150)),
                                        exit = scaleOut(tween(140), targetScale = 0.7f) + fadeOut(tween(120)),
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
                            Spacer(Modifier.height(2.dp))
                            Text(
                                text = tab.label,
                                color = contentColor,
                                fontFamily = UbuntuFontFamily,
                                fontWeight = FontWeight.Bold,
                                fontSize = 10.sp,
                                letterSpacing = 0.2.sp,
                                maxLines = 1,
                            )
                        }
                    }
                }
            }
        }
    }
}
