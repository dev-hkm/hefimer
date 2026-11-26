package com.hefimer.app.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.Image
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.QrCode2
import androidx.compose.material.icons.rounded.QrCodeScanner
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.adamglin.PhosphorIcons
import com.adamglin.phosphoricons.Regular
import com.adamglin.phosphoricons.regular.ArrowSquareOut
import com.adamglin.phosphoricons.regular.CaretLeft
import com.adamglin.phosphoricons.regular.ChartBar
import com.adamglin.phosphoricons.regular.ChatsCircle
import com.adamglin.phosphoricons.regular.Check
import com.adamglin.phosphoricons.regular.Globe
import com.adamglin.phosphoricons.regular.GridFour
import com.adamglin.phosphoricons.regular.Trash
import com.hefimer.app.data.model.PollItem
import com.hefimer.app.data.model.ToastType
import com.hefimer.app.data.model.formatRemainingCompact
import com.hefimer.app.data.network.DownloadHelper
import com.hefimer.app.data.network.HefimerApiConfig
import com.hefimer.app.ui.AppOverlay
import com.hefimer.app.ui.HefimerViewModel
import com.hefimer.app.ui.ScanTarget
import com.hefimer.app.ui.components.HefimerCollapsibleScaffold
import com.hefimer.app.ui.components.HefimerHeaderIconButton
import com.hefimer.app.ui.components.HefimerImageLightbox
import com.hefimer.app.ui.components.HefimerPrimaryButton
import com.hefimer.app.ui.components.HefimerRemoteImage
import com.hefimer.app.ui.components.HefimerSecondaryButton
import com.hefimer.app.ui.components.HefimerToggleRow
import com.hefimer.app.ui.components.QrShareModal
import com.hefimer.app.ui.components.copyToClipboard
import com.hefimer.app.ui.theme.AppMotion
import com.hefimer.app.ui.theme.AppleFluidProgressTrack
import com.hefimer.app.ui.theme.AppleGlassVariant
import com.hefimer.app.ui.theme.AppleOdometerNumber
import com.hefimer.app.ui.theme.AppleSegmentedControl
import com.hefimer.app.ui.theme.HefimerColors
import com.hefimer.app.ui.theme.HefimerHaptics
import com.hefimer.app.ui.theme.JetBrainsMonoFamily
import com.hefimer.app.ui.theme.LocalHeroBounds
import com.hefimer.app.ui.theme.appleEntrance
import com.hefimer.app.ui.theme.appleGlassSurface
import com.hefimer.app.ui.theme.applePressable
import com.hefimer.app.ui.theme.heroSource
import com.hefimer.app.ui.theme.rememberReduceMotion
import com.hefimer.app.ui.theme.tr

private enum class ExploreTool { HUB, POLL }

