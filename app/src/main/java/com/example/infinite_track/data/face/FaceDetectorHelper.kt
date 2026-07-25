package com.example.infinite_track.data.face

import android.graphics.Bitmap
import androidx.annotation.OptIn
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageProxy
import com.example.infinite_track.domain.model.face.LivenessResult
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.face.Face
import com.google.mlkit.vision.face.FaceDetection
import com.google.mlkit.vision.face.FaceDetector
import com.google.mlkit.vision.face.FaceDetectorOptions
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Result of a detection pass: the primary (largest) face plus how many faces were in frame,
 * so callers can distinguish a single face from a multiple-faces situation.
 */
data class DetectedFaces(
    val primary: Face,
    val totalFaces: Int
)

/**
 * Helper class for ML Kit Face Detection operations
 * Handles face detection, liveness verification (blink/smile), and face extraction
 * FIXED: Added proper reinitialization support
 */
@Singleton
class FaceDetectorHelper @Inject constructor() {

    companion object {
        // More flexible thresholds for progressive feedback
        private const val BLINK_HIGH_THRESHOLD = 0.4f // Original threshold for SUCCESS
        private const val BLINK_LOW_THRESHOLD = 0.6f  // Lower threshold for IN_PROGRESS

        private const val SMILE_HIGH_THRESHOLD = 0.7f // Original threshold for SUCCESS
        private const val SMILE_MEDIUM_THRESHOLD = 0.4f // Medium threshold for IN_PROGRESS
    }

    // PERBAIKAN: Buat detector nullable dan reinitializable
    private var _faceDetector: FaceDetector? = null

    // Property untuk mengakses detector yang selalu valid
    private val faceDetector: FaceDetector
        get() {
            // Jika detector null atau sudah closed, buat yang baru
            if (_faceDetector == null) {
                _faceDetector = createNewDetector()
            }
            return _faceDetector!!
        }

    /**
     * Create new ML Kit Face Detector instance
     */
    private fun createNewDetector(): FaceDetector {
        val options = FaceDetectorOptions.Builder()
            .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_FAST)
            .setLandmarkMode(FaceDetectorOptions.LANDMARK_MODE_ALL)
            .setClassificationMode(FaceDetectorOptions.CLASSIFICATION_MODE_ALL)
            .setMinFaceSize(0.15f) // Minimum face size relative to image
            .enableTracking() // Enable face tracking for better performance
            .build()

