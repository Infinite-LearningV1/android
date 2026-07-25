package com.example.infinite_track.domain.use_case.auth

import android.graphics.Bitmap
import com.example.infinite_track.data.face.FaceProcessor
import com.example.infinite_track.domain.repository.AuthRepository
import kotlinx.coroutines.flow.first
import javax.inject.Inject

/**
 * Use case for verifying face against stored embedding
 * Handles face comparison for attendance check-in/check-out
 */
/**
 * Typed result of a face match: the decision plus the similarity/threshold used, so the
 * UI can render diagnostics. Raw embeddings are never included.
 */
data class VerifyFaceMatch(
    val isMatch: Boolean,
    val similarity: Float,
    val threshold: Float
)

class VerifyFaceUseCase @Inject constructor(
    private val faceProcessor: FaceProcessor,
    private val authRepository: AuthRepository
) {
    companion object {
        // PERBAIKAN: Turunkan threshold berdasarkan hasil testing (0.197)
        private const val SIMILARITY_THRESHOLD = 0.15f // Turun dari 0.4f ke 0.15f
    }

    /**
     * Verifies captured face against stored user embedding
     * @param capturedFaceBitmap Bitmap of the captured face (sudah di-preprocess oleh FaceDetectorHelper)
     * @return Result<VerifyFaceMatch> carrying match flag, similarity, and threshold
     */
    suspend operator fun invoke(capturedFaceBitmap: Bitmap): Result<VerifyFaceMatch> {
        return try {
            // Get current user data with stored face embedding
            val currentUser = authRepository.getLoggedInUser().first()
                ?: return Result.failure(Exception("No logged in user found"))

            val storedEmbedding = currentUser.faceEmbedding
                ?: return Result.failure(Exception("No stored face embedding found. Please update your profile."))

            // Generate embedding from captured bitmap (sudah standardized ke 112x112)
            val embeddingResult = generateEmbeddingFromBitmap(capturedFaceBitmap)

            if (embeddingResult.isFailure) {
                return Result.failure(
                    embeddingResult.exceptionOrNull()
                        ?: Exception("Failed to generate embedding from captured face")
                )
            }

            val capturedEmbedding = embeddingResult.getOrNull()!!

            // Compare embeddings using cosine similarity
            val similarity = calculateCosineSimilarity(storedEmbedding, capturedEmbedding)

            // Return true if similarity exceeds threshold
            val isMatch = similarity >= SIMILARITY_THRESHOLD

            Result.success(VerifyFaceMatch(isMatch, similarity, SIMILARITY_THRESHOLD))

        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Generate embedding from bitmap using FaceProcessor
     * Enhanced to work directly with Bitmap without URL
     */
    private suspend fun generateEmbeddingFromBitmap(bitmap: Bitmap): Result<ByteArray> {
        return try {
            // Use the new direct bitmap processing method from FaceProcessor
            faceProcessor.generateEmbeddingFromBitmap(bitmap)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Create temporary file from bitmap for processing
     * This method is now deprecated in favor of direct bitmap processing
     */
    @Deprecated("Use generateEmbeddingFromBitmap directly")
    private fun createTempImageFile(bitmap: Bitmap): java.io.File? {
        return try {
            val tempFile = java.io.File.createTempFile("face_capture", ".jpg")
            val outputStream = java.io.FileOutputStream(tempFile)
            bitmap.compress(Bitmap.CompressFormat.JPEG, 90, outputStream)
            outputStream.flush()
            outputStream.close()
            tempFile
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Calculate cosine similarity between two embeddings
     * @param embedding1 First embedding (stored)
     * @param embedding2 Second embedding (captured)
     * @return Similarity score between 0.0 and 1.0
     */
    private fun calculateCosineSimilarity(embedding1: ByteArray, embedding2: ByteArray): Float {
        if (embedding1.size != embedding2.size) {
            return 0f
        }

        // Convert ByteArray to FloatArray for calculation
        val floats1 = byteArrayToFloatArray(embedding1)
        val floats2 = byteArrayToFloatArray(embedding2)

        var dotProduct = 0.0
        var normA = 0.0
        var normB = 0.0

        for (i in floats1.indices) {
            dotProduct += floats1[i] * floats2[i]
            normA += floats1[i] * floats1[i]
            normB += floats2[i] * floats2[i]
        }

        return if (normA == 0.0 || normB == 0.0) {
            0f
        } else {
            (dotProduct / (kotlin.math.sqrt(normA) * kotlin.math.sqrt(normB))).toFloat()
        }
    }

    /**
     * Convert ByteArray to FloatArray for similarity calculation
     */
    private fun byteArrayToFloatArray(byteArray: ByteArray): FloatArray {
        val floatArray = FloatArray(byteArray.size / 4)
        val buffer = java.nio.ByteBuffer.wrap(byteArray)
        buffer.order(java.nio.ByteOrder.LITTLE_ENDIAN)

        for (i in floatArray.indices) {
            floatArray[i] = buffer.getFloat(i * 4)
        }

        return floatArray
    }
}
