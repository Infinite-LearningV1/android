package com.example.infinite_track.presentation.screen.attendance.preparation

import com.example.infinite_track.presentation.design.tokens.InfiniteSemantic
import kotlin.math.roundToInt

internal data class WfaSuitabilityPresentation(
    val percentage: Int,
    val semantic: InfiniteSemantic
)

internal object WfaSuitabilityPresentationMapper {
    fun map(score: Double): WfaSuitabilityPresentation {
        val normalizedScore = if (score.isFinite()) {
            score.coerceIn(MIN_SCORE, MAX_SCORE)
        } else {
            MIN_SCORE
        }
        val semantic = when {
            normalizedScore >= POSITIVE_THRESHOLD -> InfiniteSemantic.Success
            normalizedScore >= WARNING_THRESHOLD -> InfiniteSemantic.Warning
            else -> InfiniteSemantic.Error
        }
        return WfaSuitabilityPresentation(
            percentage = (normalizedScore * PERCENT_SCALE).roundToInt(),
            semantic = semantic
        )
    }

    private const val MIN_SCORE = 0.0
    private const val MAX_SCORE = 1.0
    private const val WARNING_THRESHOLD = 0.60
    private const val POSITIVE_THRESHOLD = 0.80
    private const val PERCENT_SCALE = 100
}
