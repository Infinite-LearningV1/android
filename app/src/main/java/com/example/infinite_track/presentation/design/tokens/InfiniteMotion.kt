package com.example.infinite_track.presentation.design.tokens

import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.tween

/** Public adapter for Material 3 v0_103 motion tokens, internal in M3 1.2.1. */
object InfiniteMotion {
    const val DurationShort4 = 200
    const val DurationMedium2 = 300

    val Emphasized = CubicBezierEasing(0.2f, 0f, 0f, 1f)
    val EmphasizedDecelerate = CubicBezierEasing(0.05f, 0.7f, 0.1f, 1f)
    val EmphasizedAccelerate = CubicBezierEasing(0.3f, 0f, 0.8f, 0.15f)

    fun <T> stateChangeTween() = tween<T>(
        durationMillis = DurationShort4,
        easing = Emphasized
    )

    fun <T> enterTween() = tween<T>(
        durationMillis = DurationMedium2,
        easing = EmphasizedDecelerate
    )

    fun <T> exitTween() = tween<T>(
        durationMillis = DurationShort4,
        easing = EmphasizedAccelerate
    )
}
