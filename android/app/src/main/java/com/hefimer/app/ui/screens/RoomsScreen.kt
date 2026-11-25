package com.hefimer.app.ui.screens

import android.graphics.Bitmap
import android.graphics.Canvas as AndroidCanvas
import android.graphics.Paint as AndroidPaint
import android.graphics.Path as AndroidPath
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.calculateCentroid
import androidx.compose.foundation.gestures.calculatePan
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.Send
import androidx.compose.material.icons.automirrored.rounded.Undo
import androidx.compose.material.icons.rounded.AutoFixHigh
import androidx.compose.material.icons.rounded.Brush
import androidx.compose.material.icons.rounded.Casino
import androidx.compose.material.icons.rounded.CenterFocusStrong
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.Grid4x4
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.PanTool
import androidx.compose.material.icons.rounded.People
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.QrCode2
import androidx.compose.material.icons.rounded.QrCodeScanner
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material3.Icon
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PointMode
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.hypot
import kotlin.math.min
import kotlin.math.roundToInt
import com.hefimer.app.data.model.BoardPoint
import com.hefimer.app.data.model.ChatMessage
import com.hefimer.app.data.model.ToastType
import com.hefimer.app.data.model.formatCountdown
import com.hefimer.app.data.network.DownloadHelper
import com.hefimer.app.data.network.HefimerApiConfig
import com.hefimer.app.ui.AppOverlay
import com.hefimer.app.ui.HefimerViewModel
import com.hefimer.app.ui.RoomsUiState
import com.hefimer.app.ui.ScanTarget
import com.hefimer.app.ui.components.DropResultCard
import com.hefimer.app.ui.components.HefimerCollapsibleScaffold
import com.hefimer.app.ui.components.HefimerHeaderIconButton
import com.hefimer.app.ui.components.HefimerPrimaryButton
import com.hefimer.app.ui.components.QrShareModal
import com.hefimer.app.ui.theme.AppMotion
import com.hefimer.app.ui.theme.AppleGlassVariant
import com.hefimer.app.ui.theme.AppleMonochromeBackdrop
import com.hefimer.app.ui.theme.AppleOdometerNumber
import com.hefimer.app.ui.theme.AppleSegmentedControl
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
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val CHAT_REACTIONS = listOf(
    "heart" to "Love",
    "thumb" to "Like",
    "smile" to "Joy",
    "frown" to "Sad",
    "fire" to "Fire",
    "party" to "Boost",
    "ghost" to "Boo",
    "rocket" to "Launch",
    "star" to "Spark",
    "zap" to "Zap",
)

