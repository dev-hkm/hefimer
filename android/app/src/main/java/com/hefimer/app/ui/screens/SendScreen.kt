package com.hefimer.app.ui.screens

import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateContentSize
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
import androidx.compose.material.icons.rounded.Edit
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
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
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
import com.hefimer.app.ui.components.CodeLineNumberGutter
import com.hefimer.app.ui.components.CodeSyntaxVisualTransformation
import com.hefimer.app.ui.components.DropResultCard
import com.hefimer.app.ui.components.HefimerCollapsibleScaffold
import com.hefimer.app.ui.components.HefimerHeaderIconButton
import com.hefimer.app.ui.components.HefimerOrbitLogo
import com.hefimer.app.ui.components.HefimerPrimaryButton
import com.hefimer.app.ui.components.HefimerToggleRow
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
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
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
            kicker = if (state.isR2Mode && state.subMode == SendSubMode.FILE) {
                tr("Secret R2 Mode", "Chế độ Secret R2")
            } else {
                tr("Create a drop", "Tạo phiên chia sẻ")
            },
            title = tr("Send", "Gửi"),
            subtitle = tr(
                "Share a file or text that disappears when its moment is over.",
                "Chia sẻ tệp hoặc văn bản và tự hủy ngay khi hết hạn.",
            ),
            accentColor = accent,
            showOrbitMark = true,
            headerActions = {
                HefimerHeaderIconButton(
                    icon = Icons.Rounded.QrCodeScanner,
                    contentDescription = tr("Scan QR", "Quét QR"),
                    modifier = Modifier.heroSource("btn_qr_send", heroBounds),
                    onClick = { vm.openOverlay(AppOverlay.QrScanner(ScanTarget.AUTO), "btn_qr_send") },
                )
                HefimerHeaderIconButton(
                    icon = Icons.Rounded.History,
                    contentDescription = tr("History", "Lịch sử"),
                    modifier = Modifier.heroSource("btn_history_send", heroBounds),
                    onClick = { vm.openOverlay(AppOverlay.History, "btn_history_send") },
                )
                HefimerHeaderIconButton(
                    icon = Icons.Rounded.Settings,
                    contentDescription = tr("Settings", "Cài đặt"),
                    onClick = { vm.openSettings() },
                )
            },
        ) {
            // Concentric sliding File / Text segmented toggle
            AppleSegmentedControl(
                options = listOf(SendSubMode.FILE, SendSubMode.TEXT),
                selected = state.subMode,
                label = { if (it == SendSubMode.FILE) tr("Send File", "Gửi tệp") else tr("Send Text", "Gửi chữ") },
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

    val reduceMotion = rememberReduceMotion()
    AnimatedContent(
        targetState = state.fileResultCode.isNotBlank(),
        transitionSpec = { AppMotion.sectionTransform(reduceMotion) },
        label = "send_file_result_switch",
    ) { hasResult ->
        if (hasResult) {
            DropResultCard(
                code = state.fileResultCode,
                heading = if (state.fileResultIsR2) tr("Secret R2 Code", "Mã Secret R2") else tr("Your Drop Code", "Mã Drop của bạn"),
                subtitle = state.fileResultName,
                expiresAt = state.fileResultExpiresAt,
                isR2 = state.fileResultIsR2,
                onBack = { vm.resetFileResult(clearSelection = false) },
                onReset = { vm.resetFileResult(clearSelection = true) },
                onToast = { msg, err -> vm.showToast(msg, if (err) ToastType.ERROR else ToastType.SUCCESS) },
            )
            return@AnimatedContent
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
                        text = tr("SECRET CLOUDFLARE R2", "CLOUDFLARE R2 BÍ MẬT"),
                        color = HefimerColors.Emerald400.copy(alpha = 0.85f),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 2.sp,
                    )
                }
                Text(
                    text = tr("EXIT", "THOÁT"),
                    color = HefimerColors.White60,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.4.sp,
                    modifier = Modifier
                        .appleGlassSurface(
                            shape = CircleShape,
                            variant = AppleGlassVariant.Clear,
                            elevation = 0.dp,
                        )
                        .applePressable(pressedScale = 0.94f) { vm.exitR2Mode() }
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                )
            }
        }

        // File Dropzone Card (hugs picked file snugly with zero dead space)
        val dropzoneMode = when {
            state.fileUploading -> "uploading"
            state.pickedFiles.isNotEmpty() -> "picked"
            else -> "empty"
        }
        var showRenamePickedDialog by remember { mutableStateOf(false) }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .animateContentSize(AppMotion.layoutSpring())
                .appleGlassSurface(
                    shape = RoundedCornerShape(36.dp),
                    variant = AppleGlassVariant.Regular,
                    accentBorderTint = if (isR2) HefimerColors.Emerald400 else null,
                    accentFillTint = if (isR2) HefimerColors.EmeraldBg.copy(alpha = 0.25f) else null,
                    elevation = 12.dp,
                )
                .applePressable(enabled = !state.fileUploading, pressedScale = 0.985f) {
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
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 24.dp, vertical = 22.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                        ) {
                            // Enlarged spinning Hefimer mark (no outer ring)
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier.size(72.dp),
                            ) {
                                HefimerOrbitLogo(
                                    size = 64.dp,
                                    tint = accent,
                                    spinning = true,
                                    spinDurationMs = 2_400,
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
                                    .appleGlassSurface(
                                        shape = CircleShape,
                                        variant = AppleGlassVariant.Clear,
                                        accentBorderTint = HefimerColors.Red400,
                                        accentFillTint = HefimerColors.RedBg,
                                        elevation = 0.dp,
                                    )
                                    .applePressable(pressedScale = 0.94f) { vm.cancelFileUpload() }
                                    .padding(horizontal = 16.dp, vertical = 8.dp),
                            ) {
                                Icon(Icons.Rounded.Close, null, tint = HefimerColors.Red400, modifier = Modifier.size(13.dp))
                                Text(
                                    text = tr("CANCEL UPLOAD", "HỦY TẢI LÊN"),
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
                        val single = state.pickedFiles.singleOrNull()
                        val label = if (state.pickedFiles.size > 1) {
                            tr("${state.pickedFiles.size} files selected", "Đã chọn ${state.pickedFiles.size} tệp")
                        } else {
                            single?.name.orEmpty()
                        }

                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 18.dp, vertical = 16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(14.dp),
                            ) {
                                if (single != null && single.mimeType.startsWith("image/") && !state.isFolderMode) {
                                    AsyncImage(
                                        model = single.uri,
                                        contentDescription = single.name,
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier
                                            .size(54.dp)
                                            .clip(RoundedCornerShape(16.dp))
                                            .background(HefimerColors.Black)
                                            .border(
                                                1.dp,
                                                if (isR2) HefimerColors.Emerald400.copy(alpha = 0.5f) else HefimerColors.White15,
                                                RoundedCornerShape(16.dp),
                                            ),
                                    )
                                } else {
                                    Box(
                                        contentAlignment = Alignment.Center,
                                        modifier = Modifier
                                            .size(52.dp)
                                            .appleGlassSurface(
                                                shape = CircleShape,
                                                variant = AppleGlassVariant.Clear,
                                                accentBorderTint = if (isR2) HefimerColors.Emerald400 else null,
                                                elevation = 0.dp,
                                            ),
                                    ) {
                                        Icon(
                                            imageVector = if (state.isFolderMode) Icons.Rounded.Folder else Icons.Rounded.Description,
                                            contentDescription = null,
                                            tint = accent,
                                            modifier = Modifier.size(23.dp),
                                        )
                                    }
                                }

                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = label,
                                        color = HefimerColors.White,
                                        fontSize = 14.5.sp,
                                        lineHeight = 19.sp,
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis,
                                    )
                                    Spacer(Modifier.height(3.dp))
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    ) {
                                        Text(
                                            text = formatBytes(totalSize),
                                            color = accent.copy(alpha = 0.78f),
                                            fontFamily = JetBrainsMonoFamily,
                                            fontSize = 11.5.sp,
                                        )
                                        Text("•", color = HefimerColors.White20, fontSize = 10.sp)
                                        Text(
                                            text = tr("Tap to change", "Chạm để đổi"),
                                            color = HefimerColors.White45,
                                            fontSize = 11.sp,
                                        )
                                    }
                                }

                                if (single != null && !state.isFolderMode) {
                                    Box(
                                        contentAlignment = Alignment.Center,
                                        modifier = Modifier
                                            .size(32.dp)
                                            .appleGlassSurface(CircleShape, AppleGlassVariant.Clear, elevation = 0.dp)
                                            .applePressable(pressedScale = 0.88f) { showRenamePickedDialog = true },
                                    ) {
                                        Icon(Icons.Rounded.Edit, tr("Rename", "Đổi tên"), tint = HefimerColors.White80, modifier = Modifier.size(15.dp))
                                    }
                                }

                                Box(
                                    contentAlignment = Alignment.Center,
                                    modifier = Modifier
                                        .size(32.dp)
                                        .clip(CircleShape)
                                        .background(HefimerColors.White)
                                        .applePressable(pressedScale = 0.88f) {
                                            vm.requestConfirm(
                                                title = tr("Remove Selected File(s)?", "Xóa tệp đã chọn?"),
                                                message = tr(
                                                    "Clear \"$label\" from the upload queue?",
                                                    "Bỏ \"$label\" khỏi danh sách chuẩn bị tải lên?",
                                                ),
                                                confirmLabel = tr("Remove", "Xóa"),
                                                onConfirm = { vm.clearPickedFiles() },
                                            )
                                        },
                                ) {
                                    Icon(Icons.Rounded.Close, tr("Remove", "Xóa"), tint = HefimerColors.Black, modifier = Modifier.size(16.dp))
                                }
                            }

                            if (state.pickedFiles.size > 1) {
                                FlowRow(
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    verticalArrangement = Arrangement.spacedBy(6.dp),
                                ) {
                                    state.pickedFiles.forEachIndexed { idx, file ->
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(5.dp),
                                            modifier = Modifier
                                                .appleGlassSurface(CircleShape, AppleGlassVariant.Clear, elevation = 0.dp)
                                                .padding(start = 10.dp, end = 6.dp, top = 5.dp, bottom = 5.dp),
                                        ) {
                                            Text(
                                                text = file.name,
                                                color = HefimerColors.White75,
                                                fontSize = 10.sp,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis,
                                                modifier = Modifier.width(120.dp),
                                            )
                                            Icon(
                                                imageVector = Icons.Rounded.Close,
                                                contentDescription = tr("Remove", "Bỏ"),
                                                tint = HefimerColors.White50,
                                                modifier = Modifier
                                                    .size(14.dp)
                                                    .applePressable(pressedScale = 0.85f) { vm.removePickedFileAt(idx) },
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    else -> {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 152.dp)
                                .padding(horizontal = 22.dp, vertical = 24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                        ) {
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier
                                    .size(56.dp)
                                    .appleGlassSurface(
                                        shape = CircleShape,
                                        variant = AppleGlassVariant.Clear,
                                        accentBorderTint = if (isR2) HefimerColors.Emerald400 else null,
                                        elevation = 0.dp,
                                    ),
                            ) {
                                Icon(
                                    imageVector = when {
                                        isR2 -> Icons.Rounded.Shield
                                        state.isFolderMode -> Icons.Rounded.CreateNewFolder
                                        else -> Icons.Rounded.UploadFile
                                    },
                                    contentDescription = null,
                                    tint = accent.copy(alpha = 0.9f),
                                    modifier = Modifier.size(25.dp),
                                )
                            }
                            Spacer(Modifier.height(12.dp))
                            Text(
                                text = if (state.isFolderMode) tr("Tap to select a folder", "Chạm để chọn thư mục") else tr("Tap to select file(s)", "Chạm để chọn tệp"),
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
                                        .appleGlassSurface(
                                            shape = CircleShape,
                                            variant = AppleGlassVariant.Clear,
                                            accentBorderTint = if (isR2) HefimerColors.Emerald400 else null,
                                            elevation = 0.dp,
                                        )
                                        .padding(horizontal = 8.dp, vertical = 3.dp),
                                ) {
                                    Box(Modifier.size(5.dp).clip(CircleShape).background(if (isR2) HefimerColors.Emerald400 else HefimerColors.White80))
                                    Text(
                                        text = if (isR2) "Cloudflare R2" else providerCfg.name,
                                        color = if (isR2) HefimerColors.Emerald400 else HefimerColors.White85,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                    )
                                }
                                Text(
                                    text = "${tr("Max", "Tối đa")} ${if (isR2) "5 GB" else providerCfg.localizedMaxSize()}",
                                    color = HefimerColors.White45,
                                    fontFamily = JetBrainsMonoFamily,
                                    fontSize = 10.sp,
                                )
                                Text("•", color = HefimerColors.White20, fontSize = 10.sp)
                                Text(
                                    text = "${tr("Today", "Hôm nay")} ${formatBytes(todayBytes)}",
                                    color = HefimerColors.White45,
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
                    .appleGlassSurface(
                        shape = RoundedCornerShape(28.dp),
                        variant = AppleGlassVariant.Regular,
                        elevation = 8.dp,
                    )
                    .applePressable(pressedScale = 0.98f, onClick = onOpenProviderPicker)
                    .padding(horizontal = 14.dp, vertical = 12.dp),
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(40.dp)
                        .appleGlassSurface(
                            shape = RoundedCornerShape(15.dp),
                            variant = AppleGlassVariant.Clear,
                            elevation = 0.dp,
                        ),
                ) {
                    Icon(Icons.Rounded.Inventory2, null, tint = HefimerColors.White, modifier = Modifier.size(18.dp))
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = tr("UPLOAD PROVIDER", "MÁY CHỦ TẢI LÊN"),
                        color = HefimerColors.White45,
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
                            text = providerCfg.localizedMaxSize(),
                            color = HefimerColors.White75,
                            fontFamily = JetBrainsMonoFamily,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier
                                .appleGlassSurface(
                                    shape = CircleShape,
                                    variant = AppleGlassVariant.Clear,
                                    elevation = 0.dp,
                                )
                                .padding(horizontal = 7.dp, vertical = 2.dp),
                        )
                    }
                }
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(34.dp)
                        .appleGlassSurface(
                            shape = CircleShape,
                            variant = AppleGlassVariant.Clear,
                            elevation = 0.dp,
                        ),
                ) {
                    Icon(Icons.Rounded.ExpandMore, null, tint = HefimerColors.White60, modifier = Modifier.size(18.dp))
                }
            }
        } else {
            // Secret R2 Optional custom name & note
            HefimerTextField(
                value = state.r2CustomName,
                onValueChange = { vm.setR2CustomName(it) },
                placeholder = tr("Custom file name (optional)", "Tên tệp tùy chỉnh (không bắt buộc)"),
            )
            HefimerTextField(
                value = state.r2Description,
                onValueChange = { vm.setR2Description(it) },
                placeholder = tr("Secret note / description (optional)", "Ghi chú bí mật / mô tả (không bắt buộc)"),
            )
        }

        // Advanced Options + File/Folder Toggle Accordion
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .appleGlassSurface(
                    shape = RoundedCornerShape(26.dp),
                    variant = AppleGlassVariant.Regular,
                    accentBorderTint = if (isR2) HefimerColors.Emerald400 else null,
                    elevation = 6.dp,
                ),
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
                        text = tr("Advanced Options", "Tùy chọn nâng cao"),
                        color = HefimerColors.White70,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                    )
                    Icon(
                        imageVector = Icons.Rounded.ExpandMore,
                        contentDescription = null,
                        tint = HefimerColors.White55,
                        modifier = Modifier
                            .size(16.dp)
                            .graphicsLayer { rotationZ = chevronRot },
                    )
                }

                // Inline File / Folder mode pill
                Row(
                    modifier = Modifier
                        .appleGlassSurface(
                            shape = RoundedCornerShape(14.dp),
                            variant = AppleGlassVariant.Clear,
                            elevation = 0.dp,
                        )
                        .padding(3.dp),
                    horizontalArrangement = Arrangement.spacedBy(2.dp),
                ) {
                    listOf(false to tr("FILE", "TỆP"), true to tr("FOLDER", "THƯ MỤC")).forEach { (folder, lbl) ->
                        val active = state.isFolderMode == folder
                        val tabBg by animateColorAsState(
                            targetValue = if (active) HefimerColors.White else Color.Transparent,
                            animationSpec = tween(AppMotion.Micro, easing = AppMotion.AppleSilk),
                            label = "folder_tab_bg",
                        )
                        val tabFg by animateColorAsState(
                            targetValue = if (active) HefimerColors.Black else HefimerColors.White45,
                            animationSpec = tween(AppMotion.Micro, easing = AppMotion.AppleSilk),
                            label = "folder_tab_fg",
                        )
                        Text(
                            text = lbl,
                            color = tabFg,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp,
                            modifier = Modifier
                                .clip(RoundedCornerShape(10.dp))
                                .background(tabBg)
                                .applePressable(pressedScale = 0.93f) { vm.setFolderMode(folder) }
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
                            text = tr("SELF-DESTRUCT TIMER", "THỜI GIAN TỰ HỦY"),
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
                                val chipFg by animateColorAsState(
                                    targetValue = if (selected) HefimerColors.Black else HefimerColors.White60,
                                    animationSpec = tween(AppMotion.Micro, easing = AppMotion.AppleSilk),
                                    label = "exp_fg",
                                )
                                val chipMod = if (selected) {
                                    Modifier.clip(CircleShape).background(accent)
                                } else {
                                    Modifier.appleGlassSurface(CircleShape, AppleGlassVariant.Clear, elevation = 0.dp)
                                }
                                Text(
                                    text = opt.localizedLabel(),
                                    color = chipFg,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = chipMod
                                        .applePressable(pressedScale = 0.94f) { vm.setFileExpire(opt.value) }
                                        .padding(horizontal = 14.dp, vertical = 8.dp),
                                )
                            }
                        }
                    }

                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            text = tr("PASSWORD (OPTIONAL)", "MẬT KHẨU (KHÔNG BẮT BUỘC)"),
                            color = HefimerColors.White30,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.8.sp,
                        )
                        HefimerTextField(
                            value = state.filePassword,
                            onValueChange = { vm.setFilePassword(it) },
                            placeholder = tr("Set a password...", "Đặt mật khẩu..."),
                            leadingIcon = Icons.Rounded.Lock,
                        )
                    }

                    HefimerToggleRow(
                        title = tr("Self-Destruct", "Tự hủy sau 1 lần xem"),
                        subtitle = tr("Delete after one successful read", "Xóa ngay sau khi người nhận mở 1 lần"),
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
            text = if (state.fileUploading) {
                tr("Uploading...", "Đang tải lên...")
            } else if (isR2) {
                tr("Upload to Secret R2", "Tải lên Secret R2")
            } else {
                tr("Upload & Generate Code", "Tải lên & Tạo mã")
            },
            loading = state.fileUploading,
            enabled = state.pickedFiles.isNotEmpty() && !state.fileUploading,
            accentColor = accent,
            onClick = { vm.uploadFiles() },
        )

        if (showRenamePickedDialog && state.pickedFiles.size == 1) {
            RenamePickedFileDialog(
                initialName = state.pickedFiles.first().name,
                onDismiss = { showRenamePickedDialog = false },
                onConfirm = { nextName ->
                    vm.renamePickedSingleFile(nextName)
                    showRenamePickedDialog = false
                },
            )
        }
        }
    }
}

