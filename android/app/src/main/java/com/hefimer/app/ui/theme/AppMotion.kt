package com.hefimer.app.ui.theme

import android.provider.Settings
import androidx.activity.BackEventCompat
import androidx.activity.compose.PredictiveBackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.util.VelocityTracker
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.abs

object AppMotion {
    // Canonical Apple cubic-bezier deceleration curves
    val AppleSheet = CubicBezierEasing(0.32f, 0.72f, 0f, 1f)
    val AppleSilk = CubicBezierEasing(0.28f, 0.11f, 0.14f, 1f)
    val AppleEaseOut = CubicBezierEasing(0.23f, 1f, 0.32f, 1f)
    val AppleEase = CubicBezierEasing(0.25f, 0.1f, 0.25f, 1f)

    // Calibrated duration tokens (ms)
    const val Screen = 460
    const val ScreenPop = 420
    const val Tab = 360
    const val Sheet = 440
    const val Content = 380
    const val Fast = 240
    const val Micro = 200

    // Perceptual springs converted from Apple (response, dampingRatio)
    fun <T> navigationSpring() = spring<T>(dampingRatio = 0.90f, stiffness = 240f)
    fun <T> fluidSpring() = spring<T>(dampingRatio = 0.88f, stiffness = 260f)
    fun <T> layoutSpring() = spring<T>(dampingRatio = 0.88f, stiffness = 270f)
    fun <T> gentleSpring() = spring<T>(dampingRatio = 0.92f, stiffness = 210f)
    fun <T> snappySpring() = spring<T>(dampingRatio = 0.80f, stiffness = 360f)
    fun <T> pressDownSpring() = spring<T>(dampingRatio = 0.86f, stiffness = 420f)
    fun <T> pressReleaseSpring() = spring<T>(dampingRatio = 0.68f, stiffness = 300f)
    fun <T> softPopSpring() = spring<T>(dampingRatio = 0.72f, stiffness = 380f)
    fun <T> bouncySpring() = spring<T>(dampingRatio = 0.68f, stiffness = 320f)

    fun floatLayoutSpring() = spring<Float>(dampingRatio = 0.88f, stiffness = 270f)
    fun intOffsetFluidSpring() = spring<IntOffset>(dampingRatio = 0.88f, stiffness = 260f)
    fun intSizeLayoutSpring() = spring<IntSize>(dampingRatio = 0.88f, stiffness = 270f)

    // Peer tab switch: subtle cross-dissolve + quiet vertical breath
    fun tabTransform(reduceMotion: Boolean = false): ContentTransform {
        if (reduceMotion) {
            return fadeIn(tween(150, easing = AppleEase)) togetherWith fadeOut(tween(120, easing = AppleEase))
        }
        val enter = fadeIn(tween(Tab, easing = AppleSilk)) +
            slideInVertically(tween(Tab, easing = AppleSheet)) { (it * 0.025f).toInt() }
        val exit = fadeOut(tween(Micro, easing = AppleEase))
        return enter togetherWith exit
    }

    // In-place sibling section switch (File/Text, Devices/Activity, Chat/Board)
    fun sectionTransform(reduceMotion: Boolean = false): ContentTransform {
        if (reduceMotion) {
            return fadeIn(tween(140, easing = AppleEase)) togetherWith fadeOut(tween(120, easing = AppleEase))
        }
        val enter = fadeIn(tween(Content, easing = AppleSilk)) +
            scaleIn(
                animationSpec = tween(Content, easing = AppleSheet),
                initialScale = 0.975f,
            )
        val exit = fadeOut(tween(Fast, easing = AppleEase))
        return enter togetherWith exit
    }

    fun softCrossDissolve(): ContentTransform =
        fadeIn(tween(Content, easing = AppleSilk)) togetherWith
            fadeOut(tween(Fast, easing = AppleEase))

    val softExpandEnter: EnterTransition =
        expandVertically(animationSpec = tween(320, easing = AppleSheet)) +
            fadeIn(tween(300, easing = AppleSilk))

    val softExpandExit: ExitTransition =
        shrinkVertically(animationSpec = tween(240, easing = AppleSilk)) +
            fadeOut(tween(180, easing = AppleEase))

