package com.hefimer.app.ui.theme

import android.provider.Settings
import androidx.activity.BackEventCompat
import androidx.activity.compose.BackHandler
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
import androidx.compose.animation.core.animateFloat
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

    // Unhurried, silky Apple-calibrated duration tokens (ms)
    const val Screen = 640
    const val ScreenPop = 580
    const val Tab = 540
    const val Sheet = 600
    const val Content = 560
    const val Fast = 360
    const val Micro = 280

    // Perceptual springs tuned for calm, silky, unhurried Apple glide (zero wobble on taps)
    fun <T> navigationSpring() = spring<T>(dampingRatio = 0.94f, stiffness = 140f)
    fun <T> fluidSpring() = spring<T>(dampingRatio = 0.92f, stiffness = 155f)
    fun <T> layoutSpring() = spring<T>(dampingRatio = 0.92f, stiffness = 165f)
    fun <T> gentleSpring() = spring<T>(dampingRatio = 0.95f, stiffness = 115f)
    fun <T> snappySpring() = spring<T>(dampingRatio = 0.88f, stiffness = 210f)
    fun <T> pressDownSpring() = spring<T>(dampingRatio = 0.92f, stiffness = 310f)
    fun <T> pressReleaseSpring() = spring<T>(dampingRatio = 0.82f, stiffness = 180f)
    fun <T> softPopSpring() = spring<T>(dampingRatio = 0.86f, stiffness = 185f)
    fun <T> bouncySpring() = spring<T>(dampingRatio = 0.80f, stiffness = 190f)

    fun floatLayoutSpring() = spring<Float>(dampingRatio = 0.92f, stiffness = 165f)
    fun intOffsetFluidSpring() = spring<IntOffset>(dampingRatio = 0.92f, stiffness = 155f)
    fun intSizeLayoutSpring() = spring<IntSize>(dampingRatio = 0.92f, stiffness = 165f)

    // Peer tab switch: unhurried silk cross-dissolve + subtle vertical breath
    fun tabTransform(reduceMotion: Boolean = false): ContentTransform {
        if (reduceMotion) {
            return fadeIn(tween(150, easing = AppleEase)) togetherWith fadeOut(tween(120, easing = AppleEase))
        }
        val enter = fadeIn(tween(Tab, easing = AppleSilk)) +
            slideInVertically(tween(Tab, easing = AppleSheet)) { (it * 0.025f).toInt() }
        val exit = fadeOut(tween(Fast, easing = AppleEase))
        return enter togetherWith exit
    }

    // In-place sibling section switch (File/Text, Form/Result, Devices/Activity, Chat/Board)
    fun sectionTransform(reduceMotion: Boolean = false): ContentTransform {
        if (reduceMotion) {
            return fadeIn(tween(140, easing = AppleEase)) togetherWith fadeOut(tween(120, easing = AppleEase))
        }
        val enter = fadeIn(tween(Content, easing = AppleSilk)) +
            slideInVertically(tween(Content, easing = AppleSheet)) { (it * 0.028f).toInt() } +
            scaleIn(
                animationSpec = tween(Content, easing = AppleSheet),
                initialScale = 0.955f,
            )
        val exit = fadeOut(tween(Fast, easing = AppleEase)) +
            scaleOut(
                animationSpec = tween(Fast, easing = AppleSilk),
                targetScale = 0.975f,
            )
        return enter togetherWith exit
    }

    fun softCrossDissolve(): ContentTransform =
        fadeIn(tween(Content, easing = AppleSilk)) togetherWith
            fadeOut(tween(Fast, easing = AppleEase))

    val softExpandEnter: EnterTransition =
        expandVertically(animationSpec = tween(480, easing = AppleSheet)) +
            fadeIn(tween(440, easing = AppleSilk))

    val softExpandExit: ExitTransition =
        shrinkVertically(animationSpec = tween(360, easing = AppleSilk)) +
            fadeOut(tween(280, easing = AppleEase))

    val floatingBarEnter: EnterTransition =
        slideInVertically(tween(Sheet, easing = AppleSheet)) { it / 2 } +
            fadeIn(tween(440, easing = AppleSilk)) +
            scaleIn(tween(Sheet, easing = AppleSheet), initialScale = 0.94f)

    val floatingBarExit: ExitTransition =
        slideOutVertically(tween(420, easing = AppleSilk)) { it / 2 } +
            fadeOut(tween(300, easing = AppleEase)) +
            scaleOut(tween(420, easing = AppleSilk), targetScale = 0.95f)
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
 * - Layer 0 preserves all root tabs via SaveableStateProvider with synchronized Z-depth parallax
 *   and PredictiveBackHandler so in-app back navigation never accidentally exits the app.
 * - Layer 1 renders full-screen overlays with Hero Bounds Morph + PredictiveBackHandler edge peel.
 * - Both Close/Back button clicks AND edge back gestures share the EXACT SAME silky exit motion.
 */
