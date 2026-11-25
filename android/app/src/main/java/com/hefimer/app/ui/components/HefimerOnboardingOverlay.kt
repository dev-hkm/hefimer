package com.hefimer.app.ui.components

import androidx.activity.compose.PredictiveBackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.CloudUpload
import androidx.compose.material.icons.rounded.Code
import androidx.compose.material.icons.rounded.Description
import androidx.compose.material.icons.rounded.Devices
import androidx.compose.material.icons.rounded.Forum
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hefimer.app.ui.theme.AppLanguage
import com.hefimer.app.ui.theme.AppMotion
import com.hefimer.app.ui.theme.AppleGlassVariant
import com.hefimer.app.ui.theme.AppleMonochromeBackdrop
import com.hefimer.app.ui.theme.AppleSegmentedControl
import com.hefimer.app.ui.theme.HefimerColors
import com.hefimer.app.ui.theme.JetBrainsMonoFamily
import com.hefimer.app.ui.theme.UbuntuFontFamily
import com.hefimer.app.ui.theme.appleGlassSurface
import com.hefimer.app.ui.theme.applePressable
import com.hefimer.app.ui.theme.rememberReduceMotion
import com.hefimer.app.ui.theme.tr
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin

private data class OnboardingSlide(
    val kickerEn: String,
    val kickerVi: String,
    val titleEn: String,
    val titleVi: String,
    val subtitleEn: String,
    val subtitleVi: String,
    val emeraldAccent: Boolean = false,
)

private val ONBOARDING_SLIDES = listOf(
    OnboardingSlide(
        kickerEn = "INSTANT TRANSFER",
        kickerVi = "CHIA SẺ TỨC THÌ",
        titleEn = "Share anything with a 5-digit code.",
        titleVi = "Chia sẻ mọi thứ chỉ bằng mã 5 chữ số.",
        subtitleEn = "Drop files, folders, or live syntax-highlighted code. Zero sign-up — gone when its moment ends.",
        subtitleVi = "Gửi tệp, thư mục hoặc code tô màu trực tiếp. Không cần đăng nhập — tự hủy khi hết hạn.",
    ),
    OnboardingSlide(
        kickerEn = "MULTI-HOST ENGINE",
        kickerVi = "HỆ THỐNG 12 MÁY CHỦ",
        titleEn = "12 storage hosts. Smart auto-routing.",
        titleVi = "12 máy chủ lưu trữ. Tự động chuyển tuyến.",
        subtitleEn = "Defaults to lightning-fast sto.care and automatically routes huge files to high-capacity hosts.",
        subtitleVi = "Mặc định sto.care siêu nhanh và tự động điều hướng tệp dung lượng lớn sang máy chủ phù hợp.",
        emeraldAccent = true,
    ),
    OnboardingSlide(
        kickerEn = "COLLABORATION",
        kickerVi = "CỘNG TÁC THỜI GIAN THỰC",
        titleEn = "Infinite Space, Rooms & Link.",
        titleVi = "Space vô hạn, Phòng chat & Link thiết bị.",
        subtitleEn = "Pin notes, code, and media on a zoomable canvas, chat in ephemeral rooms, or beam to nearby devices.",
        subtitleVi = "Ghim ghi chú, code, tệp trên bảng vẽ vô hạn, trò chuyện trong phòng tạm thời hoặc gửi thẳng giữa các thiết bị.",
    ),
    OnboardingSlide(
        kickerEn = "PERSONALIZE",
        kickerVi = "CÁ NHÂN HÓA",
        titleEn = "Crafted for speed and calm.",
        titleVi = "Tinh tế, mượt mà và làm chủ theo ý bạn.",
        subtitleEn = "Choose OLED Dark or Light, switch language anytime, and enjoy fluid predictive back gestures.",
        subtitleVi = "Chọn giao diện Tối/Sáng và ngôn ngữ ngay tại đây. Hỗ trợ cử chỉ vuốt mượt chuẩn Apple mọi nơi.",
        emeraldAccent = true,
    ),
)

/**
 * 4-screen Apple-grade first-launch Onboarding flow with:
 * - Real-time HorizontalPager parallax (hero visual + typography depth separation)
 * - 4 bespoke animated Apple Glass hero canvases
 * - Morphing pill page indicator & adaptive Continue / Get Started CTA
 * - PredictiveBackHandler (steps back through pages, or dismisses on page 0)
 * - Silky scale + fade exit into SendScreen
 */
