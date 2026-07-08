package com.example.infinite_track.presentation.components.cameras

import androidx.compose.animation.animateColor
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.animateRectAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.infinite_track.presentation.screen.attendance.face.LivenessState
import com.example.infinite_track.presentation.theme.Blue_500
import com.example.infinite_track.presentation.theme.Blue_Accent_500
import com.example.infinite_track.presentation.theme.Orange_500
import com.example.infinite_track.presentation.theme.Red_Error
import kotlin.math.max

// Warna untuk berbagai status liveness menggunakan Color.kt
val vibrantBlue = Blue_500           // Detecting - menggunakan primary blue
val vibrantGreen = Blue_Accent_500   // Success - menggunakan blue accent
val vibrantRed = Red_Error  // Failure - tetap merah untuk kontras yang jelas
val vibrantYellow = Orange_500       // Waiting for liveness - menggunakan orange
val vibrantOrange = Orange_500       // Liveness detected - menggunakan orange

/**
 * Komponen untuk menampilkan bounding box deteksi wajah dengan animasi dinamis.
 * @param boundingBox Rect dari wajah yang terdeteksi (Compose UI Rect). Jika null, komponen akan hilang.
 * @param livenessState Status dari proses liveness detection untuk menentukan warna
 * @param strokeWidth Lebar garis sudut.
 * @param previewSize Size of the camera preview for coordinate scaling
 * @param imageSize Size of the camera image for coordinate scaling
 * @param isFrontCamera Apakah kamera depan digunakan (untuk mirroring horizontal)
 */
@Composable
fun FaceBoundingBox(
    modifier: Modifier = Modifier,
    boundingBox: Rect?,
    livenessState: LivenessState,
    strokeWidth: Dp = 3.dp,
    previewSize: Size? = null,
    imageSize: Size? = null,
    isFrontCamera: Boolean = true
) {
    // State untuk animasi saat boundingBox null (menghilang) atau muncul.
    val alpha by animateFloatAsState(
        targetValue = if (boundingBox != null) 1f else 0f,
        animationSpec = tween(durationMillis = 300),
        label = "alpha"
    )

    // Hitung mapping bounding box dari koordinat image ke preview menggunakan center-crop scaling
    val mappedBoundingBox = if (boundingBox != null && previewSize != null && imageSize != null) {
        // Center-crop: skala yang memenuhi preview dan mungkin terpotong di salah satu sisi
        val scale = max(previewSize.width / imageSize.width, previewSize.height / imageSize.height)
        val scaledImageWidth = imageSize.width * scale
        val scaledImageHeight = imageSize.height * scale

        // Offset agar gambar yang sudah diskalakan berada di tengah preview
        val offsetX = (previewSize.width - scaledImageWidth) / 2f
        val offsetY = (previewSize.height - scaledImageHeight) / 2f

        // Peta rect ke koordinat preview
        val left = offsetX + boundingBox.left * scale
        val top = offsetY + boundingBox.top * scale
        val right = offsetX + boundingBox.right * scale
        val bottom = offsetY + boundingBox.bottom * scale

        // Opsi mirroring horizontal untuk kamera depan
        val rectInPreview = if (isFrontCamera) {
            Rect(
                left = previewSize.width - right,
                top = top,
                right = previewSize.width - left,
                bottom = bottom
            )
        } else {
            Rect(left, top, right, bottom)
        }

        // Jaga agar tetap dalam batas preview
        Rect(
            left = rectInPreview.left.coerceIn(0f, previewSize.width),
            top = rectInPreview.top.coerceIn(0f, previewSize.height),
            right = rectInPreview.right.coerceIn(0f, previewSize.width),
            bottom = rectInPreview.bottom.coerceIn(0f, previewSize.height)
        )
    } else {
        // Fallback ke rect asli bila data scaling tidak tersedia
        boundingBox
    }

    // State untuk animasi posisi dan ukuran dengan responsif tracking mengikuti wajah
    val animatedBoundingBox by animateRectAsState(
        targetValue = mappedBoundingBox ?: Rect.Zero,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "bounds"
    )

    // Tentukan warna berdasarkan LivenessState dengan perubahan dinamis
    val baseColor = when (livenessState) {
        LivenessState.IDLE -> vibrantBlue
        LivenessState.DETECTING_FACE -> vibrantBlue
        LivenessState.WAITING_FOR_LIVENESS -> vibrantYellow
        LivenessState.LOW_LIGHT -> vibrantYellow
        LivenessState.LIVENESS_DETECTED -> vibrantGreen // Langsung hijau saat liveness terdeteksi
        LivenessState.VERIFYING_FACE -> vibrantOrange
        LivenessState.SUCCESS -> vibrantGreen
        LivenessState.FAILURE -> vibrantRed
        LivenessState.TIMEOUT -> vibrantRed
    }

    // Animasi glow yang berubah berdasarkan status dengan feedback visual
    val infiniteTransition = rememberInfiniteTransition(label = "glow")
    val glowColor by infiniteTransition.animateColor(
        initialValue = baseColor,
        targetValue = when (livenessState) {
            LivenessState.SUCCESS, LivenessState.LIVENESS_DETECTED -> {
                vibrantGreen.copy(alpha = 0.9f)
            }

            LivenessState.FAILURE, LivenessState.TIMEOUT -> {
                vibrantRed.copy(alpha = 0.7f)
            }

            LivenessState.WAITING_FOR_LIVENESS,
            LivenessState.LOW_LIGHT -> {
                vibrantYellow.copy(alpha = 0.8f)
            }

            else -> {
                baseColor.copy(alpha = 0.8f)
            }
        },
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = when (livenessState) {
                    LivenessState.WAITING_FOR_LIVENESS,
                    LivenessState.LOW_LIGHT -> 800 // Faster pulse for waiting/guidance
                    LivenessState.LIVENESS_DETECTED -> 400 // Very fast for success feedback
                    else -> 1500
                },
                easing = FastOutSlowInEasing
            ),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glowColor"
    )

    val strokeWidthPx = with(LocalDensity.current) { strokeWidth.toPx() }

    if (alpha > 0) {
        Canvas(
            modifier = modifier.fillMaxSize()
        ) {
            // Gambar sudut-sudut penanda dengan warna dinamis yang mengikuti status
            drawCornerBrackets(
                rect = animatedBoundingBox,
                alpha = alpha,
                strokeWidth = strokeWidthPx,
                baseColor = baseColor,
                glowColor = glowColor,
                livenessState = livenessState
            )
        }
    }
}

