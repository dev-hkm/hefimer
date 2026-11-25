package com.hefimer.app.ui.screens

import android.content.ClipboardManager
import android.content.Context
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Logout
import androidx.compose.material.icons.automirrored.rounded.Send
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.ContentPaste
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Description
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.Group
import androidx.compose.material.icons.rounded.GroupAdd
import androidx.compose.material.icons.rounded.Inventory2
import androidx.compose.material.icons.rounded.Laptop
import androidx.compose.material.icons.rounded.Link
import androidx.compose.material.icons.rounded.QrCode2
import androidx.compose.material.icons.rounded.QrCodeScanner
import androidx.compose.material.icons.rounded.Radio
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Smartphone
import androidx.compose.material.icons.rounded.SwapHoriz
import androidx.compose.material.icons.rounded.VerifiedUser
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.hefimer.app.data.model.DeviceTransfer
import com.hefimer.app.data.model.GroupMember
import com.hefimer.app.data.model.ToastType
import com.hefimer.app.data.model.formatBytes
import com.hefimer.app.data.model.formatRemainingCompact
import com.hefimer.app.data.network.HefimerApiConfig
import com.hefimer.app.ui.AppOverlay
import com.hefimer.app.ui.HefimerViewModel
import com.hefimer.app.ui.LinkUiState
import com.hefimer.app.ui.ScanTarget
import com.hefimer.app.ui.components.BeautifulQrCodeView
import com.hefimer.app.ui.components.HefimerCollapsibleScaffold
import com.hefimer.app.ui.components.HefimerHeaderIconButton
import com.hefimer.app.ui.components.HefimerPrimaryButton
import com.hefimer.app.ui.components.HefimerSecondaryButton
import com.hefimer.app.ui.components.HefimerToggleRow
import com.hefimer.app.ui.components.copyToClipboard
import com.hefimer.app.ui.theme.AppMotion
import com.hefimer.app.ui.theme.AppleGlassVariant
import com.hefimer.app.ui.theme.AppleSegmentedControl
import com.hefimer.app.ui.theme.HefimerColors
import com.hefimer.app.ui.theme.JetBrainsMonoFamily
import com.hefimer.app.ui.theme.appleEntrance
import com.hefimer.app.ui.theme.appleGlassSurface
import com.hefimer.app.ui.theme.applePressable
import com.hefimer.app.ui.theme.rememberReduceMotion
import com.hefimer.app.ui.theme.tr
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.net.URLEncoder

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun LinkScreen(vm: HefimerViewModel) {
    val state by vm.linkState.collectAsState()
    val reduceMotion = rememberReduceMotion()
    val group = state.sync.group

    HefimerCollapsibleScaffold(
        kicker = "Hefimer Link",
        title = tr("Paired Devices", "Thiết bị liên kết"),
        subtitle = tr(
            "Pair trusted devices once without accounts, then push drops directly to your group.",
            "Ghép nối thiết bị tin cậy không cần tài khoản để gửi trực tiếp cho nhau.",
        ),
        showOrbitMark = true,
        headerActions = {
            HefimerHeaderIconButton(
                icon = Icons.Rounded.QrCodeScanner,
                contentDescription = tr("Scan pairing QR", "Quét QR ghép nối"),
                onClick = { vm.openOverlay(AppOverlay.QrScanner(ScanTarget.PAIR_TOKEN)) },
            )
            HefimerHeaderIconButton(
                icon = Icons.Rounded.Refresh,
                contentDescription = tr("Refresh", "Làm mới"),
                onClick = { vm.refreshLink(silent = false) },
            )
        },
    ) {
        when {
            state.loading -> {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 80.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                ) {
                    CircularProgressIndicator(color = HefimerColors.White, strokeWidth = 2.dp, modifier = Modifier.size(28.dp))
                    Text(tr("SECURING THIS DEVICE", "ĐANG BẢO MẬT THIẾT BỊ"), color = HefimerColors.White40, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 2.4.sp)
                }
            }
            state.error.isNotBlank() && state.sync.device.id.isBlank() -> {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .appleGlassSurface(
                            shape = RoundedCornerShape(34.dp),
                            variant = AppleGlassVariant.Regular,
                            elevation = 12.dp,
                        )
                        .padding(28.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Text(tr("Device service unavailable", "Dịch vụ thiết bị tạm gián đoạn"), color = HefimerColors.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    Text(state.error, color = HefimerColors.White40, fontSize = 12.sp, textAlign = TextAlign.Center)
                    HefimerPrimaryButton(tr("Try Again", "Thử lại"), onClick = { vm.refreshLink(silent = false) })
                }
            }
            group == null -> {
                LinkSetupContent(vm = vm, state = state, reduceMotion = reduceMotion)
            }
            else -> {
                LinkActiveGroupContent(vm = vm, state = state, reduceMotion = reduceMotion)
            }
        }
    }

    if (state.showPairModal) {
        JoinByCodeOrQrDialog(
            state = state,
            onTokenChange = { vm.setLinkPairToken(it) },
            onScanQr = {
                vm.closeLinkPairModal()
                vm.openOverlay(AppOverlay.QrScanner(ScanTarget.PAIR_TOKEN))
            },
            onPair = {
                vm.joinLinkGroup(leaveCurrentFirst = state.sync.group != null)
            },
            onDismiss = { vm.closeLinkPairModal() },
        )
    }
}