        return FaceDetection.getClient(options)
    }

    /**
     * Force reinitialize detector - call this when reset is needed
     */
    fun reinitialize() {
        try {
            _faceDetector?.close()
        } catch (e: Exception) {
            // Silently handle close errors
        }
        _faceDetector = null
        // Detector akan dibuat ulang saat pertama kali diakses
    }

    /**
     * Detects faces in the given image frame
     * @param imageProxy Camera image frame from CameraX
     * @param onResult Callback with detection result
     */
    @OptIn(ExperimentalGetImage::class)
    fun detect(imageProxy: ImageProxy, onResult: (Result<Face>) -> Unit) {
        detectFaces(imageProxy) { result ->
            onResult(result.map { it.primary })
        }
    }

    /**
     * Detects faces and reports the primary (largest) face together with the total face count,
     * so callers can react to a multiple-faces situation.
     * @param imageProxy Camera image frame from CameraX
     * @param onResult Callback with the detection result
     */
    @OptIn(ExperimentalGetImage::class)
    fun detectFaces(imageProxy: ImageProxy, onResult: (Result<DetectedFaces>) -> Unit) {
        val mediaImage = imageProxy.image
        if (mediaImage != null) {
            val image = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)

            try {
                // Gunakan property yang akan otomatis reinitialize jika perlu
                val detector = faceDetector

                detector.process(image)
                    .addOnSuccessListener { faces ->
                        val selection = FaceSelection.select(faces) {
                            it.boundingBox.width() * it.boundingBox.height()
                        }
                        if (selection != null) {
                            onResult(
                                Result.success(
                                    DetectedFaces(
                                        primary = selection.primary,
                                        totalFaces = selection.totalFaces
                                    )
                                )
                            )
                        } else {
                            onResult(Result.failure(Exception("No faces detected")))
                        }
                    }
                    .addOnFailureListener { exception ->
                        // Jika detector closed, coba reinitialize
                        if (exception.message?.contains("closed") == true) {
                            reinitialize()
                            onResult(Result.failure(Exception("Detector was closed, please try again")))
                        } else {
                            onResult(Result.failure(exception))
                        }
                    }
                    .addOnCompleteListener {
                        // Clean up resources - ALWAYS close imageProxy here
                        imageProxy.close()
                    }
            } catch (e: Exception) {
                if (e.message?.contains("closed") == true) {
                    reinitialize()
                }
                onResult(Result.failure(e))
                imageProxy.close()
            }
        } else {
            onResult(Result.failure(Exception("Image is null")))
            imageProxy.close()
        }
    }

    /**
     * Verifies if the person is blinking (liveness detection) with progressive feedback
     * @param face Detected face from ML Kit
     * @return LivenessResult indicating blink detection status
     */
    fun verifyBlink(face: Face): LivenessResult {
        val leftEyeOpenProbability = face.leftEyeOpenProbability
        val rightEyeOpenProbability = face.rightEyeOpenProbability

        return if (leftEyeOpenProbability != null && rightEyeOpenProbability != null) {
            val avgEyeOpenProbability = (leftEyeOpenProbability + rightEyeOpenProbability) / 2f

            when {
                // SUCCESS: Both eyes clearly closed (blinking)
                leftEyeOpenProbability < BLINK_HIGH_THRESHOLD && rightEyeOpenProbability < BLINK_HIGH_THRESHOLD -> {
                    LivenessResult.SUCCESS
                }
                // IN_PROGRESS: One eye closed or both eyes partially closed
                avgEyeOpenProbability < BLINK_LOW_THRESHOLD -> {
                    LivenessResult.IN_PROGRESS
                }
                // FAILURE: Eyes too open
                else -> {
                    LivenessResult.FAILURE
                }
            }
        } else {
            LivenessResult.FAILURE // Cannot determine blink if probabilities are not available
        }
    }

    /**
     * Verifies if the person is smiling (liveness detection) with progressive feedback
     * @param face Detected face from ML Kit
     * @return LivenessResult indicating smile detection status
     */
    fun verifySmile(face: Face): LivenessResult {
        val smilingProbability = face.smilingProbability

        return if (smilingProbability != null) {
            when {
                // SUCCESS: Strong smile detected
                smilingProbability > SMILE_HIGH_THRESHOLD -> {
                    LivenessResult.SUCCESS
                }
                // IN_PROGRESS: Moderate smile, encourage user to smile more
                smilingProbability > SMILE_MEDIUM_THRESHOLD -> {
                    LivenessResult.IN_PROGRESS
                }
                // FAILURE: Little to no smile
                else -> {
                    LivenessResult.FAILURE
                }
            }
        } else {
            LivenessResult.FAILURE // Cannot determine smile if probability is not available
        }
    }

    /**
     * Extracts and preprocesses face bitmap for consistent embedding generation
     * @param face Detected face from ML Kit
     * @param image Source bitmap from camera
     * @return Preprocessed face bitmap or null if extraction fails
     */
    fun extractFaceBitmap(face: Face, image: Bitmap): Bitmap? {
        return try {
            val boundingBox = face.boundingBox

            // Add padding around the face for better context (10% on each side)
            val padding = (boundingBox.width() * 0.1f).toInt()

            // Calculate expanded bounding box with padding
            val left = maxOf(0, boundingBox.left - padding)
            val top = maxOf(0, boundingBox.top - padding)
            val right = minOf(image.width, boundingBox.right + padding)
            val bottom = minOf(image.height, boundingBox.bottom + padding)

            // Validate that we have a valid crop area
            if (left < right && top < bottom) {
                // Extract face with padding
                val croppedBitmap =
                    Bitmap.createBitmap(image, left, top, right - left, bottom - top)

                // Standardisasi ukuran dan format
                val standardizedBitmap = standardizeFaceBitmap(croppedBitmap)

                // Clean up intermediate bitmap
                if (croppedBitmap != standardizedBitmap) {
                    croppedBitmap.recycle()
                }

                standardizedBitmap
            } else {
                null
            }
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Standardizes face bitmap to consistent size and format
     * This ensures consistent preprocessing for both profile and camera images
     */
    private fun standardizeFaceBitmap(faceBitmap: Bitmap): Bitmap {
        // PERBAIKAN: Gunakan ukuran yang sama dengan FaceProcessor (112x112)
        val standardWidth = 112  // Sama dengan IMAGE_SIZE di FaceProcessor
        val standardHeight = 112  // Sama dengan IMAGE_SIZE di FaceProcessor

        return try {
            // Resize to standard dimensions with high quality
            Bitmap.createScaledBitmap(faceBitmap, standardWidth, standardHeight, true)
        } catch (e: Exception) {
            faceBitmap // Return original if standardization fails
        }
    }

    /**
     * Checks if a face is well-positioned for verification
     * @param face Detected face
     * @param imageWidth Width of the camera frame
     * @param imageHeight Height of the camera frame
     * @return true if face is properly positioned
     */
    fun isFaceWellPositioned(face: Face, imageWidth: Int, imageHeight: Int): Boolean {
        val boundingBox = face.boundingBox
        val faceWidth = boundingBox.width()
        val faceHeight = boundingBox.height()

        // Check if face is not too small or too large
        val minFaceSize = minOf(imageWidth, imageHeight) * 0.2f
        val maxFaceSize = minOf(imageWidth, imageHeight) * 0.8f
        val faceSize = minOf(faceWidth, faceHeight)

        if (faceSize < minFaceSize || faceSize > maxFaceSize) {
            return false
        }

        // Check if face is roughly centered
        val faceCenterX = boundingBox.centerX()
        val faceCenterY = boundingBox.centerY()
        val imageCenterX = imageWidth / 2f
        val imageCenterY = imageHeight / 2f

        val maxOffsetX = imageWidth * 0.25f
        val maxOffsetY = imageHeight * 0.25f

        return kotlin.math.abs(faceCenterX - imageCenterX) < maxOffsetX &&
                kotlin.math.abs(faceCenterY - imageCenterY) < maxOffsetY
    }

    /**
     * Clean up resources when done
     * PERBAIKAN: Tambahkan null check
     */
    fun release() {
        try {
            _faceDetector?.close()
            _faceDetector = null
        } catch (e: Exception) {
        }
    }
}
