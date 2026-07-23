package com.example.infinite_track.presentation.design.tokens

import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.tween

object InfiniteMotion {
    const val FastDurationMillis = 160
    const val NormalDurationMillis = 240
    const val SlowDurationMillis = 320
    const val EnterDurationMillis = 300
    const val ExitDurationMillis = 200

    private val EmphasizedEasing = CubicBezierEasing(0.2f, 0f, 0f, 1f)
    private val EmphasizedDecelerateEasing = CubicBezierEasing(0.05f, 0.7f, 0.1f, 1f)
    private val EmphasizedAccelerateEasing = CubicBezierEasing(0.3f, 0f, 0.8f, 0.15f)

    fun <T> normalTween() = tween<T>(
        durationMillis = NormalDurationMillis,
        easing = EmphasizedEasing
    )

    fun <T> enterTween() = tween<T>(
        durationMillis = EnterDurationMillis,
        easing = EmphasizedDecelerateEasing
    )

    fun <T> exitTween() = tween<T>(
        durationMillis = ExitDurationMillis,
        easing = EmphasizedAccelerateEasing
    )
}
