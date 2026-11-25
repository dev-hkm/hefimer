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
import androidx.compose.material.icons.automirrored.rounded.OpenInNew
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.FolderOpen
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material.icons.rounded.WarningAmber
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.hefimer.app.data.model.ConfirmDialogRequest
import com.hefimer.app.data.model.PendingRedownloadPrompt
import com.hefimer.app.data.model.SavedFilePrompt
import com.hefimer.app.data.model.formatBytes
import com.hefimer.app.ui.theme.AppleGlassVariant
import com.hefimer.app.ui.theme.HefimerColors
import com.hefimer.app.ui.theme.JetBrainsMonoFamily
import com.hefimer.app.ui.theme.appleGlassSurface
import com.hefimer.app.ui.theme.applePressable
import com.hefimer.app.ui.theme.tr

@Composable
fun HefimerConfirmDialog(
    request: ConfirmDialogRequest,
    onDismiss: () -> Unit,
) {
    val accent = if (request.isDestructive) HefimerColors.Red400 else HefimerColors.White
    com.hefimer.app.ui.theme.AppleAnimatedDialog(onDismissRequest = onDismiss) { close ->
        Box(modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp)) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .appleGlassSurface(
                        shape = RoundedCornerShape(34.dp),
                        variant = AppleGlassVariant.Elevated,
                        accentBorderTint = if (request.isDestructive) HefimerColors.Red400 else null,
                        elevation = 24.dp,
                    )
                    .padding(24.dp),
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(54.dp)
                        .clip(CircleShape)
                        .background(if (request.isDestructive) HefimerColors.RedBg else HefimerColors.White08)
                        .border(
                            1.dp,
                            if (request.isDestructive) HefimerColors.RedBorder else HefimerColors.White15,
                            CircleShape,
                        ),
                ) {
                    Icon(
                        imageVector = if (request.isDestructive) Icons.Rounded.DeleteOutline else Icons.Rounded.WarningAmber,
                        contentDescription = null,
                        tint = accent,
                        modifier = Modifier.size(25.dp),
                    )
                }

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Text(
                        text = request.title,
                        color = HefimerColors.White,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center,
                    )
                    Text(
                        text = request.message,
                        color = HefimerColors.White55,
                        fontSize = 13.sp,
                        lineHeight = 19.sp,
                        textAlign = TextAlign.Center,
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    HefimerSecondaryButton(
                        text = tr("Cancel", "Hủy"),
                        height = 48.dp,
                        modifier = Modifier.weight(1f),
                        onClick = close,
                    )
                    HefimerPrimaryButton(
                        text = request.confirmLabel.ifBlank { tr("Confirm", "Xác nhận") },
                        accentColor = if (request.isDestructive) HefimerColors.Red500 else HefimerColors.White,
                        contentColor = if (request.isDestructive) Color.White else HefimerColors.Black,
                        height = 48.dp,
                        modifier = Modifier.weight(1f),
                        onClick = {
                            val act = request.onConfirm
                            close()
                            act()
                        },
                    )
                }
            }
        }
    }
}

