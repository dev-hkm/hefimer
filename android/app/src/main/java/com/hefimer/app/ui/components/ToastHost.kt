package com.hefimer.app.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hefimer.app.data.model.ToastMessage
import com.hefimer.app.data.model.ToastType
import com.hefimer.app.data.model.formatBytes
import com.hefimer.app.ui.DownloadProgressState
import com.hefimer.app.ui.theme.AppMotion
import com.hefimer.app.ui.theme.AppleFluidProgressTrack
import com.hefimer.app.ui.theme.AppleRollingPercentage
import com.hefimer.app.ui.theme.HefimerColors
import com.hefimer.app.ui.theme.JetBrainsMonoFamily
import com.hefimer.app.ui.theme.UbuntuFontFamily
import com.hefimer.app.ui.theme.applePressable

@Composable
fun HefimerToastHost(
    toast: ToastMessage?,
    modifier: Modifier = Modifier,
) {
    val statusTop = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()

    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(top = statusTop + 8.dp, start = 20.dp, end = 20.dp),
        contentAlignment = Alignment.TopCenter,
    ) {
        AnimatedVisibility(
            visible = toast != null,
            enter = slideInVertically(tween(340, easing = AppMotion.AppleSheet)) { -it / 2 } +
                fadeIn(tween(240, easing = AppMotion.AppleSilk)) +
                scaleIn(tween(320, easing = AppMotion.AppleSheet), initialScale = 0.94f),
            exit = slideOutVertically(tween(240, easing = AppMotion.AppleSilk)) { -it / 2 } +
                fadeOut(tween(180, easing = AppMotion.AppleEase)) +
                scaleOut(tween(220, easing = AppMotion.AppleEase), targetScale = 0.95f),
        ) {
            val current = toast ?: return@AnimatedVisibility
            val isLight = current.type == ToastType.SUCCESS || current.type == ToastType.ERROR
            val bg = when (current.type) {
                ToastType.SUCCESS -> HefimerColors.White
                ToastType.ERROR -> HefimerColors.Red500
                ToastType.INFO -> HefimerColors.BannerBg
            }
            val fg = when (current.type) {
                ToastType.SUCCESS -> HefimerColors.Black
                ToastType.ERROR -> Color.White
                ToastType.INFO -> HefimerColors.White
            }
            val borderColor = if (isLight) bg else HefimerColors.White20

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .background(bg)
                    .border(1.dp, borderColor, RoundedCornerShape(20.dp))
                    .padding(horizontal = 16.dp, vertical = 12.dp),
            ) {
                when (current.type) {
                    ToastType.SUCCESS -> Icon(Icons.Rounded.Check, null, tint = fg, modifier = Modifier.size(17.dp))
                    ToastType.ERROR -> Icon(Icons.Rounded.ErrorOutline, null, tint = fg, modifier = Modifier.size(17.dp))
                    ToastType.INFO -> Icon(Icons.Rounded.Info, null, tint = fg.copy(alpha = 0.75f), modifier = Modifier.size(17.dp))
                }
                Text(
                    text = current.message,
                    color = fg,
                    fontFamily = UbuntuFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

@Composable
fun HefimerPrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    loading: Boolean = false,
    icon: ImageVector? = null,
    accentColor: Color = HefimerColors.White,
    contentColor: Color = HefimerColors.Black,
    height: Dp = 54.dp,
) {
    val active = enabled && !loading
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
        modifier = modifier
            .fillMaxWidth()
            .height(height)
            .clip(CircleShape)
            .background(if (active) accentColor else accentColor.copy(alpha = 0.25f))
            .applePressable(enabled = active, onClick = onClick),
    ) {
        if (loading) {
            CircularProgressIndicator(
                color = contentColor,
                strokeWidth = 2.dp,
                modifier = Modifier.size(18.dp),
            )
            Spacer(Modifier.width(10.dp))
        } else if (icon != null) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (active) contentColor else contentColor.copy(alpha = 0.5f),
                modifier = Modifier.size(18.dp),
            )
            Spacer(Modifier.width(8.dp))
        }
        Text(
            text = text,
            color = if (active) contentColor else contentColor.copy(alpha = 0.55f),
            fontFamily = UbuntuFontFamily,
            fontWeight = FontWeight.Bold,
            fontSize = 14.sp,
            letterSpacing = 0.2.sp,
        )
    }
}

@Composable
fun HefimerSecondaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    icon: ImageVector? = null,
    height: Dp = 48.dp,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
        modifier = modifier
            .fillMaxWidth()
            .height(height)
            .clip(CircleShape)
            .background(HefimerColors.White06)
            .border(1.dp, HefimerColors.White12, CircleShape)
            .applePressable(enabled = enabled, onClick = onClick),
    ) {
        if (icon != null) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (enabled) HefimerColors.White80 else HefimerColors.White30,
                modifier = Modifier.size(16.dp),
            )
            Spacer(Modifier.width(7.dp))
        }
        Text(
            text = text,
            color = if (enabled) HefimerColors.White80 else HefimerColors.White30,
            fontFamily = UbuntuFontFamily,
            fontWeight = FontWeight.Bold,
            fontSize = 13.sp,
        )
    }
}