@Composable
fun <TabT : Any, OverlayT : Any> AppleLayeredNavigationHost(
    currentTab: TabT,
    overlay: OverlayT?,
    heroKey: String?,
    onDismissOverlay: () -> Unit,
    onRootBack: (() -> Unit)? = null,
    bottomChrome: @Composable (overlayReveal: Float) -> Unit,
    renderTab: @Composable (TabT) -> Unit,
    renderOverlay: @Composable (OverlayT, dismiss: () -> Unit) -> Unit,
) {
    val heroBounds = LocalHeroBounds.current
    val density = LocalDensity.current
    val saveableHolder = rememberSaveableStateHolder()
    val haptic = LocalHapticFeedback.current
    val reduceMotion = rememberReduceMotion()
    val scope = rememberCoroutineScope()

    var displayedOverlay by remember { mutableStateOf<OverlayT?>(null) }
    var displayedHeroRect by remember { mutableStateOf<Rect?>(null) }
    var isDismissing by remember { mutableStateOf(false) }

    val dismissProgress = remember { Animatable(1f) }
    var swipeEdge by remember { mutableIntStateOf(BackEventCompat.EDGE_LEFT) }
    var startTouchY by remember { mutableFloatStateOf(0f) }
    var currentTouchY by remember { mutableFloatStateOf(0f) }
    var isPeeling by remember { mutableStateOf(false) }
    var usedEdgeSwipe by remember { mutableStateOf(false) }

    val rootBackProgress = remember { Animatable(0f) }
    var rootBackEdge by remember { mutableIntStateOf(BackEventCompat.EDGE_LEFT) }

    LaunchedEffect(overlay) {
        if (overlay != null) {
            isDismissing = false
            usedEdgeSwipe = false
            displayedOverlay = overlay
            displayedHeroRect = heroBounds[heroKey]
            dismissProgress.snapTo(1f)
            dismissProgress.animateTo(
                targetValue = 0f,
                animationSpec = if (reduceMotion) tween(160, easing = AppMotion.AppleEase) else AppMotion.navigationSpring(),
            )
        } else if (displayedOverlay != null && !isDismissing) {
            // External dismissal (e.g. QR scan completed): still play the exact same silky exit motion
            isDismissing = true
            usedEdgeSwipe = false
            val spec: AnimationSpec<Float> = when {
                reduceMotion -> tween(140, easing = AppMotion.AppleEase)
                displayedHeroRect != null -> AppMotion.navigationSpring()
                else -> tween(AppMotion.ScreenPop, easing = AppMotion.AppleSheet)
            }
            dismissProgress.animateTo(1f, spec)
            displayedOverlay = null
            displayedHeroRect = null
            isDismissing = false
        }
    }

    fun requestOverlayDismiss(fromEdgeSwipe: Boolean = false) {
        if (isDismissing || displayedOverlay == null) return
        isDismissing = true
        usedEdgeSwipe = fromEdgeSwipe
        if (!fromEdgeSwipe) {
            startTouchY = 0f
            currentTouchY = 0f
        }
        scope.launch {
            try {
                val spec: AnimationSpec<Float> = when {
                    reduceMotion -> tween(140, easing = AppMotion.AppleEase)
                    displayedHeroRect != null -> AppMotion.navigationSpring()
                    else -> tween(AppMotion.ScreenPop, easing = AppMotion.AppleSheet)
                }
                dismissProgress.animateTo(1f, spec)
            } finally {
                onDismissOverlay()
                displayedOverlay = null
                displayedHeroRect = null
                isPeeling = false
                isDismissing = false
            }
        }
    }

    // Layer 0 in-app predictive back (navigates back through result cards, sub-modes, tab history)
    if (displayedOverlay == null && onRootBack != null) {
        PredictiveBackHandler(enabled = true) { events ->
            var committed = false
            var capturedStart = false
            try {
                events.collect { event ->
                    val raw = event.progress.coerceIn(0f, 1f)
                    val curved = raw * 0.65f + (1f - (1f - raw) * (1f - raw)) * 0.35f
                    rootBackEdge = event.swipeEdge
                    if (!capturedStart) {
                        capturedStart = true
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    }
                    rootBackProgress.snapTo(curved)
                }
                committed = true
            } catch (_: CancellationException) {
                committed = false
            }

            if (committed) {
                onRootBack()
            }
            scope.launch {
                rootBackProgress.animateTo(
                    targetValue = 0f,
                    animationSpec = if (reduceMotion) tween(120, easing = AppMotion.AppleEase) else AppMotion.navigationSpring(),
                )
            }
        }
    }

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(HefimerColors.Black),
    ) {
        val rootW = with(density) { maxWidth.toPx() }.coerceAtLeast(1f)
        val rootH = with(density) { maxHeight.toPx() }.coerceAtLeast(1f)

        val overlayVisible = displayedOverlay != null
        val reveal = if (overlayVisible) (1f - dismissProgress.value).coerceIn(0f, 1f) else 0f
        val rootBack = rootBackProgress.value.coerceIn(0f, 1f)

        // LAYER 0: Zero-remount base tabs + synchronized depth parallax & optical scrim
        Box(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    val depthScale = if (reduceMotion) 1f else 1f - 0.055f * reveal
                    val backScale = if (reduceMotion) 1f else 1f - 0.032f * rootBack
                    val edgeDir = if (rootBackEdge == BackEventCompat.EDGE_LEFT) 1f else -1f
                    scaleX = depthScale * backScale
                    scaleY = depthScale * backScale
                    translationX = if (reduceMotion) 0f else edgeDir * 14.dp.toPx() * rootBack
                    alpha = 1f - 0.14f * reveal
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
                        .background(Color.Black.copy(alpha = 0.28f * reveal)),
                )
            }
        }

        // LAYER 1: Full-screen detail/workspace overlay with Hero Morph + unified Back/Close motion
        val activeOverlay = displayedOverlay
        if (activeOverlay != null) {
            // IMPORTANT: Keep enabled = true while overlay is mounted so setting isDismissing = true
            // never cancels this handler's coroutine mid-flight!
            PredictiveBackHandler(enabled = true) { events ->
                if (isDismissing) {
                    events.collect {}
                    return@PredictiveBackHandler
                }
                var committed = false
                isPeeling = true
                usedEdgeSwipe = true
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
                    // Launch completion on composition scope so it can never be cancelled by back handler disposal
                    requestOverlayDismiss(fromEdgeSwipe = true)
                } else if (!isDismissing) {
                    scope.launch {
                        dismissProgress.animateTo(0f, AppMotion.navigationSpring())
                    }
                }
            }

            val pReveal = (1f - dismissProgress.value).coerceIn(0f, 1f)
            val heroRect = displayedHeroRect
            val cornerDp = if (reduceMotion) {
                0.dp
            } else {
                when {
                    heroRect != null ->
                        ((1f - pReveal) * 28f + if (isPeeling) dismissProgress.value * 30f else 0f)
                            .coerceIn(0f, 32f).dp
                    isPeeling || dismissProgress.value > 0.001f -> (dismissProgress.value * 30f).dp
                    else -> 0.dp
                }
            }

            Surface(
                color = HefimerColors.Black,
                shape = RoundedCornerShape(cornerDp),
                shadowElevation = if (dismissProgress.value > 0.01f) 18.dp else 0.dp,
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        if (reduceMotion) {
                            alpha = pReveal
                            return@graphicsLayer
                        }
                        if (heroRect != null) {
                            val minScale = (heroRect.width / rootW).coerceIn(0.22f, 0.88f)
                            val s = minScale + (1f - minScale) * pReveal
                            val peelBonusScale = if (isPeeling) 1f - 0.055f * dismissProgress.value else 1f
                            scaleX = s * peelBonusScale
                            scaleY = s * peelBonusScale
                            translationX = (heroRect.center.x - rootW / 2f) * (1f - pReveal)
                            translationY = (heroRect.center.y - rootH / 2f) * (1f - pReveal)
                            alpha = (pReveal / 0.22f).coerceIn(0f, 1f)
                        } else {
                            val d = dismissProgress.value
                            val s = 1f - 0.11f * d
                            scaleX = s
                            scaleY = s
                            if (usedEdgeSwipe) {
                                val edgeDir = if (swipeEdge == BackEventCompat.EDGE_LEFT) 1f else -1f
                                val deltaY = ((currentTouchY - startTouchY) * 0.08f)
                                    .coerceIn(-16.dp.toPx(), 16.dp.toPx())
                                translationX = edgeDir * 28.dp.toPx() * d
                                translationY = deltaY * d
                                rotationZ = edgeDir * (deltaY / 16.dp.toPx()) * 0.85f * d
                            } else {
                                translationX = 0f
                                translationY = 44.dp.toPx() * d
                                rotationZ = 0f
                            }
                            alpha = (1f - (d - 0.55f).coerceAtLeast(0f) / 0.45f).coerceIn(0f, 1f)
                        }
                    },
            ) {
                renderOverlay(activeOverlay) { requestOverlayDismiss(fromEdgeSwipe = false) }
            }
        }
    }
}

