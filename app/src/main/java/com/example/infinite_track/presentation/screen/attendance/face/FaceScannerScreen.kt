package com.example.infinite_track.presentation.screen.attendance.face

import android.graphics.Bitmap
import androidx.camera.core.CameraSelector
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageProxy
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.SentimentSatisfied
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.example.infinite_track.presentation.components.button.ButtonStateType
import com.example.infinite_track.presentation.components.button.ButtonStyle
import com.example.infinite_track.presentation.components.button.StatefulButton
import com.example.infinite_track.presentation.components.cameras.FaceBoundingBox
import com.example.infinite_track.presentation.components.loading.LoadingAnimation
import com.example.infinite_track.presentation.theme.Blue_500
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import com.google.accompanist.permissions.shouldShowRationale
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import androidx.camera.core.Preview as CameraPreview

@OptIn(ExperimentalMaterial3Api::class, ExperimentalPermissionsApi::class)
@ExperimentalGetImage
@Composable
fun FaceScannerScreen(
    action: String,
    navController: NavController,
    modifier: Modifier = Modifier,
    viewModel: FaceScannerViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    // Camera permission state
    val cameraPermissionState = rememberPermissionState(
        android.Manifest.permission.CAMERA
    )

    // Camera executor
    val cameraExecutor = remember { Executors.newSingleThreadExecutor() }

    // KUNCI PERBAIKAN: Reset scanner setiap kali screen muncul
    // Ini akan memastikan ViewModel selalu dalam state fresh
    LaunchedEffect(Unit) {
        // Reset scanner terlebih dahulu untuk memastikan state bersih
        viewModel.initializeScanner()

        // Then request camera permission if not granted
        if (!cameraPermissionState.status.isGranted) {
            cameraPermissionState.launchPermissionRequest()
        }
    }

    // Handle navigation based on verification result
    LaunchedEffect(uiState.livenessState) {
        when (uiState.livenessState) {
            LivenessState.SUCCESS -> {
                // Send success result and navigate back to proceed with attendance
                navController.previousBackStackEntry?.savedStateHandle?.set(
                    "face_verification_result",
                    true
                )
                navController.popBackStack()
            }

            LivenessState.TIMEOUT -> {
                // TIMEOUT means user ran out of time - send failure and go back
                navController.previousBackStackEntry?.savedStateHandle?.set(
                    "face_verification_result",
                    false
                )
                navController.popBackStack()
            }

            // FAILURE stays on screen to allow retry - no automatic navigation
            LivenessState.FAILURE -> {
                // Stay on screen, show retry button - user can try again
            }

            else -> {
                // Continue with current state - don't navigate anywhere
            }
        }
    }

    // Cleanup camera executor when composable is disposed
    DisposableEffect(Unit) {
        onDispose {
            cameraExecutor.shutdown()
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        when {
            cameraPermissionState.status.isGranted -> {
                // Camera permission granted - show camera interface
                CameraContent(
                    modifier = Modifier.fillMaxSize(),
                    action = action,
                    uiState = uiState,
                    cameraExecutor = cameraExecutor,
                    onImageAnalysis = { imageProxy, bitmap ->
                        viewModel.processImageProxy(imageProxy, bitmap)
                    },
                    onRetryClick = {
                        viewModel.resetScanner()
                    },
                    onCloseClick = {
                        navController.popBackStack()
                    }
                )
            }

            cameraPermissionState.status.shouldShowRationale -> {
                // Show rationale for camera permission
                CameraPermissionRationale(
                    onRequestPermission = {
                        cameraPermissionState.launchPermissionRequest()
                    },
                    onCloseClick = {
                        navController.popBackStack()
                    }
                )
            }

            else -> {
                // Permission denied - show permission request UI
                CameraPermissionDenied(
                    onRequestPermission = {
                        cameraPermissionState.launchPermissionRequest()
                    },
                    onCloseClick = {
                        navController.popBackStack()
                    }
                )
            }
        }
    }
}

@Composable
@ExperimentalGetImage
private fun CameraContent(
    modifier: Modifier = Modifier,
    action: String,
    uiState: FaceScannerState,
    cameraExecutor: ExecutorService,
    onImageAnalysis: (ImageProxy, Bitmap) -> Unit,
    onRetryClick: () -> Unit,
    onCloseClick: () -> Unit
) {
    var previewSize by remember { mutableStateOf<androidx.compose.ui.geometry.Size?>(null) }
    Box(
        modifier = modifier
            .fillMaxSize()
            .onSizeChanged { size ->
                // Capture actual preview size for coordinate scaling
                previewSize = androidx.compose.ui.geometry.Size(
                    width = size.width.toFloat(),
                    height = size.height.toFloat()
                )
            }
    ) {
        // Layer 1: Camera Preview with proper CameraX integration
        CameraPreview(
            modifier = Modifier.fillMaxSize(),
            cameraExecutor = cameraExecutor,
            onImageAnalysis = { imageProxy, bitmap ->
                onImageAnalysis(imageProxy, bitmap)
            }
        )

        // Layer 2: Face Bounding Box Overlay with proper coordinate scaling
        FaceBoundingBox(
            modifier = Modifier.fillMaxSize(),
            boundingBox = uiState.boundingBox,
            livenessState = uiState.livenessState,
            previewSize = previewSize, // Pass actual preview size
            imageSize = uiState.imageSize // Pass image size from ViewModel
        )

        // Layer 3: Top Info Section - More compact
        TopInfoSection(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 16.dp, start = 16.dp, end = 16.dp),
            action = action
        )

        // Layer 4: Instruction Section with dynamic content
        InstructionSection(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 16.dp, start = 16.dp, end = 16.dp),
            uiState = uiState,
            onRetryClick = onRetryClick,
            onCloseClick = onCloseClick
        )

        // Layer 5: Loading Overlay for processing state
        if (uiState.isProcessing) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.3f)),
                contentAlignment = Alignment.Center
            ) {
                Card(
                    modifier = Modifier.padding(32.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        LoadingAnimation()
                        Text(
                            text = uiState.instructionText,
                            style = MaterialTheme.typography.bodyMedium,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }
    }
}

@Composable
@ExperimentalGetImage
private fun CameraPreview(
    modifier: Modifier = Modifier,
    cameraExecutor: ExecutorService,
    onImageAnalysis: (ImageProxy, Bitmap) -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current
    val previewView = remember {
        PreviewView(context).apply {
            scaleType = PreviewView.ScaleType.FILL_CENTER
        }
    }

    // Camera binding setup
    LaunchedEffect(Unit) {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(context)

        try {
            val cameraProvider = cameraProviderFuture.get()

            // Unbind all use cases before rebinding
            cameraProvider.unbindAll()

            // Preview use case
            val preview = CameraPreview.Builder()
                .build()
                .also {
                    it.surfaceProvider = previewView.surfaceProvider
                }

            // Image analysis use case
            val imageAnalysis = androidx.camera.core.ImageAnalysis.Builder()
                .setBackpressureStrategy(androidx.camera.core.ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .build()
                .also { analysis ->
                    analysis.setAnalyzer(cameraExecutor) { imageProxy ->
                        try {
                            val bitmap = imageProxyToBitmap(imageProxy)
                            if (bitmap != null) {
                                onImageAnalysis(imageProxy, bitmap)
                            } else {
                                imageProxy.close()
                            }
                        } catch (e: Exception) {
                            imageProxy.close()
                        }
                    }
                }

            // Select front camera
            val cameraSelector = CameraSelector.DEFAULT_FRONT_CAMERA

            // Bind use cases to camera
            cameraProvider.bindToLifecycle(
                lifecycleOwner,
                cameraSelector,
                preview,
                imageAnalysis
            )

        } catch (exc: Exception) {
            exc.printStackTrace()
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            // Camera cleanup handled by lifecycle
        }
    }

    // Simple AndroidView without update block
    AndroidView(
        factory = { previewView },
        modifier = modifier
    )
}

@Composable
private fun TopInfoSection(
    modifier: Modifier = Modifier,
    action: String
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "Verifikasi Wajah",
            style = MaterialTheme.typography.titleMedium,
            color = Color.White,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = if (action == "checkin") "Check In" else "Check Out",
            style = MaterialTheme.typography.bodyMedium,
            color = Color.White.copy(alpha = 0.9f),
            fontWeight = FontWeight.Medium,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
private fun InstructionSection(
    modifier: Modifier = Modifier,
    uiState: FaceScannerState,
    onRetryClick: () -> Unit,
    onCloseClick: () -> Unit
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.50f)
        ),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Status Icon
            val icon = when (uiState.livenessState) {
                LivenessState.WAITING_FOR_LIVENESS -> {
                    if (uiState.currentChallenge == LivenessChallenge.BLINK) {
                        Icons.Default.Visibility
                    } else {
                        Icons.Default.SentimentSatisfied
                    }
                }

                LivenessState.SUCCESS -> Icons.Default.SentimentSatisfied
                else -> Icons.Default.Visibility
            }

            val iconColor = when (uiState.livenessState) {
                LivenessState.SUCCESS -> Color(0xFF00FFC2)
                LivenessState.FAILURE, LivenessState.TIMEOUT -> Color(0xFFFF3B30)
                LivenessState.WAITING_FOR_LIVENESS -> Color(0xFFFFD60A)
                else -> Color(0xFF00A3FF)
            }

            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(36.dp),
                tint = iconColor
            )

            // Instruction Text
            Text(
                text = uiState.instructionText,
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
                fontWeight = FontWeight.Medium
            )

            // Progress Indicator (for countdown)
            if (uiState.showCountdown && uiState.livenessState != LivenessState.SUCCESS) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    LinearProgressIndicator(
                        progress = uiState.progress,
                        color = Blue_500,
                        modifier = Modifier
                            .weight(1f)
                            .height(3.dp)
                            .clip(RoundedCornerShape(2.dp))
                    )
                    Text(
                        text = "${uiState.timeRemaining}s",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                    )
                }
            }

            // Error Message
            uiState.errorMessage?.let { errorMessage ->
                Text(
                    text = errorMessage,
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFFFF3B30),
                    textAlign = TextAlign.Center
                )
            }

            // Action Buttons
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Close Button menggunakan StatefulButton dengan state Error
                StatefulButton(
                    text = "Tutup",
                    onClick = onCloseClick,
                    modifier = Modifier.weight(1f),
                    style = ButtonStyle.Outlined,
                    stateType = ButtonStateType.Error,
                    enabled = true
                )

                // Retry Button (show when failed or timeout) menggunakan StatefulButton
                if (uiState.livenessState == LivenessState.FAILURE ||
                    uiState.livenessState == LivenessState.TIMEOUT
                ) {
                    StatefulButton(
                        text = "Coba Lagi",
                        onClick = onRetryClick,
                        modifier = Modifier.weight(1f),
                        style = ButtonStyle.Elevated,
                        stateType = ButtonStateType.Info,
                        enabled = true
                    )
                }
            }
        }
    }
}

