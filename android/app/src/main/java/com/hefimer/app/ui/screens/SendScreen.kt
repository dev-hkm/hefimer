package com.hefimer.app.ui.screens

import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Code
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.CreateNewFolder
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Description
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material.icons.rounded.Folder
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.Inventory2
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.QrCodeScanner
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material.icons.rounded.StarOutline
import androidx.compose.material.icons.rounded.UploadFile
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.hefimer.app.data.model.ToastType
import com.hefimer.app.data.model.formatBytes
import com.hefimer.app.data.network.DownloadHelper
import com.hefimer.app.data.network.UploadProviderConfig
import com.hefimer.app.data.network.UploadProviders
import com.hefimer.app.ui.AppOverlay
import com.hefimer.app.ui.HefimerViewModel
import com.hefimer.app.ui.ScanTarget
import com.hefimer.app.ui.SendSubMode
import com.hefimer.app.ui.SendUiState
import com.hefimer.app.ui.components.CodeHighlighter
import com.hefimer.app.ui.components.DropResultCard
import com.hefimer.app.ui.components.HefimerCollapsibleScaffold
import com.hefimer.app.ui.components.HefimerHeaderIconButton
import com.hefimer.app.ui.components.HefimerOrbitLogo
import com.hefimer.app.ui.components.HefimerPrimaryButton
import com.hefimer.app.ui.components.HefimerToggleRow
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
import com.hefimer.app.ui.theme.rememberReduceMotion
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import com.hefimer.app.ui.theme.appleEntrance
import com.hefimer.app.ui.theme.applePressable
import com.hefimer.app.ui.theme.heroSource
import com.hefimer.app.ui.theme.rememberReduceMotion
import kotlinx.coroutines.launch