private fun isValidPairInput(raw: String): Boolean {
    val trimmed = raw.trim()
    if (Regex("""hfm-[A-Za-z0-9]{36}""").containsMatchIn(trimmed)) return true
    val digits = trimmed.filter { it.isDigit() }
    return digits.length == 5 && trimmed.length <= 8
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun LinkSetupContent(vm: HefimerViewModel, state: LinkUiState, reduceMotion: Boolean) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        // Accountless Identity Hero Card
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .appleEntrance(0)
                .appleGlassSurface(
                    shape = RoundedCornerShape(34.dp),
                    variant = AppleGlassVariant.Regular,
                    elevation = 12.dp,
                )
                .padding(22.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Icon(Icons.Rounded.VerifiedUser, null, tint = HefimerColors.White, modifier = Modifier.size(26.dp))
            Spacer(Modifier.height(6.dp))
            Text(tr("ACCOUNTLESS IDENTITY", "ĐỊNH DANH KHÔNG TÀI KHOẢN"), color = HefimerColors.White50, fontSize = 9.sp, fontWeight = FontWeight.Bold, letterSpacing = 2.4.sp)
            Text(tr("This phone is now a signed device.", "Thiết bị này đã sẵn sàng ghép nối."), color = HefimerColors.White, fontSize = 22.sp, fontWeight = FontWeight.Bold, letterSpacing = (-0.6).sp)
            Text(
                text = tr(
                    "Your P-256 private key stays locally on this device. Pair once with browsers or phones, no email or password required.",
                    "Khóa bảo mật P-256 chỉ lưu tại máy bạn. Ghép nối một lần với máy tính hoặc điện thoại khác, không cần email hay mật khẩu.",
                ),
                color = HefimerColors.White40,
                fontSize = 13.sp,
                lineHeight = 19.sp,
            )
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier
                    .padding(top = 4.dp)
                    .appleGlassSurface(
                        shape = CircleShape,
                        variant = AppleGlassVariant.Clear,
                        elevation = 0.dp,
                    )
                    .padding(horizontal = 12.dp, vertical = 7.dp),
            ) {
                Icon(Icons.Rounded.Smartphone, null, tint = HefimerColors.White50, modifier = Modifier.size(14.dp))
                Text(state.sync.device.name.ifBlank { state.deviceNameInput }, color = HefimerColors.White80, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
        }

        // Create / Enter Token Switch
        AppleSegmentedControl(
            options = listOf("create", "join"),
            selected = state.setupMode,
            label = { if (it == "create") tr("Create Group", "Tạo nhóm") else tr("Enter Token", "Nhập mã ghép") },
            onSelect = { vm.setLinkSetupMode(it) },
            modifier = Modifier.appleEntrance(1),
        )

        AnimatedContent(
            targetState = state.setupMode,
            transitionSpec = { AppMotion.sectionTransform(reduceMotion) },
            label = "link_setup_mode",
        ) { mode ->
            Column(
                verticalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .appleGlassSurface(
                        shape = RoundedCornerShape(34.dp),
                        variant = AppleGlassVariant.Regular,
                        elevation = 12.dp,
                    )
                    .padding(20.dp),
            ) {
                if (mode == "create") {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(tr("GROUP NAME", "TÊN NHÓM"), color = HefimerColors.White30, fontSize = 9.sp, fontWeight = FontWeight.Bold, letterSpacing = 2.sp)
                        HefimerTextField(
                            value = state.groupName,
                            onValueChange = { vm.setLinkGroupName(it.take(64)) },
                            placeholder = tr("My Hefimer Group", "Nhóm Hefimer của tôi"),
                        )
                    }
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(tr("LINK LIFETIME", "THỜI HẠN LIÊN KẾT"), color = HefimerColors.White30, fontSize = 9.sp, fontWeight = FontWeight.Bold, letterSpacing = 2.sp)
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            listOf(
                                "12h" to tr("12 hours", "12 giờ"),
                                "24h" to tr("24 hours", "24 giờ"),
                                "7d" to tr("7 days", "7 ngày"),
                                "30d" to tr("30 days", "30 ngày"),
                                "never" to tr("Forever", "Vĩnh viễn"),
                            ).forEach { (v, lbl) ->
                                val sel = state.duration == v
                                val chipFill by animateColorAsState(
                                    targetValue = if (sel) HefimerColors.White else Color.Transparent,
                                    animationSpec = tween(AppMotion.Fast, easing = AppMotion.AppleSilk),
                                    label = "link_dur_fill_$v",
                                )
                                val chipText by animateColorAsState(
                                    targetValue = if (sel) HefimerColors.Black else HefimerColors.White60,
                                    animationSpec = tween(AppMotion.Fast, easing = AppMotion.AppleSilk),
                                    label = "link_dur_text_$v",
                                )
                                Text(
                                    text = lbl,
                                    color = chipText,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier
                                        .appleGlassSurface(
                                            shape = RoundedCornerShape(14.dp),
                                            variant = AppleGlassVariant.Clear,
                                            accentBorderTint = if (sel) HefimerColors.White else null,
                                            accentFillTint = chipFill,
                                            elevation = 0.dp,
                                        )
                                        .applePressable(pressedScale = 0.93f) { vm.setLinkDuration(v) }
                                        .padding(horizontal = 14.dp, vertical = 10.dp),
                                )
                            }
                        }
                    }
                    HefimerPrimaryButton(
                        text = if (state.busyKey == "create") tr("Creating...", "Đang tạo...") else tr("Create Secure Group", "Tạo nhóm bảo mật"),
                        icon = Icons.Rounded.Group,
                        loading = state.busyKey == "create",
                        enabled = state.busyKey.isEmpty(),
                        onClick = { vm.createLinkGroup() },
                    )
                } else {
                    val context = LocalContext.current
                    Column(
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .appleGlassSurface(
                                shape = RoundedCornerShape(24.dp),
                                variant = AppleGlassVariant.Clear,
                                elevation = 0.dp,
                            )
                            .padding(16.dp),
                    ) {
                        Icon(Icons.Rounded.QrCode2, null, tint = HefimerColors.White, modifier = Modifier.size(22.dp))
                        Text(tr("Scan pairing QR or enter 5-digit / token code.", "Quét mã QR ghép nối hoặc nhập mã 5 số / token."), color = HefimerColors.White, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                        Text(tr("Accepts 5-digit Hefimer Link codes, hfm-... tokens, or full ?pair= URLs. Expires in 10 minutes.", "Hỗ trợ nhập mã ghép 5 chữ số, token hfm-... hoặc dán link ?pair=. Hết hạn sau 10 phút."), color = HefimerColors.White40, fontSize = 12.sp)
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                        HefimerSecondaryButton(
                            text = tr("Scan QR", "Quét mã QR"),
                            icon = Icons.Rounded.QrCodeScanner,
                            modifier = Modifier.weight(1f),
                            onClick = { vm.openOverlay(AppOverlay.QrScanner(ScanTarget.PAIR_TOKEN)) },
                        )
                        HefimerSecondaryButton(
                            text = tr("Paste Code", "Dán mã"),
                            icon = Icons.Rounded.ContentPaste,
                            modifier = Modifier.weight(1f),
                            onClick = {
                                val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                val clip = cm.primaryClip?.getItemAt(0)?.text?.toString()?.trim().orEmpty()
                                if (clip.isNotBlank()) {
                                    vm.setLinkPairToken(clip)
                                    vm.showToast(tr("Pasted pairing code", "Đã dán mã ghép nối"), ToastType.INFO)
                                } else {
                                    vm.showToast(tr("Clipboard is empty", "Bộ nhớ tạm đang trống"), ToastType.ERROR)
                                }
                            },
                        )
                    }

                    HefimerTextField(
                        value = state.pairToken,
                        onValueChange = { vm.setLinkPairToken(it) },
                        placeholder = tr("5-digit code (e.g. 48291) or hfm-...", "Mã 5 số (vd: 48291) hoặc hfm-..."),
                        mono = true,
                    )

                    val validToken = isValidPairInput(state.pairToken)
                    HefimerPrimaryButton(
                        text = if (state.busyKey == "join") tr("Pairing...", "Đang ghép nối...") else tr("Pair This Device", "Ghép nối thiết bị này"),
                        icon = Icons.Rounded.Link,
                        loading = state.busyKey == "join",
                        enabled = validToken && state.busyKey.isEmpty(),
                        onClick = { vm.joinLinkGroup() },
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun LinkActiveGroupContent(vm: HefimerViewModel, state: LinkUiState, reduceMotion: Boolean) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val group = state.sync.group ?: return
    val isOwner = group.ownerDeviceId == state.sync.device.id
    val pendingCount = state.sync.incoming.count { it.recipientStatus in setOf("pending", "approved", "downloading") }
    var copiedToken by remember { mutableStateOf(false) }

    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        // Active Group Hero Card with subtle concentric orbital rings matching web DeviceHub
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .appleEntrance(0)
                .appleGlassSurface(
                    shape = RoundedCornerShape(34.dp),
                    variant = AppleGlassVariant.Regular,
                    elevation = 12.dp,
                ),
        ) {
            Canvas(modifier = Modifier.matchParentSize()) {
                val center = Offset(size.width - 10.dp.toPx(), 10.dp.toPx())
                drawCircle(HefimerColors.White.copy(alpha = 0.08f), radius = 92.dp.toPx(), center = center, style = Stroke(1.dp.toPx()))
                drawCircle(HefimerColors.White.copy(alpha = 0.05f), radius = 56.dp.toPx(), center = center, style = Stroke(1.dp.toPx()))
            }

            Column(modifier = Modifier.padding(22.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Icon(Icons.Rounded.Radio, null, tint = HefimerColors.White70, modifier = Modifier.size(13.dp))
                    Text(tr("ACTIVE GROUP", "NHÓM ĐANG HOẠT ĐỘNG"), color = HefimerColors.White50, fontSize = 9.sp, fontWeight = FontWeight.Bold, letterSpacing = 2.4.sp)
                }
                Spacer(Modifier.height(10.dp))
                Text(
                    text = group.name,
                    color = HefimerColors.White,
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = (-1).sp,
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    text = "${state.sync.members.size} ${tr(if (state.sync.members.size == 1) "device" else "devices", "thiết bị")} · ${formatRemainingCompact(group.expiresAt)}",
                    color = HefimerColors.White40,
                    fontSize = 13.sp,
                )
            }
        }

        // Devices / Activity segmented control
        AppleSegmentedControl(
            options = listOf("devices", "activity"),
            selected = state.section,
            label = {
                if (it == "devices") {
                    tr("Devices", "Thiết bị")
                } else if (pendingCount > 0) {
                    "${tr("Activity", "Hoạt động")} ($pendingCount)"
                } else {
                    tr("Activity", "Hoạt động")
                }
            },
            onSelect = { vm.setLinkSection(it) },
            modifier = Modifier.appleEntrance(1),
        )

        AnimatedContent(
            targetState = state.section,
            transitionSpec = { AppMotion.sectionTransform(reduceMotion) },
            label = "link_active_section",
        ) { sec ->
            if (sec == "devices") {
                Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    // Connected Devices Card
                    Column(
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .appleGlassSurface(
                                shape = RoundedCornerShape(30.dp),
                                variant = AppleGlassVariant.Regular,
                                elevation = 10.dp,
                            )
                            .padding(18.dp),
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Column {
                                Text(tr("TRUSTED LANE", "LUỒNG TIN CẬY"), color = HefimerColors.White30, fontSize = 9.sp, fontWeight = FontWeight.Bold, letterSpacing = 2.2.sp)
                                Text(tr("Connected devices", "Thiết bị đã kết nối"), color = HefimerColors.White, fontSize = 17.sp, fontWeight = FontWeight.Bold)
                            }
                            Text(
                                text = "${state.sync.members.count { it.online }} ${tr("online", "trực tuyến")}",
                                color = HefimerColors.White60,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier
                                    .appleGlassSurface(
                                        shape = CircleShape,
                                        variant = AppleGlassVariant.Clear,
                                        elevation = 0.dp,
                                    )
                                    .padding(horizontal = 10.dp, vertical = 4.dp),
                            )
                        }

                        state.sync.members.forEach { member ->
                            MemberRow(
                                member = member,
                                isSelf = member.id == state.sync.device.id,
                                canRemove = isOwner && member.id != state.sync.device.id,
                                onRemove = {
                                    vm.requestConfirm(
                                        title = tr("Remove Device?", "Gỡ thiết bị khỏi nhóm?"),
                                        message = tr(
                                            "Unpair \"${member.name}\" from this Hefimer Link group?",
                                            "Ngắt ghép nối \"${member.name}\" khỏi nhóm Hefimer Link này?",
                                        ),
                                        confirmLabel = tr("Remove", "Gỡ thiết bị"),
                                        onConfirm = { vm.removeLinkDevice(member.id) },
                                    )
                                },
                            )
                        }

                        // Rename this device
                        Column(
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier
                                .padding(top = 6.dp)
                                .fillMaxWidth()
                                .appleGlassSurface(
                                    shape = RoundedCornerShape(22.dp),
                                    variant = AppleGlassVariant.Clear,
                                    elevation = 0.dp,
                                )
                                .padding(12.dp),
                        ) {
                            Text(tr("THIS DEVICE NAME", "TÊN THIẾT BỊ NÀY"), color = HefimerColors.White30, fontSize = 9.sp, fontWeight = FontWeight.Bold, letterSpacing = 2.sp)
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                                HefimerTextField(
                                    value = state.deviceNameInput,
                                    onValueChange = { vm.setLinkDeviceNameInput(it.take(48)) },
                                    placeholder = tr("Device name", "Tên thiết bị"),
                                    modifier = Modifier.weight(1f),
                                )
                                Text(
                                    text = tr("SAVE", "LƯU"),
                                    color = HefimerColors.White,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 1.4.sp,
                                    modifier = Modifier
                                        .appleGlassSurface(
                                            shape = RoundedCornerShape(16.dp),
                                            variant = AppleGlassVariant.Clear,
                                            accentBorderTint = HefimerColors.White20,
                                            accentFillTint = HefimerColors.White10,
                                            elevation = 0.dp,
                                        )
                                        .applePressable(pressedScale = 0.90f) { vm.renameLinkDevice() }
                                        .padding(horizontal = 14.dp, vertical = 14.dp),
                                )
                            }
                        }
                    }

                    // Owner One-Time Pairing Invite Card
                    if (isOwner) {
                        Column(
                            verticalArrangement = Arrangement.spacedBy(14.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .appleGlassSurface(
                                    shape = RoundedCornerShape(30.dp),
                                    variant = AppleGlassVariant.Regular,
                                    elevation = 10.dp,
                                )
                                .padding(18.dp),
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Column {
                                    Text(tr("ONE-TIME ENTRANCE", "MÃ MỜI MỘT LẦN"), color = HefimerColors.White40, fontSize = 9.sp, fontWeight = FontWeight.Bold, letterSpacing = 2.2.sp)
                                    Text(tr("Pair another device", "Ghép nối thiết bị khác"), color = HefimerColors.White, fontSize = 17.sp, fontWeight = FontWeight.Bold)
                                }
                                Icon(Icons.Rounded.GroupAdd, null, tint = HefimerColors.White50, modifier = Modifier.size(22.dp))
                            }

                            val inv = state.invite
                            if (inv == null) {
                                HefimerPrimaryButton(
                                    text = if (state.busyKey == "invite") tr("Generating...", "Đang tạo...") else tr("Generate QR + Token", "Tạo mã QR + Token"),
                                    icon = Icons.Rounded.QrCode2,
                                    loading = state.busyKey == "invite",
                                    height = 48.dp,
                                    onClick = { vm.createLinkInvite() },
                                )
                            } else {
                                val pairUrl = "${HefimerApiConfig.SITE_ORIGIN}/?pair=${URLEncoder.encode(inv.token, "UTF-8")}"
                                Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                                    BeautifulQrCodeView(value = pairUrl, size = 180.dp)
                                }
                                if (state.inviteShortCode.length == 5) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .appleGlassSurface(
                                                shape = RoundedCornerShape(18.dp),
                                                variant = AppleGlassVariant.Clear,
                                                elevation = 0.dp,
                                            )
                                            .applePressable(pressedScale = 0.96f) {
                                                copyToClipboard(context, "Pair Code", state.inviteShortCode)
                                                vm.showToast(tr("5-digit pairing code copied", "Đã sao chép mã ghép 5 số"), ToastType.SUCCESS)
                                            }
                                            .padding(horizontal = 14.dp, vertical = 12.dp),
                                    ) {
                                        Column {
                                            Text(tr("5-DIGIT PAIR CODE", "MÃ GHÉP 5 CHỮ SỐ"), color = HefimerColors.White40, fontSize = 9.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.6.sp)
                                            Text(state.inviteShortCode, color = HefimerColors.White, fontFamily = JetBrainsMonoFamily, fontSize = 22.sp, fontWeight = FontWeight.Bold, letterSpacing = 4.sp)
                                        }
                                        Icon(Icons.Rounded.ContentCopy, tr("Copy code", "Sao chép mã"), tint = HefimerColors.White75, modifier = Modifier.size(16.dp))
                                    }
                                }
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .appleGlassSurface(
                                            shape = RoundedCornerShape(18.dp),
                                            variant = AppleGlassVariant.Clear,
                                            elevation = 0.dp,
                                        )
                                        .applePressable(pressedScale = 0.96f) {
                                            copyToClipboard(context, "Pairing Token", inv.token)
                                            copiedToken = true
                                            vm.showToast(tr("Pairing token copied", "Đã sao chép mã ghép nối"), ToastType.SUCCESS)
                                            scope.launch { delay(1800L); copiedToken = false }
                                        }
                                        .padding(12.dp),
                                ) {
                                    Text(
                                        text = inv.token,
                                        color = HefimerColors.White60,
                                        fontFamily = JetBrainsMonoFamily,
                                        fontSize = 10.sp,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier.weight(1f),
                                    )
                                    Icon(
                                        if (copiedToken) Icons.Rounded.Check else Icons.Rounded.ContentCopy,
                                        null,
                                        tint = HefimerColors.White,
                                        modifier = Modifier.size(15.dp),
                                    )
                                }
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                ) {
                                    Text(tr("SINGLE USE", "DÙNG 1 LẦN"), color = HefimerColors.White30, fontSize = 9.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.4.sp)
                                    Text(formatRemainingCompact(inv.expiresAt), color = HefimerColors.White30, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                                }
                                HefimerSecondaryButton(tr("Replace Token", "Đổi mã mới"), height = 42.dp, onClick = { vm.createLinkInvite() })
                            }
                        }
                    }

                    // Always Approve toggle
                    HefimerToggleRow(
                        title = tr("Always approve", "Luôn tự động chấp nhận"),
                        subtitle = tr("Accept & auto-download file offers from this group", "Tự nhận và tải xuống tệp từ nhóm này"),
                        checked = group.autoApprove,
                        onToggle = { vm.toggleLinkAutoApprove() },
                        icon = Icons.Rounded.Inventory2,
                        activeColor = HefimerColors.White,
                    )

                    // Pair with another group via code or QR (always accessible even when in a group)
                    HefimerSecondaryButton(
                        text = tr("Enter Code / Pair Another Group", "Nhập mã / Ghép nối nhóm khác"),
                        icon = Icons.Rounded.SwapHoriz,
                        onClick = { vm.openLinkPairModal() },
                    )

                    // Leave / Delete Group (with confirmation modal)
                    HefimerSecondaryButton(
                        text = if (isOwner) tr("Delete Group For Everyone", "Xóa nhóm cho tất cả") else tr("Leave This Group", "Rời khỏi nhóm này"),
                        icon = Icons.AutoMirrored.Rounded.Logout,
                        onClick = {
                            vm.requestConfirm(
                                title = if (isOwner) tr("Delete Device Group?", "Xóa nhóm thiết bị?") else tr("Leave This Group?", "Rời khỏi nhóm ghép nối?"),
                                message = if (isOwner) {
                                    tr(
                                        "This will permanently unpair all ${state.sync.members.size} devices and delete \"${group.name}\".",
                                        "Thao tác này sẽ ngắt ghép nối toàn bộ ${state.sync.members.size} thiết bị và xóa nhóm \"${group.name}\".",
                                    )
                                } else {
                                    tr(
                                        "This phone will stop receiving direct drops from \"${group.name}\".",
                                        "Thiết bị này sẽ ngừng nhận tệp trực tiếp từ nhóm \"${group.name}\".",
                                    )
                                },
                                confirmLabel = if (isOwner) tr("Delete Group", "Xóa nhóm") else tr("Leave Group", "Rời nhóm"),
                                onConfirm = { vm.leaveLinkGroup() },
                            )
                        },
                    )
                }
            } else {
                // Activity section (Incoming + Outgoing transfers)
                Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    // Incoming
                    Column(
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .appleGlassSurface(
                                shape = RoundedCornerShape(30.dp),
                                variant = AppleGlassVariant.Regular,
                                elevation = 10.dp,
                            )
                            .padding(18.dp),
                    ) {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Column {
                                Text(tr("INCOMING", "ĐẾN"), color = HefimerColors.White40, fontSize = 9.sp, fontWeight = FontWeight.Bold, letterSpacing = 2.2.sp)
                                Text(tr("Waiting for you", "Đang chờ bạn"), color = HefimerColors.White, fontSize = 17.sp, fontWeight = FontWeight.Bold)
                            }
                            Icon(Icons.Rounded.Download, null, tint = HefimerColors.White40, modifier = Modifier.size(20.dp))
                        }
                        if (state.sync.incoming.isEmpty()) {
                            Text(
                                text = tr("Nothing incoming yet.", "Chưa có tệp nào gửi đến."),
                                color = HefimerColors.White30,
                                fontSize = 12.sp,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.fillMaxWidth().padding(vertical = 24.dp),
                            )
                        } else {
                            state.sync.incoming.forEach { tr ->
                                IncomingTransferRow(
                                    transfer = tr,
                                    onDecline = { vm.decideLinkTransfer(tr, "decline") },
                                    onApprove = { vm.decideLinkTransfer(tr, "approve") },
                                    onOpen = { vm.openLinkTransfer(tr) },
                                )
                            }
                        }
                    }

                    // Outgoing
                    Column(
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .appleGlassSurface(
                                shape = RoundedCornerShape(30.dp),
                                variant = AppleGlassVariant.Regular,
                                elevation = 10.dp,
                            )
                            .padding(18.dp),
                    ) {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Column {
                                Text(tr("OUTGOING", "ĐI"), color = HefimerColors.White40, fontSize = 9.sp, fontWeight = FontWeight.Bold, letterSpacing = 2.2.sp)
                                Text(tr("Sent from here", "Đã gửi từ đây"), color = HefimerColors.White, fontSize = 17.sp, fontWeight = FontWeight.Bold)
                            }
                            Icon(Icons.AutoMirrored.Rounded.Send, null, tint = HefimerColors.White40, modifier = Modifier.size(19.dp))
                        }
                        if (state.sync.outgoing.isEmpty()) {
                            Text(
                                text = tr("Your next drop will appear here automatically.", "Các tệp bạn gửi tiếp theo sẽ hiện tại đây."),
                                color = HefimerColors.White30,
                                fontSize = 12.sp,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.fillMaxWidth().padding(vertical = 24.dp),
                            )
                        } else {
                            state.sync.outgoing.forEach { tr ->
                                OutgoingTransferRow(
                                    transfer = tr,
                                    onCancel = {
                                        vm.requestConfirm(
                                            title = tr("Cancel Transfer?", "Hủy gửi tệp?"),
                                            message = tr(
                                                "Cancel pending delivery of \"${tr.name}\" to paired devices?",
                                                "Hủy lượt gửi \"${tr.name}\" tới các thiết bị đang ghép nối?",
                                            ),
                                            confirmLabel = tr("Cancel Offer", "Hủy gửi"),
                                            onConfirm = { vm.cancelLinkTransfer(tr.id) },
                                        )
                                    },
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MemberRow(
    member: GroupMember,
    isSelf: Boolean,
    canRemove: Boolean,
    onRemove: () -> Unit,
) {
    val isMobile = member.platform.contains("Android", true) || member.platform.contains("iPhone", true) || member.platform.contains("iPad", true)
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier
            .fillMaxWidth()
            .appleGlassSurface(
                shape = RoundedCornerShape(18.dp),
                variant = AppleGlassVariant.Clear,
                elevation = 0.dp,
            )
            .padding(12.dp),
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(if (member.online) HefimerColors.White else HefimerColors.White06),
        ) {
            Icon(
                imageVector = if (isMobile) Icons.Rounded.Smartphone else Icons.Rounded.Laptop,
                contentDescription = null,
                tint = if (member.online) HefimerColors.Black else HefimerColors.White40,
                modifier = Modifier.size(19.dp),
            )
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(member.name, color = HefimerColors.White, fontSize = 14.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
            val meta = buildString {
                append(member.platform)
                if (isSelf) append(" · ${tr("This device", "Thiết bị này")}")
                if (member.isOwner) append(" · ${tr("Owner", "Chủ nhóm")}")
            }
            Text(meta, color = HefimerColors.White40, fontSize = 11.sp)
        }
        if (canRemove) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(34.dp)
                    .appleGlassSurface(
                        shape = CircleShape,
                        variant = AppleGlassVariant.Clear,
                        elevation = 0.dp,
                    )
                    .applePressable(pressedScale = 0.88f, onClick = onRemove),
            ) {
                Icon(Icons.Rounded.DeleteOutline, tr("Remove", "Gỡ"), tint = HefimerColors.White50, modifier = Modifier.size(16.dp))
            }
        }
    }
}