    val floatingBarEnter: EnterTransition =
        slideInVertically(tween(Sheet, easing = AppleSheet)) { it / 2 } +
            fadeIn(tween(320, easing = AppleSilk)) +
            scaleIn(tween(Sheet, easing = AppleSheet), initialScale = 0.94f)

    val floatingBarExit: ExitTransition =
        slideOutVertically(tween(300, easing = AppleSilk)) { it / 2 } +
            fadeOut(tween(220, easing = AppleEase)) +
            scaleOut(tween(300, easing = AppleSilk), targetScale = 0.95f)
}

fun projectOffset(velocityPxPerSec: Float, decelerationRate: Float = 0.998f): Float =
    (velocityPxPerSec / 1000f) * decelerationRate / (1f - decelerationRate)

fun rubberBand(overshoot: Float, dimension: Float, constant: Float = 0.55f): Float {
    if (dimension <= 0f) return 0f
    return (overshoot * dimension * constant) / (dimension + constant * abs(overshoot))
}

@Composable
fun rememberReduceMotion(): Boolean {
    val context = LocalContext.current
    return remember(context) {
        val scale = try {
            Settings.Global.getFloat(
                context.contentResolver,
                Settings.Global.ANIMATOR_DURATION_SCALE,
                1.0f,
            )
        } catch (_: Exception) {
            1.0f
        }
        scale == 0f
    }
}

/**
 * Tactile Apple press feedback: immediate ACTION_DOWN compression, zero Material ripple.
 */
fun Modifier.applePressable(
    enabled: Boolean = true,
    pressedScale: Float = 0.965f,
    enableHaptic: Boolean = true,
    onClick: () -> Unit,
): Modifier = composed {
    val haptic = LocalHapticFeedback.current
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val reduceMotion = rememberReduceMotion()

    val scale by animateFloatAsState(
        targetValue = if (isPressed && enabled && !reduceMotion) pressedScale else 1f,
        animationSpec = if (isPressed) AppMotion.pressDownSpring() else AppMotion.pressReleaseSpring(),
        label = "apple_press_scale",
    )

    this
        .graphicsLayer {
            scaleX = scale
            scaleY = scale
        }
        .clickable(
            enabled = enabled,
            interactionSource = interactionSource,
            indication = null,
            onClick = {
                if (enableHaptic) {
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                }
                onClick()
            },
        )
}

/**
 * Staggered wave entrance capped at 5 items, GPU-only in graphicsLayer.
 */
fun Modifier.appleEntrance(
    index: Int = 0,
    stepDelayMs: Int = 42,
    translationYFromPx: Float = 24f,
): Modifier = composed {
    val reduceMotion = rememberReduceMotion()
    if (reduceMotion) return@composed this

    var visible by remember { mutableStateOf(false) }
    val clampedDelay = index.coerceIn(0, 5) * stepDelayMs

    LaunchedEffect(Unit) {
        if (clampedDelay > 0) delay(clampedDelay.toLong())
        visible = true
    }

    val alpha by animateFloatAsState(
        targetValue = if (visible) 1f else 0f,
        animationSpec = tween(AppMotion.Content, easing = AppMotion.AppleSilk),
        label = "entrance_alpha",
    )
    val offsetY by animateFloatAsState(
        targetValue = if (visible) 0f else translationYFromPx,
        animationSpec = tween(AppMotion.Content, easing = AppMotion.AppleSheet),
        label = "entrance_ty",
    )
    val scale by animateFloatAsState(
        targetValue = if (visible) 1f else 0.975f,
        animationSpec = tween(AppMotion.Content, easing = AppMotion.AppleSheet),
        label = "entrance_scale",
    )

    graphicsLayer {
        this.alpha = alpha
        this.translationY = offsetY
        this.scaleX = scale
        this.scaleY = scale
    }
}

@Stable
class HeroBoundsRegistry {
    private val rects = mutableStateMapOf<String, Rect>()
    fun record(key: String, rect: Rect) {
        rects[key] = rect
    }

    operator fun get(key: String?): Rect? = if (key == null) null else rects[key]
}

val LocalHeroBounds = staticCompositionLocalOf<HeroBoundsRegistry> {
    HeroBoundsRegistry()
}

