package com.hefimer.app.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Casino
import androidx.compose.material.icons.rounded.CenterFocusStrong
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Code
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Description
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Grid4x4
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.Inventory2
import androidx.compose.material.icons.rounded.Link
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.OpenInNew
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.QrCode2
import androidx.compose.material.icons.rounded.QrCodeScanner
import androidx.compose.material.icons.rounded.Remove
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material.icons.rounded.StickyNote2
import androidx.compose.material.icons.rounded.UploadFile
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import kotlinx.coroutines.launch
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import com.hefimer.app.data.model.DropFileEntry
import com.hefimer.app.data.model.SpaceItem
import com.hefimer.app.data.model.ToastType
import com.hefimer.app.data.model.formatBytes
import com.hefimer.app.data.model.formatCountdown
import com.hefimer.app.data.network.DownloadHelper
import com.hefimer.app.data.network.HefimerApiConfig
import com.hefimer.app.data.network.UploadProviders
import com.hefimer.app.ui.AppOverlay
import com.hefimer.app.ui.HefimerViewModel
import com.hefimer.app.ui.ScanTarget
import com.hefimer.app.ui.components.CodeHighlighter
import com.hefimer.app.ui.components.DropResultCard
import com.hefimer.app.ui.components.HefimerCollapsibleScaffold
import com.hefimer.app.ui.components.HefimerHeaderIconButton
import com.hefimer.app.ui.components.HefimerPrimaryButton
import com.hefimer.app.ui.components.HefimerSecondaryButton
import com.hefimer.app.ui.components.QrShareModal
import com.hefimer.app.ui.components.copyToClipboard
import com.hefimer.app.ui.theme.AppMotion
import com.hefimer.app.ui.theme.AppleFluidProgressTrack
import com.hefimer.app.ui.theme.AppleGlassVariant
import com.hefimer.app.ui.theme.AppleRollingPercentage
import com.hefimer.app.ui.theme.AppleSegmentedControl
import com.hefimer.app.ui.theme.AppleSwipeableSheet
import com.hefimer.app.ui.theme.HefimerColors
import com.hefimer.app.ui.theme.JetBrainsMonoFamily
import com.hefimer.app.ui.theme.LocalHeroBounds
import com.hefimer.app.ui.theme.UbuntuFontFamily
import com.hefimer.app.ui.theme.appleEntrance
import com.hefimer.app.ui.theme.appleGlassSurface
import com.hefimer.app.ui.theme.applePressable
import com.hefimer.app.ui.theme.heroSource
import com.hefimer.app.ui.theme.rememberReduceMotion
import com.hefimer.app.ui.theme.tr
import kotlinx.coroutines.delay
import kotlin.math.roundToInt