@Composable
private fun RenamePickedFileDialog(
    initialName: String,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit,
) {
    var nameInput by remember(initialName) { mutableStateOf(initialName) }
    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Box(modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp)) {
            Column(
                verticalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .appleGlassSurface(
                        shape = RoundedCornerShape(34.dp),
                        variant = AppleGlassVariant.Elevated,
                        elevation = 24.dp,
                    )
                    .padding(22.dp),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Rounded.Edit, null, tint = HefimerColors.White, modifier = Modifier.size(16.dp))
                    Text(tr("RENAME FILE BEFORE UPLOAD", "ĐỔI TÊN TỆP TRƯỚC KHI GỬI"), color = HefimerColors.White, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.8.sp)
                }
                HefimerTextField(
                    value = nameInput,
                    onValueChange = { nameInput = it },
                    placeholder = tr("filename.ext", "ten_tep.ext"),
                    mono = true,
                )
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    com.hefimer.app.ui.components.HefimerSecondaryButton(tr("Cancel", "Hủy"), height = 48.dp, modifier = Modifier.weight(1f), onClick = onDismiss)
                    HefimerPrimaryButton(
                        text = tr("Save Name", "Lưu tên"),
                        height = 48.dp,
                        modifier = Modifier.weight(1f),
                        onClick = { if (nameInput.isNotBlank()) onConfirm(nameInput.trim()) },
                    )
                }
            }
        }
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
    val reduceMotion = rememberReduceMotion()
    var showLineNumbers by remember { mutableStateOf(false) }
    var showLangPicker by remember { mutableStateOf(false) }
    var showAdvanced by remember { mutableStateOf(false) }
    var liveHighlight by remember { mutableStateOf(true) }

    AnimatedContent(
        targetState = state.textResultCode.isNotBlank(),
        transitionSpec = { AppMotion.sectionTransform(reduceMotion) },
        label = "send_text_result_switch",
    ) { hasResult ->
        if (hasResult) {
            DropResultCard(
                code = state.textResultCode,
                heading = tr("Your Drop Code", "Mã Drop của bạn"),
                expiresAt = state.textResultExpiresAt,
                onBack = { vm.resetTextResult(clearContent = false) },
                onReset = { vm.resetTextResult(clearContent = true) },
                onToast = { msg, err -> vm.showToast(msg, if (err) ToastType.ERROR else ToastType.SUCCESS) },
            )
            return@AnimatedContent
        }

        val detected = remember(state.textContent) { CodeHighlighter.detectLanguage(state.textContent) }
        val activeLang = state.manualLangId?.let { id ->
            CodeHighlighter.SUPPORTED_LANGUAGES.firstOrNull { it.id == id }
        } ?: detected
        val lines = remember(state.textContent) { maxOf(1, state.textContent.count { it == '\n' } + 1) }

        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        // Editor Card with Apple Glass optical housing
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .appleGlassSurface(
                    shape = RoundedCornerShape(32.dp),
                    variant = AppleGlassVariant.Regular,
                    elevation = 12.dp,
                ),
        ) {
            // Top bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(HefimerColors.White04)
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier
                        .appleGlassSurface(
                            shape = CircleShape,
                            variant = AppleGlassVariant.Clear,
                            elevation = 0.dp,
                        )
                        .applePressable(pressedScale = 0.94f) { showLangPicker = !showLangPicker }
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                ) {
                    Text(
                        text = activeLang.localizedLabel().uppercase(),
                        color = HefimerColors.White75,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.6.sp,
                    )
                    Icon(Icons.Rounded.Code, null, tint = HefimerColors.White55, modifier = Modifier.size(12.dp))
                }

                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    EditorMiniIcon(
                        icon = if (showLineNumbers) Icons.Rounded.VisibilityOff else Icons.Rounded.Visibility,
                        active = showLineNumbers,
                        onClick = { showLineNumbers = !showLineNumbers },
                    )
                    EditorMiniIcon(
                        icon = Icons.Rounded.Code,
                        active = liveHighlight,
                        onClick = { liveHighlight = !liveHighlight },
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
                                    }.onSuccess { outcome ->
                                        vm.showToast(tr("Saved ${outcome.fileName} to Downloads", "Đã lưu ${outcome.fileName} vào Tải xuống"), ToastType.SUCCESS)
                                        vm.notifyFileSaved(outcome)
                                    }.onFailure {
                                        vm.showToast(it.message ?: tr("Save failed", "Lưu thất bại"), ToastType.ERROR)
                                    }
                                }
                            }
                        },
                    )
                    EditorMiniIcon(
                        icon = Icons.Rounded.DeleteOutline,
                        active = false,
                        onClick = {
                            if (state.textContent.isNotBlank()) {
                                vm.requestConfirm(
                                    title = tr("Clear Editor Text?", "Xóa toàn bộ văn bản?"),
                                    message = tr(
                                        "This will erase all ${state.textContent.length} characters currently in the editor.",
                                        "Thao tác này sẽ xóa toàn bộ ${state.textContent.length} ký tự hiện có trong khung soạn thảo.",
                                    ),
                                    confirmLabel = tr("Clear All", "Xóa hết"),
                                    onConfirm = { vm.setTextContent("") },
                                )
                            }
                        },
                    )
                    EditorMiniIcon(
                        icon = Icons.Rounded.ContentCopy,
                        active = false,
                        onClick = {
                            if (state.textContent.isNotBlank()) {
                                val ok = copyToClipboard(context, "Hefimer Text", state.textContent)
                                if (ok) {
                                    vm.showToast(tr("Copied to clipboard", "Đã sao chép"), ToastType.SUCCESS)
                                } else {
                                    vm.showToast(tr("Text too large for clipboard", "Văn bản quá lớn cho bộ nhớ tạm"), ToastType.ERROR)
                                }
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
                        .background(HefimerColors.White03)
                        .horizontalScroll(rememberScrollState())
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    val autoSel = state.manualLangId == null
                    Text(
                        text = tr("Auto", "Tự động"),
                        color = if (autoSel) HefimerColors.Black else HefimerColors.White70,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier
                            .then(
                                if (autoSel) Modifier.clip(CircleShape).background(HefimerColors.White)
                                else Modifier.appleGlassSurface(CircleShape, AppleGlassVariant.Clear, elevation = 0.dp),
                            )
                            .applePressable(pressedScale = 0.94f) {
                                vm.setManualLangId(null)
                                showLangPicker = false
                            }
                            .padding(horizontal = 11.dp, vertical = 5.dp),
                    )
                    CodeHighlighter.SUPPORTED_LANGUAGES.forEach { lang ->
                        val sel = state.manualLangId == lang.id
                        Text(
                            text = lang.localizedLabel(),
                            color = if (sel) HefimerColors.Black else HefimerColors.White70,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier
                                .then(
                                    if (sel) Modifier.clip(CircleShape).background(HefimerColors.White)
                                    else Modifier.appleGlassSurface(CircleShape, AppleGlassVariant.Clear, elevation = 0.dp),
                                )
                                .applePressable(pressedScale = 0.94f) {
                                    vm.setManualLangId(lang.id)
                                    showLangPicker = false
                                }
                                .padding(horizontal = 11.dp, vertical = 5.dp),
                        )
                    }
                }
            }

            Box(Modifier.fillMaxWidth().height(0.5.dp).background(HefimerColors.White10))

            // Code/Text editing area with exact TextLayoutResult-driven line numbers
            var editorLayoutResult by remember { mutableStateOf<TextLayoutResult?>(null) }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 210.dp, max = 320.dp)
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
            ) {
                if (showLineNumbers) {
                    CodeLineNumberGutter(
                        text = state.textContent,
                        textLayoutResult = editorLayoutResult,
                        color = HefimerColors.White30,
                    )
                }

                val syntaxTransform = remember(activeLang.id, liveHighlight) {
                    if (liveHighlight && activeLang.id != "text") {
                        CodeSyntaxVisualTransformation(activeLang.id)
                    } else {
                        VisualTransformation.None
                    }
                }
                BasicTextField(
                    value = state.textContent,
                    onValueChange = { vm.setTextContent(it) },
                    visualTransformation = syntaxTransform,
                    onTextLayout = { editorLayoutResult = it },
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
                                text = tr("Paste code, links, or text here...", "Dán code, liên kết hoặc văn bản vào đây..."),
                                fontFamily = JetBrainsMonoFamily,
                                fontSize = 13.sp,
                                color = HefimerColors.White30,
                            )
                        }
                        inner()
                    },
                )
            }

            // Bottom stats bar
            Box(Modifier.fillMaxWidth().height(0.5.dp).background(HefimerColors.White10))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(HefimerColors.White04)
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                Text(
                    text = "$lines ${tr(if (lines == 1) "Line" else "Lines", "Dòng")}",
                    color = HefimerColors.White40,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    text = "${state.textContent.length} ${tr("Chars", "Ký tự")}",
                    color = HefimerColors.White40,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                )
                Text(formatBytes(state.textContent.toByteArray().size.toLong()), color = HefimerColors.White40, fontSize = 10.sp, fontWeight = FontWeight.Bold)
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
                val chipFg by animateColorAsState(
                    targetValue = if (sel) HefimerColors.Black else HefimerColors.White60,
                    animationSpec = tween(AppMotion.Micro, easing = AppMotion.AppleSilk),
                    label = "txt_exp_fg",
                )
                val chipMod = if (sel) {
                    Modifier.clip(CircleShape).background(HefimerColors.White)
                } else {
                    Modifier.appleGlassSurface(CircleShape, AppleGlassVariant.Clear, elevation = 0.dp)
                }
                Text(
                    text = opt.localizedLabel(),
                    color = chipFg,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = chipMod
                        .applePressable(pressedScale = 0.94f) { vm.setTextExpire(opt.value) }
                        .padding(horizontal = 14.dp, vertical = 8.dp),
                )
            }
        }

        // Collapsible Advanced Options
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .appleGlassSurface(
                    shape = RoundedCornerShape(26.dp),
                    variant = AppleGlassVariant.Regular,
                    elevation = 6.dp,
                ),
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
                Text(tr("Advanced Options", "Tùy chọn nâng cao"), color = HefimerColors.White70, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                Icon(
                    Icons.Rounded.ExpandMore,
                    null,
                    tint = HefimerColors.White55,
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
                        placeholder = tr("Password (optional)", "Mật khẩu (không bắt buộc)"),
                        leadingIcon = Icons.Rounded.Lock,
                    )
                    HefimerToggleRow(
                        title = tr("Self-Destruct", "Tự hủy sau 1 lần xem"),
                        subtitle = tr("Delete after one successful read", "Xóa ngay sau khi người nhận mở 1 lần"),
                        checked = state.textOneTime,
                        onToggle = { vm.toggleTextOneTime() },
                        icon = Icons.Rounded.DeleteOutline,
                        activeColor = HefimerColors.Red500,
                    )
                }
            }
        }

        HefimerPrimaryButton(
            text = if (state.textLoading) tr("Generating...", "Đang tạo mã...") else tr("Generate Code", "Tạo mã chia sẻ"),
            loading = state.textLoading,
            enabled = state.textContent.isNotBlank() && !state.textLoading,
            onClick = { vm.generateTextDrop() },
        )
        }
    }
}