fun Modifier.heroSource(key: String, registry: HeroBoundsRegistry): Modifier =
    this.onGloballyPositioned { coords ->
        if (coords.isAttached) {
            registry.record(key, coords.boundsInRoot())
        }
    }

/**
 * Zero-remount two-tier navigation host:
 * - Layer 0 preserves all root tabs via SaveableStateProvider with synchronized Z-depth parallax.
 * - Layer 1 renders full-screen overlays with Hero Bounds Morph + PredictiveBackHandler edge peel.
 */
@Composable
fun <TabT : Any, OverlayT : Any> AppleLayeredNavigationHost(
    currentTab: TabT,
    overlay: OverlayT?,
    heroKey: String?,
    onDismissOverlay: () -> Unit,
    bottomChrome: @Composable (overlayReveal: Float) -> Unit,
    renderTab: @Composable (TabT) -> Unit,
    renderOverlay: @Composable (OverlayT) -> Unit,
) {
    val heroBounds = LocalHeroBounds.current
    val density = LocalDensity.current
    val saveableHolder = rememberSaveableStateHolder()
    val haptic = LocalHapticFeedback.current
    val reduceMotion = rememberReduceMotion()

    val activeHeroRect = remember(overlay, heroKey) {
        if (overlay != null) heroBounds[heroKey] else null
    }

    val dismissProgress = remember { Animatable(0f) }
    var swipeEdge by remember { mutableIntStateOf(BackEventCompat.EDGE_LEFT) }
    var startTouchY by remember { mutableFloatStateOf(0f) }
    var currentTouchY by remember { mutableFloatStateOf(0f) }
    var isPeeling by remember { mutableStateOf(false) }

    LaunchedEffect(overlay) {
        if (overlay != null) {
            dismissProgress.snapTo(1f)
            dismissProgress.animateTo(
                targetValue = 0f,
                animationSpec = if (reduceMotion) tween(160, easing = AppMotion.AppleEase) else AppMotion.navigationSpring(),
            )
        }
    }

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(HefimerColors.Black),
    ) {
        val rootW = with(density) { maxWidth.toPx() }.coerceAtLeast(1f)
        val rootH = with(density) { maxHeight.toPx() }.coerceAtLeast(1f)

        val overlayVisible = overlay != null
        val reveal = if (overlayVisible) (1f - dismissProgress.value).coerceIn(0f, 1f) else 0f

        // LAYER 0: Zero-remount base tabs + synchronized depth parallax & optical scrim
        Box(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    val depthScale = if (reduceMotion) 1f else 1f - 0.055f * reveal
                    scaleX = depthScale
                    scaleY = depthScale
                    alpha = 1f - 0.12f * reveal
                },
        ) {
            AnimatedContent(
                targetState = currentTab,
                transitionSpec = { AppMotion.tabTransform(reduceMotion) },
                label = "root_tabs",
            ) { tab ->
                saveableHolder.SaveableStateProvider(tab.toString()) {
                    renderTab(tab)
                }
            }

            bottomChrome(reveal)

            if (reveal > 0.001f) {
                Box(
                    Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.24f * reveal)),
                )
            }
        }

        // LAYER 1: Full-screen detail/workspace overlay with Hero Morph + Predictive Back Peel
        if (overlay != null) {
            PredictiveBackHandler(enabled = true) { events ->
                var committed = false
                isPeeling = true
                var capturedStart = false
                try {
                    events.collect { event ->
                        val raw = event.progress.coerceIn(0f, 1f)
                        val curved = raw * 0.70f + (1f - (1f - raw) * (1f - raw)) * 0.30f
                        swipeEdge = event.swipeEdge
                        if (!capturedStart) {
                            startTouchY = event.touchY
                            capturedStart = true
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        }
                        currentTouchY = event.touchY
                        dismissProgress.snapTo(curved)
                    }
                    committed = true
                } catch (_: CancellationException) {
                    committed = false
                } finally {
                    isPeeling = false
                }

                if (committed) {
                    val spec: AnimationSpec<Float> = when {
                        reduceMotion -> tween(140, easing = AppMotion.AppleEase)
                        activeHeroRect != null -> AppMotion.navigationSpring()
                        else -> tween(260, easing = AppMotion.AppleSheet)
                    }
                    dismissProgress.animateTo(1f, spec)
                    onDismissOverlay()
                } else {
                    dismissProgress.animateTo(0f, AppMotion.navigationSpring())
                }
            }

            val pReveal = (1f - dismissProgress.value).coerceIn(0f, 1f)
            val cornerDp = if (reduceMotion) {
                0.dp
            } else {
                when {
                    activeHeroRect != null ->
                        ((1f - pReveal) * 26f + if (isPeeling) dismissProgress.value * 30f else 0f)
                            .coerceIn(0f, 30f).dp
                    isPeeling || dismissProgress.value > 0.001f -> (dismissProgress.value * 30f).dp
                    else -> 0.dp
                }
            }

            Surface(
                color = HefimerColors.Black,
                shape = RoundedCornerShape(cornerDp),
                shadowElevation = if (dismissProgress.value > 0.01f) 16.dp else 0.dp,
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        if (reduceMotion) {
                            alpha = pReveal
                            return@graphicsLayer
                        }
                        if (activeHeroRect != null) {
                            val minScale = (activeHeroRect.width / rootW).coerceIn(0.22f, 0.88f)
                            val s = minScale + (1f - minScale) * pReveal
                            val peelBonusScale = if (isPeeling) 1f - 0.06f * dismissProgress.value else 1f
                            scaleX = s * peelBonusScale
                            scaleY = s * peelBonusScale
                            translationX = (activeHeroRect.center.x - rootW / 2f) * (1f - pReveal)
                            translationY = (activeHeroRect.center.y - rootH / 2f) * (1f - pReveal)
                            alpha = (pReveal / 0.18f).coerceIn(0f, 1f)
                        } else {
                            val d = dismissProgress.value
                            val s = 1f - 0.14f * d
                            val edgeDir = if (swipeEdge == BackEventCompat.EDGE_LEFT) 1f else -1f
                            val deltaY = ((currentTouchY - startTouchY) * 0.08f)
                                .coerceIn(-16.dp.toPx(), 16.dp.toPx())
                            scaleX = s
                            scaleY = s
                            translationX = edgeDir * 28.dp.toPx() * d
                            translationY = deltaY * d
                            rotationZ = edgeDir * (deltaY / 16.dp.toPx()) * 0.9f * d
                            alpha = (1f - (d - 0.72f).coerceAtLeast(0f) / 0.28f).coerceIn(0f, 1f)
                        }
                    },
            ) {
                renderOverlay(overlay)
            }
        }
    }
}

