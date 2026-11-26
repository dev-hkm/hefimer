package com.hefimer.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import kotlinx.coroutines.launch
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.adamglin.PhosphorIcons
import com.adamglin.phosphoricons.Regular
import com.adamglin.phosphoricons.regular.ArrowSquareOut
import com.adamglin.phosphoricons.regular.ArrowsClockwise
import com.adamglin.phosphoricons.regular.CaretLeft
import com.adamglin.phosphoricons.regular.CaretRight
import com.adamglin.phosphoricons.regular.ClipboardText
import com.adamglin.phosphoricons.regular.CloudArrowUp
import com.adamglin.phosphoricons.regular.Eye
import com.adamglin.phosphoricons.regular.EyeSlash
import com.adamglin.phosphoricons.regular.FileText
import com.adamglin.phosphoricons.regular.Globe
import com.adamglin.phosphoricons.regular.HardDrives
import com.adamglin.phosphoricons.regular.Key
import com.adamglin.phosphoricons.regular.PencilSimple
import com.adamglin.phosphoricons.regular.Plus
import com.adamglin.phosphoricons.regular.Question
import com.adamglin.phosphoricons.regular.ShieldCheck
import com.adamglin.phosphoricons.regular.Sparkle
import com.adamglin.phosphoricons.regular.Trash
import com.adamglin.phosphoricons.regular.Vibrate
import com.hefimer.app.BuildConfig
import com.hefimer.app.data.model.CustomS3Provider
import com.hefimer.app.data.network.UploadProviders
import com.hefimer.app.ui.components.HefimerInfoDoc
import com.hefimer.app.ui.components.HefimerToggleRow
import com.hefimer.app.ui.components.MainTab
import com.hefimer.app.ui.theme.AppLanguage
import com.hefimer.app.ui.theme.AppleGlassVariant
import com.hefimer.app.ui.theme.AppleSegmentedControl
import com.hefimer.app.ui.theme.HefimerColors
import com.hefimer.app.ui.theme.JetBrainsMonoFamily
import com.hefimer.app.ui.theme.appleGlassSurface
import com.hefimer.app.ui.theme.applePressable
import com.hefimer.app.ui.theme.tr