@Composable
fun HefimerHeaderIconButton(
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    badgeDot: Boolean = false,
) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .size(38.dp)
            .clip(CircleShape)
            .background(HefimerColors.White06)
            .border(1.dp, HefimerColors.White12, CircleShape)
            .applePressable(pressedScale = 0.91f, onClick = onClick),
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = HefimerColors.White80,
            modifier = Modifier.size(18.dp),
        )
        if (badgeDot) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(7.dp)
                    .size(7.dp)
                    .clip(CircleShape)
                    .background(HefimerColors.White),
            )
        }
    }
}

@Composable
fun HefimerToggleRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onToggle: () -> Unit,
    icon: ImageVector,
    activeColor: Color = HefimerColors.Red500,
    modifier: Modifier = Modifier,
) {
    val bg by animateColorAsState(
        targetValue = if (checked) activeColor.copy(alpha = 0.12f) else HefimerColors.White04,
        animationSpec = tween(AppMotion.Fast, easing = AppMotion.AppleSilk),
        label = "toggle_bg",
    )
    val border by animateColorAsState(
        targetValue = if (checked) activeColor.copy(alpha = 0.35f) else HefimerColors.White10,
        animationSpec = tween(AppMotion.Fast, easing = AppMotion.AppleSilk),
        label = "toggle_border",
    )
    val thumbOffset by animateDpAsState(
        targetValue = if (checked) 20.dp else 2.dp,
        animationSpec = AppMotion.snappySpring(),
        label = "toggle_thumb",
    )

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(bg)
            .border(1.dp, border, RoundedCornerShape(20.dp))
            .applePressable(onClick = onToggle)
            .padding(horizontal = 14.dp, vertical = 12.dp),
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = if (checked) activeColor else HefimerColors.White40,
            modifier = Modifier.size(18.dp),
        )
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                color = if (checked) activeColor else HefimerColors.White80,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
            )
            Text(
                text = subtitle,
                color = if (checked) activeColor.copy(alpha = 0.7f) else HefimerColors.White40,
                fontSize = 11.sp,
            )
        }
        Box(
            modifier = Modifier
                .width(42.dp)
                .height(24.dp)
                .clip(CircleShape)
                .background(if (checked) activeColor else HefimerColors.White15),
        ) {
            Box(
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .offset(x = thumbOffset)
                    .size(20.dp)
                    .clip(CircleShape)
                    .background(if (checked && activeColor == HefimerColors.White) HefimerColors.Black else HefimerColors.White),
            )
        }
    }
}

@Composable
fun FloatingDownloadProgressBanner(
    state: DownloadProgressState?,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier,
) {
    AnimatedVisibility(
        visible = state != null,
        enter = AppMotion.floatingBarEnter,
        exit = AppMotion.floatingBarExit,
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp),
    ) {
        val current = state ?: return@AnimatedVisibility
        Column(
            verticalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(28.dp))
                .background(HefimerColors.BannerBg)
                .border(1.dp, HefimerColors.White15, RoundedCornerShape(28.dp))
                .padding(horizontal = 16.dp, vertical = 14.dp),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(34.dp)
                        .clip(CircleShape)
                        .background(HefimerColors.White08)
                        .border(1.dp, HefimerColors.White15, CircleShape),
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Download,
                        contentDescription = null,
                        tint = HefimerColors.White,
                        modifier = Modifier.size(17.dp),
                    )
                }

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = current.fileName,
                        color = HefimerColors.White,
                        fontFamily = UbuntuFontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    val subtitle = if (current.totalBytes > 0L) {
                        "${formatBytes(current.loadedBytes)} / ${formatBytes(current.totalBytes)}"
                    } else if (current.loadedBytes > 0L) {
                        com.hefimer.app.ui.theme.tr(
                            "Downloaded ${formatBytes(current.loadedBytes)}",
                            "Đã tải ${formatBytes(current.loadedBytes)}",
                        )
                    } else {
                        com.hefimer.app.ui.theme.tr("Connecting to host...", "Đang kết nối máy chủ...")
                    }
                    Text(
                        text = subtitle,
                        color = HefimerColors.White50,
                        style = TextStyle(
                            fontFamily = JetBrainsMonoFamily,
                            fontSize = 10.sp,
                            fontFeatureSettings = "tnum",
                        ),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }

                AppleRollingPercentage(
                    percent = current.percent,
                    color = HefimerColors.White,
                    style = TextStyle(
                        fontFamily = UbuntuFontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                    ),
                )

                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(30.dp)
                        .clip(CircleShape)
                        .background(HefimerColors.White08)
                        .border(1.dp, HefimerColors.White12, CircleShape)
                        .applePressable(onClick = onCancel),
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Close,
                        contentDescription = com.hefimer.app.ui.theme.tr("Cancel download", "Hủy tải xuống"),
                        tint = HefimerColors.White70,
                        modifier = Modifier.size(15.dp),
                    )
                }
            }

            AppleFluidProgressTrack(
                progress = current.fraction,
                accentColor = HefimerColors.White,
                trackColor = HefimerColors.White10,
                height = 5.dp,
            )
        }
    }
}

