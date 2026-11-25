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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.OpenInNew
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.ContentPasteGo
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Inventory2
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Vibration
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
    defaultStartTab: MainTab,
    defaultProviderId: String,
    promptOpenAfterDownload: Boolean,
    confirmRedownload: Boolean,
    smartClipboardBanner: Boolean,
    hapticEnabled: Boolean,
    historyCount: Int,
    onSetLightMode: (Boolean) -> Unit,
    onSetAppLanguage: (AppLanguage) -> Unit,
    onSetDefaultStartTab: (MainTab) -> Unit,
    onTogglePromptOpenAfterDownload: () -> Unit,
    onToggleConfirmRedownload: () -> Unit,
    onToggleSmartClipboardBanner: () -> Unit,
    onToggleHapticEnabled: () -> Unit,
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
            .verticalScroll(rememberScrollState())
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
                    .appleGlassSurface(
                        shape = CircleShape,
                        variant = AppleGlassVariant.Clear,
                        elevation = 0.dp,
                    )
                    .applePressable(pressedScale = 0.89f, onClick = onClose),
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

        // Default Startup Screen selector
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                text = tr("DEFAULT LAUNCH SCREEN", "MÀN HÌNH MẶC ĐỊNH KHI MỞ APP"),
                color = HefimerColors.White40,
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 2.sp,
            )
            AppleSegmentedControl(
                options = MainTab.entries,
                selected = defaultStartTab,
                label = { it.localizedLabel() },
                onSelect = onSetDefaultStartTab,
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
                    .appleGlassSurface(
                        shape = RoundedCornerShape(24.dp),
                        variant = AppleGlassVariant.Clear,
                        elevation = 0.dp,
                    )
                    .applePressable(pressedScale = 0.975f, onClick = onOpenProviderPicker)
                    .padding(horizontal = 14.dp, vertical = 12.dp),
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(36.dp)
                        .appleGlassSurface(
                            shape = RoundedCornerShape(14.dp),
                            variant = AppleGlassVariant.Clear,
                            elevation = 0.dp,
                        ),
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
                        color = HefimerColors.White45,
                        fontSize = 11.sp,
                    )
                }
                Icon(Icons.Rounded.ChevronRight, null, tint = HefimerColors.White45, modifier = Modifier.size(18.dp))
            }
        }

        // Smart UX & Safety Toggles
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                text = tr("EXPERIENCE & SAFETY", "TRẢI NGHIỆM & AN TOÀN"),
                color = HefimerColors.White40,
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 2.sp,
            )
            HefimerToggleRow(
                title = tr("Open file after saving", "Mở tệp sau khi tải xong"),
                subtitle = tr("Show app chooser sheet right after a download finishes", "Hiện bảng chọn ứng dụng mở ngay sau khi lưu tệp"),
                checked = promptOpenAfterDownload,
                onToggle = onTogglePromptOpenAfterDownload,
                icon = Icons.AutoMirrored.Rounded.OpenInNew,
                activeColor = HefimerColors.White,
            )
            HefimerToggleRow(
                title = tr("Warn on duplicate download", "Hỏi khi tải lại tệp cũ"),
                subtitle = tr("Ask confirmation before downloading the same file twice", "Hiện xác nhận nếu tệp này đã từng được tải trước đó"),
                checked = confirmRedownload,
                onToggle = onToggleConfirmRedownload,
                icon = Icons.Rounded.Refresh,
                activeColor = HefimerColors.White,
            )
            HefimerToggleRow(
                title = tr("Smart clipboard detector", "Tự nhận diện mã trong Clipboard"),
                subtitle = tr("Offer 1-tap open when copying a 5-digit code or link", "Hiện nút mở nhanh khi phát hiện mã 5 số hoặc link Hefimer"),
                checked = smartClipboardBanner,
                onToggle = onToggleSmartClipboardBanner,
                icon = Icons.Rounded.ContentPasteGo,
                activeColor = HefimerColors.White,
            )
            HefimerToggleRow(
                title = tr("Tactile haptics", "Phản hồi xúc giác (Rung nhẹ)"),
                subtitle = tr("Apple-calibrated gentle vibration on buttons and controls", "Rung phản hồi nhẹ khi chạm nút và điều khiển"),
                checked = hapticEnabled,
                onToggle = onToggleHapticEnabled,
                icon = Icons.Rounded.Vibration,
                activeColor = HefimerColors.White,
            )
        }

        // Replay Welcome Guide (Onboarding)
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier
                .fillMaxWidth()
                .appleGlassSurface(
                    shape = RoundedCornerShape(24.dp),
                    variant = AppleGlassVariant.Clear,
                    elevation = 0.dp,
                )
                .applePressable(pressedScale = 0.975f, onClick = onReplayOnboarding)
                .padding(horizontal = 14.dp, vertical = 12.dp),
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(36.dp)
                    .appleGlassSurface(
                        shape = RoundedCornerShape(14.dp),
                        variant = AppleGlassVariant.Clear,
                        elevation = 0.dp,
                    ),
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
                    color = HefimerColors.White45,
                    fontSize = 11.sp,
                )
            }
            Icon(Icons.Rounded.ChevronRight, null, tint = HefimerColors.White45, modifier = Modifier.size(18.dp))
        }

        // Clear local history
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier
                .fillMaxWidth()
                .appleGlassSurface(
                    shape = RoundedCornerShape(24.dp),
                    variant = AppleGlassVariant.Clear,
                    elevation = 0.dp,
                )
                .applePressable(enabled = historyCount > 0, pressedScale = 0.975f, onClick = onClearHistory)
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
                    color = HefimerColors.White45,
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
                text = tr("Hefimer Mobile", "Ứng dụng Hefimer"),
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