@Composable
fun SettingsScreen(
    themeMode: com.hefimer.app.data.model.ThemeMode,
    appLanguage: AppLanguage,
    defaultStartTab: MainTab,
    defaultProviderId: String,
    storageToApiToken: String,
    customS3Providers: List<CustomS3Provider>,
    promptOpenAfterDownload: Boolean,
    confirmRedownload: Boolean,
    smartClipboardBanner: Boolean,
    hapticEnabled: Boolean,
    historyCount: Int,
    onSetThemeMode: (com.hefimer.app.data.model.ThemeMode) -> Unit,
    onSetAppLanguage: (AppLanguage) -> Unit,
    onSetDefaultStartTab: (MainTab) -> Unit,
    onSetStorageToApiToken: (String) -> Unit,
    onTestCustomS3: suspend (CustomS3Provider) -> Result<CustomS3Provider>,
    onSaveCustomS3: (CustomS3Provider) -> Unit,
    onDeleteCustomS3: (CustomS3Provider) -> Unit,
    onSetDefaultProvider: (String) -> Unit,
    onTogglePromptOpenAfterDownload: () -> Unit,
    onToggleConfirmRedownload: () -> Unit,
    onToggleSmartClipboardBanner: () -> Unit,
    onToggleHapticEnabled: () -> Unit,
    onOpenProviderPicker: () -> Unit,
    onOpenInfoDoc: (HefimerInfoDoc) -> Unit,
    onOpenAdmin: () -> Unit,
    onReplayOnboarding: () -> Unit,
    onClearHistory: () -> Unit,
    onBack: () -> Unit,
) {
    val defaultCfg = UploadProviders.getConfig(defaultProviderId, customS3Providers)
    var editingS3 by remember { mutableStateOf<CustomS3Provider?>(null) }
    var showAddS3Dialog by remember { mutableStateOf(false) }

    Box(modifier = Modifier.fillMaxSize()) {
        com.hefimer.app.ui.theme.AppleMonochromeBackdrop(modifier = Modifier.fillMaxSize())

        com.hefimer.app.ui.components.HefimerCollapsibleScaffold(
            kicker = tr("Preferences", "Tùy chỉnh"),
            title = tr("Settings", "Cài đặt"),
            subtitle = tr(
                "Theme, language, custom R2 / S3 buckets, and app preferences.",
                "Giao diện, ngôn ngữ, kho R2 / S3 riêng và tùy chỉnh ứng dụng.",
            ),
            showOrbitMark = true,
            bottomDockClearance = 36.dp,
            headerActions = {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(40.dp)
                        .appleGlassSurface(shape = CircleShape, variant = AppleGlassVariant.Clear, elevation = 0.dp)
                        .applePressable(pressedScale = 0.90f, onClick = onBack),
                ) {
                    Icon(
                        imageVector = PhosphorIcons.Regular.CaretLeft,
                        contentDescription = tr("Back", "Quay lại"),
                        tint = HefimerColors.White,
                        modifier = Modifier.size(18.dp),
                    )
                }
            },
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(18.dp),
            ) {
            // 1. APPEARANCE, LANGUAGE & STARTUP
            SettingsSection(title = tr("APPEARANCE & STARTUP", "GIAO DIỆN & KHỞI ĐỘNG")) {
                SettingsSubLabel(tr("THEME", "CHỦ ĐỀ"))
                AppleSegmentedControl(
                    options = com.hefimer.app.data.model.ThemeMode.entries,
                    selected = themeMode,
                    label = {
                        when (it) {
                            com.hefimer.app.data.model.ThemeMode.SYSTEM -> tr("Auto", "Tự động")
                            com.hefimer.app.data.model.ThemeMode.DARK -> tr("Dark", "Tối")
                            com.hefimer.app.data.model.ThemeMode.LIGHT -> tr("Light", "Sáng")
                        }
                    },
                    onSelect = onSetThemeMode,
                )

                SettingsSubLabel(tr("LANGUAGE", "NGÔN NGỮ"))
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

                SettingsSubLabel(tr("DEFAULT LAUNCH TAB", "MÀN HÌNH MẶC ĐỊNH KHI MỞ"))
                AppleSegmentedControl(
                    options = MainTab.entries,
                    selected = defaultStartTab,
                    label = { it.localizedLabel() },
                    onSelect = onSetDefaultStartTab,
                )
            }

            // 2. UPLOAD ROUTING & CUSTOM R2 / S3
            SettingsSection(title = tr("STORAGE & UPLOAD ROUTING", "LƯU TRỮ & MÁY CHỦ TẢI LÊN")) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .appleGlassSurface(shape = RoundedCornerShape(24.dp), variant = AppleGlassVariant.Clear, elevation = 0.dp)
                        .applePressable(pressedScale = 0.975f, onClick = onOpenProviderPicker)
                        .padding(horizontal = 14.dp, vertical = 12.dp),
                ) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(38.dp)
                            .appleGlassSurface(shape = RoundedCornerShape(14.dp), variant = AppleGlassVariant.Clear, elevation = 0.dp),
                    ) {
                        Icon(PhosphorIcons.Regular.CloudArrowUp, null, tint = HefimerColors.White, modifier = Modifier.size(18.dp))
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = tr("Default Upload Host", "Máy chủ tải lên mặc định"),
                            color = HefimerColors.White45,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                        )
                        Text(
                            text = "${defaultCfg.name} • ${defaultCfg.localizedMaxSize()}",
                            color = HefimerColors.White,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                    Icon(PhosphorIcons.Regular.CaretRight, null, tint = HefimerColors.White45, modifier = Modifier.size(16.dp))
                }

                // storage.to Free API Token card
                var tokenDraft by remember(storageToApiToken) { mutableStateOf(storageToApiToken) }
                var tokenVisible by remember { mutableStateOf(false) }
                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .appleGlassSurface(shape = RoundedCornerShape(24.dp), variant = AppleGlassVariant.Clear, elevation = 0.dp)
                        .padding(14.dp),
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(PhosphorIcons.Regular.Key, null, tint = HefimerColors.White85, modifier = Modifier.size(16.dp))
                        Text(
                            text = tr("storage.to API Token (Optional)", "API Token storage.to (Tùy chọn)"),
                            color = HefimerColors.White,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.weight(1f),
                        )
                        if (storageToApiToken.isNotBlank()) {
                            Text(
                                text = tr("ACTIVE", "ĐANG BẬT"),
                                color = HefimerColors.Black,
                                fontSize = 8.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.sp,
                                modifier = Modifier
                                    .clip(CircleShape)
                                    .background(HefimerColors.White)
                                    .padding(horizontal = 7.dp, vertical = 2.dp),
                            )
                        }
                    }
                    Text(
                        text = tr(
                            "Free token from storage.to/account/api-tokens unlocks unlimited daily files & bandwidth.",
                            "Token miễn phí từ storage.to/account/api-tokens mở khóa không giới hạn số tệp & băng thông/ngày.",
                        ),
                        color = HefimerColors.White45,
                        fontSize = 11.sp,
                        lineHeight = 15.sp,
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(14.dp))
                                .background(HefimerColors.White08)
                                .border(1.dp, HefimerColors.White12, RoundedCornerShape(14.dp))
                                .padding(horizontal = 12.dp, vertical = 10.dp),
                        ) {
                            Box(modifier = Modifier.weight(1f)) {
                                if (tokenDraft.isEmpty()) {
                                    Text(
                                        text = tr("Paste Bearer token...", "Dán Bearer token..."),
                                        color = HefimerColors.White30,
                                        fontFamily = JetBrainsMonoFamily,
                                        fontSize = 11.sp,
                                    )
                                }
                                BasicTextField(
                                    value = tokenDraft,
                                    onValueChange = { tokenDraft = it },
                                    singleLine = true,
                                    visualTransformation = if (tokenVisible) VisualTransformation.None else PasswordVisualTransformation(),
                                    textStyle = TextStyle(color = HefimerColors.White, fontFamily = JetBrainsMonoFamily, fontSize = 11.sp),
                                    cursorBrush = SolidColor(HefimerColors.White),
                                    modifier = Modifier.fillMaxWidth(),
                                )
                            }
                            Spacer(Modifier.width(6.dp))
                            Icon(
                                imageVector = if (tokenVisible) PhosphorIcons.Regular.EyeSlash else PhosphorIcons.Regular.Eye,
                                contentDescription = tr("Toggle visibility", "Ẩn/hiện"),
                                tint = HefimerColors.White60,
                                modifier = Modifier
                                    .size(16.dp)
                                    .clip(CircleShape)
                                    .applePressable(pressedScale = 0.88f) { tokenVisible = !tokenVisible },
                            )
                        }
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .clip(RoundedCornerShape(14.dp))
                                .background(if (tokenDraft.trim() != storageToApiToken) HefimerColors.White else HefimerColors.White12)
                                .applePressable(pressedScale = 0.94f) { onSetStorageToApiToken(tokenDraft) }
                                .padding(horizontal = 14.dp, vertical = 10.dp),
                        ) {
                            Text(
                                text = if (tokenDraft.isBlank() && storageToApiToken.isNotBlank()) tr("Clear", "Xóa") else tr("Save", "Lưu"),
                                color = if (tokenDraft.trim() != storageToApiToken) HefimerColors.Black else HefimerColors.White70,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                            )
                        }
                    }
                }

                // Custom User R2 / S3 Buckets Manager
                Column(
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .appleGlassSurface(shape = RoundedCornerShape(24.dp), variant = AppleGlassVariant.Clear, elevation = 0.dp)
                        .padding(14.dp),
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(PhosphorIcons.Regular.HardDrives, null, tint = HefimerColors.White85, modifier = Modifier.size(16.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = tr("Custom R2 / S3 Storage", "Kho lưu trữ R2 / S3 riêng"),
                                color = HefimerColors.White,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                            )
                            Text(
                                text = tr(
                                    "Connect your own Cloudflare R2, AWS S3, Backblaze B2, or MinIO buckets on-device.",
                                    "Tự thêm nhiều bucket Cloudflare R2, AWS S3, Backblaze B2 hoặc MinIO ngay trên máy.",
                                ),
                                color = HefimerColors.White45,
                                fontSize = 10.sp,
                                lineHeight = 14.sp,
                            )
                        }
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .clip(CircleShape)
                                .background(HefimerColors.White)
                                .applePressable(pressedScale = 0.94f) {
                                    editingS3 = null
                                    showAddS3Dialog = true
                                }
                                .padding(horizontal = 10.dp, vertical = 6.dp),
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                Icon(PhosphorIcons.Regular.Plus, null, tint = HefimerColors.Black, modifier = Modifier.size(13.dp))
                                Text(tr("Add", "Thêm"), color = HefimerColors.Black, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    if (customS3Providers.isEmpty()) {
                        Text(
                            text = tr("No custom buckets added yet.", "Chưa có máy chủ R2 / S3 riêng nào."),
                            color = HefimerColors.White30,
                            fontSize = 11.sp,
                        )
                    } else {
                        customS3Providers.forEach { item ->
                            val isDef = item.id == defaultProviderId
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(HefimerColors.White06)
                                    .border(1.dp, if (isDef) HefimerColors.White30 else HefimerColors.White10, RoundedCornerShape(16.dp))
                                    .padding(horizontal = 12.dp, vertical = 10.dp),
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                        Text(item.name, color = HefimerColors.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                        if (isDef) {
                                            Text(
                                                text = tr("DEFAULT", "MẶC ĐỊNH"),
                                                color = HefimerColors.Black,
                                                fontSize = 8.sp,
                                                fontWeight = FontWeight.Bold,
                                                modifier = Modifier
                                                    .clip(CircleShape)
                                                    .background(HefimerColors.White)
                                                    .padding(horizontal = 6.dp, vertical = 1.dp),
                                            )
                                        }
                                    }
                                    Text(
                                        text = "${item.bucket} • ${item.region}",
                                        color = HefimerColors.White45,
                                        fontFamily = JetBrainsMonoFamily,
                                        fontSize = 10.sp,
                                    )
                                }
                                if (!isDef) {
                                    Box(
                                        modifier = Modifier
                                            .clip(CircleShape)
                                            .background(HefimerColors.White10)
                                            .applePressable(pressedScale = 0.92f) { onSetDefaultProvider(item.id) }
                                            .padding(horizontal = 8.dp, vertical = 5.dp),
                                    ) {
                                        Text(tr("Default", "Mặc định"), color = HefimerColors.White85, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                                Box(
                                    contentAlignment = Alignment.Center,
                                    modifier = Modifier
                                        .size(30.dp)
                                        .clip(CircleShape)
                                        .background(HefimerColors.White08)
                                        .applePressable(pressedScale = 0.90f) {
                                            editingS3 = item
                                            showAddS3Dialog = true
                                        },
                                ) {
                                    Icon(PhosphorIcons.Regular.PencilSimple, null, tint = HefimerColors.White80, modifier = Modifier.size(14.dp))
                                }
                                Box(
                                    contentAlignment = Alignment.Center,
                                    modifier = Modifier
                                        .size(30.dp)
                                        .clip(CircleShape)
                                        .background(HefimerColors.RedBg)
                                        .applePressable(pressedScale = 0.90f) { onDeleteCustomS3(item) },
                                ) {
                                    Icon(PhosphorIcons.Regular.Trash, null, tint = HefimerColors.Red400, modifier = Modifier.size(14.dp))
                                }
                            }
                        }
                    }
                }
            }

            // 3. EXPERIENCE & SAFETY
            SettingsSection(title = tr("EXPERIENCE & SAFETY", "TRẢI NGHIỆM & AN TOÀN")) {
                HefimerToggleRow(
                    title = tr("Open file after saving", "Mở tệp sau khi tải xong"),
                    subtitle = tr("Show app chooser sheet right after a download finishes", "Hiện bảng chọn ứng dụng mở ngay sau khi lưu tệp"),
                    checked = promptOpenAfterDownload,
                    onToggle = onTogglePromptOpenAfterDownload,
                    icon = PhosphorIcons.Regular.ArrowSquareOut,
                    activeColor = HefimerColors.White,
                )
                HefimerToggleRow(
                    title = tr("Warn on duplicate download", "Hỏi khi tải lại tệp cũ"),
                    subtitle = tr("Ask confirmation before downloading the same file twice", "Hiện xác nhận nếu tệp này đã từng được tải trước đó"),
                    checked = confirmRedownload,
                    onToggle = onToggleConfirmRedownload,
                    icon = PhosphorIcons.Regular.ArrowsClockwise,
                    activeColor = HefimerColors.White,
                )
                HefimerToggleRow(
                    title = tr("Smart clipboard detector", "Tự nhận diện mã trong Clipboard"),
                    subtitle = tr("Offer 1-tap open when copying a 5-digit code or link", "Hiện nút mở nhanh khi phát hiện mã 5 số hoặc link Hefimer"),
                    checked = smartClipboardBanner,
                    onToggle = onToggleSmartClipboardBanner,
                    icon = PhosphorIcons.Regular.ClipboardText,
                    activeColor = HefimerColors.White,
                )
                HefimerToggleRow(
                    title = tr("Tactile haptics", "Phản hồi xúc giác (Rung nhẹ)"),
                    subtitle = tr("Apple-calibrated gentle vibration on buttons and controls", "Rung phản hồi nhẹ khi chạm nút và điều khiển"),
                    checked = hapticEnabled,
                    onToggle = onToggleHapticEnabled,
                    icon = PhosphorIcons.Regular.Vibrate,
                    activeColor = HefimerColors.White,
                )
            }

            // 4. HELP, LEGAL & DATA
            SettingsSection(title = tr("HELP, LEGAL & DATA", "HỖ TRỢ, PHÁP LÝ & DỮ LIỆU")) {
                SettingsRowButton(
                    icon = PhosphorIcons.Regular.Question,
                    title = tr("FAQ & How It Works", "Câu hỏi thường gặp (FAQ)"),
                    subtitle = tr("Codes, expiration, One-Time mode & Hefimer Link", "Cách gửi nhận, thời hạn, xem 1 lần & Hefimer Link"),
                    onClick = { onOpenInfoDoc(HefimerInfoDoc.FAQ) },
                )
                SettingsRowButton(
                    icon = PhosphorIcons.Regular.ShieldCheck,
                    title = tr("Privacy & Security", "Bảo mật & Quyền riêng tư"),
                    subtitle = tr("Zero account, HTTPS/TLS, auto-deletion & data policy", "Không tài khoản, HTTPS/TLS, tự hủy & dữ liệu"),
                    onClick = { onOpenInfoDoc(HefimerInfoDoc.PRIVACY) },
                )
                SettingsRowButton(
                    icon = PhosphorIcons.Regular.FileText,
                    title = tr("Terms of Service", "Điều khoản Dịch vụ"),
                    subtitle = tr("Usage rules, third-party hosts & responsibilities", "Quy định sử dụng, máy chủ lưu trữ & trách nhiệm"),
                    onClick = { onOpenInfoDoc(HefimerInfoDoc.TERMS) },
                )
                SettingsRowButton(
                    icon = PhosphorIcons.Regular.Sparkle,
                    title = tr("Welcome Guide", "Hướng dẫn mở đầu"),
                    subtitle = tr("Replay the 4-step intro tour", "Xem lại 4 màn giới thiệu ứng dụng"),
                    onClick = onReplayOnboarding,
                )

                // Clear local history
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .appleGlassSurface(shape = RoundedCornerShape(24.dp), variant = AppleGlassVariant.Clear, elevation = 0.dp)
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
                        Icon(PhosphorIcons.Regular.Trash, null, tint = HefimerColors.Red400, modifier = Modifier.size(18.dp))
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

                // Compact PC / Web availability link
                val webContext = androidx.compose.ui.platform.LocalContext.current
                SettingsRowButton(
                    icon = PhosphorIcons.Regular.Globe,
                    title = tr("Available on PC & Web", "Có sẵn trên PC & Trình duyệt"),
                    subtitle = "hefimer.qzz.io",
                    onClick = {
                        runCatching {
                            webContext.startActivity(
                                android.content.Intent(
                                    android.content.Intent.ACTION_VIEW,
                                    android.net.Uri.parse(com.hefimer.app.data.network.HefimerApiConfig.SITE_ORIGIN),
                                ),
                            )
                        }
                    },
                )
            }

            // Version footer (discreet Admin trigger)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .applePressable(pressedScale = 0.97f, onClick = onOpenAdmin)
                    .padding(vertical = 8.dp, horizontal = 6.dp),
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
            Spacer(Modifier.height(16.dp))
        }
        }
    }

    if (showAddS3Dialog) {
        CustomS3EditorDialog(
            initial = editingS3,
            onDismiss = { showAddS3Dialog = false },
            onTestConnection = onTestCustomS3,
            onSave = { prov ->
                onSaveCustomS3(prov)
                showAddS3Dialog = false
            },
        )
    }
}

