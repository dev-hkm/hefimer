package com.hefimer.app.ui

import android.app.Application
import android.content.Intent
import android.net.Uri
import android.os.Build
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.hefimer.app.data.local.HefimerStorage
import com.hefimer.app.data.model.BoardPoint
import com.hefimer.app.data.model.BoardStroke
import com.hefimer.app.data.model.ChatMessage
import com.hefimer.app.data.model.DeviceSync
import com.hefimer.app.data.model.DeviceTransfer
import com.hefimer.app.data.model.DropCreatedOffer
import com.hefimer.app.data.model.DropFileEntry
import com.hefimer.app.data.model.DropItem
import com.hefimer.app.data.model.GlobalBroadcast
import com.hefimer.app.data.model.GlobalStatsState
import com.hefimer.app.data.model.HistoryItem
import com.hefimer.app.data.model.InviteResponse
import com.hefimer.app.data.model.RecentSpaceEntry
import com.hefimer.app.data.model.SpaceCursor
import com.hefimer.app.data.model.SpaceItem
import com.hefimer.app.data.model.SpaceMember
import com.hefimer.app.data.model.SpaceUploadTask
import com.hefimer.app.data.model.ToastMessage
import com.hefimer.app.data.model.ToastType
import com.hefimer.app.data.model.UploadStats
import com.hefimer.app.data.model.parseExpireDurationMs
import com.hefimer.app.data.network.DeviceApiClient
import com.hefimer.app.data.network.DownloadCancellationToken
import com.hefimer.app.data.network.DownloadHelper
import com.hefimer.app.data.network.FileUploadEngine
import com.hefimer.app.data.network.FirebaseRtdbClient
import com.hefimer.app.data.network.HefimerApiConfig
import com.hefimer.app.data.network.LocalPickedFile
import com.hefimer.app.data.network.R2Service
import com.hefimer.app.data.network.UploadCancellationToken
import com.hefimer.app.data.network.UploadProviders
import com.hefimer.app.ui.components.CodeHighlighter
import com.hefimer.app.ui.components.MainTab
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.floatOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.longOrNull
import java.util.UUID
import kotlin.random.Random

sealed interface AppOverlay {
    data object History : AppOverlay
    data class QrScanner(val target: ScanTarget = ScanTarget.AUTO) : AppOverlay
    data class SpaceCanvas(val spaceId: String, val spaceName: String) : AppOverlay
    data class ChatRoom(val roomId: String, val roomName: String) : AppOverlay
    data class Whiteboard(val boardId: String, val boardName: String) : AppOverlay
}

enum class ScanTarget { AUTO, RECEIVE, PAIR_TOKEN }

enum class SendSubMode { FILE, TEXT }

data class DownloadProgressState(
    val fileName: String,
    val loadedBytes: Long = 0L,
    val totalBytes: Long = -1L,
    val percent: Int = 0,
) {
    val fraction: Float
        get() = if (totalBytes > 0L) {
            (loadedBytes.toFloat() / totalBytes.toFloat()).coerceIn(0f, 1f)
        } else {
            (percent / 100f).coerceIn(0f, 1f)
        }
}

data class SendUiState(
    val subMode: SendSubMode = SendSubMode.FILE,
    val isR2Mode: Boolean = false,
    val showR2AuthDialog: Boolean = false,
    val showProviderPicker: Boolean = false,
    // File state
    val pickedFiles: List<LocalPickedFile> = emptyList(),
    val isFolderMode: Boolean = false,
    val selectedProviderId: String = "storageto",
    val fileExpire: String = "72h",
    val filePassword: String = "",
    val fileOneTime: Boolean = false,
    val r2CustomName: String = "",
    val r2Description: String = "",
    val fileUploading: Boolean = false,
    val fileProgress: Int = 0,
    val fileProgressFraction: Float = 0f,
    val fileUploadedBytes: Long = 0L,
    val fileTotalBytes: Long = 0L,
    val fileStatus: String = "",
    val fileResultCode: String = "",
    val fileResultName: String = "",
    val fileResultExpiresAt: Long? = null,
    val fileResultIsR2: Boolean = false,
    // Text state
    val textContent: String = "",
    val manualLangId: String? = null,
    val textExpire: String = "1h",
    val textPassword: String = "",
    val textOneTime: Boolean = false,
    val textLoading: Boolean = false,
    val textResultCode: String = "",
    val textResultExpiresAt: Long? = null,
)

data class ReceiveUiState(
    val code: String = "",
    val loading: Boolean = false,
    val result: DropItem? = null,
    val passwordPromptOpen: Boolean = false,
    val pendingDrop: DropItem? = null,
)

data class SpaceUiState(
    val userName: String = "",
    val lobbyMode: String = "join", // "join" or "create"
    val joinCode: String = "",
    val joinPassword: String = "",
    val joinRequiresPassword: Boolean = false,
    val createTitle: String = "",
    val createCode: String = random5Digit(),
    val createCodeStatus: String = "idle", // "idle", "checking", "available", "taken"
    val createExpire: String = "1h",
    val createPassword: String = "",
    val createdCode: String = "",
    val createdName: String = "",
    val createdExpiresAt: Long? = null,
    val loading: Boolean = false,
    val recentSpaces: List<RecentSpaceEntry> = emptyList(),
    val selectedProviderId: String = "stocare",
    // Active canvas state
    val activeSpaceId: String = "",
    val activeSpaceName: String = "",
    val activeExpiresAt: Long = 0L,
    val items: List<SpaceItem> = emptyList(),
    val members: List<SpaceMember> = emptyList(),
    val cursors: List<SpaceCursor> = emptyList(),
    val uploadTasks: List<SpaceUploadTask> = emptyList(),
)

data class RoomsUiState(
    val section: String = "chat", // "chat" or "board"
    val userName: String = "",
    // Chat lobby
    val chatLobbyMode: String = "join",
    val chatJoinCode: String = "",
    val chatCreateName: String = "",
    val chatCreateCode: String = random5Digit(),
    val chatCreateCodeStatus: String = "idle",
    val chatCreateExpire: String = "1h",
    val chatCreatedCode: String = "",
    val chatCreatedName: String = "",
    val chatCreatedExpiresAt: Long? = null,
    val chatLoading: Boolean = false,
    // Active chat
    val activeChatId: String = "",
    val activeChatName: String = "",
    val activeChatExpiresAt: Long = 0L,
    val chatMessages: List<ChatMessage> = emptyList(),
    val chatMemberCount: Int = 1,
    // Board lobby
    val boardLobbyMode: String = "create",
    val boardJoinCode: String = "",
    val boardCreateName: String = "",
    val boardCreateExpire: String = "1h",
    val boardCreatedCode: String = "",
    val boardCreatedName: String = "",
    val boardCreatedExpiresAt: Long? = null,
    val boardLoading: Boolean = false,
    // Active board
    val activeBoardId: String = "",
    val activeBoardName: String = "",
    val activeBoardExpiresAt: Long = 0L,
    val boardStrokes: List<BoardStroke> = emptyList(),
)

data class LinkUiState(
    val loading: Boolean = false,
    val busyKey: String = "",
    val error: String = "",
    val sync: DeviceSync = DeviceSync(),
    val setupMode: String = "create", // "create" or "join"
    val groupName: String = "My Hefimer Group",
    val duration: String = "7d",
    val pairToken: String = "",
    val invite: InviteResponse? = null,
    val deviceNameInput: String = "",
    val section: String = "devices", // "devices" or "activity"
    val dismissedPromptIds: Set<String> = emptySet(),
)

private fun random5Digit(): String = Random.nextInt(10000, 100000).toString()

class HefimerViewModel(application: Application) : AndroidViewModel(application) {
    val storage = HefimerStorage(application)
    val rtdb = FirebaseRtdbClient()
    val uploader = FileUploadEngine(application, storage)
    val r2Service = R2Service(application)
    val deviceApi = DeviceApiClient(storage)

    private val clientId = storage.getOrCreateClientId()
    private val json = HefimerApiConfig.json

    private val _selectedTab = MutableStateFlow(MainTab.SEND)
    val selectedTab: StateFlow<MainTab> = _selectedTab.asStateFlow()

    private val _activeOverlay = MutableStateFlow<AppOverlay?>(null)
    val activeOverlay: StateFlow<AppOverlay?> = _activeOverlay.asStateFlow()

    private val _activeHeroKey = MutableStateFlow<String?>(null)
    val activeHeroKey: StateFlow<String?> = _activeHeroKey.asStateFlow()

    private val _toast = MutableStateFlow<ToastMessage?>(null)
    val toast: StateFlow<ToastMessage?> = _toast.asStateFlow()

    private val _history = MutableStateFlow(storage.loadHistory())
    val history: StateFlow<List<HistoryItem>> = _history.asStateFlow()

    private val _standardStats = MutableStateFlow(storage.loadUploadStats(false))
    val standardStats: StateFlow<UploadStats> = _standardStats.asStateFlow()

    private val _r2Stats = MutableStateFlow(storage.loadUploadStats(true))
    val r2Stats: StateFlow<UploadStats> = _r2Stats.asStateFlow()

    private val _globalStats = MutableStateFlow(GlobalStatsState())
    val globalStats: StateFlow<GlobalStatsState> = _globalStats.asStateFlow()

    private val _noticeDismissed = MutableStateFlow(false)
    val noticeDismissed: StateFlow<Boolean> = _noticeDismissed.asStateFlow()

    private val _sendState = MutableStateFlow(SendUiState())
    val sendState: StateFlow<SendUiState> = _sendState.asStateFlow()

    private val _receiveState = MutableStateFlow(ReceiveUiState())
    val receiveState: StateFlow<ReceiveUiState> = _receiveState.asStateFlow()

    private val _spaceState = MutableStateFlow(
        SpaceUiState(
            userName = storage.getUserName(),
            recentSpaces = storage.loadRecentSpaces(),
            selectedProviderId = storage.getSpaceProvider(),
        ),
    )
    val spaceState: StateFlow<SpaceUiState> = _spaceState.asStateFlow()

    private val _roomsState = MutableStateFlow(
        RoomsUiState(userName = storage.getUserName()),
    )
    val roomsState: StateFlow<RoomsUiState> = _roomsState.asStateFlow()

    private val _linkState = MutableStateFlow(
        LinkUiState(deviceNameInput = storage.getDeviceName()),
    )
    val linkState: StateFlow<LinkUiState> = _linkState.asStateFlow()

    private val _downloadState = MutableStateFlow<DownloadProgressState?>(null)
    val downloadState: StateFlow<DownloadProgressState?> = _downloadState.asStateFlow()

    private var fileCancelToken: UploadCancellationToken? = null
    private var spaceCancelToken: UploadCancellationToken? = null
    private var activeDownloadToken: DownloadCancellationToken? = null
    private var toastJob: Job? = null
    private var spaceObserverJob: Job? = null
    private var chatObserverJob: Job? = null
    private var boardObserverJob: Job? = null
    private var codeCheckJob: Job? = null
    private var chatCodeCheckJob: Job? = null
    private var linkSyncInFlight = false
    private var firstLinkSync = true
    private val observedIncomingIds = mutableSetOf<String>()
    private var fileTabTapCount = 0
    private var lastBroadcastId: String? = null
    private var activeChatMemberKey: String? = null
    private val tabBackStack = ArrayDeque<MainTab>()
    private var lastRootBackMs = 0L

    init {
        startGlobalObservers()
        startLinkPolling()
        checkSpaceCodeAvailability(_spaceState.value.createCode)
        checkChatCodeAvailability(_roomsState.value.chatCreateCode)
    }

    fun selectTab(tab: MainTab, recordBackStack: Boolean = true) {
        val current = _selectedTab.value
        if (recordBackStack && tab != current) {
            if (tabBackStack.lastOrNull() != current) {
                tabBackStack.addLast(current)
                while (tabBackStack.size > 20) tabBackStack.removeFirst()
            }
        }
        _selectedTab.value = tab
        if (tab == MainTab.LINK) {
            storage.setDeviceLinkEnabled(true)
            refreshLink(silent = false)
        }
    }

    fun openSendProviderPicker() {
        _sendState.update { it.copy(showProviderPicker = true) }
    }

    fun closeSendProviderPicker() {
        _sendState.update { it.copy(showProviderPicker = false) }
    }