// Helper function to convert ImageProxy to Bitmap
@ExperimentalGetImage
private fun imageProxyToBitmap(imageProxy: ImageProxy): Bitmap? {
    return try {
        // Get the YUV_420_888 image from camera
        val image = imageProxy.image
        if (image != null) {
            val planes = image.planes
            val yBuffer = planes[0].buffer
            val uBuffer = planes[1].buffer
            val vBuffer = planes[2].buffer

            val ySize = yBuffer.remaining()
            val uSize = uBuffer.remaining()
            val vSize = vBuffer.remaining()

            val nv21 = ByteArray(ySize + uSize + vSize)

            // U and V are swapped
            yBuffer.get(nv21, 0, ySize)
            vBuffer.get(nv21, ySize, vSize)
            uBuffer.get(nv21, ySize + vSize, uSize)

            val yuvImage = android.graphics.YuvImage(
                nv21,
                android.graphics.ImageFormat.NV21,
                image.width,
                image.height,
                null
            )

            val out = java.io.ByteArrayOutputStream()
            yuvImage.compressToJpeg(
                android.graphics.Rect(0, 0, image.width, image.height),
                75,
                out
            )

            val imageBytes = out.toByteArray()
            val bitmap =
                android.graphics.BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)

            // Close the output stream
            out.close()
            bitmap
        } else {
            null
        }
    } catch (e: Exception) {
        null
    }
}

