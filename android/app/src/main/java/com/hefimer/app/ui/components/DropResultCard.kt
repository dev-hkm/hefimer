package com.hefimer.app.ui.components

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.Link
import androidx.compose.material.icons.rounded.QrCode2
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.hefimer.app.data.model.formatCountdown
import com.hefimer.app.data.network.DownloadHelper
import com.hefimer.app.data.network.HefimerApiConfig
import com.hefimer.app.ui.theme.AppMotion
import com.hefimer.app.ui.theme.AppleGlassVariant
import com.hefimer.app.ui.theme.HefimerColors
import com.hefimer.app.ui.theme.UbuntuFontFamily
import com.hefimer.app.ui.theme.appleGlassSurface
import com.hefimer.app.ui.theme.applePressable
import com.hefimer.app.ui.theme.rememberReduceMotion
import com.hefimer.app.ui.theme.tr
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private const val MAX_CLIPBOARD_CHARS = 250_000
private const val MAX_INTENT_TEXT_CHARS = 180_000

fun copyToClipboard(context: Context, label: String, value: String): Boolean {
    if (value.length > MAX_CLIPBOARD_CHARS) return false
    return runCatching {
        val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        cm.setPrimaryClip(ClipData.newPlainText(label, value))
        true
    }.getOrDefault(false)
}

fun shareTextSheet(context: Context, text: String, title: String = tr("Share via Hefimer", "Chia sẻ qua Hefimer")) {
    runCatching {
        val intent = if (text.length > MAX_INTENT_TEXT_CHARS) {
            val out = java.io.File(context.cacheDir, "hef_share_${System.currentTimeMillis()}.txt")
            out.writeText(text)
            val uri = androidx.core.content.FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                out,
            )
            Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_STREAM, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
        } else {
            Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_TEXT, text)
            }
        }
        context.startActivity(Intent.createChooser(intent, title).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    }
}

/**
 * Animated 5-digit result card shared across Drops, Secret R2, Chat Rooms, Boards, and Spaces.
 */