    /**
     * Handles back navigation on Layer 0 (when no overlay/sheet is mounted).
     * Returns true if consumed inside the app, or false only on intentional double-back at root.
     */
    fun handleBackNavigation(): Boolean {
        // 1. Dismiss in-app modals/prompts first
        if (_sendState.value.showProviderPicker) {
            closeSendProviderPicker()
            return true
        }
        if (_sendState.value.showR2AuthDialog) {
            dismissR2AuthDialog()
            return true
        }
        if (_receiveState.value.passwordPromptOpen) {
            dismissReceivePasswordPrompt()
            return true
        }
        if (_globalStats.value.broadcast != null) {
            dismissBroadcast()
            return true
        }

        // 2. Step back out of active results / sub-views within the current tab
        when (_selectedTab.value) {
            MainTab.SEND -> {
                val st = _sendState.value
                if (st.subMode == SendSubMode.FILE && st.fileResultCode.isNotBlank()) {
                    resetFileResult(clearSelection = false)
                    return true
                }
                if (st.subMode == SendSubMode.TEXT && st.textResultCode.isNotBlank()) {
                    resetTextResult(clearContent = false)
                    return true
                }
                if (st.subMode == SendSubMode.TEXT) {
                    setSendSubMode(SendSubMode.FILE)
                    return true
                }
            }
            MainTab.RECEIVE -> {
                if (_receiveState.value.result != null) {
                    resetReceive()
                    return true
                }
            }
            MainTab.SPACE -> {
                val sp = _spaceState.value
                if (sp.createdCode.isNotBlank()) {
                    clearSpaceCreatedCard()
                    return true
                }
                if (sp.lobbyMode == "create") {
                    setSpaceLobbyMode("join")
                    return true
                }
            }
            MainTab.ROOMS -> {
                val rm = _roomsState.value
                if (rm.section == "chat" && rm.chatCreatedCode.isNotBlank()) {
                    clearChatCreatedCard()
                    return true
                }
                if (rm.section == "board" && rm.boardCreatedCode.isNotBlank()) {
                    clearBoardCreatedCard()
                    return true
                }
                if (rm.section == "board") {
                    setRoomsSection("chat")
                    return true
                }
            }
            MainTab.LINK -> {
                if (_linkState.value.section == "activity") {
                    setLinkSection("devices")
                    return true
                }
            }
        }

        // 3. Pop previous tab from navigation history
        if (tabBackStack.isNotEmpty()) {
            val prevTab = tabBackStack.removeLast()
            selectTab(prevTab, recordBackStack = false)
            return true
        }

        // 4. Return to Send home tab if currently on another tab
        if (_selectedTab.value != MainTab.SEND) {
            selectTab(MainTab.SEND, recordBackStack = false)
            return true
        }

        // 5. At root home (Send -> File): require intentional double-back within 2.2s to exit
        val now = System.currentTimeMillis()
        if (now - lastRootBackMs > 2_200L) {
            lastRootBackMs = now
            showToast("Back again to exit Hefimer", ToastType.INFO)
            return true
        }
        return false
    }

    fun openOverlay(overlay: AppOverlay, heroKey: String? = null) {
        _activeHeroKey.value = heroKey
        _activeOverlay.value = overlay
    }

    fun dismissOverlay() {
        when (_activeOverlay.value) {
            is AppOverlay.SpaceCanvas -> leaveSpace()
            is AppOverlay.ChatRoom -> leaveChatRoom()
            is AppOverlay.Whiteboard -> leaveBoard()
            else -> {}
        }
        _activeOverlay.value = null
        _activeHeroKey.value = null
    }

    fun showToast(message: String, type: ToastType = ToastType.INFO) {
        toastJob?.cancel()
        val msg = ToastMessage(message = message, type = type)
        _toast.value = msg
        if (type != ToastType.INFO) {
            toastJob = viewModelScope.launch {
                delay(3_000L)
                if (_toast.value?.id == msg.id) _toast.value = null
            }
        } else {
            toastJob = viewModelScope.launch {
                delay(4_500L)
                if (_toast.value?.id == msg.id) _toast.value = null
            }
        }
    }

    fun dismissNotice() {
        _noticeDismissed.value = true
    }

    fun dismissBroadcast() {
        _globalStats.update { it.copy(broadcast = null) }
    }

    private fun recordHistory(item: HistoryItem) {
        _history.value = storage.addHistory(item)
    }

    fun clearHistory() {
        storage.clearHistory()
        _history.value = emptyList()
        showToast("History cleared", ToastType.SUCCESS)
    }

    // region Global Firebase observers & presence
    private fun startGlobalObservers() {
        viewModelScope.launch {
            rtdb.observe("stats").collect { elem ->
                val obj = elem as? JsonObject ?: return@collect
                val notice = obj["notice"]?.jsonPrimitive?.contentOrNull.orEmpty()
                val prevNotice = _globalStats.value.notice
                if (notice != prevNotice) _noticeDismissed.value = false

                val passwords = obj["passwords"] as? JsonObject
                val bObj = obj["broadcast"] as? JsonObject
                val broadcast = if (bObj != null) {
                    val bId = bObj["id"]?.jsonPrimitive?.contentOrNull.orEmpty()
                    val bExp = bObj["expiresAt"]?.jsonPrimitive?.longOrNull ?: 0L
                    val expired = bExp != 0L && System.currentTimeMillis() >= bExp
                    if (!expired && bId.isNotBlank() && bId != lastBroadcastId) {
                        lastBroadcastId = bId
                        GlobalBroadcast(
                            id = bId,
                            title = bObj["title"]?.jsonPrimitive?.contentOrNull.orEmpty(),
                            message = bObj["message"]?.jsonPrimitive?.contentOrNull.orEmpty(),
                            theme = bObj["theme"]?.jsonPrimitive?.contentOrNull ?: "dark",
                            icon = bObj["icon"]?.jsonPrimitive?.contentOrNull ?: "Zap",
                            expiresAt = bExp,
                        )
                    } else {
                        _globalStats.value.broadcast
                    }
                } else {
                    null
                }

                _globalStats.update { cur ->
                    cur.copy(
                        developer = obj["developer"]?.jsonPrimitive?.contentOrNull ?: cur.developer,
                        notice = notice,
                        noticeEnabled = obj["noticeEnabled"]?.jsonPrimitive?.booleanOrNull ?: false,
                        noticeTheme = obj["noticeTheme"]?.jsonPrimitive?.contentOrNull ?: "blue",
                        noticeExpiresAt = obj["noticeExpiresAt"]?.jsonPrimitive?.longOrNull ?: 0L,
                        isFrozen = obj["isFrozen"]?.jsonPrimitive?.booleanOrNull ?: false,
                        likes = obj["likes"]?.jsonPrimitive?.intOrNull ?: cur.likes,
                        adminPass = passwords?.get("admin")?.jsonPrimitive?.contentOrNull ?: cur.adminPass,
                        secretPass = passwords?.get("secret")?.jsonPrimitive?.contentOrNull ?: cur.secretPass,
                        broadcast = broadcast,
                    )
                }
            }
        }

        // Presence heartbeat every 30s
        viewModelScope.launch(Dispatchers.IO) {
            val sessId = "and_${clientId}"
            while (isActive) {
                runCatching {
                    rtdb.set(
                        "presence/$sessId",
                        buildJsonObject {
                            put("lastSeen", JsonPrimitive(System.currentTimeMillis()))
                            put("id", JsonPrimitive(sessId))
                        },
                    )
                }
                delay(30_000L)
            }
        }
    }
    // endregion

