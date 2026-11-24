package com.hefimer.app.ui.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
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
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Description
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.Group
import androidx.compose.material.icons.rounded.GroupAdd
import androidx.compose.material.icons.rounded.Inventory2
import androidx.compose.material.icons.rounded.Laptop
import androidx.compose.material.icons.rounded.Link
import androidx.compose.material.icons.rounded.Logout
import androidx.compose.material.icons.rounded.QrCode2
import androidx.compose.material.icons.rounded.QrCodeScanner
import androidx.compose.material.icons.rounded.Radio
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Send
import androidx.compose.material.icons.rounded.Smartphone
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
import com.hefimer.app.ui.theme.AppleSegmentedControl
import com.hefimer.app.ui.theme.HefimerColors
import com.hefimer.app.ui.theme.JetBrainsMonoFamily
import com.hefimer.app.ui.theme.appleEntrance
import com.hefimer.app.ui.theme.applePressable
import com.hefimer.app.ui.theme.rememberReduceMotion
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
        title = "Paired Devices",
        subtitle = "Pair trusted devices once without accounts, then push drops directly to your group.",
        showOrbitMark = true,
        headerActions = {
            HefimerHeaderIconButton(
                icon = Icons.Rounded.QrCodeScanner,
                contentDescription = "Scan pairing QR",
                onClick = { vm.openOverlay(AppOverlay.QrScanner(ScanTarget.PAIR_TOKEN)) },
            )
            HefimerHeaderIconButton(
                icon = Icons.Rounded.Refresh,
                contentDescription = "Refresh",
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
                    Text("SECURING THIS DEVICE", color = HefimerColors.White40, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 2.4.sp)
                }
            }
            state.error.isNotBlank() && state.sync.device.id.isBlank() -> {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(32.dp))
                        .background(HefimerColors.SurfaceElevated)
                        .border(1.dp, HefimerColors.White10, RoundedCornerShape(32.dp))
                        .padding(28.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Text("Device service unavailable", color = HefimerColors.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    Text(state.error, color = HefimerColors.White40, fontSize = 12.sp, textAlign = TextAlign.Center)
                    HefimerPrimaryButton("Try Again", onClick = { vm.refreshLink(silent = false) })
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
                .clip(RoundedCornerShape(30.dp))
                .background(HefimerColors.White04)
                .border(1.dp, HefimerColors.White10, RoundedCornerShape(30.dp))
                .padding(22.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Icon(Icons.Rounded.VerifiedUser, null, tint = HefimerColors.White, modifier = Modifier.size(26.dp))
            Spacer(Modifier.height(6.dp))
            Text("ACCOUNTLESS IDENTITY", color = HefimerColors.White50, fontSize = 9.sp, fontWeight = FontWeight.Bold, letterSpacing = 2.4.sp)
            Text("This phone is now a signed device.", color = HefimerColors.White, fontSize = 22.sp, fontWeight = FontWeight.Bold, letterSpacing = (-0.6).sp)
            Text(
                text = "Your P-256 private key stays locally on this device. Pair once with browsers or phones, no email or password required.",
                color = HefimerColors.White40,
                fontSize = 13.sp,
                lineHeight = 19.sp,
            )
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier
                    .padding(top = 4.dp)
                    .clip(CircleShape)
                    .background(HefimerColors.Black.copy(alpha = 0.5f))
                    .border(1.dp, HefimerColors.White10, CircleShape)
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
            label = { if (it == "create") "Create Group" else "Enter Token" },
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
                    .clip(RoundedCornerShape(30.dp))
                    .background(HefimerColors.SurfaceElevated)
                    .border(1.dp, HefimerColors.White10, RoundedCornerShape(30.dp))
                    .padding(20.dp),
            ) {
                if (mode == "create") {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text("GROUP NAME", color = HefimerColors.White30, fontSize = 9.sp, fontWeight = FontWeight.Bold, letterSpacing = 2.sp)
                        HefimerTextField(
                            value = state.groupName,
                            onValueChange = { vm.setLinkGroupName(it.take(64)) },
                            placeholder = "My Hefimer Group",
                        )
                    }
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("LINK LIFETIME", color = HefimerColors.White30, fontSize = 9.sp, fontWeight = FontWeight.Bold, letterSpacing = 2.sp)
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            listOf("12h" to "12 hours", "24h" to "24 hours", "7d" to "7 days", "30d" to "30 days", "never" to "Forever").forEach { (v, lbl) ->
                                val sel = state.duration == v
                                Text(
                                    text = lbl,
                                    color = if (sel) HefimerColors.Black else HefimerColors.White50,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(14.dp))
                                        .background(if (sel) HefimerColors.White else HefimerColors.White06)
                                        .border(1.dp, if (sel) HefimerColors.White else HefimerColors.White10, RoundedCornerShape(14.dp))
                                        .applePressable { vm.setLinkDuration(v) }
                                        .padding(horizontal = 14.dp, vertical = 10.dp),
                                )
                            }
                        }
                    }
                    HefimerPrimaryButton(
                        text = if (state.busyKey == "create") "Creating..." else "Create Secure Group",
                        icon = Icons.Rounded.Group,
                        loading = state.busyKey == "create",
                        enabled = state.busyKey.isEmpty(),
                        onClick = { vm.createLinkGroup() },
                    )
                } else {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(22.dp))
                            .background(HefimerColors.White04)
                            .border(1.dp, HefimerColors.White10, RoundedCornerShape(22.dp))
                            .padding(16.dp),
                    ) {
                        Icon(Icons.Rounded.QrCode2, null, tint = HefimerColors.White, modifier = Modifier.size(22.dp))
                        Text("Scan a pairing QR or paste the single-use token.", color = HefimerColors.White, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                        Text("Every token pairs one device and expires after ten minutes.", color = HefimerColors.White40, fontSize = 12.sp)
                    }

                    HefimerSecondaryButton(
                        text = "Scan Pairing QR Code",
                        icon = Icons.Rounded.QrCodeScanner,
                        onClick = { vm.openOverlay(AppOverlay.QrScanner(ScanTarget.PAIR_TOKEN)) },
                    )

                    HefimerTextField(
                        value = state.pairToken,
                        onValueChange = { vm.setLinkPairToken(it) },
                        placeholder = "hfm-••••••••••••••••••••••••••••••••••••",
                        mono = true,
                    )

                    val validToken = Regex("""^hfm-[A-Za-z0-9]{36}$""").matches(state.pairToken.trim())
                    HefimerPrimaryButton(
                        text = if (state.busyKey == "join") "Pairing..." else "Pair This Device",
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
                .clip(RoundedCornerShape(32.dp))
                .background(HefimerColors.White04)
                .border(1.dp, HefimerColors.White10, RoundedCornerShape(32.dp)),
        ) {
            Canvas(modifier = Modifier.matchParentSize()) {
                val center = Offset(size.width - 10.dp.toPx(), 10.dp.toPx())
                drawCircle(Color.White.copy(alpha = 0.08f), radius = 92.dp.toPx(), center = center, style = Stroke(1.dp.toPx()))
                drawCircle(Color.White.copy(alpha = 0.05f), radius = 56.dp.toPx(), center = center, style = Stroke(1.dp.toPx()))
            }

            Column(modifier = Modifier.padding(22.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Icon(Icons.Rounded.Radio, null, tint = HefimerColors.Emerald400, modifier = Modifier.size(13.dp))
                    Text("ACTIVE GROUP", color = HefimerColors.White50, fontSize = 9.sp, fontWeight = FontWeight.Bold, letterSpacing = 2.4.sp)
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
                    text = "${state.sync.members.size} device${if (state.sync.members.size == 1) "" else "s"} · ${formatRemainingCompact(group.expiresAt)}",
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
                if (it == "devices") "Devices" else if (pendingCount > 0) "Activity ($pendingCount)" else "Activity"
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
                            .clip(RoundedCornerShape(28.dp))
                            .background(HefimerColors.White025)
                            .border(1.dp, HefimerColors.White10, RoundedCornerShape(28.dp))
                            .padding(18.dp),
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Column {
                                Text("TRUSTED LANE", color = HefimerColors.White30, fontSize = 9.sp, fontWeight = FontWeight.Bold, letterSpacing = 2.2.sp)
                                Text("Connected devices", color = HefimerColors.White, fontSize = 17.sp, fontWeight = FontWeight.Bold)
                            }
                            Text(
                                text = "${state.sync.members.count { it.online }} online",
                                color = HefimerColors.White50,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier
                                    .clip(CircleShape)
                                    .border(1.dp, HefimerColors.White10, CircleShape)
                                    .padding(horizontal = 10.dp, vertical = 4.dp),
                            )
                        }

                        state.sync.members.forEach { member ->
                            MemberRow(
                                member = member,
                                isSelf = member.id == state.sync.device.id,
                                canRemove = isOwner && member.id != state.sync.device.id,
                                onRemove = { vm.removeLinkDevice(member.id) },
                            )
                        }

                        // Rename this device
                        Column(
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier
                                .padding(top = 6.dp)
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(20.dp))
                                .background(HefimerColors.Black.copy(alpha = 0.4f))
                                .border(1.dp, HefimerColors.White08, RoundedCornerShape(20.dp))
                                .padding(12.dp),
                        ) {
                            Text("THIS DEVICE NAME", color = HefimerColors.White30, fontSize = 9.sp, fontWeight = FontWeight.Bold, letterSpacing = 2.sp)
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                                HefimerTextField(
                                    value = state.deviceNameInput,
                                    onValueChange = { vm.setLinkDeviceNameInput(it.take(48)) },
                                    placeholder = "Device name",
                                    modifier = Modifier.weight(1f),
                                )
                                Text(
                                    text = "SAVE",
                                    color = HefimerColors.White,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 1.4.sp,
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(14.dp))
                                        .background(HefimerColors.White10)
                                        .border(1.dp, HefimerColors.White12, RoundedCornerShape(14.dp))
                                        .applePressable { vm.renameLinkDevice() }
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
                                .clip(RoundedCornerShape(28.dp))
                                .background(
                                    Brush.verticalGradient(listOf(HefimerColors.White08, HefimerColors.White025)),
                                )
                                .border(1.dp, HefimerColors.White10, RoundedCornerShape(28.dp))
                                .padding(18.dp),
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Column {
                                    Text("ONE-TIME ENTRANCE", color = HefimerColors.White40, fontSize = 9.sp, fontWeight = FontWeight.Bold, letterSpacing = 2.2.sp)
                                    Text("Pair another device", color = HefimerColors.White, fontSize = 17.sp, fontWeight = FontWeight.Bold)
                                }
                                Icon(Icons.Rounded.GroupAdd, null, tint = HefimerColors.White50, modifier = Modifier.size(22.dp))
                            }

                            val inv = state.invite
                            if (inv == null) {
                                HefimerPrimaryButton(
                                    text = if (state.busyKey == "invite") "Generating..." else "Generate QR + Token",
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
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(18.dp))
                                        .background(HefimerColors.Black.copy(alpha = 0.5f))
                                        .border(1.dp, HefimerColors.White12, RoundedCornerShape(18.dp))
                                        .applePressable {
                                            copyToClipboard(context, "Pairing Token", inv.token)
                                            copiedToken = true
                                            vm.showToast("Pairing token copied", ToastType.SUCCESS)
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
                                    Text("SINGLE USE", color = HefimerColors.White30, fontSize = 9.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.4.sp)
                                    Text(formatRemainingCompact(inv.expiresAt), color = HefimerColors.White30, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                                }
                                HefimerSecondaryButton("Replace Token", height = 42.dp, onClick = { vm.createLinkInvite() })
                            }
                        }
                    }

                    // Always Approve toggle
                    HefimerToggleRow(
                        title = "Always approve",
                        subtitle = "Accept & auto-download file offers from this group",
                        checked = group.autoApprove,
                        onToggle = { vm.toggleLinkAutoApprove() },
                        icon = Icons.Rounded.Inventory2,
                        activeColor = HefimerColors.White,
                    )

                    // Leave / Delete Group
                    HefimerSecondaryButton(
                        text = if (isOwner) "Delete Group For Everyone" else "Leave This Group",
                        icon = Icons.Rounded.Logout,
                        onClick = { vm.leaveLinkGroup() },
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
                            .clip(RoundedCornerShape(28.dp))
                            .background(HefimerColors.White025)
                            .border(1.dp, HefimerColors.White10, RoundedCornerShape(28.dp))
                            .padding(18.dp),
                    ) {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Column {
                                Text("INCOMING", color = HefimerColors.White40, fontSize = 9.sp, fontWeight = FontWeight.Bold, letterSpacing = 2.2.sp)
                                Text("Waiting for you", color = HefimerColors.White, fontSize = 17.sp, fontWeight = FontWeight.Bold)
                            }
                            Icon(Icons.Rounded.Download, null, tint = HefimerColors.White40, modifier = Modifier.size(20.dp))
                        }
                        if (state.sync.incoming.isEmpty()) {
                            Text(
                                text = "Nothing incoming yet.",
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
                            .clip(RoundedCornerShape(28.dp))
                            .background(HefimerColors.White025)
                            .border(1.dp, HefimerColors.White10, RoundedCornerShape(28.dp))
                            .padding(18.dp),
                    ) {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Column {
                                Text("OUTGOING", color = HefimerColors.White40, fontSize = 9.sp, fontWeight = FontWeight.Bold, letterSpacing = 2.2.sp)
                                Text("Sent from here", color = HefimerColors.White, fontSize = 17.sp, fontWeight = FontWeight.Bold)
                            }
                            Icon(Icons.Rounded.Send, null, tint = HefimerColors.White40, modifier = Modifier.size(19.dp))
                        }
                        if (state.sync.outgoing.isEmpty()) {
                            Text(
                                text = "Your next drop will appear here automatically.",
                                color = HefimerColors.White30,
                                fontSize = 12.sp,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.fillMaxWidth().padding(vertical = 24.dp),
                            )
                        } else {
                            state.sync.outgoing.forEach { tr ->
                                OutgoingTransferRow(
                                    transfer = tr,
                                    onCancel = { vm.cancelLinkTransfer(tr.id) },
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
            .clip(RoundedCornerShape(18.dp))
            .background(HefimerColors.Black.copy(alpha = 0.4f))
            .border(1.dp, HefimerColors.White08, RoundedCornerShape(18.dp))
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
                if (isSelf) append(" · This device")
                if (member.isOwner) append(" · Owner")
            }
            Text(meta, color = HefimerColors.White40, fontSize = 11.sp)
        }
        if (canRemove) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.size(34.dp).clip(CircleShape).applePressable(onClick = onRemove),
            ) {
                Icon(Icons.Rounded.DeleteOutline, "Remove", tint = HefimerColors.White30, modifier = Modifier.size(16.dp))
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
            .clip(RoundedCornerShape(20.dp))
            .background(HefimerColors.Black.copy(alpha = 0.45f))
            .border(1.dp, HefimerColors.White08, RoundedCornerShape(20.dp))
            .padding(14.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.size(38.dp).clip(RoundedCornerShape(12.dp)).background(HefimerColors.White06),
            ) {
                Icon(if (transfer.kind == "file") Icons.Rounded.Download else Icons.Rounded.Description, null, tint = HefimerColors.White, modifier = Modifier.size(18.dp))
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(transfer.name, color = HefimerColors.White, fontSize = 14.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text("${transfer.senderName ?: "Paired device"} · ${formatBytes(transfer.sizeBytes)}", color = HefimerColors.White40, fontSize = 11.sp)
            }
            LinkStatusPill(status)
        }

        if (status == "pending") {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                HefimerSecondaryButton("Decline", height = 40.dp, modifier = Modifier.weight(1f), onClick = onDecline)
                HefimerPrimaryButton("Approve", height = 40.dp, modifier = Modifier.weight(1f), onClick = onApprove)
            }
        } else if (status in setOf("approved", "downloading", "received")) {
            HefimerPrimaryButton("Open Transfer", icon = Icons.Rounded.ChevronRight, height = 42.dp, onClick = onOpen)
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
            .clip(RoundedCornerShape(20.dp))
            .background(HefimerColors.Black.copy(alpha = 0.45f))
            .border(1.dp, HefimerColors.White08, RoundedCornerShape(20.dp))
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
                        "received" -> HefimerColors.Emerald400
                        "declined", "failed" -> HefimerColors.White20
                        else -> HefimerColors.White60
                    }
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(5.dp),
                        modifier = Modifier
                            .clip(CircleShape)
                            .border(1.dp, HefimerColors.White10, CircleShape)
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
                modifier = Modifier.applePressable(onClick = onCancel),
            ) {
                Icon(Icons.Rounded.Close, null, tint = HefimerColors.White30, modifier = Modifier.size(13.dp))
                Text("CANCEL OFFER", color = HefimerColors.White30, fontSize = 9.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.4.sp)
            }
        }
    }
}