private val BOARD_COLORS = listOf(
    "#ffffff", "#ff4444", "#ff8800", "#ffff44", "#44ff44", "#00ffff",
    "#4444ff", "#8844ff", "#ff44ff", "#ff8888", "#88ff88", "#8888ff",
    "#c0c0c0", "#800000", "#808000", "#008000", "#008080", "#000080",
    "#800080", "#ffd700", "#adff2f", "#7fffd4", "#da70d6", "#f0e68c",
)

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun RoomsScreen(vm: HefimerViewModel) {
    val state by vm.roomsState.collectAsState()
    val heroBounds = LocalHeroBounds.current
    val reduceMotion = rememberReduceMotion()

    HefimerCollapsibleScaffold(
        kicker = tr("Live expiring sessions", "Phiên trực tiếp tạm thời"),
        title = tr("Rooms", "Phòng"),
        subtitle = tr(
            "Start a disposable live chat room or a real-time collaborative whiteboard.",
            "Tạo phòng trò chuyện tạm thời hoặc bảng vẽ cộng tác theo thời gian thực.",
        ),
        showOrbitMark = true,
        headerActions = {
            HefimerHeaderIconButton(
                icon = Icons.Rounded.QrCodeScanner,
                contentDescription = tr("Scan Room QR", "Quét QR phòng"),
                onClick = { vm.openOverlay(AppOverlay.QrScanner(ScanTarget.AUTO)) },
            )
            HefimerHeaderIconButton(
                icon = Icons.Rounded.History,
                contentDescription = tr("History", "Lịch sử"),
                modifier = Modifier.heroSource("btn_history_rooms", heroBounds),
                onClick = { vm.openOverlay(AppOverlay.History, "btn_history_rooms") },
            )
        },
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            AppleSegmentedControl(
                options = listOf("chat", "board"),
                selected = state.section,
                label = { if (it == "chat") tr("Chat Room", "Phòng chat") else tr("Whiteboard", "Bảng vẽ") },
                onSelect = { vm.setRoomsSection(it) },
                modifier = Modifier.appleEntrance(0),
            )

            // Shared Display Name input
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .appleEntrance(1)
                    .appleGlassSurface(
                        shape = RoundedCornerShape(30.dp),
                        variant = AppleGlassVariant.Regular,
                        elevation = 10.dp,
                    )
                    .padding(16.dp),
            ) {
                Text(tr("YOUR NAME", "TÊN CỦA BẠN"), color = HefimerColors.White30, fontSize = 9.sp, fontWeight = FontWeight.Bold, letterSpacing = 2.sp)
                HefimerTextField(
                    value = state.userName,
                    onValueChange = { vm.setSpaceUserName(it) },
                    placeholder = tr("Enter display name...", "Nhập tên hiển thị..."),
                    leadingIcon = Icons.Rounded.Person,
                )
            }

            AnimatedContent(
                targetState = state.section,
                transitionSpec = { AppMotion.sectionTransform(reduceMotion) },
                label = "rooms_section",
            ) { sec ->
                if (sec == "chat") {
                    ChatLobbySection(vm = vm, state = state)
                } else {
                    BoardLobbySection(vm = vm, state = state)
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ChatLobbySection(vm: HefimerViewModel, state: RoomsUiState) {
    if (state.chatCreatedCode.isNotBlank()) {
        DropResultCard(
            code = state.chatCreatedCode,
            heading = tr("Your Room Code", "Mã phòng của bạn"),
            subtitle = state.chatCreatedName,
            expiresAt = state.chatCreatedExpiresAt,
            shareUrl = "${HefimerApiConfig.SITE_ORIGIN}/chat-room?code=${state.chatCreatedCode}",
            resetLabel = tr("New Room", "Phòng mới"),
            primaryCtaLabel = tr("Enter Room", "Vào phòng"),
            onPrimaryCta = { vm.enterCreatedChatRoom() },
            onBack = { vm.clearChatCreatedCard() },
            onReset = { vm.clearChatCreatedCard() },
            onToast = { m, e -> vm.showToast(m, if (e) ToastType.ERROR else ToastType.SUCCESS) },
        )
        return
    }

    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        AppleSegmentedControl(
            options = listOf("join", "create"),
            selected = state.chatLobbyMode,
            label = { if (it == "join") tr("Join Room", "Vào phòng") else tr("Create Room", "Tạo phòng") },
            height = 40.dp,
            outerRadius = 20.dp,
            onSelect = { vm.setChatLobbyMode(it) },
        )

        Column(
            verticalArrangement = Arrangement.spacedBy(14.dp),
            modifier = Modifier
                .fillMaxWidth()
                .appleGlassSurface(
                    shape = RoundedCornerShape(32.dp),
                    variant = AppleGlassVariant.Regular,
                    elevation = 12.dp,
                )
                .padding(20.dp),
        ) {
            if (state.chatLobbyMode == "join") {
                Text(tr("5-DIGIT ROOM CODE", "MÃ PHÒNG 5 CHỮ SỐ"), color = HefimerColors.White30, fontSize = 9.sp, fontWeight = FontWeight.Bold, letterSpacing = 2.sp)
                FiveDigitPinSlots(
                    code = state.chatJoinCode,
                    onCodeChange = { vm.setChatJoinCode(it) },
                    onDone = { vm.joinChatRoom() },
                )
                HefimerPrimaryButton(
                    text = if (state.chatLoading) tr("Joining...", "Đang tham gia...") else tr("Join Chat Room", "Tham gia phòng chat"),
                    loading = state.chatLoading,
                    enabled = state.chatJoinCode.length == 5 && !state.chatLoading,
                    onClick = { vm.joinChatRoom() },
                )
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(tr("ROOM NAME", "TÊN PHÒNG"), color = HefimerColors.White30, fontSize = 9.sp, fontWeight = FontWeight.Bold, letterSpacing = 2.sp)
                    HefimerTextField(
                        value = state.chatCreateName,
                        onValueChange = { vm.setChatCreateName(it) },
                        placeholder = tr("Quick sync, study group...", "Trao đổi nhanh, nhóm học..."),
                    )
                }

                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(tr("ROOM CODE", "MÃ PHÒNG"), color = HefimerColors.White30, fontSize = 9.sp, fontWeight = FontWeight.Bold, letterSpacing = 2.sp)
                        val (stTxt, stCol) = when (state.chatCreateCodeStatus) {
                            "checking" -> tr("Checking...", "Đang kiểm tra...") to HefimerColors.White40
                            "available" -> tr("Available", "Khả dụng") to HefimerColors.White80
                            "taken" -> tr("Taken", "Đã được dùng") to HefimerColors.Red400
                            else -> "" to HefimerColors.White30
                        }
                        if (stTxt.isNotEmpty()) Text(stTxt, color = stCol, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                        HefimerTextField(
                            value = state.chatCreateCode,
                            onValueChange = { vm.setChatCreateCode(it) },
                            placeholder = "12345",
                            mono = true,
                            modifier = Modifier.weight(1f),
                        )
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .size(48.dp)
                                .appleGlassSurface(
                                    shape = RoundedCornerShape(18.dp),
                                    variant = AppleGlassVariant.Clear,
                                    elevation = 0.dp,
                                )
                                .applePressable(pressedScale = 0.89f) { vm.randomizeChatCode() },
                        ) {
                            Icon(Icons.Rounded.Casino, tr("Random", "Ngẫu nhiên"), tint = HefimerColors.White, modifier = Modifier.size(20.dp))
                        }
                    }
                }

                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(tr("ROOM LIFETIME", "THỜI HẠN PHÒNG"), color = HefimerColors.White30, fontSize = 9.sp, fontWeight = FontWeight.Bold, letterSpacing = 2.sp)
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf(
                            "15m" to tr("15 min", "15 phút"),
                            "30m" to tr("30 min", "30 phút"),
                            "1h" to tr("1 hour", "1 giờ"),
                            "6h" to tr("6 hours", "6 giờ"),
                            "12h" to tr("12 hours", "12 giờ"),
                            "24h" to tr("24 hours", "24 giờ"),
                        ).forEach { (v, lbl) ->
                            val sel = state.chatCreateExpire == v
                            val chipFill by animateColorAsState(
                                targetValue = if (sel) HefimerColors.White else Color.Transparent,
                                animationSpec = tween(AppMotion.Fast, easing = AppMotion.AppleSilk),
                                label = "chat_expire_fill_$v",
                            )
                            val chipText by animateColorAsState(
                                targetValue = if (sel) HefimerColors.Black else HefimerColors.White70,
                                animationSpec = tween(AppMotion.Fast, easing = AppMotion.AppleSilk),
                                label = "chat_expire_text_$v",
                            )
                            Text(
                                text = lbl,
                                color = chipText,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier
                                    .appleGlassSurface(
                                        shape = CircleShape,
                                        variant = AppleGlassVariant.Clear,
                                        accentBorderTint = if (sel) HefimerColors.White else null,
                                        accentFillTint = chipFill,
                                        elevation = 0.dp,
                                    )
                                    .applePressable(pressedScale = 0.93f) { vm.setChatCreateExpire(v) }
                                    .padding(horizontal = 13.dp, vertical = 8.dp),
                            )
                        }
                    }
                }

                HefimerPrimaryButton(
                    text = if (state.chatLoading) tr("Creating...", "Đang tạo...") else tr("Create Chat Room", "Tạo phòng chat"),
                    loading = state.chatLoading,
                    enabled = !state.chatLoading && state.chatCreateCodeStatus != "taken",
                    onClick = { vm.createChatRoom() },
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun BoardLobbySection(vm: HefimerViewModel, state: RoomsUiState) {
    if (state.boardCreatedCode.isNotBlank()) {
        DropResultCard(
            code = state.boardCreatedCode,
            heading = tr("Your Board Code", "Mã bảng trắng của bạn"),
            subtitle = state.boardCreatedName,
            expiresAt = state.boardCreatedExpiresAt,
            shareUrl = "${HefimerApiConfig.SITE_ORIGIN}/board?code=${state.boardCreatedCode}",
            resetLabel = tr("New Board", "Bảng mới"),
            primaryCtaLabel = tr("Enter Board", "Mở bảng"),
            onPrimaryCta = { vm.enterCreatedBoard() },
            onBack = { vm.clearBoardCreatedCard() },
            onReset = { vm.clearBoardCreatedCard() },
            onToast = { m, e -> vm.showToast(m, if (e) ToastType.ERROR else ToastType.SUCCESS) },
        )
        return
    }

    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        AppleSegmentedControl(
            options = listOf("create", "join"),
            selected = state.boardLobbyMode,
            label = { if (it == "create") tr("Create Board", "Tạo bảng") else tr("Join Board", "Vào bảng") },
            height = 40.dp,
            outerRadius = 20.dp,
            onSelect = { vm.setBoardLobbyMode(it) },
        )

        Column(
            verticalArrangement = Arrangement.spacedBy(14.dp),
            modifier = Modifier
                .fillMaxWidth()
                .appleGlassSurface(
                    shape = RoundedCornerShape(32.dp),
                    variant = AppleGlassVariant.Regular,
                    elevation = 12.dp,
                )
                .padding(20.dp),
        ) {
            if (state.boardLobbyMode == "join") {
                Text(tr("5-DIGIT BOARD CODE", "MÃ BẢNG 5 CHỮ SỐ"), color = HefimerColors.White30, fontSize = 9.sp, fontWeight = FontWeight.Bold, letterSpacing = 2.sp)
                FiveDigitPinSlots(
                    code = state.boardJoinCode,
                    onCodeChange = { vm.setBoardJoinCode(it) },
                    onDone = { vm.joinBoard() },
                )
                HefimerPrimaryButton(
                    text = if (state.boardLoading) tr("Joining...", "Đang tham gia...") else tr("Join Whiteboard", "Tham gia bảng trắng"),
                    loading = state.boardLoading,
                    enabled = state.boardJoinCode.length == 5 && !state.boardLoading,
                    onClick = { vm.joinBoard() },
                )
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(tr("BOARD NAME", "TÊN BẢNG"), color = HefimerColors.White30, fontSize = 9.sp, fontWeight = FontWeight.Bold, letterSpacing = 2.sp)
                    HefimerTextField(
                        value = state.boardCreateName,
                        onValueChange = { vm.setBoardCreateName(it) },
                        placeholder = tr("Architecture sketch, brainstorm...", "Phác thảo kiến trúc, lên ý tưởng..."),
                    )
                }

                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(tr("DURATION", "THỜI HẠN"), color = HefimerColors.White30, fontSize = 9.sp, fontWeight = FontWeight.Bold, letterSpacing = 2.sp)
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf(
                            "1h" to tr("1 hour", "1 giờ"),
                            "6h" to tr("6 hours", "6 giờ"),
                            "12h" to tr("12 hours", "12 giờ"),
                            "24h" to tr("24 hours", "24 giờ"),
                        ).forEach { (v, lbl) ->
                            val sel = state.boardCreateExpire == v
                            val chipFill by animateColorAsState(
                                targetValue = if (sel) HefimerColors.White else Color.Transparent,
                                animationSpec = tween(AppMotion.Fast, easing = AppMotion.AppleSilk),
                                label = "board_expire_fill_$v",
                            )
                            val chipText by animateColorAsState(
                                targetValue = if (sel) HefimerColors.Black else HefimerColors.White70,
                                animationSpec = tween(AppMotion.Fast, easing = AppMotion.AppleSilk),
                                label = "board_expire_text_$v",
                            )
                            Text(
                                text = lbl,
                                color = chipText,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier
                                    .appleGlassSurface(
                                        shape = CircleShape,
                                        variant = AppleGlassVariant.Clear,
                                        accentBorderTint = if (sel) HefimerColors.White else null,
                                        accentFillTint = chipFill,
                                        elevation = 0.dp,
                                    )
                                    .applePressable(pressedScale = 0.93f) { vm.setBoardCreateExpire(v) }
                                    .padding(horizontal = 14.dp, vertical = 8.dp),
                            )
                        }
                    }
                }

                HefimerPrimaryButton(
                    text = if (state.boardLoading) tr("Creating...", "Đang tạo...") else tr("Create Whiteboard", "Tạo bảng trắng"),
                    loading = state.boardLoading,
                    enabled = !state.boardLoading,
                    onClick = { vm.createBoard() },
                )
            }
        }
    }
}

