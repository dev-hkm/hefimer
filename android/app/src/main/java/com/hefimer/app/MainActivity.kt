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
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import com.hefimer.app.ui.AppOverlay
import com.hefimer.app.ui.HefimerViewModel
import com.hefimer.app.ui.components.FloatingDownloadProgressBanner
import com.hefimer.app.ui.components.HefimerFloatingBottomDock
import com.hefimer.app.ui.components.HefimerLaunchIntro
import com.hefimer.app.ui.components.HefimerPrimaryButton
import com.hefimer.app.ui.components.HefimerToastHost
import com.hefimer.app.ui.components.MainTab
import com.hefimer.app.ui.components.QrScannerOverlay
import com.hefimer.app.ui.screens.ChatRoomOverlay
import com.hefimer.app.ui.screens.FloatingIncomingOfferBanner
import com.hefimer.app.ui.screens.HistoryOverlayScreen
import com.hefimer.app.ui.screens.LinkScreen
import com.hefimer.app.ui.screens.ProviderPickerSheetContent
import com.hefimer.app.ui.screens.ReceiveScreen
import com.hefimer.app.ui.screens.RoomsScreen
import com.hefimer.app.ui.screens.SendScreen
import com.hefimer.app.ui.screens.SpaceCanvasOverlay
import com.hefimer.app.ui.screens.SpaceScreen
import com.hefimer.app.ui.screens.WhiteboardOverlay
import com.hefimer.app.ui.theme.AppleLayeredNavigationHost
import com.hefimer.app.ui.theme.AppleSwipeableSheet
import com.hefimer.app.ui.theme.HefimerColors
import com.hefimer.app.ui.theme.HefimerTheme
import com.hefimer.app.ui.theme.HeroBoundsRegistry
import com.hefimer.app.ui.theme.LocalHeroBounds
import com.hefimer.app.ui.theme.applePressable

class MainActivity : ComponentActivity() {
    private val viewModel: HefimerViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        // True edge-to-edge transparency for both status bar and gesture/button navigation bar
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
        )
        super.onCreate(savedInstanceState)

        WindowCompat.setDecorFitsSystemWindows(window, false)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            window.isNavigationBarContrastEnforced = false
            window.isStatusBarContrastEnforced = false
        }

        viewModel.handleIncomingIntent(intent)

        setContent {
            HefimerTheme {
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
}

@Composable
private fun HefimerRootScreen(
    vm: HefimerViewModel,
    onExitApp: () -> Unit,
) {
    val selectedTab by vm.selectedTab.collectAsState()
    val activeOverlay by vm.activeOverlay.collectAsState()
    val activeHeroKey by vm.activeHeroKey.collectAsState()
    val toast by vm.toast.collectAsState()
    val sendState by vm.sendState.collectAsState()
    val linkState by vm.linkState.collectAsState()
    val downloadState by vm.downloadState.collectAsState()
    val globalStats by vm.globalStats.collectAsState()
    val noticeDismissed by vm.noticeDismissed.collectAsState()

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

                        // Floating compact pill Bottom Navigation Dock
                        HefimerFloatingBottomDock(
                            selectedTab = selectedTab,
                            onSelectTab = { vm.selectTab(it) },
                            linkBadgeCount = linkBadgeCount,
                            linkGroupActive = linkState.sync.group != null,
                            isR2Mode = sendState.isR2Mode,
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

            // Send Provider Picker Sheet (mounted above bottom nav so it is NEVER covered by the dock)
            AppleSwipeableSheet(
                visible = sendState.showProviderPicker,
                onDismiss = { vm.closeSendProviderPicker() },
            ) { dismiss ->
                ProviderPickerSheetContent(
                    selectedId = sendState.selectedProviderId,
                    onSelect = {
                        vm.selectUploadProvider(it)
                        dismiss()
                    },
                    onClose = dismiss,
                )
            }

            // Global Notice Banner (from Firebase stats)
            AnimatedVisibility(
                visible = isNoticeVisible,
                enter = fadeIn(),
                exit = fadeOut(),
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = statusTop + 54.dp, start = 16.dp, end = 16.dp),
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(18.dp))
                        .background(HefimerColors.SurfaceCard.copy(alpha = 0.94f))
                        .border(1.dp, HefimerColors.White15, RoundedCornerShape(18.dp))
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                ) {
                    Icon(Icons.Rounded.Notifications, null, tint = HefimerColors.Emerald400, modifier = Modifier.size(16.dp))
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
                        "Dismiss notice",
                        tint = HefimerColors.White50,
                        modifier = Modifier.size(16.dp).applePressable { vm.dismissNotice() },
                    )
                }
            }

            // Global Broadcast Modal
            globalStats.broadcast?.let { broadcast ->
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .fillMaxSize()
                        .background(HefimerColors.Black.copy(alpha = 0.86f))
                        .padding(24.dp),
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(34.dp))
                            .background(HefimerColors.SurfaceCard)
                            .border(1.dp, HefimerColors.White15, RoundedCornerShape(34.dp))
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
                        HefimerPrimaryButton(text = "Got it", onClick = { vm.dismissBroadcast() })
                    }
                }
            }

            // System Frozen Overlay
            if (globalStats.isFrozen) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .fillMaxSize()
                        .background(HefimerColors.Black.copy(alpha = 0.92f))
                        .padding(32.dp),
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(14.dp),
                    ) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .size(72.dp)
                                .clip(CircleShape)
                                .background(HefimerColors.RedBg)
                                .border(1.dp, HefimerColors.RedBorder, CircleShape),
                        ) {
                            Icon(Icons.Rounded.AcUnit, null, tint = HefimerColors.Red400, modifier = Modifier.size(34.dp))
                        }
                        Text("SYSTEM FROZEN", color = HefimerColors.White, fontSize = 20.sp, fontWeight = FontWeight.Bold, letterSpacing = 2.sp)
                        Text(
                            text = "The administrator has temporarily suspended operations for maintenance. Please check back later.",
                            color = HefimerColors.White40,
                            fontSize = 13.sp,
                            textAlign = TextAlign.Center,
                        )
                    }
                }
            }

            // Floating Toast Host
            HefimerToastHost(toast = toast, modifier = Modifier.align(Alignment.TopCenter))

            // Minimalist Apple-grade Circular Logo Launch Intro (runs once on cold start)
            var showLaunchIntro by remember { mutableStateOf(true) }
            if (showLaunchIntro) {
                HefimerLaunchIntro(onFinished = { showLaunchIntro = false })
            }
        }
    }
}