@Composable
fun SendScreen(vm: HefimerViewModel) {
    val state by vm.sendState.collectAsState()
    val stdStats by vm.standardStats.collectAsState()
    val r2Stats by vm.r2Stats.collectAsState()
    val heroBounds = LocalHeroBounds.current
    val reduceMotion = rememberReduceMotion()

    val accent = if (state.isR2Mode && state.subMode == SendSubMode.FILE) {
        HefimerColors.Emerald400
    } else {
        HefimerColors.White
    }

    Box {
        HefimerCollapsibleScaffold(
            kicker = if (state.isR2Mode && state.subMode == SendSubMode.FILE) "Secret R2 Mode" else "Create a drop",
            title = "Send",
            subtitle = "Share a file or text that disappears when its moment is over.",
            accentColor = accent,
            showOrbitMark = true,
            headerActions = {
                HefimerHeaderIconButton(
                    icon = Icons.Rounded.QrCodeScanner,
                    contentDescription = "Scan QR",
                    modifier = Modifier.heroSource("btn_qr_send", heroBounds),
                    onClick = { vm.openOverlay(AppOverlay.QrScanner(ScanTarget.AUTO), "btn_qr_send") },
                )
                HefimerHeaderIconButton(
                    icon = Icons.Rounded.History,
                    contentDescription = "History",
                    modifier = Modifier.heroSource("btn_history_send", heroBounds),
                    onClick = { vm.openOverlay(AppOverlay.History, "btn_history_send") },
                )
                HefimerHeaderIconButton(
                    icon = Icons.Rounded.Settings,
                    contentDescription = "Settings",
                    onClick = { vm.openSettings() },
                )
            },
        ) {
            // Concentric sliding File / Text segmented toggle
            AppleSegmentedControl(
                options = listOf(SendSubMode.FILE, SendSubMode.TEXT),
                selected = state.subMode,
                label = { if (it == SendSubMode.FILE) "Send File" else "Send Text" },
                accentColor = accent,
                onSelect = { vm.setSendSubMode(it) },
                modifier = Modifier.appleEntrance(0),
            )

            Spacer(Modifier.height(18.dp))

            AnimatedContent(
                targetState = state.subMode,
                transitionSpec = { AppMotion.sectionTransform(reduceMotion) },
                label = "send_mode_switch",
            ) { mode ->
                when (mode) {
                    SendSubMode.FILE -> SendFileSection(
                        vm = vm,
                        state = state,
                        todayBytes = if (state.isR2Mode) r2Stats.totalSize else stdStats.totalSize,
                        onOpenProviderPicker = { vm.openSendProviderPicker() },
                    )
                    SendSubMode.TEXT -> SendTextSection(vm = vm, state = state)
                }
            }
        }

        // Secret R2 Passcode Modal (triggered by 6 taps on Send File)
        if (state.showR2AuthDialog) {
            SecretR2PasscodeModal(
                onDismiss = { vm.dismissR2AuthDialog() },
                onSubmit = { vm.verifyR2Passcode(it) },
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun SendFileSection(
    vm: HefimerViewModel,
    state: SendUiState,
    todayBytes: Long,
    onOpenProviderPicker: () -> Unit,
) {
    val context = LocalContext.current
    var showAdvanced by remember { mutableStateOf(false) }

    val multiFileLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenMultipleDocuments(),
    ) { uris ->
        if (uris.isNotEmpty()) {
            uris.forEach { uri ->
                runCatching {
                    context.contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
            }
            vm.onFilesPicked(uris)
        }
    }

    val folderLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocumentTree(),
    ) { treeUri ->
        if (treeUri != null) vm.onFolderPicked(treeUri)
    }

    if (state.fileResultCode.isNotBlank()) {
        DropResultCard(
            code = state.fileResultCode,
            heading = if (state.fileResultIsR2) "Secret R2 Code" else "Your Drop Code",
            subtitle = state.fileResultName,
            expiresAt = state.fileResultExpiresAt,
            isR2 = state.fileResultIsR2,
            onBack = { vm.resetFileResult(clearSelection = false) },
            onReset = { vm.resetFileResult(clearSelection = true) },
            onToast = { msg, err -> vm.showToast(msg, if (err) ToastType.ERROR else ToastType.SUCCESS) },
        )
        return
    }

    val isR2 = state.isR2Mode
    val accent = if (isR2) HefimerColors.Emerald400 else HefimerColors.White
    val providerCfg = UploadProviders.getConfig(state.selectedProviderId)
    val expireOptions = if (isR2) UploadProviders.STANDARD_EXPIRE_OPTIONS else providerCfg.expireOptions

    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        if (isR2) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(HefimerColors.Emerald400),
                    )
                    Text(
                        text = "SECRET CLOUDFLARE R2",
                        color = HefimerColors.Emerald400.copy(alpha = 0.85f),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 2.sp,
                    )
                }
                Text(
                    text = "EXIT",
                    color = HefimerColors.White50,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.4.sp,
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(HefimerColors.White06)
                        .border(1.dp, HefimerColors.White10, CircleShape)
                        .applePressable { vm.exitR2Mode() }
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                )
            }
        }

        // File Dropzone Card
        val reduceMotion = rememberReduceMotion()
        val dropBg = if (isR2) HefimerColors.EmeraldBg else HefimerColors.White025
        val dropBorder = if (isR2) HefimerColors.EmeraldBorder else HefimerColors.White20
        val dropzoneMode = when {
            state.fileUploading -> "uploading"
            state.pickedFiles.isNotEmpty() -> "picked"
            else -> "empty"
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 160.dp)
                .clip(RoundedCornerShape(34.dp))
                .background(dropBg)
                .border(1.dp, dropBorder, RoundedCornerShape(34.dp))
                .applePressable(enabled = !state.fileUploading) {
                    if (state.isFolderMode) {
                        folderLauncher.launch(null)
                    } else {
                        multiFileLauncher.launch(arrayOf("*/*"))
                    }
                },
        ) {
            AnimatedContent(
                targetState = dropzoneMode,
                transitionSpec = { AppMotion.sectionTransform(reduceMotion) },
                label = "dropzone_mode",
            ) { mode ->
                when (mode) {
                    "uploading" -> {
                        val smoothProgress by animateFloatAsState(
                            targetValue = state.fileProgressFraction.coerceIn(0f, 1f),
                            animationSpec = if (reduceMotion) tween(120, easing = AppMotion.AppleEase) else AppMotion.gentleSpring(),
                            label = "upload_ring_progress",
                        )

                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 24.dp, vertical = 22.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                        ) {
                            // Orbital Aperture Ring + spinning Hefimer mark
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier.size(84.dp),
                            ) {
                                Canvas(modifier = Modifier.size(84.dp)) {
                                    val strokePx = 3.2.dp.toPx()
                                    val inset = strokePx / 2f + 2.dp.toPx()
                                    val arcSize = Size(size.width - inset * 2f, size.height - inset * 2f)
                                    val topLeft = Offset(inset, inset)
                                    // Subtle ring track
                                    drawArc(
                                        color = accent.copy(alpha = 0.13f),
                                        startAngle = 0f,
                                        sweepAngle = 360f,
                                        useCenter = false,
                                        topLeft = topLeft,
                                        size = arcSize,
                                        style = Stroke(width = strokePx, cap = StrokeCap.Round),
                                    )
                                    // Smooth spring-interpolated sweep arc
                                    val sweep = (smoothProgress * 360f).coerceIn(0f, 360f)
                                    if (sweep > 0.5f) {
                                        drawArc(
                                            color = accent,
                                            startAngle = -90f,
                                            sweepAngle = sweep,
                                            useCenter = false,
                                            topLeft = topLeft,
                                            size = arcSize,
                                            style = Stroke(width = strokePx, cap = StrokeCap.Round),
                                        )
                                        // Leading specular bead at arc tip
                                        if (smoothProgress in 0.01f..0.99f) {
                                            val rad = (-90.0 + sweep) * PI / 180.0
                                            val radius = arcSize.width / 2f
                                            val cx = size.width / 2f + (radius * cos(rad)).toFloat()
                                            val cy = size.height / 2f + (radius * sin(rad)).toFloat()
                                            drawCircle(
                                                color = accent.copy(alpha = 0.35f),
                                                radius = strokePx * 1.9f,
                                                center = Offset(cx, cy),
                                            )
                                            drawCircle(
                                                color = HefimerColors.White,
                                                radius = strokePx * 0.72f,
                                                center = Offset(cx, cy),
                                            )
                                        }
                                    }
                                }
                                HefimerOrbitLogo(
                                    size = 34.dp,
                                    tint = accent,
                                    spinning = true,
                                    spinDurationMs = 3_600,
                                )
                            }

                            Spacer(Modifier.height(14.dp))

                            // Per-digit independent rolling percentage
                            AppleRollingPercentage(
                                percent = state.fileProgress,
                                color = HefimerColors.White,
                                style = TextStyle(
                                    fontFamily = UbuntuFontFamily,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 32.sp,
                                    letterSpacing = (-0.6).sp,
                                ),
                            )

                            // Live byte telemetry (tabular monospace)
                            if (state.fileTotalBytes > 0L) {
                                Spacer(Modifier.height(4.dp))
                                Text(
                                    text = "${formatBytes(state.fileUploadedBytes)} / ${formatBytes(state.fileTotalBytes)}",
                                    color = accent.copy(alpha = 0.78f),
                                    style = TextStyle(
                                        fontFamily = JetBrainsMonoFamily,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Medium,
                                        fontFeatureSettings = "tnum",
                                    ),
                                )
                            }

                            if (state.fileStatus.isNotBlank()) {
                                Spacer(Modifier.height(6.dp))
                                AnimatedContent(
                                    targetState = state.fileStatus,
                                    transitionSpec = { AppMotion.softCrossDissolve() },
                                    label = "upload_status_text",
                                ) { statusLine ->
                                    Text(
                                        text = statusLine,
                                        color = HefimerColors.White50,
                                        fontSize = 12.sp,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        textAlign = TextAlign.Center,
                                    )
                                }
                            }

                            Spacer(Modifier.height(16.dp))

                            // Fluid spring-interpolated track with specular sheen
                            AppleFluidProgressTrack(
                                progress = state.fileProgressFraction,
                                accentColor = accent,
                                trackColor = HefimerColors.White10,
                                height = 6.dp,
                            )

                            Spacer(Modifier.height(16.dp))

                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(5.dp),
                                modifier = Modifier
                                    .clip(CircleShape)
                                    .background(HefimerColors.RedBg)
                                    .border(1.dp, HefimerColors.RedBorder, CircleShape)
                                    .applePressable { vm.cancelFileUpload() }
                                    .padding(horizontal = 16.dp, vertical = 8.dp),
                            ) {
                                Icon(Icons.Rounded.Close, null, tint = HefimerColors.Red400, modifier = Modifier.size(13.dp))
                                Text(
                                    text = "CANCEL UPLOAD",
                                    color = HefimerColors.Red400,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 1.4.sp,
                                )
                            }
                        }
                    }

                    "picked" -> {
                        val totalSize = state.pickedFiles.sumOf { it.size }
                        val label = if (state.pickedFiles.size > 1) {
                            "${state.pickedFiles.size} files selected"
                        } else {
                            state.pickedFiles.firstOrNull()?.name.orEmpty()
                        }
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(22.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(16.dp),
                        ) {
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier
                                    .size(56.dp)
                                    .clip(CircleShape)
                                    .background(accent.copy(alpha = 0.10f))
                                    .border(1.dp, accent.copy(alpha = 0.22f), CircleShape),
                            ) {
                                Icon(
                                    imageVector = if (state.isFolderMode) Icons.Rounded.Folder else Icons.Rounded.Description,
                                    contentDescription = null,
                                    tint = accent,
                                    modifier = Modifier.size(24.dp),
                                )
                            }
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = label,
                                    color = HefimerColors.White,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis,
                                )
                                Spacer(Modifier.height(4.dp))
                                Text(
                                    text = formatBytes(totalSize),
                                    color = accent.copy(alpha = 0.65f),
                                    fontFamily = JetBrainsMonoFamily,
                                    fontSize = 12.sp,
                                )
                            }
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier
                                    .size(34.dp)
                                    .clip(CircleShape)
                                    .background(HefimerColors.White)
                                    .applePressable { vm.clearPickedFiles() },
                            ) {
                                Icon(Icons.Rounded.Close, "Remove", tint = HefimerColors.Black, modifier = Modifier.size(16.dp))
                            }
                        }
                    }

                    else -> {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 22.dp, vertical = 26.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                        ) {
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier
                                    .size(56.dp)
                                    .clip(CircleShape)
                                    .background(accent.copy(alpha = 0.08f))
                                    .border(1.dp, accent.copy(alpha = 0.16f), CircleShape),
                            ) {
                                Icon(
                                    imageVector = when {
                                        isR2 -> Icons.Rounded.Shield
                                        state.isFolderMode -> Icons.Rounded.CreateNewFolder
                                        else -> Icons.Rounded.UploadFile
                                    },
                                    contentDescription = null,
                                    tint = accent.copy(alpha = 0.85f),
                                    modifier = Modifier.size(25.dp),
                                )
                            }
                            Spacer(Modifier.height(12.dp))
                            Text(
                                text = if (state.isFolderMode) "Tap to select a folder" else "Tap to select file(s)",
                                color = HefimerColors.White,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                            )
                            Spacer(Modifier.height(8.dp))
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(5.dp),
                                    modifier = Modifier
                                        .clip(CircleShape)
                                        .background(HefimerColors.Emerald400.copy(alpha = 0.12f))
                                        .border(1.dp, HefimerColors.Emerald400.copy(alpha = 0.25f), CircleShape)
                                        .padding(horizontal = 8.dp, vertical = 3.dp),
                                ) {
                                    Box(Modifier.size(5.dp).clip(CircleShape).background(HefimerColors.Emerald300))
                                    Text(
                                        text = if (isR2) "Cloudflare R2" else providerCfg.name,
                                        color = HefimerColors.Emerald300,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                    )
                                }
                                Text(
                                    text = "Max ${if (isR2) "5 GB" else providerCfg.maxSizeLabel}",
                                    color = HefimerColors.White40,
                                    fontFamily = JetBrainsMonoFamily,
                                    fontSize = 10.sp,
                                )
                                Text("•", color = HefimerColors.White20, fontSize = 10.sp)
                                Text(
                                    text = "Today ${formatBytes(todayBytes)}",
                                    color = HefimerColors.White40,
                                    fontFamily = JetBrainsMonoFamily,
                                    fontSize = 10.sp,
                                )
                            }
                        }
                    }
                }
            }
        }

        // Upload Provider Picker Row (for standard mode)
        if (!isR2) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(22.dp))
                    .background(HefimerColors.White025)
                    .border(1.dp, HefimerColors.White10, RoundedCornerShape(22.dp))
                    .applePressable(onClick = onOpenProviderPicker)
                    .padding(horizontal = 14.dp, vertical = 12.dp),
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(HefimerColors.Black)
                        .border(1.dp, HefimerColors.White10, RoundedCornerShape(14.dp)),
                ) {
                    Icon(Icons.Rounded.Inventory2, null, tint = HefimerColors.White, modifier = Modifier.size(18.dp))
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "UPLOAD PROVIDER",
                        color = HefimerColors.White40,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.8.sp,
                    )
                    Spacer(Modifier.height(2.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Text(
                            text = providerCfg.name,
                            color = HefimerColors.White,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                        )
                        Text(
                            text = providerCfg.maxSizeLabel,
                            color = HefimerColors.Emerald300,
                            fontFamily = JetBrainsMonoFamily,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier
                                .clip(CircleShape)
                                .background(HefimerColors.Emerald400.copy(alpha = 0.12f))
                                .border(1.dp, HefimerColors.Emerald400.copy(alpha = 0.2f), CircleShape)
                                .padding(horizontal = 7.dp, vertical = 2.dp),
                        )
                    }
                }
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(34.dp)
                        .clip(CircleShape)
                        .border(1.dp, HefimerColors.White10, CircleShape),
                ) {
                    Icon(Icons.Rounded.ExpandMore, null, tint = HefimerColors.White50, modifier = Modifier.size(18.dp))
                }
            }
        } else {
            // Secret R2 Optional custom name & note
            HefimerTextField(
                value = state.r2CustomName,
                onValueChange = { vm.setR2CustomName(it) },
                placeholder = "Custom file name (optional)",
            )
            HefimerTextField(
                value = state.r2Description,
                onValueChange = { vm.setR2Description(it) },
                placeholder = "Secret note / description (optional)",
            )
        }

        // Advanced Options + File/Folder Toggle Accordion
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(24.dp))
                .background(HefimerColors.White025)
                .border(1.dp, HefimerColors.White10, RoundedCornerShape(24.dp)),
        ) {
            val chevronRot by animateFloatAsState(
                targetValue = if (showAdvanced) 180f else 0f,
                animationSpec = AppMotion.snappySpring(),
                label = "adv_chevron",
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .applePressable(pressedScale = 0.99f) { showAdvanced = !showAdvanced }
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Text(
                        text = "Advanced Options",
                        color = HefimerColors.White60,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                    )
                    Icon(
                        imageVector = Icons.Rounded.ExpandMore,
                        contentDescription = null,
                        tint = HefimerColors.White50,
                        modifier = Modifier
                            .size(16.dp)
                            .graphicsLayer { rotationZ = chevronRot },
                    )
                }

                // Inline File / Folder mode pill
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(HefimerColors.Black.copy(alpha = 0.55f))
                        .border(1.dp, HefimerColors.White08, RoundedCornerShape(12.dp))
                        .padding(3.dp),
                    horizontalArrangement = Arrangement.spacedBy(2.dp),
                ) {
                    listOf(false to "FILE", true to "FOLDER").forEach { (folder, lbl) ->
                        val active = state.isFolderMode == folder
                        Text(
                            text = lbl,
                            color = if (active) HefimerColors.White else HefimerColors.White30,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp,
                            modifier = Modifier
                                .clip(RoundedCornerShape(9.dp))
                                .background(if (active) HefimerColors.White15 else Color.Transparent)
                                .applePressable(pressedScale = 0.95f) { vm.setFolderMode(folder) }
                                .padding(horizontal = 10.dp, vertical = 5.dp),
                        )
                    }
                }
            }

            AnimatedVisibility(
                visible = showAdvanced,
                enter = AppMotion.softExpandEnter,
                exit = AppMotion.softExpandExit,
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 16.dp, end = 16.dp, bottom = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                ) {
                    Box(Modifier.fillMaxWidth().height(0.5.dp).background(HefimerColors.White08))

                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            text = "SELF-DESTRUCT TIMER",
                            color = HefimerColors.White30,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.8.sp,
                        )
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            expireOptions.forEach { opt ->
                                val selected = state.fileExpire == opt.value
                                Text(
                                    text = opt.label,
                                    color = if (selected) HefimerColors.Black else HefimerColors.White50,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier
                                        .clip(CircleShape)
                                        .background(if (selected) accent else HefimerColors.White06)
                                        .applePressable(pressedScale = 0.94f) { vm.setFileExpire(opt.value) }
                                        .padding(horizontal = 14.dp, vertical = 8.dp),
                                )
                            }
                        }
                    }

                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            text = "PASSWORD (OPTIONAL)",
                            color = HefimerColors.White30,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.8.sp,
                        )
                        HefimerTextField(
                            value = state.filePassword,
                            onValueChange = { vm.setFilePassword(it) },
                            placeholder = "Set a password...",
                            leadingIcon = Icons.Rounded.Lock,
                        )
                    }

                    HefimerToggleRow(
                        title = "Self-Destruct",
                        subtitle = "Delete after one successful read",
                        checked = state.fileOneTime,
                        onToggle = { vm.toggleFileOneTime() },
                        icon = Icons.Rounded.DeleteOutline,
                        activeColor = HefimerColors.Red500,
                    )
                }
            }
        }

        // Primary CTA
        HefimerPrimaryButton(
            text = if (state.fileUploading) "Uploading..." else if (isR2) "Upload to Secret R2" else "Upload & Generate Code",
            loading = state.fileUploading,
            enabled = state.pickedFiles.isNotEmpty() && !state.fileUploading,
            accentColor = accent,
            onClick = { vm.uploadFiles() },
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun SendTextSection(
    vm: HefimerViewModel,
    state: SendUiState,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var showLineNumbers by remember { mutableStateOf(false) }
    var showLangPicker by remember { mutableStateOf(false) }
    var showAdvanced by remember { mutableStateOf(false) }
    var previewHighlight by remember { mutableStateOf(false) }

    if (state.textResultCode.isNotBlank()) {
        DropResultCard(
            code = state.textResultCode,
            heading = "Your Drop Code",
            expiresAt = state.textResultExpiresAt,
            onBack = { vm.resetTextResult(clearContent = false) },
            onReset = { vm.resetTextResult(clearContent = true) },
            onToast = { msg, err -> vm.showToast(msg, if (err) ToastType.ERROR else ToastType.SUCCESS) },
        )
        return
    }

    val detected = remember(state.textContent) { CodeHighlighter.detectLanguage(state.textContent) }
    val activeLang = state.manualLangId?.let { id ->
        CodeHighlighter.SUPPORTED_LANGUAGES.firstOrNull { it.id == id }
    } ?: detected
    val lines = remember(state.textContent) { maxOf(1, state.textContent.count { it == '\n' } + 1) }

    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        // Editor Card matching web SendText
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(30.dp))
                .background(HefimerColors.Black)
                .border(1.dp, HefimerColors.White20, RoundedCornerShape(30.dp)),
        ) {
            // Top bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(HefimerColors.SurfaceCard)
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(HefimerColors.White10)
                        .border(1.dp, HefimerColors.White10, CircleShape)
                        .applePressable { showLangPicker = !showLangPicker }
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                ) {
                    Text(
                        text = activeLang.label.uppercase(),
                        color = HefimerColors.White70,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.6.sp,
                    )
                    Icon(Icons.Rounded.Code, null, tint = HefimerColors.White50, modifier = Modifier.size(12.dp))
                }

                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    EditorMiniIcon(
                        icon = if (showLineNumbers) Icons.Rounded.VisibilityOff else Icons.Rounded.Visibility,
                        active = showLineNumbers,
                        onClick = { showLineNumbers = !showLineNumbers },
                    )
                    EditorMiniIcon(
                        icon = Icons.Rounded.Code,
                        active = previewHighlight,
                        onClick = { previewHighlight = !previewHighlight },
                    )
                    EditorMiniIcon(
                        icon = Icons.Rounded.Download,
                        active = false,
                        onClick = {
                            if (state.textContent.isNotBlank()) {
                                scope.launch {
                                    runCatching {
                                        DownloadHelper.saveTextToDownloads(
                                            context,
                                            "hefimer_snippet.${activeLang.ext}",
                                            state.textContent,
                                        )
                                    }.onSuccess {
                                        vm.showToast("Saved $it to Downloads", ToastType.SUCCESS)
                                    }.onFailure {
                                        vm.showToast(it.message ?: "Save failed", ToastType.ERROR)
                                    }
                                }
                            }
                        },
                    )
                    EditorMiniIcon(
                        icon = Icons.Rounded.DeleteOutline,
                        active = false,
                        onClick = { vm.setTextContent("") },
                    )
                    EditorMiniIcon(
                        icon = Icons.Rounded.ContentCopy,
                        active = false,
                        onClick = {
                            if (state.textContent.isNotBlank()) {
                                copyToClipboard(context, "Hefimer Text", state.textContent)
                                vm.showToast("Copied to clipboard", ToastType.SUCCESS)
                            }
                        },
                    )
                }
            }

            // Optional language chips strip
            AnimatedVisibility(
                visible = showLangPicker,
                enter = AppMotion.softExpandEnter,
                exit = AppMotion.softExpandExit,
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(HefimerColors.SurfaceElevated)
                        .horizontalScroll(rememberScrollState())
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    val autoSel = state.manualLangId == null
                    Text(
                        text = "Auto",
                        color = if (autoSel) HefimerColors.Black else HefimerColors.White60,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(if (autoSel) HefimerColors.White else HefimerColors.White08)
                            .applePressable {
                                vm.setManualLangId(null)
                                showLangPicker = false
                            }
                            .padding(horizontal = 11.dp, vertical = 5.dp),
                    )
                    CodeHighlighter.SUPPORTED_LANGUAGES.forEach { lang ->
                        val sel = state.manualLangId == lang.id
                        Text(
                            text = lang.label,
                            color = if (sel) HefimerColors.Black else HefimerColors.White60,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier
                                .clip(CircleShape)
                                .background(if (sel) HefimerColors.White else HefimerColors.White08)
                                .applePressable {
                                    vm.setManualLangId(lang.id)
                                    showLangPicker = false
                                }
                                .padding(horizontal = 11.dp, vertical = 5.dp),
                        )
                    }
                }
            }

            Box(Modifier.fillMaxWidth().height(0.5.dp).background(HefimerColors.White10))

            // Code/Text editing area
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 210.dp, max = 320.dp)
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
            ) {
                if (showLineNumbers) {
                    Text(
                        text = (1..lines).joinToString("\n"),
                        color = HefimerColors.White20,
                        fontFamily = JetBrainsMonoFamily,
                        fontSize = 12.sp,
                        lineHeight = 20.sp,
                        textAlign = TextAlign.End,
                        modifier = Modifier.padding(end = 12.dp),
                    )
                }

                if (previewHighlight && state.textContent.isNotBlank()) {
                    val highlighted = remember(state.textContent, activeLang.id) {
                        CodeHighlighter.highlight(state.textContent, activeLang.id)
                    }
                    Text(
                        text = highlighted,
                        fontFamily = JetBrainsMonoFamily,
                        fontSize = 13.sp,
                        lineHeight = 20.sp,
                        color = HefimerColors.White,
                        modifier = Modifier
                            .weight(1f)
                            .applePressable(pressedScale = 0.995f, enableHaptic = false) { previewHighlight = false },
                    )
                } else {
                    BasicTextField(
                        value = state.textContent,
                        onValueChange = { vm.setTextContent(it) },
                        textStyle = TextStyle(
                            fontFamily = JetBrainsMonoFamily,
                            fontSize = 13.sp,
                            lineHeight = 20.sp,
                            color = HefimerColors.White,
                        ),
                        cursorBrush = SolidColor(HefimerColors.White),
                        modifier = Modifier
                            .weight(1f)
                            .heightIn(min = 180.dp),
                        decorationBox = { inner ->
                            if (state.textContent.isEmpty()) {
                                Text(
                                    text = "Paste code, links, or text here...",
                                    fontFamily = JetBrainsMonoFamily,
                                    fontSize = 13.sp,
                                    color = HefimerColors.White30,
                                )
                            }
                            inner()
                        },
                    )
                }
            }

            // Bottom stats bar
            Box(Modifier.fillMaxWidth().height(0.5.dp).background(HefimerColors.White10))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(HefimerColors.SurfaceCard)
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                Text("$lines ${if (lines == 1) "Line" else "Lines"}", color = HefimerColors.White30, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                Text("${state.textContent.length} Chars", color = HefimerColors.White30, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                Text(formatBytes(state.textContent.toByteArray().size.toLong()), color = HefimerColors.White30, fontSize = 10.sp, fontWeight = FontWeight.Bold)
            }
        }

        // Expiry pills
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth(),
        ) {
            UploadProviders.STANDARD_EXPIRE_OPTIONS.forEach { opt ->
                val sel = state.textExpire == opt.value
                Text(
                    text = opt.label,
                    color = if (sel) HefimerColors.Black else HefimerColors.White60,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(if (sel) HefimerColors.White else HefimerColors.White08)
                        .applePressable(pressedScale = 0.94f) { vm.setTextExpire(opt.value) }
                        .padding(horizontal = 14.dp, vertical = 8.dp),
                )
            }
        }

        // Collapsible Advanced Options
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(24.dp))
                .background(HefimerColors.White025)
                .border(1.dp, HefimerColors.White10, RoundedCornerShape(24.dp)),
        ) {
            val chevronRot by animateFloatAsState(
                targetValue = if (showAdvanced) 180f else 0f,
                animationSpec = AppMotion.snappySpring(),
                label = "text_adv_chevron",
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .applePressable(pressedScale = 0.99f) { showAdvanced = !showAdvanced }
                    .padding(horizontal = 16.dp, vertical = 13.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text("Advanced Options", color = HefimerColors.White60, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                Icon(
                    Icons.Rounded.ExpandMore,
                    null,
                    tint = HefimerColors.White50,
                    modifier = Modifier.size(16.dp).graphicsLayer { rotationZ = chevronRot },
                )
            }
            AnimatedVisibility(visible = showAdvanced, enter = AppMotion.softExpandEnter, exit = AppMotion.softExpandExit) {
                Column(
                    modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    HefimerTextField(
                        value = state.textPassword,
                        onValueChange = { vm.setTextPassword(it) },
                        placeholder = "Password (optional)",
                        leadingIcon = Icons.Rounded.Lock,
                    )
                    HefimerToggleRow(
                        title = "Self-Destruct",
                        subtitle = "Delete after one successful read",
                        checked = state.textOneTime,
                        onToggle = { vm.toggleTextOneTime() },
                        icon = Icons.Rounded.DeleteOutline,
                        activeColor = HefimerColors.Red500,
                    )
                }
            }
        }

        HefimerPrimaryButton(
            text = if (state.textLoading) "Generating..." else "Generate Code",
            loading = state.textLoading,
            enabled = state.textContent.isNotBlank() && !state.textLoading,
            onClick = { vm.generateTextDrop() },
        )
    }
}

