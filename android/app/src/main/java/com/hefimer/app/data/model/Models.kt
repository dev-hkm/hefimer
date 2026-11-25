package com.hefimer.app.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject

@Serializable
data class DropFileEntry(
    val fileName: String = "",
    val fileUrl: String = "",
    val url: String = "",
    @SerialName("raw_url") val rawUrl: String = "",
    val downloadUrl: String = "",
    val objectKey: String = "",
    val fileSize: Long = 0L,
    val size: Long = 0L,
    val mimeType: String = "",
    val contentType: String = "",
) {
    val resolvedUrl: String
        get() = listOf(fileUrl, url, rawUrl, downloadUrl).firstOrNull { it.isNotBlank() }?.trim().orEmpty()

    val resolvedSize: Long
        get() = if (fileSize > 0) fileSize else size

    val resolvedName: String
        get() = fileName.ifBlank { "download" }
}

@Serializable
data class DropItem(
    val code: String = "",
    val type: String = "",
    val text: String? = null,
    val fileName: String? = null,
    val fileUrl: String? = null,
    val url: String? = null,
    @SerialName("raw_url") val rawUrl: String? = null,
    val downloadUrl: String? = null,
    val objectKey: String? = null,
    val fileSize: Long = 0L,
    val mimeType: String? = null,
    val contentType: String? = null,
    val description: String? = null,
    val storageProvider: String? = null,
    val isSecretR2: Boolean = false,
    val isDirectFile: Boolean = false,
    val isOneTime: Boolean = false,
    val password: String? = null,
    val expire: String? = null,
    val expiresAt: Long = 0L,
    val files: List<DropFileEntry>? = null,
) {
    val resolvedUrl: String
        get() = listOf(fileUrl, url, rawUrl, downloadUrl)
            .firstOrNull { !it.isNullOrBlank() }?.trim().orEmpty()

    val isR2Drop: Boolean
        get() = type == "r2_file" || isSecretR2 || !objectKey.isNullOrBlank()
}

@Serializable
data class HistoryItem(
    val code: String,
    val type: String, // "file", "r2_file", "text", "room", "board", "space"
    val action: String, // "sent", "received", "created", "joined"
    val createdAt: Long = System.currentTimeMillis(),
    val fileName: String? = null,
    val fileSize: Long? = null,
    val isR2: Boolean = false,
    val roomName: String? = null,
    val boardName: String? = null,
)

@Serializable
data class UploadStats(
    val firstUploadTime: Long = 0L,
    val totalSize: Long = 0L,
)

@Serializable
data class ChatMessage(
    val id: String = "",
    val sender: String = "Guest",
    val senderId: String = "",
    val text: String = "",
    val timestamp: Long = 0L,
    val isAdmin: Boolean = false,
    val reactions: Map<String, Int> = emptyMap(),
)

@Serializable
data class BoardPoint(
    val x: Float = 0f,
    val y: Float = 0f,
)

@Serializable
data class BoardStroke(
    val id: String = "",
    val color: String = "#ffffff",
    val size: Float = 4f,
    val points: List<BoardPoint> = emptyList(),
    val timestamp: Long = 0L,
)

@Serializable
data class SpaceItem(
    val id: String,
    val kind: String = "file", // "file", "note", "link", "code"
    val name: String = "Untitled",
    val x: Float = 100f,
    val y: Float = 100f,
    val zIndex: Int = 1,
    val uploadedBy: String = "Anonymous",
    val timestamp: Long = 0L,
    val sourceNode: String = "items", // "items" or "files"
    val url: String? = null,
    val size: Long? = null,
    val mimeType: String? = null,
    val providerName: String? = null,
    val content: String? = null,
    val noteColor: String = "zinc",
    val language: String? = null,
)

@Serializable
data class SpaceMember(
    val id: String,
    val name: String = "Guest",
    val color: String = "#34d399",
    val joinedAt: Long = 0L,
)

@Serializable
data class SpaceCursor(
    val id: String,
    val name: String = "Guest",
    val color: String = "#34d399",
    val x: Float = 0f,
    val y: Float = 0f,
    val updatedAt: Long = 0L,
)

@Serializable
data class RecentSpaceEntry(
    val code: String,
    val name: String,
    val visitedAt: Long = System.currentTimeMillis(),
)

data class SpaceUploadTask(
    val id: String,
    val fileName: String,
    val progress: Int = 0,
    val status: String = "Starting...",
    val providerName: String = "",
)

// Hefimer Link (Paired Devices) Models
@Serializable
data class DeviceInfo(
    val id: String = "",
    val name: String = "This device",
)

@Serializable
data class DeviceGroup(
    val id: String = "",
    val name: String = "",
    val ownerDeviceId: String = "",
    val expiresAt: Long? = null,
    val autoApprove: Boolean = false,
)

@Serializable
data class GroupMember(
    val id: String = "",
    val name: String = "",
    val platform: String = "Android",
    @SerialName("last_seen_at") val lastSeenAt: Long = 0L,
    @SerialName("auto_approve") val autoApprove: Boolean = false,
    @SerialName("is_owner") val isOwner: Boolean = false,
    val online: Boolean = false,
)

@Serializable
data class TransferRecipient(
    @SerialName("device_id") val deviceId: String = "",
    @SerialName("device_name") val deviceName: String = "",
    val status: String = "pending",
    @SerialName("updated_at") val updatedAt: Long = 0L,
)