private fun localizedSpaceKind(kind: String): String = when (kind) {
    "file" -> tr("FILE", "TỆP")
    "note" -> tr("NOTE", "GHI CHÚ")
    "link" -> tr("LINK", "LIÊN KẾT")
    "code" -> tr("CODE", "MÃ")
    else -> kind.uppercase()
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SpaceScreen(vm: HefimerViewModel) {
    val state by vm.spaceState.collectAsState()
    val heroBounds = LocalHeroBounds.current
    val reduceMotion = rememberReduceMotion()

    HefimerCollapsibleScaffold(
        kicker = tr("Live collaborative canvas", "Bảng cộng tác trực tiếp"),
        title = "Space",
        subtitle = tr(
            "Drop files, sticky notes, links, and code onto an infinite shared board.",
            "Ghim tệp, ghi chú, liên kết và code lên không gian bảng vô hạn.",
        ),
        showOrbitMark = true,
        headerActions = {
            HefimerHeaderIconButton(
                icon = Icons.Rounded.QrCodeScanner,
                contentDescription = tr("Scan Space QR", "Quét QR Space"),
                onClick = { vm.openOverlay(AppOverlay.QrScanner(ScanTarget.AUTO)) },
            )
            HefimerHeaderIconButton(
                icon = Icons.Rounded.History,
                contentDescription = tr("History", "Lịch sử"),
                modifier = Modifier.heroSource("btn_history_space", heroBounds),
                onClick = { vm.openOverlay(AppOverlay.History, "btn_history_space") },
            )
        },
    ) {
        if (state.createdCode.isNotBlank()) {
            DropResultCard(
                code = state.createdCode,
                heading = tr("Your Space Code", "Mã Space của bạn"),
                subtitle = state.createdName,
                expiresAt = state.createdExpiresAt,
                shareUrl = "${HefimerApiConfig.SITE_ORIGIN}/space?code=${state.createdCode}",
                resetLabel = tr("New Space", "Space mới"),
                primaryCtaLabel = tr("Enter Space", "Vào Space"),
                onPrimaryCta = { vm.enterCreatedSpace() },
                onBack = { vm.clearSpaceCreatedCard() },
                onReset = { vm.clearSpaceCreatedCard() },
                onToast = { msg, err -> vm.showToast(msg, if (err) ToastType.ERROR else ToastType.SUCCESS) },
            )
            return@HefimerCollapsibleScaffold
        }

        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            // Display Name Card
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .appleEntrance(0)
                    .appleGlassSurface(
                        shape = RoundedCornerShape(32.dp),
                        variant = AppleGlassVariant.Regular,
                        elevation = 10.dp,
                    )
                    .padding(18.dp),
            ) {
                Text(
                    text = tr("YOUR DISPLAY NAME", "TÊN HIỂN THỊ CỦA BẠN"),
                    color = HefimerColors.White40,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 2.sp,
                )
                HefimerTextField(
                    value = state.userName,
                    onValueChange = { vm.setSpaceUserName(it) },
                    placeholder = tr("Enter your name...", "Nhập tên của bạn..."),
                    leadingIcon = Icons.Rounded.Person,
                )
            }

            // Join / Create mode switch
            AppleSegmentedControl(
                options = listOf("join", "create"),
                selected = state.lobbyMode,
                label = { if (it == "join") tr("Join Space", "Tham gia Space") else tr("Create Space", "Tạo Space") },
                onSelect = { vm.setSpaceLobbyMode(it) },
                modifier = Modifier.appleEntrance(1),
            )

            AnimatedContent(
                targetState = state.lobbyMode,
                transitionSpec = { AppMotion.sectionTransform(reduceMotion) },
                label = "space_lobby_mode",
            ) { mode ->
                if (mode == "join") {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(14.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .appleGlassSurface(
                                shape = RoundedCornerShape(34.dp),
                                variant = AppleGlassVariant.Regular,
                                elevation = 12.dp,
                            )
                            .padding(20.dp),
                    ) {
                        Text(
                            text = tr("5-DIGIT SPACE CODE", "MÃ SPACE 5 CHỮ SỐ"),
                            color = HefimerColors.White40,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 2.sp,
                        )
                        FiveDigitPinSlots(
                            code = state.joinCode,
                            onCodeChange = { vm.setSpaceJoinCode(it) },
                            onDone = { vm.joinSpace() },
                        )
                        if (state.joinRequiresPassword) {
                            HefimerTextField(
                                value = state.joinPassword,
                                onValueChange = { vm.setSpaceJoinPassword(it) },
                                placeholder = tr("Enter space passcode...", "Nhập mật khẩu Space..."),
                                leadingIcon = Icons.Rounded.Lock,
                                isPassword = true,
                            )
                        }
                        HefimerPrimaryButton(
                            text = if (state.loading) tr("Joining...", "Đang vào...") else tr("Join Space", "Vào Space"),
                            loading = state.loading,
                            enabled = state.joinCode.length == 5 && !state.loading,
                            onClick = { vm.joinSpace() },
                        )
                    }
                } else {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(14.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .appleGlassSurface(
                                shape = RoundedCornerShape(34.dp),
                                variant = AppleGlassVariant.Regular,
                                elevation = 12.dp,
                            )
                            .padding(20.dp),
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(tr("SPACE TITLE", "TÊN SPACE"), color = HefimerColors.White40, fontSize = 9.sp, fontWeight = FontWeight.Bold, letterSpacing = 2.sp)
                            HefimerTextField(
                                value = state.createTitle,
                                onValueChange = { vm.setSpaceCreateTitle(it) },
                                placeholder = tr("Design review, sprint notes...", "Góp ý thiết kế, ghi chú dự án..."),
                            )
                        }

                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Text(tr("CUSTOM 5-DIGIT CODE", "MÃ 5 CHỮ SỐ TÙY CHỈNH"), color = HefimerColors.White40, fontSize = 9.sp, fontWeight = FontWeight.Bold, letterSpacing = 2.sp)
                                val (statusText, statusColor) = when (state.createCodeStatus) {
                                    "checking" -> tr("Checking...", "Đang kiểm tra...") to HefimerColors.White45
                                    "available" -> tr("Available", "Khả dụng") to HefimerColors.White85
                                    "taken" -> tr("Already in use", "Đã được sử dụng") to HefimerColors.Red400
                                    else -> "" to HefimerColors.White30
                                }
                                if (statusText.isNotEmpty()) {
                                    Text(statusText, color = statusColor, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                HefimerTextField(
                                    value = state.createCode,
                                    onValueChange = { vm.setSpaceCreateCode(it) },
                                    placeholder = "12345",
                                    mono = true,
                                    modifier = Modifier.weight(1f),
                                )
                                Box(
                                    contentAlignment = Alignment.Center,
                                    modifier = Modifier
                                        .size(48.dp)
                                        .appleGlassSurface(
                                            shape = RoundedCornerShape(16.dp),
                                            variant = AppleGlassVariant.Clear,
                                            elevation = 0.dp,
                                        )
                                        .applePressable(pressedScale = 0.90f) { vm.randomizeSpaceCode() },
                                ) {
                                    Icon(Icons.Rounded.Casino, tr("Randomize", "Ngẫu nhiên"), tint = HefimerColors.White, modifier = Modifier.size(20.dp))
                                }
                            }
                        }

                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(tr("DURATION", "THỜI HẠN"), color = HefimerColors.White40, fontSize = 9.sp, fontWeight = FontWeight.Bold, letterSpacing = 2.sp)
                            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                listOf(
                                    "30m" to tr("30 min", "30 phút"),
                                    "1h" to tr("1 hour", "1 giờ"),
                                    "6h" to tr("6 hours", "6 giờ"),
                                    "12h" to tr("12 hours", "12 giờ"),
                                    "24h" to tr("24 hours", "24 giờ"),
                                ).forEach { (v, lbl) ->
                                    val sel = state.createExpire == v
                                    val fg by animateColorAsState(
                                        targetValue = if (sel) HefimerColors.Black else HefimerColors.White60,
                                        animationSpec = tween(AppMotion.Micro, easing = AppMotion.AppleSilk),
                                        label = "sp_dur_fg",
                                    )
                                    val chipMod = if (sel) {
                                        Modifier.clip(CircleShape).background(HefimerColors.White)
                                    } else {
                                        Modifier.appleGlassSurface(CircleShape, AppleGlassVariant.Clear, elevation = 0.dp)
                                    }
                                    Text(
                                        text = lbl,
                                        color = fg,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = chipMod
                                            .applePressable(pressedScale = 0.94f) { vm.setSpaceCreateExpire(v) }
                                            .padding(horizontal = 14.dp, vertical = 8.dp),
                                    )
                                }
                            }
                        }

                        HefimerTextField(
                            value = state.createPassword,
                            onValueChange = { vm.setSpaceCreatePassword(it) },
                            placeholder = tr("Passcode (optional)", "Mật mã (tùy chọn)"),
                            leadingIcon = Icons.Rounded.Lock,
                        )

                        HefimerPrimaryButton(
                            text = if (state.loading) tr("Creating...", "Đang tạo...") else tr("Create Space", "Tạo Space"),
                            loading = state.loading,
                            enabled = !state.loading && state.createCodeStatus != "taken",
                            onClick = { vm.createSpace() },
                        )
                    }
                }
            }

            // Recent Spaces quick-rejoin
            if (state.recentSpaces.isNotEmpty()) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.appleEntrance(2)) {
                    Text(
                        text = tr("RECENT SPACES", "SPACE GẦN ĐÂY"),
                        color = HefimerColors.White40,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 2.2.sp,
                    )
                    state.recentSpaces.forEach { recent ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier
                                .fillMaxWidth()
                                .appleGlassSurface(
                                    shape = RoundedCornerShape(24.dp),
                                    variant = AppleGlassVariant.Regular,
                                    elevation = 6.dp,
                                )
                                .applePressable(pressedScale = 0.98f) { vm.joinSpace(recent.code) }
                                .padding(horizontal = 16.dp, vertical = 14.dp),
                        ) {
                            Column {
                                Text(recent.name, color = HefimerColors.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                                Text("#${recent.code}", color = HefimerColors.White45, fontFamily = JetBrainsMonoFamily, fontSize = 11.sp)
                            }
                            Text(
                                text = tr("Rejoin", "Vào lại"),
                                color = HefimerColors.Black,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier
                                    .clip(CircleShape)
                                    .background(HefimerColors.White)
                                    .padding(horizontal = 12.dp, vertical = 6.dp),
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Full-screen interactive collaborative infinite canvas overlay for Hefimer Space.
 */
@Composable
fun SpaceCanvasOverlay(
    vm: HefimerViewModel,
    onClose: () -> Unit,
) {
    val state by vm.spaceState.collectAsState()
    val context = LocalContext.current
    val density = LocalDensity.current
    val scope = rememberCoroutineScope()
    val statusTop = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    val navBottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()

    val panX = remember { Animatable(0f) }
    val panY = remember { Animatable(0f) }
    val zoom = remember { Animatable(1f) }
    var snapGrid by remember { mutableStateOf(true) }
    var showQr by remember { mutableStateOf(false) }
    var showProviderSheet by remember { mutableStateOf(false) }
    var composerMode by remember { mutableStateOf<String?>(null) } // "note", "link", "code"
    var editingItem by remember { mutableStateOf<SpaceItem?>(null) }
    var previewItem by remember { mutableStateOf<SpaceItem?>(null) }
    var countdown by remember(state.activeExpiresAt) { mutableStateOf(formatCountdown(state.activeExpiresAt)) }

    LaunchedEffect(state.activeExpiresAt) {
        if (state.activeExpiresAt <= 0L) return@LaunchedEffect
        while (true) {
            countdown = formatCountdown(state.activeExpiresAt)
            if (System.currentTimeMillis() >= state.activeExpiresAt) {
                vm.showToast(tr("Space has expired", "Space đã hết hạn"), ToastType.ERROR)
                onClose()
                break
            }
            delay(1000L)
        }
    }

    BoxWithConstraints(
        modifier = Modifier.fillMaxSize(),
    ) {
        val viewportW = with(density) { maxWidth.toPx() }.coerceAtLeast(1f)
        val viewportH = with(density) { maxHeight.toPx() }.coerceAtLeast(1f)

        fun viewportCenterWorld(offsetIndex: Int = 0): Pair<Float, Float> {
            val z = zoom.value.coerceAtLeast(0.20f)
            val cardW = with(density) { 248.dp.toPx() }
            val wx = ((viewportW / 2f) - panX.value) / z - (cardW / 2f) + (offsetIndex * 28f)
            val wy = ((viewportH / 2f) - panY.value) / z - 90f + (offsetIndex * 28f)
            return wx to wy
        }

        fun animateCameraTo(targetPanX: Float, targetPanY: Float, targetZoom: Float) {
            scope.launch { panX.animateTo(targetPanX, AppMotion.navigationSpring()) }
            scope.launch { panY.animateTo(targetPanY, AppMotion.navigationSpring()) }
            scope.launch { zoom.animateTo(targetZoom.coerceIn(0.20f, 3.0f), AppMotion.navigationSpring()) }
        }

        fun recenterCanvas() {
            val items = state.items
            if (items.isEmpty()) {
                animateCameraTo(0f, 0f, 1f)
                return
            }
            val cardW = with(density) { 248.dp.toPx() }
            val cardH = with(density) { 180.dp.toPx() }
            val minX = items.minOf { it.x }
            val maxX = items.maxOf { it.x + cardW }
            val minY = items.minOf { it.y }
            val maxY = items.maxOf { it.y + cardH }
            val contentW = (maxX - minX).coerceAtLeast(cardW)
            val contentH = (maxY - minY).coerceAtLeast(cardH)
            val fitZoom = minOf(
                (viewportW * 0.82f) / contentW,
                (viewportH * 0.62f) / contentH,
            ).coerceIn(0.30f, 1.15f)
            val centerX = (minX + maxX) / 2f
            val centerY = (minY + maxY) / 2f
            animateCameraTo(
                targetPanX = viewportW / 2f - centerX * fitZoom,
                targetPanY = viewportH / 2f - centerY * fitZoom,
                targetZoom = fitZoom,
            )
        }

        fun zoomByFactor(factor: Float) {
            val curZoom = zoom.value
            val targetZoom = (curZoom * factor).coerceIn(0.20f, 3.0f)
            val ratio = targetZoom / curZoom
            val cx = viewportW / 2f
            val cy = viewportH / 2f
            animateCameraTo(
                targetPanX = cx - (cx - panX.value) * ratio,
                targetPanY = cy - (cy - panY.value) * ratio,
                targetZoom = targetZoom,
            )
        }

        val filePicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenMultipleDocuments()) { uris ->
            if (uris.isNotEmpty()) {
                val (wx, wy) = viewportCenterWorld()
                vm.spaceUploadFiles(uris, wx, wy)
            }
        }

        // Gesture layer for infinite pan + focal-point pinch zoom
        Box(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(Unit) {
                    detectTransformGestures { centroid, pan, gestureZoom, _ ->
                        val curZoom = zoom.value
                        val newZoom = (curZoom * gestureZoom).coerceIn(0.20f, 3.0f)
                        val ratio = newZoom / curZoom
                        val nextPanX = centroid.x - (centroid.x - panX.value) * ratio + pan.x
                        val nextPanY = centroid.y - (centroid.y - panY.value) * ratio + pan.y
                        scope.launch {
                            zoom.snapTo(newZoom)
                            panX.snapTo(nextPanX)
                            panY.snapTo(nextPanY)
                        }
                    }
                },
        ) {
            // Large adaptive dual-scale infinite grid (minor dots + major grid lines + origin crosshair)
            Canvas(modifier = Modifier.fillMaxSize()) {
                val curZoom = zoom.value
                val curPanX = panX.value
                val curPanY = panY.value

                var worldStep = 36.dp.toPx()
                while (worldStep * curZoom < 22f) worldStep *= 2f
                while (worldStep * curZoom > 96f) worldStep /= 2f

                val minorStep = worldStep * curZoom
                val majorStep = minorStep * 4f

                val majorStartX = ((curPanX % majorStep) + majorStep) % majorStep
                val majorStartY = ((curPanY % majorStep) + majorStep) % majorStep
                val majorLineColor = HefimerColors.White.copy(alpha = 0.06f)
                var mx = majorStartX
                while (mx < size.width) {
                    drawLine(
                        color = majorLineColor,
                        start = Offset(mx, 0f),
                        end = Offset(mx, size.height),
                        strokeWidth = 1f,
                    )
                    mx += majorStep
                }
                var my = majorStartY
                while (my < size.height) {
                    drawLine(
                        color = majorLineColor,
                        start = Offset(0f, my),
                        end = Offset(size.width, my),
                        strokeWidth = 1f,
                    )
                    my += majorStep
                }

                val ox = ((curPanX % minorStep) + minorStep) % minorStep
                val oy = ((curPanY % minorStep) + minorStep) % minorStep
                val dotRadius = (1.25.dp.toPx() * curZoom.coerceIn(0.65f, 1.6f)).coerceIn(1.0f, 2.6f)
                val dotColor = HefimerColors.White.copy(alpha = 0.16f)
                var x = ox
                while (x < size.width) {
                    var y = oy
                    while (y < size.height) {
                        drawCircle(
                            color = dotColor,
                            radius = dotRadius,
                            center = Offset(x, y),
                        )
                        y += minorStep
                    }
                    x += minorStep
                }

                if (curPanX in -40f..(size.width + 40f) && curPanY in -40f..(size.height + 40f)) {
                    val arm = 14.dp.toPx()
                    drawLine(
                        color = HefimerColors.White.copy(alpha = 0.24f),
                        start = Offset(curPanX - arm, curPanY),
                        end = Offset(curPanX + arm, curPanY),
                        strokeWidth = 1.5f,
                    )
                    drawLine(
                        color = HefimerColors.White.copy(alpha = 0.24f),
                        start = Offset(curPanX, curPanY - arm),
                        end = Offset(curPanX, curPanY + arm),
                        strokeWidth = 1.5f,
                    )
                }
            }

            // World items positioned directly in screen space (unclipped, infinite hit-testing)
            state.items.forEach { item ->
                key(item.id) {
                    SpaceCanvasCard(
                        item = item,
                        panX = panX.value,
                        panY = panY.value,
                        zoom = zoom.value,
                        snapGrid = snapGrid,
                        onDragFinish = { nx, ny ->
                            val nextZ = (state.items.maxOfOrNull { it.zIndex } ?: 1) + 1
                            vm.spaceMoveItem(item, nx, ny, nextZ)
                        },
                        onTap = { previewItem = item },
                        onEdit = {
                            if (item.kind != "file") {
                                editingItem = item
                                composerMode = item.kind
                            } else {
                                previewItem = item
                            }
                        },
                        onCopy = {
                            val copyTarget = item.content?.takeIf { it.isNotBlank() } ?: item.url ?: item.name
                            copyToClipboard(context, item.name, copyTarget)
                            vm.showToast(tr("Copied ${item.name}", "Đã sao chép ${item.name}"), ToastType.SUCCESS)
                        },
                        onDownload = {
                            item.url?.let { url ->
                                vm.downloadDropFile(DropFileEntry(fileName = item.name, fileUrl = url, fileSize = item.size ?: 0L))
                            }
                        },
                        onOpenLink = { item.url?.let { DownloadHelper.openExternalUrl(context, it) } },
                        onDelete = { vm.spaceDeleteItem(item) },
                    )
                }
            }

            // Live remote cursors in screen space
            state.cursors.forEach { cursor ->
                val col = runCatching { Color(android.graphics.Color.parseColor(cursor.color)) }
                    .getOrDefault(HefimerColors.Emerald400)
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier
                        .graphicsLayer {
                            translationX = cursor.x * zoom.value + panX.value
                            translationY = cursor.y * zoom.value + panY.value
                        }
                        .clip(CircleShape)
                        .background(col)
                        .padding(horizontal = 8.dp, vertical = 3.dp),
                ) {
                    Text(cursor.name, color = HefimerColors.Black, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        // Empty hint
        if (state.items.isEmpty()) {
            Column(
                modifier = Modifier
                    .align(Alignment.Center)
                    .padding(horizontal = 32.dp)
                    .appleGlassSurface(
                        shape = RoundedCornerShape(30.dp),
                        variant = AppleGlassVariant.Regular,
                        elevation = 10.dp,
                    )
                    .padding(horizontal = 24.dp, vertical = 20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Text(tr("Infinite Canvas", "Bảng vẽ vô tận"), color = HefimerColors.White85, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                Text(
                    text = tr(
                        "Pinch to zoom, drag to explore, or drop files, notes, links & code below.",
                        "Chụm để thu phóng, kéo để di chuyển hoặc thêm tệp, ghi chú, liên kết & mã bên dưới.",
                    ),
                    color = HefimerColors.White50,
                    fontSize = 12.sp,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                )
            }
        }

        // Top Floating Apple Glass Space HUD Capsule
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth()
                .padding(top = statusTop + 8.dp, start = 14.dp, end = 14.dp)
                .appleGlassSurface(
                    shape = RoundedCornerShape(28.dp),
                    variant = AppleGlassVariant.Elevated,
                    elevation = 16.dp,
                )
                .padding(horizontal = 10.dp, vertical = 8.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.weight(1f)) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(36.dp)
                        .appleGlassSurface(
                            shape = CircleShape,
                            variant = AppleGlassVariant.Clear,
                            elevation = 0.dp,
                        )
                        .applePressable(pressedScale = 0.89f, onClick = onClose),
                ) {
                    Icon(Icons.AutoMirrored.Rounded.ArrowBack, tr("Leave", "Rời đi"), tint = HefimerColors.White, modifier = Modifier.size(18.dp))
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = state.activeSpaceName.ifBlank { "Space ${state.activeSpaceId}" },
                        color = HefimerColors.White,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "#${state.activeSpaceId}",
                            color = HefimerColors.White80,
                            fontFamily = JetBrainsMonoFamily,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                        )
                        Text("• ${state.members.size.coerceAtLeast(1)} ${tr("online", "trực tuyến")}", color = HefimerColors.White50, fontSize = 10.sp)
                        if (countdown.isNotBlank()) {
                            Text("• $countdown", color = HefimerColors.White45, fontSize = 10.sp)
                        }
                    }
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                SpaceMiniTool(Icons.Rounded.Grid4x4, active = snapGrid) {
                    snapGrid = !snapGrid
                    vm.showToast(if (snapGrid) tr("Snap to grid ON", "Bật bám lưới") else tr("Free positioning ON", "Bật đặt tự do"), ToastType.INFO)
                }
                SpaceMiniTool(Icons.Rounded.AutoAwesome, active = false) {
                    vm.spaceAutoArrange()
                    animateCameraTo(0f, 0f, 0.88f)
                }
                SpaceMiniTool(Icons.Rounded.CenterFocusStrong, active = false) {
                    recenterCanvas()
                }
                SpaceMiniTool(Icons.Rounded.QrCode2, active = false) { showQr = true }
            }
        }

        // Upload queue progress HUD (Elevated Apple Glass)
        if (state.uploadTasks.isNotEmpty()) {
            val task = state.uploadTasks.first()
            Column(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = statusTop + 78.dp, start = 18.dp, end = 18.dp)
                    .fillMaxWidth()
                    .appleGlassSurface(
                        shape = RoundedCornerShape(28.dp),
                        variant = AppleGlassVariant.Elevated,
                        elevation = 18.dp,
                    )
                    .padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(task.fileName, color = HefimerColors.White, fontSize = 12.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text(task.status, color = HefimerColors.White50, fontSize = 10.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        AppleRollingPercentage(
                            percent = task.progress,
                            color = HefimerColors.White,
                            style = TextStyle(
                                fontFamily = UbuntuFontFamily,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                            ),
                        )
                        Icon(
                            Icons.Rounded.Close,
                            tr("Cancel", "Hủy"),
                            tint = HefimerColors.Red400,
                            modifier = Modifier
                                .size(20.dp)
                                .applePressable(pressedScale = 0.88f) { vm.cancelSpaceUpload() },
                        )
                    }
                }
                AppleFluidProgressTrack(
                    progress = (task.progress / 100f).coerceIn(0f, 1f),
                    accentColor = HefimerColors.White,
                    trackColor = HefimerColors.White10,
                    height = 5.dp,
                )
            }
        }

        // Floating Zoom Controls Pill (above bottom creation dock)
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(bottom = navBottom + 86.dp, end = 16.dp)
                .appleGlassSurface(
                    shape = CircleShape,
                    variant = AppleGlassVariant.Elevated,
                    elevation = 16.dp,
                )
                .padding(horizontal = 6.dp, vertical = 4.dp),
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(30.dp)
                    .clip(CircleShape)
                    .applePressable(pressedScale = 0.88f) { zoomByFactor(0.78f) },
            ) {
                Icon(Icons.Rounded.Remove, tr("Zoom out", "Thu nhỏ"), tint = HefimerColors.White75, modifier = Modifier.size(16.dp))
            }
            Text(
                text = "${(zoom.value * 100f).roundToInt()}%",
                color = HefimerColors.White85,
                style = TextStyle(
                    fontFamily = JetBrainsMonoFamily,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    fontFeatureSettings = "tnum",
                ),
                modifier = Modifier
                    .clip(CircleShape)
                    .applePressable(pressedScale = 0.92f) { recenterCanvas() }
                    .padding(horizontal = 6.dp, vertical = 4.dp),
            )
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(30.dp)
                    .clip(CircleShape)
                    .applePressable(pressedScale = 0.88f) { zoomByFactor(1.28f) },
            ) {
                Icon(Icons.Rounded.Add, tr("Zoom in", "Phóng to"), tint = HefimerColors.White75, modifier = Modifier.size(16.dp))
            }
        }

        // Bottom floating canvas creation toolbar
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = navBottom + 14.dp, start = 16.dp, end = 16.dp)
                .appleGlassSurface(
                    shape = RoundedCornerShape(30.dp),
                    variant = AppleGlassVariant.Elevated,
                    elevation = 20.dp,
                )
                .padding(horizontal = 12.dp, vertical = 8.dp),
        ) {
            SpaceDockAction(Icons.Rounded.UploadFile, tr("File", "Tệp"), primary = true) { filePicker.launch(arrayOf("*/*")) }
            SpaceDockAction(Icons.Rounded.StickyNote2, tr("Note", "Ghi chú")) { editingItem = null; composerMode = "note" }
            SpaceDockAction(Icons.Rounded.Link, tr("Link", "Liên kết")) { editingItem = null; composerMode = "link" }
            SpaceDockAction(Icons.Rounded.Code, tr("Code", "Mã")) { editingItem = null; composerMode = "code" }
            SpaceDockAction(Icons.Rounded.Inventory2, UploadProviders.getConfig(state.selectedProviderId).name.take(7)) {
                showProviderSheet = true
            }
        }

        // Share QR Modal
        if (showQr) {
            QrShareModal(
                title = tr("Share Space", "Chia sẻ Space"),
                url = "${HefimerApiConfig.SITE_ORIGIN}/space?code=${state.activeSpaceId}",
                code = state.activeSpaceId,
                onDismiss = { showQr = false },
                onToast = { m, e -> vm.showToast(m, if (e) ToastType.ERROR else ToastType.SUCCESS) },
            )
        }

        // Provider Picker Sheet
        AppleSwipeableSheet(visible = showProviderSheet, onDismiss = { showProviderSheet = false }) { dismiss ->
            ProviderPickerSheetContent(
                selectedId = state.selectedProviderId,
                onSelect = {
                    vm.selectSpaceProvider(it)
                    dismiss()
                },
                onClose = dismiss,
            )
        }

        // Composer Modal for Note / Link / Code
        if (composerMode != null) {
            SpaceComposerDialog(
                mode = composerMode!!,
                editing = editingItem,
                onDismiss = { composerMode = null; editingItem = null },
                onSave = { name, content, url, color ->
                    val existing = editingItem
                    if (existing != null) {
                        vm.spaceUpdateItem(existing, name, content, url, color)
                    } else {
                        val (wx, wy) = viewportCenterWorld(state.items.size % 5)
                        vm.spaceAddItem(composerMode!!, name, content, url, color, wx, wy)
                    }
                    composerMode = null
                    editingItem = null
                },
            )
        }

        // Item detail preview modal
        previewItem?.let { item ->
            SpaceItemPreviewDialog(
                item = item,
                onDismiss = { previewItem = null },
                onEdit = {
                    if (item.kind != "file") {
                        editingItem = item
                        composerMode = item.kind
                    }
                    previewItem = null
                },
                onDelete = {
                    vm.spaceDeleteItem(item)
                    previewItem = null
                },
                onDownload = {
                    item.url?.let { u ->
                        vm.downloadDropFile(DropFileEntry(fileName = item.name, fileUrl = u, fileSize = item.size ?: 0L))
                    }
                },
                onCopy = { txt ->
                    copyToClipboard(context, item.name, txt)
                    vm.showToast(tr("Copied", "Đã sao chép"), ToastType.SUCCESS)
                },
                onOpenLink = { u -> DownloadHelper.openExternalUrl(context, u) },
            )
        }
    }
}