// Permission UI Components
@Composable
private fun CameraPermissionRationale(
    onRequestPermission: () -> Unit,
    onCloseClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(32.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            ),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Visibility,
                    contentDescription = null,
                    modifier = Modifier.size(64.dp),
                    tint = MaterialTheme.colorScheme.primary
                )

                Text(
                    text = "Izin Kamera Diperlukan",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )

                Text(
                    text = "Aplikasi memerlukan akses kamera untuk melakukan verifikasi wajah. Fitur ini membantu memastikan keamanan absensi Anda.",
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
                )

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    OutlinedButton(
                        onClick = onCloseClick,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Batal")
                    }

                    Button(
                        onClick = onRequestPermission,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Berikan Izin")
                    }
                }
            }
        }
    }
}

@Composable
private fun CameraPermissionDenied(
    onRequestPermission: () -> Unit,
    onCloseClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(32.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            ),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = null,
                    modifier = Modifier.size(64.dp),
                    tint = Color(0xFFFF3B30)
                )

                Text(
                    text = "Akses Kamera Dibutuhkan",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )

                Text(
                    text = "Tanpa akses kamera, fitur verifikasi wajah tidak dapat berfungsi. Silakan berikan izin kamera untuk melanjutkan proses absensi.",
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
                )

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    OutlinedButton(
                        onClick = onCloseClick,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Tutup")
                    }

                    Button(
                        onClick = onRequestPermission,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Coba Lagi")
                    }
                }
            }
        }
    }
}