/**
 * Full-screen real-time expiring Chat Room overlay with 10-emoji reactions and live countdown.
 */
@Composable
fun ChatRoomOverlay(
    vm: HefimerViewModel,
    onClose: () -> Unit,
) {
    val state by vm.roomsState.collectAsState()
    val statusTop = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    val navBottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
    val listState = rememberLazyListState()
    var input by remember { mutableStateOf("") }
    var reactionTargetId by remember { mutableStateOf<String?>(null) }
    var showQr by remember { mutableStateOf(false) }
    var countdown by remember(state.activeChatExpiresAt) { mutableStateOf(formatCountdown(state.activeChatExpiresAt)) }
    var isCritical by remember { mutableStateOf(false) }
    val timeFmt = remember { SimpleDateFormat("HH:mm", Locale.getDefault()) }

    LaunchedEffect(state.activeChatExpiresAt) {
        if (state.activeChatExpiresAt <= 0L) return@LaunchedEffect
        while (true) {
            val diff = state.activeChatExpiresAt - System.currentTimeMillis()
            if (diff <= 0L) {
                vm.showToast(tr("Room has expired", "Phòng đã hết hạn"), ToastType.ERROR)
                onClose()
                break
            }
            countdown = formatCountdown(state.activeChatExpiresAt)
            isCritical = diff < 60_000L
            delay(1000L)
        }
    }

    LaunchedEffect(state.chatMessages.size) {
        if (state.chatMessages.isNotEmpty()) {
            listState.animateScrollToItem(state.chatMessages.lastIndex)
        }
    }

    var composerFocused by remember { mutableStateOf(false) }
    Box(modifier = Modifier.fillMaxSize()) {
        AppleMonochromeBackdrop()
        Column(
            modifier = Modifier
                .fillMaxSize()
                .imePadding(),
        ) {
            // Translucent Apple Glass pinned chat header behind transparent status bar
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .appleGlassSurface(
                        shape = RoundedCornerShape(bottomStart = 28.dp, bottomEnd = 28.dp),
                        variant = AppleGlassVariant.Elevated,
                        elevation = 14.dp,
                    ),
            ) {
                Spacer(Modifier.height(statusTop))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.weight(1f),
                    ) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .size(38.dp)
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
                                text = state.activeChatName.ifBlank { "${tr("Room", "Phòng")} #${state.activeChatId}" },
                                color = HefimerColors.White,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Text("#${state.activeChatId}", color = HefimerColors.White50, fontFamily = JetBrainsMonoFamily, fontSize = 11.sp)
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                                    Icon(Icons.Rounded.People, null, tint = HefimerColors.White75, modifier = Modifier.size(12.dp))
                                    AppleOdometerNumber(
                                        value = state.chatMemberCount,
                                        color = HefimerColors.White75,
                                        style = TextStyle(fontFamily = UbuntuFontFamily, fontSize = 11.sp, fontWeight = FontWeight.Bold),
                                    )
                                }
                                if (countdown.isNotBlank()) {
                                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                                        Icon(Icons.Rounded.Schedule, null, tint = if (isCritical) HefimerColors.Red400 else HefimerColors.White40, modifier = Modifier.size(11.dp))
                                        Text(
                                            text = countdown,
                                            color = if (isCritical) HefimerColors.Red400 else HefimerColors.White40,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                        )
                                    }
                                }
                            }
                        }
                    }

                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(38.dp)
                            .appleGlassSurface(
                                shape = CircleShape,
                                variant = AppleGlassVariant.Clear,
                                elevation = 0.dp,
                            )
                            .applePressable(pressedScale = 0.89f) { showQr = true },
                    ) {
                        Icon(Icons.Rounded.QrCode2, tr("Share QR", "Chia sẻ QR"), tint = HefimerColors.White, modifier = Modifier.size(18.dp))
                    }
                }
            }

            // Message stream
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                if (state.chatMessages.isEmpty()) {
                    item {
                        Box(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 80.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(tr("Messages vanish when this room expires.", "Tin nhắn sẽ tự xóa khi phòng hết hạn."), color = HefimerColors.White30, fontSize = 13.sp)
                        }
                    }
                }

                items(state.chatMessages, key = { it.id }) { msg ->
                    val isMine = msg.sender.equals(state.userName.trim(), ignoreCase = true)
                    ChatBubbleItem(
                        msg = msg,
                        isMine = isMine,
                        timeText = if (msg.timestamp > 0L) timeFmt.format(Date(msg.timestamp)) else "",
                        showReactionBar = reactionTargetId == msg.id,
                        onToggleReactionBar = { reactionTargetId = if (reactionTargetId == msg.id) null else msg.id },
                        onReact = { rId ->
                            vm.addChatReaction(msg.id, rId)
                            reactionTargetId = null
                        },
                    )
                }
            }

            // Frosted Apple Glass composer dock
            val canSend = input.isNotBlank()
            val sendBg by animateColorAsState(
                targetValue = if (canSend) HefimerColors.White else HefimerColors.White10,
                animationSpec = tween(AppMotion.Fast, easing = AppMotion.AppleSilk),
                label = "chat_send_bg",
            )
            val sendTint by animateColorAsState(
                targetValue = if (canSend) HefimerColors.Black else HefimerColors.White30,
                animationSpec = tween(AppMotion.Fast, easing = AppMotion.AppleSilk),
                label = "chat_send_tint",
            )
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .appleGlassSurface(
                        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
                        variant = AppleGlassVariant.Elevated,
                        elevation = 16.dp,
                    )
                    .padding(start = 16.dp, end = 16.dp, top = 12.dp, bottom = navBottom + 12.dp),
            ) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .appleGlassSurface(
                            shape = CircleShape,
                            variant = AppleGlassVariant.Clear,
                            accentBorderTint = if (composerFocused) HefimerColors.White else null,
                            accentFillTint = if (composerFocused) HefimerColors.White.copy(alpha = 0.06f) else null,
                            elevation = 0.dp,
                        )
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                ) {
                    BasicTextField(
                        value = input,
                        onValueChange = { input = it },
                        textStyle = TextStyle(fontFamily = UbuntuFontFamily, fontSize = 14.sp, color = HefimerColors.White),
                        cursorBrush = SolidColor(HefimerColors.White),
                        modifier = Modifier
                            .fillMaxWidth()
                            .onFocusChanged { composerFocused = it.isFocused },
                        decorationBox = { inner ->
                            if (input.isEmpty()) Text(tr("Message room...", "Nhắn tin vào phòng..."), color = HefimerColors.White30, fontSize = 14.sp)
                            inner()
                        },
                    )
                }
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(sendBg)
                        .applePressable(enabled = canSend, pressedScale = 0.88f) {
                            vm.sendChatMessage(input)
                            input = ""
                        },
                ) {
                    Icon(
                        Icons.AutoMirrored.Rounded.Send,
                        tr("Send", "Gửi"),
                        tint = sendTint,
                        modifier = Modifier.size(18.dp),
                    )
                }
            }
        }
    }

    if (showQr) {
        QrShareModal(
            title = tr("Share Room Link", "Chia sẻ liên kết phòng"),
            url = "${HefimerApiConfig.SITE_ORIGIN}/chat-room?code=${state.activeChatId}",
            code = state.activeChatId,
            onDismiss = { showQr = false },
            onToast = { m, e -> vm.showToast(m, if (e) ToastType.ERROR else ToastType.SUCCESS) },
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ChatBubbleItem(
    msg: ChatMessage,
    isMine: Boolean,
    timeText: String,
    showReactionBar: Boolean,
    onToggleReactionBar: () -> Unit,
    onReact: (String) -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = if (isMine) Alignment.End else Alignment.Start,
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
        ) {
            Text(
                text = if (msg.isAdmin) "${msg.sender} • ${tr("ADMIN", "QUẢN TRỊ")}" else msg.sender,
                color = if (msg.isAdmin) HefimerColors.White else HefimerColors.White40,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
            )
            if (timeText.isNotEmpty()) {
                Text(timeText, color = HefimerColors.White20, fontSize = 9.sp)
            }
        }

        val bubbleShape = RoundedCornerShape(
            topStart = 22.dp,
            topEnd = 22.dp,
            bottomStart = if (isMine) 22.dp else 6.dp,
            bottomEnd = if (isMine) 6.dp else 22.dp,
        )
        Box(
            modifier = Modifier
                .widthIn(max = 290.dp)
                .then(
                    if (isMine) {
                        Modifier
                            .clip(bubbleShape)
                            .background(HefimerColors.White)
                            .border(1.dp, HefimerColors.White, bubbleShape)
                    } else {
                        Modifier.appleGlassSurface(
                            shape = bubbleShape,
                            variant = AppleGlassVariant.Clear,
                            elevation = 2.dp,
                        )
                    },
                )
                .applePressable(pressedScale = 0.98f, onClick = onToggleReactionBar)
                .padding(horizontal = 15.dp, vertical = 11.dp),
        ) {
            Text(
                text = msg.text,
                color = if (isMine) HefimerColors.Black else HefimerColors.White,
                fontSize = 14.sp,
                lineHeight = 20.sp,
            )
        }

        // Reaction counts
        if (msg.reactions.isNotEmpty()) {
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.padding(top = 5.dp),
            ) {
                msg.reactions.entries.filter { it.value > 0 }.forEach { (rId, count) ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(5.dp),
                        modifier = Modifier
                            .appleGlassSurface(
                                shape = CircleShape,
                                variant = AppleGlassVariant.Clear,
                                elevation = 0.dp,
                            )
                            .applePressable(pressedScale = 0.90f) { onReact(rId) }
                            .padding(horizontal = 9.dp, vertical = 4.dp),
                    ) {
                        com.hefimer.app.ui.components.HefimerReactionGlyph(
                            reactionId = rId,
                            tint = HefimerColors.White,
                            size = 13.dp,
                        )
                        Text(count.toString(), color = HefimerColors.White80, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // Expandable custom monochrome vector reaction picker (zero emojis)
        AnimatedVisibility(visible = showReactionBar, enter = AppMotion.softExpandEnter, exit = AppMotion.softExpandExit) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .padding(top = 6.dp)
                    .horizontalScroll(rememberScrollState())
                    .appleGlassSurface(
                        shape = CircleShape,
                        variant = AppleGlassVariant.Elevated,
                        elevation = 14.dp,
                    )
                    .padding(horizontal = 8.dp, vertical = 5.dp),
            ) {
                CHAT_REACTIONS.forEach { (id, _) ->
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(HefimerColors.White06)
                            .applePressable(pressedScale = 0.84f) { onReact(id) },
                    ) {
                        com.hefimer.app.ui.components.HefimerReactionGlyph(
                            reactionId = id,
                            tint = HefimerColors.White,
                            size = 18.dp,
                        )
                    }
                }
            }
        }
    }
}

