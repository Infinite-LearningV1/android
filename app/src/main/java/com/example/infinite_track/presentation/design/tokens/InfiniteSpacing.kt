package com.example.infinite_track.presentation.design.tokens

import androidx.compose.runtime.Immutable
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@Immutable
data class InfiniteSpacingSet(
    val xs: Dp = 4.dp,
    val sm: Dp = 8.dp,
    val md: Dp = 12.dp,
    val lg: Dp = 16.dp,
    val xl: Dp = 24.dp,
    val xxl: Dp = 32.dp
)

object InfiniteSpacing {
    val Default = InfiniteSpacingSet()

    fun contentPadding(size: InfiniteSize, density: InfiniteDensity): Dp {
        return when (density) {
            InfiniteDensity.Compact -> when (size) {
                InfiniteSize.Small -> 8.dp
                InfiniteSize.Medium -> 10.dp
                InfiniteSize.Large -> 12.dp
            }
            InfiniteDensity.Comfortable -> when (size) {
                InfiniteSize.Small -> 10.dp
                InfiniteSize.Medium -> 14.dp
                InfiniteSize.Large -> 18.dp
            }
            InfiniteDensity.Spacious -> when (size) {
                InfiniteSize.Small -> 14.dp
                InfiniteSize.Medium -> 18.dp
                InfiniteSize.Large -> 24.dp
            }
        }
    }

    fun gap(size: InfiniteSize): Dp {
        return when (size) {
            InfiniteSize.Small -> 6.dp
            InfiniteSize.Medium -> 10.dp
            InfiniteSize.Large -> 14.dp
        }
    }
}
