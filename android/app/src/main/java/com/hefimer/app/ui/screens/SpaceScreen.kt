package com.hefimer.app.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Casino
import androidx.compose.material.icons.rounded.CenterFocusStrong
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Code
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Description
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.Grid4x4
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.Inventory2
import androidx.compose.material.icons.rounded.Link
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.OpenInNew
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.QrCode2
import androidx.compose.material.icons.rounded.QrCodeScanner
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material.icons.rounded.StickyNote2
import androidx.compose.material.icons.rounded.UploadFile
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
import com.hefimer.app.ui.theme.AppleRollingPercentage
import com.hefimer.app.ui.theme.AppleSegmentedControl
import com.hefimer.app.ui.theme.AppleSwipeableSheet
import com.hefimer.app.ui.theme.HefimerColors
import com.hefimer.app.ui.theme.JetBrainsMonoFamily
import com.hefimer.app.ui.theme.LocalHeroBounds
import com.hefimer.app.ui.theme.UbuntuFontFamily
import com.hefimer.app.ui.theme.appleEntrance
import com.hefimer.app.ui.theme.applePressable
import com.hefimer.app.ui.theme.heroSource
import com.hefimer.app.ui.theme.rememberReduceMotion
import kotlinx.coroutines.delay
import kotlin.math.roundToInt

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SpaceScreen(vm: HefimerViewModel) {
    val state by vm.spaceState.collectAsState()
    val heroBounds = LocalHeroBounds.current
    val reduceMotion = rememberReduceMotion()

    HefimerCollapsibleScaffold(
        kicker = "Live collaborative canvas",
        title = "Space",
        subtitle = "Drop files, sticky notes, links, and code onto an infinite shared board.",
        showOrbitMark = true,
        headerActions = {
            HefimerHeaderIconButton(
                icon = Icons.Rounded.QrCodeScanner,
                contentDescription = "Scan Space QR",
                onClick = { vm.openOverlay(AppOverlay.QrScanner(ScanTarget.AUTO)) },
            )
            HefimerHeaderIconButton(
                icon = Icons.Rounded.History,
                contentDescription = "History",
                modifier = Modifier.heroSource("btn_history_space", heroBounds),
                onClick = { vm.openOverlay(AppOverlay.History, "btn_history_space") },
            )
        },
    ) {
        if (state.createdCode.isNotBlank()) {
            DropResultCard(
                code = state.createdCode,
                heading = "Your Space Code",
                subtitle = state.createdName,
                expiresAt = state.createdExpiresAt,
                shareUrl = "${HefimerApiConfig.SITE_ORIGIN}/space?code=${state.createdCode}",
                resetLabel = "New Space",
                primaryCtaLabel = "Enter Space",
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
                    .clip(RoundedCornerShape(28.dp))
                    .background(HefimerColors.SurfaceElevated)
                    .border(1.dp, HefimerColors.White10, RoundedCornerShape(28.dp))
                    .padding(18.dp),
            ) {
                Text(
                    text = "YOUR DISPLAY NAME",
                    color = HefimerColors.White30,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 2.sp,
                )
                HefimerTextField(
                    value = state.userName,
                    onValueChange = { vm.setSpaceUserName(it) },
                    placeholder = "Enter your name...",
                    leadingIcon = Icons.Rounded.Person,
                )
            }

            // Join / Create mode switch
            AppleSegmentedControl(
                options = listOf("join", "create"),
                selected = state.lobbyMode,
                label = { if (it == "join") "Join Space" else "Create Space" },
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
                            .clip(RoundedCornerShape(30.dp))
                            .background(HefimerColors.SurfaceElevated)
                            .border(1.dp, HefimerColors.White12, RoundedCornerShape(30.dp))
                            .padding(20.dp),
                    ) {
                        Text(
                            text = "5-DIGIT SPACE CODE",
                            color = HefimerColors.White30,
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
                                placeholder = "Enter space passcode...",
                                leadingIcon = Icons.Rounded.Lock,
                                isPassword = true,
                            )
                        }
                        HefimerPrimaryButton(
                            text = if (state.loading) "Joining..." else "Join Space",
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
                            .clip(RoundedCornerShape(30.dp))
                            .background(HefimerColors.SurfaceElevated)
                            .border(1.dp, HefimerColors.White12, RoundedCornerShape(30.dp))
                            .padding(20.dp),
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text("SPACE TITLE", color = HefimerColors.White30, fontSize = 9.sp, fontWeight = FontWeight.Bold, letterSpacing = 2.sp)
                            HefimerTextField(
                                value = state.createTitle,
                                onValueChange = { vm.setSpaceCreateTitle(it) },
                                placeholder = "Design review, sprint notes...",
                            )
                        }

                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Text("CUSTOM 5-DIGIT CODE", color = HefimerColors.White30, fontSize = 9.sp, fontWeight = FontWeight.Bold, letterSpacing = 2.sp)
                                val (statusText, statusColor) = when (state.createCodeStatus) {
                                    "checking" -> "Checking..." to HefimerColors.White40
                                    "available" -> "Available" to HefimerColors.Emerald400
                                    "taken" -> "Already in use" to HefimerColors.Red400
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
                                        .clip(RoundedCornerShape(16.dp))
                                        .background(HefimerColors.White08)
                                        .border(1.dp, HefimerColors.White12, RoundedCornerShape(16.dp))
                                        .applePressable { vm.randomizeSpaceCode() },
                                ) {
                                    Icon(Icons.Rounded.Casino, "Randomize", tint = HefimerColors.White, modifier = Modifier.size(20.dp))
                                }
                            }
                        }

                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("DURATION", color = HefimerColors.White30, fontSize = 9.sp, fontWeight = FontWeight.Bold, letterSpacing = 2.sp)
                            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                listOf("30m" to "30 min", "1h" to "1 hour", "6h" to "6 hours", "12h" to "12 hours", "24h" to "24 hours").forEach { (v, lbl) ->
                                    val sel = state.createExpire == v
                                    Text(
                                        text = lbl,
                                        color = if (sel) HefimerColors.Black else HefimerColors.White60,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier
                                            .clip(CircleShape)
                                            .background(if (sel) HefimerColors.White else HefimerColors.White08)
                                            .applePressable { vm.setSpaceCreateExpire(v) }
                                            .padding(horizontal = 14.dp, vertical = 8.dp),
                                    )
                                }
                            }
                        }

                        HefimerTextField(
                            value = state.createPassword,
                            onValueChange = { vm.setSpaceCreatePassword(it) },
                            placeholder = "Passcode (optional)",
                            leadingIcon = Icons.Rounded.Lock,
                        )

                        HefimerPrimaryButton(
                            text = if (state.loading) "Creating..." else "Create Space",
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
                        text = "RECENT SPACES",
                        color = HefimerColors.White30,
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
                                .clip(RoundedCornerShape(22.dp))
                                .background(HefimerColors.White04)
                                .border(1.dp, HefimerColors.White10, RoundedCornerShape(22.dp))
                                .applePressable { vm.joinSpace(recent.code) }
                                .padding(horizontal = 16.dp, vertical = 14.dp),
                        ) {
                            Column {
                                Text(recent.name, color = HefimerColors.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                                Text("#${recent.code}", color = HefimerColors.White40, fontFamily = JetBrainsMonoFamily, fontSize = 11.sp)
                            }
                            Text(
                                text = "Rejoin",
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
    val statusTop = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    val navBottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()

    var panX by remember { mutableFloatStateOf(0f) }
    var panY by remember { mutableFloatStateOf(0f) }
    var zoom by remember { mutableFloatStateOf(1f) }
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
            if (countdown == "Expired") {
                vm.showToast("Space has expired", ToastType.ERROR)
                onClose()
                break
            }
            delay(1000L)
        }
    }

    val filePicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenMultipleDocuments()) { uris ->
        if (uris.isNotEmpty()) {
            val targetX = ((-panX) / zoom + 80f).coerceIn(-2000f, 4000f)
            val targetY = ((-panY) / zoom + 160f).coerceIn(-2000f, 4000f)
            vm.spaceUploadFiles(uris, targetX, targetY)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(HefimerColors.Black)
            .pointerInput(Unit) {
                detectTransformGestures { _, pan, gestureZoom, _ ->
                    zoom = (zoom * gestureZoom).coerceIn(0.35f, 2.2f)
                    panX += pan.x
                    panY += pan.y
                }
            },
    ) {
        // Infinite dot-grid background synchronized with pan & zoom
        Canvas(modifier = Modifier.fillMaxSize()) {
            val step = (28.dp.toPx() * zoom).coerceAtLeast(14f)
            val ox = ((panX % step) + step) % step
            val oy = ((panY % step) + step) % step
            val dotRadius = (1.1.dp.toPx() * zoom).coerceIn(0.8f, 2.5f)
            var x = ox
            while (x < size.width) {
                var y = oy
                while (y < size.height) {
                    drawCircle(color = Color.White.copy(alpha = 0.10f), radius = dotRadius, center = Offset(x, y))
                    y += step
                }
                x += step
            }
        }

        // World items container transformed by pan & zoom
        Box(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    translationX = panX
                    translationY = panY
                    scaleX = zoom
                    scaleY = zoom
                    transformOrigin = androidx.compose.ui.graphics.TransformOrigin(0f, 0f)
                },
        ) {
            state.items.forEach { item ->
                SpaceCanvasCard(
                    item = item,
                    zoom = zoom,
                    snapGrid = snapGrid,
                    onDragFinish = { nx, ny ->
                        val nextZ = (state.items.maxOfOrNull { it.zIndex } ?: 1) + 1
                        vm.spaceMoveItem(item, nx, ny, nextZ)
                    },
                    onTap = { previewItem = item },
                    onDownload = {
                        item.url?.let { url ->
                            vm.downloadDropFile(DropFileEntry(fileName = item.name, fileUrl = url, fileSize = item.size ?: 0L))
                        }
                    },
                    onOpenLink = { item.url?.let { DownloadHelper.openExternalUrl(context, it) } },
                    onDelete = { vm.spaceDeleteItem(item) },
                )
            }

            // Live remote cursors
            state.cursors.forEach { cursor ->
                val col = runCatching { Color(android.graphics.Color.parseColor(cursor.color)) }
                    .getOrDefault(HefimerColors.Emerald400)
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier
                        .offset { IntOffset(cursor.x.roundToInt(), cursor.y.roundToInt()) }
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
                modifier = Modifier.align(Alignment.Center),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Text("Empty Canvas", color = HefimerColors.White50, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                Text("Use the dock below to drop files, sticky notes, links, or code.", color = HefimerColors.White30, fontSize = 12.sp)
            }
        }

        // Top translucent Space HUD
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth()
                .background(HefimerColors.Black.copy(alpha = 0.78f))
                .padding(top = statusTop + 8.dp, bottom = 10.dp, start = 14.dp, end = 14.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.weight(1f)) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(HefimerColors.White08)
                        .border(1.dp, HefimerColors.White12, CircleShape)
                        .applePressable(onClick = onClose),
                ) {
                    Icon(Icons.AutoMirrored.Rounded.ArrowBack, "Leave", tint = HefimerColors.White, modifier = Modifier.size(18.dp))
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = state.activeSpaceName.ifBlank { "Space ${state.activeSpaceId}" },
                        color = HefimerColors.White,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "#${state.activeSpaceId}",
                            color = HefimerColors.Emerald400,
                            fontFamily = JetBrainsMonoFamily,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                        )
                        Text("• ${state.members.size.coerceAtLeast(1)} online", color = HefimerColors.White50, fontSize = 10.sp)
                        if (countdown.isNotBlank()) {
                            Text("• $countdown", color = HefimerColors.White40, fontSize = 10.sp)
                        }
                    }
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                SpaceMiniTool(Icons.Rounded.Grid4x4, active = snapGrid) { snapGrid = !snapGrid }
                SpaceMiniTool(Icons.Rounded.AutoAwesome, active = false) { vm.spaceAutoArrange() }
                SpaceMiniTool(Icons.Rounded.CenterFocusStrong, active = false) {
                    panX = 0f; panY = 0f; zoom = 1f
                }
                SpaceMiniTool(Icons.Rounded.QrCode2, active = false) { showQr = true }
            }
        }

        // Upload queue progress HUD
        if (state.uploadTasks.isNotEmpty()) {
            val task = state.uploadTasks.first()
            Column(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = statusTop + 72.dp, start = 20.dp, end = 20.dp)
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(22.dp))
                    .background(HefimerColors.SurfaceCard)
                    .border(1.dp, HefimerColors.White15, RoundedCornerShape(22.dp))
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
                            color = HefimerColors.Emerald400,
                            style = TextStyle(
                                fontFamily = UbuntuFontFamily,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                            ),
                        )
                        Icon(
                            Icons.Rounded.Close,
                            "Cancel",
                            tint = HefimerColors.Red400,
                            modifier = Modifier
                                .size(20.dp)
                                .applePressable { vm.cancelSpaceUpload() },
                        )
                    }
                }
                AppleFluidProgressTrack(
                    progress = (task.progress / 100f).coerceIn(0f, 1f),
                    accentColor = HefimerColors.Emerald400,
                    trackColor = HefimerColors.White10,
                    height = 5.dp,
                )
            }
        }

        // Bottom floating canvas creation toolbar
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = navBottom + 14.dp, start = 16.dp, end = 16.dp)
                .clip(RoundedCornerShape(28.dp))
                .background(Color(0xEE0B0B0B))
                .border(1.dp, HefimerColors.White15, RoundedCornerShape(28.dp))
                .padding(horizontal = 12.dp, vertical = 8.dp),
        ) {
            SpaceDockAction(Icons.Rounded.UploadFile, "File", primary = true) { filePicker.launch(arrayOf("*/*")) }
            SpaceDockAction(Icons.Rounded.StickyNote2, "Note") { editingItem = null; composerMode = "note" }
            SpaceDockAction(Icons.Rounded.Link, "Link") { editingItem = null; composerMode = "link" }
            SpaceDockAction(Icons.Rounded.Code, "Code") { editingItem = null; composerMode = "code" }
            SpaceDockAction(Icons.Rounded.Inventory2, UploadProviders.getConfig(state.selectedProviderId).name.take(7)) {
                showProviderSheet = true
            }
        }

        // Share QR Modal
        if (showQr) {
            QrShareModal(
                title = "Share Space",
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
                        val x = ((-panX) / zoom + 70f).coerceIn(-2000f, 4000f)
                        val y = ((-panY) / zoom + 160f).coerceIn(-2000f, 4000f)
                        vm.spaceAddItem(composerMode!!, name, content, url, color, x, y)
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
                    vm.showToast("Copied", ToastType.SUCCESS)
                },
                onOpenLink = { u -> DownloadHelper.openExternalUrl(context, u) },
            )
        }
    }
}