@Composable
fun ExploreScreen(vm: HefimerViewModel) {
    val heroBounds = LocalHeroBounds.current
    val reduceMotion = rememberReduceMotion()
    val context = LocalContext.current
    var activeTool by remember { mutableStateOf(ExploreTool.HUB) }

    HefimerCollapsibleScaffold(
        kicker = tr("Lab & Community", "Công cụ & Cộng đồng"),
        title = tr("Explore", "Khám phá"),
        subtitle = tr(
            "Polls, collaborative spaces, and live rooms.",
            "Bình chọn 5 mã, không gian Space và phòng chat.",
        ),
        showOrbitMark = true,
        headerActions = {
            if (activeTool != ExploreTool.HUB) {
                HefimerHeaderIconButton(
                    icon = PhosphorIcons.Regular.CaretLeft,
                    contentDescription = tr("Back", "Quay lại"),
                    onClick = { activeTool = ExploreTool.HUB },
                )
            }
            HefimerHeaderIconButton(
                icon = Icons.Rounded.QrCodeScanner,
                contentDescription = tr("Scan QR", "Quét QR"),
                modifier = Modifier.heroSource("btn_qr_explore", heroBounds),
                onClick = { vm.openOverlay(AppOverlay.QrScanner(ScanTarget.AUTO), "btn_qr_explore") },
            )
            HefimerHeaderIconButton(
                icon = Icons.Rounded.History,
                contentDescription = tr("History", "Lịch sử"),
                modifier = Modifier.heroSource("btn_history_explore", heroBounds),
                onClick = { vm.openOverlay(AppOverlay.History, "btn_history_explore") },
            )
            HefimerHeaderIconButton(
                icon = Icons.Rounded.Settings,
                contentDescription = tr("Settings", "Cài đặt"),
                onClick = { vm.openSettings() },
            )
        },
    ) {
        AnimatedContent(
            targetState = activeTool,
            transitionSpec = { AppMotion.sectionTransform(reduceMotion) },
            label = "explore_tool_switch",
        ) { tool ->
            when (tool) {
                ExploreTool.HUB -> Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    // 1. Collaborative Spaces & Rooms (moved from bottom nav into Explore)
                    Row(
                        modifier = Modifier.fillMaxWidth().appleEntrance(0),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        ExploreBentoTile(
                            icon = PhosphorIcons.Regular.GridFour,
                            badge = tr("BOARD", "BẢNG"),
                            title = "Space",
                            subtitle = tr("Pin files, links & notes", "Ghim tệp, link & ghi chú"),
                            modifier = Modifier
                                .weight(1f)
                                .heroSource("explore_space", heroBounds),
                            onClick = { vm.openOverlay(AppOverlay.SpaceHub, "explore_space") },
                        )
                        ExploreBentoTile(
                            icon = PhosphorIcons.Regular.ChatsCircle,
                            badge = tr("LIVE", "TRỰC TIẾP"),
                            title = tr("Rooms", "Phòng"),
                            subtitle = tr("Chat & whiteboard", "Chat & bảng vẽ chung"),
                            modifier = Modifier
                                .weight(1f)
                                .heroSource("explore_rooms", heroBounds),
                            onClick = { vm.openOverlay(AppOverlay.RoomsHub, "explore_rooms") },
                        )
                    }

                    // 2. Live Poll Card (5-digit code, images, optional password, max 48h)
                    ExploreFeatureRow(
                        icon = PhosphorIcons.Regular.ChartBar,
                        title = tr("Live Poll (5-Digit Code)", "Bình chọn nhanh (Mã 5 số)"),
                        subtitle = tr(
                            "Create polls with images & optional password • up to 48h",
                            "Tạo bình chọn kèm ảnh & mật khẩu • tối đa 48 giờ",
                        ),
                        modifier = Modifier.appleEntrance(1),
                        onClick = { activeTool = ExploreTool.POLL },
                    )

                    // 3. Compact, minimal Web / PC availability card
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .appleEntrance(2)
                            .appleGlassSurface(shape = RoundedCornerShape(24.dp), variant = AppleGlassVariant.Clear, elevation = 0.dp)
                            .applePressable(pressedScale = 0.975f) {
                                runCatching {
                                    context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(HefimerApiConfig.SITE_ORIGIN)))
                                }
                            }
                            .padding(horizontal = 16.dp, vertical = 14.dp),
                    ) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .size(38.dp)
                                .appleGlassSurface(shape = CircleShape, variant = AppleGlassVariant.Clear, elevation = 0.dp),
                        ) {
                            Icon(PhosphorIcons.Regular.Globe, null, tint = HefimerColors.White, modifier = Modifier.size(18.dp))
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = tr("Available on PC & Web", "Có sẵn trên PC & Trình duyệt"),
                                color = HefimerColors.White,
                                fontSize = 13.5.sp,
                                fontWeight = FontWeight.Bold,
                            )
                            Text(
                                text = "hefimer.qzz.io",
                                color = HefimerColors.White50,
                                fontFamily = JetBrainsMonoFamily,
                                fontSize = 11.sp,
                            )
                        }
                        Icon(PhosphorIcons.Regular.ArrowSquareOut, null, tint = HefimerColors.White45, modifier = Modifier.size(16.dp))
                    }
                }

                ExploreTool.POLL -> PollSection(vm = vm)
            }
        }
    }
}