@Composable
private fun IncomingTransferRow(
    transfer: DeviceTransfer,
    onDecline: () -> Unit,
    onApprove: () -> Unit,
    onOpen: () -> Unit,
) {
    val status = transfer.recipientStatus ?: "pending"
    Column(
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier
            .fillMaxWidth()
            .appleGlassSurface(
                shape = RoundedCornerShape(20.dp),
                variant = AppleGlassVariant.Clear,
                elevation = 0.dp,
            )
            .padding(14.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
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
                Icon(if (transfer.kind == "file") Icons.Rounded.Download else Icons.Rounded.Description, null, tint = HefimerColors.White, modifier = Modifier.size(18.dp))
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(transfer.name, color = HefimerColors.White, fontSize = 14.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text("${transfer.senderName ?: tr("Paired device", "Thiết bị đã ghép")} · ${formatBytes(transfer.sizeBytes)}", color = HefimerColors.White40, fontSize = 11.sp)
            }
            LinkStatusPill(status)
        }

        if (status == "pending") {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                HefimerSecondaryButton(tr("Decline", "Từ chối"), height = 40.dp, modifier = Modifier.weight(1f), onClick = onDecline)
                HefimerPrimaryButton(tr("Approve", "Chấp nhận"), height = 40.dp, modifier = Modifier.weight(1f), onClick = onApprove)
            }
        } else if (status in setOf("approved", "downloading", "received")) {
            HefimerPrimaryButton(tr("Open Transfer", "Mở tệp nhận"), icon = Icons.Rounded.ChevronRight, height = 42.dp, onClick = onOpen)
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun OutgoingTransferRow(
    transfer: DeviceTransfer,
    onCancel: () -> Unit,
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(10.dp),
        modifier = Modifier
            .fillMaxWidth()
            .appleGlassSurface(
                shape = RoundedCornerShape(20.dp),
                variant = AppleGlassVariant.Clear,
                elevation = 0.dp,
            )
            .padding(14.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.weight(1f)) {
                Text(transfer.name, color = HefimerColors.White, fontSize = 14.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text("${transfer.provider} · ${formatRemainingCompact(transfer.expiresAt)}", color = HefimerColors.White40, fontSize = 11.sp)
            }
            LinkStatusPill(transfer.transferStatus)
        }
        if (!transfer.recipients.isNullOrEmpty()) {
            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                transfer.recipients.forEach { rec ->
                    val dotColor = when (rec.status) {
                        "received" -> HefimerColors.White
                        "declined", "failed" -> HefimerColors.White20
                        else -> HefimerColors.White60
                    }
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(5.dp),
                        modifier = Modifier
                            .appleGlassSurface(
                                shape = CircleShape,
                                variant = AppleGlassVariant.Clear,
                                elevation = 0.dp,
                            )
                            .padding(horizontal = 8.dp, vertical = 4.dp),
                    ) {
                        Box(Modifier.size(6.dp).clip(CircleShape).background(dotColor))
                        Text(rec.deviceName, color = HefimerColors.White50, fontSize = 10.sp)
                    }
                }
            }
        }
        if (transfer.transferStatus == "active") {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                modifier = Modifier
                    .appleGlassSurface(
                        shape = CircleShape,
                        variant = AppleGlassVariant.Clear,
                        elevation = 0.dp,
                    )
                    .applePressable(pressedScale = 0.94f, onClick = onCancel)
                    .padding(horizontal = 10.dp, vertical = 5.dp),
            ) {
                Icon(Icons.Rounded.Close, null, tint = HefimerColors.White50, modifier = Modifier.size(13.dp))
                Text(tr("CANCEL OFFER", "HỦY GỬI"), color = HefimerColors.White50, fontSize = 9.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.4.sp)
            }
        }
    }
}