/**
 * Swipe-to-dismiss sheet with 1:1 vertical tracking, 0.55 elastic rubber-banding on upward pull,
 * momentum projection, release velocity handoff, AND full PredictiveBackHandler real-time edge-peel
 * tracking so even small bottom sheets respond fluidly to Android predictive back gestures.
 */
@Composable
fun AppleSwipeableSheet(
    visible: Boolean,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable (dismiss: () -> Unit) -> Unit,
) {
    val reduceMotion = rememberReduceMotion()
    val haptic = LocalHapticFeedback.current
    val scope = rememberCoroutineScope()
    var isMounted by remember { mutableStateOf(false) }
    var isDismissing by remember { mutableStateOf(false) }
    val offsetY = remember { Animatable(1200f) }
    val backPeel = remember { Animatable(0f) }
    val velocityTracker = remember { VelocityTracker() }

    LaunchedEffect(visible) {
        if (visible) {
            isMounted = true
            isDismissing = false
            backPeel.snapTo(0f)
            offsetY.snapTo(1200f)
            offsetY.animateTo(
                targetValue = 0f,
                animationSpec = if (reduceMotion) tween(160, easing = AppMotion.AppleEase) else AppMotion.navigationSpring(),
            )
        } else if (isMounted && !isDismissing) {
            isDismissing = true
            try {
                offsetY.animateTo(
                    targetValue = 1200f,
                    animationSpec = if (reduceMotion) tween(140, easing = AppMotion.AppleEase) else tween(AppMotion.Sheet, easing = AppMotion.AppleSheet),
                )
            } finally {
                isMounted = false
                isDismissing = false
                backPeel.snapTo(0f)
            }
        }
    }

    if (!isMounted) return

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val heightPx = constraints.maxHeight.toFloat().coerceAtLeast(1f)
        val dragDismissFrac = (offsetY.value / (heightPx * 0.65f)).coerceIn(0f, 1f)
        val combinedDismiss = maxOf(dragDismissFrac, backPeel.value * 0.72f).coerceIn(0f, 1f)
        val progress = (1f - combinedDismiss).coerceIn(0f, 1f)

        fun requestSheetDismiss(initialVelocity: Float = 0f) {
            if (isDismissing) return
            isDismissing = true
            scope.launch {
                try {
                    val spec: AnimationSpec<Float> = when {
                        reduceMotion -> tween(140, easing = AppMotion.AppleEase)
                        initialVelocity > 250f -> spring(dampingRatio = 0.90f, stiffness = 200f)
                        else -> tween(AppMotion.Sheet, easing = AppMotion.AppleSheet)
                    }
                    offsetY.animateTo(
                        targetValue = heightPx,
                        initialVelocity = initialVelocity,
                        animationSpec = spec,
                    )
                } finally {
                    isMounted = false
                    isDismissing = false
                    backPeel.snapTo(0f)
                    onDismiss()
                }
            }
        }

        // Full Predictive Back Gesture tracking for EVERY bottom sheet (never deadlocks!)
        PredictiveBackHandler(enabled = true) { events ->
            if (isDismissing) {
                events.collect {}
                return@PredictiveBackHandler
            }
            var committed = false
            var capturedStart = false
            try {
                events.collect { event ->
                    val raw = event.progress.coerceIn(0f, 1f)
                    val curved = raw * 0.68f + (1f - (1f - raw) * (1f - raw)) * 0.32f
                    if (!capturedStart) {
                        capturedStart = true
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    }
                    backPeel.snapTo(curved)
                    offsetY.snapTo(curved * heightPx * 0.28f)
                }
                committed = true
            } catch (_: CancellationException) {
                committed = false
            }

            if (committed) {
                requestSheetDismiss()
            } else if (!isDismissing) {
                scope.launch { backPeel.animateTo(0f, AppMotion.navigationSpring()) }
                scope.launch { offsetY.animateTo(0f, AppMotion.navigationSpring()) }
            }
        }

        // Backdrop scrim derived from single progress scalar
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.68f * progress))
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                ) {
                    requestSheetDismiss()
                },
        )

        Box(
            modifier = modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .graphicsLayer {
                    val peel = backPeel.value
                    val elasticStretch = if (offsetY.value < 0f && !reduceMotion) {
                        1f + (-offsetY.value / (heightPx * 0.85f)).coerceIn(0f, 0.032f)
                    } else {
                        1f
                    }
                    val peelScale = if (reduceMotion) 1f else 1f - 0.055f * peel
                    translationY = offsetY.value
                    scaleX = peelScale
                    scaleY = peelScale * elasticStretch
                    transformOrigin = androidx.compose.ui.graphics.TransformOrigin(0.5f, 1f)
                }
                .pointerInput(heightPx) {
                    detectVerticalDragGestures(
                        onDragStart = { velocityTracker.resetTracking() },
                        onVerticalDrag = { change, dragAmount ->
                            velocityTracker.addPosition(change.uptimeMillis, change.position)
                            change.consume()
                            val candidate = offsetY.value + dragAmount
                            val mapped = if (candidate < 0f) {
                                -rubberBand(-candidate, heightPx * 0.36f, 0.55f)
                            } else {
                                candidate
                            }
                            scope.launch { offsetY.snapTo(mapped) }
                        },
                        onDragEnd = {
                            val vy = velocityTracker.calculateVelocity().y
                            val projected = offsetY.value + projectOffset(vy, decelerationRate = 0.99f)
                            val shouldDismiss = projected > heightPx * 0.24f || vy > 900f
                            if (shouldDismiss) {
                                requestSheetDismiss(initialVelocity = vy)
                            } else {
                                scope.launch {
                                    offsetY.animateTo(
                                        targetValue = 0f,
                                        initialVelocity = vy,
                                        animationSpec = AppMotion.navigationSpring(),
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
            content { requestSheetDismiss() }
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
                (slideInVertically(tween(340, easing = AppMotion.AppleSheet)) { it * dir } +
                    fadeIn(tween(260, easing = AppMotion.AppleSilk)))
                    .togetherWith(
                        slideOutVertically(tween(280, easing = AppMotion.AppleSilk)) { -it * dir } +
                            fadeOut(tween(200, easing = AppMotion.AppleEase)),
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

/**
 * Per-digit independent rolling percentage display (only changing digit columns slide,
 * keeping tens/hundreds rock-steady with tabular numerals `tnum`).
 */
@Composable
fun AppleRollingPercentage(
    percent: Int,
    style: TextStyle = MaterialTheme.typography.headlineMedium,
    color: Color = HefimerColors.White,
) {
    val clamped = percent.coerceIn(0, 100)
    val text = clamped.toString()
    var prev by remember { mutableIntStateOf(clamped) }
    val dir = if (clamped >= prev) 1 else -1
    SideEffect { prev = clamped }
    val reduceMotion = rememberReduceMotion()

    Row(verticalAlignment = Alignment.CenterVertically) {
        text.forEachIndexed { idx, ch ->
            // Key from right so ones/tens columns keep identity when going 9 -> 10 -> 100
            val colKey = text.length - 1 - idx
            androidx.compose.runtime.key(colKey) {
                AnimatedContent(
                    targetState = ch,
                    transitionSpec = {
                        if (reduceMotion) {
                            fadeIn(tween(100, easing = AppMotion.AppleEase)) togetherWith
                                fadeOut(tween(90, easing = AppMotion.AppleEase))
                        } else {
                            (slideInVertically(tween(340, easing = AppMotion.AppleSheet)) { (it * 0.78f * dir).toInt() } +
                                fadeIn(tween(260, easing = AppMotion.AppleSilk)))
                                .togetherWith(
                                    slideOutVertically(tween(280, easing = AppMotion.AppleSilk)) { (-it * 0.78f * dir).toInt() } +
                                        fadeOut(tween(180, easing = AppMotion.AppleEase)),
                                )
                                .using(SizeTransform(clip = true))
                        }
                    },
                    label = "pct_digit_$colKey",
                ) { digit ->
                    Text(
                        text = digit.toString(),
                        color = color,
                        style = style.copy(fontFeatureSettings = "tnum"),
                    )
                }
            }
        }
        Text(
            text = "%",
            color = color.copy(alpha = 0.6f),
            style = style.copy(
                fontSize = style.fontSize * 0.68f,
                fontFeatureSettings = "tnum",
            ),
            modifier = Modifier.padding(start = 2.dp),
        )
    }
}

/**
 * Spring-interpolated fluid progress bar with subtle specular sweep across the filled track.
 */
@Composable
fun AppleFluidProgressTrack(
    progress: Float,
    accentColor: Color = HefimerColors.White,
    trackColor: Color = HefimerColors.White10,
    height: Dp = 6.dp,
    modifier: Modifier = Modifier,
) {
    val reduceMotion = rememberReduceMotion()
    val animatedProgress by animateFloatAsState(
        targetValue = progress.coerceIn(0f, 1f),
        animationSpec = if (reduceMotion) tween(120, easing = AppMotion.AppleEase) else AppMotion.gentleSpring(),
        label = "fluid_progress_fill",
    )

    val sheenOffset = if (!reduceMotion && animatedProgress in 0.02f..0.99f) {
        val infinite = androidx.compose.animation.core.rememberInfiniteTransition(label = "progress_sheen")
        val anim by infinite.animateFloat(
            initialValue = -0.35f,
            targetValue = 1.35f,
            animationSpec = androidx.compose.animation.core.infiniteRepeatable(
                animation = tween(1900, easing = androidx.compose.animation.core.LinearEasing),
            ),
            label = "sheen_x",
        )
        anim
    } else {
        -1f
    }

    androidx.compose.foundation.Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(height)
            .clip(androidx.compose.foundation.shape.CircleShape),
    ) {
        drawRect(color = trackColor)
        val fillW = (size.width * animatedProgress).coerceAtLeast(if (animatedProgress > 0f) size.height else 0f)
        if (fillW > 0f) {
            drawRoundRect(
                color = accentColor,
                size = androidx.compose.ui.geometry.Size(fillW, size.height),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(size.height / 2f, size.height / 2f),
            )
            if (sheenOffset > -0.5f) {
                val bandW = (fillW * 0.45f).coerceAtLeast(36f)
                val startX = fillW * sheenOffset - bandW / 2f
                drawRect(
                    brush = androidx.compose.ui.graphics.Brush.horizontalGradient(
                        colors = listOf(Color.Transparent, Color.White.copy(alpha = 0.42f), Color.Transparent),
                        startX = startX,
                        endX = startX + bandW,
                    ),
                    size = androidx.compose.ui.geometry.Size(fillW, size.height),
                )
            }
        }
    }
}
