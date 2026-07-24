package com.example.infinite_track.presentation.screen.attendance.face

import android.graphics.Bitmap
import androidx.camera.core.ImageProxy
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.infinite_track.data.face.FaceDetectorHelper
import com.example.infinite_track.data.face.LivenessResult
import com.example.infinite_track.domain.use_case.auth.VerifyFaceUseCase
import com.google.mlkit.vision.face.Face
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Enum untuk tantangan liveness detection
 */
enum class LivenessChallenge {
    BLINK, SMILE, TURN_LEFT, TURN_RIGHT
}

/**
 * Enum untuk status proses liveness detection
 */
enum class LivenessState {
    IDLE,
    DETECTING_FACE,
    WAITING_FOR_LIVENESS,
    LOW_LIGHT,
    LIVENESS_DETECTED,
    VERIFYING_FACE,
    SUCCESS,
    FAILURE,
    TIMEOUT
}

/**
 * Data class untuk state lengkap face scanner
 */
data class FaceScannerState(
    val livenessState: LivenessState = LivenessState.IDLE,
    val currentChallenge: LivenessChallenge = LivenessChallenge.BLINK,
    val instructionText: String = "",
    val boundingBox: Rect? = null, // Changed to androidx.compose.ui.geometry.Rect
    val progress: Float = 0f,
    val errorMessage: String? = null,
    val isProcessing: Boolean = false,
    val timeRemaining: Int = 20, // 20 detik sesuai kebutuhan
    val showCountdown: Boolean = false,
    val imageSize: Size? = null, // Add image size for coordinate scaling
    val failureReason: FaceVerificationFailureReason? = null,
    val detectedFaceCount: Int = 0,
    val capturedFacePreview: Bitmap? = null,
    val challengeIndex: Int = 1,
    val challengeTotal: Int = 4,
    val readyToVerify: Boolean = false,
    val similarity: Float? = null,
    val threshold: Float? = null
)

/**
 * ViewModel untuk mengatur logika face scanning dengan liveness detection
 */
