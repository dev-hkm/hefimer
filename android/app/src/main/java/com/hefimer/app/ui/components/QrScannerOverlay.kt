package com.hefimer.app.ui.components

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageDecoder
import android.graphics.ImageFormat
import android.net.Uri
import android.os.Build
import android.util.Size
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.FlashOff
import androidx.compose.material.icons.rounded.FlashOn
import androidx.compose.material.icons.rounded.PhotoLibrary
import androidx.compose.material.icons.rounded.QrCodeScanner
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.google.zxing.BarcodeFormat
import com.google.zxing.BinaryBitmap
import com.google.zxing.DecodeHintType
import com.google.zxing.MultiFormatReader
import com.google.zxing.PlanarYUVLuminanceSource
import com.google.zxing.RGBLuminanceSource
import com.google.zxing.common.GlobalHistogramBinarizer
import com.google.zxing.common.HybridBinarizer
import com.hefimer.app.ui.theme.AppleGlassVariant
import com.hefimer.app.ui.theme.HefimerColors
import com.hefimer.app.ui.theme.HefimerHaptics
import com.hefimer.app.ui.theme.appleGlassSurface
import com.hefimer.app.ui.theme.applePressable
import com.hefimer.app.ui.theme.tr
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicReference

@Composable
fun QrScannerOverlay(
    hint: String = tr(
        "Point camera at a Hefimer Drop, Space, Room, or Link QR code",
        "Đưa camera vào mã QR Drop, Space, Phòng hoặc Link của Hefimer",
    ),
    onResult: (String) -> Unit,
    onClose: () -> Unit,
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val haptic = LocalHapticFeedback.current
    val scope = rememberCoroutineScope()
    val statusTop = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    val navBottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()

    var hasPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED,
        )
    }
    var torchOn by remember { mutableStateOf(false) }
    var decodingImage by remember { mutableStateOf(false) }
    var galleryError by remember { mutableStateOf<String?>(null) }
    var boundCamera by remember { mutableStateOf<Camera?>(null) }
    val providerRef = remember { AtomicReference<ProcessCameraProvider?>(null) }
    val analysisRef = remember { AtomicReference<ImageAnalysis?>(null) }
    val scanned = remember { AtomicBoolean(false) }
    val executor = remember { Executors.newSingleThreadExecutor() }

    fun releaseCameraImmediately() {
        scanned.set(true)
        torchOn = false
        runCatching { boundCamera?.cameraControl?.enableTorch(false) }
        runCatching { analysisRef.getAndSet(null)?.clearAnalyzer() }
        runCatching { providerRef.getAndSet(null)?.unbindAll() }
    }

    fun triggerClose() {
        releaseCameraImmediately()
        onClose()
    }

    fun triggerSuccess(text: String) {
        releaseCameraImmediately()
        if (HefimerHaptics.enabled) {
            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
        }
        onResult(text)
    }

    val permLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        hasPermission = granted
    }

    val galleryPicker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        galleryError = null
        decodingImage = true
        scope.launch {
            val decoded = decodeQrFromUri(context, uri)
            decodingImage = false
            if (!decoded.isNullOrBlank()) {
                triggerSuccess(decoded)
            } else {
                galleryError = tr(
                    "No valid QR code found in that image",
                    "Không tìm thấy mã QR hợp lệ trong ảnh vừa chọn",
                )
                delay(2800L)
                galleryError = null
            }
        }
    }

    LaunchedEffect(Unit) {
        if (!hasPermission) permLauncher.launch(Manifest.permission.CAMERA)
    }

    DisposableEffect(Unit) {
        onDispose {
            releaseCameraImmediately()
            runCatching { executor.shutdownNow() }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(HefimerColors.Black),
    ) {
        if (hasPermission) {
            AndroidView(
                modifier = Modifier.fillMaxSize(),
                factory = { ctx ->
                    val previewView = PreviewView(ctx).apply {
                        // TextureView (COMPATIBLE) composites smoothly inside Compose graphicsLayer transforms
                        // during exit animations without SurfaceView punch-hole lag.
                        implementationMode = PreviewView.ImplementationMode.COMPATIBLE
                        scaleType = PreviewView.ScaleType.FILL_CENTER
                    }
                    val providerFuture = ProcessCameraProvider.getInstance(ctx)
                    providerFuture.addListener({
                        if (scanned.get()) return@addListener
                        val provider = runCatching { providerFuture.get() }.getOrNull() ?: return@addListener
                        providerRef.set(provider)
                        val preview = Preview.Builder().build().also {
                            it.surfaceProvider = previewView.surfaceProvider
                        }
                        val reader = MultiFormatReader().apply {
                            setHints(
                                mapOf(
                                    DecodeHintType.POSSIBLE_FORMATS to listOf(BarcodeFormat.QR_CODE),
                                    DecodeHintType.TRY_HARDER to true,
                                ),
                            )
                        }
                        val analysis = ImageAnalysis.Builder()
                            .setTargetResolution(Size(1280, 720))
                            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                            .build()
                        analysisRef.set(analysis)
                        analysis.setAnalyzer(executor) { imageProxy ->
                            try {
                                if (scanned.get()) return@setAnalyzer
                                if (imageProxy.format == ImageFormat.YUV_420_888) {
                                    val plane = imageProxy.planes[0]
                                    val buffer = plane.buffer
                                    val bytes = ByteArray(buffer.remaining())
                                    buffer.get(bytes)
                                    val source = PlanarYUVLuminanceSource(
                                        bytes,
                                        plane.rowStride,
                                        imageProxy.height,
                                        0,
                                        0,
                                        imageProxy.width,
                                        imageProxy.height,
                                        false,
                                    )
                                    val bitmap = BinaryBitmap(HybridBinarizer(source))
                                    val decoded = runCatching { reader.decodeWithState(bitmap) }.getOrNull()
                                    reader.reset()
                                    if (decoded != null && scanned.compareAndSet(false, true)) {
                                        val text = decoded.text
                                        ContextCompat.getMainExecutor(ctx).execute {
                                            triggerSuccess(text)
                                        }
                                    }
                                }
                            } finally {
                                imageProxy.close()
                            }
                        }

                        runCatching {
                            provider.unbindAll()
                            boundCamera = provider.bindToLifecycle(
                                lifecycleOwner,
                                CameraSelector.DEFAULT_BACK_CAMERA,
                                preview,
                                analysis,
                            )
                        }
                    }, ContextCompat.getMainExecutor(ctx))
                    previewView
                },
            )

            // Crisp monochrome viewfinder reticle
            Canvas(modifier = Modifier.fillMaxSize()) {
                val frameSize = minOf(size.width, size.height) * 0.66f
                val left = (size.width - frameSize) / 2f
                val top = (size.height - frameSize) / 2f
                drawRoundRect(
                    color = Color.White.copy(alpha = 0.85f),
                    topLeft = Offset(left, top),
                    size = androidx.compose.ui.geometry.Size(frameSize, frameSize),
                    cornerRadius = CornerRadius(32.dp.toPx(), 32.dp.toPx()),
                    style = Stroke(width = 2.5.dp.toPx()),
                )
            }
        } else {
            Column(
                modifier = Modifier
                    .align(Alignment.Center)
                    .padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(64.dp)
                        .clip(CircleShape)
                        .background(HefimerColors.White08)
                        .border(1.dp, HefimerColors.White15, CircleShape),
                ) {
                    Icon(Icons.Rounded.QrCodeScanner, null, tint = HefimerColors.White, modifier = Modifier.size(28.dp))
                }
                Spacer(Modifier.height(16.dp))
                Text(
                    text = tr("Camera permission required", "Cần quyền truy cập máy ảnh"),
                    color = HefimerColors.White,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    text = tr(
                        "Allow camera access or pick a QR image from your library.",
                        "Cho phép camera để quét trực tiếp hoặc chọn ảnh QR từ thư viện.",
                    ),
                    color = HefimerColors.White40,
                    fontSize = 13.sp,
                    textAlign = TextAlign.Center,
                )
                Spacer(Modifier.height(20.dp))
                HefimerPrimaryButton(
                    text = tr("Grant Permission", "Cấp quyền"),
                    onClick = { permLauncher.launch(Manifest.permission.CAMERA) },
                )
                Spacer(Modifier.height(10.dp))
                HefimerSecondaryButton(
                    text = tr("Pick QR from Gallery", "Chọn ảnh QR từ thư viện"),
                    icon = Icons.Rounded.PhotoLibrary,
                    onClick = {
                        galleryPicker.launch(
                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly),
                        )
                    },
                )
            }
        }

        // Top translucent bar
        Row(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth()
                .padding(top = statusTop + 14.dp, start = 20.dp, end = 20.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(44.dp)
                    .appleGlassSurface(
                        shape = CircleShape,
                        variant = AppleGlassVariant.Clear,
                        elevation = 12.dp,
                    )
                    .applePressable(pressedScale = 0.89f, onClick = { triggerClose() }),
            ) {
                Icon(Icons.Rounded.Close, tr("Close scanner", "Đóng máy quét"), tint = Color.White, modifier = Modifier.size(20.dp))
            }

            Text(
                text = tr("SCAN QR CODE", "QUÉT MÃ QR"),
                color = Color.White,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 2.4.sp,
                modifier = Modifier
                    .appleGlassSurface(
                        shape = CircleShape,
                        variant = AppleGlassVariant.Clear,
                        elevation = 10.dp,
                    )
                    .padding(horizontal = 16.dp, vertical = 9.dp),
            )

            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(44.dp)
                    .then(
                        if (torchOn) {
                            Modifier
                                .clip(CircleShape)
                                .background(Color.White)
                                .border(1.dp, Color.White, CircleShape)
                        } else {
                            Modifier.appleGlassSurface(
                                shape = CircleShape,
                                variant = AppleGlassVariant.Clear,
                                elevation = 12.dp,
                            )
                        },
                    )
                    .applePressable(pressedScale = 0.89f, enabled = hasPermission) {
                        torchOn = !torchOn
                        boundCamera?.cameraControl?.enableTorch(torchOn)
                    },
            ) {
                Icon(
                    imageVector = if (torchOn) Icons.Rounded.FlashOn else Icons.Rounded.FlashOff,
                    contentDescription = tr("Torch", "Đèn flash"),
                    tint = if (torchOn) Color.Black else Color.White,
                    modifier = Modifier.size(20.dp),
                )
            }
        }

        // Bottom controls: Gallery picker pill + Error feedback + Hint pill
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(bottom = navBottom + 24.dp, start = 24.dp, end = 24.dp),
        ) {
            AnimatedVisibility(visible = galleryError != null, enter = fadeIn(), exit = fadeOut()) {
                Text(
                    text = galleryError.orEmpty(),
                    color = Color.White,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(HefimerColors.Red500)
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                )
            }

            if (hasPermission) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center,
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(Color.White)
                        .applePressable(pressedScale = 0.94f, enabled = !decodingImage) {
                            galleryPicker.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly),
                            )
                        }
                        .padding(horizontal = 20.dp, vertical = 12.dp),
                ) {
                    if (decodingImage) {
                        CircularProgressIndicator(
                            color = Color.Black,
                            strokeWidth = 2.dp,
                            modifier = Modifier.size(16.dp),
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Rounded.PhotoLibrary,
                            contentDescription = null,
                            tint = Color.Black,
                            modifier = Modifier.size(17.dp),
                        )
                    }
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = if (decodingImage) tr("Reading QR...", "Đang đọc mã QR...") else tr("Pick QR from Gallery", "Chọn ảnh QR từ thư viện"),
                        color = Color.Black,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                    )
                }
            }

            Box(
                modifier = Modifier
                    .appleGlassSurface(
                        shape = RoundedCornerShape(26.dp),
                        variant = AppleGlassVariant.Elevated,
                        elevation = 16.dp,
                    )
                    .padding(horizontal = 18.dp, vertical = 12.dp),
            ) {
                Text(
                    text = hint,
                    color = HefimerColors.White80,
                    fontSize = 12.sp,
                    textAlign = TextAlign.Center,
                )
            }
        }
    }
}