@Composable
fun HefimerOnboardingOverlay(
    isLightMode: Boolean,
    appLanguage: AppLanguage,
    defaultProviderName: String,
    onSetLightMode: (Boolean) -> Unit,
    onSetAppLanguage: (AppLanguage) -> Unit,
    onComplete: () -> Unit,
) {
    val reduceMotion = rememberReduceMotion()
    val haptic = LocalHapticFeedback.current
    val scope = rememberCoroutineScope()
    val pagerState = rememberPagerState(pageCount = { ONBOARDING_SLIDES.size })

    val statusTop = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    val navBottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()

    val exitProgress = remember { Animatable(0f) }
    var isExiting by remember { mutableStateOf(false) }

    fun finishOnboarding() {
        if (isExiting) return
        isExiting = true
        scope.launch {
            try {
                exitProgress.animateTo(
                    targetValue = 1f,
                    animationSpec = if (reduceMotion) {
                        tween(140, easing = AppMotion.AppleEase)
                    } else {
                        tween(540, easing = AppMotion.AppleSheet)
                    },
                )
            } finally {
                onComplete()
            }
        }
    }

    // Subtle single-tick haptic on page settlement
    var lastSettledPage by remember { mutableStateOf(0) }
    LaunchedEffect(pagerState.settledPage) {
        if (pagerState.settledPage != lastSettledPage) {
            lastSettledPage = pagerState.settledPage
            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
        }
    }

    // Predictive back: navigate to previous onboarding slide, or smoothly complete on slide 0
    PredictiveBackHandler(enabled = true) { events ->
        var committed = false
        try {
            events.collect { }
            committed = true
        } catch (_: CancellationException) {
            committed = false
        }
        if (committed) {
            if (pagerState.currentPage > 0) {
                scope.launch {
                    pagerState.animateScrollToPage(
                        page = pagerState.currentPage - 1,
                        animationSpec = if (reduceMotion) {
                            tween(160, easing = AppMotion.AppleEase)
                        } else {
                            tween(520, easing = AppMotion.AppleSheet)
                        },
                    )
                }
            } else {
                finishOnboarding()
            }
        }
    }

    val currentPage = pagerState.currentPage
    val isLastPage = currentPage == ONBOARDING_SLIDES.lastIndex
    val pExit = exitProgress.value

    Box(
        modifier = Modifier
            .fillMaxSize()
            .graphicsLayer {
                alpha = (1f - pExit).coerceIn(0f, 1f)
                val s = if (reduceMotion) 1f else 1f + 0.045f * pExit
                scaleX = s
                scaleY = s
            },
    ) {
        AppleMonochromeBackdrop()

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = statusTop + 14.dp, bottom = navBottom + 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            // Top Brand Bar + Glass Skip Pill
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 22.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(9.dp),
                ) {
                    HefimerOrbitLogo(size = 24.dp, tint = HefimerColors.White)
                    Text(
                        text = "Hefimer",
                        fontFamily = UbuntuFontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        letterSpacing = (-0.4).sp,
                        color = HefimerColors.White,
                    )
                }

                Box(
                    modifier = Modifier
                        .graphicsLayer { alpha = if (isLastPage) 0f else 1f }
                        .appleGlassSurface(
                            shape = CircleShape,
                            variant = AppleGlassVariant.Clear,
                            elevation = 4.dp,
                        )
                        .applePressable(enabled = !isLastPage, pressedScale = 0.94f) { finishOnboarding() }
                        .padding(horizontal = 14.dp, vertical = 7.dp),
                ) {
                    Text(
                        text = tr("Skip", "Bỏ qua"),
                        color = HefimerColors.White75,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
            }

            // Interactive 4-Page Carousel with continuous parallax
            HorizontalPager(
                state = pagerState,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
            ) { page ->
                val pageOffset = (pagerState.currentPage - page) + pagerState.currentPageOffsetFraction
                val absOff = abs(pageOffset).coerceIn(0f, 1f)
                val slideData = ONBOARDING_SLIDES[page]

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 26.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) {
                    // Bespoke Hero Stage with smooth depth parallax
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(312.dp)
                            .graphicsLayer {
                                if (!reduceMotion) {
                                    translationX = pageOffset * 90f
                                    val s = 1f - 0.09f * absOff
                                    scaleX = s
                                    scaleY = s
                                    alpha = (1f - 0.55f * absOff).coerceIn(0f, 1f)
                                }
                            },
                    ) {
                        when (page) {
                            0 -> EphemeralDropHero(isActive = currentPage == 0, reduceMotion = reduceMotion)
                            1 -> MultiHostOrbitHero(reduceMotion = reduceMotion)
                            2 -> SpatialCanvasHero(reduceMotion = reduceMotion)
                            else -> PersonalizeThemeHero(
                                isLightMode = isLightMode,
                                appLanguage = appLanguage,
                                defaultProviderName = defaultProviderName,
                                onSetLightMode = onSetLightMode,
                                onSetAppLanguage = onSetAppLanguage,
                            )
                        }
                    }

                    Spacer(Modifier.height(24.dp))

                    // Editorial Typography with subtle secondary parallax
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .fillMaxWidth()
                            .graphicsLayer {
                                if (!reduceMotion) {
                                    translationX = pageOffset * 42f
                                    alpha = (1f - 0.65f * absOff).coerceIn(0f, 1f)
                                }
                            },
                    ) {
                        Text(
                            text = tr(slideData.kickerEn, slideData.kickerVi),
                            color = HefimerColors.White45,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 2.6.sp,
                            textAlign = TextAlign.Center,
                        )
                        Spacer(Modifier.height(10.dp))
                        Text(
                            text = tr(slideData.titleEn, slideData.titleVi),
                            fontFamily = UbuntuFontFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = 27.sp,
                            lineHeight = 33.sp,
                            letterSpacing = (-0.6).sp,
                            color = HefimerColors.White,
                            textAlign = TextAlign.Center,
                        )
                        Spacer(Modifier.height(12.dp))
                        Text(
                            text = tr(slideData.subtitleEn, slideData.subtitleVi),
                            color = HefimerColors.White55,
                            fontSize = 14.sp,
                            lineHeight = 21.sp,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(horizontal = 8.dp),
                        )
                    }
                }
            }

            // Bottom Morphing Pill Indicators + Adaptive Primary Action
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(22.dp),
            ) {
                // Morphing Pill Page Indicator
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    repeat(ONBOARDING_SLIDES.size) { idx ->
                        val active = idx == currentPage
                        val width by animateDpAsState(
                            targetValue = if (active) 28.dp else 7.dp,
                            animationSpec = AppMotion.layoutSpring(),
                            label = "onb_dot_w_$idx",
                        )
                        val dotColor = if (active) HefimerColors.White else HefimerColors.White20
                        Box(
                            modifier = Modifier
                                .width(width)
                                .height(7.dp)
                                .clip(CircleShape)
                                .background(dotColor)
                                .applePressable(pressedScale = 0.9f) {
                                    scope.launch {
                                        pagerState.animateScrollToPage(
                                            page = idx,
                                            animationSpec = tween(560, easing = AppMotion.AppleSheet),
                                        )
                                    }
                                },
                        )
                    }
                }

                // Adaptive Primary CTA Pill (Pure Monochrome)
                val ctaBg = HefimerColors.White
                val ctaFg = HefimerColors.Black

                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                        .clip(CircleShape)
                        .background(ctaBg)
                        .applePressable(pressedScale = 0.96f) {
                            if (isLastPage) {
                                finishOnboarding()
                            } else {
                                scope.launch {
                                    pagerState.animateScrollToPage(
                                        page = currentPage + 1,
                                        animationSpec = if (reduceMotion) {
                                            tween(160, easing = AppMotion.AppleEase)
                                        } else {
                                            tween(540, easing = AppMotion.AppleSheet)
                                        },
                                    )
                                }
                            }
                        },
                ) {
                    AnimatedContent(
                        targetState = isLastPage,
                        transitionSpec = {
                            (fadeIn(tween(340, easing = AppMotion.AppleSilk)) +
                                scaleIn(tween(380, easing = AppMotion.AppleSheet), initialScale = 0.92f)) togetherWith
                                (fadeOut(tween(220, easing = AppMotion.AppleEase)) +
                                    scaleOut(tween(220, easing = AppMotion.AppleEase), targetScale = 0.94f))
                        },
                        label = "onb_cta_label",
                    ) { last ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            Text(
                                text = if (last) tr("Start Using Hefimer", "Bắt đầu dùng Hefimer") else tr("Continue", "Tiếp tục"),
                                color = ctaFg,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = (-0.2).sp,
                            )
                            Icon(
                                imageVector = if (last) Icons.Rounded.AutoAwesome else Icons.AutoMirrored.Rounded.ArrowForward,
                                contentDescription = null,
                                tint = ctaFg,
                                modifier = Modifier.size(18.dp),
                            )
                        }
                    }
                }
            }
        }
    }
}

