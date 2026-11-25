package com.hefimer.app

import android.content.Intent
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
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
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AcUnit
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.ContentPasteGo
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material.icons.rounded.WifiOff
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import com.hefimer.app.data.model.ToastType
import com.hefimer.app.data.network.DownloadHelper
import com.hefimer.app.data.network.UploadProviders
import com.hefimer.app.ui.AppOverlay
import com.hefimer.app.ui.HefimerViewModel
import com.hefimer.app.ui.components.AdminDashboardSheetContent
import com.hefimer.app.ui.components.AdminLoginDialog
import com.hefimer.app.ui.components.FloatingDownloadProgressBanner
import com.hefimer.app.ui.components.HefimerConfirmDialog
import com.hefimer.app.ui.components.HefimerFloatingBottomDock
import com.hefimer.app.ui.components.HefimerInfoDoc
import com.hefimer.app.ui.components.HefimerInfoDocSheetContent
import com.hefimer.app.ui.components.HefimerLaunchIntro
import com.hefimer.app.ui.components.HefimerOnboardingOverlay
import com.hefimer.app.ui.components.HefimerPrimaryButton
import com.hefimer.app.ui.components.HefimerToastHost
import com.hefimer.app.ui.components.MainTab
import com.hefimer.app.ui.components.PostSaveBottomSheetContent
import com.hefimer.app.ui.components.QrScannerOverlay
import com.hefimer.app.ui.components.RedownloadConfirmDialog
import com.hefimer.app.ui.screens.ChatRoomOverlay
import com.hefimer.app.ui.screens.FloatingIncomingOfferBanner
import com.hefimer.app.ui.screens.HistoryOverlayScreen
import com.hefimer.app.ui.screens.LinkScreen
import com.hefimer.app.ui.screens.ProviderPickerSheetContent
import com.hefimer.app.ui.screens.ReceiveScreen
import com.hefimer.app.ui.screens.RoomsScreen
import com.hefimer.app.ui.screens.SendScreen
import com.hefimer.app.ui.screens.SettingsScreen
import com.hefimer.app.ui.screens.SpaceCanvasOverlay
import com.hefimer.app.ui.screens.SpaceScreen
import com.hefimer.app.ui.screens.WhiteboardOverlay
import com.hefimer.app.ui.theme.AppleGlassVariant
import com.hefimer.app.ui.theme.AppleLayeredNavigationHost
import com.hefimer.app.ui.theme.AppleMonochromeBackdrop
import com.hefimer.app.ui.theme.AppleSwipeableSheet
import com.hefimer.app.ui.theme.HefimerColors
import com.hefimer.app.ui.theme.HefimerHaptics
import com.hefimer.app.ui.theme.HefimerTheme
import com.hefimer.app.ui.theme.JetBrainsMonoFamily
import com.hefimer.app.ui.theme.HeroBoundsRegistry
import com.hefimer.app.ui.theme.LocalHeroBounds
import com.hefimer.app.ui.theme.appleGlassSurface
import com.hefimer.app.ui.theme.applePressable
import com.hefimer.app.ui.theme.tr

class MainActivity : ComponentActivity() {
    private val viewModel: HefimerViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        // True edge-to-edge transparency for both status bar and gesture/button navigation bar
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
        )
        super.onCreate(savedInstanceState)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            splashScreen.setOnExitAnimationListener { it.remove() }
        }

        WindowCompat.setDecorFitsSystemWindows(window, false)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            window.isNavigationBarContrastEnforced = false
            @Suppress("DEPRECATION")
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.VANILLA_ICE_CREAM) {
                window.isStatusBarContrastEnforced = false
            }
        }

        viewModel.handleIncomingIntent(intent)

        setContent {
            val isLightMode by viewModel.isLightMode.collectAsState()
            LaunchedEffect(isLightMode) {
                if (isLightMode) {
                    enableEdgeToEdge(
                        statusBarStyle = SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT),
                        navigationBarStyle = SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT),
                    )
                } else {
                    enableEdgeToEdge(
                        statusBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
                        navigationBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
                    )
                }
            }
            HefimerTheme(isLightMode = isLightMode) {
                HefimerRootScreen(
                    vm = viewModel,
                    onExitApp = { finish() },
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        viewModel.handleIncomingIntent(intent)
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) {
            viewModel.checkClipboardForHefimer()
        }
    }
}