@Composable
private fun ExploreBentoTile(
    icon: ImageVector,
    badge: String,
    title: String,
    subtitle: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(10.dp),
        modifier = modifier
            .appleGlassSurface(shape = RoundedCornerShape(28.dp), variant = AppleGlassVariant.Regular, elevation = 8.dp)
            .applePressable(pressedScale = 0.965f, onClick = onClick)
            .padding(16.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(38.dp)
                    .appleGlassSurface(shape = RoundedCornerShape(14.dp), variant = AppleGlassVariant.Clear, elevation = 0.dp),
            ) {
                Icon(icon, null, tint = HefimerColors.White, modifier = Modifier.size(19.dp))
            }
            Text(
                text = badge,
                color = HefimerColors.White45,
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.4.sp,
            )
        }
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(text = title, color = HefimerColors.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
            Text(text = subtitle, color = HefimerColors.White45, fontSize = 11.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
        }
    }
}

@Composable
private fun ExploreFeatureRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
        modifier = modifier
            .fillMaxWidth()
            .appleGlassSurface(shape = RoundedCornerShape(28.dp), variant = AppleGlassVariant.Regular, elevation = 8.dp)
            .applePressable(pressedScale = 0.975f, onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 16.dp),
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(44.dp)
                .appleGlassSurface(shape = RoundedCornerShape(16.dp), variant = AppleGlassVariant.Clear, elevation = 0.dp),
        ) {
            Icon(icon, null, tint = HefimerColors.White, modifier = Modifier.size(21.dp))
        }
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(text = title, color = HefimerColors.White, fontSize = 15.sp, fontWeight = FontWeight.Bold)
            Text(text = subtitle, color = HefimerColors.White45, fontSize = 11.5.sp)
        }
        Box(
            modifier = Modifier
                .clip(CircleShape)
                .background(HefimerColors.White)
                .padding(horizontal = 12.dp, vertical = 6.dp),
        ) {
            Text(tr("Open", "Mở"), color = HefimerColors.Black, fontSize = 11.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun PollSection(vm: HefimerViewModel) {
    val state by vm.pollState.collectAsState()
    val context = LocalContext.current
    var pendingOptionImageTarget by remember { mutableStateOf<String?>(null) }
    var showQr by remember { mutableStateOf(false) }
    var pollLightbox by remember { mutableStateOf<Pair<String, String>?>(null) }

    val imagePicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) {
            vm.uploadPollImage(uri, pendingOptionImageTarget)
        }
    }

    val activePoll = state.activePoll
    if (activePoll != null) {
        LivePollView(
            poll = activePoll,
            votedIds = state.votedOptionIds,
            onConfirmVote = { selected ->
                HefimerHaptics.reactionPop(context)
                vm.submitPollSelection(selected)
            },
            onCopyCode = { code ->
                copyToClipboard(context, "Hefimer Poll", code)
                HefimerHaptics.success(context)
                vm.showToast(tr("Copied poll code #$code", "Đã sao chép mã bình chọn #$code"), ToastType.SUCCESS)
            },
            onShowQr = { showQr = true },
            onBack = { vm.closeActivePoll() },
        )
        if (showQr) {
            QrShareModal(
                title = tr("Share Poll #${activePoll.code}", "Chia sẻ bình chọn #${activePoll.code}"),
                url = "${HefimerApiConfig.SITE_ORIGIN}/?code=${activePoll.code}",
                code = activePoll.code,
                onDismiss = { showQr = false },
                onToast = { m, e -> vm.showToast(m, if (e) ToastType.ERROR else ToastType.SUCCESS) },
            )
        }
        return
    }

    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        AppleSegmentedControl(
            options = listOf("join", "create"),
            selected = state.lobbyMode,
            label = { if (it == "join") tr("Join Poll", "Tham gia") else tr("Create Poll", "Tạo bình chọn") },
            onSelect = { vm.setPollLobbyMode(it) },
        )

        if (state.lobbyMode == "join") {
            Column(
                verticalArrangement = Arrangement.spacedBy(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .animateContentSize(AppMotion.layoutSpring())
                    .appleGlassSurface(shape = RoundedCornerShape(30.dp), variant = AppleGlassVariant.Regular, elevation = 10.dp)
                    .padding(20.dp),
            ) {
                Text(
                    text = tr("ENTER 5-DIGIT POLL CODE", "NHẬP MÃ BÌNH CHỌN 5 SỐ"),
                    color = HefimerColors.White45,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 2.sp,
                )
                FiveDigitPinSlots(
                    code = state.joinCode,
                    onCodeChange = { vm.setPollJoinCode(it) },
                    onDone = { vm.joinPoll() },
                )
                AnimatedVisibility(visible = state.needsPassword, enter = AppMotion.softExpandEnter, exit = AppMotion.softExpandExit) {
                    HefimerTextField(
                        value = state.joinPassword,
                        onValueChange = { vm.setPollJoinPassword(it) },
                        placeholder = tr("Enter poll password...", "Nhập mật khẩu bình chọn..."),
                        isPassword = true,
                    )
                }
                HefimerPrimaryButton(
                    text = if (state.loading) tr("Opening...", "Đang mở...") else tr("Open Poll", "Mở bình chọn"),
                    loading = state.loading,
                    enabled = state.joinCode.length == 5 && !state.loading,
                    onClick = { vm.joinPoll() },
                )
            }
        } else {
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .animateContentSize(AppMotion.layoutSpring())
                    .appleGlassSurface(shape = RoundedCornerShape(30.dp), variant = AppleGlassVariant.Regular, elevation = 10.dp)
                    .padding(18.dp),
            ) {
                Text(
                    text = tr("QUESTION & COVER IMAGE", "CÂU HỎI & ẢNH BÌA"),
                    color = HefimerColors.White45,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.8.sp,
                )
                HefimerTextField(
                    value = state.question,
                    onValueChange = { vm.setPollQuestion(it) },
                    placeholder = tr("What should we vote on?", "Bạn muốn bình chọn điều gì?"),
                )

                // Cover image picker
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    HefimerSecondaryButton(
                        text = when {
                            state.uploadingCover -> tr("Uploading...", "Đang tải ảnh...")
                            state.coverImageUrl.isNotBlank() -> tr("Change Cover Image", "Đổi ảnh bìa")
                            else -> tr("Add Cover Image", "Thêm ảnh bìa")
                        },
                        icon = Icons.Rounded.Image,
                        height = 42.dp,
                        enabled = !state.uploadingCover,
                        modifier = Modifier.weight(1f),
                        onClick = {
                            pendingOptionImageTarget = null
                            imagePicker.launch("image/*")
                        },
                    )
                    if (state.coverImageUrl.isNotBlank()) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .size(42.dp)
                                .appleGlassSurface(CircleShape, AppleGlassVariant.Clear, elevation = 0.dp)
                                .applePressable(pressedScale = 0.9f) { vm.clearPollCoverImage() },
                        ) {
                            Icon(Icons.Rounded.Close, null, tint = HefimerColors.White70, modifier = Modifier.size(16.dp))
                        }
                    }
                }
                if (state.coverImageUrl.isNotBlank()) {
                    HefimerRemoteImage(
                        model = state.coverImageUrl,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(140.dp)
                            .clip(RoundedCornerShape(18.dp))
                            .border(1.dp, HefimerColors.White12, RoundedCornerShape(18.dp)),
                        onClick = { pollLightbox = state.coverImageUrl to tr("Cover Image", "Ảnh bìa bình chọn") },
                    )
                }

                Text(
                    text = tr("OPTIONS (2–6, OPTIONAL IMAGES)", "LỰA CHỌN (2–6, CÓ THỂ GẮN ẢNH)"),
                    color = HefimerColors.White45,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.8.sp,
                )

                state.draftOptions.forEachIndexed { idx, opt ->
                    Column(
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .appleGlassSurface(RoundedCornerShape(18.dp), AppleGlassVariant.Clear, elevation = 0.dp)
                            .padding(10.dp),
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            HefimerTextField(
                                value = opt.label,
                                onValueChange = { vm.updatePollDraftOptionLabel(opt.id, it) },
                                placeholder = tr("Option ${idx + 1}", "Lựa chọn ${idx + 1}"),
                                modifier = Modifier.weight(1f),
                            )
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier
                                    .size(40.dp)
                                    .appleGlassSurface(CircleShape, AppleGlassVariant.Clear, elevation = 0.dp)
                                    .applePressable(enabled = !opt.uploadingImage, pressedScale = 0.9f) {
                                        pendingOptionImageTarget = opt.id
                                        imagePicker.launch("image/*")
                                    },
                            ) {
                                if (opt.uploadingImage) {
                                    CircularProgressIndicator(color = HefimerColors.White, strokeWidth = 1.8.dp, modifier = Modifier.size(16.dp))
                                } else {
                                    Icon(Icons.Rounded.Image, null, tint = if (opt.imageUrl.isNotBlank()) HefimerColors.White else HefimerColors.White50, modifier = Modifier.size(17.dp))
                                }
                            }
                            if (state.draftOptions.size > 2) {
                                Box(
                                    contentAlignment = Alignment.Center,
                                    modifier = Modifier
                                        .size(40.dp)
                                        .appleGlassSurface(CircleShape, AppleGlassVariant.Clear, elevation = 0.dp)
                                        .applePressable(pressedScale = 0.9f) { vm.removePollDraftOption(opt.id) },
                                ) {
                                    Icon(PhosphorIcons.Regular.Trash, null, tint = HefimerColors.Red400, modifier = Modifier.size(16.dp))
                                }
                            }
                        }
                        if (opt.imageUrl.isNotBlank()) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                HefimerRemoteImage(
                                    model = opt.imageUrl,
                                    compact = true,
                                    modifier = Modifier.size(44.dp).clip(RoundedCornerShape(10.dp)),
                                    onClick = { pollLightbox = opt.imageUrl to (opt.label.ifBlank { tr("Option Image", "Ảnh lựa chọn") }) },
                                )
                                Text(tr("Image ready", "Đã gắn ảnh"), color = HefimerColors.White60, fontSize = 11.sp, modifier = Modifier.weight(1f))
                                Box(
                                    contentAlignment = Alignment.Center,
                                    modifier = Modifier
                                        .size(26.dp)
                                        .clip(CircleShape)
                                        .background(HefimerColors.White10)
                                        .applePressable(pressedScale = 0.9f) { vm.clearPollDraftOptionImage(opt.id) },
                                ) {
                                    Icon(Icons.Rounded.Close, null, tint = HefimerColors.White70, modifier = Modifier.size(13.dp))
                                }
                            }
                        }
                    }
                }

                if (state.draftOptions.size < 6) {
                    HefimerSecondaryButton(
                        text = tr("Add Option", "Thêm lựa chọn"),
                        icon = Icons.Rounded.Add,
                        height = 42.dp,
                        onClick = { vm.addPollDraftOption() },
                    )
                }

                HefimerToggleRow(
                    title = tr("Allow Multiple Choices", "Cho phép chọn nhiều"),
                    subtitle = tr("Voters can select more than one option", "Người tham gia có thể chọn nhiều đáp án"),
                    checked = state.allowMultiple,
                    onToggle = { vm.setPollAllowMultiple(!state.allowMultiple) },
                    icon = PhosphorIcons.Regular.Check,
                    activeColor = HefimerColors.White,
                )

                HefimerToggleRow(
                    title = tr("Allow Changing Vote", "Cho phép đổi lựa chọn"),
                    subtitle = tr("Voters can re-select and update their choice", "Người tham gia có thể chọn lại đáp án khác"),
                    checked = state.allowRevote,
                    onToggle = { vm.setPollAllowRevote(!state.allowRevote) },
                    icon = Icons.Rounded.QrCode2,
                    activeColor = HefimerColors.White,
                )

                Text(
                    text = tr("EXPIRES IN (MAX 48H)", "THỜI HẠN (TỐI ĐA 48 GIỜ)"),
                    color = HefimerColors.White45,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.8.sp,
                )
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    listOf("1h", "6h", "12h", "24h", "48h").forEach { exp ->
                        val active = state.expire == exp
                        val chipBg by animateColorAsState(if (active) HefimerColors.White else HefimerColors.White06, tween(AppMotion.Fast, easing = AppMotion.AppleSilk), label = "poll_exp")
                        Box(
                            modifier = Modifier
                                .clip(CircleShape)
                                .background(chipBg)
                                .border(1.dp, if (active) HefimerColors.White else HefimerColors.White12, CircleShape)
                                .applePressable(pressedScale = 0.94f) { vm.setPollExpire(exp) }
                                .padding(horizontal = 14.dp, vertical = 7.dp),
                        ) {
                            Text(exp, color = if (active) HefimerColors.Black else HefimerColors.White70, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                HefimerTextField(
                    value = state.createPassword,
                    onValueChange = { vm.setPollCreatePassword(it) },
                    placeholder = tr("Optional password...", "Mật khẩu tùy chọn..."),
                    isPassword = true,
                )

                HefimerPrimaryButton(
                    text = if (state.loading) tr("Creating Poll...", "Đang tạo bình chọn...") else tr("Create Poll", "Tạo bình chọn"),
                    loading = state.loading,
                    enabled = !state.loading && !state.uploadingCover && state.draftOptions.none { it.uploadingImage },
                    onClick = { vm.createPoll() },
                )
            }
        }
    }

    pollLightbox?.let { (imgUrl, imgTitle) ->
        HefimerImageLightbox(
            model = imgUrl,
            title = imgTitle,
            onDismiss = { pollLightbox = null },
        )
    }
}

