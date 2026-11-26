package com.hefimer.app.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
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
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
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
import androidx.compose.runtime.mutableIntStateOf
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
import coil.compose.AsyncImage
import com.adamglin.PhosphorIcons
import com.adamglin.phosphoricons.Regular
import com.adamglin.phosphoricons.regular.ArrowSquareOut
import com.adamglin.phosphoricons.regular.CaretLeft
import com.adamglin.phosphoricons.regular.ChartBar
import com.adamglin.phosphoricons.regular.ChatsCircle
import com.adamglin.phosphoricons.regular.Check
import com.adamglin.phosphoricons.regular.Globe
import com.adamglin.phosphoricons.regular.GridFour
import com.adamglin.phosphoricons.regular.Play
import com.adamglin.phosphoricons.regular.Pause
import com.adamglin.phosphoricons.regular.ArrowCounterClockwise
import com.adamglin.phosphoricons.regular.Shuffle
import com.adamglin.phosphoricons.regular.Timer
import com.adamglin.phosphoricons.regular.Trash
import com.hefimer.app.data.model.PollItem
import com.hefimer.app.data.model.ToastType
import com.hefimer.app.data.model.formatRemainingCompact
import com.hefimer.app.data.network.HefimerApiConfig
import com.hefimer.app.ui.AppOverlay
import com.hefimer.app.ui.HefimerViewModel
import com.hefimer.app.ui.ScanTarget
import com.hefimer.app.ui.components.HefimerCollapsibleScaffold
import com.hefimer.app.ui.components.HefimerHeaderIconButton
import com.hefimer.app.ui.components.HefimerPrimaryButton
import com.hefimer.app.ui.components.HefimerSecondaryButton
import com.hefimer.app.ui.components.HefimerToggleRow
import com.hefimer.app.ui.components.QrShareModal
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
import kotlinx.coroutines.delay