// =========================================================================
// PAGE 1 HERO: Live 5-Digit Ephemeral Drop Card with Apple Glass Crystal Tiles
// =========================================================================
@Composable
private fun EphemeralDropHero(
    isActive: Boolean,
    reduceMotion: Boolean,
) {
    val inf = rememberInfiniteTransition(label = "onb_drop_float")
    val floatY by inf.animateFloat(
        initialValue = -5f,
        targetValue = 5f,
        animationSpec = infiniteRepeatable(
            animation = tween(3200, easing = AppMotion.AppleEase),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "drop_float_y",
    )

    val digits = listOf('4', '8', '2', '9', '1')

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .fillMaxWidth()
            .graphicsLayer { if (!reduceMotion) translationY = floatY }
            .appleGlassSurface(
                shape = RoundedCornerShape(36.dp),
                variant = AppleGlassVariant.Elevated,
                elevation = 18.dp,
            )
            .padding(horizontal = 20.dp, vertical = 22.dp),
    ) {
        // Floating glass file chip
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier
                .appleGlassSurface(
                    shape = CircleShape,
                    variant = AppleGlassVariant.Clear,
                    elevation = 0.dp,
                )
                .padding(horizontal = 12.dp, vertical = 6.dp),
        ) {
            Icon(Icons.Rounded.Description, null, tint = HefimerColors.White85, modifier = Modifier.size(14.dp))
            Text(
                text = tr("launch_deck.pdf", "ho_so_ra_mat.pdf"),
                color = HefimerColors.White90,
                fontFamily = JetBrainsMonoFamily,
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium,
            )
            Text(
                text = "24 MB",
                color = HefimerColors.White45,
                fontFamily = JetBrainsMonoFamily,
                fontSize = 10.sp,
            )
        }

        Spacer(Modifier.height(18.dp))

        // 5-digit staggered crystal glass tiles
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            digits.forEachIndexed { idx, ch ->
                val p = remember(isActive) { Animatable(if (reduceMotion) 1f else 0f) }
                LaunchedEffect(isActive) {
                    if (!reduceMotion && isActive) {
                        p.snapTo(0f)
                        delay(idx * 105L + 120L)
                        p.animateTo(1f, tween(720, easing = AppMotion.AppleSheet))
                    } else if (reduceMotion) {
                        p.snapTo(1f)
                    }
                }
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .width(44.dp)
                        .height(58.dp)
                        .graphicsLayer {
                            val v = p.value
                            alpha = (v * 1.15f).coerceIn(0f, 1f)
                            translationY = (1f - v) * 32f
                            val s = 0.85f + 0.15f * v
                            scaleX = s
                            scaleY = s
                        }
                        .appleGlassSurface(
                            shape = RoundedCornerShape(20.dp),
                            variant = AppleGlassVariant.Clear,
                            elevation = 2.dp,
                        ),
                ) {
                    Text(
                        text = ch.toString(),
                        fontFamily = UbuntuFontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 24.sp,
                        color = HefimerColors.White,
                    )
                }
            }
        }

        Spacer(Modifier.height(16.dp))

        // Self-destruct countdown pill
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            modifier = Modifier
                .appleGlassSurface(
                    shape = CircleShape,
                    variant = AppleGlassVariant.Clear,
                    elevation = 0.dp,
                )
                .padding(horizontal = 12.dp, vertical = 6.dp),
        ) {
            Icon(Icons.Rounded.Schedule, null, tint = HefimerColors.White60, modifier = Modifier.size(13.dp))
            Text(
                text = tr("SELF-DESTRUCTS IN 01:00:00", "TỰ HỦY SAU 01:00:00"),
                color = HefimerColors.White75,
                fontFamily = JetBrainsMonoFamily,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.8.sp,
            )
        }
    }
}