@HiltViewModel
class FaceScannerViewModel @Inject constructor(
    private val faceDetectorHelper: FaceDetectorHelper,
    private val verifyFaceUseCase: VerifyFaceUseCase
) : ViewModel() {

    companion object {
        private const val TIMEOUT_SECONDS = 20
        private const val LIVENESS_HOLD_DURATION = 1500L // 1.5 detik hold untuk stabilitas
    }

    // Change from mutableStateOf to StateFlow for better compatibility
    private val _uiState = MutableStateFlow(FaceScannerState())
    val uiState: StateFlow<FaceScannerState> = _uiState.asStateFlow()

    private var timeoutJob: Job? = null
    private var livenessJob: Job? = null
    private var currentDetectedFace: Face? = null
    private var currentImageBitmap: Bitmap? = null
    private val sequencer = LivenessSequencer()

    /**
     * Inisialisasi scanner dengan random challenge
     * DIPUBLIKKAN agar bisa dipanggil dari UI untuk reset
     */
    fun initializeScanner() {
        // STEP 1: Cancel any ongoing jobs first
        timeoutJob?.cancel()
        livenessJob?.cancel()

        // STEP 2: Clear all cached data
        currentDetectedFace = null
        currentImageBitmap = null

        // STEP 3: Reinitialize FaceDetector
        faceDetectorHelper.reinitialize()

        // STEP 4: Reset the fixed 4-challenge liveness sequence
        sequencer.reset()
        val firstChallenge = sequencer.current

        // STEP 5: Reset state ke kondisi benar-benar fresh
        _uiState.value = FaceScannerState(
            currentChallenge = firstChallenge,
            challengeIndex = sequencer.index,
            challengeTotal = sequencer.total,
            instructionText = challengePrompt(firstChallenge),
            livenessState = LivenessState.DETECTING_FACE,
            timeRemaining = TIMEOUT_SECONDS,
            showCountdown = true,
            isProcessing = false,
            errorMessage = null,
            boundingBox = null,
            progress = 0f,
            imageSize = null,
            readyToVerify = false
        )

        // STEP 6: Start fresh per-challenge timeout
        startTimeout()
    }

    /**
     * Reset scanner untuk mencoba lagi
     */
    fun resetScanner() {
        initializeScanner()
    }

    /**
     * Proses frame dari kamera untuk deteksi wajah dan verifikasi liveness
     */
    fun processImageProxy(imageProxy: ImageProxy, imageBitmap: Bitmap) {
        val currentState = _uiState.value.livenessState
        val isProcessing = _uiState.value.isProcessing

        // Hanya blokir jika sedang processing atau scanner sudah berada di state final
        if (
            isProcessing ||
            currentState == LivenessState.SUCCESS ||
            currentState == LivenessState.FAILURE ||
            currentState == LivenessState.TIMEOUT
        ) {
            imageProxy.close()
            return
        }

        currentImageBitmap = imageBitmap

        // Panggil FaceDetectorHelper untuk mendeteksi wajah + jumlah wajah dalam frame
        faceDetectorHelper.detectFaces(imageProxy) { result ->
            result.onSuccess { detected ->
                handleFaceDetected(detected.primary, imageBitmap, detected.totalFaces)
            }.onFailure { exception ->
                handleFaceDetectionError(exception.message ?: "Error mendeteksi wajah")
            }
        }
    }

    /**
     * Handle ketika wajah berhasil terdeteksi
     */
    private fun handleFaceDetected(face: Face, imageBitmap: Bitmap, totalFaces: Int) {
        currentDetectedFace = face
        val imageWidth = imageBitmap.width
        val imageHeight = imageBitmap.height

        // Convert android.graphics.Rect to androidx.compose.ui.geometry.Rect
        val androidRect = face.boundingBox
        val composeRect = Rect(
            left = androidRect.left.toFloat(),
            top = androidRect.top.toFloat(),
            right = androidRect.right.toFloat(),
            bottom = androidRect.bottom.toFloat()
        )

        // Update bounding box dan image size untuk UI dengan coordinate scaling yang proper
        _uiState.value = _uiState.value.copy(
            boundingBox = composeRect,
            imageSize = Size(
                width = imageWidth.toFloat(),
                height = imageHeight.toFloat()
            ),
            detectedFaceCount = totalFaces
        )

        // Jika ada lebih dari satu wajah, minta pengguna menyisakan satu wajah dulu
        if (FaceCountGuidance.reasonFor(totalFaces) == FaceVerificationFailureReason.MULTIPLE_FACES) {
            livenessJob?.cancel()
            _uiState.value = _uiState.value.copy(
                instructionText = "Pastikan hanya ada satu wajah di dalam frame",
                errorMessage = null
            )
            return
        }

        // Cek apakah wajah berada di posisi yang baik
        if (!faceDetectorHelper.isFaceWellPositioned(face, imageWidth, imageHeight)) {
            _uiState.value = _uiState.value.copy(
                livenessState = LivenessState.DETECTING_FACE,
                instructionText = "Posisikan wajah Anda lebih dekat dan di tengah frame",
                errorMessage = null
            )
            return
        }

        if (isLowLight(imageBitmap = imageBitmap, face = face)) {
            livenessJob?.cancel()
            _uiState.value = _uiState.value.copy(
                livenessState = LivenessState.LOW_LIGHT,
                instructionText = "Pencahayaan kurang. Pindah ke area lebih terang sebelum verifikasi dilanjutkan.",
                errorMessage = "Wajah sudah terdeteksi, tetapi pencahayaan belum cukup untuk verifikasi.",
                isProcessing = false
            )
            return
        }

        if (_uiState.value.livenessState == LivenessState.LOW_LIGHT) {
            _uiState.value = _uiState.value.copy(
                livenessState = LivenessState.WAITING_FOR_LIVENESS,
                errorMessage = null,
                instructionText = when (_uiState.value.currentChallenge) {
                    LivenessChallenge.BLINK -> "Pencahayaan membaik. Sekarang kedipkan mata Anda"
                    LivenessChallenge.SMILE -> "Pencahayaan membaik. Sekarang tersenyum"
                    LivenessChallenge.TURN_LEFT -> "Pencahayaan membaik. Sekarang tengok ke kiri"
                    LivenessChallenge.TURN_RIGHT -> "Pencahayaan membaik. Sekarang tengok ke kanan"
                }
            )
        }

        // Wajah sudah di posisi yang baik, lanjut ke pengecekan liveness
        when (_uiState.value.livenessState) {
            LivenessState.DETECTING_FACE -> {
                // Transisi ke waiting for liveness
                _uiState.value = _uiState.value.copy(
                    livenessState = LivenessState.WAITING_FOR_LIVENESS,
                    instructionText = when (_uiState.value.currentChallenge) {
                        LivenessChallenge.BLINK -> "Wajah terdeteksi! Sekarang kedipkan mata Anda"
                        LivenessChallenge.SMILE -> "Wajah terdeteksi! Sekarang tersenyum"
                        LivenessChallenge.TURN_LEFT -> "Wajah terdeteksi! Sekarang tengok ke kiri"
                        LivenessChallenge.TURN_RIGHT -> "Wajah terdeteksi! Sekarang tengok ke kanan"
                    }
                )
            }

            LivenessState.WAITING_FOR_LIVENESS -> {
                // Cek apakah challenge liveness terpenuhi
                checkLivenessChallenge(face)
            }

            else -> {
                // State lain tidak perlu di-handle di sini
            }
        }
    }

    private fun isLowLight(imageBitmap: Bitmap, face: Face): Boolean {
        if (imageBitmap.width <= 1 || imageBitmap.height <= 1) return false

        val boundingBox = face.boundingBox
        val left = boundingBox.left.coerceIn(0, imageBitmap.width - 1)
        val top = boundingBox.top.coerceIn(0, imageBitmap.height - 1)
        val right = boundingBox.right.coerceIn(left + 1, imageBitmap.width)
        val bottom = boundingBox.bottom.coerceIn(top + 1, imageBitmap.height)
        val width = right - left
        val height = bottom - top
        val pixels = IntArray(width * height)

        return try {
            imageBitmap.getPixels(pixels, 0, width, left, top, width, height)
            FaceLightingQuality.evaluate(pixels) == LightingQuality.LOW_LIGHT
        } catch (exception: IllegalArgumentException) {
            false
        }
    }

    /**
     * Cek apakah challenge liveness saat ini terpenuhi dengan progressive feedback
     */
    private fun checkLivenessChallenge(face: Face) {
        val current = _uiState.value.currentChallenge
        val livenessResult = when (current) {
            LivenessChallenge.BLINK -> faceDetectorHelper.verifyBlink(face)
            LivenessChallenge.SMILE -> faceDetectorHelper.verifySmile(face)
            LivenessChallenge.TURN_LEFT, LivenessChallenge.TURN_RIGHT ->
                HeadTurnEvaluator.evaluate(face.headEulerAngleY, current)
        }

        when (livenessResult) {
            LivenessResult.SUCCESS -> {
                // Tantangan saat ini terpenuhi - tahan sebentar lalu maju ke tantangan berikutnya
                _uiState.value = _uiState.value.copy(
                    livenessState = LivenessState.LIVENESS_DETECTED,
                    instructionText = "Bagus! Tetap di posisi..."
                )
                livenessJob?.cancel()
                livenessJob = viewModelScope.launch {
                    delay(LIVENESS_HOLD_DURATION)
                    advanceChallenge()
                }
            }

            LivenessResult.IN_PROGRESS -> {
                _uiState.value = _uiState.value.copy(
                    livenessState = LivenessState.WAITING_FOR_LIVENESS,
                    instructionText = challengeProgressText(current)
                )
            }

            LivenessResult.FAILURE -> {
                _uiState.value = _uiState.value.copy(
                    livenessState = LivenessState.WAITING_FOR_LIVENESS,
                    instructionText = challengeFailureText(current)
                )
            }
        }
    }

    /**
     * Maju ke tantangan berikutnya, atau tandai siap verifikasi jika keempat tantangan selesai.
     */
    private fun advanceChallenge() {
        sequencer.pass()
        if (sequencer.isComplete) {
            timeoutJob?.cancel()
            livenessJob?.cancel()
            _uiState.value = _uiState.value.copy(
                livenessState = LivenessState.LIVENESS_DETECTED,
                readyToVerify = true,
                challengeIndex = sequencer.total,
                instructionText = "Semua tantangan selesai. Tekan Verify untuk melanjutkan.",
                showCountdown = false
            )
        } else {
            val next = sequencer.current
            _uiState.value = _uiState.value.copy(
                currentChallenge = next,
                challengeIndex = sequencer.index,
                livenessState = LivenessState.WAITING_FOR_LIVENESS,
                instructionText = challengePrompt(next),
                errorMessage = null
            )
            // Reset the 20s countdown for the new challenge
            startTimeout()
        }
    }

    /**
     * Dipanggil UI saat pengguna menekan Verify (hanya valid setelah semua tantangan selesai).
     */
    fun onVerifyClicked() {
        if (!_uiState.value.readyToVerify) return
        proceedWithFaceVerification()
    }

    private fun challengePrompt(challenge: LivenessChallenge): String = when (challenge) {
        LivenessChallenge.BLINK -> "Posisikan wajah Anda di dalam frame, lalu kedipkan mata"
        LivenessChallenge.SMILE -> "Posisikan wajah Anda di dalam frame, lalu tersenyum"
        LivenessChallenge.TURN_LEFT -> "Posisikan wajah Anda di dalam frame, lalu tengok ke kiri"
        LivenessChallenge.TURN_RIGHT -> "Posisikan wajah Anda di dalam frame, lalu tengok ke kanan"
    }

    private fun challengeProgressText(challenge: LivenessChallenge): String = when (challenge) {
        LivenessChallenge.BLINK -> "Hampir berhasil! Coba kedipkan kedua mata bersamaan"
        LivenessChallenge.SMILE -> "Bagus! Tersenyum sedikit lebih lebar lagi"
        LivenessChallenge.TURN_LEFT -> "Hampir! Tengok sedikit lagi ke kiri"
        LivenessChallenge.TURN_RIGHT -> "Hampir! Tengok sedikit lagi ke kanan"
    }

    private fun challengeFailureText(challenge: LivenessChallenge): String = when (challenge) {
        LivenessChallenge.BLINK -> "Silakan kedipkan mata Anda dengan jelas"
        LivenessChallenge.SMILE -> "Silakan tersenyum dengan lebih jelas"
        LivenessChallenge.TURN_LEFT -> "Silakan tengok ke kiri dengan jelas"
        LivenessChallenge.TURN_RIGHT -> "Silakan tengok ke kanan dengan jelas"
    }

    /**
     * Lanjutkan dengan verifikasi wajah setelah liveness terkonfirmasi
     */
    private fun proceedWithFaceVerification() {
        val face = currentDetectedFace
        val bitmap = currentImageBitmap

        if (face == null || bitmap == null) {
            handleVerificationFailure(
                FaceVerificationFailureReason.TECHNICAL_FAILURE,
                "Gagal mengambil data wajah"
            )
            return
        }

        _uiState.value = _uiState.value.copy(
            livenessState = LivenessState.VERIFYING_FACE,
            isProcessing = true,
            instructionText = "Memverifikasi identitas Anda...",
            showCountdown = false
        )

        viewModelScope.launch {
            try {
                // Ekstrak bitmap wajah dari gambar penuh
                val faceBitmap = faceDetectorHelper.extractFaceBitmap(face, bitmap)

                if (faceBitmap == null) {
                    handleVerificationFailure(
                        FaceVerificationFailureReason.TECHNICAL_FAILURE,
                        "Gagal mengekstrak wajah dari gambar"
                    )
                    return@launch
                }

                // Simpan foto wajah yang dipakai untuk verifikasi (transient, untuk result surface)
                _uiState.value = _uiState.value.copy(capturedFacePreview = faceBitmap)

                // Verifikasi wajah lalu petakan hasilnya ke reason terdiferensiasi
                val outcome = FaceOutcomeMapper.fromMatch(verifyFaceUseCase(faceBitmap))
                _uiState.value = _uiState.value.copy(
                    similarity = outcome.similarity,
                    threshold = outcome.threshold
                )
                if (outcome.livenessState == LivenessState.SUCCESS) {
                    handleVerificationSuccess()
                } else {
                    val reason = outcome.failureReason
                        ?: FaceVerificationFailureReason.TECHNICAL_FAILURE
                    val message = if (reason == FaceVerificationFailureReason.NOT_MATCHED) {
                        "Wajah tidak cocok dengan data yang tersimpan. Silakan coba lagi."
                    } else {
                        "Gagal memverifikasi wajah. Silakan coba lagi."
                    }
                    handleVerificationFailure(reason, message)
                }
            } catch (e: Exception) {
                handleVerificationFailure(
                    FaceVerificationFailureReason.TECHNICAL_FAILURE,
                    "Terjadi kesalahan: ${e.message}"
                )
            }
        }
    }

    /**
     * Handle sukses verifikasi wajah
     */
    private fun handleVerificationSuccess() {
        timeoutJob?.cancel()
        livenessJob?.cancel()

        _uiState.value = _uiState.value.copy(
            livenessState = LivenessState.SUCCESS,
            failureReason = null,
            isProcessing = false,
            instructionText = "Verifikasi berhasil! Identitas terkonfirmasi.",
            progress = 1f,
            showCountdown = false,
            errorMessage = null
        )
    }

    /**
     * Handle error deteksi wajah
     */
    private fun handleFaceDetectionError(errorMessage: String) {
        // Hanya update instruction jika masih dalam tahap deteksi
        if (_uiState.value.livenessState == LivenessState.DETECTING_FACE) {
            _uiState.value = _uiState.value.copy(
                instructionText = "Mencari wajah... Pastikan wajah terlihat jelas di dalam frame",
                boundingBox = null
            )
        }
    }

    /**
     * Handle kegagalan verifikasi wajah dengan alasan terdiferensiasi
     */
    private fun handleVerificationFailure(
        reason: FaceVerificationFailureReason,
        errorMessage: String
    ) {
        timeoutJob?.cancel()
        livenessJob?.cancel()

        _uiState.value = _uiState.value.copy(
            livenessState = LivenessState.FAILURE,
            failureReason = reason,
            isProcessing = false,
            instructionText = "Verifikasi gagal",
            errorMessage = errorMessage,
            showCountdown = false
        )
    }

    /**
     * Mulai countdown timer untuk timeout
     */
    private fun startTimeout() {
        timeoutJob?.cancel()
        timeoutJob = viewModelScope.launch {
            repeat(TIMEOUT_SECONDS) { second ->
                val remainingTime = TIMEOUT_SECONDS - second
                _uiState.value = _uiState.value.copy(
                    timeRemaining = remainingTime,
                    progress = second.toFloat() / TIMEOUT_SECONDS
                )
                delay(1000)

                // Cek apakah proses sudah selesai
                if (_uiState.value.livenessState == LivenessState.SUCCESS ||
                    _uiState.value.livenessState == LivenessState.FAILURE
                ) {
                    return@launch
                }
            }

            // Timeout tercapai
            handleTimeout()
        }
    }

    /**
     * Handle timeout
     */
    private fun handleTimeout() {
        livenessJob?.cancel()

        _uiState.value = _uiState.value.copy(
            livenessState = LivenessState.TIMEOUT,
            isProcessing = false,
            instructionText = "Waktu habis",
            errorMessage = "Tidak dapat mendeteksi wajah dalam waktu $TIMEOUT_SECONDS detik. Silakan coba lagi.",
            showCountdown = false,
            timeRemaining = 0
        )
    }

    /**
     * Bersihkan resources ketika ViewModel dihancurkan
     */
    override fun onCleared() {
        super.onCleared()
        timeoutJob?.cancel()
        livenessJob?.cancel()
        faceDetectorHelper.release()
    }
}
