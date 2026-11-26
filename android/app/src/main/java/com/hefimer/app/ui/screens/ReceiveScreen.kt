package com.hefimer.app.ui.screens

import android.content.ClipboardManager
import android.content.Context
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.InsertDriveFile
import androidx.compose.material.icons.rounded.Archive
import androidx.compose.material.icons.rounded.AudioFile
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.ContentPaste
import androidx.compose.material.icons.rounded.Description
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.FolderCopy
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.Image
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.QrCode2
import androidx.compose.material.icons.rounded.QrCodeScanner
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material.icons.rounded.UploadFile
import androidx.compose.material.icons.rounded.VideoFile
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import com.hefimer.app.data.model.DropFileEntry
import com.hefimer.app.data.model.DropItem
import com.hefimer.app.data.model.ToastType
import com.hefimer.app.data.model.formatBytes
import com.hefimer.app.data.network.DirectDownloadMode
import com.hefimer.app.data.network.DownloadHelper
import com.hefimer.app.data.network.HefimerApiConfig
import com.hefimer.app.data.network.UploadProviders
import com.hefimer.app.ui.AppOverlay
import com.hefimer.app.ui.HefimerViewModel
import com.hefimer.app.ui.ScanTarget
import com.hefimer.app.ui.components.CodeHighlighter
import com.hefimer.app.ui.components.CodeLineNumberGutter
import com.hefimer.app.ui.components.HefimerImageLightbox
import com.hefimer.app.ui.components.HefimerRemoteImage
import com.hefimer.app.ui.components.HefimerCollapsibleScaffold
import com.hefimer.app.ui.components.HefimerHeaderIconButton
import com.hefimer.app.ui.components.HefimerPrimaryButton
import com.hefimer.app.ui.components.HefimerSecondaryButton
import com.hefimer.app.ui.components.QrShareModal
import com.hefimer.app.ui.components.copyToClipboard
import com.hefimer.app.ui.theme.AppMotion
import com.hefimer.app.ui.theme.AppleGlassVariant
import com.hefimer.app.ui.theme.HefimerColors
import com.hefimer.app.ui.theme.JetBrainsMonoFamily
import com.hefimer.app.ui.theme.LocalHeroBounds
import com.hefimer.app.ui.theme.UbuntuFontFamily
import com.hefimer.app.ui.theme.appleEntrance
import com.hefimer.app.ui.theme.appleGlassSurface
import com.hefimer.app.ui.theme.applePressable
import com.hefimer.app.ui.theme.rememberReduceMotion
import com.hefimer.app.ui.theme.heroSource
import com.hefimer.app.ui.theme.rememberReduceMotion
import com.hefimer.app.ui.theme.tr
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun ReceiveScreen(vm: HefimerViewModel) {
    val state by vm.receiveState.collectAsState()
    val context = LocalContext.current
    val heroBounds = LocalHeroBounds.current
    val reduceMotion = rememberReduceMotion()

    HefimerCollapsibleScaffold(
        kicker = tr("Retrieve a drop", "Nhận tệp & văn bản"),
        title = tr("Receive", "Nhận"),
        subtitle = tr(
            "Enter a 5-digit code or scan a Hefimer QR to unlock files and snippets.",
            "Nhập mã 5 chữ số hoặc quét mã QR Hefimer để mở tệp và đoạn mã.",
        ),
        showOrbitMark = true,
        headerActions = {
            HefimerHeaderIconButton(
                icon = Icons.Rounded.QrCodeScanner,
                contentDescription = tr("Scan QR", "Quét QR"),
                modifier = Modifier.heroSource("btn_qr_receive", heroBounds),
                onClick = { vm.openOverlay(AppOverlay.QrScanner(ScanTarget.RECEIVE), "btn_qr_receive") },
            )
            HefimerHeaderIconButton(
                icon = Icons.Rounded.History,
                contentDescription = tr("History", "Lịch sử"),
                modifier = Modifier.heroSource("btn_history_receive", heroBounds),
                onClick = { vm.openOverlay(AppOverlay.History, "btn_history_receive") },
            )
        },
    ) {
        AnimatedContent(
            targetState = state.result,
            transitionSpec = { AppMotion.sectionTransform(reduceMotion) },
            label = "receive_state",
        ) { drop ->
            if (drop != null) {
                ReceivedDropView(
                    vm = vm,
                    item = drop,
                    onBack = { vm.resetReceive() },
                )
            } else {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(20.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .appleEntrance(0)
                        .appleGlassSurface(
                            shape = RoundedCornerShape(38.dp),
                            variant = AppleGlassVariant.Regular,
                            elevation = 14.dp,
                        )
                        .padding(horizontal = 22.dp, vertical = 30.dp),
                ) {
                    Text(
                        text = tr("ENTER 5-DIGIT CODE", "NHẬP MÃ 5 CHỮ SỐ"),
                        color = HefimerColors.White40,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 2.4.sp,
                    )

                    FiveDigitPinSlots(
                        code = state.code,
                        onCodeChange = { next ->
                            vm.setReceiveCode(next)
                            if (next.length == 5) vm.retrieveDrop(next)
                        },
                        onDone = { if (state.code.length == 5) vm.retrieveDrop() },
                    )

                    HefimerPrimaryButton(
                        text = if (state.loading) tr("Retrieving...", "Đang tải...") else tr("Retrieve", "Nhận ngay"),
                        icon = Icons.Rounded.Download,
                        loading = state.loading,
                        enabled = state.code.length == 5 && !state.loading,
                        onClick = { vm.retrieveDrop() },
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        HefimerSecondaryButton(
                            text = tr("Paste Code", "Dán mã"),
                            icon = Icons.Rounded.ContentPaste,
                            modifier = Modifier.weight(1f),
                            onClick = {
                                val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                val clip = cm.primaryClip?.getItemAt(0)?.text?.toString().orEmpty()
                                val digits = clip.filter { it.isDigit() }.take(5)
                                if (digits.length == 5) {
                                    vm.setReceiveCode(digits)
                                    vm.retrieveDrop(digits)
                                } else {
                                    vm.showToast(tr("No 5-digit code in clipboard", "Không thấy mã 5 số trong bộ nhớ tạm"), ToastType.ERROR)
                                }
                            },
                        )
                        HefimerSecondaryButton(
                            text = tr("Scan QR", "Quét QR"),
                            icon = Icons.Rounded.QrCodeScanner,
                            modifier = Modifier.weight(1f),
                            onClick = { vm.openOverlay(AppOverlay.QrScanner(ScanTarget.RECEIVE)) },
                        )
                    }
                }
            }
        }
    }

    if (state.passwordPromptOpen) {
        UnlockPasswordDialog(
            onDismiss = { vm.dismissReceivePasswordPrompt() },
            onUnlock = { vm.unlockReceiveWithPassword(it) },
        )
    }
}