@Composable
fun DropResultCard(
    code: String,
    heading: String = tr("Your Drop Code", "Mã Drop của bạn"),
    subtitle: String? = null,
    expiresAt: Long? = null,
    shareUrl: String = "${HefimerApiConfig.SITE_ORIGIN}/?code=$code",
    isR2: Boolean = false,
    resetLabel: String = tr("New Drop", "Drop mới"),
    primaryCtaLabel: String? = null,
    onPrimaryCta: (() -> Unit)? = null,
    onBack: (() -> Unit)? = null,
    onReset: () -> Unit,
    onToast: (String, Boolean) -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val reduceMotion = rememberReduceMotion()
    var copiedCode by remember { mutableStateOf(false) }
    var copiedLink by remember { mutableStateOf(false) }
    var showQr by remember { mutableStateOf(false) }
    var timeLeft by remember(expiresAt) { mutableStateOf(if (expiresAt != null) formatCountdown(expiresAt) else "") }

    LaunchedEffect(expiresAt) {
        if (expiresAt == null || expiresAt <= 0L) return@LaunchedEffect
        while (true) {
            timeLeft = formatCountdown(expiresAt)
            if (System.currentTimeMillis() >= expiresAt) break
            delay(1000L)
        }
    }

    val accent = if (isR2) HefimerColors.Emerald400 else HefimerColors.White
    val cardBg = if (isR2) HefimerColors.EmeraldBg else HefimerColors.White04
    val cardBorder = if (isR2) HefimerColors.EmeraldBorder else HefimerColors.White12

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .appleGlassSurface(
                shape = RoundedCornerShape(38.dp),
                variant = AppleGlassVariant.Regular,
                accentBorderTint = if (isR2) HefimerColors.Emerald400 else null,
                accentFillTint = if (isR2) HefimerColors.EmeraldBg.copy(alpha = 0.22f) else null,
                elevation = 14.dp,
            )
            .padding(22.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        // Top mini action row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (onBack != null) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(5.dp),
                    modifier = Modifier
                        .appleGlassSurface(
                            shape = CircleShape,
                            variant = AppleGlassVariant.Clear,
                            accentBorderTint = if (isR2) HefimerColors.Emerald400 else null,
                            elevation = 0.dp,
                        )
                        .applePressable(pressedScale = 0.92f, onClick = onBack)
                        .padding(horizontal = 12.dp, vertical = 7.dp),
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                        contentDescription = tr("Back", "Quay lại"),
                        tint = accent.copy(alpha = 0.78f),
                        modifier = Modifier.size(13.dp),
                    )
                    Text(
                        text = tr("BACK", "QUAY LẠI"),
                        color = accent.copy(alpha = 0.78f),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.2.sp,
                    )
                }
            } else {
                Spacer(Modifier.width(1.dp))
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(5.dp),
                modifier = Modifier
                    .appleGlassSurface(
                        shape = CircleShape,
                        variant = AppleGlassVariant.Clear,
                        accentBorderTint = if (isR2) HefimerColors.Emerald400 else null,
                        elevation = 0.dp,
                    )
                    .applePressable(pressedScale = 0.92f, onClick = onReset)
                    .padding(horizontal = 12.dp, vertical = 7.dp),
            ) {
                Text(
                    text = resetLabel.uppercase(),
                    color = accent.copy(alpha = 0.80f),
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.2.sp,
                )
                Icon(
                    imageVector = Icons.Rounded.Add,
                    contentDescription = resetLabel,
                    tint = accent.copy(alpha = 0.80f),
                    modifier = Modifier.size(13.dp),
                )
            }
        }

        Spacer(Modifier.height(24.dp))

        // Kicker
        Text(
            text = heading.uppercase(),
            color = accent.copy(alpha = 0.55f),
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 2.4.sp,
        )
        Spacer(Modifier.height(6.dp))
        Box(
            modifier = Modifier
                .width(28.dp)
                .height(2.dp)
                .clip(CircleShape)
                .background(accent.copy(alpha = 0.25f)),
        )

        Spacer(Modifier.height(22.dp))

        // Staggered animated 5-digit boxes matching ResultView (tap to copy code directly)
        Row(
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .clip(RoundedCornerShape(22.dp))
                .applePressable(pressedScale = 0.96f) {
                    copyToClipboard(context, "Hefimer Code", code)
                    copiedCode = true
                    com.hefimer.app.ui.theme.HefimerHaptics.success(context)
                    onToast(tr("Code #$code copied", "Đã sao chép mã #$code"), false)
                    scope.launch { delay(2000L); copiedCode = false }
                }
                .padding(horizontal = 4.dp, vertical = 2.dp),
        ) {
            code.take(5).forEachIndexed { idx, ch ->
                AnimatedDigitTile(
                    digit = ch,
                    index = idx,
                    accent = accent,
                    isR2 = isR2,
                    reduceMotion = reduceMotion,
                )
            }
        }

        Spacer(Modifier.height(18.dp))

        // Countdown pill
        if (timeLeft.isNotBlank()) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier
                    .appleGlassSurface(
                        shape = CircleShape,
                        variant = AppleGlassVariant.Clear,
                        accentBorderTint = if (isR2) HefimerColors.Emerald400 else null,
                        elevation = 0.dp,
                    )
                    .padding(horizontal = 12.dp, vertical = 6.dp),
            ) {
                Icon(
                    imageVector = Icons.Rounded.Schedule,
                    contentDescription = null,
                    tint = accent.copy(alpha = 0.5f),
                    modifier = Modifier.size(13.dp),
                )
                Text(
                    text = if (timeLeft == "Expired" || timeLeft == "Đã hết hạn") tr("Expired", "Đã hết hạn") else tr("Expires in $timeLeft", "Hết hạn sau $timeLeft"),
                    color = accent.copy(alpha = 0.72f),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    style = androidx.compose.ui.text.TextStyle(fontFeatureSettings = "tnum"),
                )
            }
        }

        if (!subtitle.isNullOrBlank()) {
            Spacer(Modifier.height(10.dp))
            Text(
                text = subtitle,
                color = accent.copy(alpha = 0.50f),
                fontSize = 11.sp,
                fontStyle = FontStyle.Italic,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center,
            )
        }

        Spacer(Modifier.height(22.dp))

        // Copy Link & Copy Code
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            AnimatedCopyPill(
                label = if (copiedLink) tr("Copied!", "Đã chép!") else tr("Copy Link", "Chép link"),
                copied = copiedLink,
                icon = Icons.Rounded.Link,
                isR2 = isR2,
                modifier = Modifier.weight(1f),
                onClick = {
                    copyToClipboard(context, "Hefimer Link", shareUrl)
                    copiedLink = true
                    onToast(tr("Link copied", "Đã sao chép liên kết"), false)
                    scope.launch {
                        delay(2000L)
                        copiedLink = false
                    }
                },
            )
            AnimatedCopyPill(
                label = if (copiedCode) tr("Copied!", "Đã chép!") else tr("Copy Code", "Chép mã"),
                copied = copiedCode,
                icon = Icons.Rounded.ContentCopy,
                isR2 = isR2,
                modifier = Modifier.weight(1f),
                onClick = {
                    copyToClipboard(context, "Hefimer Code", code)
                    copiedCode = true
                    onToast(tr("Code copied", "Đã sao chép mã"), false)
                    scope.launch {
                        delay(2000L)
                        copiedCode = false
                    }
                },
            )
        }

        Spacer(Modifier.height(10.dp))

        // QR & Native Share row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp)
                    .appleGlassSurface(
                        shape = CircleShape,
                        variant = AppleGlassVariant.Clear,
                        accentBorderTint = if (isR2) HefimerColors.Emerald400 else null,
                        elevation = 0.dp,
                    )
                    .applePressable(pressedScale = 0.96f) { showQr = true },
            ) {
                Icon(
                    imageVector = Icons.Rounded.QrCode2,
                    contentDescription = null,
                    tint = accent.copy(alpha = 0.88f),
                    modifier = Modifier.size(17.dp),
                )
                Spacer(Modifier.width(7.dp))
                Text(
                    text = tr("QR Code", "Mã QR"),
                    color = accent.copy(alpha = 0.88f),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                )
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp)
                    .appleGlassSurface(
                        shape = CircleShape,
                        variant = AppleGlassVariant.Clear,
                        accentBorderTint = if (isR2) HefimerColors.Emerald400 else null,
                        elevation = 0.dp,
                    )
                    .applePressable(pressedScale = 0.96f) {
                        shareTextSheet(context, tr("Hefimer code: $code\n$shareUrl", "Mã Hefimer: $code\n$shareUrl"))
                    },
            ) {
                Icon(
                    imageVector = Icons.Rounded.Share,
                    contentDescription = null,
                    tint = accent.copy(alpha = 0.88f),
                    modifier = Modifier.size(16.dp),
                )
                Spacer(Modifier.width(7.dp))
                Text(
                    text = tr("Share", "Chia sẻ"),
                    color = accent.copy(alpha = 0.88f),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                )
            }
        }

        if (primaryCtaLabel != null && onPrimaryCta != null) {
            Spacer(Modifier.height(12.dp))
            HefimerPrimaryButton(
                text = primaryCtaLabel,
                icon = Icons.AutoMirrored.Rounded.ArrowForward,
                accentColor = accent,
                onClick = onPrimaryCta,
            )
        }
    }

    if (showQr) {
        QrShareModal(
            title = if (isR2) tr("Share Secret R2 Drop", "Chia sẻ Secret R2") else tr("Share via QR Code", "Chia sẻ qua mã QR"),
            url = shareUrl,
            code = code,
            emeraldTheme = isR2,
            onDismiss = { showQr = false },
            onToast = onToast,
        )
    }
}

