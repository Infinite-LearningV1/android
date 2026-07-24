package com.example.infinite_track.domain.use_case.auth

/**
 * Debug-only diagnostics for face matching.
 *
 * Similarity and threshold are biometric-derived signals. They must never reach release
 * logs, analytics, crash reports, navigation arguments, or saved state. This payload only
 * exists to support tuning in debug builds.
 */
data class FaceMatchDiagnostics(
    val similarity: Float,
    val threshold: Float,
    val isMatch: Boolean
)

internal object FaceMatchDiagnosticsFactory {
    /**
     * Returns a diagnostics payload only when [isDebug] is true; otherwise null so callers
     * cannot accidentally leak biometric signals in release builds.
     */
    fun build(
        isDebug: Boolean,
        similarity: Float,
        threshold: Float,
        isMatch: Boolean
    ): FaceMatchDiagnostics? =
        if (isDebug) FaceMatchDiagnostics(similarity, threshold, isMatch) else null
}