@Composable
private fun EditorMiniIcon(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    active: Boolean,
    onClick: () -> Unit,
) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .size(32.dp)
            .clip(CircleShape)
            .background(if (active) HefimerColors.White else HefimerColors.White06)
            .border(1.dp, HefimerColors.White10, CircleShape)
            .applePressable(pressedScale = 0.9f, onClick = onClick),
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = if (active) HefimerColors.Black else HefimerColors.White60,
            modifier = Modifier.size(15.dp),
        )
    }
}

@Composable
fun ProviderPickerSheetContent(
    selectedId: String,
    defaultProviderId: String? = null,
    onSetDefault: ((String) -> Unit)? = null,
    onSelect: (String) -> Unit,
    onClose: () -> Unit,
) {
    var expandedId by remember { mutableStateOf<String?>(null) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .fillMaxHeight(0.76f)
            .clip(RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp))
            .background(HefimerColors.SurfaceSheet)
            .border(1.dp, HefimerColors.White12, RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp))
            .navigationBarsPadding(),
    ) {
        // Drag handle pill
        Box(
            modifier = Modifier
                .align(Alignment.CenterHorizontally)
                .padding(top = 10.dp)
                .width(38.dp)
                .height(4.dp)
                .clip(CircleShape)
                .background(HefimerColors.White20),
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "TRANSFER ROUTE",
                    color = HefimerColors.Emerald400,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 2.2.sp,
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text = "Choose an upload provider",
                    color = HefimerColors.White,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    text = "Tap ★ to set default. Larger files auto-route seamlessly.",
                    color = HefimerColors.White40,
                    fontSize = 11.sp,
                )
            }
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(34.dp)
                    .clip(CircleShape)
                    .background(HefimerColors.White06)
                    .border(1.dp, HefimerColors.White10, CircleShape)
                    .applePressable(onClick = onClose),
            ) {
                Icon(Icons.Rounded.Close, "Close", tint = HefimerColors.White60, modifier = Modifier.size(16.dp))
            }
        }

        Box(Modifier.fillMaxWidth().height(0.5.dp).background(HefimerColors.White10))

        LazyColumn(
            modifier = Modifier.fillMaxWidth().weight(1f),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(start = 14.dp, end = 14.dp, top = 12.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            items(UploadProviders.ALL, key = { it.id }) { prov ->
                ProviderRowCard(
                    prov = prov,
                    selected = prov.id == selectedId,
                    isDefault = prov.id == defaultProviderId,
                    expanded = expandedId == prov.id,
                    onSelect = { onSelect(prov.id) },
                    onSetDefault = onSetDefault?.let { cb -> { cb(prov.id) } },
                    onToggleExpand = { expandedId = if (expandedId == prov.id) null else prov.id },
                )
            }
        }
    }
}