@Composable
private fun SpaceCanvasCard(
    item: SpaceItem,
    panX: Float,
    panY: Float,
    zoom: Float,
    snapGrid: Boolean,
    onDragFinish: (Float, Float) -> Unit,
    onTap: () -> Unit,
    onEdit: () -> Unit,
    onCopy: () -> Unit,
    onDownload: () -> Unit,
    onOpenLink: () -> Unit,
    onDelete: () -> Unit,
) {
    val haptic = LocalHapticFeedback.current
    val scope = rememberCoroutineScope()
    val currentSnap by rememberUpdatedState(snapGrid)
    val currentOnDragFinish by rememberUpdatedState(onDragFinish)

    val worldX = remember(item.id) { Animatable(item.x) }
    val worldY = remember(item.id) { Animatable(item.y) }
    var isDragging by remember(item.id) { mutableStateOf(false) }

    LaunchedEffect(item.x, item.y, isDragging) {
        if (!isDragging) {
            if (kotlin.math.abs(worldX.value - item.x) > 0.5f) {
                launch { worldX.animateTo(item.x, AppMotion.layoutSpring()) }
            }
            if (kotlin.math.abs(worldY.value - item.y) > 0.5f) {
                launch { worldY.animateTo(item.y, AppMotion.layoutSpring()) }
            }
        }
    }

    val liftScale by animateFloatAsState(
        targetValue = if (isDragging) 1.035f else 1f,
        animationSpec = if (isDragging) AppMotion.pressDownSpring() else AppMotion.fluidSpring(),
        label = "card_lift",
    )

    val (noteBg, noteBorder) = if (item.kind == "note") {
        when (item.noteColor) {
            "emerald" -> HefimerColors.NoteEmeraldBg to HefimerColors.Emerald400
            "amber" -> HefimerColors.NoteAmberBg to HefimerColors.Amber400
            "sky" -> HefimerColors.NoteSkyBg to HefimerColors.Sky400
            "rose" -> HefimerColors.NoteRoseBg to HefimerColors.Rose400
            "violet" -> HefimerColors.NoteVioletBg to HefimerColors.Violet400
            else -> HefimerColors.NoteZincBg to null
        }
    } else {
        null to null
    }

    val previewUrl = remember(item.url, item.mimeType) {
        if (item.kind == "file" && item.mimeType?.startsWith("image/") == true) {
            DownloadHelper.getMediaPreviewUrl(item.url, item.mimeType)
        } else null
    }

    Column(
        modifier = Modifier
            .zIndex(if (isDragging) 1000f else item.zIndex.toFloat())
            .graphicsLayer {
                translationX = worldX.value * zoom + panX
                translationY = worldY.value * zoom + panY
                scaleX = zoom * liftScale
                scaleY = zoom * liftScale
                transformOrigin = TransformOrigin(0f, 0f)
                shadowElevation = if (isDragging) 24f else 8f
            }
            .width(248.dp)
            .appleGlassSurface(
                shape = RoundedCornerShape(26.dp),
                variant = if (isDragging) AppleGlassVariant.Elevated else AppleGlassVariant.Regular,
                accentBorderTint = if (isDragging) HefimerColors.White else noteBorder,
                accentFillTint = noteBg,
                elevation = if (isDragging) 20.dp else 10.dp,
            )
            .pointerInput(item.id) {
                detectDragGestures(
                    onDragStart = {
                        isDragging = true
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    },
                    onDragEnd = {
                        val rawX = worldX.value
                        val rawY = worldY.value
                        val finalX = if (currentSnap) (rawX / 24f).roundToInt() * 24f else rawX
                        val finalY = if (currentSnap) (rawY / 24f).roundToInt() * 24f else rawY
                        isDragging = false
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        scope.launch { worldX.animateTo(finalX, AppMotion.layoutSpring()) }
                        scope.launch { worldY.animateTo(finalY, AppMotion.layoutSpring()) }
                        currentOnDragFinish(finalX, finalY)
                    },
                    onDragCancel = {
                        isDragging = false
                    },
                    onDrag = { change, dragAmount ->
                        change.consume()
                        scope.launch {
                            worldX.snapTo(worldX.value + dragAmount.x)
                            worldY.snapTo(worldY.value + dragAmount.y)
                        }
                    },
                )
            }
            .applePressable(pressedScale = 0.985f, enableHaptic = false, onClick = onTap)
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = localizedSpaceKind(item.kind),
                color = HefimerColors.White45,
                fontSize = 8.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.8.sp,
            )
            Text(
                text = item.uploadedBy,
                color = HefimerColors.White40,
                fontSize = 9.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }

        if (previewUrl != null) {
            AsyncImage(
                model = previewUrl,
                contentDescription = item.name,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(118.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(HefimerColors.Black),
            )
        }

        Text(
            text = item.name,
            color = HefimerColors.White,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )

        if (!item.content.isNullOrBlank()) {
            Text(
                text = item.content,
                color = HefimerColors.White75,
                fontFamily = if (item.kind == "code") JetBrainsMonoFamily else UbuntuFontFamily,
                fontSize = if (item.kind == "code") 11.sp else 12.sp,
                maxLines = 4,
                overflow = TextOverflow.Ellipsis,
            )
        } else if (item.kind == "link" && !item.url.isNullOrBlank()) {
            Text(
                text = item.url,
                color = HefimerColors.Sky400,
                fontFamily = JetBrainsMonoFamily,
                fontSize = 10.sp,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        } else if (item.kind == "file") {
            Text(
                text = "${formatBytes(item.size ?: 0L)} • ${item.providerName ?: tr("Host", "Máy chủ")}",
                color = HefimerColors.White45,
                fontFamily = JetBrainsMonoFamily,
                fontSize = 10.sp,
            )
        }

        // Generous 32.dp circular glass action buttons
        Row(
            modifier = Modifier.fillMaxWidth().padding(top = 2.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (item.kind == "file" && !item.url.isNullOrBlank()) {
                SpaceCardActionIcon(Icons.Rounded.Download, tr("Download", "Tải xuống"), HefimerColors.White85, onDownload)
            }
            if (item.kind == "link" && !item.url.isNullOrBlank()) {
                SpaceCardActionIcon(Icons.Rounded.OpenInNew, tr("Open", "Mở"), HefimerColors.Sky400, onOpenLink)
            }
            if (item.kind != "file") {
                SpaceCardActionIcon(Icons.Rounded.ContentCopy, tr("Copy", "Sao chép"), HefimerColors.White75, onCopy)
                SpaceCardActionIcon(Icons.Rounded.Edit, tr("Edit", "Sửa"), HefimerColors.White75, onEdit)
            }
            SpaceCardActionIcon(Icons.Rounded.DeleteOutline, tr("Delete", "Xóa"), HefimerColors.Red400, onDelete)
        }
    }
}

@Composable
private fun SpaceCardActionIcon(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    contentDescription: String,
    tint: Color,
    onClick: () -> Unit,
) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .size(32.dp)
            .appleGlassSurface(
                shape = CircleShape,
                variant = AppleGlassVariant.Clear,
                elevation = 0.dp,
            )
            .applePressable(pressedScale = 0.88f, onClick = onClick),
    ) {
        Icon(icon, contentDescription, tint = tint, modifier = Modifier.size(15.dp))
    }
}

