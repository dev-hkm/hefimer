package com.hefimer.app.ui.components

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import androidx.compose.animation.AnimatedContent
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
import com.hefimer.app.ui.theme.HefimerColors
import com.hefimer.app.ui.theme.UbuntuFontFamily
import com.hefimer.app.ui.theme.applePressable
import com.hefimer.app.ui.theme.rememberReduceMotion
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

fun copyToClipboard(context: Context, label: String, value: String) {
    val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    cm.setPrimaryClip(ClipData.newPlainText(label, value))
}

fun shareTextSheet(context: Context, text: String, title: String = "Share via Hefimer") {
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_TEXT, text)
    }
    context.startActivity(Intent.createChooser(intent, title))
}

/**
 * Animated 5-digit result card shared across Drops, Secret R2, Chat Rooms, Boards, and Spaces.
 */
@Composable
fun DropResultCard(
    code: String,
    heading: String = "Your Drop Code",
    subtitle: String? = null,
    expiresAt: Long? = null,
    shareUrl: String = "${HefimerApiConfig.SITE_ORIGIN}/?code=$code",
    isR2: Boolean = false,
    resetLabel: String = "New Drop",
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
            if (timeLeft == "Expired") break
            delay(1000L)
        }
    }

    val accent = if (isR2) HefimerColors.Emerald400 else HefimerColors.White
    val cardBg = if (isR2) HefimerColors.EmeraldBg else HefimerColors.White04
    val cardBorder = if (isR2) HefimerColors.EmeraldBorder else HefimerColors.White12

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(34.dp))
            .background(HefimerColors.SurfaceElevated)
            .border(1.dp, if (isR2) HefimerColors.EmeraldBorder else HefimerColors.White12, RoundedCornerShape(34.dp))
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
                        .clip(CircleShape)
                        .background(if (isR2) HefimerColors.EmeraldBg else HefimerColors.White06)
                        .border(1.dp, cardBorder, CircleShape)
                        .applePressable(onClick = onBack)
                        .padding(horizontal = 12.dp, vertical = 7.dp),
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                        contentDescription = "Back",
                        tint = accent.copy(alpha = 0.7f),
                        modifier = Modifier.size(13.dp),
                    )
                    Text(
                        text = "BACK",
                        color = accent.copy(alpha = 0.7f),
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
                    .clip(CircleShape)
                    .background(if (isR2) HefimerColors.EmeraldBg else HefimerColors.White06)
                    .border(1.dp, cardBorder, CircleShape)
                    .applePressable(onClick = onReset)
                    .padding(horizontal = 12.dp, vertical = 7.dp),
            ) {
                Text(
                    text = resetLabel.uppercase(),
                    color = accent.copy(alpha = 0.75f),
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.2.sp,
                )
                Icon(
                    imageVector = Icons.Rounded.Add,
                    contentDescription = resetLabel,
                    tint = accent.copy(alpha = 0.75f),
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

        // Staggered animated 5-digit boxes matching ResultView
        Row(
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            code.take(5).forEachIndexed { idx, ch ->
                AnimatedDigitTile(
                    digit = ch,
                    index = idx,
                    accent = accent,
                    bg = cardBg,
                    border = cardBorder,
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
                    .clip(CircleShape)
                    .background(if (isR2) HefimerColors.EmeraldBg else HefimerColors.White06)
                    .border(1.dp, cardBorder, CircleShape)
                    .padding(horizontal = 12.dp, vertical = 6.dp),
            ) {
                Icon(
                    imageVector = Icons.Rounded.Schedule,
                    contentDescription = null,
                    tint = accent.copy(alpha = 0.45f),
                    modifier = Modifier.size(13.dp),
                )
                Text(
                    text = "Expires in $timeLeft",
                    color = accent.copy(alpha = 0.65f),
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
                color = accent.copy(alpha = 0.45f),
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
                label = if (copiedLink) "Copied!" else "Copy Link",
                copied = copiedLink,
                icon = Icons.Rounded.Link,
                isR2 = isR2,
                modifier = Modifier.weight(1f),
                onClick = {
                    copyToClipboard(context, "Hefimer Link", shareUrl)
                    copiedLink = true
                    onToast("Link copied", false)
                    scope.launch {
                        delay(2000L)
                        copiedLink = false
                    }
                },
            )
            AnimatedCopyPill(
                label = if (copiedCode) "Copied!" else "Copy Code",
                copied = copiedCode,
                icon = Icons.Rounded.ContentCopy,
                isR2 = isR2,
                modifier = Modifier.weight(1f),
                onClick = {
                    copyToClipboard(context, "Hefimer Code", code)
                    copiedCode = true
                    onToast("Code copied", false)
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
                    .clip(CircleShape)
                    .background(if (isR2) HefimerColors.EmeraldBg else HefimerColors.White06)
                    .border(1.dp, cardBorder, CircleShape)
                    .applePressable { showQr = true },
            ) {
                Icon(
                    imageVector = Icons.Rounded.QrCode2,
                    contentDescription = null,
                    tint = accent.copy(alpha = 0.85f),
                    modifier = Modifier.size(17.dp),
                )
                Spacer(Modifier.width(7.dp))
                Text(
                    text = "QR Code",
                    color = accent.copy(alpha = 0.85f),
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
                    .clip(CircleShape)
                    .background(if (isR2) HefimerColors.EmeraldBg else HefimerColors.White06)
                    .border(1.dp, cardBorder, CircleShape)
                    .applePressable {
                        shareTextSheet(context, "Hefimer code: $code\n$shareUrl")
                    },
            ) {
                Icon(
                    imageVector = Icons.Rounded.Share,
                    contentDescription = null,
                    tint = accent.copy(alpha = 0.85f),
                    modifier = Modifier.size(16.dp),
                )
                Spacer(Modifier.width(7.dp))
                Text(
                    text = "Share",
                    color = accent.copy(alpha = 0.85f),
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
            title = if (isR2) "Share Secret R2 Drop" else "Share via QR Code",
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
    bg: Color,
    border: Color,
    reduceMotion: Boolean,
) {
    val progress = remember(digit, index) { Animatable(if (reduceMotion) 1f else 0f) }
    LaunchedEffect(digit, index) {
        if (!reduceMotion) {
            delay(index * 55L + 40L)
            progress.animateTo(1f, AppMotion.softPopSpring())
        }
    }
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .width(54.dp)
            .height(72.dp)
            .graphicsLayer {
                val p = progress.value
                alpha = p.coerceIn(0f, 1f)
                val s = 0.86f + 0.14f * p
                scaleX = s
                scaleY = s
                translationY = (1f - p) * 26f
            }
            .clip(RoundedCornerShape(22.dp))
            .background(
                Brush.verticalGradient(
                    listOf(accent.copy(alpha = 0.08f), bg),
                ),
            )
            .border(1.dp, border, RoundedCornerShape(22.dp)),
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
    val activeBg = if (isR2) HefimerColors.Emerald400 else HefimerColors.White
    val inactiveBg = if (isR2) HefimerColors.EmeraldBg else HefimerColors.White10
    val border = if (copied) activeBg else if (isR2) HefimerColors.EmeraldBorder else HefimerColors.White12
    val fg = if (copied) HefimerColors.Black else if (isR2) HefimerColors.Emerald400 else HefimerColors.White80

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
        modifier = modifier
            .height(48.dp)
            .clip(CircleShape)
            .background(if (copied) activeBg else inactiveBg)
            .border(1.dp, border, CircleShape)
            .applePressable(onClick = onClick),
    ) {
        AnimatedContent(
            targetState = copied,
            transitionSpec = {
                (fadeIn(tween(220, easing = AppMotion.AppleSilk)) + scaleIn(tween(260, easing = AppMotion.AppleSheet), initialScale = 0.8f)) togetherWith
                    (fadeOut(tween(160, easing = AppMotion.AppleEase)) + scaleOut(tween(160, easing = AppMotion.AppleEase), targetScale = 0.8f))
            },
            label = "copy_icon",
        ) { isCopied ->
            Icon(
                imageVector = if (isCopied) Icons.Rounded.Check else icon,
                contentDescription = null,
                tint = fg,
                modifier = Modifier.size(16.dp),
            )
        }
        Spacer(Modifier.width(7.dp))
        Text(
            text = label,
            color = fg,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
        )
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
                    .clip(RoundedCornerShape(34.dp))
                    .background(HefimerColors.SurfaceCard)
                    .border(1.dp, HefimerColors.White12, RoundedCornerShape(34.dp))
                    .padding(24.dp),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = title.uppercase(),
                        color = HefimerColors.White40,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 2.2.sp,
                    )
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(34.dp)
                            .clip(CircleShape)
                            .background(HefimerColors.White06)
                            .applePressable(onClick = onDismiss),
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Close,
                            contentDescription = "Close",
                            tint = HefimerColors.White60,
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
                    text = "Scan to open & retrieve instantly",
                    color = HefimerColors.White80,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                )
                Spacer(Modifier.height(3.dp))
                Text(
                    text = "Compatible with any smartphone camera or Hefimer scanner",
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
                            .applePressable {
                                scope.launch {
                                    runCatching {
                                        val bmp = BeautifulQrRenderer.renderBitmap(url, 1024, emeraldTheme)
                                        DownloadHelper.saveBitmapToGallery(context, bmp, "hefimer_qr_$code.png")
                                    }.onSuccess {
                                        onToast("QR saved to Pictures/Hefimer", false)
                                    }.onFailure {
                                        onToast(it.message ?: "Failed to save QR", true)
                                    }
                                }
                            },
                    ) {
                        Icon(Icons.Rounded.Download, null, tint = HefimerColors.Black, modifier = Modifier.size(15.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Save QR", color = HefimerColors.Black, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center,
                        modifier = Modifier
                            .weight(1f)
                            .height(46.dp)
                            .clip(CircleShape)
                            .background(HefimerColors.White08)
                            .border(1.dp, HefimerColors.White12, CircleShape)
                            .applePressable {
                                copyToClipboard(context, "Hefimer Link", url)
                                onToast("Link copied", false)
                            },
                    ) {
                        Icon(Icons.Rounded.ContentCopy, null, tint = HefimerColors.White, modifier = Modifier.size(15.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Copy Link", color = HefimerColors.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