@Composable
private fun ProviderRowCard(
    prov: UploadProviderConfig,
    selected: Boolean,
    isDefault: Boolean,
    expanded: Boolean,
    onSelect: () -> Unit,
    onSetDefault: (() -> Unit)?,
    onToggleExpand: () -> Unit,
) {
    val bg by animateColorAsState(
        targetValue = if (selected) HefimerColors.White else HefimerColors.White04,
        animationSpec = tween(220, easing = AppMotion.AppleSilk),
        label = "prov_bg",
    )
    val fg = if (selected) HefimerColors.Black else HefimerColors.White

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(bg)
            .border(1.dp, if (selected) HefimerColors.White else HefimerColors.White10, RoundedCornerShape(20.dp)),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(14.dp))
                    .applePressable(pressedScale = 0.98f, onClick = onSelect)
                    .padding(6.dp),
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(36.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(if (selected) HefimerColors.Black.copy(alpha = 0.08f) else HefimerColors.Black.copy(alpha = 0.45f))
                        .border(1.dp, if (selected) HefimerColors.Black.copy(alpha = 0.14f) else HefimerColors.White10, RoundedCornerShape(12.dp)),
                ) {
                    Icon(
                        imageVector = if (prov.isLargeFile) Icons.Rounded.Inventory2 else Icons.Rounded.UploadFile,
                        contentDescription = null,
                        tint = fg,
                        modifier = Modifier.size(17.dp),
                    )
                }
                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        Text(prov.name, color = fg, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                        if (isDefault) {
                            Text(
                                text = "DEFAULT",
                                color = if (selected) HefimerColors.Black.copy(alpha = 0.7f) else HefimerColors.Emerald400,
                                fontSize = 8.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.sp,
                                modifier = Modifier
                                    .clip(CircleShape)
                                    .background(if (selected) HefimerColors.Black.copy(alpha = 0.12f) else HefimerColors.EmeraldBg)
                                    .padding(horizontal = 6.dp, vertical = 2.dp),
                            )
                        } else if (prov.isLargeFile) {
                            Text(
                                text = "LARGE",
                                color = if (selected) HefimerColors.Black.copy(alpha = 0.6f) else HefimerColors.Emerald300,
                                fontSize = 8.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.sp,
                                modifier = Modifier
                                    .clip(CircleShape)
                                    .background(if (selected) HefimerColors.Black.copy(alpha = 0.1f) else HefimerColors.EmeraldBg)
                                    .padding(horizontal = 6.dp, vertical = 2.dp),
                            )
                        }
                    }
                    Text(
                        text = prov.maxSizeLabel,
                        color = fg.copy(alpha = 0.5f),
                        fontFamily = JetBrainsMonoFamily,
                        fontSize = 10.sp,
                    )
                }
                if (selected) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(20.dp)
                            .clip(CircleShape)
                            .background(HefimerColors.Black),
                    ) {
                        Icon(Icons.Rounded.Check, null, tint = HefimerColors.White, modifier = Modifier.size(12.dp))
                    }
                }
            }

            if (onSetDefault != null) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(34.dp)
                        .clip(RoundedCornerShape(11.dp))
                        .border(1.dp, if (selected) HefimerColors.Black.copy(alpha = 0.14f) else HefimerColors.White10, RoundedCornerShape(11.dp))
                        .applePressable(pressedScale = 0.90f, onClick = onSetDefault),
                ) {
                    Icon(
                        imageVector = if (isDefault) Icons.Rounded.Star else Icons.Rounded.StarOutline,
                        contentDescription = "Set as default",
                        tint = if (isDefault) HefimerColors.Amber400 else fg.copy(alpha = 0.5f),
                        modifier = Modifier.size(16.dp),
                    )
                }
            }

            val rot by animateFloatAsState(if (expanded) 180f else 0f, AppMotion.snappySpring(), label = "exp_rot")
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(34.dp)
                    .clip(RoundedCornerShape(11.dp))
                    .border(1.dp, if (selected) HefimerColors.Black.copy(alpha = 0.14f) else HefimerColors.White10, RoundedCornerShape(11.dp))
                    .applePressable(pressedScale = 0.92f, onClick = onToggleExpand),
            ) {
                Icon(
                    Icons.Rounded.ExpandMore,
                    null,
                    tint = fg.copy(alpha = 0.65f),
                    modifier = Modifier.size(17.dp).graphicsLayer { rotationZ = rot },
                )
            }
        }

        AnimatedVisibility(visible = expanded, enter = AppMotion.softExpandEnter, exit = AppMotion.softExpandExit) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 16.dp, end = 16.dp, bottom = 12.dp),
                verticalArrangement = Arrangement.spacedBy(5.dp),
            ) {
                Box(Modifier.fillMaxWidth().height(0.5.dp).background(fg.copy(alpha = 0.1f)))
                Spacer(Modifier.height(4.dp))
                PolicyLine("EXPIRY", prov.expiry, fg)
                PolicyLine("DELIVERY", prov.speed, fg)
                PolicyLine("NOTES", prov.features, fg)
            }
        }
    }
}

