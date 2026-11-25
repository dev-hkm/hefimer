package com.hefimer.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Inventory2
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hefimer.app.BuildConfig
import com.hefimer.app.data.network.UploadProviders
import com.hefimer.app.ui.theme.AppLanguage
import com.hefimer.app.ui.theme.AppleGlassVariant
import com.hefimer.app.ui.theme.AppleSegmentedControl
import com.hefimer.app.ui.theme.HefimerColors
import com.hefimer.app.ui.theme.JetBrainsMonoFamily
import com.hefimer.app.ui.theme.appleGlassSurface
import com.hefimer.app.ui.theme.applePressable
import com.hefimer.app.ui.theme.tr

@Composable
fun SettingsSheetContent(
    isLightMode: Boolean,
    appLanguage: AppLanguage,
    defaultProviderId: String,
    historyCount: Int,
    onSetLightMode: (Boolean) -> Unit,
    onSetAppLanguage: (AppLanguage) -> Unit,
    onOpenProviderPicker: () -> Unit,
    onReplayOnboarding: () -> Unit,
    onClearHistory: () -> Unit,
    onClose: () -> Unit,
) {
    val defaultCfg = UploadProviders.getConfig(defaultProviderId)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .appleGlassSurface(
                shape = RoundedCornerShape(topStart = 36.dp, topEnd = 36.dp),
                variant = AppleGlassVariant.Elevated,
                elevation = 24.dp,
            )
            .navigationBarsPadding()
            .padding(horizontal = 20.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        // Grabber pill
        Box(
            modifier = Modifier
                .align(Alignment.CenterHorizontally)
                .width(38.dp)
                .height(4.dp)
                .clip(CircleShape)
                .background(HefimerColors.White20),
        )

        // Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = tr("PREFERENCES", "TÙY CHỈNH"),
                    color = HefimerColors.White45,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 2.2.sp,
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text = tr("Settings", "Cài đặt"),
                    color = HefimerColors.White,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                )
            }
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(34.dp)
                    .clip(CircleShape)
                    .background(HefimerColors.White06)
                    .border(1.dp, HefimerColors.White10, CircleShape)
                    .applePressable(onClick = onClose),
            ) {
                Icon(Icons.Rounded.Close, tr("Close", "Đóng"), tint = HefimerColors.White60, modifier = Modifier.size(16.dp))
            }
        }

        // Appearance Mode (OLED Dark / Apple Light)
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                text = tr("APPEARANCE", "GIAO DIỆN"),
                color = HefimerColors.White40,
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 2.sp,
            )
            AppleSegmentedControl(
                options = listOf(false, true),
                selected = isLightMode,
                label = { if (it) tr("Light", "Sáng") else tr("Dark", "Tối") },
                onSelect = onSetLightMode,
            )
        }

        // Language selector (Auto / Tiếng Việt / English)
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                text = tr("LANGUAGE", "NGÔN NGỮ"),
                color = HefimerColors.White40,
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 2.sp,
            )
            AppleSegmentedControl(
                options = listOf(AppLanguage.AUTO, AppLanguage.VI, AppLanguage.EN),
                selected = appLanguage,
                label = {
                    when (it) {
                        AppLanguage.AUTO -> tr("Auto", "Tự động")
                        AppLanguage.VI -> "Tiếng Việt"
                        AppLanguage.EN -> "English"
                    }
                },
                onSelect = onSetAppLanguage,
            )
        }

        // Default Upload Provider
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                text = tr("DEFAULT UPLOAD PROVIDER", "MÁY CHỦ TẢI LÊN MẶC ĐỊNH"),
                color = HefimerColors.White40,
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 2.sp,
            )
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(24.dp))
                    .background(HefimerColors.White04)
                    .border(1.dp, HefimerColors.White10, RoundedCornerShape(24.dp))
                    .applePressable(pressedScale = 0.98f, onClick = onOpenProviderPicker)
                    .padding(horizontal = 14.dp, vertical = 12.dp),
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(36.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(HefimerColors.White08)
                        .border(1.dp, HefimerColors.White15, RoundedCornerShape(14.dp)),
                ) {
                    Icon(Icons.Rounded.Inventory2, null, tint = HefimerColors.White, modifier = Modifier.size(18.dp))
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = defaultCfg.name,
                        color = HefimerColors.White,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        text = "${defaultCfg.localizedMaxSize()} • ${tr("Auto-routes larger files", "Tự chuyển tuyến tệp lớn")}",
                        color = HefimerColors.White40,
                        fontSize = 11.sp,
                    )
                }
                Icon(Icons.Rounded.ChevronRight, null, tint = HefimerColors.White40, modifier = Modifier.size(18.dp))
            }
        }

        // Replay Welcome Guide (Onboarding)
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(24.dp))
                .background(HefimerColors.White04)
                .border(1.dp, HefimerColors.White10, RoundedCornerShape(24.dp))
                .applePressable(pressedScale = 0.98f, onClick = onReplayOnboarding)
                .padding(horizontal = 14.dp, vertical = 12.dp),
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(36.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(HefimerColors.White06)
                    .border(1.dp, HefimerColors.White12, RoundedCornerShape(14.dp)),
            ) {
                Icon(Icons.Rounded.AutoAwesome, null, tint = HefimerColors.White, modifier = Modifier.size(18.dp))
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = tr("Welcome Guide", "Hướng dẫn mở đầu"),
                    color = HefimerColors.White,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    text = tr("Replay the 4-step intro tour", "Xem lại 4 màn giới thiệu ứng dụng"),
                    color = HefimerColors.White40,
                    fontSize = 11.sp,
                )
            }
            Icon(Icons.Rounded.ChevronRight, null, tint = HefimerColors.White40, modifier = Modifier.size(18.dp))
        }

        // Clear local history
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(24.dp))
                .background(HefimerColors.White04)
                .border(1.dp, HefimerColors.White10, RoundedCornerShape(24.dp))
                .applePressable(enabled = historyCount > 0, pressedScale = 0.98f, onClick = onClearHistory)
                .padding(horizontal = 14.dp, vertical = 12.dp),
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(36.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(HefimerColors.RedBg)
                    .border(1.dp, HefimerColors.RedBorder, RoundedCornerShape(14.dp)),
            ) {
                Icon(Icons.Rounded.DeleteOutline, null, tint = HefimerColors.Red400, modifier = Modifier.size(18.dp))
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = tr("Clear Recent History", "Xóa lịch sử gần đây"),
                    color = if (historyCount > 0) HefimerColors.White else HefimerColors.White40,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    text = if (historyCount > 0) {
                        tr("$historyCount saved item${if (historyCount == 1) "" else "s"}", "$historyCount mục đã lưu")
                    } else {
                        tr("History is empty", "Lịch sử đang trống")
                    },
                    color = HefimerColors.White40,
                    fontSize = 11.sp,
                )
            }
        }

        // Version footer
        Row(
            modifier = Modifier.fillMaxWidth().padding(bottom = 6.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "Hefimer Native",
                color = HefimerColors.White30,
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium,
            )
            Text(
                text = "v${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})",
                color = HefimerColors.White30,
                fontFamily = JetBrainsMonoFamily,
                fontSize = 11.sp,
            )
        }
    }
}