@Composable
private fun SpaceCanvasCard(
    item: SpaceItem,
    zoom: Float,
    snapGrid: Boolean,
    onDragFinish: (Float, Float) -> Unit,
    onTap: () -> Unit,
    onDownload: () -> Unit,
    onOpenLink: () -> Unit,
    onDelete: () -> Unit,
) {
    var localX by remember(item.id, item.x) { mutableFloatStateOf(item.x) }
    var localY by remember(item.id, item.y) { mutableFloatStateOf(item.y) }

    val (bg, border) = if (item.kind == "note") {
        when (item.noteColor) {
            "emerald" -> HefimerColors.NoteEmeraldBg to HefimerColors.NoteEmeraldBorder
            "amber" -> HefimerColors.NoteAmberBg to HefimerColors.NoteAmberBorder
            "sky" -> HefimerColors.NoteSkyBg to HefimerColors.NoteSkyBorder
            "rose" -> HefimerColors.NoteRoseBg to HefimerColors.NoteRoseBorder
            "violet" -> HefimerColors.NoteVioletBg to HefimerColors.NoteVioletBorder
            else -> HefimerColors.NoteZincBg to HefimerColors.NoteZincBorder
        }
    } else {
        HefimerColors.SurfaceCard.copy(alpha = 0.96f) to HefimerColors.White15
    }

    val previewUrl = remember(item.url, item.mimeType) {
        if (item.kind == "file" && item.mimeType?.startsWith("image/") == true) {
            DownloadHelper.getMediaPreviewUrl(item.url, item.mimeType)
        } else null
    }

    Column(
        modifier = Modifier
            .offset { IntOffset(localX.roundToInt(), localY.roundToInt()) }
            .width(240.dp)
            .clip(RoundedCornerShape(24.dp))
            .background(bg)
            .border(1.dp, border, RoundedCornerShape(24.dp))
            .pointerInput(item.id, zoom, snapGrid) {
                detectDragGestures(
                    onDragEnd = {
                        val finalX = if (snapGrid) (localX / 20f).roundToInt() * 20f else localX
                        val finalY = if (snapGrid) (localY / 20f).roundToInt() * 20f else localY
                        localX = finalX
                        localY = finalY
                        onDragFinish(finalX, finalY)
                    },
                    onDrag = { change, dragAmount ->
                        change.consume()
                        localX += dragAmount.x / zoom
                        localY += dragAmount.y / zoom
                    },
                )
            }
            .applePressable(pressedScale = 0.985f, onClick = onTap)
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = item.kind.uppercase(),
                color = HefimerColors.White40,
                fontSize = 8.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.8.sp,
            )
            Text(
                text = item.uploadedBy,
                color = HefimerColors.White30,
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
                color = HefimerColors.White70,
                fontFamily = if (item.kind == "code") JetBrainsMonoFamily else com.hefimer.app.ui.theme.UbuntuFontFamily,
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
                text = "${formatBytes(item.size ?: 0L)} • ${item.providerName ?: "Host"}",
                color = HefimerColors.White40,
                fontFamily = JetBrainsMonoFamily,
                fontSize = 10.sp,
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (item.kind == "file" && !item.url.isNullOrBlank()) {
                Icon(
                    Icons.Rounded.Download,
                    "Download",
                    tint = HefimerColors.White70,
                    modifier = Modifier.size(18.dp).applePressable(onClick = onDownload),
                )
                Spacer(Modifier.width(12.dp))
            }
            if (item.kind == "link" && !item.url.isNullOrBlank()) {
                Icon(
                    Icons.Rounded.OpenInNew,
                    "Open",
                    tint = HefimerColors.Sky400,
                    modifier = Modifier.size(18.dp).applePressable(onClick = onOpenLink),
                )
                Spacer(Modifier.width(12.dp))
            }
            Icon(
                Icons.Rounded.DeleteOutline,
                "Delete",
                tint = HefimerColors.White30,
                modifier = Modifier.size(18.dp).applePressable(onClick = onDelete),
            )
        }
    }
}