@Composable
private fun EditorMiniIcon(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    active: Boolean,
    onClick: () -> Unit,
) {
    val bg by animateColorAsState(
        targetValue = if (active) HefimerColors.White else Color.Transparent,
        animationSpec = tween(AppMotion.Micro, easing = AppMotion.AppleSilk),
        label = "editor_icon_bg",
    )
    val tint by animateColorAsState(
        targetValue = if (active) HefimerColors.Black else HefimerColors.White70,
        animationSpec = tween(AppMotion.Micro, easing = AppMotion.AppleSilk),
        label = "editor_icon_tint",
    )
    val mod = if (active) {
        Modifier.size(32.dp).clip(CircleShape).background(bg)
    } else {
        Modifier.size(32.dp).appleGlassSurface(CircleShape, AppleGlassVariant.Clear, elevation = 0.dp)
    }
    Box(
        contentAlignment = Alignment.Center,
        modifier = mod.applePressable(pressedScale = 0.88f, onClick = onClick),
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = tint,
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
            .appleGlassSurface(
                shape = RoundedCornerShape(topStart = 36.dp, topEnd = 36.dp),
                variant = AppleGlassVariant.Elevated,
                elevation = 24.dp,
            )
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
                    text = tr("TRANSFER ROUTE", "TUYẾN LƯU TRỮ"),
                    color = HefimerColors.White45,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 2.2.sp,
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text = tr("Choose an upload provider", "Chọn máy chủ tải lên"),
                    color = HefimerColors.White,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    text = tr(
                        "Tap ★ to set default. Larger files auto-route seamlessly.",
                        "Chạm ★ để đặt mặc định. Tệp lớn tự chuyển tuyến mượt mà.",
                    ),
                    color = HefimerColors.White45,
                    fontSize = 11.sp,
                )
            }
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(34.dp)
                    .appleGlassSurface(
                        shape = CircleShape,
                        variant = AppleGlassVariant.Clear,
                        elevation = 0.dp,
                    )
                    .applePressable(pressedScale = 0.89f, onClick = onClose),
            ) {
                Icon(Icons.Rounded.Close, tr("Close", "Đóng"), tint = HefimerColors.White70, modifier = Modifier.size(16.dp))
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
    val fg by animateColorAsState(
        targetValue = if (selected) HefimerColors.Black else HefimerColors.White,
        animationSpec = tween(220, easing = AppMotion.AppleSilk),
        label = "prov_fg",
    )

    val cardModifier = if (selected) {
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(HefimerColors.White)
            .border(1.dp, HefimerColors.White, RoundedCornerShape(24.dp))
    } else {
        Modifier
            .fillMaxWidth()
            .appleGlassSurface(
                shape = RoundedCornerShape(24.dp),
                variant = AppleGlassVariant.Clear,
                elevation = 0.dp,
            )
    }

    Column(modifier = cardModifier) {
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
                    .clip(RoundedCornerShape(16.dp))
                    .applePressable(pressedScale = 0.98f, onClick = onSelect)
                    .padding(6.dp),
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(36.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(if (selected) HefimerColors.Black.copy(alpha = 0.08f) else HefimerColors.White08)
                        .border(1.dp, if (selected) HefimerColors.Black.copy(alpha = 0.14f) else HefimerColors.White12, RoundedCornerShape(14.dp)),
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
                                text = tr("DEFAULT", "MẶC ĐỊNH"),
                                color = if (selected) HefimerColors.Black.copy(alpha = 0.7f) else HefimerColors.White85,
                                fontSize = 8.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.sp,
                                modifier = Modifier
                                    .clip(CircleShape)
                                    .background(if (selected) HefimerColors.Black.copy(alpha = 0.12f) else HefimerColors.White12)
                                    .padding(horizontal = 6.dp, vertical = 2.dp),
                            )
                        } else if (prov.isLargeFile) {
                            Text(
                                text = tr("LARGE", "TỆP LỚN"),
                                color = if (selected) HefimerColors.Black.copy(alpha = 0.6f) else HefimerColors.White60,
                                fontSize = 8.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.sp,
                                modifier = Modifier
                                    .clip(CircleShape)
                                    .background(if (selected) HefimerColors.Black.copy(alpha = 0.1f) else HefimerColors.White08)
                                    .padding(horizontal = 6.dp, vertical = 2.dp),
                            )
                        }
                    }
                    Text(
                        text = prov.localizedMaxSize(),
                        color = fg.copy(alpha = 0.55f),
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
                val starBg = if (isDefault) fg else Color.Transparent
                val starBorder = if (isDefault) fg else fg.copy(alpha = 0.14f)
                val starTint = if (isDefault) {
                    if (selected) HefimerColors.White else HefimerColors.Black
                } else {
                    fg.copy(alpha = 0.55f)
                }
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(34.dp)
                        .clip(RoundedCornerShape(11.dp))
                        .background(starBg)
                        .border(1.dp, starBorder, RoundedCornerShape(11.dp))
                        .applePressable(pressedScale = 0.90f, onClick = onSetDefault),
                ) {
                    Icon(
                        imageVector = if (isDefault) Icons.Rounded.Star else Icons.Rounded.StarOutline,
                        contentDescription = tr("Set as default", "Đặt làm mặc định"),
                        tint = starTint,
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
                    .border(1.dp, if (selected) HefimerColors.Black.copy(alpha = 0.14f) else HefimerColors.White12, RoundedCornerShape(11.dp))
                    .applePressable(pressedScale = 0.90f, onClick = onToggleExpand),
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
                    .padding(start = 16.dp, end = 16.dp, bottom = 14.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Box(Modifier.fillMaxWidth().height(0.5.dp).background(fg.copy(alpha = 0.1f)))
                Spacer(Modifier.height(4.dp))
                PolicyLine(tr("LIMITS", "GIỚI HẠN"), prov.localizedLimitDetail(), fg)
                PolicyLine(tr("EXPIRY", "HẾT HẠN"), prov.localizedExpiry(), fg)
                PolicyLine(tr("DELIVERY", "TỐC ĐỘ"), prov.localizedSpeed(), fg)
                if (prov.localizedDownloadFlow().isNotBlank()) {
                    PolicyLine(tr("DOWNLOAD", "KIỂU TẢI"), prov.localizedDownloadFlow(), fg)
                }
                if (prov.localizedPrivacyDetail().isNotBlank()) {
                    PolicyLine(tr("PRIVACY", "BẢO MẬT"), prov.localizedPrivacyDetail(), fg)
                }
                if (prov.localizedBestFor().isNotBlank()) {
                    PolicyLine(tr("BEST FOR", "PHÙ HỢP"), prov.localizedBestFor(), fg)
                }
                if (prov.localizedLimitations().isNotBlank()) {
                    PolicyLine(tr("CAVEATS", "HẠN CHẾ"), prov.localizedLimitations(), fg)
                }
                PolicyLine(tr("NOTES", "GHI CHÚ"), prov.localizedFeatures(), fg)
            }
        }
    }
}

