package com.example.infinite_track.presentation.design.tokens

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

object InfiniteRadius {
    val Small: Dp = 10.dp
    val Medium: Dp = 14.dp
    val Large: Dp = 18.dp
    val XLarge: Dp = 24.dp
    val Pill: Dp = 999.dp

    fun radius(size: InfiniteSize): Dp {
        return when (size) {
            InfiniteSize.Small -> Small
            InfiniteSize.Medium -> Medium
            InfiniteSize.Large -> Large
        }
    }

    fun shape(size: InfiniteSize) = RoundedCornerShape(radius(size))
    fun pillShape() = RoundedCornerShape(Pill)
}