@Composable
private fun LinkStatusPill(status: String) {
    val label = when (status) {
        "pending" -> tr("WAITING", "ĐANG CHỜ")
        "approved" -> tr("READY", "SẴN SÀNG")
        "declined" -> tr("DECLINED", "TỪ CHỐI")
        "downloading" -> tr("OPENING", "ĐANG MỞ")
        "received" -> tr("RECEIVED", "ĐÃ NHẬN")
        "failed" -> tr("FAILED", "LỖI")
        "cancelled" -> tr("CANCELLED", "ĐÃ HỦY")
        "expired" -> tr("EXPIRED", "HẾT HẠN")
        else -> status.uppercase()
    }
    val active = status in setOf("approved", "received")
    Text(
        text = label,
        color = if (active) HefimerColors.White else HefimerColors.White50,
        fontSize = 8.sp,
        fontWeight = FontWeight.Bold,
        letterSpacing = 1.4.sp,
        modifier = Modifier
            .appleGlassSurface(
                shape = CircleShape,
                variant = AppleGlassVariant.Clear,
                accentBorderTint = if (active) HefimerColors.White20 else null,
                accentFillTint = if (active) HefimerColors.White10 else null,
                elevation = 0.dp,
            )
            .padding(horizontal = 9.dp, vertical = 4.dp),
    )
}