/**
 * Swipe-to-dismiss sheet with 1:1 tracking, 0.55 rubber-banding, momentum projection,
 * and release velocity handoff straight into spring physics.
 */
@Composable
fun AppleSwipeableSheet(
    visible: Boolean,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    if (!visible) return
    val reduceMotion = rememberReduceMotion()
    val scope = rememberCoroutineScope()
    val offsetY = remember { Animatable(900f) }
    val velocityTracker = remember { VelocityTracker() }

    LaunchedEffect(Unit) {
        offsetY.animateTo(
            targetValue = 0f,
            animationSpec = if (reduceMotion) tween(160, easing = AppMotion.AppleEase) else AppMotion.navigationSpring(),
        )
    }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val heightPx = constraints.maxHeight.toFloat().coerceAtLeast(1f)
        val progress = (1f - (offsetY.value / (heightPx * 0.65f))).coerceIn(0f, 1f)

        // Backdrop scrim derived from single progress scalar
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.72f * progress))
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                ) {
                    scope.launch {
                        offsetY.animateTo(
                            heightPx,
                            tween(260, easing = AppMotion.AppleSheet),
                        )
                        onDismiss()
                    }
                },
        )

        Box(
            modifier = modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .graphicsLayer {
                    translationY = offsetY.value
                }
                .pointerInput(heightPx) {
                    detectVerticalDragGestures(
                        onDragStart = { velocityTracker.resetTracking() },
                        onVerticalDrag = { change, dragAmount ->
                            velocityTracker.addPosition(change.uptimeMillis, change.position)
                            change.consume()
                            val candidate = offsetY.value + dragAmount
                            val mapped = if (candidate < 0f) {
                                -rubberBand(-candidate, heightPx * 0.4f, 0.55f)
                            } else {
                                candidate
                            }
                            scope.launch { offsetY.snapTo(mapped) }
                        },
                        onDragEnd = {
                            val vy = velocityTracker.calculateVelocity().y
                            val projected = offsetY.value + projectOffset(vy, decelerationRate = 0.99f)
                            val shouldDismiss = projected > heightPx * 0.25f || vy > 950f
                            scope.launch {
                                if (shouldDismiss) {
                                    offsetY.animateTo(
                                        targetValue = heightPx,
                                        initialVelocity = vy,
                                        animationSpec = spring(dampingRatio = 0.85f, stiffness = 300f),
                                    )
                                    onDismiss()
                                } else {
                                    offsetY.animateTo(
                                        targetValue = 0f,
                                        initialVelocity = vy,
                                        animationSpec = spring(dampingRatio = 0.82f, stiffness = 360f),
                                    )
                                }
                            }
                        },
                        onDragCancel = {
                            scope.launch { offsetY.animateTo(0f, AppMotion.navigationSpring()) }
                        },
                    )
                },
        ) {
            content()
        }
    }
}

