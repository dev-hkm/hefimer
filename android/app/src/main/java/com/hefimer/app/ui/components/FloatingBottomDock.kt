package com.hefimer.app.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import com.adamglin.PhosphorIcons
import com.adamglin.phosphoricons.Regular
import com.adamglin.phosphoricons.regular.ChatsCircle
import com.adamglin.phosphoricons.regular.Devices
import com.adamglin.phosphoricons.regular.DownloadSimple
import com.adamglin.phosphoricons.regular.GridFour
import com.adamglin.phosphoricons.regular.UploadSimple
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hefimer.app.ui.theme.AppMotion
import com.hefimer.app.ui.theme.AppleGlassVariant
import com.hefimer.app.ui.theme.HefimerColors
import com.hefimer.app.ui.theme.HefimerHaptics
import com.hefimer.app.ui.theme.appleGlassSurface
import com.hefimer.app.ui.theme.rememberReduceMotion
import com.hefimer.app.ui.theme.tr

enum class MainTab(val label: String, val labelVi: String, val icon: ImageVector) {
    SEND("Send", "Gửi", PhosphorIcons.Regular.UploadSimple),
    RECEIVE("Receive", "Nhận", PhosphorIcons.Regular.DownloadSimple),
    SPACE("Space", "Space", PhosphorIcons.Regular.GridFour),
    ROOMS("Rooms", "Phòng", PhosphorIcons.Regular.ChatsCircle),
    LINK("Link", "Link", PhosphorIcons.Regular.Devices),
    ;

    fun localizedLabel(): String = tr(label, labelVi)
}

/**
 * Custom dynamic curved-notch Shape that carves a smooth Apple-liquid concave cradle
 * centered strictly at [notchCenterX] underneath the gliding active circular orb.
 */
private class NotchedLiquidDockShape(
    private val notchCenterX: Float,
    private val notchRadiusPx: Float,
    private val notchDepthPx: Float,
    private val cornerRadiusPx: Float,
) : Shape {
    override fun createOutline(
        size: Size,
        layoutDirection: LayoutDirection,
        density: Density,
    ): Outline {
        val w = size.width
        val h = size.height
        val r = cornerRadiusPx.coerceAtMost(minOf(w, h) * 0.5f)
        val nr = notchRadiusPx.coerceAtLeast(12f)
        val maxSpan = ((w - r * 2f) * 0.25f).coerceAtLeast(nr)
        val halfSpan = (nr * 1.16f).coerceAtMost(maxSpan)
        val midX = notchCenterX.coerceIn(r + halfSpan, w - r - halfSpan)
        val startX = midX - halfSpan
        val endX = midX + halfSpan

        if (endX - startX < nr) {
            return Outline.Rounded(
                RoundRect(
                    rect = Rect(0f, 0f, w, h),
                    cornerRadius = CornerRadius(r, r),
                ),
            )
        }

        val path = Path().apply {
            moveTo(r, 0f)
            lineTo(startX, 0f)
            // Left smooth S-curve down into concave cradle
            cubicTo(
                x1 = startX + halfSpan * 0.36f,
                y1 = 0f,
                x2 = midX - nr * 0.84f,
                y2 = notchDepthPx,
                x3 = midX,
                y3 = notchDepthPx,
            )
            // Right smooth S-curve back up to top bar edge
            cubicTo(
                x1 = midX + nr * 0.84f,
                y1 = notchDepthPx,
                x2 = endX - halfSpan * 0.36f,
                y2 = 0f,
                x3 = endX,
                y3 = 0f,
            )
            lineTo(w - r, 0f)
            arcTo(
                rect = Rect(w - r * 2f, 0f, w, r * 2f),
                startAngleDegrees = -90f,
                sweepAngleDegrees = 90f,
                forceMoveTo = false,
            )
            lineTo(w, h - r)
            arcTo(
                rect = Rect(w - r * 2f, h - r * 2f, w, h),
                startAngleDegrees = 0f,
                sweepAngleDegrees = 90f,
                forceMoveTo = false,
            )
            lineTo(r, h)
            arcTo(
                rect = Rect(0f, h - r * 2f, r * 2f, h),
                startAngleDegrees = 90f,
                sweepAngleDegrees = 90f,
                forceMoveTo = false,
            )
            lineTo(0f, r)
            arcTo(
                rect = Rect(0f, 0f, r * 2f, r * 2f),
                startAngleDegrees = 180f,
                sweepAngleDegrees = 90f,
                forceMoveTo = false,
            )
            close()
        }
        return Outline.Generic(path)
    }
}

