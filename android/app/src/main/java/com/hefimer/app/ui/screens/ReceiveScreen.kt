package com.hefimer.app.ui.screens

import android.content.ClipboardManager
import android.content.Context
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.material.icons.rounded.Archive
import androidx.compose.material.icons.rounded.AudioFile
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.ContentPaste
import androidx.compose.material.icons.rounded.Description
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.FolderCopy
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.Image
import androidx.compose.material.icons.rounded.InsertDriveFile
import androidx.compose.material.icons.rounded.Lock
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
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
import com.hefimer.app.ui.components.HefimerCollapsibleScaffold
import com.hefimer.app.ui.components.HefimerHeaderIconButton
import com.hefimer.app.ui.components.HefimerPrimaryButton
import com.hefimer.app.ui.components.HefimerSecondaryButton
import com.hefimer.app.ui.components.copyToClipboard
import com.hefimer.app.ui.theme.AppMotion
import com.hefimer.app.ui.theme.HefimerColors
import com.hefimer.app.ui.theme.JetBrainsMonoFamily
import com.hefimer.app.ui.theme.LocalHeroBounds
import com.hefimer.app.ui.theme.UbuntuFontFamily
import com.hefimer.app.ui.theme.appleEntrance
import com.hefimer.app.ui.theme.applePressable
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
                        .clip(RoundedCornerShape(38.dp))
                        .background(HefimerColors.SurfaceElevated)
                        .border(1.dp, HefimerColors.White12, RoundedCornerShape(38.dp))
                        .padding(horizontal = 22.dp, vertical = 30.dp),
                ) {
                    Text(
                        text = tr("ENTER 5-DIGIT CODE", "NHẬP MÃ 5 CHỮ SỐ"),
                        color = HefimerColors.White30,
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
    val blink = rememberInfiniteTransition(label = "pin_cursor").animateFloat(
        initialValue = 1f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(tween(550, easing = AppMotion.AppleEase), RepeatMode.Reverse),
        label = "cursor_alpha",
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(78.dp),
        contentAlignment = Alignment.Center,
    ) {
        // Visual 5 slots
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            for (i in 0 until 5) {
                val ch = code.getOrNull(i)?.toString().orEmpty()
                val isCursorSlot = focused && code.length == i
                val lineColor = when {
                    ch.isNotEmpty() -> HefimerColors.White
                    isCursorSlot -> HefimerColors.White
                    else -> HefimerColors.White15
                }
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier
                        .width(54.dp)
                        .height(68.dp),
                ) {
                    Box(
                        modifier = Modifier.weight(1f),
                        contentAlignment = Alignment.Center,
                    ) {
                        if (ch.isNotEmpty()) {
                            Text(
                                text = ch,
                                fontFamily = UbuntuFontFamily,
                                fontWeight = FontWeight.Bold,
                                fontSize = 36.sp,
                                color = HefimerColors.White,
                            )
                        } else if (isCursorSlot) {
                            Box(
                                modifier = Modifier
                                    .width(2.dp)
                                    .height(28.dp)
                                    .alpha(blink.value)
                                    .background(HefimerColors.White),
                            )
                        }
                    }
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(3.dp)
                            .clip(CircleShape)
                            .background(lineColor),
                    )
                }
            }
        }

        // Invisible overlay numeric input
        BasicTextField(
            value = code,
            onValueChange = { onCodeChange(it.filter { c -> c.isDigit() }.take(5)) },
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
                    text = "DROP #${item.code}",
                    color = accent.copy(alpha = 0.55f),
                    fontFamily = JetBrainsMonoFamily,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    text = "Received Data",
                    color = HefimerColors.White,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                )
            }
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier
                    .clip(CircleShape)
                    .background(HefimerColors.White08)
                    .border(1.dp, HefimerColors.White12, CircleShape)
                    .applePressable(onClick = onBack)
                    .padding(horizontal = 14.dp, vertical = 9.dp),
            ) {
                Icon(Icons.AutoMirrored.Rounded.ArrowBack, null, tint = HefimerColors.White80, modifier = Modifier.size(15.dp))
                Text("Back", color = HefimerColors.White80, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
        }

        // Case 1: Multi-file drop
        val multiFiles = item.files
        if (!multiFiles.isNullOrEmpty()) {
            val totalSize = multiFiles.sumOf { it.resolvedSize }
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(32.dp))
                    .background(HefimerColors.White04)
                    .border(1.dp, if (isR2) HefimerColors.EmeraldBorder else HefimerColors.White15, RoundedCornerShape(32.dp))
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
                            .clip(RoundedCornerShape(14.dp))
                            .background(accent.copy(alpha = 0.12f))
                            .border(1.dp, accent.copy(alpha = 0.25f), RoundedCornerShape(14.dp)),
                    ) {
                        Icon(Icons.Rounded.FolderCopy, null, tint = accent, modifier = Modifier.size(22.dp))
                    }
                    Column {
                        Text("Shared Files", color = HefimerColors.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                        Text(
                            text = "${multiFiles.size} items • ${formatBytes(totalSize)}",
                            color = HefimerColors.White40,
                            fontSize = 12.sp,
                        )
                    }
                }
                Box(Modifier.fillMaxWidth().height(0.5.dp).background(HefimerColors.White10))

                multiFiles.forEach { fileEntry ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(18.dp))
                            .background(HefimerColors.Black.copy(alpha = 0.45f))
                            .border(1.dp, HefimerColors.White08, RoundedCornerShape(18.dp))
                            .padding(12.dp),
                    ) {
                        val (icon, iconTint) = fileIconForName(fileEntry.resolvedName, isR2)
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .size(38.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(HefimerColors.White06),
                        ) {
                            Icon(icon, null, tint = iconTint, modifier = Modifier.size(18.dp))
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = fileEntry.resolvedName,
                                color = HefimerColors.White90,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                            Text(
                                text = formatBytes(fileEntry.resolvedSize),
                                color = HefimerColors.White30,
                                fontFamily = JetBrainsMonoFamily,
                                fontSize = 10.sp,
                            )
                        }
                        Text(
                            text = "Download",
                            color = HefimerColors.Black,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .background(accent)
                                .applePressable { vm.downloadDropFile(fileEntry) }
                                .padding(horizontal = 12.dp, vertical = 7.dp),
                        )
                    }
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                HefimerPrimaryButton(
                    text = "Download All",
                    icon = Icons.Rounded.Download,
                    accentColor = accent,
                    modifier = Modifier.weight(1.2f),
                    onClick = { vm.downloadAllDropFiles(multiFiles) },
                )
                HefimerSecondaryButton(
                    text = if (copiedLink) "Copied!" else "Copy Link",
                    icon = if (copiedLink) Icons.Rounded.Check else Icons.Rounded.ContentCopy,
                    modifier = Modifier.weight(1f),
                    height = 54.dp,
                    onClick = {
                        copyToClipboard(context, "Hefimer Link", dropShareUrl)
                        copiedLink = true
                        vm.showToast("Link copied", ToastType.SUCCESS)
                        scope.launch { delay(2000L); copiedLink = false }
                    },
                )
            }
        } else if (isR2) {
            // Case 2: Secret R2 Single File
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(34.dp))
                    .background(HefimerColors.EmeraldBg)
                    .border(1.dp, HefimerColors.EmeraldBorder, RoundedCornerShape(34.dp))
                    .padding(28.dp),
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(64.dp)
                        .clip(CircleShape)
                        .background(HefimerColors.Emerald400.copy(alpha = 0.16f))
                        .border(1.dp, HefimerColors.EmeraldBorder, CircleShape),
                ) {
                    Icon(Icons.Rounded.Shield, null, tint = HefimerColors.Emerald400, modifier = Modifier.size(30.dp))
                }
                Text(
                    text = item.fileName ?: "Secret File",
                    color = HefimerColors.White,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                )
                Text(
                    text = "SECRET CLOUDFLARE R2 STORAGE",
                    color = HefimerColors.Emerald400.copy(alpha = 0.6f),
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

            HefimerPrimaryButton(
                text = "Download from R2",
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
            // Case 3: Standard Single File
            val downloadUrl = item.resolvedUrl
            val isTab = DownloadHelper.getDirectDownloadMode(downloadUrl) == DirectDownloadMode.TAB
            val providerName = UploadProviders.getProviderNameFromUrl(downloadUrl, false)

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(34.dp))
                    .background(HefimerColors.White04)
                    .border(1.dp, HefimerColors.White15, RoundedCornerShape(34.dp))
                    .padding(28.dp),
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(64.dp)
                        .clip(CircleShape)
                        .background(HefimerColors.White10)
                        .border(1.dp, HefimerColors.White15, CircleShape),
                ) {
                    Icon(Icons.Rounded.UploadFile, null, tint = HefimerColors.White, modifier = Modifier.size(30.dp))
                }
                Text(
                    text = item.fileName ?: "Unknown File",
                    color = HefimerColors.White,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                )
                Text(
                    text = "HOSTED ON ${providerName.uppercase()}",
                    color = HefimerColors.White40,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 2.sp,
                )
            }

            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                HefimerPrimaryButton(
                    text = if (isTab) "Open in $providerName" else "Download",
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
                    text = if (copiedLink) "Copied!" else "Copy Link",
                    icon = if (copiedLink) Icons.Rounded.Check else Icons.Rounded.ContentCopy,
                    modifier = Modifier.weight(1f),
                    height = 54.dp,
                    onClick = {
                        copyToClipboard(context, "Hefimer Link", dropShareUrl)
                        copiedLink = true
                        vm.showToast("Link copied", ToastType.SUCCESS)
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
                    .clip(RoundedCornerShape(30.dp))
                    .background(HefimerColors.Black)
                    .border(1.dp, HefimerColors.White15, RoundedCornerShape(30.dp)),
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(HefimerColors.SurfaceCard)
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = lang.label.uppercase(),
                        color = HefimerColors.White70,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.8.sp,
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(HefimerColors.White10)
                            .padding(horizontal = 12.dp, vertical = 5.dp),
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .size(34.dp)
                                .clip(CircleShape)
                                .background(if (showLineNumbers) HefimerColors.White else HefimerColors.White06)
                                .applePressable { showLineNumbers = !showLineNumbers },
                        ) {
                            Icon(
                                if (showLineNumbers) Icons.Rounded.VisibilityOff else Icons.Rounded.Visibility,
                                null,
                                tint = if (showLineNumbers) HefimerColors.Black else HefimerColors.White60,
                                modifier = Modifier.size(15.dp),
                            )
                        }
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .size(34.dp)
                                .clip(CircleShape)
                                .background(HefimerColors.White06)
                                .applePressable { showRenameDialog = true },
                        ) {
                            Icon(Icons.Rounded.Download, null, tint = HefimerColors.White60, modifier = Modifier.size(15.dp))
                        }
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .size(34.dp)
                                .clip(CircleShape)
                                .background(if (copiedText) HefimerColors.White else HefimerColors.White06)
                                .applePressable {
                                    copyToClipboard(context, "Hefimer Text", textBody)
                                    copiedText = true
                                    vm.showToast(tr("Text copied", "Đã sao chép văn bản"), ToastType.SUCCESS)
                                    scope.launch { delay(2000L); copiedText = false }
                                },
                        ) {
                            Icon(
                                if (copiedText) Icons.Rounded.Check else Icons.Rounded.ContentCopy,
                                null,
                                tint = if (copiedText) HefimerColors.Black else HefimerColors.White60,
                                modifier = Modifier.size(15.dp),
                            )
                        }
                    }
                }

                Box(Modifier.fillMaxWidth().height(0.5.dp).background(HefimerColors.White10))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 140.dp, max = 380.dp)
                        .verticalScroll(rememberScrollState())
                        .horizontalScroll(rememberScrollState())
                        .padding(18.dp),
                ) {
                    if (showLineNumbers) {
                        Text(
                            text = (1..lineCount).joinToString("\n"),
                            color = HefimerColors.White20,
                            fontFamily = JetBrainsMonoFamily,
                            fontSize = 12.sp,
                            lineHeight = 20.sp,
                            textAlign = TextAlign.End,
                            modifier = Modifier.padding(end = 14.dp),
                        )
                    }
                    Text(
                        text = highlighted,
                        fontFamily = JetBrainsMonoFamily,
                        fontSize = 13.5.sp,
                        lineHeight = 20.sp,
                        color = HefimerColors.White,
                    )
                }

                Box(Modifier.fillMaxWidth().height(0.5.dp).background(HefimerColors.White10))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(HefimerColors.SurfaceCard)
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                ) {
                    Text("$lineCount ${tr("Lines", "Dòng")}", color = HefimerColors.White30, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    Text("${textBody.length} ${tr("Chars", "Ký tự")}", color = HefimerColors.White30, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    Text(formatBytes(textBody.toByteArray().size.toLong()), color = HefimerColors.White30, fontSize = 10.sp, fontWeight = FontWeight.Bold)
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
                            copyToClipboard(context, "Hefimer Text", textBody)
                            copiedText = true
                            vm.showToast(tr("Text copied", "Đã sao chép văn bản"), ToastType.SUCCESS)
                            scope.launch { delay(2000L); copiedText = false }
                        },
                    )
                } else {
                    HefimerPrimaryButton(
                        text = if (copiedText) tr("Copied!", "Đã chép!") else tr("Copy Text", "Sao chép văn bản"),
                        icon = if (copiedText) Icons.Rounded.Check else Icons.Rounded.ContentCopy,
                        onClick = {
                            copyToClipboard(context, "Hefimer Text", textBody)
                            copiedText = true
                            vm.showToast(tr("Text copied", "Đã sao chép văn bản"), ToastType.SUCCESS)
                            scope.launch { delay(2000L); copiedText = false }
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
                            }.onSuccess {
                                vm.showToast(tr("Saved $it to Downloads", "Đã lưu $it vào Tải xuống"), ToastType.SUCCESS)
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
        else -> Icons.Rounded.InsertDriveFile to if (isR2) HefimerColors.Emerald400 else HefimerColors.White70
    }
}

@Composable
private fun UnlockPasswordDialog(
    onDismiss: () -> Unit,
    onUnlock: (String) -> Unit,
) {
    var pw by remember { mutableStateOf("") }
    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Box(modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp)) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(36.dp))
                    .background(HefimerColors.SurfaceCard)
                    .border(1.dp, HefimerColors.White12, RoundedCornerShape(36.dp))
                    .padding(24.dp),
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(56.dp)
                        .clip(CircleShape)
                        .background(HefimerColors.White06)
                        .border(1.dp, HefimerColors.White12, CircleShape),
                ) {
                    Icon(Icons.Rounded.Lock, null, tint = HefimerColors.White, modifier = Modifier.size(24.dp))
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(tr("PROTECTED DROP", "NỘI DUNG CÓ MẬT KHẨU"), color = HefimerColors.White, fontSize = 13.sp, fontWeight = FontWeight.Bold, letterSpacing = 2.sp)
                    Spacer(Modifier.height(4.dp))
                    Text(tr("Enter password to unlock", "Nhập mật khẩu để mở khóa"), color = HefimerColors.White40, fontSize = 11.sp)
                }
                HefimerTextField(
                    value = pw,
                    onValueChange = { pw = it },
                    placeholder = tr("Password...", "Mật khẩu..."),
                    isPassword = true,
                )
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    HefimerSecondaryButton(tr("Cancel", "Hủy"), onClick = onDismiss, modifier = Modifier.weight(1f))
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
    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Box(modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp)) {
            Column(
                verticalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(32.dp))
                    .background(HefimerColors.SurfaceCard)
                    .border(1.dp, HefimerColors.White12, RoundedCornerShape(32.dp))
                    .padding(22.dp),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Rounded.Edit, null, tint = HefimerColors.White, modifier = Modifier.size(16.dp))
                    Text(tr("SAVE AS FILE", "LƯU THÀNH TỆP"), color = HefimerColors.White, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 2.sp)
                }
                HefimerTextField(value = input, onValueChange = { input = it }, placeholder = "filename.txt", mono = true)
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    HefimerSecondaryButton(tr("Cancel", "Hủy"), onClick = onDismiss, modifier = Modifier.weight(1f))
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