@Serializable
data class DeviceTransfer(
    val id: String = "",
    val kind: String = "file",
    val name: String = "",
    val provider: String = "",
    @SerialName("size_bytes") val sizeBytes: Long = 0L,
    @SerialName("drop_code") val dropCode: String = "",
    @SerialName("expires_at") val expiresAt: Long = 0L,
    @SerialName("created_at") val createdAt: Long = 0L,
    @SerialName("transfer_status") val transferStatus: String = "active",
    @SerialName("recipient_status") val recipientStatus: String? = null,
    @SerialName("sender_name") val senderName: String? = null,
    @SerialName("sender_device_id") val senderDeviceId: String? = null,
    val payload: JsonObject? = null,
    val recipients: List<TransferRecipient>? = null,
)

@Serializable
data class DeviceSync(
    val device: DeviceInfo = DeviceInfo(),
    val group: DeviceGroup? = null,
    val members: List<GroupMember> = emptyList(),
    val incoming: List<DeviceTransfer> = emptyList(),
    val outgoing: List<DeviceTransfer> = emptyList(),
)

@Serializable
data class InviteResponse(
    val token: String = "",
    val expiresAt: Long = 0L,
)

@Serializable
data class DownloadedFileRecord(
    val key: String,
    val fileName: String,
    val fileSize: Long = 0L,
    val mimeType: String = "application/octet-stream",
    val savedUri: String = "",
    val downloadedAt: Long = System.currentTimeMillis(),
)

data class SavedFilePrompt(
    val fileName: String,
    val mimeType: String,
    val sizeBytes: Long,
    val uriString: String,
)

data class PendingRedownloadPrompt(
    val files: List<DropFileEntry>,
    val matchedRecord: DownloadedFileRecord,
)

data class ConfirmDialogRequest(
    val title: String,
    val message: String,
    val confirmLabel: String,
    val isDestructive: Boolean = true,
    val onConfirm: () -> Unit,
)

data class DropCreatedOffer(
    val kind: String,
    val name: String,
    val provider: String,
    val sizeBytes: Long,
    val dropCode: String,
    val expiresAt: Long,
    val payload: JsonObject,
)

data class GlobalBroadcast(
    val id: String,
    val title: String,
    val message: String,
    val theme: String,
    val icon: String,
    val expiresAt: Long,
)

data class GlobalStatsState(
    val developer: String = "Khanh Minh",
    val notice: String = "",
    val noticeEnabled: Boolean = false,
    val noticeTheme: String = "blue",
    val noticeExpiresAt: Long = 0L,
    val isFrozen: Boolean = false,
    val likes: Int = 0,
    val broadcast: GlobalBroadcast? = null,
)

data class ToastMessage(
    val id: Long = System.currentTimeMillis(),
    val message: String,
    val type: ToastType = ToastType.INFO,
)

enum class ToastType { SUCCESS, ERROR, INFO }

fun formatBytes(bytes: Long): String {
    if (bytes <= 0L) return "0 B"
    val units = arrayOf("B", "KB", "MB", "GB", "TB")
    var value = bytes.toDouble()
    var idx = 0
    while (value >= 1024.0 && idx < units.lastIndex) {
        value /= 1024.0
        idx++
    }
    return if (idx == 0) "$bytes B" else String.format(java.util.Locale.US, "%.2f %s", value, units[idx])
}

fun formatCountdown(expiresAt: Long, now: Long = System.currentTimeMillis()): String {
    val diff = expiresAt - now
    if (diff <= 0L) return com.hefimer.app.ui.theme.tr("Expired", "Đã hết hạn")
    val totalSec = diff / 1000L
    val days = totalSec / 86400L
    val hours = (totalSec % 86400L) / 3600L
    val minutes = (totalSec % 3600L) / 60L
    val seconds = totalSec % 60L
    return if (com.hefimer.app.ui.theme.HefimerLang.isVi) {
        when {
            days > 0 -> "${days}n ${hours}g"
            hours > 0 -> "${hours}g ${minutes}p ${seconds}s"
            else -> "${minutes}p ${seconds}s"
        }
    } else {
        when {
            days > 0 -> "${days}d ${hours}h"
            hours > 0 -> "${hours}h ${minutes}m ${seconds}s"
            else -> "${minutes}m ${seconds}s"
        }
    }
}

fun formatRemainingCompact(expiresAt: Long?, now: Long = System.currentTimeMillis()): String {
    if (expiresAt == null || expiresAt <= 0L) return com.hefimer.app.ui.theme.tr("No expiry", "Không hết hạn")
    val diff = expiresAt - now
    if (diff <= 0L) return com.hefimer.app.ui.theme.tr("Expired", "Đã hết hạn")
    val hours = diff / 3_600_000L
    return when {
        hours >= 48 -> {
            val d = (hours + 23) / 24
            com.hefimer.app.ui.theme.tr("$d days left", "Còn $d ngày")
        }
        hours >= 1 -> com.hefimer.app.ui.theme.tr("${hours}h left", "Còn ${hours}g")
        else -> {
            val m = ((diff + 59_999L) / 60_000L).coerceAtLeast(1)
            com.hefimer.app.ui.theme.tr("${m}m left", "Còn ${m}p")
        }
    }
}

fun parseExpireDurationMs(expire: String): Long {
    val match = Regex("""^(\d+)(sec|min|s|m|p|h|d|w)$""", RegexOption.IGNORE_CASE).find(expire.trim())
        ?: return 3_600_000L
    val amount = match.groupValues[1].toLongOrNull() ?: 1L
    return when (match.groupValues[2].lowercase()) {
        "s", "sec" -> amount * 1_000L
        "m", "min", "p" -> amount * 60_000L
        "h" -> amount * 3_600_000L
        "d" -> amount * 86_400_000L
        "w" -> amount * 7L * 86_400_000L
        else -> 3_600_000L
    }
}
