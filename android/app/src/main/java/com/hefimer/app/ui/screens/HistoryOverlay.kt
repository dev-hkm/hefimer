package com.hefimer.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Brush
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Code
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Forum
import androidx.compose.material.icons.rounded.GridView
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material.icons.rounded.UploadFile
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hefimer.app.data.model.HistoryItem
import com.hefimer.app.data.model.ToastType
import com.hefimer.app.data.model.formatBytes
import com.hefimer.app.ui.HefimerViewModel
import com.hefimer.app.ui.components.HefimerCollapsibleScaffold
import com.hefimer.app.ui.components.HefimerHeaderIconButton
import com.hefimer.app.ui.components.MainTab
import com.hefimer.app.ui.components.copyToClipboard
import com.hefimer.app.ui.theme.HefimerColors
import com.hefimer.app.ui.theme.JetBrainsMonoFamily
import com.hefimer.app.ui.theme.appleEntrance
import com.hefimer.app.ui.theme.applePressable
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun HistoryOverlayScreen(
    vm: HefimerViewModel,
    onClose: () -> Unit,
) {
    val history by vm.history.collectAsState()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var copiedCode by remember { mutableStateOf<String?>(null) }
    val timeFmt = remember { SimpleDateFormat("HH:mm", Locale.getDefault()) }

    HefimerCollapsibleScaffold(
        kicker = "Recent activity",
        title = "History",
        subtitle = "Tap any item to reopen it or copy its 5-digit code.",
        bottomDockClearance = 36.dp,
        headerActions = {
            if (history.isNotEmpty()) {
                HefimerHeaderIconButton(
                    icon = Icons.Rounded.DeleteOutline,
                    contentDescription = "Clear history",
                    onClick = { vm.clearHistory() },
                )
            }
            HefimerHeaderIconButton(
                icon = Icons.AutoMirrored.Rounded.ArrowBack,
                contentDescription = "Close",
                onClick = onClose,
            )
        },
    ) {
        if (history.isEmpty()) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 64.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Icon(Icons.Rounded.History, null, tint = HefimerColors.White20, modifier = Modifier.size(48.dp))
                Text("No recent history", color = HefimerColors.White40, fontSize = 14.sp, fontWeight = FontWeight.Medium)
            }
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                history.forEachIndexed { idx, item ->
                    HistoryItemCard(
                        item = item,
                        timeLabel = timeFmt.format(Date(item.createdAt)),
                        copied = copiedCode == item.code,
                        modifier = Modifier.appleEntrance(idx),
                        onOpen = {
                            onClose()
                            when (item.type) {
                                "space" -> {
                                    vm.setSpaceJoinCode(item.code)
                                    vm.setSpaceLobbyMode("join")
                                    vm.selectTab(MainTab.SPACE)
                                }
                                "room" -> {
                                    vm.setRoomsSection("chat")
                                    vm.setChatLobbyMode("join")
                                    vm.setChatJoinCode(item.code)
                                    vm.selectTab(MainTab.ROOMS)
                                }
                                "board" -> {
                                    vm.setRoomsSection("board")
                                    vm.setBoardLobbyMode("join")
                                    vm.setBoardJoinCode(item.code)
                                    vm.selectTab(MainTab.ROOMS)
                                }
                                else -> {
                                    vm.selectTab(MainTab.RECEIVE)
                                    vm.setReceiveCode(item.code)
                                    vm.retrieveDrop(item.code)
                                }
                            }
                        },
                        onCopy = {
                            copyToClipboard(context, "Hefimer Code", item.code)
                            copiedCode = item.code
                            vm.showToast("Code ${item.code} copied", ToastType.SUCCESS)
                            scope.launch {
                                delay(1800L)
                                if (copiedCode == item.code) copiedCode = null
                            }
                        },
                    )
                }
            }
        }
    }
}

@Composable
private fun HistoryItemCard(
    item: HistoryItem,
    timeLabel: String,
    copied: Boolean,
    modifier: Modifier = Modifier,
    onOpen: () -> Unit,
    onCopy: () -> Unit,
) {
    val isCreated = item.action in setOf("sent", "created")
    val badgeBg = if (isCreated) HefimerColors.Emerald400.copy(alpha = 0.16f) else HefimerColors.Blue400.copy(alpha = 0.16f)
    val badgeFg = if (isCreated) HefimerColors.Emerald400 else HefimerColors.Blue400
    val badgeLabel = when (item.action) {
        "sent", "created" -> "CREATED"
        "joined" -> "JOINED"
        else -> "RECEIVED"
    }
    val typeIcon = when (item.type) {
        "r2_file" -> Icons.Rounded.Shield
        "file" -> Icons.Rounded.UploadFile
        "text" -> Icons.Rounded.Code
        "room" -> Icons.Rounded.Forum
        "board" -> Icons.Rounded.Brush
        "space" -> Icons.Rounded.GridView
        else -> Icons.Rounded.History
    }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(HefimerColors.White04)
            .border(1.dp, HefimerColors.White10, RoundedCornerShape(24.dp))
            .applePressable(pressedScale = 0.98f, onClick = onOpen)
            .padding(horizontal = 16.dp, vertical = 14.dp),
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(
                    text = item.code,
                    color = HefimerColors.White,
                    fontFamily = JetBrainsMonoFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 22.sp,
                    letterSpacing = 2.sp,
                )
                Text(
                    text = badgeLabel,
                    color = badgeFg,
                    fontSize = 8.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.2.sp,
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(badgeBg)
                        .padding(horizontal = 8.dp, vertical = 3.dp),
                )
            }
            Spacer(Modifier.height(5.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Icon(
                    imageVector = typeIcon,
                    contentDescription = null,
                    tint = if (item.isR2) HefimerColors.Emerald400 else HefimerColors.White50,
                    modifier = Modifier.size(13.dp),
                )
                Text(
                    text = item.fileName ?: item.roomName ?: item.boardName ?: item.type.uppercase(),
                    color = HefimerColors.White50,
                    fontSize = 11.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                if (item.fileSize != null && item.fileSize > 0L) {
                    Text("•", color = HefimerColors.White20, fontSize = 10.sp)
                    Text(formatBytes(item.fileSize), color = HefimerColors.White30, fontSize = 10.sp)
                }
                Text("•", color = HefimerColors.White20, fontSize = 10.sp)
                Text(timeLabel, color = HefimerColors.White30, fontSize = 10.sp)
            }
        }

        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(if (copied) HefimerColors.Emerald400 else HefimerColors.White08)
                .applePressable(pressedScale = 0.9f, onClick = onCopy),
        ) {
            Icon(
                imageVector = if (copied) Icons.Rounded.Check else Icons.Rounded.ContentCopy,
                contentDescription = "Copy",
                tint = if (copied) HefimerColors.Black else HefimerColors.White70,
                modifier = Modifier.size(16.dp),
            )
        }
    }
}