/**
 * Floating incoming transfer prompt banner (shown above bottom dock when a paired device sends a drop).
 */
@Composable
fun FloatingIncomingOfferBanner(
    transfer: DeviceTransfer?,
    onApprove: (DeviceTransfer) -> Unit,
    onDecline: (DeviceTransfer) -> Unit,
    modifier: Modifier = Modifier,
) {
    AnimatedVisibility(
        visible = transfer != null,
        enter = AppMotion.floatingBarEnter,
        exit = AppMotion.floatingBarExit,
        modifier = modifier.fillMaxWidth().padding(horizontal = 16.dp),
    ) {
        val current = transfer ?: return@AnimatedVisibility
        Column(
            verticalArrangement = Arrangement.spacedBy(14.dp),
            modifier = Modifier
                .fillMaxWidth()
                .appleGlassSurface(
                    shape = RoundedCornerShape(34.dp),
                    variant = AppleGlassVariant.Elevated,
                    elevation = 20.dp,
                )
                .padding(18.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(46.dp)
                        .appleGlassSurface(
                            shape = RoundedCornerShape(16.dp),
                            variant = AppleGlassVariant.Clear,
                            elevation = 0.dp,
                        ),
                ) {
                    Icon(if (current.kind == "file") Icons.Rounded.Download else Icons.Rounded.Description, null, tint = HefimerColors.White, modifier = Modifier.size(21.dp))
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = tr(
                            "INCOMING FROM ${(current.senderName ?: "PAIRED DEVICE").uppercase()}",
                            "ĐANG GỬI TỪ ${(current.senderName ?: "THIẾT BỊ LIÊN KẾT").uppercase()}",
                        ),
                        color = HefimerColors.White60,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 2.sp,
                    )
                    Text(current.name, color = HefimerColors.White, fontSize = 16.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text("${formatBytes(current.sizeBytes)} · ${current.provider}", color = HefimerColors.White40, fontSize = 11.sp)
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                HefimerSecondaryButton(tr("Decline", "Từ chối"), height = 44.dp, modifier = Modifier.weight(1f), onClick = { onDecline(current) })
                HefimerPrimaryButton(tr("Approve", "Chấp nhận"), icon = Icons.Rounded.Check, height = 44.dp, modifier = Modifier.weight(1f), onClick = { onApprove(current) })
            }
        }
    }
}