@Composable
fun FiveDigitPinSlots(
    code: String,
    onCodeChange: (String) -> Unit,
    onDone: () -> Unit,
) {
    var focused by remember { mutableStateOf(false) }
    val reduceMotion = rememberReduceMotion()
    val context = LocalContext.current
    val view = androidx.compose.ui.platform.LocalView.current
    val blink = rememberInfiniteTransition(label = "pin_cursor").animateFloat(
        initialValue = 1f,
        targetValue = 0.15f,
        animationSpec = infiniteRepeatable(tween(720, easing = AppMotion.AppleSilk), RepeatMode.Reverse),
        label = "cursor_alpha",
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(78.dp),
        contentAlignment = Alignment.Center,
    ) {
        // Visual 5 Apple Glass slots with calm slot breathe & digit morph
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            for (i in 0 until 5) {
                val ch = code.getOrNull(i)?.toString().orEmpty()
                val isCursorSlot = focused && code.length == i
                val slotScale by animateFloatAsState(
                    targetValue = when {
                        reduceMotion -> 1f
                        isCursorSlot -> 1.045f
                        ch.isNotEmpty() -> 1.015f
                        else -> 1f
                    },
                    animationSpec = AppMotion.fluidSpring(),
                    label = "pin_slot_scale_$i",
                )
                val borderTint by animateColorAsState(
                    targetValue = when {
                        isCursorSlot -> HefimerColors.White
                        ch.isNotEmpty() -> HefimerColors.White20
                        else -> Color.Transparent
                    },
                    animationSpec = tween(AppMotion.Fast, easing = AppMotion.AppleSilk),
                    label = "pin_slot_border_$i",
                )
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .width(54.dp)
                        .height(70.dp)
                        .graphicsLayer {
                            scaleX = slotScale
                            scaleY = slotScale
                        }
                        .appleGlassSurface(
                            shape = RoundedCornerShape(18.dp),
                            variant = AppleGlassVariant.Clear,
                            accentBorderTint = if (borderTint.alpha > 0.01f) borderTint else null,
                            elevation = 0.dp,
                        ),
                ) {
                    AnimatedContent(
                        targetState = if (ch.isNotEmpty()) ch else if (isCursorSlot) "|" else "",
                        transitionSpec = {
                            if (reduceMotion) {
                                fadeIn(tween(110, easing = AppMotion.AppleEase)) togetherWith
                                    fadeOut(tween(90, easing = AppMotion.AppleEase))
                            } else {
                                (fadeIn(tween(280, easing = AppMotion.AppleSilk)) +
                                    scaleIn(tween(340, easing = AppMotion.AppleSheet), initialScale = 0.82f) +
                                    slideInVertically(tween(340, easing = AppMotion.AppleSheet)) { it / 5 })
                                    .togetherWith(
                                        fadeOut(tween(150, easing = AppMotion.AppleEase)) +
                                            scaleOut(tween(150, easing = AppMotion.AppleSilk), targetScale = 0.88f),
                                    )
                            }
                        },
                        label = "pin_char_$i",
                    ) { token ->
                        when (token) {
                            "" -> Spacer(Modifier.size(2.dp))
                            "|" -> Box(
                                modifier = Modifier
                                    .width(2.dp)
                                    .height(26.dp)
                                    .graphicsLayer { alpha = blink.value }
                                    .background(HefimerColors.White),
                            )
                            else -> Text(
                                text = token,
                                fontFamily = UbuntuFontFamily,
                                fontWeight = FontWeight.Bold,
                                fontSize = 32.sp,
                                color = HefimerColors.White,
                            )
                        }
                    }
                }
            }
        }

        // Invisible overlay numeric input
        val pinAccessibilityLabel = tr("Enter 5-digit Drop code", "Nhập mã Drop 5 chữ số")
        BasicTextField(
            value = code,
            onValueChange = { raw ->
                val clean = raw.filter { c -> c.isDigit() }.take(5)
                if (clean != code) {
                    com.hefimer.app.ui.theme.HefimerHaptics.tick(context, view)
                }
                onCodeChange(clean)
            },
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Number,
                imeAction = ImeAction.Go,
            ),
            keyboardActions = KeyboardActions(onGo = { onDone() }),
            cursorBrush = SolidColor(Color.Transparent),
            textStyle = TextStyle(color = Color.Transparent, fontSize = 1.sp),
            modifier = Modifier
                .fillMaxWidth()
                .height(78.dp)
                .semantics { contentDescription = pinAccessibilityLabel }
                .onFocusChanged { focused = it.isFocused },
        )
    }
}

