package com.example.infinite_track.presentation.design.tokens

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween

object InfiniteMotion {
    const val FastDurationMillis = 160
    const val NormalDurationMillis = 240
    const val SlowDurationMillis = 320

    fun <T> normalTween() = tween<T>(
        durationMillis = NormalDurationMillis,
        easing = FastOutSlowInEasing
    )
}
