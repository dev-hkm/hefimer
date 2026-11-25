package com.hefimer.app.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
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
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hefimer.app.data.model.ToastMessage
import com.hefimer.app.data.model.ToastType
import com.hefimer.app.data.model.formatBytes
import com.hefimer.app.ui.DownloadProgressState
import com.hefimer.app.ui.theme.AppMotion
import com.hefimer.app.ui.theme.AppleFluidProgressTrack
import com.hefimer.app.ui.theme.AppleGlassVariant
import com.hefimer.app.ui.theme.AppleRollingPercentage
import com.hefimer.app.ui.theme.HefimerColors
import com.hefimer.app.ui.theme.JetBrainsMonoFamily
import com.hefimer.app.ui.theme.UbuntuFontFamily
import com.hefimer.app.ui.theme.appleGlassSurface
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
            enter = slideInVertically(tween(440, easing = AppMotion.AppleSheet)) { -it / 2 } +
                fadeIn(tween(320, easing = AppMotion.AppleSilk)) +
                scaleIn(tween(420, easing = AppMotion.AppleSheet), initialScale = 0.935f),
            exit = slideOutVertically(tween(320, easing = AppMotion.AppleSilk)) { -it / 2 } +
                fadeOut(tween(220, easing = AppMotion.AppleEase)) +
                scaleOut(tween(280, easing = AppMotion.AppleSilk), targetScale = 0.955f),
        ) {
            val current = toast ?: return@AnimatedVisibility
            val isInfo = current.type == ToastType.INFO
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

            val rowModifier = if (isInfo) {
                Modifier.appleGlassSurface(
                    shape = CircleShape,
                    variant = com.hefimer.app.ui.theme.AppleGlassVariant.Elevated,
                    elevation = 16.dp,
                )
            } else {
                Modifier
                    .clip(CircleShape)
                    .background(bg)
                    .border(1.dp, bg, CircleShape)
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = rowModifier.padding(horizontal = 18.dp, vertical = 12.dp),
            ) {
                when (current.type) {
                    ToastType.SUCCESS -> Icon(Icons.Rounded.Check, null, tint = fg, modifier = Modifier.size(17.dp))
                    ToastType.ERROR -> Icon(Icons.Rounded.ErrorOutline, null, tint = fg, modifier = Modifier.size(17.dp))
                    ToastType.INFO -> Icon(Icons.Rounded.Info, null, tint = fg.copy(alpha = 0.78f), modifier = Modifier.size(17.dp))
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
    val bgColor by animateColorAsState(
        targetValue = if (active) accentColor else accentColor.copy(alpha = 0.24f),
        animationSpec = tween(320, easing = AppMotion.AppleSilk),
        label = "primary_btn_bg",
    )
    val fgColor by animateColorAsState(
        targetValue = if (active) contentColor else contentColor.copy(alpha = 0.52f),
        animationSpec = tween(260, easing = AppMotion.AppleSilk),
        label = "primary_btn_fg",
    )

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
        modifier = modifier
            .fillMaxWidth()
            .height(height)
            .clip(CircleShape)
            .background(bgColor)
            .applePressable(enabled = active, pressedScale = 0.96f, onClick = onClick)
            .padding(horizontal = 16.dp),
    ) {
        AnimatedContent(
            targetState = Triple(loading, icon, text),
            transitionSpec = {
                (fadeIn(tween(340, easing = AppMotion.AppleSilk)) +
                    scaleIn(tween(380, easing = AppMotion.AppleSheet), initialScale = 0.94f)) togetherWith
                    (fadeOut(tween(210, easing = AppMotion.AppleEase)) +
                        scaleOut(tween(210, easing = AppMotion.AppleEase), targetScale = 0.96f))
            },
            label = "primary_btn_content",
        ) { (isLoading, ic, labelText) ->
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        color = fgColor,
                        strokeWidth = 2.dp,
                        modifier = Modifier.size(18.dp),
                    )
                    Spacer(Modifier.width(10.dp))
                } else if (ic != null) {
                    Icon(
                        imageVector = ic,
                        contentDescription = null,
                        tint = fgColor,
                        modifier = Modifier.size(18.dp),
                    )
                    Spacer(Modifier.width(8.dp))
                }
                Text(
                    text = labelText,
                    color = fgColor,
                    fontFamily = UbuntuFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    letterSpacing = 0.2.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
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
    val bgColor by animateColorAsState(
        targetValue = if (enabled) HefimerColors.White06 else HefimerColors.White04,
        animationSpec = tween(280, easing = AppMotion.AppleSilk),
        label = "sec_btn_bg",
    )
    val borderColor by animateColorAsState(
        targetValue = if (enabled) HefimerColors.White12 else HefimerColors.White08,
        animationSpec = tween(280, easing = AppMotion.AppleSilk),
        label = "sec_btn_border",
    )
    val fgColor by animateColorAsState(
        targetValue = if (enabled) HefimerColors.White80 else HefimerColors.White30,
        animationSpec = tween(260, easing = AppMotion.AppleSilk),
        label = "sec_btn_fg",
    )

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
        modifier = modifier
            .fillMaxWidth()
            .height(height)
            .appleGlassSurface(
                shape = CircleShape,
                variant = AppleGlassVariant.Clear,
                accentFillTint = bgColor,
                elevation = if (enabled) 4.dp else 0.dp,
            )
            .applePressable(enabled = enabled, pressedScale = 0.96f, onClick = onClick)
            .padding(horizontal = 14.dp),
    ) {
        AnimatedContent(
            targetState = icon to text,
            transitionSpec = {
                (fadeIn(tween(320, easing = AppMotion.AppleSilk)) +
                    scaleIn(tween(360, easing = AppMotion.AppleSheet), initialScale = 0.94f)) togetherWith
                    (fadeOut(tween(200, easing = AppMotion.AppleEase)) +
                        scaleOut(tween(200, easing = AppMotion.AppleEase), targetScale = 0.96f))
            },
            label = "sec_btn_content",
        ) { (ic, labelText) ->
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
            ) {
                if (ic != null) {
                    Icon(
                        imageVector = ic,
                        contentDescription = null,
                        tint = fgColor,
                        modifier = Modifier.size(16.dp),
                    )
                    Spacer(Modifier.width(7.dp))
                }
                Text(
                    text = labelText,
                    color = fgColor,
                    fontFamily = UbuntuFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
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
            .appleGlassSurface(
                shape = CircleShape,
                variant = AppleGlassVariant.Clear,
                elevation = 6.dp,
            )
            .applePressable(pressedScale = 0.89f, onClick = onClick),
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = HefimerColors.White80,
            modifier = Modifier.size(18.dp),
        )
        AnimatedVisibility(
            visible = badgeDot,
            enter = scaleIn(AppMotion.softPopSpring(), initialScale = 0.5f) + fadeIn(tween(180)),
            exit = scaleOut(tween(150), targetScale = 0.5f) + fadeOut(tween(150)),
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(7.dp),
        ) {
            Box(
                modifier = Modifier
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
    val trackColor by animateColorAsState(
        targetValue = if (checked) activeColor else HefimerColors.White15,
        animationSpec = tween(AppMotion.Fast, easing = AppMotion.AppleSilk),
        label = "toggle_track",
    )
    val iconColor by animateColorAsState(
        targetValue = if (checked) activeColor else HefimerColors.White40,
        animationSpec = tween(AppMotion.Fast, easing = AppMotion.AppleSilk),
        label = "toggle_icon",
    )
    val titleColor by animateColorAsState(
        targetValue = if (checked) activeColor else HefimerColors.White80,
        animationSpec = tween(AppMotion.Fast, easing = AppMotion.AppleSilk),
        label = "toggle_title",
    )
    val thumbOffset by animateDpAsState(
        targetValue = if (checked) 20.dp else 2.dp,
        animationSpec = AppMotion.layoutSpring(),
        label = "toggle_thumb",
    )

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .fillMaxWidth()
            .appleGlassSurface(
                shape = RoundedCornerShape(22.dp),
                variant = AppleGlassVariant.Clear,
                accentBorderTint = if (checked) activeColor else null,
                accentFillTint = if (checked) activeColor.copy(alpha = 0.14f) else null,
                elevation = 0.dp,
            )
            .applePressable(pressedScale = 0.97f, onClick = onToggle)
            .padding(horizontal = 14.dp, vertical = 12.dp),
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = iconColor,
            modifier = Modifier.size(18.dp),
        )
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                color = titleColor,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
            )
            Text(
                text = subtitle,
                color = if (checked) activeColor.copy(alpha = 0.72f) else HefimerColors.White40,
                fontSize = 11.sp,
            )
        }
        Box(
            modifier = Modifier
                .width(42.dp)
                .height(24.dp)
                .clip(CircleShape)
                .background(trackColor),
        ) {
            Box(
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .offset { IntOffset(thumbOffset.roundToPx(), 0) }
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
                .appleGlassSurface(
                    shape = RoundedCornerShape(30.dp),
                    variant = AppleGlassVariant.Elevated,
                    elevation = 18.dp,
                )
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
                        .appleGlassSurface(
                            shape = CircleShape,
                            variant = AppleGlassVariant.Clear,
                            elevation = 0.dp,
                        )
                        .applePressable(pressedScale = 0.89f, onClick = onCancel),
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