/**
 * Curved-notch Apple Liquid Glass floating bottom navigation dock.
 * - Elevated Apple Glass bar with real-time liquid concave cradle centered on the active orb.
 * - Shows a crisp localized label ONLY beneath the active orb (zero clutter on inactive tabs).
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
    val reduceMotion = rememberReduceMotion()
    val tabs = MainTab.entries
    val selectedIndex = tabs.indexOf(selectedTab).coerceAtLeast(0)
    val activeAccent = HefimerColors.White

    Box(
        modifier = modifier
            .fillMaxWidth()
            .background(
                Brush.verticalGradient(
                    0.0f to Color.Transparent,
                    0.42f to HefimerColors.Black.copy(alpha = 0.45f),
                    0.78f to HefimerColors.Black.copy(alpha = 0.80f),
                    1.0f to HefimerColors.Black.copy(alpha = 0.94f),
                ),
            )
            .padding(top = 8.dp, bottom = navBottom + 10.dp, start = 16.dp, end = 16.dp),
        contentAlignment = Alignment.Center,
    ) {
        val barHeight = 58.dp
        val orbSize = 48.dp
        val orbRise = 18.dp
        val totalHeight = barHeight + orbRise
        val horizontalPad = 20.dp

        BoxWithConstraints(
            modifier = Modifier
                .widthIn(max = 330.dp)
                .fillMaxWidth()
                .height(totalHeight),
        ) {
            val density = LocalDensity.current
            val barWidthPx = with(density) { maxWidth.toPx() }.coerceAtLeast(1f)
            val horizPadPx = with(density) { horizontalPad.toPx() }
            val innerWidthPx = (barWidthPx - horizPadPx * 2f).coerceAtLeast(1f)
            val slotWidthPx = innerWidthPx / tabs.size
            val orbSizePx = with(density) { orbSize.toPx() }
            val orbRisePx = with(density) { orbRise.toPx() }
            val notchRadiusPx = with(density) { 25.dp.toPx() }
            val notchDepthPx = with(density) { 25.dp.toPx() }
            val cornerRadiusPx = with(density) { 18.dp.toPx() }

            var hoveredTabIdx by remember { mutableIntStateOf(-1) }
            val activeIndex = if (hoveredTabIdx in tabs.indices) hoveredTabIdx else selectedIndex
            val currentTabs by rememberUpdatedState(tabs)
            val currentOnSelectTab by rememberUpdatedState(onSelectTab)

            // Smooth horizontal glide for both the orb and the glass bar's concave notch
            val animatedIndex by animateFloatAsState(
                targetValue = activeIndex.toFloat(),
                animationSpec = if (reduceMotion) tween(140, easing = AppMotion.AppleEase) else AppMotion.layoutSpring(),
                label = "notched_dock_idx",
            )
            val orbCenterX = horizPadPx + slotWidthPx * (animatedIndex + 0.5f)

            // Soft liquid droplet bounce on tab change
            val orbBounceOffset = remember { Animatable(0f) }
            LaunchedEffect(selectedTab) {
                if (!reduceMotion) {
                    orbBounceOffset.snapTo(4.5f)
                    orbBounceOffset.animateTo(0f, AppMotion.softPopSpring())
                }
            }

            // Liquid squish-and-stretch on press/scrub
            val orbSquishX by animateFloatAsState(
                targetValue = if (hoveredTabIdx >= 0) 1.09f else 1f,
                animationSpec = if (hoveredTabIdx >= 0) AppMotion.pressDownSpring() else AppMotion.pressReleaseSpring(),
                label = "orb_squish_x",
            )
            val orbSquishY by animateFloatAsState(
                targetValue = if (hoveredTabIdx >= 0) 0.89f else 1f,
                animationSpec = if (hoveredTabIdx >= 0) AppMotion.pressDownSpring() else AppMotion.pressReleaseSpring(),
                label = "orb_squish_y",
            )
            val orbColor by animateColorAsState(
                targetValue = activeAccent,
                animationSpec = tween(AppMotion.Fast, easing = AppMotion.AppleSilk),
                label = "orb_color",
            )

            val dockShape = remember(orbCenterX, notchRadiusPx, notchDepthPx, cornerRadiusPx) {
                NotchedLiquidDockShape(
                    notchCenterX = orbCenterX,
                    notchRadiusPx = notchRadiusPx,
                    notchDepthPx = notchDepthPx,
                    cornerRadiusPx = cornerRadiusPx,
                )
            }

            // Curved-notch Elevated Apple Glass bar
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .height(barHeight)
                    .appleGlassSurface(
                        shape = dockShape,
                        variant = AppleGlassVariant.Elevated,
                        elevation = 20.dp,
                    ),
            )

            // Floating active circular orb centered above the concave cradle
            val activeTab = tabs[activeIndex]
            val orbPressDipPx = with(density) { (if (hoveredTabIdx >= 0) 2.5.dp else 0.dp).toPx() }
            val orbSettlePx = with(density) { orbBounceOffset.value.dp.toPx() }
            val orbIconScale by animateFloatAsState(
                targetValue = if (hoveredTabIdx >= 0) 0.90f else 1.04f,
                animationSpec = if (hoveredTabIdx >= 0) AppMotion.pressDownSpring() else AppMotion.softPopSpring(),
                label = "orb_icon_scale",
            )

            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(orbSize)
                    .graphicsLayer {
                        translationX = orbCenterX - orbSizePx * 0.5f
                        translationY = orbPressDipPx + orbSettlePx
                        scaleX = orbSquishX
                        scaleY = orbSquishY
                    }
                    .shadow(
                        elevation = 14.dp,
                        shape = CircleShape,
                        ambientColor = HefimerColors.Black.copy(alpha = 0.55f),
                        spotColor = HefimerColors.Black.copy(alpha = 0.55f),
                    )
                    .clip(CircleShape)
                    .background(orbColor)
                    .border(1.dp, HefimerColors.White20, CircleShape),
            ) {
                DockTabIconWithBadge(
                    tab = activeTab,
                    tint = HefimerColors.Black,
                    badgeBg = HefimerColors.Black,
                    badgeFg = HefimerColors.White,
                    dotColor = HefimerColors.Black,
                    linkBadgeCount = linkBadgeCount,
                    linkGroupActive = linkGroupActive,
                    modifier = Modifier.graphicsLayer {
                        scaleX = orbIconScale
                        scaleY = orbIconScale
                    },
                )
            }

            // Interactive touch/scrub strip covering the entire dock height
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = horizontalPad)
                    .pointerInput(tabs.size, slotWidthPx, horizPadPx) {
                        awaitEachGesture {
                            val down = awaitFirstDown(requireUnconsumed = false)
                            val opts = currentTabs
                            var curIdx = ((down.position.x - horizPadPx) / slotWidthPx)
                                .toInt()
                                .coerceIn(0, opts.lastIndex)
                            hoveredTabIdx = curIdx
                            while (true) {
                                val event = awaitPointerEvent()
                                val change = event.changes.firstOrNull { it.id == down.id } ?: break
                                if (!change.pressed) {
                                    val releaseIdx = ((change.position.x - horizPadPx) / slotWidthPx)
                                        .toInt()
                                        .coerceIn(0, opts.lastIndex)
                                    if (HefimerHaptics.enabled) {
                                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    }
                                    currentOnSelectTab(opts[releaseIdx])
                                    hoveredTabIdx = -1
                                    break
                                }
                                change.consume()
                                val nextIdx = ((change.position.x - horizPadPx) / slotWidthPx)
                                    .toInt()
                                    .coerceIn(0, opts.lastIndex)
                                if (nextIdx != curIdx) {
                                    curIdx = nextIdx
                                    hoveredTabIdx = nextIdx
                                    if (HefimerHaptics.enabled) {
                                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    }
                                    currentOnSelectTab(opts[nextIdx])
                                }
                            }
                        }
                    },
                verticalAlignment = Alignment.Bottom,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                tabs.forEachIndexed { idx, tab ->
                    val isSelected = idx == activeIndex
                    val isHovered = idx == hoveredTabIdx
                    val iconAlpha by animateFloatAsState(
                        targetValue = if (isSelected) 0f else 1f,
                        animationSpec = tween(AppMotion.Micro, easing = AppMotion.AppleSilk),
                        label = "dock_bar_alpha_$idx",
                    )
                    val iconYOffset by animateFloatAsState(
                        targetValue = if (isSelected) -orbRisePx * 0.38f else 0f,
                        animationSpec = AppMotion.snappySpring(),
                        label = "dock_bar_y_$idx",
                    )
                    val pressScale by animateFloatAsState(
                        targetValue = if (isHovered && !isSelected) 0.86f else 1f,
                        animationSpec = if (isHovered) AppMotion.pressDownSpring() else AppMotion.pressReleaseSpring(),
                        label = "dock_bar_press_$idx",
                    )

                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .weight(1f)
                            .height(barHeight),
                    ) {
                        // Inactive icon centered inside the bar
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier.graphicsLayer {
                                alpha = iconAlpha
                                translationY = iconYOffset
                                scaleX = pressScale
                                scaleY = pressScale
                            },
                        ) {
                            DockTabIconWithBadge(
                                tab = tab,
                                tint = HefimerColors.White50,
                                badgeBg = HefimerColors.White,
                                badgeFg = HefimerColors.Black,
                                dotColor = HefimerColors.White,
                                linkBadgeCount = linkBadgeCount,
                                linkGroupActive = linkGroupActive,
                            )
                        }

                        // Active-only short label nestled cleanly underneath the floating orb
                        androidx.compose.animation.AnimatedVisibility(
                            visible = isSelected,
                            enter = fadeIn(tween(180, easing = AppMotion.AppleSilk)) +
                                slideInVertically(AppMotion.snappySpring()) { it / 2 },
                            exit = fadeOut(tween(110, easing = AppMotion.AppleEase)) +
                                slideOutVertically(tween(110, easing = AppMotion.AppleEase)) { it / 2 },
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .padding(bottom = 6.dp),
                        ) {
                            Text(
                                text = tab.localizedLabel(),
                                color = HefimerColors.White,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DockTabIconWithBadge(
    tab: MainTab,
    tint: Color,
    badgeBg: Color,
    badgeFg: Color,
    dotColor: Color,
    linkBadgeCount: Int,
    linkGroupActive: Boolean,
    modifier: Modifier = Modifier,
) {
    Box(contentAlignment = Alignment.Center, modifier = modifier) {
        Icon(
            imageVector = tab.icon,
            contentDescription = tab.localizedLabel(),
            tint = tint,
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
                        .background(dotColor),
                )
            }
            AnimatedVisibility(
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
                        .background(badgeBg),
                ) {
                    Text(
                        text = if (linkBadgeCount > 9) "9+" else linkBadgeCount.toString(),
                        color = badgeFg,
                        fontSize = 8.sp,
                        fontWeight = FontWeight.Bold,
                    )
                }
            }
        }
    }
}