@Composable
fun RedownloadConfirmDialog(
    prompt: PendingRedownloadPrompt,
    hasExistingReadableFile: Boolean,
    onOpenExisting: () -> Unit,
    onRedownload: () -> Unit,
    onDismiss: () -> Unit,
) {
    val firstName = if (prompt.files.size > 1) {
        tr("${prompt.files.size} files", "${prompt.files.size} tệp")
    } else {
        prompt.matchedRecord.fileName.ifBlank { prompt.files.firstOrNull()?.resolvedName.orEmpty() }
    }

    com.hefimer.app.ui.theme.AppleAnimatedDialog(onDismissRequest = onDismiss) { close ->
        Box(modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp)) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .appleGlassSurface(
                        shape = RoundedCornerShape(34.dp),
                        variant = AppleGlassVariant.Elevated,
                        elevation = 24.dp,
                    )
                    .padding(24.dp),
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(54.dp)
                        .appleGlassSurface(
                            shape = CircleShape,
                            variant = AppleGlassVariant.Clear,
                            elevation = 0.dp,
                        ),
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Download,
                        contentDescription = null,
                        tint = HefimerColors.White,
                        modifier = Modifier.size(25.dp),
                    )
                }

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Text(
                        text = tr("Already Downloaded", "Tệp này đã được tải trước đó"),
                        color = HefimerColors.White,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center,
                    )
                    Text(
                        text = tr(
                            "\"$firstName\" was already saved to your device. Are you sure you want to download it again?",
                            "Bạn đã tải \"$firstName\" trước đây. Bạn có chắc muốn tải xuống lần nữa không?",
                        ),
                        color = HefimerColors.White55,
                        fontSize = 13.sp,
                        lineHeight = 19.sp,
                        textAlign = TextAlign.Center,
                    )
                }

                if (hasExistingReadableFile) {
                    HefimerSecondaryButton(
                        text = tr("Open Saved File Instead", "Mở tệp đã lưu trước đó"),
                        icon = Icons.AutoMirrored.Rounded.OpenInNew,
                        height = 46.dp,
                        onClick = onOpenExisting,
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    HefimerSecondaryButton(
                        text = tr("Cancel", "Hủy"),
                        height = 48.dp,
                        modifier = Modifier.weight(1f),
                        onClick = close,
                    )
                    HefimerPrimaryButton(
                        text = tr("Download Again", "Tải lại"),
                        icon = Icons.Rounded.Refresh,
                        height = 48.dp,
                        modifier = Modifier.weight(1f),
                        onClick = onRedownload,
                    )
                }
            }
        }
    }
}

@Composable
fun PostSaveBottomSheetContent(
    prompt: SavedFilePrompt,
    onOpenWithApp: () -> Unit,
    onShare: () -> Unit,
    onOpenDownloads: () -> Unit,
    onClose: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .appleGlassSurface(
                shape = RoundedCornerShape(topStart = 36.dp, topEnd = 36.dp),
                variant = AppleGlassVariant.Elevated,
                elevation = 24.dp,
            )
            .navigationBarsPadding()
            .padding(horizontal = 20.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Box(
            modifier = Modifier
                .align(Alignment.CenterHorizontally)
                .width(38.dp)
                .height(4.dp)
                .clip(CircleShape)
                .background(HefimerColors.White20),
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(46.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(HefimerColors.White08)
                    .border(1.dp, HefimerColors.White20, RoundedCornerShape(16.dp)),
            ) {
                Icon(
                    imageVector = Icons.Rounded.CheckCircle,
                    contentDescription = null,
                    tint = HefimerColors.White,
                    modifier = Modifier.size(24.dp),
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = tr("SAVED TO DEVICE", "ĐÃ LƯU VÀO THIẾT BỊ"),
                    color = HefimerColors.White70,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 2.sp,
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text = prompt.fileName,
                    color = HefimerColors.White,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                val sizeLabel = if (prompt.sizeBytes > 0L) "${formatBytes(prompt.sizeBytes)} • " else ""
                Text(
                    text = "$sizeLabel${prompt.mimeType}",
                    color = HefimerColors.White45,
                    fontFamily = JetBrainsMonoFamily,
                    fontSize = 10.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(34.dp)
                    .appleGlassSurface(CircleShape, AppleGlassVariant.Clear, elevation = 0.dp)
                    .applePressable(pressedScale = 0.89f, onClick = onClose),
            ) {
                Icon(Icons.Rounded.Close, tr("Close", "Đóng"), tint = HefimerColors.White60, modifier = Modifier.size(16.dp))
            }
        }

        HefimerPrimaryButton(
            text = tr("Open With App", "Mở bằng ứng dụng..."),
            icon = Icons.AutoMirrored.Rounded.OpenInNew,
            onClick = onOpenWithApp,
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            HefimerSecondaryButton(
                text = tr("Share", "Chia sẻ"),
                icon = Icons.Rounded.Share,
                modifier = Modifier.weight(1f),
                onClick = onShare,
            )
            HefimerSecondaryButton(
                text = tr("Downloads", "Thư mục tải"),
                icon = Icons.Rounded.FolderOpen,
                modifier = Modifier.weight(1f),
                onClick = onOpenDownloads,
            )
        }
    }
}