    // region Deep links & Android Share Intent handling
    fun handleIncomingIntent(intent: Intent?) {
        if (intent == null) return
        when (intent.action) {
            Intent.ACTION_VIEW -> {
                val uri = intent.data ?: return
                handleScannedOrDeepLink(uri.toString(), ScanTarget.AUTO)
            }
            Intent.ACTION_SEND -> {
                val sharedText = intent.getStringExtra(Intent.EXTRA_TEXT)
                val streamUri = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    intent.getParcelableExtra(Intent.EXTRA_STREAM, Uri::class.java)
                } else {
                    @Suppress("DEPRECATION")
                    intent.getParcelableExtra(Intent.EXTRA_STREAM)
                }
                if (streamUri != null) {
                    onFilesPicked(listOf(streamUri))
                    _selectedTab.value = MainTab.SEND
                    _sendState.update { it.copy(subMode = SendSubMode.FILE) }
                } else if (!sharedText.isNullOrBlank()) {
                    _selectedTab.value = MainTab.SEND
                    _sendState.update { it.copy(subMode = SendSubMode.TEXT, textContent = sharedText) }
                }
            }
            Intent.ACTION_SEND_MULTIPLE -> {
                val uris = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    intent.getParcelableArrayListExtra(Intent.EXTRA_STREAM, Uri::class.java)
                } else {
                    @Suppress("DEPRECATION")
                    intent.getParcelableArrayListExtra(Intent.EXTRA_STREAM)
                }
                if (!uris.isNullOrEmpty()) {
                    onFilesPicked(uris)
                    _selectedTab.value = MainTab.SEND
                    _sendState.update { it.copy(subMode = SendSubMode.FILE) }
                }
            }
        }
    }

    fun handleScannedOrDeepLink(rawValue: String, target: ScanTarget) {
        val value = rawValue.trim()
        if (value.isBlank()) return
        if (_activeOverlay.value is AppOverlay.QrScanner) {
            _activeOverlay.value = null
        }

        // Direct pairing token (hfm-...)
        val pairMatch = Regex("""hfm-[A-Za-z0-9]{36}""").find(value)
        if (pairMatch != null || target == ScanTarget.PAIR_TOKEN) {
            val token = pairMatch?.value ?: value
            storage.setDeviceLinkEnabled(true)
            _linkState.update { it.copy(pairToken = token, setupMode = "join") }
            _selectedTab.value = MainTab.LINK
            refreshLink(silent = true)
            showToast("Pairing token captured", ToastType.SUCCESS)
            return
        }

        val uri = runCatching { Uri.parse(value) }.getOrNull()
        val path = uri?.path.orEmpty()
        val codeParam = uri?.getQueryParameter("code")?.filter { it.isDigit() }?.take(5)
            ?: value.filter { it.isDigit() }.takeIf { it.length == 5 }

        if (codeParam != null && codeParam.length == 5) {
            when {
                path.contains("/space") -> {
                    _spaceState.update { it.copy(joinCode = codeParam, lobbyMode = "join") }
                    _selectedTab.value = MainTab.SPACE
                    showToast("Space $codeParam ready to join", ToastType.INFO)
                }
                path.contains("/chat-room") -> {
                    _roomsState.update { it.copy(section = "chat", chatLobbyMode = "join", chatJoinCode = codeParam) }
                    _selectedTab.value = MainTab.ROOMS
                    showToast("Chat room $codeParam ready to join", ToastType.INFO)
                }
                path.contains("/board") -> {
                    _roomsState.update { it.copy(section = "board", boardLobbyMode = "join", boardJoinCode = codeParam) }
                    _selectedTab.value = MainTab.ROOMS
                    showToast("Board $codeParam ready to join", ToastType.INFO)
                }
                else -> {
                    _selectedTab.value = MainTab.RECEIVE
                    setReceiveCode(codeParam)
                    retrieveDrop(codeOverride = codeParam)
                }
            }
        } else {
            showToast("Unrecognized QR code", ToastType.ERROR)
        }
    }
    // endregion

    // region SEND TAB (File, Text, Secret R2)
    fun setSendSubMode(mode: SendSubMode) {
        if (mode == SendSubMode.FILE && _sendState.value.subMode == SendSubMode.FILE) {
            fileTabTapCount++
            if (fileTabTapCount >= 6) {
                fileTabTapCount = 0
                if (_sendState.value.isR2Mode) {
                    _sendState.update { it.copy(isR2Mode = false) }
                    showToast("Secret R2 mode deactivated", ToastType.SUCCESS)
                } else {
                    _sendState.update { it.copy(showR2AuthDialog = true) }
                }
            }
        } else {
            fileTabTapCount = if (mode == SendSubMode.FILE) 1 else 0
            _sendState.update { it.copy(subMode = mode) }
        }
    }

    fun dismissR2AuthDialog() {
        _sendState.update { it.copy(showR2AuthDialog = false) }
    }

    fun verifyR2Passcode(input: String) {
        if (input == _globalStats.value.secretPass) {
            _sendState.update { it.copy(isR2Mode = true, showR2AuthDialog = false, fileExpire = "1h") }
            showToast("Secret R2 mode activated", ToastType.SUCCESS)
        } else {
            showToast("Incorrect password", ToastType.ERROR)
        }
    }

    fun exitR2Mode() {
        _sendState.update {
            it.copy(
                isR2Mode = false,
                fileExpire = UploadProviders.getConfig(it.selectedProviderId).defaultExpire,
            )
        }
        showToast("Exited Secret R2 mode", ToastType.INFO)
    }

    fun setFolderMode(enabled: Boolean) {
        _sendState.update { it.copy(isFolderMode = enabled, pickedFiles = emptyList()) }
    }

    fun onFilesPicked(uris: List<Uri>) {
        if (uris.isEmpty()) return
        val resolved = uris.map { uploader.resolvePickedFile(it) }
        _sendState.update { it.copy(pickedFiles = resolved, isFolderMode = false) }
    }

    fun onFolderPicked(treeUri: Uri) {
        viewModelScope.launch {
            val token = UploadCancellationToken()
            fileCancelToken = token
            _sendState.update {
                it.copy(
                    fileUploading = true,
                    fileProgress = 0,
                    fileProgressFraction = 0f,
                    fileUploadedBytes = 0L,
                    fileTotalBytes = 0L,
                    fileStatus = "Zipping folder contents...",
                )
            }
            try {
                val zipped = uploader.zipFolderTree(
                    treeUri = treeUri,
                    onProgress = { pct ->
                        _sendState.update {
                            it.copy(
                                fileProgress = pct,
                                fileProgressFraction = (pct / 100f).coerceIn(0f, 1f),
                            )
                        }
                    },
                    cancelToken = token,
                )
                _sendState.update {
                    it.copy(
                        pickedFiles = listOf(zipped),
                        isFolderMode = true,
                        fileUploading = false,
                        fileProgress = 0,
                        fileProgressFraction = 0f,
                        fileUploadedBytes = 0L,
                        fileTotalBytes = 0L,
                        fileStatus = "",
                    )
                }
                showToast("Folder packed as ${zipped.name}", ToastType.SUCCESS)
            } catch (e: Throwable) {
                _sendState.update {
                    it.copy(
                        fileUploading = false,
                        fileProgress = 0,
                        fileProgressFraction = 0f,
                        fileUploadedBytes = 0L,
                        fileTotalBytes = 0L,
                        fileStatus = "",
                    )
                }
                if (e.message != "UPLOAD_CANCELED") {
                    showToast(e.message ?: "Failed to package folder", ToastType.ERROR)
                }
            }
        }
    }

    fun clearPickedFiles() {
        _sendState.value.pickedFiles.forEach { it.tempFile?.delete() }
        _sendState.update { it.copy(pickedFiles = emptyList()) }
    }

    fun selectUploadProvider(providerId: String) {
        _sendState.update {
            it.copy(
                selectedProviderId = providerId,
                fileExpire = UploadProviders.normalizeExpire(providerId, it.fileExpire),
            )
        }
    }

    fun setFileExpire(expire: String) = _sendState.update { it.copy(fileExpire = expire) }
    fun setFilePassword(pw: String) = _sendState.update { it.copy(filePassword = pw) }
    fun toggleFileOneTime() = _sendState.update { it.copy(fileOneTime = !it.fileOneTime) }
    fun setR2CustomName(v: String) = _sendState.update { it.copy(r2CustomName = v) }
    fun setR2Description(v: String) = _sendState.update { it.copy(r2Description = v) }

    fun cancelFileUpload() {
        fileCancelToken?.cancel()
        fileCancelToken = null
        _sendState.update {
            it.copy(
                fileUploading = false,
                fileProgress = 0,
                fileProgressFraction = 0f,
                fileUploadedBytes = 0L,
                fileTotalBytes = 0L,
                fileStatus = "",
            )
        }
        showToast("Upload canceled", ToastType.INFO)
    }

    fun resetFileResult(clearSelection: Boolean) {
        if (clearSelection) clearPickedFiles()
        _sendState.update {
            it.copy(
                fileResultCode = "",
                fileResultName = "",
                fileResultExpiresAt = null,
                fileResultIsR2 = false,
                filePassword = "",
                fileOneTime = false,
                r2CustomName = "",
                r2Description = "",
            )
        }
    }

    fun uploadFiles() {
        val st = _sendState.value
        if (st.pickedFiles.isEmpty()) {
            showToast("Please select a file first", ToastType.ERROR)
            return
        }
        if (st.isR2Mode) {
            uploadFilesToR2()
            return
        }

        val providerCfg = UploadProviders.getConfig(st.selectedProviderId)
        val totalSize = st.pickedFiles.sumOf { it.size }
        val safeTotal = totalSize.coerceAtLeast(1L)
        if (st.pickedFiles.any { it.size > providerCfg.maxSizeBytes }) {
            showToast("File exceeds limit of ${providerCfg.maxSizeLabel} for ${providerCfg.name}", ToastType.ERROR)
            return
        }

        val token = UploadCancellationToken()
        fileCancelToken = token
        _sendState.update {
            it.copy(
                fileUploading = true,
                fileProgress = 0,
                fileProgressFraction = 0f,
                fileUploadedBytes = 0L,
                fileTotalBytes = totalSize,
                fileStatus = "Preparing upload...",
            )
        }

        viewModelScope.launch {
            try {
                val isMulti = st.pickedFiles.size > 1
                var actualProviderName = providerCfg.name
                val uploadedEntries = mutableListOf<JsonObject>()
                val fileBytes = LongArray(st.pickedFiles.size)

                st.pickedFiles.forEachIndexed { idx, picked ->
                    val res = uploader.uploadWithFallback(
                        primaryProviderId = st.selectedProviderId,
                        picked = picked,
                        expire = st.fileExpire,
                        cancelToken = token,
                        onProgress = { pct ->
                            if (picked.size <= 0L) {
                                val approx = (pct / 100f / st.pickedFiles.size + idx.toFloat() / st.pickedFiles.size).coerceIn(0f, 1f)
                                _sendState.update {
                                    it.copy(
                                        fileProgress = (approx * 100f).toInt().coerceIn(0, 100),
                                        fileProgressFraction = approx,
                                    )
                                }
                            }
                        },
                        onBytesProgress = { loaded, _ ->
                            fileBytes[idx] = loaded.coerceIn(0L, picked.size.coerceAtLeast(loaded))
                            val cumLoaded = fileBytes.sum().coerceIn(0L, safeTotal)
                            val frac = (cumLoaded.toFloat() / safeTotal.toFloat()).coerceIn(0f, 1f)
                            val pct = (frac * 100f).toInt().coerceIn(0, 100)
                            _sendState.update {
                                it.copy(
                                    fileProgress = pct,
                                    fileProgressFraction = frac,
                                    fileUploadedBytes = cumLoaded,
                                    fileTotalBytes = totalSize,
                                )
                            }
                        },
                        onStatus = { msg ->
                            val prefix = if (isMulti) "[${idx + 1}/${st.pickedFiles.size}] " else ""
                            _sendState.update { it.copy(fileStatus = "$prefix$msg") }
                        },
                        onFallback = { from, to ->
                            fileBytes[idx] = 0L
                            showToast("$from unavailable, switching to $to...", ToastType.INFO)
                        },
                    )
                    fileBytes[idx] = picked.size
                    actualProviderName = res.providerName
                    uploadedEntries.add(
                        buildJsonObject {
                            put("fileUrl", JsonPrimitive(res.fileUrl))
                            put("fileName", JsonPrimitive(picked.name))
                            put("fileSize", JsonPrimitive(picked.size))
                            put("mimeType", JsonPrimitive(picked.mimeType))
                        },
                    )
                }

                _sendState.update {
                    it.copy(
                        fileProgress = 100,
                        fileProgressFraction = 1f,
                        fileUploadedBytes = totalSize,
                        fileStatus = "Generating 5-digit drop code...",
                    )
                }
                val code = generateUniqueDropCode()
                val expiresAt = System.currentTimeMillis() + parseExpireDurationMs(st.fileExpire)
                val firstFile = st.pickedFiles.first()
                val firstUrl = uploadedEntries.first()["fileUrl"]!!.jsonPrimitive.content
                val displayName = if (isMulti) "${uploadedEntries.size} files" else firstFile.name

                val textPayload = if (isMulti) {
                    "__HEFIMER_FILES__${JsonArray(uploadedEntries)}"
                } else {
                    "__HEFIMER_FILE__" + buildJsonObject {
                        put("isDirectFile", JsonPrimitive(true))
                        put("fileName", JsonPrimitive(firstFile.name))
                        put("fileUrl", JsonPrimitive(firstUrl))
                    }.toString()
                }

                val dropJson = buildJsonObject {
                    put("isDirectFile", JsonPrimitive(true))
                    put("fileName", JsonPrimitive(displayName))
                    put("fileUrl", JsonPrimitive(firstUrl))
                    put("mimeType", JsonPrimitive(if (isMulti) "application/octet-stream" else firstFile.mimeType))
                    put("type", JsonPrimitive("file"))
                    put("expire", JsonPrimitive(st.fileExpire))
                    put("text", JsonPrimitive(textPayload))
                    put("expiresAt", JsonPrimitive(expiresAt))
                    put("timestamp", FirebaseRtdbClient.serverTimestamp())
                    if (isMulti) put("files", JsonArray(uploadedEntries))
                    if (st.filePassword.isNotBlank()) put("password", JsonPrimitive(st.filePassword.trim()))
                    if (st.fileOneTime) put("isOneTime", JsonPrimitive(true))
                }

                rtdb.set("drops/$code", dropJson)

                _standardStats.value = storage.recordUploadStats(totalSize, false)
                recordHistory(
                    HistoryItem(
                        code = code,
                        type = "file",
                        action = "sent",
                        fileName = displayName,
                        fileSize = totalSize,
                    ),
                )
                _sendState.update {
                    it.copy(
                        fileUploading = false,
                        fileProgress = 100,
                        fileProgressFraction = 1f,
                        fileUploadedBytes = totalSize,
                        fileTotalBytes = totalSize,
                        fileStatus = "",
                        fileResultCode = code,
                        fileResultName = displayName,
                        fileResultExpiresAt = expiresAt,
                        fileResultIsR2 = false,
                    )
                }
                showToast("Uploaded via $actualProviderName!", ToastType.SUCCESS)
                offerDropToLinkedGroup(
                    DropCreatedOffer(
                        kind = "file",
                        name = displayName,
                        provider = actualProviderName,
                        sizeBytes = totalSize,
                        dropCode = code,
                        expiresAt = expiresAt,
                        payload = dropJson,
                    ),
                )
            } catch (e: Throwable) {
                if (!token.isCancelled() && e.message != "UPLOAD_CANCELED") {
                    showToast(e.message?.take(140) ?: "Upload failed", ToastType.ERROR)
                }
                _sendState.update {
                    it.copy(
                        fileUploading = false,
                        fileProgress = 0,
                        fileProgressFraction = 0f,
                        fileUploadedBytes = 0L,
                        fileStatus = "",
                    )
                }
            } finally {
                fileCancelToken = null
            }
        }
    }

    private fun uploadFilesToR2() {
        val st = _sendState.value
        val totalSize = st.pickedFiles.sumOf { it.size }
        val safeTotal = totalSize.coerceAtLeast(1L)
        val token = UploadCancellationToken()
        fileCancelToken = token
        _sendState.update {
            it.copy(
                fileUploading = true,
                fileProgress = 0,
                fileProgressFraction = 0f,
                fileUploadedBytes = 0L,
                fileTotalBytes = totalSize,
                fileStatus = "Preparing Secret R2...",
            )
        }

        viewModelScope.launch {
            try {
                val isMulti = st.pickedFiles.size > 1
                val entries = mutableListOf<JsonObject>()
                val fileBytes = LongArray(st.pickedFiles.size)
                var singleKey = ""
                val singleName = st.r2CustomName.trim().ifBlank { st.pickedFiles.first().name }

                st.pickedFiles.forEachIndexed { idx, picked ->
                    val targetName = if (!isMulti) singleName else picked.name
                    val prefix = if (isMulti) "[${idx + 1}/${st.pickedFiles.size}] " else ""
                    _sendState.update { it.copy(fileStatus = "${prefix}Streaming to Secret R2: $targetName") }
                    val key = r2Service.uploadToR2(
                        picked = picked,
                        customName = targetName,
                        cancelToken = token,
                        onProgress = { loaded, _, _ ->
                            fileBytes[idx] = loaded.coerceIn(0L, picked.size.coerceAtLeast(loaded))
                            val cumLoaded = fileBytes.sum().coerceIn(0L, safeTotal)
                            val frac = (cumLoaded.toFloat() / safeTotal.toFloat()).coerceIn(0f, 1f)
                            val pct = (frac * 100f).toInt().coerceIn(0, 100)
                            _sendState.update {
                                it.copy(
                                    fileProgress = pct,
                                    fileProgressFraction = frac,
                                    fileUploadedBytes = cumLoaded,
                                    fileTotalBytes = totalSize,
                                )
                            }
                        },
                    )
                    fileBytes[idx] = picked.size
                    if (!isMulti) singleKey = key
                    entries.add(
                        buildJsonObject {
                            put("objectKey", JsonPrimitive(key))
                            put("fileName", JsonPrimitive(targetName))
                            put("fileSize", JsonPrimitive(picked.size))
                            put("contentType", JsonPrimitive(picked.mimeType))
                        },
                    )
                }

                _sendState.update {
                    it.copy(
                        fileProgress = 100,
                        fileProgressFraction = 1f,
                        fileUploadedBytes = totalSize,
                        fileStatus = "Finalizing Secret R2 drop...",
                    )
                }
                val code = generateUniqueDropCode()
                val expiresAt = System.currentTimeMillis() + parseExpireDurationMs(st.fileExpire)
                val displayName = if (isMulti) "${entries.size} files" else singleName

                val dropJson = buildJsonObject {
                    put("type", JsonPrimitive("r2_file"))
                    put("fileName", JsonPrimitive(displayName))
                    put("fileSize", JsonPrimitive(totalSize))
                    put("contentType", JsonPrimitive(if (isMulti) "application/octet-stream" else st.pickedFiles.first().mimeType))
                    put("description", JsonPrimitive(st.r2Description.trim()))
                    put("storageProvider", JsonPrimitive("hefimer-r2"))
                    put("isSecretR2", JsonPrimitive(true))
                    put("expiresAt", JsonPrimitive(expiresAt))
                    put("timestamp", FirebaseRtdbClient.serverTimestamp())
                    if (isMulti) put("files", JsonArray(entries)) else put("objectKey", JsonPrimitive(singleKey))
                    if (st.filePassword.isNotBlank()) put("password", JsonPrimitive(st.filePassword.trim()))
                    if (st.fileOneTime) put("isOneTime", JsonPrimitive(true))
                }

                rtdb.set("drops/$code", dropJson)
                _r2Stats.value = storage.recordUploadStats(totalSize, true)
                recordHistory(
                    HistoryItem(
                        code = code,
                        type = "r2_file",
                        action = "sent",
                        fileName = displayName,
                        fileSize = totalSize,
                        isR2 = true,
                    ),
                )
                _sendState.update {
                    it.copy(
                        fileUploading = false,
                        fileProgress = 100,
                        fileProgressFraction = 1f,
                        fileUploadedBytes = totalSize,
                        fileTotalBytes = totalSize,
                        fileStatus = "",
                        fileResultCode = code,
                        fileResultName = displayName,
                        fileResultExpiresAt = expiresAt,
                        fileResultIsR2 = true,
                    )
                }
                showToast("Uploaded to Secret Cloudflare R2!", ToastType.SUCCESS)
            } catch (e: Throwable) {
                if (!token.isCancelled() && e.message != "UPLOAD_CANCELED") {
                    showToast(e.message?.take(140) ?: "Secret R2 upload failed", ToastType.ERROR)
                }
                _sendState.update {
                    it.copy(
                        fileUploading = false,
                        fileProgress = 0,
                        fileProgressFraction = 0f,
                        fileUploadedBytes = 0L,
                        fileStatus = "",
                    )
                }
            } finally {
                fileCancelToken = null
            }
        }
    }

    fun setTextContent(value: String) = _sendState.update { it.copy(textContent = value) }
    fun setManualLangId(id: String?) = _sendState.update { it.copy(manualLangId = id) }
    fun setTextExpire(value: String) = _sendState.update { it.copy(textExpire = value) }
    fun setTextPassword(value: String) = _sendState.update { it.copy(textPassword = value) }
    fun toggleTextOneTime() = _sendState.update { it.copy(textOneTime = !it.textOneTime) }

    fun resetTextResult(clearContent: Boolean) {
        _sendState.update {
            it.copy(
                textContent = if (clearContent) "" else it.textContent,
                textResultCode = "",
                textResultExpiresAt = null,
                textPassword = "",
                textOneTime = false,
            )
        }
    }

    fun generateTextDrop() {
        val st = _sendState.value
        if (st.textContent.isBlank()) {
            showToast("Please enter some text or code", ToastType.ERROR)
            return
        }
        _sendState.update { it.copy(textLoading = true) }
        viewModelScope.launch {
            try {
                val code = generateUniqueDropCode()
                val expiresAt = System.currentTimeMillis() + parseExpireDurationMs(st.textExpire)
                val dropJson = buildJsonObject {
                    put("text", JsonPrimitive(st.textContent))
                    put("type", JsonPrimitive("text"))
                    put("expire", JsonPrimitive(st.textExpire))
                    put("expiresAt", JsonPrimitive(expiresAt))
                    put("timestamp", FirebaseRtdbClient.serverTimestamp())
                    if (st.textPassword.isNotBlank()) put("password", JsonPrimitive(st.textPassword.trim()))
                    if (st.textOneTime) put("isOneTime", JsonPrimitive(true))
                }
                rtdb.set("drops/$code", dropJson)

                recordHistory(HistoryItem(code = code, type = "text", action = "sent"))
                _sendState.update {
                    it.copy(textLoading = false, textResultCode = code, textResultExpiresAt = expiresAt)
                }
                showToast("Drop code $code generated!", ToastType.SUCCESS)

                val lang = st.manualLangId?.let { id ->
                    CodeHighlighter.SUPPORTED_LANGUAGES.firstOrNull { it.id == id }
                } ?: CodeHighlighter.detectLanguage(st.textContent)
                offerDropToLinkedGroup(
                    DropCreatedOffer(
                        kind = "text",
                        name = if (lang.id == "text") "Shared text" else "${lang.label} snippet",
                        provider = "Hefimer",
                        sizeBytes = st.textContent.toByteArray().size.toLong(),
                        dropCode = code,
                        expiresAt = expiresAt,
                        payload = dropJson,
                    ),
                )
            } catch (e: Throwable) {
                _sendState.update { it.copy(textLoading = false) }
                showToast(e.message ?: "Failed to generate code", ToastType.ERROR)
            }
        }
    }

    private suspend fun generateUniqueDropCode(): String {
        repeat(6) {
            val candidate = random5Digit()
            val existing = rtdb.get("drops/$candidate")
            if (existing == null) return candidate
        }
        throw IllegalStateException("Could not generate a unique 5-digit code")
    }
    // endregion

    // region RECEIVE TAB
    fun setReceiveCode(value: String) {
        val cleaned = value.filter { it.isDigit() }.take(5)
        _receiveState.update { it.copy(code = cleaned) }
    }

    fun resetReceive() {
        _receiveState.update { ReceiveUiState() }
    }

    fun dismissReceivePasswordPrompt() {
        _receiveState.update { it.copy(passwordPromptOpen = false, pendingDrop = null) }
    }

    fun unlockReceiveWithPassword(inputPassword: String) {
        val pending = _receiveState.value.pendingDrop ?: return
        if (inputPassword != pending.password) {
            showToast("Incorrect password", ToastType.ERROR)
            return
        }
        finalizeReceivedDrop(pending)
    }

    fun retrieveDrop(codeOverride: String? = null) {
        val targetCode = (codeOverride ?: _receiveState.value.code).filter { it.isDigit() }.take(5)
        if (targetCode.length != 5) {
            showToast("Please enter exactly 5 digits", ToastType.ERROR)
            return
        }
        _receiveState.update { it.copy(code = targetCode, loading = true) }

        viewModelScope.launch {
            try {
                val raw = rtdb.get("drops/$targetCode") as? JsonObject
                if (raw == null) {
                    _receiveState.update { it.copy(loading = false) }
                    showToast("Code not found", ToastType.ERROR)
                    return@launch
                }
                val parsed = normalizeDropItem(targetCode, raw)
                if (parsed.expiresAt > 0L && parsed.expiresAt < System.currentTimeMillis()) {
                    _receiveState.update { it.copy(loading = false) }
                    showToast("Code has expired", ToastType.ERROR)
                    return@launch
                }
                if (!parsed.password.isNullOrBlank()) {
                    _receiveState.update {
                        it.copy(loading = false, passwordPromptOpen = true, pendingDrop = parsed)
                    }
                    return@launch
                }
                finalizeReceivedDrop(parsed)
            } catch (e: Throwable) {
                _receiveState.update { it.copy(loading = false) }
                showToast(e.message ?: "Failed to retrieve drop", ToastType.ERROR)
            }
        }
    }

    private fun finalizeReceivedDrop(drop: DropItem) {
        _receiveState.update {
            it.copy(loading = false, passwordPromptOpen = false, pendingDrop = null, result = drop)
        }
        recordHistory(
            HistoryItem(
                code = drop.code,
                type = drop.type.ifBlank { "text" },
                action = "received",
                fileName = drop.fileName,
                isR2 = drop.isR2Drop,
            ),
        )
        showToast("Drop retrieved!", ToastType.SUCCESS)

        // Notify Hefimer Link if this drop matched an incoming transfer
        markLinkedTransferReceived(drop.code)

        // Perform one-time self-destruct cleanup matching web ReceiveResult
        if (drop.isOneTime) {
            viewModelScope.launch {
                runCatching {
                    drop.objectKey?.takeIf { it.isNotBlank() }?.let { r2Service.deleteObject(it) }
                    drop.files?.forEach { f ->
                        if (f.objectKey.isNotBlank()) r2Service.deleteObject(f.objectKey)
                    }
                    rtdb.remove("drops/${drop.code}")
                    showToast("This drop self-destructed after viewing", ToastType.INFO)
                }
            }
        }
    }

    private fun normalizeDropItem(code: String, raw: JsonObject): DropItem {
        var item = runCatching {
            json.decodeFromJsonElement(DropItem.serializer(), raw)
        }.getOrDefault(DropItem()).copy(code = code)

        val textVal = item.text
        if (!textVal.isNullOrBlank() && textVal.startsWith("__HEFIMER_FILE__")) {
            runCatching {
                val obj = json.parseToJsonElement(textVal.removePrefix("__HEFIMER_FILE__")).jsonObject
                if (obj["isDirectFile"]?.jsonPrimitive?.booleanOrNull == true) {
                    item = item.copy(
                        type = "file",
                        fileName = obj["fileName"]?.jsonPrimitive?.contentOrNull ?: item.fileName,
                        fileUrl = obj["fileUrl"]?.jsonPrimitive?.contentOrNull ?: item.fileUrl,
                        objectKey = obj["objectKey"]?.jsonPrimitive?.contentOrNull ?: item.objectKey,
                        fileSize = obj["fileSize"]?.jsonPrimitive?.longOrNull ?: item.fileSize,
                        isSecretR2 = obj["isSecretR2"]?.jsonPrimitive?.booleanOrNull ?: item.isSecretR2,
                        text = null,
                    )
                }
            }
        } else if (!textVal.isNullOrBlank() && textVal.startsWith("__HEFIMER_FILES__")) {
            runCatching {
                val arr = json.parseToJsonElement(textVal.removePrefix("__HEFIMER_FILES__")).jsonArray
                val list = arr.map { json.decodeFromJsonElement(DropFileEntry.serializer(), it) }
                item = item.copy(files = list, text = null)
            }
        }
        return item
    }

    fun cancelActiveDownload() {
        activeDownloadToken?.cancel()
        activeDownloadToken = null
        _downloadState.value = null
        showToast("Download canceled", ToastType.INFO)
    }

    fun downloadDropFile(entry: DropFileEntry) {
        viewModelScope.launch {
            downloadSingleDropEntry(entry)
        }
    }

    private suspend fun downloadSingleDropEntry(entry: DropFileEntry) {
        val token = DownloadCancellationToken()
        activeDownloadToken = token
        val knownSize = entry.resolvedSize.takeIf { it > 0L } ?: -1L
        _downloadState.value = DownloadProgressState(
            fileName = entry.resolvedName,
            loadedBytes = 0L,
            totalBytes = knownSize,
            percent = 0,
        )
        try {
            if (entry.objectKey.isNotBlank()) {
                val url = r2Service.getDownloadUrl(entry.objectKey)
                DownloadHelper.streamDirectUrlToDownloads(
                    context = getApplication(),
                    url = url,
                    fileName = entry.resolvedName,
                    cancelToken = token,
                    onProgress = { loaded, total, pct ->
                        val effectiveTotal = if (total > 0L) total else knownSize
                        val effectivePct = if (effectiveTotal > 0L) {
                            ((loaded * 100L) / effectiveTotal).toInt().coerceIn(0, 100)
                        } else {
                            pct
                        }
                        _downloadState.value = DownloadProgressState(
                            fileName = entry.resolvedName,
                            loadedBytes = loaded,
                            totalBytes = effectiveTotal,
                            percent = effectivePct,
                        )
                    },
                )
                showToast("Saved ${entry.resolvedName} to Downloads", ToastType.SUCCESS)
            } else {
                val url = entry.resolvedUrl
                if (!url.startsWith("http", ignoreCase = true)) {
                    showToast("Invalid or missing file link", ToastType.ERROR)
                    return
                }
                val mode = DownloadHelper.downloadFile(
                    context = getApplication(),
                    url = url,
                    fileName = entry.resolvedName,
                    cancelToken = token,
                    onProgress = { loaded, total, pct ->
                        val effectiveTotal = if (total > 0L) total else knownSize
                        val effectivePct = if (effectiveTotal > 0L) {
                            ((loaded * 100L) / effectiveTotal).toInt().coerceIn(0, 100)
                        } else {
                            pct
                        }
                        _downloadState.value = DownloadProgressState(
                            fileName = entry.resolvedName,
                            loadedBytes = loaded,
                            totalBytes = effectiveTotal,
                            percent = effectivePct,
                        )
                    },
                )
                showToast(
                    if (mode == "tab") "Opening in browser..." else "Saved ${entry.resolvedName} to Downloads",
                    ToastType.SUCCESS,
                )
            }
        } catch (e: Throwable) {
            if (!token.isCancelled() && e.message != "DOWNLOAD_CANCELED") {
                showToast(e.message?.take(140) ?: "Download failed", ToastType.ERROR)
            }
        } finally {
            if (activeDownloadToken === token) {
                activeDownloadToken = null
                _downloadState.value = null
            }
        }
    }

    fun downloadAllDropFiles(files: List<DropFileEntry>) {
        viewModelScope.launch {
            showToast("Downloading ${files.size} files...", ToastType.INFO)
            files.forEachIndexed { idx, entry ->
                downloadSingleDropEntry(entry)
                if (idx < files.lastIndex) delay(250L)
            }
        }
    }
    // endregion

    // region SPACE (Collaborative Live Canvas)
    fun setSpaceUserName(name: String) {
        storage.setUserName(name)
        _spaceState.update { it.copy(userName = name) }
        _roomsState.update { it.copy(userName = name) }
    }

    fun setSpaceLobbyMode(mode: String) = _spaceState.update { it.copy(lobbyMode = mode) }
    fun setSpaceJoinCode(code: String) = _spaceState.update {
        it.copy(joinCode = code.filter { c -> c.isDigit() }.take(5), joinRequiresPassword = false, joinPassword = "")
    }
    fun setSpaceJoinPassword(pw: String) = _spaceState.update { it.copy(joinPassword = pw) }
    fun setSpaceCreateTitle(t: String) = _spaceState.update { it.copy(createTitle = t) }
    fun setSpaceCreateExpire(e: String) = _spaceState.update { it.copy(createExpire = e) }
    fun setSpaceCreatePassword(pw: String) = _spaceState.update { it.copy(createPassword = pw) }

    fun setSpaceCreateCode(raw: String) {
        val clean = raw.filter { it.isDigit() }.take(5)
        _spaceState.update { it.copy(createCode = clean) }
        checkSpaceCodeAvailability(clean)
    }

    fun randomizeSpaceCode() = setSpaceCreateCode(random5Digit())

    fun selectSpaceProvider(id: String) {
        storage.setSpaceProvider(id)
        _spaceState.update { it.copy(selectedProviderId = id) }
    }

    private fun checkSpaceCodeAvailability(code: String) {
        codeCheckJob?.cancel()
        if (code.length != 5) {
            _spaceState.update { it.copy(createCodeStatus = "idle") }
            return
        }
        codeCheckJob = viewModelScope.launch {
            delay(280L)
            _spaceState.update { it.copy(createCodeStatus = "checking") }
            val existing = runCatching { rtdb.get("spaces/$code") as? JsonObject }.getOrNull()
            val status = if (existing != null) {
                val exp = existing["expiresAt"]?.jsonPrimitive?.longOrNull ?: 0L
                if (exp in 1 until System.currentTimeMillis()) "available" else "taken"
            } else {
                "available"
            }
            _spaceState.update { it.copy(createCodeStatus = status) }
        }
    }

    fun createSpace() {
        val st = _spaceState.value
        val name = st.userName.trim()
        val title = st.createTitle.trim()
        if (name.isBlank()) { showToast("Please enter your display name", ToastType.ERROR); return }
        if (title.isBlank()) { showToast("Please enter a space title", ToastType.ERROR); return }
        if (st.createCode.length != 5) { showToast("Code must be 5 digits", ToastType.ERROR); return }
        if (st.createCodeStatus == "taken") { showToast("Space code is already taken", ToastType.ERROR); return }

        _spaceState.update { it.copy(loading = true) }
        viewModelScope.launch {
            try {
                val expiresAt = System.currentTimeMillis() + parseExpireDurationMs(st.createExpire)
                val payload = buildJsonObject {
                    put("createdAt", FirebaseRtdbClient.serverTimestamp())
                    put("creator", JsonPrimitive(name))
                    put("name", JsonPrimitive(title))
                    put("expiresAt", JsonPrimitive(expiresAt))
                    if (st.createPassword.isNotBlank()) put("password", JsonPrimitive(st.createPassword.trim()))
                }
                rtdb.set("spaces/${st.createCode}", payload)
                val recents = storage.saveRecentSpace(st.createCode, title)
                recordHistory(HistoryItem(code = st.createCode, type = "space", action = "created", roomName = title))
                _spaceState.update {
                    it.copy(
                        loading = false,
                        createdCode = st.createCode,
                        createdName = title,
                        createdExpiresAt = expiresAt,
                        recentSpaces = recents,
                    )
                }
                showToast("Space created!", ToastType.SUCCESS)
            } catch (e: Throwable) {
                _spaceState.update { it.copy(loading = false) }
                showToast(e.message ?: "Failed to create space", ToastType.ERROR)
            }
        }
    }

    fun clearSpaceCreatedCard() {
        _spaceState.update { it.copy(createdCode = "", createdName = "", createdExpiresAt = null) }
        randomizeSpaceCode()
    }

    fun enterCreatedSpace() {
        val st = _spaceState.value
        if (st.createdCode.length != 5) return
        val code = st.createdCode
        val name = st.createdName
        val exp = st.createdExpiresAt ?: 0L
        _spaceState.update { it.copy(createdCode = "", createdName = "", createdExpiresAt = null) }
        startSpaceSession(code, name, exp)
    }

    fun joinSpace(codeOverride: String? = null) {
        val st = _spaceState.value
        val name = st.userName.trim().ifBlank { "Guest" }
        val target = (codeOverride ?: st.joinCode).filter { it.isDigit() }.take(5)
        if (target.length != 5) { showToast("Space code must be 5 digits", ToastType.ERROR); return }

        _spaceState.update { it.copy(loading = true, userName = name) }
        storage.setUserName(name)
        viewModelScope.launch {
            try {
                val obj = rtdb.get("spaces/$target") as? JsonObject
                if (obj == null) {
                    _spaceState.update { it.copy(loading = false) }
                    showToast("Space not found", ToastType.ERROR)
                    return@launch
                }
                val exp = obj["expiresAt"]?.jsonPrimitive?.longOrNull ?: 0L
                if (exp in 1 until System.currentTimeMillis()) {
                    _spaceState.update { it.copy(loading = false) }
                    showToast("Space has expired", ToastType.ERROR)
                    return@launch
                }
                val pw = obj["password"]?.jsonPrimitive?.contentOrNull
                if (!pw.isNullOrBlank()) {
                    if (!st.joinRequiresPassword || st.joinPassword.isBlank()) {
                        _spaceState.update { it.copy(loading = false, joinRequiresPassword = true, joinCode = target) }
                        showToast("This space requires a passcode", ToastType.INFO)
                        return@launch
                    }
                    if (st.joinPassword != pw) {
                        _spaceState.update { it.copy(loading = false) }
                        showToast("Incorrect space passcode", ToastType.ERROR)
                        return@launch
                    }
                }
                val sName = obj["name"]?.jsonPrimitive?.contentOrNull ?: "Space $target"
                val recents = storage.saveRecentSpace(target, sName)
                recordHistory(HistoryItem(code = target, type = "space", action = "joined", roomName = sName))
                _spaceState.update { it.copy(loading = false, recentSpaces = recents, joinRequiresPassword = false, joinPassword = "") }
                startSpaceSession(target, sName, exp)
            } catch (e: Throwable) {
                _spaceState.update { it.copy(loading = false) }
                showToast(e.message ?: "Failed to join space", ToastType.ERROR)
            }
        }
    }

    private fun startSpaceSession(code: String, name: String, expiresAt: Long) {
        spaceObserverJob?.cancel()
        val displayName = _spaceState.value.userName.trim().ifBlank { "Guest" }
        val userColor = getUserColor(displayName + clientId)

        _spaceState.update {
            it.copy(
                activeSpaceId = code,
                activeSpaceName = name,
                activeExpiresAt = expiresAt,
                items = emptyList(),
                members = emptyList(),
                cursors = emptyList(),
            )
        }
        openOverlay(AppOverlay.SpaceCanvas(code, name))

        spaceObserverJob = viewModelScope.launch {
            runCatching {
                rtdb.set(
                    "spaces/$code/members/$clientId",
                    buildJsonObject {
                        put("name", JsonPrimitive(displayName))
                        put("color", JsonPrimitive(userColor))
                        put("joinedAt", FirebaseRtdbClient.serverTimestamp())
                    },
                )
            }

            rtdb.observe("spaces/$code").collect { elem ->
                val obj = elem as? JsonObject
                if (obj == null) return@collect

                val exp = obj["expiresAt"]?.jsonPrimitive?.longOrNull ?: expiresAt
                val sName = obj["name"]?.jsonPrimitive?.contentOrNull ?: name

                val byId = linkedMapOf<String, SpaceItem>()
                (obj["files"] as? JsonObject)?.forEach { (id, v) ->
                    val vo = v as? JsonObject ?: return@forEach
                    byId[id] = parseLegacySpaceFile(id, vo)
                }
                (obj["items"] as? JsonObject)?.forEach { (id, v) ->
                    val vo = v as? JsonObject ?: return@forEach
                    byId[id] = parseSpaceItem(id, vo)
                }

                val membersList = (obj["members"] as? JsonObject)?.mapNotNull { (id, v) ->
                    val vo = v as? JsonObject ?: return@mapNotNull null
                    SpaceMember(
                        id = id,
                        name = vo["name"]?.jsonPrimitive?.contentOrNull ?: "Guest",
                        color = vo["color"]?.jsonPrimitive?.contentOrNull ?: getUserColor(id),
                        joinedAt = vo["joinedAt"]?.jsonPrimitive?.longOrNull ?: 0L,
                    )
                }.orEmpty()

                val now = System.currentTimeMillis()
                val cursorsList = (obj["cursors"] as? JsonObject)?.mapNotNull { (id, v) ->
                    if (id == clientId) return@mapNotNull null
                    val vo = v as? JsonObject ?: return@mapNotNull null
                    val updated = vo["updatedAt"]?.jsonPrimitive?.longOrNull ?: 0L
                    if (now - updated > 20_000L) return@mapNotNull null
                    SpaceCursor(
                        id = id,
                        name = vo["name"]?.jsonPrimitive?.contentOrNull ?: "Guest",
                        color = vo["color"]?.jsonPrimitive?.contentOrNull ?: getUserColor(id),
                        x = vo["x"]?.jsonPrimitive?.floatOrNull ?: 0f,
                        y = vo["y"]?.jsonPrimitive?.floatOrNull ?: 0f,
                        updatedAt = updated,
                    )
                }.orEmpty()

                _spaceState.update {
                    it.copy(
                        activeSpaceName = sName,
                        activeExpiresAt = exp,
                        items = byId.values.sortedBy { item -> item.zIndex },
                        members = membersList,
                        cursors = cursorsList,
                    )
                }
            }
        }
    }

    fun leaveSpace() {
        val code = _spaceState.value.activeSpaceId
        spaceObserverJob?.cancel()
        spaceObserverJob = null
        if (code.isNotBlank()) {
            viewModelScope.launch(Dispatchers.IO) {
                runCatching { rtdb.remove("spaces/$code/members/$clientId") }
                runCatching { rtdb.remove("spaces/$code/cursors/$clientId") }
            }
        }
        _spaceState.update {
            it.copy(activeSpaceId = "", items = emptyList(), members = emptyList(), cursors = emptyList())
        }
    }

    fun spaceAddItem(kind: String, name: String, content: String?, url: String?, noteColor: String, x: Float, y: Float) {
        val code = _spaceState.value.activeSpaceId
        if (code.isBlank()) return
        val id = UUID.randomUUID().toString().replace("-", "").take(12)
        val nextZ = (_spaceState.value.items.maxOfOrNull { it.zIndex } ?: 1) + 1
        val author = _spaceState.value.userName.trim().ifBlank { "Guest" }
        val payload = buildJsonObject {
            put("kind", JsonPrimitive(kind))
            put("name", JsonPrimitive(name.ifBlank { if (kind == "note") "Sticky Note" else "Untitled" }))
            content?.let { put("content", JsonPrimitive(it)) }
            url?.let { put("url", JsonPrimitive(it)) }
            if (kind == "note") put("noteColor", JsonPrimitive(noteColor))
            put("uploadedBy", JsonPrimitive(author))
            put("timestamp", JsonPrimitive(System.currentTimeMillis()))
            put("x", JsonPrimitive(x))
            put("y", JsonPrimitive(y))
            put("zIndex", JsonPrimitive(nextZ))
        }
        viewModelScope.launch {
            runCatching { rtdb.set("spaces/$code/items/$id", payload) }
                .onSuccess { showToast("Placed on canvas", ToastType.SUCCESS) }
                .onFailure { showToast("Failed to add item", ToastType.ERROR) }
        }
    }

    fun spaceUpdateItem(item: SpaceItem, name: String, content: String?, url: String?, noteColor: String) {
        val code = _spaceState.value.activeSpaceId
        if (code.isBlank()) return
        val patch = buildJsonObject {
            put("name", JsonPrimitive(name))
            content?.let { put("content", JsonPrimitive(it)) }
            url?.let { put("url", JsonPrimitive(it)) }
            if (item.kind == "note") put("noteColor", JsonPrimitive(noteColor))
        }
        viewModelScope.launch {
            runCatching { rtdb.update("spaces/$code/${item.sourceNode}/${item.id}", patch) }
                .onSuccess { showToast("Updated item", ToastType.SUCCESS) }
        }
    }

    fun spaceMoveItem(item: SpaceItem, x: Float, y: Float, zIndex: Int? = null) {
        val code = _spaceState.value.activeSpaceId
        if (code.isBlank()) return
        // Optimistic local update for buttery 120Hz responsiveness
        _spaceState.update { st ->
            st.copy(
                items = st.items.map {
                    if (it.id == item.id) it.copy(x = x, y = y, zIndex = zIndex ?: it.zIndex) else it
                },
            )
        }
        val patch = buildJsonObject {
            put("x", JsonPrimitive(x))
            put("y", JsonPrimitive(y))
            zIndex?.let { put("zIndex", JsonPrimitive(it)) }
        }
        viewModelScope.launch(Dispatchers.IO) {
            runCatching { rtdb.update("spaces/$code/${item.sourceNode}/${item.id}", patch) }
        }
    }

    fun spaceDeleteItem(item: SpaceItem) {
        val code = _spaceState.value.activeSpaceId
        if (code.isBlank()) return
        viewModelScope.launch {
            runCatching { rtdb.remove("spaces/$code/${item.sourceNode}/${item.id}") }
                .onSuccess { showToast("Removed from space", ToastType.SUCCESS) }
        }
    }

    fun spaceAutoArrange() {
        val st = _spaceState.value
        val code = st.activeSpaceId
        if (code.isBlank() || st.items.isEmpty()) return
        val kindOrder = mapOf("file" to 0, "note" to 1, "link" to 2, "code" to 3)
        val sorted = st.items.sortedWith(compareBy({ kindOrder[it.kind] ?: 9 }, { it.timestamp }))
        val cols = kotlin.math.ceil(kotlin.math.sqrt(sorted.size.toDouble())).toInt().coerceIn(1, 3)
        val patchMap = mutableMapOf<String, JsonElement>()
        var y = 100f
        sorted.chunked(cols).forEach { row ->
            row.forEachIndexed { colIdx, item ->
                val x = 64f + colIdx * 280f
                patchMap["spaces/$code/${item.sourceNode}/${item.id}/x"] = JsonPrimitive(x)
                patchMap["spaces/$code/${item.sourceNode}/${item.id}/y"] = JsonPrimitive(y)
            }
            y += 230f
        }
        viewModelScope.launch {
            runCatching { rtdb.update("", JsonObject(patchMap)) }
                .onSuccess { showToast("Canvas auto-arranged", ToastType.SUCCESS) }
        }
    }

    fun spaceUploadFiles(uris: List<Uri>, startX: Float, startY: Float) {
        val st = _spaceState.value
        val code = st.activeSpaceId
        if (code.isBlank() || uris.isEmpty()) return
        val pickedList = uris.map { uploader.resolvePickedFile(it) }
        val token = UploadCancellationToken()
        spaceCancelToken = token
        val provName = UploadProviders.getConfig(st.selectedProviderId).name
        val tasks = pickedList.mapIndexed { i, f ->
            SpaceUploadTask("up_${System.currentTimeMillis()}_$i", f.name, 0, if (i == 0) "Starting..." else "Queued...", provName)
        }
        _spaceState.update { it.copy(uploadTasks = it.uploadTasks + tasks) }

        viewModelScope.launch {
            pickedList.forEachIndexed { idx, picked ->
                if (token.isCancelled()) return@forEachIndexed
                val taskId = tasks[idx].id
                fun patchTask(fn: (SpaceUploadTask) -> SpaceUploadTask) {
                    _spaceState.update { s -> s.copy(uploadTasks = s.uploadTasks.map { if (it.id == taskId) fn(it) else it }) }
                }
                try {
                    val res = uploader.uploadWithFallback(
                        primaryProviderId = _spaceState.value.selectedProviderId,
                        picked = picked,
                        expire = "24h",
                        cancelToken = token,
                        onProgress = { pct -> patchTask { it.copy(progress = pct) } },
                        onStatus = { msg -> patchTask { it.copy(status = msg) } },
                        onFallback = { from, to ->
                            patchTask { it.copy(providerName = to, status = "Switching to $to...") }
                            showToast("$from unavailable, switching to $to", ToastType.INFO)
                        },
                    )
                    val itemId = UUID.randomUUID().toString().replace("-", "").take(12)
                    val nextZ = (_spaceState.value.items.maxOfOrNull { it.zIndex } ?: 1) + 1 + idx
                    val payload = buildJsonObject {
                        put("kind", JsonPrimitive("file"))
                        put("name", JsonPrimitive(picked.name))
                        put("size", JsonPrimitive(picked.size))
                        put("mimeType", JsonPrimitive(picked.mimeType))
                        put("url", JsonPrimitive(res.fileUrl))
                        put("providerName", JsonPrimitive(res.providerName))
                        put("uploadedBy", JsonPrimitive(_spaceState.value.userName.ifBlank { "Guest" }))
                        put("timestamp", JsonPrimitive(System.currentTimeMillis()))
                        put("x", JsonPrimitive(startX + idx * 32f))
                        put("y", JsonPrimitive(startY + idx * 32f))
                        put("zIndex", JsonPrimitive(nextZ))
                    }
                    rtdb.set("spaces/$code/items/$itemId", payload)
                    showToast("Uploaded ${picked.name} via ${res.providerName}", ToastType.SUCCESS)
                } catch (e: Throwable) {
                    if (!token.isCancelled() && e.message != "UPLOAD_CANCELED") {
                        showToast(e.message ?: "Upload failed", ToastType.ERROR)
                    }
                } finally {
                    _spaceState.update { s -> s.copy(uploadTasks = s.uploadTasks.filterNot { it.id == taskId }) }
                }
            }
        }
    }

    fun cancelSpaceUpload() {
        spaceCancelToken?.cancel()
        spaceCancelToken = null
        _spaceState.update { it.copy(uploadTasks = emptyList()) }
    }

    private fun parseLegacySpaceFile(id: String, raw: JsonObject): SpaceItem = SpaceItem(
        id = id,
        kind = "file",
        name = raw["name"]?.jsonPrimitive?.contentOrNull ?: "Untitled file",
        x = raw["x"]?.jsonPrimitive?.floatOrNull ?: 100f,
        y = raw["y"]?.jsonPrimitive?.floatOrNull ?: 100f,
        zIndex = raw["zIndex"]?.jsonPrimitive?.intOrNull ?: 1,
        uploadedBy = raw["uploadedBy"]?.jsonPrimitive?.contentOrNull ?: "Anonymous",
        timestamp = raw["timestamp"]?.jsonPrimitive?.longOrNull ?: 0L,
        sourceNode = "files",
        url = raw["url"]?.jsonPrimitive?.contentOrNull,
        size = raw["size"]?.jsonPrimitive?.longOrNull ?: 0L,
        mimeType = raw["type"]?.jsonPrimitive?.contentOrNull ?: raw["mimeType"]?.jsonPrimitive?.contentOrNull,
        providerName = raw["providerName"]?.jsonPrimitive?.contentOrNull,
    )

    private fun parseSpaceItem(id: String, raw: JsonObject): SpaceItem {
        val kind = raw["kind"]?.jsonPrimitive?.contentOrNull
            ?.takeIf { it in setOf("file", "note", "link", "code") } ?: "file"
        return SpaceItem(
            id = id,
            kind = kind,
            name = raw["name"]?.jsonPrimitive?.contentOrNull ?: if (kind == "note") "Sticky Note" else "Untitled",
            x = raw["x"]?.jsonPrimitive?.floatOrNull ?: 100f,
            y = raw["y"]?.jsonPrimitive?.floatOrNull ?: 100f,
            zIndex = raw["zIndex"]?.jsonPrimitive?.intOrNull ?: 1,
            uploadedBy = raw["uploadedBy"]?.jsonPrimitive?.contentOrNull ?: "Anonymous",
            timestamp = raw["timestamp"]?.jsonPrimitive?.longOrNull ?: 0L,
            sourceNode = "items",
            url = raw["url"]?.jsonPrimitive?.contentOrNull,
            size = raw["size"]?.jsonPrimitive?.longOrNull,
            mimeType = raw["mimeType"]?.jsonPrimitive?.contentOrNull,
            providerName = raw["providerName"]?.jsonPrimitive?.contentOrNull,
            content = raw["content"]?.jsonPrimitive?.contentOrNull,
            noteColor = raw["noteColor"]?.jsonPrimitive?.contentOrNull ?: "zinc",
            language = raw["language"]?.jsonPrimitive?.contentOrNull,
        )
    }

    private fun getUserColor(seed: String): String {
        val palette = listOf("#34d399", "#38bdf8", "#fbbf24", "#f472b6", "#a78bfa", "#fb7185", "#2dd4bf", "#facc15")
        var h = 0
        for (ch in seed) h = h * 31 + ch.code
        return palette[kotlin.math.abs(h) % palette.size]
    }
    // endregion

    // region ROOMS (Live Chat & Collaborative Whiteboard)
    fun setRoomsSection(section: String) = _roomsState.update { it.copy(section = section) }
    fun setChatLobbyMode(mode: String) = _roomsState.update { it.copy(chatLobbyMode = mode) }
    fun setChatJoinCode(v: String) = _roomsState.update { it.copy(chatJoinCode = v.filter { c -> c.isDigit() }.take(5)) }
    fun setChatCreateName(v: String) = _roomsState.update { it.copy(chatCreateName = v) }
    fun setChatCreateExpire(v: String) = _roomsState.update { it.copy(chatCreateExpire = v) }
    fun setChatCreateCode(v: String) {
        val clean = v.filter { it.isDigit() }.take(5)
        _roomsState.update { it.copy(chatCreateCode = clean) }
        checkChatCodeAvailability(clean)
    }
    fun randomizeChatCode() = setChatCreateCode(random5Digit())

    private fun checkChatCodeAvailability(code: String) {
        chatCodeCheckJob?.cancel()
        if (code.length != 5) {
            _roomsState.update { it.copy(chatCreateCodeStatus = "idle") }
            return
        }
        chatCodeCheckJob = viewModelScope.launch {
            delay(280L)
            _roomsState.update { it.copy(chatCreateCodeStatus = "checking") }
            val existing = runCatching { rtdb.get("chats/$code") as? JsonObject }.getOrNull()
            val status = if (existing != null) {
                val exp = existing["expiresAt"]?.jsonPrimitive?.longOrNull ?: 0L
                if (exp in 1 until System.currentTimeMillis()) "available" else "taken"
            } else {
                "available"
            }
            _roomsState.update { it.copy(chatCreateCodeStatus = status) }
        }
    }

    fun createChatRoom() {
        val st = _roomsState.value
        val user = st.userName.trim()
        val name = st.chatCreateName.trim()
        if (user.isBlank()) { showToast("Please enter your name", ToastType.ERROR); return }
        if (name.isBlank()) { showToast("Please enter a room name", ToastType.ERROR); return }
        if (st.chatCreateCode.length != 5) { showToast("Room code must be 5 digits", ToastType.ERROR); return }
        if (st.chatCreateCodeStatus == "taken") { showToast("Room code already taken", ToastType.ERROR); return }

        _roomsState.update { it.copy(chatLoading = true) }
        viewModelScope.launch {
            try {
                val expiresAt = System.currentTimeMillis() + parseExpireDurationMs(st.chatCreateExpire)
                rtdb.set(
                    "chats/${st.chatCreateCode}",
                    buildJsonObject {
                        put("createdAt", FirebaseRtdbClient.serverTimestamp())
                        put("creator", JsonPrimitive(user))
                        put("name", JsonPrimitive(name))
                        put("expiresAt", JsonPrimitive(expiresAt))
                    },
                )
                recordHistory(HistoryItem(code = st.chatCreateCode, type = "room", action = "sent", roomName = name))
                _roomsState.update {
                    it.copy(
                        chatLoading = false,
                        chatCreatedCode = st.chatCreateCode,
                        chatCreatedName = name,
                        chatCreatedExpiresAt = expiresAt,
                    )
                }
                showToast("Room created!", ToastType.SUCCESS)
            } catch (e: Throwable) {
                _roomsState.update { it.copy(chatLoading = false) }
                showToast(e.message ?: "Failed to create room", ToastType.ERROR)
            }
        }
    }

    fun clearChatCreatedCard() {
        _roomsState.update { it.copy(chatCreatedCode = "", chatCreatedName = "", chatCreatedExpiresAt = null) }
        randomizeChatCode()
    }

    fun enterCreatedChatRoom() {
        val st = _roomsState.value
        if (st.chatCreatedCode.length != 5) return
        val code = st.chatCreatedCode
        val name = st.chatCreatedName
        val exp = st.chatCreatedExpiresAt ?: 0L
        _roomsState.update { it.copy(chatCreatedCode = "", chatCreatedName = "", chatCreatedExpiresAt = null) }
        startChatSession(code, name, exp)
    }

    fun joinChatRoom(codeOverride: String? = null) {
        val st = _roomsState.value
        val user = st.userName.trim()
        val target = (codeOverride ?: st.chatJoinCode).filter { it.isDigit() }.take(5)
        if (user.isBlank()) { showToast("Please enter your name", ToastType.ERROR); return }
        if (target.length != 5) { showToast("Room ID must be 5 digits", ToastType.ERROR); return }

        _roomsState.update { it.copy(chatLoading = true) }
        viewModelScope.launch {
            try {
                val obj = rtdb.get("chats/$target") as? JsonObject
                if (obj == null) {
                    _roomsState.update { it.copy(chatLoading = false) }
                    showToast("Room not found", ToastType.ERROR)
                    return@launch
                }
                val exp = obj["expiresAt"]?.jsonPrimitive?.longOrNull ?: 0L
                if (exp in 1 until System.currentTimeMillis()) {
                    _roomsState.update { it.copy(chatLoading = false) }
                    showToast("Room has expired", ToastType.ERROR)
                    return@launch
                }
                val rName = obj["name"]?.jsonPrimitive?.contentOrNull ?: "Chat $target"
                recordHistory(HistoryItem(code = target, type = "room", action = "received", roomName = rName))
                _roomsState.update { it.copy(chatLoading = false) }
                startChatSession(target, rName, exp)
            } catch (e: Throwable) {
                _roomsState.update { it.copy(chatLoading = false) }
                showToast(e.message ?: "Failed to join room", ToastType.ERROR)
            }
        }
    }

    private fun startChatSession(roomId: String, roomName: String, expiresAt: Long) {
        chatObserverJob?.cancel()
        val memberKey = "and_${clientId}"
        activeChatMemberKey = memberKey
        _roomsState.update {
            it.copy(
                activeChatId = roomId,
                activeChatName = roomName,
                activeChatExpiresAt = expiresAt,
                chatMessages = emptyList(),
                chatMemberCount = 1,
            )
        }
        openOverlay(AppOverlay.ChatRoom(roomId, roomName))

        chatObserverJob = viewModelScope.launch {
            runCatching {
                rtdb.set(
                    "chats/$roomId/members/$memberKey",
                    buildJsonObject {
                        put("name", JsonPrimitive(_roomsState.value.userName.ifBlank { "Guest" }))
                        put("joinedAt", FirebaseRtdbClient.serverTimestamp())
                    },
                )
            }
            rtdb.observe("chats/$roomId").collect { elem ->
                val obj = elem as? JsonObject ?: return@collect
                val exp = obj["expiresAt"]?.jsonPrimitive?.longOrNull ?: expiresAt
                val name = obj["name"]?.jsonPrimitive?.contentOrNull ?: roomName
                val membersCount = (obj["members"] as? JsonObject)?.size ?: 1
                val msgs = (obj["messages"] as? JsonObject)?.mapNotNull { (id, v) ->
                    val vo = v as? JsonObject ?: return@mapNotNull null
                    val reactions = (vo["reactions"] as? JsonObject)?.mapNotNull { (rk, rv) ->
                        val count = rv.jsonPrimitive.intOrNull ?: return@mapNotNull null
                        rk to count
                    }?.toMap().orEmpty()
                    ChatMessage(
                        id = id,
                        sender = vo["sender"]?.jsonPrimitive?.contentOrNull ?: "Guest",
                        senderId = vo["senderId"]?.jsonPrimitive?.contentOrNull.orEmpty(),
                        text = vo["text"]?.jsonPrimitive?.contentOrNull.orEmpty(),
                        timestamp = vo["timestamp"]?.jsonPrimitive?.longOrNull ?: 0L,
                        isAdmin = vo["isAdmin"]?.jsonPrimitive?.booleanOrNull ?: false,
                        reactions = reactions,
                    )
                }?.sortedBy { it.timestamp }.orEmpty()

                _roomsState.update {
                    it.copy(
                        activeChatName = name,
                        activeChatExpiresAt = exp,
                        chatMessages = msgs,
                        chatMemberCount = membersCount.coerceAtLeast(1),
                    )
                }
            }
        }
    }

    fun sendChatMessage(text: String) {
        val clean = text.trim()
        val roomId = _roomsState.value.activeChatId
        if (clean.isBlank() || roomId.isBlank()) return
        val sender = _roomsState.value.userName.ifBlank { "Guest" }
        viewModelScope.launch {
            runCatching {
                rtdb.push(
                    "chats/$roomId/messages",
                    buildJsonObject {
                        put("sender", JsonPrimitive(sender))
                        put("senderId", JsonPrimitive(clientId))
                        put("text", JsonPrimitive(clean))
                        put("timestamp", JsonPrimitive(System.currentTimeMillis()))
                        put("isAdmin", JsonPrimitive(false))
                    },
                )
            }.onFailure {
                showToast("Failed to send message", ToastType.ERROR)
            }
        }
    }

    fun addChatReaction(msgId: String, reactionId: String) {
        val roomId = _roomsState.value.activeChatId
        if (roomId.isBlank()) return
        viewModelScope.launch(Dispatchers.IO) {
            runCatching {
                rtdb.incrementInt("chats/$roomId/messages/$msgId/reactions/$reactionId", 1)
            }
        }
    }

    fun leaveChatRoom() {
        val roomId = _roomsState.value.activeChatId
        val key = activeChatMemberKey
        chatObserverJob?.cancel()
        chatObserverJob = null
        if (roomId.isNotBlank() && !key.isNullOrBlank()) {
            viewModelScope.launch(Dispatchers.IO) {
                runCatching { rtdb.remove("chats/$roomId/members/$key") }
            }
        }
        _roomsState.update { it.copy(activeChatId = "", chatMessages = emptyList()) }
    }

    // Whiteboard
    fun setBoardLobbyMode(mode: String) = _roomsState.update { it.copy(boardLobbyMode = mode) }
    fun setBoardJoinCode(v: String) = _roomsState.update { it.copy(boardJoinCode = v.filter { c -> c.isDigit() }.take(5)) }
    fun setBoardCreateName(v: String) = _roomsState.update { it.copy(boardCreateName = v) }
    fun setBoardCreateExpire(v: String) = _roomsState.update { it.copy(boardCreateExpire = v) }

    fun createBoard() {
        val st = _roomsState.value
        val user = st.userName.trim()
        val name = st.boardCreateName.trim()
        if (user.isBlank() || name.isBlank()) {
            showToast("Please enter board name and your name", ToastType.ERROR)
            return
        }
        _roomsState.update { it.copy(boardLoading = true) }
        viewModelScope.launch {
            try {
                val id = random5Digit()
                val expiresAt = System.currentTimeMillis() + parseExpireDurationMs(st.boardCreateExpire)
                rtdb.set(
                    "boards/$id",
                    buildJsonObject {
                        put("name", JsonPrimitive(name))
                        put("creator", JsonPrimitive(user))
                        put("expiresAt", JsonPrimitive(expiresAt))
                        put("createdAt", FirebaseRtdbClient.serverTimestamp())
                    },
                )
                recordHistory(HistoryItem(code = id, type = "board", action = "created", boardName = name))
                _roomsState.update {
                    it.copy(
                        boardLoading = false,
                        boardCreatedCode = id,
                        boardCreatedName = name,
                        boardCreatedExpiresAt = expiresAt,
                    )
                }
                showToast("Board created!", ToastType.SUCCESS)
            } catch (e: Throwable) {
                _roomsState.update { it.copy(boardLoading = false) }
                showToast(e.message ?: "Failed to create board", ToastType.ERROR)
            }
        }
    }

    fun clearBoardCreatedCard() {
        _roomsState.update { it.copy(boardCreatedCode = "", boardCreatedName = "", boardCreatedExpiresAt = null) }
    }

    fun enterCreatedBoard() {
        val st = _roomsState.value
        if (st.boardCreatedCode.length != 5) return
        val id = st.boardCreatedCode
        val name = st.boardCreatedName
        val exp = st.boardCreatedExpiresAt ?: 0L
        _roomsState.update { it.copy(boardCreatedCode = "", boardCreatedName = "", boardCreatedExpiresAt = null) }
        startBoardSession(id, name, exp)
    }

    fun joinBoard(codeOverride: String? = null) {
        val st = _roomsState.value
        val user = st.userName.trim()
        val target = (codeOverride ?: st.boardJoinCode).filter { it.isDigit() }.take(5)
        if (user.isBlank()) { showToast("Please enter your name", ToastType.ERROR); return }
        if (target.length != 5) { showToast("Board ID must be 5 digits", ToastType.ERROR); return }

        _roomsState.update { it.copy(boardLoading = true) }
        viewModelScope.launch {
            try {
                val obj = rtdb.get("boards/$target") as? JsonObject
                if (obj == null) {
                    _roomsState.update { it.copy(boardLoading = false) }
                    showToast("Board not found", ToastType.ERROR)
                    return@launch
                }
                val exp = obj["expiresAt"]?.jsonPrimitive?.longOrNull ?: 0L
                if (exp in 1 until System.currentTimeMillis()) {
                    _roomsState.update { it.copy(boardLoading = false) }
                    showToast("Board has expired", ToastType.ERROR)
                    return@launch
                }
                val bName = obj["name"]?.jsonPrimitive?.contentOrNull ?: "Board $target"
                recordHistory(HistoryItem(code = target, type = "board", action = "joined", boardName = bName))
                _roomsState.update { it.copy(boardLoading = false) }
                startBoardSession(target, bName, exp)
            } catch (e: Throwable) {
                _roomsState.update { it.copy(boardLoading = false) }
                showToast(e.message ?: "Failed to join board", ToastType.ERROR)
            }
        }
    }

    private fun startBoardSession(boardId: String, boardName: String, expiresAt: Long) {
        boardObserverJob?.cancel()
        _roomsState.update {
            it.copy(activeBoardId = boardId, activeBoardName = boardName, activeBoardExpiresAt = expiresAt, boardStrokes = emptyList())
        }
        openOverlay(AppOverlay.Whiteboard(boardId, boardName))

        boardObserverJob = viewModelScope.launch {
            rtdb.observe("board_paths/$boardId").collect { elem ->
                val obj = elem as? JsonObject
                if (obj == null) {
                    _roomsState.update { it.copy(boardStrokes = emptyList()) }
                    return@collect
                }
                val strokes = obj.mapNotNull { (id, v) ->
                    val vo = v as? JsonObject ?: return@mapNotNull null
                    val pts = (vo["points"] as? JsonArray)?.mapNotNull { pe ->
                        val po = pe as? JsonObject ?: return@mapNotNull null
                        val x = po["x"]?.jsonPrimitive?.floatOrNull ?: return@mapNotNull null
                        val y = po["y"]?.jsonPrimitive?.floatOrNull ?: return@mapNotNull null
                        BoardPoint(x, y)
                    }.orEmpty()
                    if (pts.isEmpty()) return@mapNotNull null
                    BoardStroke(
                        id = id,
                        color = vo["color"]?.jsonPrimitive?.contentOrNull ?: "#ffffff",
                        size = vo["size"]?.jsonPrimitive?.floatOrNull ?: 4f,
                        points = pts,
                        timestamp = vo["timestamp"]?.jsonPrimitive?.longOrNull ?: 0L,
                    )
                }.sortedBy { it.timestamp }
                _roomsState.update { it.copy(boardStrokes = strokes) }
            }
        }
    }

    fun pushBoardStroke(colorHex: String, strokeWidth: Float, points: List<BoardPoint>) {
        val boardId = _roomsState.value.activeBoardId
        if (boardId.isBlank() || points.isEmpty()) return
        val localStroke = BoardStroke(
            id = "local_${System.currentTimeMillis()}",
            color = colorHex,
            size = strokeWidth,
            points = points,
            timestamp = System.currentTimeMillis(),
        )
        _roomsState.update { it.copy(boardStrokes = it.boardStrokes + localStroke) }
        viewModelScope.launch(Dispatchers.IO) {
            runCatching {
                rtdb.push(
                    "board_paths/$boardId",
                    buildJsonObject {
                        put("color", JsonPrimitive(colorHex))
                        put("size", JsonPrimitive(strokeWidth))
                        put(
                            "points",
                            buildJsonArray {
                                points.forEach { p ->
                                    add(buildJsonObject {
                                        put("x", JsonPrimitive(p.x))
                                        put("y", JsonPrimitive(p.y))
                                    })
                                }
                            },
                        )
                        put("timestamp", FirebaseRtdbClient.serverTimestamp())
                    },
                )
            }
        }
    }

    fun clearBoard() {
        val boardId = _roomsState.value.activeBoardId
        if (boardId.isBlank()) return
        viewModelScope.launch {
            runCatching { rtdb.remove("board_paths/$boardId") }
                .onSuccess { showToast("Board cleared", ToastType.SUCCESS) }
                .onFailure { showToast("Failed to clear board", ToastType.ERROR) }
        }
    }

    fun leaveBoard() {
        boardObserverJob?.cancel()
        boardObserverJob = null
        _roomsState.update { it.copy(activeBoardId = "", boardStrokes = emptyList()) }
    }
    // endregion

    // region HEFIMER LINK (Paired Devices)
    private fun startLinkPolling() {
        viewModelScope.launch {
            if (storage.isDeviceLinkEnabled()) {
                refreshLink(silent = true)
            }
            while (isActive) {
                delay(4_500L)
                if (storage.isDeviceLinkEnabled() || _selectedTab.value == MainTab.LINK) {
                    refreshLink(silent = true)
                }
            }
        }
    }

    fun refreshLink(silent: Boolean = false) {
        if (linkSyncInFlight) return
        linkSyncInFlight = true
        if (!silent) {
            _linkState.update {
                it.copy(
                    loading = it.sync.device.id.isBlank(),
                    busyKey = if (it.sync.device.id.isNotBlank()) "refresh" else "",
                )
            }
        }
        viewModelScope.launch {
            try {
                deviceApi.ensureRegistered()
                val next = deviceApi.sync()
                val autoDownloads = if (firstLinkSync) {
                    emptyList()
                } else {
                    next.incoming.filter { tr ->
                        next.group?.autoApprove == true &&
                            tr.kind == "file" &&
                            tr.recipientStatus == "approved" &&
                            tr.id !in observedIncomingIds
                    }
                }
                next.incoming.forEach { observedIncomingIds.add(it.id) }
                firstLinkSync = false

                _linkState.update {
                    it.copy(
                        loading = false,
                        busyKey = if (it.busyKey == "refresh") "" else it.busyKey,
                        error = "",
                        sync = next,
                        deviceNameInput = if (it.deviceNameInput.isBlank() || it.sync.device.id.isBlank()) next.device.name else it.deviceNameInput,
                    )
                }

                for (transfer in autoDownloads) {
                    launch { executeAutoDownload(transfer) }
                }
            } catch (e: Throwable) {
                _linkState.update {
                    it.copy(
                        loading = false,
                        busyKey = if (it.busyKey == "refresh") "" else it.busyKey,
                        error = e.message ?: "Device service is unavailable",
                    )
                }
            } finally {
                linkSyncInFlight = false
            }
        }
    }

    private suspend fun executeAutoDownload(transfer: DeviceTransfer) {
        try {
            deviceApi.updateTransferStatus(transfer.id, "downloading")
            val payload = transfer.payload
            val filesArr = payload?.get("files") as? JsonArray
            if (filesArr != null && filesArr.isNotEmpty()) {
                for (elem in filesArr) {
                    val obj = elem as? JsonObject ?: continue
                    val url = listOf("fileUrl", "url", "raw_url", "downloadUrl")
                        .firstNotNullOfOrNull { obj[it]?.jsonPrimitive?.contentOrNull?.takeIf { u -> u.startsWith("https://", true) } }
                        ?: continue
                    val name = obj["fileName"]?.jsonPrimitive?.contentOrNull ?: transfer.name
                    DownloadHelper.downloadFile(getApplication(), url, name)
                    delay(350L)
                }
            } else if (payload != null) {
                val url = listOf("fileUrl", "url", "raw_url", "downloadUrl")
                    .firstNotNullOfOrNull { payload[it]?.jsonPrimitive?.contentOrNull?.takeIf { u -> u.startsWith("https://", true) } }
                if (!url.isNullOrBlank()) {
                    val name = payload["fileName"]?.jsonPrimitive?.contentOrNull ?: transfer.name
                    DownloadHelper.downloadFile(getApplication(), url, name)
                }
            }
            deviceApi.updateTransferStatus(transfer.id, "received")
            showToast("${transfer.name} downloading automatically", ToastType.SUCCESS)
        } catch (e: Throwable) {
            runCatching { deviceApi.updateTransferStatus(transfer.id, "failed") }
            showToast(e.message ?: "Auto-download failed", ToastType.ERROR)
        }
    }

    private fun offerDropToLinkedGroup(offer: DropCreatedOffer) {
        if (!storage.isDeviceLinkEnabled()) return
        viewModelScope.launch {
            runCatching {
                val current = deviceApi.sync()
                _linkState.update { it.copy(sync = current) }
                if (current.group == null) return@runCatching
                deviceApi.createTransfer(offer)
                val count = (current.members.size - 1).coerceAtLeast(0)
                showToast(
                    if (count > 0) "Offered to $count paired device${if (count == 1) "" else "s"}" else "Saved to your device group",
                    ToastType.SUCCESS,
                )
                refreshLink(silent = true)
            }
        }
    }

    private fun markLinkedTransferReceived(dropCode: String) {
        val transfer = _linkState.value.sync.incoming.firstOrNull { it.dropCode == dropCode } ?: return
        if (transfer.recipientStatus !in setOf("approved", "downloading")) return
        viewModelScope.launch {
            runCatching {
                if (transfer.recipientStatus == "approved") {
                    deviceApi.updateTransferStatus(transfer.id, "downloading")
                }
                deviceApi.updateTransferStatus(transfer.id, "received")
                refreshLink(silent = true)
            }
        }
    }

    fun setLinkSetupMode(m: String) = _linkState.update { it.copy(setupMode = m) }
    fun setLinkGroupName(v: String) = _linkState.update { it.copy(groupName = v) }
    fun setLinkDuration(v: String) = _linkState.update { it.copy(duration = v) }
    fun setLinkPairToken(v: String) = _linkState.update { it.copy(pairToken = v.trim()) }
    fun setLinkDeviceNameInput(v: String) = _linkState.update { it.copy(deviceNameInput = v) }
    fun setLinkSection(s: String) = _linkState.update { it.copy(section = s) }

    fun dismissIncomingPrompt(id: String) {
        _linkState.update { it.copy(dismissedPromptIds = it.dismissedPromptIds + id) }
    }

    private fun runLinkAction(key: String, successMsg: String? = null, block: suspend () -> Unit) {
        _linkState.update { it.copy(busyKey = key) }
        viewModelScope.launch {
            try {
                storage.setDeviceLinkEnabled(true)
                block()
                if (!successMsg.isNullOrBlank()) showToast(successMsg, ToastType.SUCCESS)
                refreshLink(silent = true)
            } catch (e: Throwable) {
                showToast(e.message ?: "Action failed", ToastType.ERROR)
            } finally {
                _linkState.update { it.copy(busyKey = "") }
            }
        }
    }

    fun createLinkGroup() {
        val st = _linkState.value
        runLinkAction("create", "Device group created") {
            deviceApi.createGroup(st.groupName.ifBlank { "My Hefimer Group" }, st.duration)
            val invite = deviceApi.createInvite()
            _linkState.update { it.copy(invite = invite) }
        }
    }

    fun joinLinkGroup() {
        val token = _linkState.value.pairToken.trim()
        runLinkAction("join", "Devices paired successfully") {
            deviceApi.joinGroup(token)
            _linkState.update { it.copy(pairToken = "") }
        }
    }

    fun createLinkInvite() = runLinkAction("invite") {
        val invite = deviceApi.createInvite()
        _linkState.update { it.copy(invite = invite) }
    }

    fun renameLinkDevice() {
        val name = _linkState.value.deviceNameInput.trim()
        if (name.isBlank()) return
        runLinkAction("rename", "Device renamed") {
            deviceApi.rename(name)
        }
    }

    fun toggleLinkAutoApprove() {
        val cur = _linkState.value.sync.group?.autoApprove ?: return
        runLinkAction("auto", if (cur) "Approval required again" else "Auto-approve enabled") {
            deviceApi.setAutoApprove(!cur)
        }
    }

    fun removeLinkDevice(deviceId: String) = runLinkAction("remove:$deviceId", "Device removed") {
        deviceApi.removeDevice(deviceId)
    }

    fun leaveLinkGroup() {
        val isOwner = _linkState.value.sync.group?.ownerDeviceId == _linkState.value.sync.device.id
        runLinkAction("leave", if (isOwner) "Group deleted" else "Device unpaired") {
            deviceApi.leaveGroup()
            _linkState.update { it.copy(invite = null) }
        }
    }

    fun decideLinkTransfer(transfer: DeviceTransfer, decision: String) {
        runLinkAction("$decision:${transfer.id}") {
            deviceApi.decideTransfer(transfer.id, decision)
            if (decision == "approve") {
                _selectedTab.value = MainTab.RECEIVE
                setReceiveCode(transfer.dropCode)
                retrieveDrop(transfer.dropCode)
            }
        }
    }

    fun openLinkTransfer(transfer: DeviceTransfer) {
        viewModelScope.launch {
            if (transfer.recipientStatus == "approved") {
                runCatching { deviceApi.updateTransferStatus(transfer.id, "downloading") }
            }
            _selectedTab.value = MainTab.RECEIVE
            setReceiveCode(transfer.dropCode)
            retrieveDrop(transfer.dropCode)
        }
    }

    fun cancelLinkTransfer(transferId: String) = runLinkAction("cancel:$transferId", "Transfer cancelled") {
        deviceApi.cancelTransfer(transferId)
    }
    // endregion
}