@Composable
private fun PolicyLine(label: String, value: String, fg: Color) {
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(
            text = label,
            color = fg.copy(alpha = 0.5f),
            fontSize = 9.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.2.sp,
            modifier = Modifier.width(66.dp),
        )
        Text(
            text = value,
            color = fg.copy(alpha = 0.78f),
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
                    .appleGlassSurface(
                        shape = RoundedCornerShape(36.dp),
                        variant = AppleGlassVariant.Elevated,
                        accentBorderTint = HefimerColors.Emerald400,
                        elevation = 24.dp,
                    )
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
                        text = tr("SECRET R2 ACCESS", "TRUY CẬP R2 BÍ MẬT"),
                        color = HefimerColors.Emerald400,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 2.4.sp,
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = tr(
                            "Enter secret passcode for private Cloudflare R2 storage",
                            "Nhập mật mã bí mật cho kho lưu trữ Cloudflare R2 riêng tư",
                        ),
                        color = HefimerColors.White40,
                        fontSize = 12.sp,
                        textAlign = TextAlign.Center,
                    )
                }
                HefimerTextField(
                    value = pass,
                    onValueChange = { pass = it },
                    placeholder = tr("Passcode...", "Mật mã..."),
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
                            .appleGlassSurface(
                                shape = CircleShape,
                                variant = AppleGlassVariant.Clear,
                                elevation = 0.dp,
                            )
                            .applePressable(pressedScale = 0.96f, onClick = onDismiss),
                    ) {
                        Text(tr("Cancel", "Hủy"), color = HefimerColors.White60, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    }
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .clip(CircleShape)
                            .background(HefimerColors.Emerald400)
                            .applePressable(pressedScale = 0.96f) { onSubmit(pass) },
                    ) {
                        Text(tr("Unlock", "Mở khóa"), color = HefimerColors.Black, fontSize = 13.sp, fontWeight = FontWeight.Bold)
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
    var focused by remember { mutableStateOf(false) }
    val iconTint by animateColorAsState(
        targetValue = if (focused) HefimerColors.White else HefimerColors.White40,
        animationSpec = tween(AppMotion.Fast, easing = AppMotion.AppleSilk),
        label = "field_icon_tint",
    )
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .fillMaxWidth()
            .appleGlassSurface(
                shape = RoundedCornerShape(20.dp),
                variant = AppleGlassVariant.Clear,
                accentBorderTint = if (focused) HefimerColors.White else null,
                accentFillTint = if (focused) HefimerColors.White.copy(alpha = 0.06f) else null,
                elevation = 0.dp,
            )
            .padding(horizontal = 14.dp, vertical = 14.dp),
    ) {
        if (leadingIcon != null) {
            Icon(leadingIcon, null, tint = iconTint, modifier = Modifier.size(16.dp))
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
            modifier = Modifier
                .weight(1f)
                .onFocusChanged { focused = it.isFocused },
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
