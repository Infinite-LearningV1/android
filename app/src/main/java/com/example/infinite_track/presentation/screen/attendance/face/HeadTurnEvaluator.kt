package com.example.infinite_track.presentation.screen.attendance.face

import com.example.infinite_track.data.face.LivenessResult

/**
 * Pure evaluator for head-turn liveness challenges based on ML Kit's `headEulerAngleY`.
 *
 * Front camera convention: a positive `eulerY` means the user turns toward their own right,
 * negative toward their own left. Kept free of ML Kit types for JVM unit testing.
 */
object HeadTurnEvaluator {
    private const val SUCCESS_DEGREES = 22f
    private const val PROGRESS_DEGREES = 10f

    fun evaluate(
        eulerY: Float,
        direction: LivenessChallenge,
        threshold: Float = SUCCESS_DEGREES
    ): LivenessResult {
        val signed = when (direction) {
            LivenessChallenge.TURN_RIGHT -> eulerY
            LivenessChallenge.TURN_LEFT -> -eulerY
            else -> return LivenessResult.FAILURE
        }
        return when {
            signed >= threshold -> LivenessResult.SUCCESS
            signed >= PROGRESS_DEGREES -> LivenessResult.IN_PROGRESS
            else -> LivenessResult.FAILURE
        }
    }
}