@Composable
private fun SettingsSection(
    title: String,
    content: @Composable androidx.compose.foundation.layout.ColumnScope.() -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = title,
            color = HefimerColors.White45,
            fontSize = 9.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 2.sp,
            modifier = Modifier.padding(start = 4.dp),
        )
        content()
    }
}

@Composable
private fun SettingsSubLabel(text: String) {
    Text(
        text = text,
        color = HefimerColors.White40,
        fontSize = 9.sp,
        fontWeight = FontWeight.Bold,
        letterSpacing = 1.6.sp,
        modifier = Modifier.padding(start = 4.dp, top = 2.dp),
    )
}

@Composable
private fun SettingsRowButton(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier
            .fillMaxWidth()
            .appleGlassSurface(shape = RoundedCornerShape(24.dp), variant = AppleGlassVariant.Clear, elevation = 0.dp)
            .applePressable(pressedScale = 0.975f, onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 12.dp),
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(36.dp)
                .appleGlassSurface(shape = RoundedCornerShape(14.dp), variant = AppleGlassVariant.Clear, elevation = 0.dp),
        ) {
            Icon(icon, null, tint = HefimerColors.White, modifier = Modifier.size(18.dp))
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, color = HefimerColors.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
            Text(text = subtitle, color = HefimerColors.White45, fontSize = 11.sp)
        }
        Icon(PhosphorIcons.Regular.CaretRight, null, tint = HefimerColors.White45, modifier = Modifier.size(16.dp))
    }
}

