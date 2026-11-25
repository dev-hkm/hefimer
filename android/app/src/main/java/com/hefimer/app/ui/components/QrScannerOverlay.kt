package com.hefimer.app.ui.components

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.ImageFormat
import android.util.Size
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.FlashOff
import androidx.compose.material.icons.rounded.FlashOn
import androidx.compose.material.icons.rounded.QrCodeScanner
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
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
import androidx.compose.ui.platform.LocalLifecycleOwner
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
import com.google.zxing.common.HybridBinarizer
import com.hefimer.app.ui.theme.AppleGlassVariant
import com.hefimer.app.ui.theme.HefimerColors
import com.hefimer.app.ui.theme.appleGlassSurface
import com.hefimer.app.ui.theme.applePressable
import com.hefimer.app.ui.theme.tr
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean

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
    val statusTop = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    val navBottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()

    var hasPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED,
        )
    }
    var torchOn by remember { mutableStateOf(false) }
    var boundCamera by remember { mutableStateOf<Camera?>(null) }
    val scanned = remember { AtomicBoolean(false) }
    val executor = remember { Executors.newSingleThreadExecutor() }

    val permLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        hasPermission = granted
    }

    LaunchedEffect(Unit) {
        if (!hasPermission) permLauncher.launch(Manifest.permission.CAMERA)
    }

    DisposableEffect(Unit) {
        onDispose { executor.shutdown() }
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
                        scaleType = PreviewView.ScaleType.FILL_CENTER
                    }
                    val providerFuture = ProcessCameraProvider.getInstance(ctx)
                    providerFuture.addListener({
                        val provider = providerFuture.get()
                        val preview = Preview.Builder().build().also {
                            it.surfaceProvider = previewView.surfaceProvider
                        }
                        val reader = MultiFormatReader().apply {
                            setHints(
                                mapOf(
                                    DecodeHintType.POSSIBLE_FORMATS to listOf(BarcodeFormat.QR_CODE),
                                ),
                            )
                        }
                        val analysis = ImageAnalysis.Builder()
                            .setTargetResolution(Size(1280, 720))
                            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                            .build()
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
                                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                            onResult(text)
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
                        "Allow camera access to scan Hefimer QR codes instantly.",
                        "Cho phép truy cập camera để quét mã QR Hefimer tức thì.",
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
                    .applePressable(pressedScale = 0.89f, onClick = onClose),
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
                    .applePressable(pressedScale = 0.89f) {
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

        // Bottom caption pill
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = navBottom + 28.dp, start = 24.dp, end = 24.dp)
                .appleGlassSurface(
                    shape = RoundedCornerShape(26.dp),
                    variant = AppleGlassVariant.Elevated,
                    elevation = 16.dp,
                )
                .padding(horizontal = 18.dp, vertical = 14.dp),
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