// Preview Composables for different states
@Preview(showBackground = true, name = "Check In - Initial State")
@Composable
fun PreviewFaceScannerCheckIn() {
    MaterialTheme {
        FaceScannerScreenPreview(
            action = "checkin",
            state = FaceScannerState(
                livenessState = LivenessState.IDLE,
                instructionText = "Posisikan wajah Anda dalam frame",
                isProcessing = false,
                showCountdown = false
            )
        )
    }
}

@Preview(showBackground = true, name = "Check Out - Waiting for Liveness")
@Composable
fun PreviewFaceScannerCheckOutWaiting() {
    MaterialTheme {
        FaceScannerScreenPreview(
            action = "checkout",
            state = FaceScannerState(
                livenessState = LivenessState.WAITING_FOR_LIVENESS,
                currentChallenge = LivenessChallenge.BLINK,
                instructionText = "Silakan berkedip untuk verifikasi",
                isProcessing = false,
                showCountdown = true,
                progress = 0.6f,
                timeRemaining = 4
            )
        )
    }
}

@Preview(showBackground = true, name = "Processing State")
@Composable
fun PreviewFaceScannerProcessing() {
    MaterialTheme {
        FaceScannerScreenPreview(
            action = "checkin",
            state = FaceScannerState(
                livenessState = LivenessState.VERIFYING_FACE,
                instructionText = "Memproses verifikasi wajah...",
                isProcessing = true,
                showCountdown = false
            )
        )
    }
}

@Preview(showBackground = true, name = "Success State")
@Composable
fun PreviewFaceScannerSuccess() {
    MaterialTheme {
        FaceScannerScreenPreview(
            action = "checkin",
            state = FaceScannerState(
                livenessState = LivenessState.SUCCESS,
                instructionText = "Verifikasi berhasil!",
                isProcessing = false,
                showCountdown = false
            )
        )
    }
}

@Preview(showBackground = true, name = "Failure State")
@Composable
fun PreviewFaceScannerFailure() {
    MaterialTheme {
        FaceScannerScreenPreview(
            action = "checkout",
            state = FaceScannerState(
                livenessState = LivenessState.FAILURE,
                instructionText = "Verifikasi gagal",
                errorMessage = "Wajah tidak terdeteksi dengan jelas. Silakan coba lagi.",
                isProcessing = false,
                showCountdown = false
            )
        )
    }
}

@Preview(showBackground = true, name = "Timeout State")
@Composable
fun PreviewFaceScannerTimeout() {
    MaterialTheme {
        FaceScannerScreenPreview(
            action = "checkin",
            state = FaceScannerState(
                livenessState = LivenessState.TIMEOUT,
                instructionText = "Waktu habis",
                errorMessage = "Waktu verifikasi telah habis. Silakan coba lagi.",
                isProcessing = false,
                showCountdown = false
            )
        )
    }
}

// Simplified preview composable without camera and ViewModel dependencies
@Composable
private fun FaceScannerScreenPreview(
    action: String,
    state: FaceScannerState
) {
    Box(modifier = Modifier.fillMaxSize()) {
        // Mock camera background
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black)
        )

        // Top Info Section
        TopInfoSection(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 48.dp, start = 24.dp, end = 24.dp),
            action = action
        )

        // Instruction Section
        InstructionSection(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 48.dp, start = 24.dp, end = 24.dp),
            uiState = state,
            onRetryClick = {},
            onCloseClick = {}
        )

        // Loading Overlay for processing state
        if (state.isProcessing) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.3f)),
                contentAlignment = Alignment.Center
            ) {
                Card(
                    modifier = Modifier.padding(32.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        CircularProgressIndicator()
                        Text(
                            text = state.instructionText,
                            style = MaterialTheme.typography.bodyMedium,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }
    }
}