@Composable
private fun SpaceMiniTool(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    active: Boolean,
    onClick: () -> Unit,
) {
    val bg by animateColorAsState(
        targetValue = if (active) HefimerColors.White else Color.Transparent,
        animationSpec = tween(AppMotion.Micro, easing = AppMotion.AppleSilk),
        label = "sp_tool_bg",
    )
    val tint by animateColorAsState(
        targetValue = if (active) HefimerColors.Black else HefimerColors.White85,
        animationSpec = tween(AppMotion.Micro, easing = AppMotion.AppleSilk),
        label = "sp_tool_tint",
    )
    val mod = if (active) {
        Modifier.size(34.dp).clip(CircleShape).background(bg)
    } else {
        Modifier.size(34.dp).appleGlassSurface(CircleShape, AppleGlassVariant.Clear, elevation = 0.dp)
    }
    Box(
        contentAlignment = Alignment.Center,
        modifier = mod.applePressable(pressedScale = 0.89f, onClick = onClick),
    ) {
        Icon(icon, null, tint = tint, modifier = Modifier.size(16.dp))
    }
}

@Composable
private fun SpaceDockAction(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    primary: Boolean = false,
    onClick: () -> Unit,
) {
    val mod = if (primary) {
        Modifier.clip(RoundedCornerShape(18.dp)).background(HefimerColors.White)
    } else {
        Modifier.appleGlassSurface(RoundedCornerShape(18.dp), AppleGlassVariant.Clear, elevation = 0.dp)
    }
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = mod
            .applePressable(pressedScale = 0.92f, onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 8.dp),
    ) {
        Icon(icon, null, tint = if (primary) HefimerColors.Black else HefimerColors.White85, modifier = Modifier.size(18.dp))
        Spacer(Modifier.height(2.dp))
        Text(
            text = label,
            color = if (primary) HefimerColors.Black else HefimerColors.White70,
            fontSize = 9.sp,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
        )
    }
}