// =========================================================================
// PAGE 2 HERO: Orbital 12-Host Smart Auto-Routing Constellation
// =========================================================================
@Composable
private fun MultiHostOrbitHero(reduceMotion: Boolean) {
    val inf = rememberInfiniteTransition(label = "onb_orbit")
    val angle by inf.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(22_000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "orbit_angle",
    )
    val pulse by inf.animateFloat(
        initialValue = 0.96f,
        targetValue = 1.04f,
        animationSpec = infiniteRepeatable(
            animation = tween(2600, easing = AppMotion.AppleEase),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "center_pulse",
    )

    val ringColor = HefimerColors.White15
    val orbitAccent = HefimerColors.White

    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier.size(280.dp),
    ) {
        // Concentric orbital rings
        Canvas(modifier = Modifier.fillMaxSize()) {
            val c = center
            drawCircle(
                color = ringColor,
                radius = size.minDimension * 0.44f,
                center = c,
                style = Stroke(
                    width = 1.2.dp.toPx(),
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f)),
                ),
            )
            drawCircle(
                color = orbitAccent.copy(alpha = 0.24f),
                radius = size.minDimension * 0.26f,
                center = c,
                style = Stroke(width = 1.2.dp.toPx()),
            )
            // Orbiting specular dot
            val rad = (if (reduceMotion) 35f else angle) * PI / 180.0
            val r = size.minDimension * 0.44f
            val dotX = c.x + (r * cos(rad)).toFloat()
            val dotY = c.y + (r * sin(rad)).toFloat()
            drawCircle(color = orbitAccent.copy(alpha = 0.25f), radius = 10.dp.toPx(), center = Offset(dotX, dotY))
            drawCircle(color = orbitAccent, radius = 4.dp.toPx(), center = Offset(dotX, dotY))
        }

        // Center glass lens orb (sto.care)
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier
                .size(104.dp)
                .graphicsLayer {
                    if (!reduceMotion) {
                        scaleX = pulse
                        scaleY = pulse
                    }
                }
                .appleGlassSurface(
                    shape = CircleShape,
                    variant = AppleGlassVariant.Elevated,
                    elevation = 20.dp,
                ),
        ) {
            Icon(Icons.Rounded.CloudUpload, null, tint = HefimerColors.White, modifier = Modifier.size(24.dp))
            Spacer(Modifier.height(4.dp))
            Text(
                text = "sto.care",
                color = HefimerColors.White,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
            )
            Text(
                text = tr("DEFAULT", "MẶC ĐỊNH"),
                color = HefimerColors.White50,
                fontSize = 8.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.4.sp,
            )
        }

        // Satellite Apple Glass provider chips around orbit (Secret R2 keeps subtle emerald!)
        HostSatelliteChip("storage.to • 10 GB", Modifier.align(Alignment.TopCenter).offset(y = 4.dp))
        HostSatelliteChip("Gofile • ∞", Modifier.align(Alignment.CenterStart).offset(x = (-4).dp, y = (-26).dp))
        HostSatelliteChip("Viking • 15 GB", Modifier.align(Alignment.CenterEnd).offset(x = 4.dp, y = (-26).dp))
        HostSatelliteChip("Secret R2", Modifier.align(Alignment.BottomStart).offset(x = 14.dp, y = (-20).dp), emerald = true)
        HostSatelliteChip("Uguu • Put.icu", Modifier.align(Alignment.BottomEnd).offset(x = (-10).dp, y = (-20).dp))
    }
}