@Composable
private fun HefimerRootScreen(
    vm: HefimerViewModel,
    onExitApp: () -> Unit,
) {
    val context = LocalContext.current
    val selectedTab by vm.selectedTab.collectAsState()
    val defaultStartTab by vm.defaultStartTab.collectAsState()
    val isOnline by vm.isOnline.collectAsState()
    val activeOverlay by vm.activeOverlay.collectAsState()
    val activeHeroKey by vm.activeHeroKey.collectAsState()
    val toast by vm.toast.collectAsState()
    val sendState by vm.sendState.collectAsState()
    val linkState by vm.linkState.collectAsState()
    val downloadState by vm.downloadState.collectAsState()
    val globalStats by vm.globalStats.collectAsState()
    val noticeDismissed by vm.noticeDismissed.collectAsState()
    val isLightMode by vm.isLightMode.collectAsState()
    val appLanguage by vm.appLanguage.collectAsState()
    val defaultProviderId by vm.defaultProviderId.collectAsState()
    val storageToApiToken by vm.storageToApiToken.collectAsState()
    val customS3Providers by vm.customS3Providers.collectAsState()
    val showSettingsSheet by vm.showSettingsSheet.collectAsState()
    val showOnboarding by vm.showOnboarding.collectAsState()
    val promptOpenAfterDownload by vm.promptOpenAfterDownload.collectAsState()
    val confirmRedownload by vm.confirmRedownload.collectAsState()
    val smartClipboardBanner by vm.smartClipboardBanner.collectAsState()
    val hapticEnabled by vm.hapticEnabled.collectAsState()
    val confirmDialog by vm.confirmDialog.collectAsState()
    val pendingRedownload by vm.pendingRedownload.collectAsState()
    val savedFilePrompt by vm.savedFilePrompt.collectAsState()
    val clipboardSuggestion by vm.clipboardSuggestion.collectAsState()
    val history by vm.history.collectAsState()
    val adminState by vm.adminState.collectAsState()
    var activeInfoDoc by remember { mutableStateOf<HefimerInfoDoc?>(null) }

    LaunchedEffect(hapticEnabled) {
        HefimerHaptics.enabled = hapticEnabled
    }

    val heroRegistry = remember { HeroBoundsRegistry() }
    val navBottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
    val statusTop = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()

    val linkPending = linkState.sync.incoming.filter { it.recipientStatus == "pending" }
    val linkReady = linkState.sync.incoming.filter { it.recipientStatus in setOf("approved", "downloading") }
    val linkBadgeCount = linkPending.size + linkReady.size
    val floatingOffer = if (selectedTab != MainTab.LINK && activeOverlay == null) {
        linkPending.firstOrNull { it.id !in linkState.dismissedPromptIds }
    } else {
        null
    }

    val isNoticeVisible = globalStats.noticeEnabled &&
        globalStats.notice.isNotBlank() &&
        !noticeDismissed &&
        (globalStats.noticeExpiresAt == 0L || System.currentTimeMillis() < globalStats.noticeExpiresAt) &&
        activeOverlay == null

    CompositionLocalProvider(LocalHeroBounds provides heroRegistry) {
        Box(modifier = Modifier.fillMaxSize()) {
            AppleMonochromeBackdrop(
                isR2Mode = false,
            )
            AppleLayeredNavigationHost(
                currentTab = selectedTab,
                overlay = activeOverlay,
                heroKey = activeHeroKey,
                onDismissOverlay = { vm.dismissOverlay() },
                onRootBack = {
                    if (!vm.handleBackNavigation()) {
                        onExitApp()
                    }
                },
                isFastDismissOverlay = { it is AppOverlay.QrScanner },
                bottomChrome = { overlayReveal ->
                    Box(modifier = Modifier.fillMaxSize()) {
                        // Global floating incoming Hefimer Link offer banner
                        FloatingIncomingOfferBanner(
                            transfer = floatingOffer,
                            onApprove = { vm.decideLinkTransfer(it, "approve") },
                            onDecline = {
                                vm.dismissIncomingPrompt(it.id)
                                vm.decideLinkTransfer(it, "decline")
                            },
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .padding(bottom = navBottom + if (downloadState != null) 166.dp else 78.dp),
                        )

                        // Floating curved-notch Bottom Navigation Dock
                        HefimerFloatingBottomDock(
                            selectedTab = selectedTab,
                            onSelectTab = { vm.selectTab(it) },
                            linkBadgeCount = linkBadgeCount,
                            linkGroupActive = linkState.sync.group != null,
                            isR2Mode = false,
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .graphicsLayer {
                                    alpha = 1f - overlayReveal
                                    translationY = overlayReveal * 40f
                                },
                        )
                    }
                },
                renderTab = { tab ->
                    when (tab) {
                        MainTab.SEND -> SendScreen(vm)
                        MainTab.RECEIVE -> ReceiveScreen(vm)
                        MainTab.SPACE -> SpaceScreen(vm)
                        MainTab.ROOMS -> RoomsScreen(vm)
                        MainTab.LINK -> LinkScreen(vm)
                    }
                },
                renderOverlay = { overlay, dismiss ->
                    when (overlay) {
                        AppOverlay.History -> HistoryOverlayScreen(vm = vm, onClose = dismiss)
                        AppOverlay.Settings -> SettingsScreen(
                            isLightMode = isLightMode,
                            appLanguage = appLanguage,
                            defaultStartTab = defaultStartTab,
                            defaultProviderId = defaultProviderId,
                            storageToApiToken = storageToApiToken,
                            customS3Providers = customS3Providers,
                            promptOpenAfterDownload = promptOpenAfterDownload,
                            confirmRedownload = confirmRedownload,
                            smartClipboardBanner = smartClipboardBanner,
                            hapticEnabled = hapticEnabled,
                            historyCount = history.size,
                            onSetLightMode = { vm.setLightMode(it) },
                            onSetAppLanguage = { vm.setAppLanguage(it) },
                            onSetDefaultStartTab = { vm.setDefaultStartTab(it) },
                            onSetStorageToApiToken = { vm.setStorageToApiToken(it) },
                            onTestCustomS3 = { vm.testCustomS3Connection(it) },
                            onSaveCustomS3 = { vm.saveCustomS3Provider(it) },
                            onDeleteCustomS3 = { item ->
                                vm.requestConfirm(
                                    title = tr("Delete ${item.name}?", "Xóa ${item.name}?"),
                                    message = tr(
                                        "This custom S3/R2 bucket configuration will be removed from this device.",
                                        "Cấu hình máy chủ S3/R2 riêng này sẽ bị xóa khỏi thiết bị.",
                                    ),
                                    confirmLabel = tr("Delete", "Xóa"),
                                    isDestructive = true,
                                ) {
                                    vm.deleteCustomS3Provider(item.id)
                                }
                            },
                            onSetDefaultProvider = { vm.setDefaultUploadProvider(it) },
                            onTogglePromptOpenAfterDownload = { vm.togglePromptOpenAfterDownload() },
                            onToggleConfirmRedownload = { vm.toggleConfirmRedownload() },
                            onToggleSmartClipboardBanner = { vm.toggleSmartClipboardBanner() },
                            onToggleHapticEnabled = { vm.toggleHapticEnabled() },
                            onOpenProviderPicker = { vm.openSendProviderPicker() },
                            onOpenInfoDoc = { doc -> activeInfoDoc = doc },
                            onOpenAdmin = { vm.openAdminEntry() },
                            onReplayOnboarding = {
                                dismiss()
                                vm.replayOnboarding()
                            },
                            onClearHistory = {
                                vm.requestConfirm(
                                    title = tr("Clear All History?", "Xóa toàn bộ lịch sử?"),
                                    message = tr(
                                        "All saved transfer codes and shortcuts will be removed from this device.",
                                        "Tất cả mã chia sẻ và lịch sử đã lưu trên thiết bị này sẽ bị xóa.",
                                    ),
                                    confirmLabel = tr("Clear History", "Xóa lịch sử"),
                                    isDestructive = true,
                                ) {
                                    vm.clearHistory()
                                }
                            },
                            onBack = dismiss,
                        )
                        is AppOverlay.QrScanner -> QrScannerOverlay(
                            onResult = { code -> vm.handleScannedOrDeepLink(code, overlay.target) },
                            onClose = dismiss,
                        )
                        is AppOverlay.SpaceCanvas -> SpaceCanvasOverlay(vm = vm, onClose = dismiss)
                        is AppOverlay.ChatRoom -> ChatRoomOverlay(vm = vm, onClose = dismiss)
                        is AppOverlay.Whiteboard -> WhiteboardOverlay(vm = vm, onClose = dismiss)
                    }
                },
            )

            // Live native download progress banner (visible across both tabs and overlays)
            FloatingDownloadProgressBanner(
                state = downloadState,
                onCancel = { vm.cancelActiveDownload() },
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = navBottom + if (activeOverlay == null) 78.dp else 20.dp),
            )

            // Smart Clipboard Suggestion Banner (detects Hefimer codes/links/tokens automatically)
            AnimatedVisibility(
                visible = clipboardSuggestion != null && activeOverlay == null && !showOnboarding,
                enter = fadeIn(),
                exit = fadeOut(),
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(
                        bottom = navBottom + if (downloadState != null || floatingOffer != null) 166.dp else 78.dp,
                        start = 16.dp,
                        end = 16.dp,
                    ),
            ) {
                clipboardSuggestion?.let { suggestion ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .appleGlassSurface(
                                shape = RoundedCornerShape(24.dp),
                                variant = AppleGlassVariant.Elevated,
                                elevation = 16.dp,
                            )
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                    ) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .size(34.dp)
                                .clip(CircleShape)
                                .background(HefimerColors.White10),
                        ) {
                            Icon(Icons.Rounded.ContentPasteGo, null, tint = HefimerColors.White, modifier = Modifier.size(17.dp))
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = suggestion.kindLabel,
                                color = HefimerColors.White45,
                                fontFamily = JetBrainsMonoFamily,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.4.sp,
                            )
                            Text(
                                text = suggestion.displayLabel,
                                color = HefimerColors.White90,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .clip(CircleShape)
                                .background(HefimerColors.White)
                                .applePressable(pressedScale = 0.90f) { vm.acceptClipboardSuggestion() }
                                .padding(horizontal = 12.dp, vertical = 7.dp),
                        ) {
                            Text(tr("Open", "Mở"), color = HefimerColors.Black, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                        Icon(
                            imageVector = Icons.Rounded.Close,
                            contentDescription = tr("Dismiss", "Đóng"),
                            tint = HefimerColors.White50,
                            modifier = Modifier
                                .size(18.dp)
                                .applePressable(pressedScale = 0.88f) { vm.dismissClipboardSuggestion() },
                        )
                    }
                }
            }

            // FAQ, Privacy & Security, and Terms of Service Sheet (Web parity)
            AppleSwipeableSheet(
                visible = activeInfoDoc != null,
                onDismiss = { activeInfoDoc = null },
            ) { dismiss ->
                activeInfoDoc?.let { doc ->
                    HefimerInfoDocSheetContent(
                        doc = doc,
                        onClose = dismiss,
                    )
                }
            }

            // Admin Dashboard Sheet (Backend KV PBKDF2 + 4 R2 Credentials)
            AppleSwipeableSheet(
                visible = adminState.showDashboard,
                onDismiss = { vm.closeAdminDashboard() },
            ) { dismiss ->
                AdminDashboardSheetContent(
                    stats = adminState.stats,
                    r2Config = adminState.r2Config,
                    busy = adminState.busy,
                    onToggleFreeze = { vm.toggleAdminFreeze() },
                    onSetDeveloper = { vm.setAdminDeveloper(it) },
                    onSendBroadcast = { t, m, mins -> vm.sendAdminBroadcast(t, m, mins) },
                    onClearBroadcast = { vm.clearAdminBroadcast() },
                    onUpdateLikes = { vm.updateAdminLikes(it) },
                    onUpdatePasswords = { a, s -> vm.updateAdminPasswords(a, s) },
                    onUpdateR2Config = { acc, ak, sk, b -> vm.updateAdminR2Config(acc, ak, sk, b) },
                    onLogout = {
                        dismiss()
                        vm.logoutAdmin()
                    },
                    onClose = dismiss,
                )
            }

            if (adminState.showLogin) {
                AdminLoginDialog(
                    loading = adminState.loginLoading,
                    onDismiss = { vm.dismissAdminLogin() },
                    onSubmit = { vm.loginAdmin(it) },
                )
            }

            // Post-Save "Open With App" Bottom Sheet
            AppleSwipeableSheet(
                visible = savedFilePrompt != null,
                onDismiss = { vm.dismissSavedFilePrompt() },
            ) { dismiss ->
                savedFilePrompt?.let { prompt ->
                    PostSaveBottomSheetContent(
                        prompt = prompt,
                        onOpenWithApp = {
                            val opened = DownloadHelper.openSavedFileWithChooser(
                                context = context,
                                uriString = prompt.uriString,
                                mimeType = prompt.mimeType,
                                fileName = prompt.fileName,
                            )
                            if (!opened) {
                                vm.showToast(
                                    tr("No compatible app found to open this file", "Không tìm thấy ứng dụng phù hợp để mở tệp này"),
                                    ToastType.ERROR,
                                )
                            }
                            dismiss()
                        },
                        onShare = {
                            DownloadHelper.shareSavedFile(
                                context = context,
                                uriString = prompt.uriString,
                                mimeType = prompt.mimeType,
                                fileName = prompt.fileName,
                            )
                            dismiss()
                        },
                        onOpenDownloads = {
                            DownloadHelper.openDownloadsFolder(context)
                            dismiss()
                        },
                        onClose = dismiss,
                    )
                }
            }

            // Send Provider Picker Sheet (mounted above bottom nav & Settings so it is NEVER covered)
            AppleSwipeableSheet(
                visible = sendState.showProviderPicker,
                onDismiss = { vm.closeSendProviderPicker() },
            ) { dismiss ->
                ProviderPickerSheetContent(
                    providers = UploadProviders.allProviders(customS3Providers),
                    selectedId = sendState.selectedProviderId,
                    defaultProviderId = defaultProviderId,
                    onSetDefault = { vm.setDefaultUploadProvider(it) },
                    onSelect = {
                        vm.selectUploadProvider(it)
                        dismiss()
                    },
                    onClose = dismiss,
                )
            }

            // Offline Network Status Pill
            AnimatedVisibility(
                visible = !isOnline && activeOverlay == null,
                enter = fadeIn(),
                exit = fadeOut(),
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = statusTop + 54.dp, start = 20.dp, end = 20.dp),
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier
                        .appleGlassSurface(
                            shape = CircleShape,
                            variant = AppleGlassVariant.Elevated,
                            accentBorderTint = HefimerColors.RedBorder,
                            elevation = 14.dp,
                        )
                        .padding(horizontal = 14.dp, vertical = 8.dp),
                ) {
                    Icon(Icons.Rounded.WifiOff, null, tint = HefimerColors.Red400, modifier = Modifier.size(15.dp))
                    Text(
                        text = com.hefimer.app.ui.theme.tr(
                            "Offline • Waiting for connection",
                            "Mất kết nối mạng • Đang chờ tín hiệu",
                        ),
                        color = HefimerColors.White90,
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }

            // Global Notice Banner (from Firebase stats)
            AnimatedVisibility(
                visible = isNoticeVisible,
                enter = fadeIn(),
                exit = fadeOut(),
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = statusTop + if (!isOnline) 98.dp else 54.dp, start = 16.dp, end = 16.dp),
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .appleGlassSurface(
                            shape = RoundedCornerShape(22.dp),
                            variant = AppleGlassVariant.Elevated,
                            elevation = 14.dp,
                        )
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                ) {
                    Icon(Icons.Rounded.Notifications, null, tint = HefimerColors.White80, modifier = Modifier.size(16.dp))
                    Text(
                        text = globalStats.notice,
                        color = HefimerColors.White90,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f),
                    )
                    Icon(
                        Icons.Rounded.Close,
                        com.hefimer.app.ui.theme.tr("Dismiss notice", "Đóng thông báo"),
                        tint = HefimerColors.White50,
                        modifier = Modifier.size(16.dp).applePressable(pressedScale = 0.88f) { vm.dismissNotice() },
                    )
                }
            }

            // Global Broadcast Modal
            globalStats.broadcast?.let { broadcast ->
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .fillMaxSize()
                        .background(HefimerColors.Black.copy(alpha = 0.65f))
                        .padding(24.dp),
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .appleGlassSurface(
                                shape = RoundedCornerShape(36.dp),
                                variant = AppleGlassVariant.Elevated,
                                elevation = 24.dp,
                            )
                            .padding(26.dp),
                    ) {
                        Text(
                            text = broadcast.title.uppercase(),
                            color = HefimerColors.White,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center,
                        )
                        if (broadcast.message.isNotBlank()) {
                            Text(
                                text = broadcast.message,
                                color = HefimerColors.White70,
                                fontSize = 14.sp,
                                textAlign = TextAlign.Center,
                            )
                        }
                        HefimerPrimaryButton(
                            text = tr("Got it", "Đã hiểu"),
                            onClick = { vm.dismissBroadcast() },
                        )
                    }
                }
            }

            // System Frozen Overlay
            if (globalStats.isFrozen) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .fillMaxSize()
                        .background(HefimerColors.Black.copy(alpha = 0.88f))
                        .padding(32.dp),
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(14.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .appleGlassSurface(
                                shape = RoundedCornerShape(36.dp),
                                variant = AppleGlassVariant.Elevated,
                                accentBorderTint = HefimerColors.Red400,
                                elevation = 24.dp,
                            )
                            .padding(28.dp),
                    ) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .size(72.dp)
                                .appleGlassSurface(
                                    shape = CircleShape,
                                    variant = AppleGlassVariant.Clear,
                                    accentBorderTint = HefimerColors.Red400,
                                    accentFillTint = HefimerColors.RedBg,
                                    elevation = 0.dp,
                                ),
                        ) {
                            Icon(Icons.Rounded.AcUnit, null, tint = HefimerColors.Red400, modifier = Modifier.size(34.dp))
                        }
                        Text(
                            text = tr("SYSTEM FROZEN", "HỆ THỐNG TẠM ĐÓNG BĂNG"),
                            color = HefimerColors.White,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 2.sp,
                        )
                        Text(
                            text = tr(
                                "The administrator has temporarily suspended operations for maintenance. Please check back later.",
                                "Quản trị viên đã tạm ngừng hệ thống để bảo trì. Vui lòng quay lại sau.",
                            ),
                            color = HefimerColors.White50,
                            fontSize = 13.sp,
                            textAlign = TextAlign.Center,
                        )
                    }
                }
            }

            // Floating Toast Host
            HefimerToastHost(toast = toast, modifier = Modifier.align(Alignment.TopCenter))

            // Duplicate Re-Download Confirmation Modal
            pendingRedownload?.let { pending ->
                val readable = remember(pending.matchedRecord.savedUri) {
                    DownloadHelper.isUriReadable(context, pending.matchedRecord.savedUri)
                }
                RedownloadConfirmDialog(
                    prompt = pending,
                    hasExistingReadableFile = readable,
                    onOpenExisting = { vm.openExistingFromRedownload() },
                    onRedownload = { vm.confirmPendingRedownload() },
                    onDismiss = { vm.dismissPendingRedownload() },
                )
            }

            // Global Destructive Action Confirmation Modal
            confirmDialog?.let { req ->
                HefimerConfirmDialog(
                    request = req,
                    onDismiss = { vm.dismissConfirm() },
                )
            }

            // 4-Screen First-Launch Apple Onboarding (emerges directly beneath the logo intro)
            if (showOnboarding) {
                HefimerOnboardingOverlay(
                    isLightMode = isLightMode,
                    appLanguage = appLanguage,
                    defaultProviderName = UploadProviders.getConfig(defaultProviderId).name,
                    onSetLightMode = { vm.setLightMode(it) },
                    onSetAppLanguage = { vm.setAppLanguage(it) },
                    onComplete = { vm.completeOnboarding() },
                )
            }

            // Minimalist Apple-grade Circular Logo Launch Intro (runs once on cold start)
            var showLaunchIntro by remember { mutableStateOf(true) }
            if (showLaunchIntro) {
                HefimerLaunchIntro(onFinished = { showLaunchIntro = false })
            }
        }
    }
}