private fun DrawScope.drawCornerBrackets(
    rect: Rect,
    alpha: Float,
    strokeWidth: Float,
    baseColor: Color,
    glowColor: Color,
    livenessState: LivenessState
) {
    if (rect.width == 0f || rect.height == 0f) return

    val cornerLength = minOf(rect.width, rect.height) * 0.2f

    // Brush yang berubah berdasarkan status
    val brush = when (livenessState) {
        LivenessState.SUCCESS -> {
            Brush.linearGradient(
                colors = listOf(vibrantGreen, vibrantGreen.copy(alpha = 0.8f)),
                start = rect.topLeft,
                end = rect.bottomRight
            )
        }

        LivenessState.FAILURE, LivenessState.TIMEOUT -> {
            Brush.linearGradient(
                colors = listOf(vibrantRed, vibrantRed.copy(alpha = 0.8f)),
                start = rect.topLeft,
                end = rect.bottomRight
            )
        }

        else -> {
            Brush.linearGradient(
                colors = listOf(baseColor, glowColor),
                start = rect.topLeft,
                end = rect.bottomRight
            )
        }
    }

    // Top-Left Corner
    drawLine(
        brush,
        Offset(rect.left, rect.top + cornerLength),
        rect.topLeft,
        strokeWidth,
        alpha = alpha
    )
    drawLine(
        brush,
        Offset(rect.left + cornerLength, rect.top),
        rect.topLeft,
        strokeWidth,
        alpha = alpha
    )

    // Top-Right Corner
    drawLine(
        brush,
        Offset(rect.right - cornerLength, rect.top),
        rect.topRight,
        strokeWidth,
        alpha = alpha
    )
    drawLine(
        brush,
        Offset(rect.right, rect.top + cornerLength),
        rect.topRight,
        strokeWidth,
        alpha = alpha
    )

    // Bottom-Left Corner
    drawLine(
        brush,
        Offset(rect.left, rect.bottom - cornerLength),
        rect.bottomLeft,
        strokeWidth,
        alpha = alpha
    )
    drawLine(
        brush,
        Offset(rect.left + cornerLength, rect.bottom),
        rect.bottomLeft,
        strokeWidth,
        alpha = alpha
    )

    // Bottom-Right Corner
    drawLine(
        brush,
        Offset(rect.right - cornerLength, rect.bottom),
        rect.bottomRight,
        strokeWidth,
        alpha = alpha
    )
    drawLine(
        brush,
        Offset(rect.right, rect.bottom - cornerLength),
        rect.bottomRight,
        strokeWidth,
        alpha = alpha
    )
}

@Preview(showBackground = true, backgroundColor = 0xFF000000)
@Composable
private fun FaceBoundingBoxPreview() {
    MaterialTheme {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black)
        ) {
            FaceBoundingBox(
                boundingBox = Rect(
                    left = 100f,
                    top = 200f,
                    right = 300f,
                    bottom = 400f
                ),
                livenessState = LivenessState.DETECTING_FACE
            )
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF000000)
@Composable
private fun FaceBoundingBoxWaitingPreview() {
    MaterialTheme {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black)
        ) {
            FaceBoundingBox(
                boundingBox = Rect(
                    left = 120f,
                    top = 180f,
                    right = 280f,
                    bottom = 380f
                ),
                livenessState = LivenessState.WAITING_FOR_LIVENESS
            )
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF000000)
@Composable
private fun FaceBoundingBoxSuccessPreview() {
    MaterialTheme {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black)
        ) {
            FaceBoundingBox(
                boundingBox = Rect(
                    left = 110f,
                    top = 190f,
                    right = 290f,
                    bottom = 390f
                ),
                livenessState = LivenessState.SUCCESS
            )
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF000000)
@Composable
private fun FaceBoundingBoxFailurePreview() {
    MaterialTheme {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black)
        ) {
            FaceBoundingBox(
                boundingBox = Rect(
                    left = 90f,
                    top = 210f,
                    right = 310f,
                    bottom = 410f
                ),
                livenessState = LivenessState.FAILURE
            )
        }
    }
}