private enum class ExploreTool { HUB, POLL, TIMER, PICKER }

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
            "Polls, collaborative spaces, rooms, and quick lab utilities.",
            "Bình chọn 5 mã, không gian Space, phòng chat và tiện ích phòng tin.",
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

                    // 3. Lab Timer & Random Picker (zero-server utilities)
                    Row(
                        modifier = Modifier.fillMaxWidth().appleEntrance(2),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        ExploreBentoTile(
                            icon = PhosphorIcons.Regular.Timer,
                            badge = tr("FOCUS", "TẬP TRUNG"),
                            title = tr("Lab Timer", "Đếm ngược"),
                            subtitle = tr("Pomodoro & countdown", "Pomodoro & hẹn giờ"),
                            modifier = Modifier.weight(1f),
                            onClick = { activeTool = ExploreTool.TIMER },
                        )
                        ExploreBentoTile(
                            icon = PhosphorIcons.Regular.Shuffle,
                            badge = tr("TEAM", "NHÓM"),
                            title = tr("Randomize", "Quay ngẫu nhiên"),
                            subtitle = tr("Pick winner or split teams", "Chọn tên hoặc chia nhóm"),
                            modifier = Modifier.weight(1f),
                            onClick = { activeTool = ExploreTool.PICKER },
                        )
                    }

                    // 4. Compact, minimal Web / PC availability card
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .appleEntrance(3)
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
                ExploreTool.TIMER -> LabTimerSection()
                ExploreTool.PICKER -> RandomPickerSection(vm = vm)
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

    val imagePicker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) {
            vm.uploadPollImage(uri, pendingOptionImageTarget)
        }
    }

    val activePoll = state.activePoll
    if (activePoll != null) {
        LivePollView(
            poll = activePoll,
            votedIds = state.votedOptionIds,
            onVote = { optId ->
                HefimerHaptics.reactionPop(context)
                vm.votePoll(optId)
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
                            imagePicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
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
                    AsyncImage(
                        model = state.coverImageUrl,
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(140.dp)
                            .clip(RoundedCornerShape(18.dp))
                            .border(1.dp, HefimerColors.White12, RoundedCornerShape(18.dp)),
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
                                        imagePicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
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
                                AsyncImage(
                                    model = opt.imageUrl,
                                    contentDescription = null,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.size(44.dp).clip(RoundedCornerShape(10.dp)),
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
}

@Composable
private fun LivePollView(
    poll: PollItem,
    votedIds: Set<String>,
    onVote: (String) -> Unit,
    onShowQr: () -> Unit,
    onBack: () -> Unit,
) {
    val total = poll.totalVotes.coerceAtLeast(0)
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
                    modifier = Modifier.clip(CircleShape).background(HefimerColors.White).padding(horizontal = 10.dp, vertical = 4.dp),
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
            AsyncImage(
                model = poll.coverImageUrl,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(160.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .border(1.dp, HefimerColors.White12, RoundedCornerShape(20.dp)),
            )
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
            val picked = opt.id in votedIds
            val frac = if (total > 0) (opt.votes.toFloat() / total.toFloat()).coerceIn(0f, 1f) else 0f
            val pct = (frac * 100f).toInt()
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .appleGlassSurface(
                        shape = RoundedCornerShape(20.dp),
                        variant = AppleGlassVariant.Clear,
                        accentBorderTint = if (picked) HefimerColors.White else null,
                        accentFillTint = if (picked) HefimerColors.White08 else null,
                        elevation = 0.dp,
                    )
                    .applePressable(pressedScale = 0.975f) { onVote(opt.id) }
                    .padding(12.dp),
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    if (opt.imageUrl.isNotBlank()) {
                        AsyncImage(
                            model = opt.imageUrl,
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.size(48.dp).clip(RoundedCornerShape(12.dp)),
                        )
                    }
                    Text(
                        text = opt.label,
                        color = HefimerColors.White,
                        fontSize = 14.sp,
                        fontWeight = if (picked) FontWeight.Bold else FontWeight.Medium,
                        modifier = Modifier.weight(1f),
                    )
                    Text(
                        text = "$pct% (${opt.votes})",
                        color = if (picked) HefimerColors.White else HefimerColors.White60,
                        fontFamily = JetBrainsMonoFamily,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                    )
                }
                AppleFluidProgressTrack(
                    progress = frac,
                    accentColor = if (picked) HefimerColors.White else HefimerColors.White60,
                    trackColor = HefimerColors.White10,
                    height = 5.dp,
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun LabTimerSection() {
    val context = LocalContext.current
    var presetSec by remember { mutableIntStateOf(25 * 60) }
    var remainingSec by remember { mutableIntStateOf(25 * 60) }
    var running by remember { mutableStateOf(false) }

    LaunchedEffect(running) {
        while (running && remainingSec > 0) {
            delay(1000L)
            remainingSec = (remainingSec - 1).coerceAtLeast(0)
            if (remainingSec == 0) {
                running = false
                HefimerHaptics.success(context)
            }
        }
    }

    val mins = remainingSec / 60
    val secs = remainingSec % 60
    val frac = if (presetSec > 0) (1f - remainingSec.toFloat() / presetSec.toFloat()).coerceIn(0f, 1f) else 0f

    Column(
        verticalArrangement = Arrangement.spacedBy(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .fillMaxWidth()
            .appleGlassSurface(RoundedCornerShape(30.dp), AppleGlassVariant.Regular, elevation = 10.dp)
            .padding(22.dp),
    ) {
        Text(
            text = tr("LAB FOCUS TIMER", "ĐỒNG HỒ PHÒNG TIN"),
            color = HefimerColors.White45,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 2.sp,
        )
        Text(
            text = "%02d:%02d".format(mins, secs),
            color = HefimerColors.White,
            fontFamily = JetBrainsMonoFamily,
            fontWeight = FontWeight.Bold,
            fontSize = 48.sp,
        )
        AppleFluidProgressTrack(progress = frac, accentColor = HefimerColors.White, trackColor = HefimerColors.White10, height = 6.dp)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf(1 to "1m", 5 to "5m", 10 to "10m", 25 to "25m").forEach { (m, lbl) ->
                val sec = m * 60
                val active = presetSec == sec
                Box(
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(if (active) HefimerColors.White else HefimerColors.White06)
                        .applePressable(pressedScale = 0.94f) {
                            running = false
                            presetSec = sec
                            remainingSec = sec
                        }
                        .padding(horizontal = 14.dp, vertical = 7.dp),
                ) {
                    Text(lbl, color = if (active) HefimerColors.Black else HefimerColors.White70, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
            HefimerSecondaryButton(
                text = tr("Reset", "Đặt lại"),
                icon = PhosphorIcons.Regular.ArrowCounterClockwise,
                modifier = Modifier.weight(1f),
                onClick = {
                    running = false
                    remainingSec = presetSec
                },
            )
            HefimerPrimaryButton(
                text = if (running) tr("Pause", "Tạm dừng") else tr("Start", "Bắt đầu"),
                icon = if (running) PhosphorIcons.Regular.Pause else PhosphorIcons.Regular.Play,
                height = 48.dp,
                modifier = Modifier.weight(1f),
                onClick = {
                    if (remainingSec == 0) remainingSec = presetSec
                    running = !running
                },
            )
        }
    }
}

@Composable
private fun RandomPickerSection(vm: HefimerViewModel) {
    val context = LocalContext.current
    var rawEntries by remember { mutableStateOf("") }
    var pickedWinner by remember { mutableStateOf<String?>(null) }
    var teams by remember { mutableStateOf<List<List<String>>>(emptyList()) }
    var teamCount by remember { mutableIntStateOf(2) }

    val items = rawEntries.lines().map { it.trim() }.filter { it.isNotEmpty() }

    Column(
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier
            .fillMaxWidth()
            .animateContentSize(AppMotion.layoutSpring())
            .appleGlassSurface(RoundedCornerShape(30.dp), AppleGlassVariant.Regular, elevation = 10.dp)
            .padding(20.dp),
    ) {
        Text(
            text = tr("RANDOM PICKER & TEAM SPLIT", "QUAY SỐ & CHIA NHÓM"),
            color = HefimerColors.White45,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 2.sp,
        )
        HefimerTextField(
            value = rawEntries,
            onValueChange = { rawEntries = it },
            placeholder = tr("Enter one name or number per line...", "Mỗi dòng 1 tên hoặc số..."),
            singleLine = false,
            modifier = Modifier.heightIn(min = 108.dp),
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(tr("Teams:", "Số nhóm:"), color = HefimerColors.White55, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            listOf(2, 3, 4).forEach { n ->
                val active = teamCount == n
                Box(
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(if (active) HefimerColors.White else HefimerColors.White06)
                        .applePressable(pressedScale = 0.94f) { teamCount = n }
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                ) {
                    Text("$n", color = if (active) HefimerColors.Black else HefimerColors.White70, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
            HefimerPrimaryButton(
                text = tr("Pick 1", "Chọn 1"),
                icon = PhosphorIcons.Regular.Shuffle,
                height = 46.dp,
                enabled = items.isNotEmpty(),
                modifier = Modifier.weight(1f),
                onClick = {
                    if (items.isNotEmpty()) {
                        HefimerHaptics.success(context)
                        teams = emptyList()
                        pickedWinner = items.random()
                    }
                },
            )
            HefimerSecondaryButton(
                text = tr("Split Teams", "Chia nhóm"),
                height = 46.dp,
                enabled = items.size >= 2,
                modifier = Modifier.weight(1f),
                onClick = {
                    if (items.size >= 2) {
                        HefimerHaptics.success(context)
                        pickedWinner = null
                        val shuffled = items.shuffled()
                        val buckets = List(teamCount) { mutableListOf<String>() }
                        shuffled.forEachIndexed { idx, s -> buckets[idx % teamCount].add(s) }
                        teams = buckets.filter { it.isNotEmpty() }
                    } else {
                        vm.showToast(tr("Enter at least 2 entries", "Nhập ít nhất 2 dòng"), ToastType.INFO)
                    }
                },
            )
        }
        if (pickedWinner != null) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .appleGlassSurface(RoundedCornerShape(20.dp), AppleGlassVariant.Clear, accentBorderTint = HefimerColors.White, elevation = 0.dp)
                    .padding(16.dp),
            ) {
                Text(
                    text = pickedWinner.orEmpty(),
                    color = HefimerColors.White,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                )
            }
        }
        if (teams.isNotEmpty()) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                teams.forEachIndexed { idx, group ->
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .appleGlassSurface(RoundedCornerShape(16.dp), AppleGlassVariant.Clear, elevation = 0.dp)
                            .padding(12.dp),
                    ) {
                        Text(tr("Team ${idx + 1}", "Nhóm ${idx + 1}"), color = HefimerColors.White50, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.height(3.dp))
                        Text(group.joinToString(" • "), color = HefimerColors.White, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                    }
                }
            }
        }
    }
}