@Composable
private fun ReceivedDropView(
    vm: HefimerViewModel,
    item: DropItem,
    onBack: () -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var copiedText by remember { mutableStateOf(false) }
    var copiedLink by remember { mutableStateOf(false) }
    var showLineNumbers by remember { mutableStateOf(false) }
    var showRenameDialog by remember { mutableStateOf(false) }

    val isR2 = item.isR2Drop
    val accent = if (isR2) HefimerColors.Emerald400 else HefimerColors.White
    val dropShareUrl = "${HefimerApiConfig.SITE_ORIGIN}/?code=${item.code}"

    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        // Header row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column {
                Text(
                    text = tr("DROP #${item.code}", "MÃ NHẬN #${item.code}"),
                    color = accent.copy(alpha = 0.55f),
                    fontFamily = JetBrainsMonoFamily,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    text = tr("Received Data", "Dữ liệu đã nhận"),
                    color = HefimerColors.White,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                )
            }
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier
                    .appleGlassSurface(
                        shape = CircleShape,
                        variant = AppleGlassVariant.Clear,
                        elevation = 4.dp,
                    )
                    .applePressable(pressedScale = 0.92f, onClick = onBack)
                    .padding(horizontal = 14.dp, vertical = 9.dp),
            ) {
                Icon(Icons.AutoMirrored.Rounded.ArrowBack, null, tint = HefimerColors.White85, modifier = Modifier.size(15.dp))
                Text(tr("Back", "Quay lại"), color = HefimerColors.White85, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
        }

        // Case 1: Multi-file drop
        val multiFiles = item.files
        if (!multiFiles.isNullOrEmpty()) {
            val totalSize = multiFiles.sumOf { it.resolvedSize }
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .appleGlassSurface(
                        shape = RoundedCornerShape(34.dp),
                        variant = AppleGlassVariant.Regular,
                        accentBorderTint = if (isR2) HefimerColors.Emerald400 else null,
                        accentFillTint = if (isR2) HefimerColors.EmeraldBg.copy(alpha = 0.22f) else null,
                        elevation = 12.dp,
                    )
                    .padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(46.dp)
                            .appleGlassSurface(
                                shape = RoundedCornerShape(14.dp),
                                variant = AppleGlassVariant.Clear,
                                accentBorderTint = if (isR2) HefimerColors.Emerald400 else null,
                                elevation = 0.dp,
                            ),
                    ) {
                        Icon(Icons.Rounded.FolderCopy, null, tint = accent, modifier = Modifier.size(22.dp))
                    }
                    Column {
                        Text(tr("Shared Files", "Tệp được chia sẻ"), color = HefimerColors.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                        Text(
                            text = "${multiFiles.size} ${tr("items", "mục")} • ${formatBytes(totalSize)}",
                            color = HefimerColors.White45,
                            fontSize = 12.sp,
                        )
                    }
                }
                Box(Modifier.fillMaxWidth().height(0.5.dp).background(HefimerColors.White10))

                multiFiles.forEach { fileEntry ->
                    val entryIsPreviewableImage = item.e2eeKey.isNullOrBlank() &&
                        DownloadHelper.isImageFileName(fileEntry.resolvedName)
                    val entryPreviewUrl by produceState<String?>(
                        initialValue = if (entryIsPreviewableImage && fileEntry.objectKey.isBlank()) {
                            DownloadHelper.getMediaPreviewUrl(
                                fileEntry.resolvedUrl,
                                DownloadHelper.inferImageMime(fileEntry.resolvedName),
                                fileEntry.resolvedName,
                            )
                        } else null,
                        key1 = fileEntry.objectKey,
                        key2 = fileEntry.resolvedUrl,
                        key3 = entryIsPreviewableImage,
                    ) {
                        if (entryIsPreviewableImage && fileEntry.objectKey.isNotBlank()) {
                            value = vm.resolveR2PreviewUrl(fileEntry.objectKey)
                        }
                    }
                    var showEntryLightbox by remember(fileEntry) { mutableStateOf(false) }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .appleGlassSurface(
                                shape = RoundedCornerShape(22.dp),
                                variant = AppleGlassVariant.Clear,
                                elevation = 0.dp,
                            )
                            .padding(12.dp),
                    ) {
                        if (entryPreviewUrl != null) {
                            HefimerRemoteImage(
                                model = entryPreviewUrl,
                                fileName = fileEntry.resolvedName,
                                compact = true,
                                modifier = Modifier
                                    .size(44.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .border(1.dp, HefimerColors.White15, RoundedCornerShape(12.dp)),
                                onClick = { showEntryLightbox = true },
                            )
                        } else {
                            val (icon, iconTint) = fileIconForName(fileEntry.resolvedName, isR2)
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier
                                    .size(38.dp)
                                    .appleGlassSurface(
                                        shape = RoundedCornerShape(12.dp),
                                        variant = AppleGlassVariant.Clear,
                                        elevation = 0.dp,
                                    ),
                            ) {
                                Icon(icon, null, tint = iconTint, modifier = Modifier.size(18.dp))
                            }
                        }
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .let { colMod ->
                                    if (entryPreviewUrl != null) {
                                        colMod.applePressable(pressedScale = 0.985f) { showEntryLightbox = true }
                                    } else colMod
                                },
                        ) {
                            Text(
                                text = fileEntry.resolvedName,
                                color = HefimerColors.White90,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                            Text(
                                text = formatBytes(fileEntry.resolvedSize) +
                                    if (entryPreviewUrl != null) " • ${tr("Tap to preview", "Chạm xem ảnh")}" else "",
                                color = HefimerColors.White30,
                                fontFamily = JetBrainsMonoFamily,
                                fontSize = 10.sp,
                            )
                        }
                        Text(
                            text = tr("Download", "Tải xuống"),
                            color = HefimerColors.Black,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier
                                .clip(CircleShape)
                                .background(accent)
                                .applePressable(pressedScale = 0.94f) { vm.downloadDropFile(fileEntry) }
                                .padding(horizontal = 13.dp, vertical = 7.dp),
                        )
                    }

                    if (showEntryLightbox && entryPreviewUrl != null) {
                        HefimerImageLightbox(
                            model = entryPreviewUrl,
                            title = fileEntry.resolvedName,
                            mimeType = DownloadHelper.inferImageMime(fileEntry.resolvedName),
                            onDownload = { vm.downloadDropFile(fileEntry) },
                            onDismiss = { showEntryLightbox = false },
                        )
                    }
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                HefimerPrimaryButton(
                    text = tr("Download All", "Tải tất cả"),
                    icon = Icons.Rounded.Download,
                    accentColor = accent,
                    modifier = Modifier.weight(1.2f),
                    onClick = { vm.downloadAllDropFiles(multiFiles) },
                )
                HefimerSecondaryButton(
                    text = if (copiedLink) tr("Copied!", "Đã chép!") else tr("Copy Link", "Chép link"),
                    icon = if (copiedLink) Icons.Rounded.Check else Icons.Rounded.ContentCopy,
                    modifier = Modifier.weight(1f),
                    height = 54.dp,
                    onClick = {
                        copyToClipboard(context, "Hefimer Link", dropShareUrl)
                        copiedLink = true
                        vm.showToast(tr("Link copied", "Đã chép liên kết"), ToastType.SUCCESS)
                        scope.launch { delay(2000L); copiedLink = false }
                    },
                )
            }
        } else if (isR2) {
            // Case 2: Secret R2 Single File + optional inline image/SVG preview when not E2EE
            val r2IsPreviewable = item.e2eeKey.isNullOrBlank() &&
                DownloadHelper.isImageFileName(item.fileName, item.mimeType)
            val r2PreviewUrl by produceState<String?>(
                initialValue = null,
                key1 = item.objectKey,
                key2 = r2IsPreviewable,
            ) {
                if (r2IsPreviewable && !item.objectKey.isNullOrBlank()) {
                    value = vm.resolveR2PreviewUrl(item.objectKey)
                }
            }
            var showR2Lightbox by remember(item.objectKey) { mutableStateOf(false) }

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .appleGlassSurface(
                        shape = RoundedCornerShape(36.dp),
                        variant = AppleGlassVariant.Regular,
                        accentBorderTint = HefimerColors.Emerald400,
                        accentFillTint = HefimerColors.EmeraldBg.copy(alpha = 0.28f),
                        elevation = 14.dp,
                    )
                    .padding(28.dp),
            ) {
                if (r2PreviewUrl != null) {
                    HefimerRemoteImage(
                        model = r2PreviewUrl,
                        fileName = item.fileName,
                        mimeType = item.mimeType,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(190.dp)
                            .clip(RoundedCornerShape(22.dp))
                            .border(1.dp, HefimerColors.Emerald400.copy(alpha = 0.45f), RoundedCornerShape(22.dp)),
                        onClick = { showR2Lightbox = true },
                    )
                } else {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(64.dp)
                            .appleGlassSurface(
                                shape = CircleShape,
                                variant = AppleGlassVariant.Clear,
                                accentBorderTint = HefimerColors.Emerald400,
                                accentFillTint = HefimerColors.Emerald400.copy(alpha = 0.14f),
                                elevation = 0.dp,
                            ),
                    ) {
                        Icon(Icons.Rounded.Shield, null, tint = HefimerColors.Emerald400, modifier = Modifier.size(30.dp))
                    }
                }
                Text(
                    text = item.fileName ?: tr("Secret File", "Tệp bí mật"),
                    color = HefimerColors.White,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                )
                Text(
                    text = if (!item.e2eeKey.isNullOrBlank()) {
                        tr("SECRET R2 • E2EE ENCRYPTED", "R2 BÍ MẬT • MÃ HÓA E2EE")
                    } else {
                        tr("SECRET CLOUDFLARE R2 STORAGE", "LƯU TRỮ CLOUDFLARE R2 BÍ MẬT")
                    },
                    color = HefimerColors.Emerald400.copy(alpha = 0.68f),
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 2.sp,
                )
                if (!item.description.isNullOrBlank()) {
                    Text(
                        text = "\"${item.description}\"",
                        color = HefimerColors.White60,
                        fontSize = 13.sp,
                        fontStyle = FontStyle.Italic,
                        textAlign = TextAlign.Center,
                    )
                }
            }

            if (showR2Lightbox && r2PreviewUrl != null) {
                HefimerImageLightbox(
                    model = r2PreviewUrl,
                    title = item.fileName ?: tr("Secret Image", "Ảnh R2"),
                    mimeType = item.mimeType,
                    onDownload = {
                        vm.downloadDropFile(
                            DropFileEntry(
                                fileName = item.fileName ?: "download",
                                objectKey = item.objectKey.orEmpty(),
                                fileSize = item.fileSize,
                            ),
                        )
                    },
                    onDismiss = { showR2Lightbox = false },
                )
            }

            HefimerPrimaryButton(
                text = tr("Download from R2", "Tải xuống từ R2"),
                icon = Icons.Rounded.Download,
                accentColor = HefimerColors.Emerald400,
                onClick = {
                    vm.downloadDropFile(
                        DropFileEntry(
                            fileName = item.fileName ?: "download",
                            objectKey = item.objectKey.orEmpty(),
                            fileSize = item.fileSize,
                        ),
                    )
                },
            )
        } else if (item.type == "file" || (item.type.isBlank() && item.resolvedUrl.isNotBlank())) {
            // Case 3: Standard Single File + inline image/SVG preview & lightbox
            val downloadUrl = item.resolvedUrl
            val isTab = DownloadHelper.getDirectDownloadMode(downloadUrl) == DirectDownloadMode.TAB
            val providerName = UploadProviders.getProviderNameFromUrl(downloadUrl, false)
            val guessedMime = remember(item.mimeType, item.fileName, downloadUrl) {
                DownloadHelper.inferImageMime(item.fileName ?: downloadUrl, item.mimeType)
            }
            val previewUrl = remember(downloadUrl, guessedMime, item.fileName, item.e2eeKey) {
                if (item.e2eeKey.isNullOrBlank() && guessedMime?.startsWith("image/") == true) {
                    DownloadHelper.getMediaPreviewUrl(downloadUrl, guessedMime, item.fileName)
                } else null
            }
            var showFullImage by remember { mutableStateOf(false) }

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .appleGlassSurface(
                        shape = RoundedCornerShape(36.dp),
                        variant = AppleGlassVariant.Regular,
                        elevation = 14.dp,
                    )
                    .padding(24.dp),
            ) {
                if (previewUrl != null) {
                    val fmtBadge = remember(item.fileName, guessedMime) {
                        val ext = item.fileName.orEmpty().substringAfterLast('.', "").uppercase()
                        if (DownloadHelper.isSvgAsset(item.fileName, guessedMime)) "SVG"
                        else ext.ifBlank { "IMG" }
                    }
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(198.dp)
                            .clip(RoundedCornerShape(22.dp))
                            .border(1.dp, HefimerColors.White15, RoundedCornerShape(22.dp)),
                    ) {
                        HefimerRemoteImage(
                            model = previewUrl,
                            fileName = item.fileName,
                            mimeType = guessedMime,
                            modifier = Modifier.fillMaxWidth().height(198.dp),
                            onClick = { showFullImage = true },
                        )
                        Row(
                            modifier = Modifier
                                .align(Alignment.BottomStart)
                                .padding(10.dp)
                                .appleGlassSurface(CircleShape, AppleGlassVariant.Elevated, elevation = 0.dp)
                                .padding(horizontal = 10.dp, vertical = 4.dp),
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                text = fmtBadge,
                                color = HefimerColors.White,
                                fontFamily = JetBrainsMonoFamily,
                                fontSize = 9.5.sp,
                                fontWeight = FontWeight.Bold,
                            )
                            Text("•", color = HefimerColors.White30, fontSize = 9.sp)
                            Text(
                                text = tr("Tap to zoom", "Chạm để phóng to"),
                                color = HefimerColors.White75,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.SemiBold,
                            )
                        }
                    }
                } else {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(64.dp)
                            .appleGlassSurface(
                                shape = CircleShape,
                                variant = AppleGlassVariant.Clear,
                                elevation = 0.dp,
                            ),
                    ) {
                        Icon(
                            imageVector = if (!item.e2eeKey.isNullOrBlank()) Icons.Rounded.Shield else Icons.Rounded.UploadFile,
                            contentDescription = null,
                            tint = HefimerColors.White,
                            modifier = Modifier.size(30.dp),
                        )
                    }
                }
                Text(
                    text = item.fileName ?: tr("Unknown File", "Tệp không rõ tên"),
                    color = HefimerColors.White,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                )
                Text(
                    text = "${tr("HOSTED ON", "LƯU TRỮ TRÊN")} ${providerName.uppercase()}" +
                        if (!item.e2eeKey.isNullOrBlank()) " • E2EE" else "",
                    color = HefimerColors.White45,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 2.sp,
                )
            }

            if (showFullImage && previewUrl != null) {
                HefimerImageLightbox(
                    model = previewUrl,
                    title = item.fileName ?: tr("Image", "Ảnh"),
                    mimeType = guessedMime,
                    onDownload = {
                        vm.downloadDropFile(
                            DropFileEntry(
                                fileName = item.fileName ?: "download",
                                fileUrl = downloadUrl,
                                fileSize = item.fileSize,
                            ),
                        )
                    },
                    onDismiss = { showFullImage = false },
                )
            }

            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                HefimerPrimaryButton(
                    text = if (isTab) tr("Open in $providerName", "Mở trên $providerName") else tr("Download", "Tải xuống"),
                    icon = Icons.Rounded.Download,
                    modifier = Modifier.weight(1.3f),
                    onClick = {
                        vm.downloadDropFile(
                            DropFileEntry(
                                fileName = item.fileName ?: "download",
                                fileUrl = downloadUrl,
                                fileSize = item.fileSize,
                            ),
                        )
                    },
                )
                HefimerSecondaryButton(
                    text = if (copiedLink) tr("Copied!", "Đã chép!") else tr("Copy Link", "Chép link"),
                    icon = if (copiedLink) Icons.Rounded.Check else Icons.Rounded.ContentCopy,
                    modifier = Modifier.weight(1f),
                    height = 54.dp,
                    onClick = {
                        copyToClipboard(context, "Hefimer Link", dropShareUrl)
                        copiedLink = true
                        vm.showToast(tr("Link copied", "Đã chép liên kết"), ToastType.SUCCESS)
                        scope.launch { delay(2000L); copiedLink = false }
                    },
                )
            }
        } else {
            // Case 4: Text / Code Drop with Dracula Highlighting & Smart Action
            val textBody = item.text.orEmpty()
            val lang = remember(textBody) { CodeHighlighter.detectLanguage(textBody) }
            val smartAction = remember(textBody) { CodeHighlighter.getSmartAction(textBody) }
            val highlighted = remember(textBody, lang.id) { CodeHighlighter.highlight(textBody, lang.id) }
            val lineCount = remember(textBody) { maxOf(1, textBody.count { it == '\n' } + 1) }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .appleGlassSurface(
                        shape = RoundedCornerShape(34.dp),
                        variant = AppleGlassVariant.Regular,
                        elevation = 12.dp,
                    ),
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(HefimerColors.White04)
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = lang.localizedLabel().uppercase(),
                        color = HefimerColors.White75,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.8.sp,
                        modifier = Modifier
                            .appleGlassSurface(
                                shape = CircleShape,
                                variant = AppleGlassVariant.Clear,
                                elevation = 0.dp,
                            )
                            .padding(horizontal = 12.dp, vertical = 5.dp),
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        val lineBtnBg by animateColorAsState(
                            targetValue = if (showLineNumbers) HefimerColors.White else Color.Transparent,
                            animationSpec = tween(AppMotion.Micro, easing = AppMotion.AppleSilk),
                            label = "rx_lines_bg",
                        )
                        val lineBtnTint by animateColorAsState(
                            targetValue = if (showLineNumbers) HefimerColors.Black else HefimerColors.White70,
                            animationSpec = tween(AppMotion.Micro, easing = AppMotion.AppleSilk),
                            label = "rx_lines_tint",
                        )
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .size(34.dp)
                                .then(
                                    if (showLineNumbers) {
                                        Modifier.clip(CircleShape).background(lineBtnBg)
                                    } else {
                                        Modifier.appleGlassSurface(CircleShape, AppleGlassVariant.Clear, elevation = 0.dp)
                                    },
                                )
                                .applePressable(pressedScale = 0.89f) { showLineNumbers = !showLineNumbers },
                        ) {
                            Icon(
                                if (showLineNumbers) Icons.Rounded.VisibilityOff else Icons.Rounded.Visibility,
                                tr("Line numbers", "Số dòng"),
                                tint = lineBtnTint,
                                modifier = Modifier.size(15.dp),
                            )
                        }
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .size(34.dp)
                                .appleGlassSurface(CircleShape, AppleGlassVariant.Clear, elevation = 0.dp)
                                .applePressable(pressedScale = 0.89f) { showRenameDialog = true },
                        ) {
                            Icon(Icons.Rounded.Download, tr("Save file", "Lưu tệp"), tint = HefimerColors.White70, modifier = Modifier.size(15.dp))
                        }
                        val copyBtnBg by animateColorAsState(
                            targetValue = if (copiedText) HefimerColors.White else Color.Transparent,
                            animationSpec = tween(AppMotion.Micro, easing = AppMotion.AppleSilk),
                            label = "rx_copy_bg",
                        )
                        val copyBtnTint by animateColorAsState(
                            targetValue = if (copiedText) HefimerColors.Black else HefimerColors.White70,
                            animationSpec = tween(AppMotion.Micro, easing = AppMotion.AppleSilk),
                            label = "rx_copy_tint",
                        )
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .size(34.dp)
                                .then(
                                    if (copiedText) {
                                        Modifier.clip(CircleShape).background(copyBtnBg)
                                    } else {
                                        Modifier.appleGlassSurface(CircleShape, AppleGlassVariant.Clear, elevation = 0.dp)
                                    },
                                )
                                .applePressable(pressedScale = 0.89f) {
                                    if (copyToClipboard(context, "Hefimer Text", textBody)) {
                                        copiedText = true
                                        vm.showToast(tr("Text copied", "Đã sao chép văn bản"), ToastType.SUCCESS)
                                        scope.launch { delay(2000L); copiedText = false }
                                    } else {
                                        vm.showToast(tr("Text too large for clipboard — save as file instead", "Văn bản quá lớn cho bộ nhớ tạm — hãy lưu thành tệp"), ToastType.ERROR)
                                    }
                                },
                        ) {
                            Icon(
                                if (copiedText) Icons.Rounded.Check else Icons.Rounded.ContentCopy,
                                tr("Copy", "Sao chép"),
                                tint = copyBtnTint,
                                modifier = Modifier.size(15.dp),
                            )
                        }
                    }
                }

                Box(Modifier.fillMaxWidth().height(0.5.dp).background(HefimerColors.White10))

                var textLayoutResult by remember(textBody) { mutableStateOf<TextLayoutResult?>(null) }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 140.dp, max = 380.dp)
                        .verticalScroll(rememberScrollState())
                        .horizontalScroll(rememberScrollState())
                        .padding(18.dp),
                ) {
                    if (showLineNumbers) {
                        CodeLineNumberGutter(
                            text = textBody,
                            textLayoutResult = textLayoutResult,
                            color = HefimerColors.White20,
                        )
                    }
                    Text(
                        text = highlighted,
                        fontFamily = JetBrainsMonoFamily,
                        fontSize = 13.5.sp,
                        lineHeight = 20.sp,
                        color = HefimerColors.White,
                        onTextLayout = { textLayoutResult = it },
                    )
                }

                Box(Modifier.fillMaxWidth().height(0.5.dp).background(HefimerColors.White10))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(HefimerColors.White04)
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                ) {
                    Text("$lineCount ${tr("Lines", "Dòng")}", color = HefimerColors.White40, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    Text("${textBody.length} ${tr("Chars", "Ký tự")}", color = HefimerColors.White40, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    Text(formatBytes(textBody.toByteArray().size.toLong()), color = HefimerColors.White40, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                if (smartAction != null) {
                    HefimerPrimaryButton(
                        text = smartAction.label,
                        icon = smartAction.icon,
                        modifier = Modifier.weight(1.4f),
                        onClick = { DownloadHelper.openExternalUrl(context, smartAction.url) },
                    )
                    HefimerSecondaryButton(
                        text = if (copiedText) tr("Copied!", "Đã chép!") else tr("Copy", "Sao chép"),
                        icon = if (copiedText) Icons.Rounded.Check else Icons.Rounded.ContentCopy,
                        modifier = Modifier.weight(1f),
                        height = 54.dp,
                        onClick = {
                            if (copyToClipboard(context, "Hefimer Text", textBody)) {
                                copiedText = true
                                vm.showToast(tr("Text copied", "Đã sao chép văn bản"), ToastType.SUCCESS)
                                scope.launch { delay(2000L); copiedText = false }
                            } else {
                                vm.showToast(tr("Text too large for clipboard — save as file instead", "Văn bản quá lớn cho bộ nhớ tạm — hãy lưu thành tệp"), ToastType.ERROR)
                            }
                        },
                    )
                } else {
                    HefimerPrimaryButton(
                        text = if (copiedText) tr("Copied!", "Đã chép!") else tr("Copy Text", "Sao chép văn bản"),
                        icon = if (copiedText) Icons.Rounded.Check else Icons.Rounded.ContentCopy,
                        onClick = {
                            if (copyToClipboard(context, "Hefimer Text", textBody)) {
                                copiedText = true
                                vm.showToast(tr("Text copied", "Đã sao chép văn bản"), ToastType.SUCCESS)
                                scope.launch { delay(2000L); copiedText = false }
                            } else {
                                vm.showToast(tr("Text too large for clipboard — save as file instead", "Văn bản quá lớn cho bộ nhớ tạm — hãy lưu thành tệp"), ToastType.ERROR)
                            }
                        },
                    )
                }
            }

            if (showRenameDialog) {
                SaveTextRenameDialog(
                    defaultName = "hefimer_${item.code}.${lang.ext}",
                    onDismiss = { showRenameDialog = false },
                    onSave = { name ->
                        showRenameDialog = false
                        scope.launch {
                            runCatching {
                                DownloadHelper.saveTextToDownloads(context, name, textBody)
                            }.onSuccess { outcome ->
                                vm.showToast(tr("Saved ${outcome.fileName} to Downloads", "Đã lưu ${outcome.fileName} vào Tải xuống"), ToastType.SUCCESS)
                                vm.notifyFileSaved(outcome)
                            }.onFailure {
                                vm.showToast(it.message ?: tr("Failed to save file", "Không thể lưu tệp"), ToastType.ERROR)
                            }
                        }
                    },
                )
            }
        }
    }
}