@Composable
private fun JoinByCodeOrQrDialog(
    state: LinkUiState,
    onTokenChange: (String) -> Unit,
    onScanQr: () -> Unit,
    onPair: () -> Unit,
    onDismiss: () -> Unit,
) {
    val context = LocalContext.current
    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Box(modifier = Modifier.fillMaxWidth().padding(horizontal = 22.dp)) {
            Column(
                verticalArrangement = Arrangement.spacedBy(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .appleGlassSurface(
                        shape = RoundedCornerShape(34.dp),
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
                        Text(tr("PAIR WITH CODE OR QR", "GHÉP BẰNG MÃ HOẶC QR"), color = HefimerColors.White45, fontSize = 9.sp, fontWeight = FontWeight.Bold, letterSpacing = 2.sp)
                        Text(tr("Join another group", "Tham gia nhóm khác"), color = HefimerColors.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    }
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(34.dp)
                            .appleGlassSurface(CircleShape, AppleGlassVariant.Clear, elevation = 0.dp)
                            .applePressable(pressedScale = 0.89f, onClick = onDismiss),
                    ) {
                        Icon(Icons.Rounded.Close, tr("Close", "Đóng"), tint = HefimerColors.White60, modifier = Modifier.size(16.dp))
                    }
                }

                if (state.sync.group != null) {
                    Text(
                        text = tr(
                            "Pairing a new code will leave \"${state.sync.group.name}\" automatically.",
                            "Khi ghép mã mới, thiết bị sẽ tự rời nhóm \"${state.sync.group.name}\".",
                        ),
                        color = HefimerColors.White60,
                        fontSize = 11.5.sp,
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    HefimerSecondaryButton(
                        text = tr("Scan QR", "Quét QR"),
                        icon = Icons.Rounded.QrCodeScanner,
                        height = 44.dp,
                        modifier = Modifier.weight(1f),
                        onClick = onScanQr,
                    )
                    HefimerSecondaryButton(
                        text = tr("Paste", "Dán mã"),
                        icon = Icons.Rounded.ContentPaste,
                        height = 44.dp,
                        modifier = Modifier.weight(1f),
                        onClick = {
                            val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            val clip = cm.primaryClip?.getItemAt(0)?.text?.toString()?.trim().orEmpty()
                            if (clip.isNotBlank()) onTokenChange(clip)
                        },
                    )
                }

                HefimerTextField(
                    value = state.pairToken,
                    onValueChange = onTokenChange,
                    placeholder = tr("5-digit code or hfm-...", "Mã 5 số hoặc hfm-..."),
                    mono = true,
                )

                HefimerPrimaryButton(
                    text = if (state.busyKey == "join") tr("Pairing...", "Đang ghép...") else tr("Pair Now", "Ghép nối ngay"),
                    icon = Icons.Rounded.Link,
                    loading = state.busyKey == "join",
                    enabled = isValidPairInput(state.pairToken) && state.busyKey.isEmpty(),
                    onClick = onPair,
                )
            }
        }
    }
}