private suspend fun decodeQrFromUri(context: Context, uri: Uri): String? = withContext(Dispatchers.IO) {
    val bmp = loadBoundedBitmap(context, uri, maxSidePx = 1800) ?: return@withContext null
    try {
        decodeQrFromBitmap(bmp)
    } finally {
        bmp.recycle()
    }
}

private fun loadBoundedBitmap(context: Context, uri: Uri, maxSidePx: Int): Bitmap? = runCatching {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
        val src = ImageDecoder.createSource(context.contentResolver, uri)
        ImageDecoder.decodeBitmap(src) { decoder, info, _ ->
            decoder.isMutableRequired = false
            decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
            val w = info.size.width
            val h = info.size.height
            val longest = maxOf(w, h)
            if (longest > maxSidePx && longest > 0) {
                val scale = maxSidePx.toFloat() / longest.toFloat()
                decoder.setTargetSize(
                    (w * scale).toInt().coerceAtLeast(1),
                    (h * scale).toInt().coerceAtLeast(1),
                )
            }
        }
    } else {
        val boundsOpts = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        context.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, boundsOpts) }
        var sample = 1
        val longest = maxOf(boundsOpts.outWidth, boundsOpts.outHeight)
        while (longest / sample > maxSidePx) sample *= 2
        val decodeOpts = BitmapFactory.Options().apply { inSampleSize = sample }
        context.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, decodeOpts) }
    }
}.getOrNull()

private fun decodeQrFromBitmap(bitmap: Bitmap): String? {
    val w = bitmap.width
    val h = bitmap.height
    if (w <= 0 || h <= 0) return null
    val pixels = IntArray(w * h)
    bitmap.getPixels(pixels, 0, w, 0, 0, w, h)

    val source = RGBLuminanceSource(w, h, pixels)
    val reader = MultiFormatReader().apply {
        setHints(
            mapOf(
                DecodeHintType.POSSIBLE_FORMATS to listOf(BarcodeFormat.QR_CODE),
                DecodeHintType.TRY_HARDER to true,
            ),
        )
    }

    val attempts = listOf(
        BinaryBitmap(HybridBinarizer(source)),
        BinaryBitmap(GlobalHistogramBinarizer(source)),
        BinaryBitmap(HybridBinarizer(source.invert())),
        BinaryBitmap(GlobalHistogramBinarizer(source.invert())),
    )

    for (attempt in attempts) {
        val decoded = runCatching { reader.decodeWithState(attempt).text }.getOrNull()
        reader.reset()
        if (!decoded.isNullOrBlank()) return decoded
    }
    return null
}