@Composable
private fun PolicyLine(label: String, value: String, fg: Color) {
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(
            text = label,
            color = fg.copy(alpha = 0.45f),
            fontSize = 9.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.2.sp,
            modifier = Modifier.width(66.dp),
        )
        Text(
            text = value,
            color = fg.copy(alpha = 0.72f),
            fontSize = 11.sp,
            lineHeight = 15.sp,
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun SecretR2PasscodeModal(
    onDismiss: () -> Unit,
    onSubmit: (String) -> Unit,
) {
    var pass by remember { mutableStateOf("") }
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Box(modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp)) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(32.dp))
                    .background(HefimerColors.SurfaceCard)
                    .border(1.dp, HefimerColors.EmeraldBorder, RoundedCornerShape(32.dp))
                    .padding(24.dp),
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(56.dp)
                        .clip(CircleShape)
                        .background(HefimerColors.EmeraldBg)
                        .border(1.dp, HefimerColors.EmeraldBorder, CircleShape),
                ) {
                    Icon(Icons.Rounded.Shield, null, tint = HefimerColors.Emerald400, modifier = Modifier.size(26.dp))
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "SECRET R2 ACCESS",
                        color = HefimerColors.Emerald400,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 2.4.sp,
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = "Enter secret passcode for private Cloudflare R2 storage",
                        color = HefimerColors.White40,
                        fontSize = 12.sp,
                        textAlign = TextAlign.Center,
                    )
                }
                HefimerTextField(
                    value = pass,
                    onValueChange = { pass = it },
                    placeholder = "Passcode...",
                    leadingIcon = Icons.Rounded.Lock,
                    isPassword = true,
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .clip(CircleShape)
                            .background(HefimerColors.White06)
                            .border(1.dp, HefimerColors.White10, CircleShape)
                            .applePressable(onClick = onDismiss),
                    ) {
                        Text("Cancel", color = HefimerColors.White60, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    }
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .clip(CircleShape)
                            .background(HefimerColors.Emerald400)
                            .applePressable { onSubmit(pass) },
                    ) {
                        Text("Unlock", color = HefimerColors.Black, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
fun HefimerTextField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier,
    leadingIcon: androidx.compose.ui.graphics.vector.ImageVector? = null,
    isPassword: Boolean = false,
    mono: Boolean = false,
    singleLine: Boolean = true,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(HefimerColors.Black.copy(alpha = 0.65f))
            .border(1.dp, HefimerColors.White12, RoundedCornerShape(18.dp))
            .padding(horizontal = 14.dp, vertical = 14.dp),
    ) {
        if (leadingIcon != null) {
            Icon(leadingIcon, null, tint = HefimerColors.White30, modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(10.dp))
        }
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            singleLine = singleLine,
            visualTransformation = if (isPassword) PasswordVisualTransformation() else androidx.compose.ui.text.input.VisualTransformation.None,
            textStyle = TextStyle(
                fontFamily = if (mono) JetBrainsMonoFamily else UbuntuFontFamily,
                fontSize = 13.5.sp,
                fontWeight = FontWeight.Medium,
                color = HefimerColors.White,
            ),
            cursorBrush = SolidColor(HefimerColors.White),
            modifier = Modifier.weight(1f),
            decorationBox = { inner ->
                if (value.isEmpty()) {
                    Text(
                        text = placeholder,
                        fontFamily = if (mono) JetBrainsMonoFamily else UbuntuFontFamily,
                        fontSize = 13.5.sp,
                        color = HefimerColors.White30,
                    )
                }
                inner()
            },
        )
    }
}
