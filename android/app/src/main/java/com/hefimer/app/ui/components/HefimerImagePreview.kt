package com.hefimer.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.BrokenImage
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.SubcomposeAsyncImage
import coil.request.ImageRequest
import com.hefimer.app.data.network.DownloadHelper
import com.hefimer.app.ui.theme.AppleAnimatedDialog
import com.hefimer.app.ui.theme.AppleGlassVariant
import com.hefimer.app.ui.theme.HefimerColors
import com.hefimer.app.ui.theme.JetBrainsMonoFamily
import com.hefimer.app.ui.theme.appleGlassSurface
import com.hefimer.app.ui.theme.applePressable
import com.hefimer.app.ui.theme.tr
import java.util.Locale

/**
 * Shared image preview component supporting raster images (PNG/JPG/WebP/GIF/HEIC) and SVG vectors.
 * Automatically resolves proxied hosts, displays crisp loading/retry states, and uses Fit for SVGs.
 */
@Composable
fun HefimerRemoteImage(
    model: Any?,
    modifier: Modifier = Modifier,
    fileName: String? = null,
    mimeType: String? = null,
    contentDescription: String? = null,
    contentScale: ContentScale? = null,
    compact: Boolean = false,
    onClick: (() -> Unit)? = null,
) {
    val context = LocalContext.current
    var retryTick by remember(model) { mutableIntStateOf(0) }
    val resolvedModel = remember(model, fileName, retryTick) {
        if (model is String) DownloadHelper.resolveDirectImageUrl(model, fileName) ?: model else model
    }
    val isSvg = remember(model, fileName, mimeType) {
        DownloadHelper.isSvgAsset(fileName ?: (model as? String), mimeType)
    }
    val effectiveScale = contentScale ?: if (isSvg) ContentScale.Fit else ContentScale.Crop
    val canvasColor = if (isSvg) Color(0xFF16161A) else HefimerColors.White06

    val clickableModifier = if (onClick != null) {
        modifier
            .background(canvasColor)
            .applePressable(pressedScale = 0.975f, onClick = onClick)
    } else {
        modifier.background(canvasColor)
    }

    SubcomposeAsyncImage(
        model = ImageRequest.Builder(context)
            .data(resolvedModel)
            .setParameter("retry", retryTick, memoryCacheKey = null)
            .crossfade(true)
            .build(),
        contentDescription = contentDescription ?: fileName,
        contentScale = effectiveScale,
        modifier = clickableModifier.let { base ->
            if (isSvg && !compact) base.padding(8.dp) else if (isSvg) base.padding(4.dp) else base
        },
        loading = {
            Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                CircularProgressIndicator(
                    color = HefimerColors.White55,
                    strokeWidth = if (compact) 1.6.dp else 2.dp,
                    modifier = Modifier.size(if (compact) 16.dp else 22.dp),
                )
            }
        },
        error = {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .fillMaxSize()
                    .applePressable(pressedScale = 0.94f) { retryTick += 1 },
            ) {
                if (compact) {
                    Icon(
                        imageVector = Icons.Rounded.Refresh,
                        contentDescription = tr("Retry", "Thử lại"),
                        tint = HefimerColors.White50,
                        modifier = Modifier.size(16.dp),
                    )
                } else {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.BrokenImage,
                            contentDescription = null,
                            tint = HefimerColors.White45,
                            modifier = Modifier.size(22.dp),
                        )
                        Text(
                            text = tr("Tap to reload", "Chạm để tải lại"),
                            color = HefimerColors.White50,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.SemiBold,
                        )
                    }
                }
            }
        },
    )
}

/**
 * Full-screen Apple-styled zoomable Lightbox for raster images and SVGs.
 * Includes pinch/double-tap zoom, pan, canvas contrast toggle (Dark/Light for transparent SVGs/PNGs),
 * format badge, optional download action, and close button.
 */