@Composable
private fun SpaceComposerDialog(
    mode: String,
    editing: SpaceItem?,
    onDismiss: () -> Unit,
    onSave: (name: String, content: String?, url: String?, noteColor: String) -> Unit,
) {
    var name by remember { mutableStateOf(editing?.name.orEmpty()) }
    var content by remember { mutableStateOf(editing?.content.orEmpty()) }
    var url by remember { mutableStateOf(editing?.url.orEmpty()) }
    var color by remember { mutableStateOf(editing?.noteColor ?: "zinc") }

    val colors = listOf(
        "zinc" to Color(0xFFA1A1AA),
        "emerald" to HefimerColors.Emerald400,
        "amber" to HefimerColors.Amber400,
        "sky" to HefimerColors.Sky400,
        "rose" to HefimerColors.Rose400,
        "violet" to HefimerColors.Violet400,
    )

    val modalHeading = when (mode) {
        "note" -> if (editing != null) tr("EDIT NOTE", "CHỈNH SỬA GHI CHÚ") else tr("ADD NOTE", "THÊM GHI CHÚ")
        "link" -> if (editing != null) tr("EDIT LINK", "CHỈNH SỬA LIÊN KẾT") else tr("ADD LINK", "THÊM LIÊN KẾT")
        "code" -> if (editing != null) tr("EDIT CODE", "CHỈNH SỬA MÃ NGUỒN") else tr("ADD CODE", "THÊM MÃ NGUỒN")
        else -> if (editing != null) tr("EDIT ITEM", "CHỈNH SỬA") else tr("ADD ITEM", "THÊM MỤC")
    }

    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Box(modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp)) {
            Column(
                verticalArrangement = Arrangement.spacedBy(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .appleGlassSurface(
                        shape = RoundedCornerShape(36.dp),
                        variant = AppleGlassVariant.Elevated,
                        elevation = 24.dp,
                    )
                    .padding(22.dp),
            ) {
                Text(
                    text = modalHeading,
                    color = HefimerColors.White55,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 2.2.sp,
                )
                HefimerTextField(
                    value = name,
                    onValueChange = { name = it },
                    placeholder = when (mode) {
                        "note" -> tr("Note title...", "Tiêu đề ghi chú...")
                        "link" -> tr("Link title...", "Tiêu đề liên kết...")
                        else -> tr("Snippet title...", "Tiêu đề đoạn mã...")
                    },
                )
                if (mode == "link") {
                    HefimerTextField(
                        value = url,
                        onValueChange = { url = it },
                        placeholder = "https://...",
                        mono = true,
                    )
                } else {
                    HefimerTextField(
                        value = content,
                        onValueChange = { content = it },
                        placeholder = if (mode == "code") tr("Paste code...", "Dán mã nguồn...") else tr("Write note...", "Viết nội dung ghi chú..."),
                        mono = mode == "code",
                        singleLine = false,
                        modifier = Modifier.height(130.dp),
                    )
                }
                if (mode == "note") {
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        colors.forEach { (id, dot) ->
                            val selected = color == id
                            val swatchScale by animateFloatAsState(
                                targetValue = if (selected) 1.12f else 1f,
                                animationSpec = AppMotion.snappySpring(),
                                label = "swatch_scale_$id",
                            )
                            Box(
                                modifier = Modifier
                                    .size(28.dp)
                                    .graphicsLayer {
                                        scaleX = swatchScale
                                        scaleY = swatchScale
                                    }
                                    .clip(CircleShape)
                                    .background(dot)
                                    .border(if (selected) 2.5.dp else 1.dp, if (selected) HefimerColors.White else HefimerColors.White20, CircleShape)
                                    .applePressable(pressedScale = 0.88f) { color = id },
                            )
                        }
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    HefimerSecondaryButton(tr("Cancel", "Hủy"), onClick = onDismiss, modifier = Modifier.weight(1f))
                    HefimerPrimaryButton(
                        text = if (editing != null) tr("Update", "Cập nhật") else tr("Place", "Đặt lên bảng"),
                        height = 48.dp,
                        modifier = Modifier.weight(1f),
                        onClick = {
                            val finalUrl = if (mode == "link") {
                                val u = url.trim()
                                if (u.isNotBlank() && !u.startsWith("http")) "https://$u" else u
                            } else null
                            val defaultName = when (mode) {
                                "note" -> tr("Sticky Note", "Ghi chú dán")
                                "link" -> tr("Shared Link", "Liên kết")
                                "code" -> tr("Code Snippet", "Đoạn mã")
                                else -> tr("Untitled", "Không tên")
                            }
                            onSave(
                                name.trim().ifBlank { defaultName },
                                if (mode != "link") content else null,
                                finalUrl,
                                color,
                            )
                        },
                    )
                }
            }
        }
    }
}