private fun fileIconForName(fileName: String, isR2: Boolean): Pair<ImageVector, Color> {
    val ext = fileName.substringAfterLast('.', "").lowercase()
    return when (ext) {
        "jpg", "jpeg", "png", "gif", "webp", "svg", "heic" -> Icons.Rounded.Image to HefimerColors.White80
        "mp4", "mkv", "avi", "mov", "webm" -> Icons.Rounded.VideoFile to HefimerColors.White80
        "mp3", "wav", "ogg", "m4a", "flac" -> Icons.Rounded.AudioFile to HefimerColors.White80
        "zip", "rar", "7z", "tar", "gz" -> Icons.Rounded.Archive to HefimerColors.White80
        "pdf" -> Icons.Rounded.Description to HefimerColors.White80
        else -> Icons.AutoMirrored.Rounded.InsertDriveFile to if (isR2) HefimerColors.Emerald400 else HefimerColors.White70
    }
}

@Composable
private fun UnlockPasswordDialog(
    onDismiss: () -> Unit,
    onUnlock: (String) -> Unit,
) {
    var pw by remember { mutableStateOf("") }
    com.hefimer.app.ui.theme.AppleAnimatedDialog(onDismissRequest = onDismiss) { close ->
        Box(modifier = Modifier.fillMaxWidth().imePadding().padding(horizontal = 24.dp)) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .appleGlassSurface(
                        shape = RoundedCornerShape(36.dp),
                        variant = AppleGlassVariant.Elevated,
                        elevation = 24.dp,
                    )
                    .padding(24.dp),
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(56.dp)
                        .appleGlassSurface(
                            shape = CircleShape,
                            variant = AppleGlassVariant.Clear,
                            elevation = 0.dp,
                        ),
                ) {
                    Icon(Icons.Rounded.Lock, null, tint = HefimerColors.White, modifier = Modifier.size(24.dp))
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(tr("PROTECTED DROP", "NỘI DUNG CÓ MẬT KHẨU"), color = HefimerColors.White, fontSize = 13.sp, fontWeight = FontWeight.Bold, letterSpacing = 2.sp)
                    Spacer(Modifier.height(4.dp))
                    Text(tr("Enter password to unlock", "Nhập mật khẩu để mở khóa"), color = HefimerColors.White45, fontSize = 11.sp)
                }
                HefimerTextField(
                    value = pw,
                    onValueChange = { pw = it },
                    placeholder = tr("Password...", "Mật khẩu..."),
                    isPassword = true,
                )
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    HefimerSecondaryButton(tr("Cancel", "Hủy"), onClick = close, modifier = Modifier.weight(1f))
                    HefimerPrimaryButton(tr("Unlock", "Mở khóa"), height = 48.dp, onClick = { onUnlock(pw) }, modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun SaveTextRenameDialog(
    defaultName: String,
    onDismiss: () -> Unit,
    onSave: (String) -> Unit,
) {
    var input by remember { mutableStateOf(defaultName) }
    com.hefimer.app.ui.theme.AppleAnimatedDialog(onDismissRequest = onDismiss) { close ->
        Box(modifier = Modifier.fillMaxWidth().imePadding().padding(horizontal = 24.dp)) {
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
                    Text(tr("SAVE AS FILE", "LƯU THÀNH TỆP"), color = HefimerColors.White, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 2.sp)
                }
                HefimerTextField(value = input, onValueChange = { input = it }, placeholder = tr("filename.txt", "ten_tep.txt"), mono = true)
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    HefimerSecondaryButton(tr("Cancel", "Hủy"), onClick = close, modifier = Modifier.weight(1f))
                    HefimerPrimaryButton(
                        text = tr("Save", "Lưu"),
                        accentColor = HefimerColors.White,
                        height = 48.dp,
                        modifier = Modifier.weight(1f),
                        onClick = { if (input.isNotBlank()) onSave(input.trim()) },
                    )
                }
            }
        }
    }
}