@Composable
private fun HostSatelliteChip(
    label: String,
    modifier: Modifier = Modifier,
    emerald: Boolean = false,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(5.dp),
        modifier = modifier
            .appleGlassSurface(
                shape = CircleShape,
                variant = AppleGlassVariant.Regular,
                accentBorderTint = if (emerald) HefimerColors.Emerald400 else null,
                accentFillTint = if (emerald) HefimerColors.EmeraldBg.copy(alpha = 0.25f) else null,
                elevation = 8.dp,
            )
            .padding(horizontal = 10.dp, vertical = 6.dp),
    ) {
        Box(
            modifier = Modifier
                .size(5.dp)
                .clip(CircleShape)
                .background(if (emerald) HefimerColors.Emerald400 else HefimerColors.White70),
        )
        Text(
            text = label,
            color = if (emerald) HefimerColors.Emerald400 else HefimerColors.White85,
            fontFamily = JetBrainsMonoFamily,
            fontSize = 9.5.sp,
            fontWeight = FontWeight.SemiBold,
        )
    }
}

// =========================================================================
// PAGE 3 HERO: Spatial Canvas + Rooms + Link Live Apple Glass Preview
// =========================================================================
@Composable
private fun SpatialCanvasHero(reduceMotion: Boolean) {
    val inf = rememberInfiniteTransition(label = "onb_space_float")
    val driftA by inf.animateFloat(
        initialValue = -4f,
        targetValue = 4f,
        animationSpec = infiniteRepeatable(tween(2900, easing = AppMotion.AppleEase), RepeatMode.Reverse),
        label = "drift_a",
    )
    val driftB by inf.animateFloat(
        initialValue = 4f,
        targetValue = -4f,
        animationSpec = infiniteRepeatable(tween(3400, easing = AppMotion.AppleEase), RepeatMode.Reverse),
        label = "drift_b",
    )

    val gridColor = HefimerColors.White12

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(264.dp)
            .appleGlassSurface(
                shape = RoundedCornerShape(36.dp),
                variant = AppleGlassVariant.Regular,
                elevation = 12.dp,
            ),
    ) {
        // Architectural dot grid
        Canvas(modifier = Modifier.fillMaxSize()) {
            val step = 22.dp.toPx()
            var x = step
            while (x < size.width) {
                var y = step
                while (y < size.height) {
                    drawCircle(color = gridColor, radius = 1.2.dp.toPx(), center = Offset(x, y))
                    y += step
                }
                x += step
            }
        }

        // Live sync pill top-left (monochrome glass)
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(14.dp)
                .appleGlassSurface(
                    shape = CircleShape,
                    variant = AppleGlassVariant.Clear,
                    elevation = 0.dp,
                )
                .padding(horizontal = 10.dp, vertical = 5.dp),
        ) {
            Box(Modifier.size(6.dp).clip(CircleShape).background(HefimerColors.White))
            Text(
                text = tr("REAL-TIME SYNC", "ĐỒNG BỘ TRỰC TIẾP"),
                color = HefimerColors.White85,
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.4.sp,
            )
        }

        // Floating Glass Code Snippet Card (Dracula colors)
        Column(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(top = 48.dp, end = 16.dp)
                .width(164.dp)
                .graphicsLayer { if (!reduceMotion) translationY = driftA }
                .appleGlassSurface(
                    shape = RoundedCornerShape(22.dp),
                    variant = AppleGlassVariant.Elevated,
                    elevation = 14.dp,
                )
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(5.dp),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(5.dp),
            ) {
                Icon(Icons.Rounded.Code, null, tint = HefimerColors.SyntaxKeyword, modifier = Modifier.size(12.dp))
                Text(tr("snippet.kt", "doan_ma.kt"), color = HefimerColors.White70, fontFamily = JetBrainsMonoFamily, fontSize = 9.sp)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                Box(Modifier.width(22.dp).height(4.dp).clip(CircleShape).background(HefimerColors.SyntaxKeyword))
                Box(Modifier.width(44.dp).height(4.dp).clip(CircleShape).background(HefimerColors.SyntaxFunction))
                Box(Modifier.width(26.dp).height(4.dp).clip(CircleShape).background(HefimerColors.SyntaxType))
            }
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                Spacer(Modifier.width(10.dp))
                Box(Modifier.width(34.dp).height(4.dp).clip(CircleShape).background(HefimerColors.SyntaxString))
                Box(Modifier.width(20.dp).height(4.dp).clip(CircleShape).background(HefimerColors.SyntaxNumber))
            }
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                Spacer(Modifier.width(10.dp))
                Box(Modifier.width(56.dp).height(4.dp).clip(CircleShape).background(HefimerColors.SyntaxComment))
            }
        }

        // Floating Glass Chat Room Card
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(9.dp),
            modifier = Modifier
                .align(Alignment.CenterStart)
                .padding(start = 16.dp, top = 26.dp)
                .graphicsLayer { if (!reduceMotion) translationY = driftB }
                .appleGlassSurface(
                    shape = RoundedCornerShape(22.dp),
                    variant = AppleGlassVariant.Elevated,
                    elevation = 14.dp,
                )
                .padding(horizontal = 12.dp, vertical = 10.dp),
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(28.dp)
                    .appleGlassSurface(
                        shape = CircleShape,
                        variant = AppleGlassVariant.Clear,
                        elevation = 0.dp,
                    ),
            ) {
                Icon(Icons.Rounded.Forum, null, tint = HefimerColors.White, modifier = Modifier.size(14.dp))
            }
            Column {
                Text(tr("Room #84210", "Phòng #84210"), color = HefimerColors.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                Text(tr("Ready to ship? ✨", "Sẵn sàng gửi chưa? ✨"), color = HefimerColors.White55, fontSize = 10.sp)
            }
        }

        // Floating Glass Device Link Pill bottom-right
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(7.dp),
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp)
                .graphicsLayer { if (!reduceMotion) translationY = driftA * 0.7f }
                .appleGlassSurface(
                    shape = CircleShape,
                    variant = AppleGlassVariant.Elevated,
                    elevation = 12.dp,
                )
                .padding(horizontal = 12.dp, vertical = 8.dp),
        ) {
            Icon(Icons.Rounded.Devices, null, tint = HefimerColors.White85, modifier = Modifier.size(14.dp))
            Text(tr("MacBook Pro linked", "Đã kết nối MacBook"), color = HefimerColors.White85, fontSize = 10.sp, fontWeight = FontWeight.SemiBold)
            Icon(Icons.Rounded.Check, null, tint = HefimerColors.White, modifier = Modifier.size(13.dp))
        }
    }
}