@Composable
private fun LivePollView(
    poll: PollItem,
    votedIds: Set<String>,
    onConfirmVote: (Set<String>) -> Unit,
    onCopyCode: (String) -> Unit,
    onShowQr: () -> Unit,
    onBack: () -> Unit,
) {
    val context = LocalContext.current
    val total = poll.totalVotes.coerceAtLeast(0)
    var draftSelection by remember(poll.code, votedIds) { mutableStateOf(votedIds) }
    val canChange = votedIds.isEmpty() || poll.allowRevote
    val hasPendingChange = draftSelection.isNotEmpty() && draftSelection != votedIds

    Column(
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier
            .fillMaxWidth()
            .animateContentSize(AppMotion.layoutSpring())
            .appleGlassSurface(shape = RoundedCornerShape(30.dp), variant = AppleGlassVariant.Regular, elevation = 10.dp)
            .padding(18.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "#${poll.code}",
                    color = HefimerColors.Black,
                    fontFamily = JetBrainsMonoFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(HefimerColors.White)
                        .applePressable(pressedScale = 0.92f) { onCopyCode(poll.code) }
                        .padding(horizontal = 10.dp, vertical = 4.dp),
                )
                Text(
                    text = formatRemainingCompact(poll.expiresAt),
                    color = HefimerColors.White50,
                    fontSize = 11.sp,
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(34.dp)
                        .appleGlassSurface(CircleShape, AppleGlassVariant.Clear, elevation = 0.dp)
                        .applePressable(pressedScale = 0.9f, onClick = onShowQr),
                ) {
                    Icon(Icons.Rounded.QrCode2, null, tint = HefimerColors.White, modifier = Modifier.size(16.dp))
                }
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(34.dp)
                        .appleGlassSurface(CircleShape, AppleGlassVariant.Clear, elevation = 0.dp)
                        .applePressable(pressedScale = 0.9f, onClick = onBack),
                ) {
                    Icon(Icons.Rounded.Close, null, tint = HefimerColors.White70, modifier = Modifier.size(16.dp))
                }
            }
        }

        if (poll.coverImageUrl.isNotBlank()) {
            var showCoverLightbox by remember(poll.coverImageUrl) { mutableStateOf(false) }
            HefimerRemoteImage(
                model = poll.coverImageUrl,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(160.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .border(1.dp, HefimerColors.White12, RoundedCornerShape(20.dp)),
                onClick = { showCoverLightbox = true },
            )
            if (showCoverLightbox) {
                HefimerImageLightbox(
                    model = poll.coverImageUrl,
                    title = poll.question,
                    onDismiss = { showCoverLightbox = false },
                )
            }
        }

        Text(
            text = poll.question,
            color = HefimerColors.White,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
        )

        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            AppleOdometerNumber(value = total, color = HefimerColors.White75)
            Text(
                text = tr("total votes", "lượt bình chọn") +
                    if (poll.allowMultiple) " • ${tr("Multi-select", "Chọn nhiều")}" else "",
                color = HefimerColors.White45,
                fontSize = 12.sp,
            )
        }

        poll.options.forEach { opt ->
            val selected = opt.id in draftSelection
            val confirmed = opt.id in votedIds
            val frac = if (total > 0) (opt.votes.toFloat() / total.toFloat()).coerceIn(0f, 1f) else 0f
            val pct = (frac * 100f).toInt()
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .appleGlassSurface(
                        shape = RoundedCornerShape(20.dp),
                        variant = AppleGlassVariant.Clear,
                        accentBorderTint = if (selected) HefimerColors.White else null,
                        accentFillTint = if (selected) HefimerColors.White08 else null,
                        elevation = 0.dp,
                    )
                    .applePressable(enabled = canChange, pressedScale = 0.975f) {
                        HefimerHaptics.tick(context)
                        draftSelection = if (poll.allowMultiple) {
                            if (opt.id in draftSelection) draftSelection - opt.id else draftSelection + opt.id
                        } else {
                            setOf(opt.id)
                        }
                    }
                    .padding(12.dp),
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    if (opt.imageUrl.isNotBlank()) {
                        var showOptLightbox by remember(opt.imageUrl) { mutableStateOf(false) }
                        HefimerRemoteImage(
                            model = opt.imageUrl,
                            compact = true,
                            modifier = Modifier.size(48.dp).clip(RoundedCornerShape(12.dp)),
                            onClick = { showOptLightbox = true },
                        )
                        if (showOptLightbox) {
                            HefimerImageLightbox(
                                model = opt.imageUrl,
                                title = opt.label.ifBlank { tr("Option Image", "Ảnh lựa chọn") },
                                onDismiss = { showOptLightbox = false },
                            )
                        }
                    }
                    Text(
                        text = opt.label,
                        color = HefimerColors.White,
                        fontSize = 14.sp,
                        fontWeight = if (selected || confirmed) FontWeight.Bold else FontWeight.Medium,
                        modifier = Modifier.weight(1f),
                    )
                    if (selected) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier.size(20.dp).clip(CircleShape).background(HefimerColors.White),
                        ) {
                            Icon(PhosphorIcons.Regular.Check, null, tint = HefimerColors.Black, modifier = Modifier.size(12.dp))
                        }
                    }
                    Text(
                        text = "$pct% (${opt.votes})",
                        color = if (selected || confirmed) HefimerColors.White else HefimerColors.White60,
                        fontFamily = JetBrainsMonoFamily,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                    )
                }
                AppleFluidProgressTrack(
                    progress = frac,
                    accentColor = if (selected || confirmed) HefimerColors.White else HefimerColors.White60,
                    trackColor = HefimerColors.White10,
                    height = 5.dp,
                )
            }
        }

        AnimatedVisibility(visible = hasPendingChange, enter = AppMotion.softExpandEnter, exit = AppMotion.softExpandExit) {
            HefimerPrimaryButton(
                text = if (votedIds.isEmpty()) {
                    tr("Confirm Vote", "Xác nhận bình chọn")
                } else {
                    tr("Update Choice", "Cập nhật lựa chọn")
                },
                icon = PhosphorIcons.Regular.Check,
                onClick = { onConfirmVote(draftSelection) },
            )
        }

        if (votedIds.isNotEmpty() && !hasPendingChange) {
            Text(
                text = if (poll.allowRevote) {
                    tr("Tap another option to change your vote", "Chạm lựa chọn khác nếu bạn muốn đổi ý")
                } else {
                    tr("Your vote is confirmed and locked", "Bình chọn của bạn đã được xác nhận và khóa")
                },
                color = HefimerColors.White45,
                fontSize = 11.sp,
            )
        }
    }
}