/**
 * Concentric sliding segmented control (R_inner = R_outer - padding) driven by layoutSpring().
 */
@Composable
fun <T> AppleSegmentedControl(
    options: List<T>,
    selected: T,
    label: (T) -> String,
    onSelect: (T) -> Unit,
    modifier: Modifier = Modifier,
    height: Dp = 44.dp,
    outerRadius: Dp = 22.dp,
    padding: Dp = 4.dp,
    accentColor: Color = HefimerColors.White,
    selectedTextColor: Color = HefimerColors.Black,
) {
    val selectedIndex = options.indexOf(selected).coerceAtLeast(0)
    val haptic = LocalHapticFeedback.current
    val innerRadius = (outerRadius - padding).coerceAtLeast(8.dp)

    BoxWithConstraints(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(outerRadius))
            .background(HefimerColors.White04)
            .border(1.dp, HefimerColors.White10, RoundedCornerShape(outerRadius))
            .padding(padding),
    ) {
        val pillWidth = maxWidth / options.size.coerceAtLeast(1)
        val pillOffsetX by animateDpAsState(
            targetValue = pillWidth * selectedIndex,
            animationSpec = AppMotion.layoutSpring(),
            label = "segmented_pill_x",
        )

        Box(
            modifier = Modifier
                .offset(x = pillOffsetX)
                .width(pillWidth)
                .height(height - padding * 2)
                .clip(RoundedCornerShape(innerRadius))
                .background(accentColor),
        )

        Row(Modifier.fillMaxWidth()) {
            options.forEach { item ->
                val isSelected = item == selected
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .weight(1f)
                        .height(height - padding * 2)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                        ) {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            onSelect(item)
                        },
                ) {
                    Text(
                        text = label(item),
                        color = if (isSelected) selectedTextColor else HefimerColors.White50,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
    }
}

/**
 * Rolling odometer numeric display with tabular figures (tnum).
 */
@Composable
fun AppleOdometerNumber(
    value: Int,
    suffix: String = "",
    style: TextStyle = MaterialTheme.typography.titleMedium,
    color: Color = HefimerColors.White,
) {
    var previous by remember { mutableIntStateOf(value) }
    val dir = if (value >= previous) 1 else -1
    SideEffect { previous = value }
    val reduceMotion = rememberReduceMotion()

    AnimatedContent(
        targetState = value,
        transitionSpec = {
            if (reduceMotion) {
                fadeIn(tween(120, easing = AppMotion.AppleEase)) togetherWith
                    fadeOut(tween(100, easing = AppMotion.AppleEase))
            } else {
                (slideInVertically(tween(240, easing = AppMotion.AppleSheet)) { it * dir } +
                    fadeIn(tween(180, easing = AppMotion.AppleSilk)))
                    .togetherWith(
                        slideOutVertically(tween(200, easing = AppMotion.AppleSilk)) { -it * dir } +
                            fadeOut(tween(140, easing = AppMotion.AppleEase)),
                    )
                    .using(SizeTransform(clip = false))
            }
        },
        label = "odometer_number",
    ) { count ->
        Text(
            text = "$count$suffix",
            color = color,
            style = style.copy(fontFeatureSettings = "tnum"),
        )
    }
}