@Composable
private fun SpaceItemPreviewDialog(
    item: SpaceItem,
    onDismiss: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onDownload: () -> Unit,
    onCopy: (String) -> Unit,
    onOpenLink: (String) -> Unit,
) {
    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Box(modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp)) {
            Column(
                verticalArrangement = Arrangement.spacedBy(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .appleGlassSurface(
                        shape = RoundedCornerShape(36.dp),
                        variant = AppleGlassVariant.Elevated,
                        elevation = 24.dp,
                    )
                    .padding(22.dp),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(localizedSpaceKind(item.kind), color = HefimerColors.White45, fontSize = 9.sp, fontWeight = FontWeight.Bold, letterSpacing = 2.sp)
                        Text(item.name, color = HefimerColors.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    }
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(34.dp)
                            .appleGlassSurface(CircleShape, AppleGlassVariant.Clear, elevation = 0.dp)
                            .applePressable(pressedScale = 0.89f, onClick = onDismiss),
                    ) {
                        Icon(Icons.Rounded.Close, tr("Close", "Đóng"), tint = HefimerColors.White75, modifier = Modifier.size(16.dp))
                    }
                }

                if (!item.content.isNullOrBlank()) {
                    val text = if (item.kind == "code") {
                        CodeHighlighter.highlight(item.content, CodeHighlighter.detectLanguage(item.content).id)
                    } else null
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 280.dp)
                            .verticalScroll(rememberScrollState())
                            .appleGlassSurface(
                                shape = RoundedCornerShape(20.dp),
                                variant = AppleGlassVariant.Clear,
                                elevation = 0.dp,
                            )
                            .padding(14.dp),
                    ) {
                        if (text != null) {
                            Text(text, fontFamily = JetBrainsMonoFamily, fontSize = 12.sp, color = HefimerColors.White)
                        } else {
                            Text(item.content, fontSize = 13.sp, color = HefimerColors.White85)
                        }
                    }
                } else if (item.kind == "file") {
                    Text(
                        text = "${formatBytes(item.size ?: 0L)} • ${item.providerName ?: tr("Host", "Máy chủ")}",
                        color = HefimerColors.White55,
                        fontFamily = JetBrainsMonoFamily,
                        fontSize = 12.sp,
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (item.kind == "file") {
                        HefimerPrimaryButton(tr("Download", "Tải xuống"), icon = Icons.Rounded.Download, height = 46.dp, modifier = Modifier.weight(1f), onClick = onDownload)
                    } else if (item.kind == "link" && !item.url.isNullOrBlank()) {
                        HefimerPrimaryButton(tr("Open Link", "Mở liên kết"), icon = Icons.Rounded.OpenInNew, height = 46.dp, modifier = Modifier.weight(1f), onClick = { onOpenLink(item.url) })
                    } else if (!item.content.isNullOrBlank()) {
                        HefimerPrimaryButton(tr("Copy", "Sao chép"), icon = Icons.Rounded.ContentCopy, height = 46.dp, modifier = Modifier.weight(1f), onClick = { onCopy(item.content) })
                    }
                    if (item.kind != "file") {
                        HefimerSecondaryButton(tr("Edit", "Sửa"), height = 46.dp, modifier = Modifier.weight(0.7f), onClick = onEdit)
                    }
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(46.dp)
                            .appleGlassSurface(
                                shape = CircleShape,
                                variant = AppleGlassVariant.Clear,
                                accentBorderTint = HefimerColors.Red400,
                                accentFillTint = HefimerColors.RedBg,
                                elevation = 0.dp,
                            )
                            .applePressable(pressedScale = 0.90f, onClick = onDelete),
                    ) {
                        Icon(Icons.Rounded.DeleteOutline, tr("Delete", "Xóa"), tint = HefimerColors.Red400, modifier = Modifier.size(18.dp))
                    }
                }
            }
        }
    }
}

