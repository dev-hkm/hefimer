package com.hefimer.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import com.adamglin.PhosphorIcons
import com.adamglin.phosphoricons.Regular
import com.adamglin.phosphoricons.regular.Bell
import com.adamglin.phosphoricons.regular.CloudArrowUp
import com.adamglin.phosphoricons.regular.Lock
import com.adamglin.phosphoricons.regular.ShieldCheck
import com.adamglin.phosphoricons.regular.Snowflake
import com.adamglin.phosphoricons.regular.UserGear
import com.adamglin.phosphoricons.regular.X
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.hefimer.app.data.network.AdminR2ConfigDto
import com.hefimer.app.data.network.AdminStatsDto
import com.hefimer.app.ui.screens.HefimerTextField
import com.hefimer.app.ui.theme.AppleGlassVariant
import com.hefimer.app.ui.theme.AppleSegmentedControl
import com.hefimer.app.ui.theme.HefimerColors
import com.hefimer.app.ui.theme.JetBrainsMonoFamily
import com.hefimer.app.ui.theme.appleGlassSurface
import com.hefimer.app.ui.theme.applePressable
import com.hefimer.app.ui.theme.tr

@Composable
fun AdminLoginDialog(
    loading: Boolean,
    onDismiss: () -> Unit,
    onSubmit: (String) -> Unit,
) {
    var pass by remember { mutableStateOf("") }
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .appleGlassSurface(
                    shape = RoundedCornerShape(32.dp),
                    variant = AppleGlassVariant.Elevated,
                    elevation = 24.dp,
                )
                .padding(24.dp),
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(48.dp)
                        .appleGlassSurface(shape = CircleShape, variant = AppleGlassVariant.Clear, elevation = 0.dp),
                ) {
                    Icon(PhosphorIcons.Regular.UserGear, null, tint = HefimerColors.White, modifier = Modifier.size(22.dp))
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = tr("ADMIN CONSOLE", "BẢNG ĐIỀU KHIỂN ADMIN"),
                        color = HefimerColors.White,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 2.sp,
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = tr(
                            "Verified on Hefimer backend via salted PBKDF2",
                            "Xác thực trực tiếp trên backend qua PBKDF2 bảo mật",
                        ),
                        color = HefimerColors.White45,
                        fontSize = 11.sp,
                        textAlign = TextAlign.Center,
                    )
                }
                HefimerTextField(
                    value = pass,
                    onValueChange = { pass = it },
                    placeholder = tr("Admin password...", "Mật khẩu quản trị..."),
                    leadingIcon = PhosphorIcons.Regular.Lock,
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
                            .height(46.dp)
                            .appleGlassSurface(shape = CircleShape, variant = AppleGlassVariant.Clear, elevation = 0.dp)
                            .applePressable(pressedScale = 0.96f, onClick = onDismiss),
                    ) {
                        Text(tr("Cancel", "Hủy"), color = HefimerColors.White60, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    }
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .weight(1f)
                            .height(46.dp)
                            .clip(CircleShape)
                            .background(HefimerColors.White)
                            .applePressable(enabled = !loading, pressedScale = 0.96f) { onSubmit(pass) },
                    ) {
                        if (loading) {
                            CircularProgressIndicator(
                                color = HefimerColors.Black,
                                strokeWidth = 2.dp,
                                modifier = Modifier.size(18.dp),
                            )
                        } else {
                            Text(tr("Sign In", "Đăng nhập"), color = HefimerColors.Black, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun AdminDashboardSheetContent(
    stats: AdminStatsDto,
    r2Config: AdminR2ConfigDto,
    busy: Boolean,
    onToggleFreeze: () -> Unit,
    onSetDeveloper: (String) -> Unit,
    onSendBroadcast: (title: String, message: String, durationMinutes: Int) -> Unit,
    onClearBroadcast: () -> Unit,
    onUpdateLikes: (Int) -> Unit,
    onUpdatePasswords: (adminPass: String, secretPass: String) -> Unit,
    onUpdateR2Config: (accountId: String, accessKeyId: String, secretAccessKey: String, bucketName: String) -> Unit,
    onLogout: () -> Unit,
    onClose: () -> Unit,
) {
    var broadcastTitle by remember { mutableStateOf("") }
    var broadcastMsg by remember { mutableStateOf("") }
    var broadcastMinutes by remember { mutableStateOf(60) }
    var likesDraft by remember(stats.likes) { mutableStateOf(stats.likes.toString()) }

    var newAdminPass by remember { mutableStateOf("") }
    var newSecretPass by remember { mutableStateOf("") }

    var accountId by remember(r2Config.accountId) { mutableStateOf(r2Config.accountId) }
    var accessKeyId by remember(r2Config.accessKeyId) { mutableStateOf(r2Config.accessKeyId) }
    var secretAccessKey by remember(r2Config.secretAccessKey) { mutableStateOf(r2Config.secretAccessKey) }
    var bucketName by remember(r2Config.bucketName) { mutableStateOf(r2Config.bucketName) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .fillMaxHeight(0.88f)
            .appleGlassSurface(
                shape = RoundedCornerShape(topStart = 36.dp, topEnd = 36.dp),
                variant = AppleGlassVariant.Elevated,
                elevation = 24.dp,
            )
            .navigationBarsPadding(),
    ) {
        Spacer(Modifier.height(10.dp))
        Box(
            modifier = Modifier
                .align(Alignment.CenterHorizontally)
                .width(38.dp)
                .height(4.dp)
                .clip(CircleShape)
                .background(HefimerColors.White20),
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(38.dp)
                    .appleGlassSurface(shape = RoundedCornerShape(14.dp), variant = AppleGlassVariant.Clear, elevation = 0.dp),
            ) {
                Icon(PhosphorIcons.Regular.UserGear, null, tint = HefimerColors.White, modifier = Modifier.size(18.dp))
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = tr("BACKEND CONTROL", "QUẢN TRỊ BACKEND"),
                    color = HefimerColors.White45,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 2.2.sp,
                )
                Text(
                    text = tr("Admin Dashboard", "Bảng điều khiển Admin"),
                    color = HefimerColors.White,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                )
            }
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(34.dp)
                    .appleGlassSurface(shape = CircleShape, variant = AppleGlassVariant.Clear, elevation = 0.dp)
                    .applePressable(pressedScale = 0.89f, onClick = onClose),
            ) {
                Icon(PhosphorIcons.Regular.X, tr("Close", "Đóng"), tint = HefimerColors.White70, modifier = Modifier.size(16.dp))
            }
        }

        Box(Modifier.fillMaxWidth().height(0.5.dp).background(HefimerColors.White10))

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 18.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            // 1. System Controls (Freeze & Developer & Likes)
            Column(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .appleGlassSurface(shape = RoundedCornerShape(24.dp), variant = AppleGlassVariant.Clear, elevation = 0.dp)
                    .padding(14.dp),
            ) {
                Text(
                    text = tr("SYSTEM CONTROLS", "ĐIỀU KHIỂN HỆ THỐNG"),
                    color = HefimerColors.White45,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 2.sp,
                )
                HefimerToggleRow(
                    title = tr("Freeze Public System", "Đóng băng hệ thống"),
                    subtitle = tr("Temporarily lock public uploads & drops", "Tạm khóa tải lên và chia sẻ công khai"),
                    checked = stats.isFrozen,
                    onToggle = onToggleFreeze,
                    icon = PhosphorIcons.Regular.Snowflake,
                    activeColor = HefimerColors.Red400,
                )
                Text(
                    text = tr("DEVELOPER CREDIT", "TÍN NHIỆM NHÀ PHÁT TRIỂN"),
                    color = HefimerColors.White40,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.6.sp,
                )
                AppleSegmentedControl(
                    options = listOf("Khanh Minh", "Minh Ngoc"),
                    selected = if (stats.developer == "Minh Ngoc") "Minh Ngoc" else "Khanh Minh",
                    label = { it },
                    onSelect = onSetDeveloper,
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    HefimerTextField(
                        value = likesDraft,
                        onValueChange = { likesDraft = it.filter { ch -> ch.isDigit() } },
                        placeholder = tr("Global likes...", "Số lượt thích..."),
                        mono = true,
                        modifier = Modifier.weight(1f),
                    )
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .height(48.dp)
                            .clip(RoundedCornerShape(18.dp))
                            .background(HefimerColors.White)
                            .applePressable(enabled = !busy, pressedScale = 0.95f) {
                                likesDraft.toIntOrNull()?.let(onUpdateLikes)
                            }
                            .padding(horizontal = 16.dp),
                    ) {
                        Text(tr("Set Likes", "Lưu"), color = HefimerColors.Black, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }

            // 2. Global Broadcast
            Column(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .appleGlassSurface(shape = RoundedCornerShape(24.dp), variant = AppleGlassVariant.Clear, elevation = 0.dp)
                    .padding(14.dp),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(PhosphorIcons.Regular.Bell, null, tint = HefimerColors.White70, modifier = Modifier.size(16.dp))
                    Text(
                        text = tr("GLOBAL BROADCAST", "THÔNG BÁO TOÀN HỆ THỐNG"),
                        color = HefimerColors.White45,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 2.sp,
                    )
                }
                HefimerTextField(
                    value = broadcastTitle,
                    onValueChange = { broadcastTitle = it },
                    placeholder = tr("Title (optional)...", "Tiêu đề (tùy chọn)..."),
                )
                HefimerTextField(
                    value = broadcastMsg,
                    onValueChange = { broadcastMsg = it },
                    placeholder = tr("Announcement message...", "Nội dung thông báo..."),
                    singleLine = false,
                )
                AppleSegmentedControl(
                    options = listOf(15, 60, 1440, 0),
                    selected = broadcastMinutes,
                    label = {
                        when (it) {
                            15 -> "15m"
                            60 -> "1h"
                            1440 -> "24h"
                            else -> tr("Always", "Luôn")
                        }
                    },
                    onSelect = { broadcastMinutes = it },
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .weight(1f)
                            .height(44.dp)
                            .appleGlassSurface(shape = CircleShape, variant = AppleGlassVariant.Clear, elevation = 0.dp)
                            .applePressable(enabled = !busy, pressedScale = 0.96f, onClick = onClearBroadcast),
                    ) {
                        Text(tr("Clear", "Xóa"), color = HefimerColors.White70, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .weight(2f)
                            .height(44.dp)
                            .clip(CircleShape)
                            .background(HefimerColors.White)
                            .applePressable(enabled = !busy, pressedScale = 0.96f) {
                                onSendBroadcast(broadcastTitle, broadcastMsg, broadcastMinutes)
                                broadcastTitle = ""
                                broadcastMsg = ""
                            },
                    ) {
                        Text(tr("Broadcast", "Phát thông báo"), color = HefimerColors.Black, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }

            // 3. Backend Security Passwords (PBKDF2-SHA256)
            Column(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .appleGlassSurface(shape = RoundedCornerShape(24.dp), variant = AppleGlassVariant.Clear, elevation = 0.dp)
                    .padding(14.dp),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(PhosphorIcons.Regular.ShieldCheck, null, tint = HefimerColors.White70, modifier = Modifier.size(16.dp))
                    Text(
                        text = tr("SECURITY PASSWORDS (BACKEND KV)", "MẬT KHẨU BẢO MẬT (BACKEND)"),
                        color = HefimerColors.White45,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.8.sp,
                    )
                }
                HefimerTextField(
                    value = newAdminPass,
                    onValueChange = { newAdminPass = it },
                    placeholder = tr("New Admin password...", "Mật khẩu Admin mới..."),
                    leadingIcon = PhosphorIcons.Regular.Lock,
                    isPassword = true,
                    mono = true,
                )
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(44.dp)
                        .clip(CircleShape)
                        .background(HefimerColors.White)
                        .applePressable(enabled = !busy, pressedScale = 0.96f) {
                            onUpdatePasswords(newAdminPass, newSecretPass)
                            newAdminPass = ""
                            newSecretPass = ""
                        },
                ) {
                    Text(
                        text = tr("Update Passwords", "Cập nhật mật khẩu"),
                        color = HefimerColors.Black,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                    )
                }
            }

            // 4. Cloudflare R2 Credentials (4 fields)
            Column(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .appleGlassSurface(shape = RoundedCornerShape(24.dp), variant = AppleGlassVariant.Clear, elevation = 0.dp)
                    .padding(14.dp),
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Icon(PhosphorIcons.Regular.CloudArrowUp, null, tint = HefimerColors.White85, modifier = Modifier.size(16.dp))
                    Text(
                        text = tr("CLOUDFLARE R2 CREDENTIALS", "CẤU HÌNH CLOUDFLARE R2 (4 KHÓA)"),
                        color = HefimerColors.White45,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.8.sp,
                        modifier = Modifier.weight(1f),
                    )
                    Text(
                        text = r2Config.source.uppercase(),
                        color = HefimerColors.Black,
                        fontFamily = JetBrainsMonoFamily,
                        fontSize = 8.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(HefimerColors.White)
                            .padding(horizontal = 7.dp, vertical = 2.dp),
                    )
                }
                HefimerTextField(
                    value = accountId,
                    onValueChange = { accountId = it },
                    placeholder = "R2 Account ID",
                    mono = true,
                )
                HefimerTextField(
                    value = accessKeyId,
                    onValueChange = { accessKeyId = it },
                    placeholder = "R2 Access Key ID",
                    mono = true,
                )
                HefimerTextField(
                    value = secretAccessKey,
                    onValueChange = { secretAccessKey = it },
                    placeholder = "R2 Secret Access Key",
                    isPassword = true,
                    mono = true,
                )
                HefimerTextField(
                    value = bucketName,
                    onValueChange = { bucketName = it },
                    placeholder = "R2 Bucket Name",
                    mono = true,
                )
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(44.dp)
                        .clip(CircleShape)
                        .background(HefimerColors.White)
                        .applePressable(enabled = !busy, pressedScale = 0.96f) {
                            onUpdateR2Config(accountId, accessKeyId, secretAccessKey, bucketName)
                        },
                ) {
                    Text(
                        text = tr("Save R2 Credentials", "Lưu 4 thông số R2"),
                        color = HefimerColors.Black,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                    )
                }
            }

            // Logout button
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(44.dp)
                    .clip(CircleShape)
                    .background(HefimerColors.RedBg)
                    .border(1.dp, HefimerColors.RedBorder, CircleShape)
                    .applePressable(pressedScale = 0.96f, onClick = onLogout),
            ) {
                Text(
                    text = tr("Lock & Sign Out Admin", "Khóa & Đăng xuất Admin"),
                    color = HefimerColors.Red400,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                )
            }
            Spacer(Modifier.height(10.dp))
        }
    }
}