@Composable
fun HefimerImageLightbox(
    model: Any?,
    title: String,
    mimeType: String? = null,
    onDownload: (() -> Unit)? = null,
    onDismiss: () -> Unit,
) {
    val context = LocalContext.current
    val resolvedModel = remember(model, title) {
        if (model is String) DownloadHelper.resolveDirectImageUrl(model, title) ?: model else model
    }
    val isSvg = remember(model, title, mimeType) {
        DownloadHelper.isSvgAsset(title.ifBlank { model as? String }, mimeType)
    }
    val extBadge = remember(title, mimeType, model) {
        val fromTitle = title.substringBefore('?').substringAfterLast('.', "").uppercase()
        when {
            isSvg -> "SVG"
            fromTitle in setOf("PNG", "JPG", "JPEG", "WEBP", "GIF", "HEIC", "BMP", "AVIF") -> fromTitle
            else -> "IMAGE"
        }
    }

    var zoom by remember { mutableFloatStateOf(1f) }
    var pan by remember { mutableStateOf(Offset.Zero) }
    var lightCanvas by remember { mutableStateOf(false) }

    AppleAnimatedDialog(onDismissRequest = onDismiss) { close ->
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp)
                .appleGlassSurface(
                    shape = RoundedCornerShape(30.dp),
                    variant = AppleGlassVariant.Elevated,
                    elevation = 24.dp,
                )
                .padding(12.dp),
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                // Header strip: title + format pill + optional light canvas toggle + close button
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = extBadge,
                        color = HefimerColors.White,
                        fontFamily = JetBrainsMonoFamily,
                        fontSize = 9.5.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp,
                        modifier = Modifier
                            .appleGlassSurface(CircleShape, AppleGlassVariant.Clear, elevation = 0.dp)
                            .padding(horizontal = 9.dp, vertical = 4.dp),
                    )
                    Text(
                        text = title.ifBlank { tr("Image Preview", "Xem trước ảnh") },
                        color = HefimerColors.White85,
                        fontSize = 12.5.sp,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f),
                    )
                    Text(
                        text = if (lightCanvas) tr("Dark BG", "Nền tối") else tr("Light BG", "Nền sáng"),
                        color = HefimerColors.White75,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier
                            .appleGlassSurface(CircleShape, AppleGlassVariant.Clear, elevation = 0.dp)
                            .applePressable(pressedScale = 0.92f) { lightCanvas = !lightCanvas }
                            .padding(horizontal = 10.dp, vertical = 5.dp),
                    )
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(32.dp)
                            .appleGlassSurface(CircleShape, AppleGlassVariant.Clear, elevation = 0.dp)
                            .applePressable(pressedScale = 0.88f) { close() },
                    ) {
                        Icon(Icons.Rounded.Close, tr("Close", "Đóng"), tint = HefimerColors.White, modifier = Modifier.size(16.dp))
                    }
                }

                // Zoomable viewport
                val viewportBg = if (lightCanvas) Color(0xFFF2F2F7) else Color(0xFF0D0D10)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 260.dp, max = 500.dp)
                        .clip(RoundedCornerShape(22.dp))
                        .clipToBounds()
                        .background(viewportBg)
                        .border(0.5.dp, HefimerColors.White12, RoundedCornerShape(22.dp))
                        .pointerInput(resolvedModel) {
                            detectTapGestures(
                                onDoubleTap = {
                                    if (zoom > 1.2f) {
                                        zoom = 1f
                                        pan = Offset.Zero
                                    } else {
                                        zoom = 2.6f
                                    }
                                },
                            )
                        }
                        .pointerInput(resolvedModel) {
                            detectTransformGestures { _, panChange, zoomChange, _ ->
                                val nextZoom = (zoom * zoomChange).coerceIn(1f, 5f)
                                zoom = nextZoom
                                pan = if (nextZoom <= 1.02f) {
                                    Offset.Zero
                                } else {
                                    Offset(
                                        x = (pan.x + panChange.x).coerceIn(-750f, 750f),
                                        y = (pan.y + panChange.y).coerceIn(-750f, 750f),
                                    )
                                }
                            }
                        },
                    contentAlignment = Alignment.Center,
                ) {
                    SubcomposeAsyncImage(
                        model = ImageRequest.Builder(context)
                            .data(resolvedModel)
                            .crossfade(true)
                            .build(),
                        contentDescription = title,
                        contentScale = ContentScale.Fit,
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(if (isSvg) 18.dp else 0.dp)
                            .graphicsLayer {
                                scaleX = zoom
                                scaleY = zoom
                                translationX = pan.x
                                translationY = pan.y
                            },
                        loading = {
                            Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                                CircularProgressIndicator(color = if (lightCanvas) Color.Black else Color.White, strokeWidth = 2.dp)
                            }
                        },
                        error = {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(6.dp),
                            ) {
                                Icon(Icons.Rounded.BrokenImage, null, tint = if (lightCanvas) Color.DarkGray else HefimerColors.White50, modifier = Modifier.size(30.dp))
                                Text(tr("Unable to render image", "Không thể hiển thị ảnh"), color = if (lightCanvas) Color.DarkGray else HefimerColors.White55, fontSize = 12.sp)
                            }
                        },
                    )
                }

                // Footer strip: zoom hint + optional direct Download button
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = if (zoom > 1.05f) {
                            "${String.format(Locale.US, "%.1f", zoom)}x"
                        } else {
                            tr("Pinch or double-tap to zoom", "Chạm 2 lần hoặc kéo để phóng to")
                        },
                        color = HefimerColors.White55,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                    )
                    if (onDownload != null) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .clip(CircleShape)
                                .background(HefimerColors.White)
                                .applePressable(pressedScale = 0.94f) {
                                    close()
                                    onDownload()
                                }
                                .padding(horizontal = 14.dp, vertical = 8.dp),
                        ) {
                            Icon(Icons.Rounded.Download, null, tint = HefimerColors.Black, modifier = Modifier.size(15.dp))
                            Spacer(Modifier.width(5.dp))
                            Text(
                                text = tr("Download", "Tải xuống"),
                                color = HefimerColors.Black,
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.Bold,
                            )
                        }
                    }
                }
            }
        }
    }
}