@Composable
private fun CustomS3EditorDialog(
    initial: CustomS3Provider?,
    onDismiss: () -> Unit,
    onTestConnection: suspend (CustomS3Provider) -> Result<CustomS3Provider>,
    onSave: (CustomS3Provider) -> Unit,
) {
    val scope = rememberCoroutineScope()
    var name by remember { mutableStateOf(initial?.name ?: "") }
    var endpoint by remember { mutableStateOf(initial?.endpoint ?: "") }
    var region by remember { mutableStateOf(initial?.region ?: "auto") }
    var bucket by remember { mutableStateOf(initial?.bucket ?: "") }
    var accessKeyId by remember { mutableStateOf(initial?.accessKeyId ?: "") }
    var secretAccessKey by remember { mutableStateOf(initial?.secretAccessKey ?: "") }
    var publicBaseUrl by remember { mutableStateOf(initial?.publicBaseUrl ?: "") }
    var isTesting by remember { mutableStateOf(false) }
    var testError by remember { mutableStateOf<String?>(null) }

    val canSave = !isTesting && endpoint.isNotBlank() && bucket.isNotBlank() && accessKeyId.isNotBlank() && secretAccessKey.isNotBlank()

    com.hefimer.app.ui.theme.AppleAnimatedDialog(
        onDismissRequest = onDismiss,
        dismissEnabled = !isTesting,
    ) { close ->
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .appleGlassSurface(shape = RoundedCornerShape(30.dp), variant = AppleGlassVariant.Elevated, elevation = 24.dp)
                .padding(20.dp),
        ) {
            Column(
                modifier = Modifier
                    .verticalScroll(rememberScrollState())
                    .let {
                        @Suppress("Unused")
                        it
                    },
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Text(
                    text = if (initial == null) {
                        tr("Add R2 / S3 Bucket", "Thêm kho R2 / S3")
                    } else {
                        tr("Edit R2 / S3 Bucket", "Sửa kho R2 / S3")
                    },
                    color = HefimerColors.White,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                )
                HefimerTextField(
                    value = name,
                    onValueChange = { name = it; testError = null },
                    placeholder = tr("Display name (e.g. My R2)", "Tên hiển thị (vd: R2 của tôi)"),
                )
                HefimerTextField(
                    value = endpoint,
                    onValueChange = { endpoint = it; testError = null },
                    placeholder = "https://<account>.r2.cloudflarestorage.com",
                    mono = true,
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    HefimerTextField(
                        value = region,
                        onValueChange = { region = it; testError = null },
                        placeholder = "auto / us-east-1",
                        mono = true,
                        modifier = Modifier.weight(1f),
                    )
                    HefimerTextField(
                        value = bucket,
                        onValueChange = { bucket = it; testError = null },
                        placeholder = tr("Bucket name", "Tên bucket"),
                        mono = true,
                        modifier = Modifier.weight(1.3f),
                    )
                }
                HefimerTextField(
                    value = accessKeyId,
                    onValueChange = { accessKeyId = it; testError = null },
                    placeholder = "Access Key ID",
                    mono = true,
                )
                HefimerTextField(
                    value = secretAccessKey,
                    onValueChange = { secretAccessKey = it; testError = null },
                    placeholder = "Secret Access Key",
                    isPassword = true,
                    mono = true,
                )
                HefimerTextField(
                    value = publicBaseUrl,
                    onValueChange = { publicBaseUrl = it; testError = null },
                    placeholder = tr("Public CDN URL (optional)", "Tên miền CDN công khai (tùy chọn)"),
                    mono = true,
                )
                if (!testError.isNullOrBlank()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(HefimerColors.Rose400.copy(alpha = 0.14f))
                            .border(1.dp, HefimerColors.Rose400.copy(alpha = 0.35f), RoundedCornerShape(14.dp))
                            .padding(horizontal = 12.dp, vertical = 9.dp),
                    ) {
                        Text(
                            text = testError.orEmpty(),
                            color = HefimerColors.Rose400,
                            fontSize = 11.5.sp,
                            lineHeight = 16.sp,
                        )
                    }
                }
                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .weight(1f)
                            .height(44.dp)
                            .appleGlassSurface(shape = CircleShape, variant = AppleGlassVariant.Clear, elevation = 0.dp)
                            .applePressable(enabled = !isTesting, pressedScale = 0.96f, onClick = close),
                    ) {
                        Text(tr("Cancel", "Hủy"), color = HefimerColors.White60, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    }
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .weight(1.25f)
                            .height(44.dp)
                            .clip(CircleShape)
                            .background(if (canSave) HefimerColors.White else HefimerColors.White20)
                            .applePressable(enabled = canSave, pressedScale = 0.96f) {
                                val draft = CustomS3Provider(
                                    id = initial?.id.orEmpty(),
                                    name = name.ifBlank { bucket },
                                    endpoint = endpoint,
                                    region = region.ifBlank { "auto" },
                                    bucket = bucket,
                                    accessKeyId = accessKeyId,
                                    secretAccessKey = secretAccessKey,
                                    publicBaseUrl = publicBaseUrl,
                                )
                                isTesting = true
                                testError = null
                                scope.launch {
                                    val result = onTestConnection(draft)
                                    isTesting = false
                                    result.onSuccess { verified ->
                                        onSave(verified)
                                    }.onFailure { err ->
                                        testError = err.message ?: tr("Connection test failed", "Kiểm tra kết nối thất bại")
                                    }
                                }
                            },
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                        ) {
                            if (isTesting) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(14.dp),
                                    strokeWidth = 2.dp,
                                    color = HefimerColors.White70,
                                )
                            }
                            Text(
                                text = if (isTesting) {
                                    tr("Testing...", "Đang kiểm tra...")
                                } else {
                                    tr("Test & Save", "Kiểm tra & Lưu")
                                },
                                color = if (canSave) HefimerColors.Black else HefimerColors.White45,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                            )
                        }
                    }
                }
            }
        }
    }
}