@Composable
private fun AnimatedDigitTile(
    digit: Char,
    index: Int,
    accent: Color,
    isR2: Boolean,
    reduceMotion: Boolean,
) {
    val context = LocalContext.current
    val progress = remember(digit, index) { Animatable(if (reduceMotion) 1f else 0f) }
    LaunchedEffect(digit, index) {
        if (!reduceMotion) {
            delay(index * 115L + 90L)
            // Delicate ascending pulse per digit as the 5-digit code materializes
            launch {
                delay(180L)
                com.hefimer.app.ui.theme.HefimerHaptics.codeDigitReveal(context, index)
            }
            progress.animateTo(
                targetValue = 1f,
                animationSpec = tween(durationMillis = 820, easing = AppMotion.AppleSheet),
            )
        } else {
            delay(index * 70L)
            com.hefimer.app.ui.theme.HefimerHaptics.codeDigitReveal(context, index)
        }
    }
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .width(54.dp)
            .height(72.dp)
            .graphicsLayer {
                val p = progress.value
                alpha = (p * 1.15f).coerceIn(0f, 1f)
                val s = 0.88f + 0.12f * p
                scaleX = s
                scaleY = s
                translationY = (1f - p) * 28f
            }
            .appleGlassSurface(
                shape = RoundedCornerShape(26.dp),
                variant = AppleGlassVariant.Clear,
                elevation = 2.dp,
            ),
    ) {
        Text(
            text = digit.toString(),
            fontFamily = UbuntuFontFamily,
            fontWeight = FontWeight.Bold,
            fontSize = 32.sp,
            color = accent,
        )
    }
}