@Composable
private fun LinkStatusPill(status: String) {
    val label = when (status) {
        "pending" -> "WAITING"
        "approved" -> "READY"
        "declined" -> "DECLINED"
        "downloading" -> "OPENING"
        "received" -> "RECEIVED"
        "failed" -> "FAILED"
        "cancelled" -> "CANCELLED"
        "expired" -> "EXPIRED"
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
            .clip(CircleShape)
            .background(if (active) HefimerColors.White12 else HefimerColors.White04)
            .border(1.dp, if (active) HefimerColors.White20 else HefimerColors.White10, CircleShape)
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
        enter = slideInVertically(tween(380, easing = AppMotion.AppleSheet)) { it / 2 } +
            fadeIn(tween(260, easing = AppMotion.AppleSilk)) +
            scaleIn(tween(340, easing = AppMotion.AppleSheet), initialScale = 0.94f),
        exit = slideOutVertically(tween(260, easing = AppMotion.AppleSilk)) { it / 2 } +
            fadeOut(tween(180, easing = AppMotion.AppleEase)) +
            scaleOut(tween(220), targetScale = 0.95f),
        modifier = modifier.fillMaxWidth().padding(horizontal = 16.dp),
    ) {
        val current = transfer ?: return@AnimatedVisibility
        Column(
            verticalArrangement = Arrangement.spacedBy(14.dp),
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(28.dp))
                .background(Color(0xF50B0B0B))
                .border(1.dp, HefimerColors.White15, RoundedCornerShape(28.dp))
                .padding(18.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(46.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(HefimerColors.White10)
                        .border(1.dp, HefimerColors.White20, RoundedCornerShape(16.dp)),
                ) {
                    Icon(if (current.kind == "file") Icons.Rounded.Download else Icons.Rounded.Description, null, tint = HefimerColors.White, modifier = Modifier.size(21.dp))
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "INCOMING FROM ${(current.senderName ?: "PAIRED DEVICE").uppercase()}",
                        color = HefimerColors.Emerald400,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 2.sp,
                    )
                    Text(current.name, color = HefimerColors.White, fontSize = 16.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text("${formatBytes(current.sizeBytes)} · ${current.provider}", color = HefimerColors.White40, fontSize = 11.sp)
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                HefimerSecondaryButton("Decline", height = 44.dp, modifier = Modifier.weight(1f), onClick = { onDecline(current) })
                HefimerPrimaryButton("Approve", icon = Icons.Rounded.Check, height = 44.dp, modifier = Modifier.weight(1f), onClick = { onApprove(current) })
            }
        }
    }
}