/**
 * Full-screen live collaborative Whiteboard overlay synced 1:1 with PC board_paths/{boardId}
 * using a 1280x720 virtual 16:9 canvas with 1-finger draw/pan, 2-finger pinch-zoom, Undo, and 2x HD export.
 */
@Composable
fun WhiteboardOverlay(
    vm: HefimerViewModel,
    onClose: () -> Unit,
) {
    val state by vm.roomsState.collectAsState()
    val context = LocalContext.current
    val density = LocalDensity.current
    val scope = rememberCoroutineScope()
    val statusTop = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    val navBottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()

    var currentColorHex by remember { mutableStateOf("#ffffff") }
    var strokeSize by remember { mutableFloatStateOf(4f) }
    var isEraser by remember { mutableStateOf(false) }
    var isPanTool by remember { mutableStateOf(false) }
    var showGrid by remember { mutableStateOf(true) }
    var showPalette by remember { mutableStateOf(false) }
    var showQr by remember { mutableStateOf(false) }

    val boardW = state.boardWidth.coerceAtLeast(320f)
    val boardH = state.boardHeight.coerceAtLeast(240f)
    var viewportSize by remember { mutableStateOf(IntSize.Zero) }
    var zoom by remember { mutableFloatStateOf(1f) }
    var pan by remember { mutableStateOf(Offset.Zero) }
    var hasInitializedFit by remember { mutableStateOf(false) }
    var nowMs by remember { mutableLongStateOf(System.currentTimeMillis()) }
    val activePoints = remember { mutableStateListOf<BoardPoint>() }

    LaunchedEffect(state.activeBoardId, state.activeBoardExpiresAt) {
        while (state.activeBoardExpiresAt > 0L) {
            nowMs = System.currentTimeMillis()
            delay(1000L)
        }
    }

    val marginH = with(density) { 24.dp.toPx() }
    val topReserve = with(density) { statusTop.toPx() + 92.dp.toPx() }
    val bottomReserve = with(density) { navBottom.toPx() + 108.dp.toPx() }
    val fitZoom = remember(viewportSize, boardW, boardH, marginH, topReserve, bottomReserve) {
        if (viewportSize.width <= 0 || viewportSize.height <= 0) {
            0.5f
        } else {
            val availW = (viewportSize.width - marginH).coerceAtLeast(160f)
            val availH = (viewportSize.height - topReserve - bottomReserve).coerceAtLeast(160f)
            min(availW / boardW, availH / boardH).coerceIn(0.16f, 3f)
        }
    }

    fun clampBoardPan(rawPan: Offset, targetZoom: Float): Offset {
        if (viewportSize.width <= 0 || viewportSize.height <= 0) return rawPan
        val visibleMargin = 80f
        val minX = visibleMargin - boardW * targetZoom
        val maxX = (viewportSize.width - visibleMargin).coerceAtLeast(minX)
        val minY = visibleMargin - boardH * targetZoom
        val maxY = (viewportSize.height - visibleMargin).coerceAtLeast(minY)
        return Offset(
            x = rawPan.x.coerceIn(minX, maxX),
            y = rawPan.y.coerceIn(minY, maxY),
        )
    }

    fun resetFitView() {
        if (viewportSize.width <= 0 || viewportSize.height <= 0) return
        zoom = fitZoom
        val centerOffsetY = (topReserve - bottomReserve) * 0.5f
        pan = Offset(
            x = (viewportSize.width - boardW * fitZoom) / 2f,
            y = (viewportSize.height - boardH * fitZoom) / 2f + centerOffsetY,
        )
    }

    LaunchedEffect(viewportSize, boardW, boardH) {
        if (!hasInitializedFit && viewportSize.width > 0 && viewportSize.height > 0) {
            resetFitView()
            hasInitializedFit = true
        }
    }

    val gridPoints = remember(boardW, boardH) {
        val list = ArrayList<Offset>()
        var gx = 30f
        while (gx < boardW) {
            var gy = 30f
            while (gy < boardH) {
                list.add(Offset(gx, gy))
                gy += 30f
            }
            gx += 30f
        }
        list
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF060608))
            .clipToBounds()
            .onSizeChanged { viewportSize = it },
    ) {
        // Real-time collaborative 1280x720 PC-synced drawing Canvas with 2-finger pinch/pan
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(boardW, boardH) {
                    awaitEachGesture {
                        val firstEvent = awaitPointerEvent()
                        val down = firstEvent.changes.firstOrNull { it.pressed } ?: return@awaitEachGesture
                        var isDrawingStroke = false
                        var hasMultitouched = firstEvent.changes.count { it.pressed } > 1

                        if (!hasMultitouched && !isPanTool) {
                            val bx = (down.position.x - pan.x) / zoom
                            val by = (down.position.y - pan.y) / zoom
                            if (bx in -36f..(boardW + 36f) && by in -36f..(boardH + 36f)) {
                                isDrawingStroke = true
                                activePoints.clear()
                                activePoints.add(BoardPoint(bx.coerceIn(0f, boardW), by.coerceIn(0f, boardH)))
                                down.consume()
                            }
                        }

                        while (true) {
                            val event = awaitPointerEvent()
                            val pressed = event.changes.filter { it.pressed }
                            if (pressed.isEmpty()) break

                            if (pressed.size >= 2) {
                                if (isDrawingStroke) {
                                    isDrawingStroke = false
                                    activePoints.clear()
                                }
                                hasMultitouched = true
                                val zoomChange = event.calculateZoom()
                                val panChange = event.calculatePan()
                                val centroid = event.calculateCentroid(useCurrent = false)
                                val oldZoom = zoom
                                val minZoom = (fitZoom * 0.65f).coerceAtLeast(0.14f)
                                val maxZoom = (fitZoom * 6f).coerceAtLeast(4f)
                                val newZoom = (oldZoom * zoomChange).coerceIn(minZoom, maxZoom)
                                val scaleFactor = if (oldZoom > 0f) newZoom / oldZoom else 1f
                                val rawPan = centroid - (centroid - pan) * scaleFactor + panChange
                                zoom = newZoom
                                pan = clampBoardPan(rawPan, newZoom)
                                event.changes.forEach { it.consume() }
                            } else if (isDrawingStroke && !hasMultitouched) {
                                val change = pressed.first()
                                val bx = ((change.position.x - pan.x) / zoom).coerceIn(0f, boardW)
                                val by = ((change.position.y - pan.y) / zoom).coerceIn(0f, boardH)
                                val prev = activePoints.lastOrNull()
                                if (prev == null || hypot(bx - prev.x, by - prev.y) >= 1.2f) {
                                    activePoints.add(BoardPoint(bx, by))
                                }
                                change.consume()
                            } else if (isPanTool && !hasMultitouched) {
                                val panChange = event.calculatePan()
                                if (panChange != Offset.Zero) {
                                    pan = clampBoardPan(pan + panChange, zoom)
                                    event.changes.forEach { it.consume() }
                                }
                            }
                        }

                        if (isDrawingStroke && !hasMultitouched && activePoints.isNotEmpty()) {
                            val color = if (isEraser) "#000000" else currentColorHex
                            val w = if (isEraser) strokeSize * 2.5f else strokeSize
                            vm.pushBoardStroke(color, w, activePoints.toList())
                        }
                        activePoints.clear()
                    }
                },
        ) {
            withTransform({
                translate(left = pan.x, top = pan.y)
                scale(scaleX = zoom, scaleY = zoom, pivot = Offset.Zero)
            }) {
                // Virtual PC-matched 1280x720 board surface
                drawRect(
                    color = HefimerColors.Black,
                    topLeft = Offset.Zero,
                    size = Size(boardW, boardH),
                )
                drawRect(
                    color = HefimerColors.White20,
                    topLeft = Offset.Zero,
                    size = Size(boardW, boardH),
                    style = Stroke(width = (1.5f / zoom).coerceAtLeast(0.8f)),
                )

                clipRect(0f, 0f, boardW, boardH) {
                    if (showGrid && gridPoints.isNotEmpty()) {
                        drawPoints(
                            points = gridPoints,
                            pointMode = PointMode.Points,
                            color = Color.White.copy(alpha = 0.18f),
                            strokeWidth = (2.2f).coerceAtLeast(1.4f / zoom),
                            cap = StrokeCap.Round,
                        )
                    }

                    fun drawStrokePath(hex: String, width: Float, pts: List<BoardPoint>) {
                        if (pts.isEmpty()) return
                        val col = runCatching { Color(android.graphics.Color.parseColor(hex)) }
                            .getOrDefault(HefimerColors.White)
                        if (pts.size == 1) {
                            drawCircle(
                                color = col,
                                radius = (width / 2f).coerceAtLeast(0.8f),
                                center = Offset(pts[0].x, pts[0].y),
                            )
                        } else {
                            val path = Path().apply {
                                moveTo(pts.first().x, pts.first().y)
                                for (i in 1 until pts.size) {
                                    lineTo(pts[i].x, pts[i].y)
                                }
                            }
                            drawPath(
                                path = path,
                                color = col,
                                style = Stroke(width = width, cap = StrokeCap.Round, join = StrokeJoin.Round),
                            )
                        }
                    }

                    state.boardStrokes.forEach { st ->
                        drawStrokePath(st.color, st.size, st.points)
                    }

                    if (activePoints.isNotEmpty()) {
                        val activeCol = if (isEraser) "#000000" else currentColorHex
                        val activeW = if (isEraser) strokeSize * 2.5f else strokeSize
                        drawStrokePath(activeCol, activeW, activePoints)
                    }
                }
            }
        }

        // Floating Apple Glass top HUD capsule
        val zoomPercent = if (fitZoom > 0f) ((zoom / fitZoom) * 100f).roundToInt() else 100
        val countdownSuffix = if (state.activeBoardExpiresAt > 0L) " • ${formatCountdown(state.activeBoardExpiresAt, nowMs)}" else ""
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
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
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(9.dp),
                modifier = Modifier.weight(1f),
            ) {
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
                        text = state.activeBoardName.ifBlank { "${tr("Board", "Bảng")} #${state.activeBoardId}" },
                        color = HefimerColors.White,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        text = "#${state.activeBoardId} • ${boardW.toInt()}×${boardH.toInt()} • ${state.boardStrokes.size}$countdownSuffix",
                        color = HefimerColors.White45,
                        fontFamily = JetBrainsMonoFamily,
                        fontSize = 10.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(5.dp),
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier
                        .height(36.dp)
                        .appleGlassSurface(
                            shape = CircleShape,
                            variant = AppleGlassVariant.Clear,
                            elevation = 0.dp,
                        )
                        .applePressable(pressedScale = 0.90f) { resetFitView() }
                        .padding(horizontal = 9.dp),
                ) {
                    Icon(Icons.Rounded.CenterFocusStrong, tr("Fit Board", "Thu vừa màn hình"), tint = HefimerColors.White80, modifier = Modifier.size(15.dp))
                    Text(
                        text = "$zoomPercent%",
                        color = HefimerColors.White80,
                        fontFamily = JetBrainsMonoFamily,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.SemiBold,
                    )
                }

                // 2x Retina HD Export (2560x1440) synced with PC export
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(36.dp)
                        .appleGlassSurface(
                            shape = CircleShape,
                            variant = AppleGlassVariant.Clear,
                            elevation = 0.dp,
                        )
                        .applePressable(pressedScale = 0.89f) {
                            scope.launch {
                                runCatching {
                                    val exportScale = 2f
                                    val outW = (boardW * exportScale).roundToInt().coerceAtLeast(1280)
                                    val outH = (boardH * exportScale).roundToInt().coerceAtLeast(720)
                                    val bmp = Bitmap.createBitmap(outW, outH, Bitmap.Config.ARGB_8888)
                                    val c = AndroidCanvas(bmp)
                                    c.drawColor(android.graphics.Color.BLACK)
                                    c.scale(exportScale, exportScale)
                                    val strokePaint = AndroidPaint(AndroidPaint.ANTI_ALIAS_FLAG).apply {
                                        style = AndroidPaint.Style.STROKE
                                        strokeCap = AndroidPaint.Cap.ROUND
                                        strokeJoin = AndroidPaint.Join.ROUND
                                    }
                                    val dotPaint = AndroidPaint(AndroidPaint.ANTI_ALIAS_FLAG).apply {
                                        style = AndroidPaint.Style.FILL
                                    }
                                    state.boardStrokes.forEach { st ->
                                        if (st.points.isEmpty()) return@forEach
                                        val parsedColor = runCatching { android.graphics.Color.parseColor(st.color) }
                                            .getOrDefault(android.graphics.Color.WHITE)
                                        if (st.points.size == 1) {
                                            dotPaint.color = parsedColor
                                            val pt = st.points.first()
                                            c.drawCircle(pt.x, pt.y, (st.size / 2f).coerceAtLeast(0.8f), dotPaint)
                                        } else {
                                            strokePaint.color = parsedColor
                                            strokePaint.strokeWidth = st.size
                                            val ap = AndroidPath().apply {
                                                moveTo(st.points.first().x, st.points.first().y)
                                                for (i in 1 until st.points.size) lineTo(st.points[i].x, st.points[i].y)
                                            }
                                            c.drawPath(ap, strokePaint)
                                        }
                                    }
                                    DownloadHelper.saveBitmapToGallery(
                                        context,
                                        bmp,
                                        "hefimer_board_${state.activeBoardId}_hd.png",
                                    )
                                }.onSuccess { outcome ->
                                    vm.notifyFileSaved(outcome)
                                    vm.showToast(
                                        tr("Saved 2x HD Board (${(boardW * 2).toInt()}×${(boardH * 2).toInt()})", "Đã lưu bảng HD 2x (${(boardW * 2).toInt()}×${(boardH * 2).toInt()})"),
                                        ToastType.SUCCESS,
                                    )
                                }.onFailure {
                                    vm.showToast(tr("Failed to export board", "Xuất bảng thất bại"), ToastType.ERROR)
                                }
                            }
                        },
                ) {
                    Icon(Icons.Rounded.Download, tr("Export 2x PNG", "Xuất PNG 2x"), tint = HefimerColors.White, modifier = Modifier.size(17.dp))
                }

                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(36.dp)
                        .appleGlassSurface(
                            shape = CircleShape,
                            variant = AppleGlassVariant.Clear,
                            elevation = 0.dp,
                        )
                        .applePressable(pressedScale = 0.89f) { showQr = true },
                ) {
                    Icon(Icons.Rounded.QrCode2, tr("Share", "Chia sẻ"), tint = HefimerColors.White, modifier = Modifier.size(17.dp))
                }
            }
        }

        // Bottom drawing palette & tools dock
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = navBottom + 14.dp, start = 14.dp, end = 14.dp),
        ) {
            AnimatedVisibility(visible = showPalette, enter = AppMotion.softExpandEnter, exit = AppMotion.softExpandExit) {
                Column(
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .appleGlassSurface(
                            shape = RoundedCornerShape(28.dp),
                            variant = AppleGlassVariant.Elevated,
                            elevation = 18.dp,
                        )
                        .padding(14.dp),
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.horizontalScroll(rememberScrollState()),
                    ) {
                        BOARD_COLORS.forEach { hex ->
                            val col = Color(android.graphics.Color.parseColor(hex))
                            val sel = !isEraser && !isPanTool && currentColorHex == hex
                            Box(
                                modifier = Modifier
                                    .size(28.dp)
                                    .clip(CircleShape)
                                    .background(col)
                                    .border(if (sel) 2.5.dp else 1.dp, if (sel) HefimerColors.White else HefimerColors.White20, CircleShape)
                                    .applePressable(pressedScale = 0.88f) {
                                        currentColorHex = hex
                                        isEraser = false
                                        isPanTool = false
                                    },
                            )
                        }
                    }
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text("${tr("SIZE", "CỠ NÉT")} ${strokeSize.roundToInt()}", color = HefimerColors.White50, fontFamily = JetBrainsMonoFamily, fontSize = 10.sp)
                        Slider(
                            value = strokeSize,
                            onValueChange = { strokeSize = it },
                            valueRange = 1f..20f,
                            colors = SliderDefaults.colors(
                                thumbColor = HefimerColors.White,
                                activeTrackColor = HefimerColors.White,
                                inactiveTrackColor = HefimerColors.White15,
                            ),
                            modifier = Modifier.weight(1f),
                        )
                    }
                }
            }

            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .appleGlassSurface(
                        shape = CircleShape,
                        variant = AppleGlassVariant.Elevated,
                        elevation = 20.dp,
                    )
                    .padding(horizontal = 10.dp, vertical = 7.dp),
            ) {
                BoardToolBtn(Icons.Rounded.Brush, active = !isEraser && !isPanTool) {
                    isEraser = false
                    isPanTool = false
                }
                BoardToolBtn(Icons.Rounded.AutoFixHigh, active = isEraser && !isPanTool) {
                    isEraser = true
                    isPanTool = false
                }
                BoardToolBtn(Icons.Rounded.PanTool, active = isPanTool) {
                    isPanTool = !isPanTool
                }
                BoardToolBtn(Icons.Rounded.Palette, active = showPalette) { showPalette = !showPalette }
                BoardToolBtn(Icons.Rounded.Grid4x4, active = showGrid) { showGrid = !showGrid }
                BoardToolBtn(
                    icon = Icons.AutoMirrored.Rounded.Undo,
                    active = false,
                    enabled = state.canUndoBoardStroke || state.boardStrokes.isNotEmpty(),
                ) {
                    vm.undoLastBoardStroke()
                }

                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(38.dp)
                        .appleGlassSurface(
                            shape = CircleShape,
                            variant = AppleGlassVariant.Clear,
                            accentBorderTint = if (state.boardStrokes.isNotEmpty()) HefimerColors.Red400.copy(alpha = 0.55f) else null,
                            elevation = 0.dp,
                        )
                        .applePressable(
                            pressedScale = 0.88f,
                            enabled = state.boardStrokes.isNotEmpty(),
                        ) {
                            vm.requestConfirm(
                                title = tr("Clear Whiteboard?", "Xóa toàn bộ bảng vẽ?"),
                                message = tr(
                                    "All strokes on Board #${state.activeBoardId} will be erased for everyone.",
                                    "Tất cả nét vẽ trên Bảng #${state.activeBoardId} sẽ bị xóa khỏi màn hình của mọi người.",
                                ),
                                confirmLabel = tr("Clear Board", "Xóa bảng"),
                                isDestructive = true,
                            ) {
                                vm.clearBoard()
                            }
                        },
                ) {
                    Icon(
                        Icons.Rounded.DeleteOutline,
                        tr("Clear", "Xóa"),
                        tint = if (state.boardStrokes.isNotEmpty()) HefimerColors.Red400 else HefimerColors.White30,
                        modifier = Modifier.size(18.dp),
                    )
                }
            }
        }
    }

    if (showQr) {
        QrShareModal(
            title = tr("Share Board Link", "Chia sẻ liên kết bảng"),
            url = "${HefimerApiConfig.SITE_ORIGIN}/board?code=${state.activeBoardId}",
            code = state.activeBoardId,
            onDismiss = { showQr = false },
            onToast = { m, e -> vm.showToast(m, if (e) ToastType.ERROR else ToastType.SUCCESS) },
        )
    }
}

@Composable
private fun BoardToolBtn(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    active: Boolean,
    enabled: Boolean = true,
    onClick: () -> Unit,
) {
    val activeFill by animateColorAsState(
        targetValue = if (active) HefimerColors.White else Color.Transparent,
        animationSpec = tween(AppMotion.Fast, easing = AppMotion.AppleSilk),
        label = "board_tool_fill",
    )
    val iconTint by animateColorAsState(
        targetValue = when {
            !enabled -> HefimerColors.White30
            active -> HefimerColors.Black
            else -> HefimerColors.White80
        },
        animationSpec = tween(AppMotion.Fast, easing = AppMotion.AppleSilk),
        label = "board_tool_tint",
    )
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .size(38.dp)
            .appleGlassSurface(
                shape = CircleShape,
                variant = AppleGlassVariant.Clear,
                accentBorderTint = if (active) HefimerColors.White else null,
                accentFillTint = activeFill,
                elevation = 0.dp,
            )
            .applePressable(pressedScale = 0.88f, enabled = enabled, onClick = onClick),
    ) {
        Icon(icon, null, tint = iconTint, modifier = Modifier.size(18.dp))
    }
}