@Composable
private fun AnimatedCopyPill(
    label: String,
    copied: Boolean,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    isR2: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    val activeBg = HefimerColors.White
    val fgColor by animateColorAsState(
        targetValue = if (copied) HefimerColors.Black else HefimerColors.White85,
        animationSpec = tween(340, easing = AppMotion.AppleSilk),
        label = "copy_pill_fg",
    )

    val pillShape = CircleShape
    val modifierWithGlass = if (copied) {
        modifier
            .height(48.dp)
            .clip(pillShape)
            .background(activeBg)
            .border(1.dp, activeBg, pillShape)
    } else {
        modifier
            .height(48.dp)
            .appleGlassSurface(
                shape = pillShape,
                variant = AppleGlassVariant.Clear,
                elevation = 0.dp,
            )
    }

    val context = LocalContext.current
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
        modifier = modifierWithGlass.applePressable(pressedScale = 0.96f, enableHaptic = false) {
            com.hefimer.app.ui.theme.HefimerHaptics.copyConfirm(context)
            onClick()
        },
    ) {
        AnimatedContent(
            targetState = copied to label,
            transitionSpec = {
                (fadeIn(tween(340, easing = AppMotion.AppleSilk)) +
                    scaleIn(tween(380, easing = AppMotion.AppleSheet), initialScale = 0.92f)) togetherWith
                    (fadeOut(tween(200, easing = AppMotion.AppleEase)) +
                        scaleOut(tween(200, easing = AppMotion.AppleEase), targetScale = 0.94f))
            },
            label = "copy_pill_content",
        ) { (isCopied, lbl) ->
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
            ) {
                Icon(
                    imageVector = if (isCopied) Icons.Rounded.Check else icon,
                    contentDescription = null,
                    tint = fgColor,
                    modifier = Modifier.size(16.dp),
                )
                Spacer(Modifier.width(7.dp))
                Text(
                    text = lbl,
                    color = fgColor,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                )
            }
        }
    }
}

@Composable
fun QrShareModal(
    title: String,
    url: String,
    code: String,
    emeraldTheme: Boolean = false,
    onDismiss: () -> Unit,
    onToast: (String, Boolean) -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp),
            contentAlignment = Alignment.Center,
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .fillMaxWidth()
                    .appleGlassSurface(
                        shape = RoundedCornerShape(36.dp),
                        variant = AppleGlassVariant.Elevated,
                        accentBorderTint = if (emeraldTheme) HefimerColors.Emerald400 else null,
                        elevation = 24.dp,
                    )
                    .padding(24.dp),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = title.uppercase(),
                        color = HefimerColors.White45,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 2.2.sp,
                    )
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(34.dp)
                            .appleGlassSurface(
                                shape = CircleShape,
                                variant = AppleGlassVariant.Clear,
                                elevation = 0.dp,
                            )
                            .applePressable(pressedScale = 0.89f, onClick = onDismiss),
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Close,
                            contentDescription = tr("Close", "Đóng"),
                            tint = HefimerColors.White70,
                            modifier = Modifier.size(16.dp),
                        )
                    }
                }

                Spacer(Modifier.height(18.dp))

                BeautifulQrCodeView(
                    value = url,
                    size = 212.dp,
                    emeraldTheme = emeraldTheme,
                )

                Spacer(Modifier.height(16.dp))

                Text(
                    text = tr("Scan to open & retrieve instantly", "Quét để mở & nhận tức thì"),
                    color = HefimerColors.White80,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                )
                Spacer(Modifier.height(3.dp))
                Text(
                    text = tr(
                        "Compatible with any smartphone camera or Hefimer scanner",
                        "Tương thích với mọi camera điện thoại hoặc trình quét Hefimer",
                    ),
                    color = HefimerColors.White30,
                    fontSize = 11.sp,
                    textAlign = TextAlign.Center,
                )

                Spacer(Modifier.height(20.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center,
                        modifier = Modifier
                            .weight(1f)
                            .height(46.dp)
                            .clip(CircleShape)
                            .background(HefimerColors.White)
                            .applePressable(pressedScale = 0.96f) {
                                scope.launch {
                                    runCatching {
                                        val bmp = BeautifulQrRenderer.renderBitmap(url, 1024, emeraldTheme)
                                        DownloadHelper.saveBitmapToGallery(context, bmp, "hefimer_qr_$code.png")
                                    }.onSuccess {
                                        onToast(tr("QR saved to Pictures/Hefimer", "Đã lưu mã QR vào Pictures/Hefimer"), false)
                                    }.onFailure {
                                        onToast(it.message ?: tr("Failed to save QR", "Không thể lưu mã QR"), true)
                                    }
                                }
                            },
                    ) {
                        Icon(Icons.Rounded.Download, null, tint = HefimerColors.Black, modifier = Modifier.size(15.dp))
                        Spacer(Modifier.width(6.dp))
                        Text(tr("Save QR", "Lưu mã QR"), color = HefimerColors.Black, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center,
                        modifier = Modifier
                            .weight(1f)
                            .height(46.dp)
                            .appleGlassSurface(
                                shape = CircleShape,
                                variant = AppleGlassVariant.Clear,
                                elevation = 0.dp,
                            )
                            .applePressable(pressedScale = 0.96f) {
                                copyToClipboard(context, "Hefimer Link", url)
                                onToast(tr("Link copied", "Đã sao chép liên kết"), false)
                            },
                    ) {
                        Icon(Icons.Rounded.ContentCopy, null, tint = HefimerColors.White, modifier = Modifier.size(15.dp))
                        Spacer(Modifier.width(6.dp))
                        Text(tr("Copy Link", "Chép link"), color = HefimerColors.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