@Composable
private fun SpaceMiniTool(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    active: Boolean,
    onClick: () -> Unit,
) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .size(36.dp)
            .clip(CircleShape)
            .background(if (active) HefimerColors.White else HefimerColors.White08)
            .border(1.dp, HefimerColors.White12, CircleShape)
            .applePressable(pressedScale = 0.9f, onClick = onClick),
    ) {
        Icon(icon, null, tint = if (active) HefimerColors.Black else HefimerColors.White80, modifier = Modifier.size(16.dp))
    }
}

@Composable
private fun SpaceDockAction(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    primary: Boolean = false,
    onClick: () -> Unit,
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clip(RoundedCornerShape(18.dp))
            .background(if (primary) HefimerColors.White else HefimerColors.White06)
            .applePressable(pressedScale = 0.92f, onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 8.dp),
    ) {
        Icon(icon, null, tint = if (primary) HefimerColors.Black else HefimerColors.White80, modifier = Modifier.size(18.dp))
        Spacer(Modifier.height(2.dp))
        Text(
            text = label,
            color = if (primary) HefimerColors.Black else HefimerColors.White60,
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

    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Box(modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp)) {
            Column(
                verticalArrangement = Arrangement.spacedBy(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(30.dp))
                    .background(HefimerColors.SurfaceCard)
                    .border(1.dp, HefimerColors.White15, RoundedCornerShape(30.dp))
                    .padding(22.dp),
            ) {
                Text(
                    text = "${if (editing != null) "EDIT" else "ADD"} ${mode.uppercase()}",
                    color = HefimerColors.White50,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 2.2.sp,
                )
                HefimerTextField(
                    value = name,
                    onValueChange = { name = it },
                    placeholder = if (mode == "note") "Note title..." else if (mode == "link") "Link title..." else "Snippet title...",
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
                        placeholder = if (mode == "code") "Paste code..." else "Write note...",
                        mono = mode == "code",
                        singleLine = false,
                        modifier = Modifier.height(130.dp),
                    )
                }
                if (mode == "note") {
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        colors.forEach { (id, dot) ->
                            val selected = color == id
                            Box(
                                modifier = Modifier
                                    .size(28.dp)
                                    .clip(CircleShape)
                                    .background(dot)
                                    .border(if (selected) 2.5.dp else 0.dp, HefimerColors.White, CircleShape)
                                    .applePressable { color = id },
                            )
                        }
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    HefimerSecondaryButton("Cancel", onClick = onDismiss, modifier = Modifier.weight(1f))
                    HefimerPrimaryButton(
                        text = if (editing != null) "Update" else "Place",
                        height = 48.dp,
                        modifier = Modifier.weight(1f),
                        onClick = {
                            val finalUrl = if (mode == "link") {
                                val u = url.trim()
                                if (u.isNotBlank() && !u.startsWith("http")) "https://$u" else u
                            } else null
                            onSave(
                                name.trim().ifBlank { if (mode == "note") "Sticky Note" else mode.replaceFirstChar { it.uppercase() } },
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
                    .clip(RoundedCornerShape(30.dp))
                    .background(HefimerColors.SurfaceCard)
                    .border(1.dp, HefimerColors.White15, RoundedCornerShape(30.dp))
                    .padding(22.dp),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(item.kind.uppercase(), color = HefimerColors.White40, fontSize = 9.sp, fontWeight = FontWeight.Bold, letterSpacing = 2.sp)
                        Text(item.name, color = HefimerColors.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    }
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier.size(34.dp).clip(CircleShape).background(HefimerColors.White08).applePressable(onClick = onDismiss),
                    ) {
                        Icon(Icons.Rounded.Close, null, tint = HefimerColors.White70, modifier = Modifier.size(16.dp))
                    }
                }

                if (!item.content.isNullOrBlank()) {
                    val text = if (item.kind == "code") {
                        CodeHighlighter.highlight(item.content, CodeHighlighter.detectLanguage(item.content).id)
                    } else null
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(18.dp))
                            .background(HefimerColors.Black)
                            .border(1.dp, HefimerColors.White10, RoundedCornerShape(18.dp))
                            .padding(14.dp),
                    ) {
                        if (text != null) {
                            Text(text, fontFamily = JetBrainsMonoFamily, fontSize = 12.sp, color = HefimerColors.White)
                        } else {
                            Text(item.content, fontSize = 13.sp, color = HefimerColors.White80)
                        }
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (item.kind == "file") {
                        HefimerPrimaryButton("Download", icon = Icons.Rounded.Download, height = 46.dp, modifier = Modifier.weight(1f), onClick = onDownload)
                    } else if (item.kind == "link" && !item.url.isNullOrBlank()) {
                        HefimerPrimaryButton("Open Link", icon = Icons.Rounded.OpenInNew, height = 46.dp, modifier = Modifier.weight(1f), onClick = { onOpenLink(item.url) })
                    } else if (!item.content.isNullOrBlank()) {
                        HefimerPrimaryButton("Copy", icon = Icons.Rounded.ContentCopy, height = 46.dp, modifier = Modifier.weight(1f), onClick = { onCopy(item.content) })
                    }
                    if (item.kind != "file") {
                        HefimerSecondaryButton("Edit", height = 46.dp, modifier = Modifier.weight(0.7f), onClick = onEdit)
                    }
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(46.dp)
                            .clip(CircleShape)
                            .background(HefimerColors.RedBg)
                            .border(1.dp, HefimerColors.RedBorder, CircleShape)
                            .applePressable(onClick = onDelete),
                    ) {
                        Icon(Icons.Rounded.DeleteOutline, "Delete", tint = HefimerColors.Red400, modifier = Modifier.size(18.dp))
                    }
                }
            }
        }
    }
}