// =========================================================================
// PAGE 4 HERO: Interactive Live Appearance & Language Apple Glass Card
// =========================================================================
@Composable
private fun PersonalizeThemeHero(
    isLightMode: Boolean,
    appLanguage: AppLanguage,
    defaultProviderName: String,
    onSetLightMode: (Boolean) -> Unit,
    onSetAppLanguage: (AppLanguage) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .appleGlassSurface(
                shape = RoundedCornerShape(36.dp),
                variant = AppleGlassVariant.Elevated,
                elevation = 18.dp,
            )
            .padding(18.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = tr("APPEARANCE", "GIAO DIỆN"),
                color = HefimerColors.White45,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 2.sp,
            )
            Text(
                text = if (isLightMode) tr("Apple Light", "Sáng tinh gọn") else tr("OLED Dark", "Tối OLED"),
                color = HefimerColors.White75,
                fontFamily = JetBrainsMonoFamily,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
            )
        }

        AppleSegmentedControl(
            options = listOf(false, true),
            selected = isLightMode,
            label = { if (it) tr("Light", "Sáng") else tr("Dark", "Tối") },
            accentColor = HefimerColors.White,
            selectedTextColor = HefimerColors.Black,
            height = 40.dp,
            onSelect = onSetLightMode,
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = tr("LANGUAGE", "NGÔN NGỮ"),
                color = HefimerColors.White45,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 2.sp,
            )
            Text(
                text = when (appLanguage) {
                    AppLanguage.AUTO -> tr("Region Auto", "Tự nhận vùng")
                    AppLanguage.VI -> "Tiếng Việt"
                    AppLanguage.EN -> "English"
                },
                color = HefimerColors.White75,
                fontFamily = JetBrainsMonoFamily,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
            )
        }

        AppleSegmentedControl(
            options = listOf(AppLanguage.AUTO, AppLanguage.VI, AppLanguage.EN),
            selected = appLanguage,
            label = {
                when (it) {
                    AppLanguage.AUTO -> tr("Auto", "Tự động")
                    AppLanguage.VI -> "Tiếng Việt"
                    AppLanguage.EN -> "English"
                }
            },
            accentColor = HefimerColors.White,
            selectedTextColor = HefimerColors.Black,
            height = 40.dp,
            onSelect = onSetAppLanguage,
        )

        Box(Modifier.fillMaxWidth().height(0.5.dp).background(HefimerColors.White10))

        ReadyCheckRow(
            icon = Icons.Rounded.CloudUpload,
            title = tr("Default Host: $defaultProviderName", "Máy chủ mặc định: $defaultProviderName"),
            subtitle = tr("12 providers ready + smart auto-routing", "12 máy chủ sẵn sàng + tự chuyển tuyến"),
        )
    }
}

@Composable
private fun ReadyCheckRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(34.dp)
                .appleGlassSurface(
                    shape = RoundedCornerShape(13.dp),
                    variant = AppleGlassVariant.Clear,
                    elevation = 0.dp,
                ),
        ) {
            Icon(icon, null, tint = HefimerColors.White, modifier = Modifier.size(16.dp))
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(title, color = HefimerColors.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
            Text(subtitle, color = HefimerColors.White45, fontSize = 11.sp)
        }
        Icon(Icons.Rounded.Check, null, tint = HefimerColors.White85, modifier = Modifier.size(16.dp))
    }
}
