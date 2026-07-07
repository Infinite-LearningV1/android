package com.example.infinite_track.presentation.design.tokens

import androidx.compose.material3.CardElevation
import androidx.compose.material3.CardDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

object InfiniteElevation {
    val None: Dp = 0.dp
    val Soft: Dp = 2.dp
    val Medium: Dp = 6.dp
    val High: Dp = 10.dp

    fun elevation(variant: InfiniteSurfaceVariant, showShadow: Boolean): Dp {
        if (!showShadow) return None
        return when (variant) {
            InfiniteSurfaceVariant.Elevated -> Medium
            InfiniteSurfaceVariant.SoftGradient -> Soft
            InfiniteSurfaceVariant.Glass -> Soft
            InfiniteSurfaceVariant.StatusTint -> Soft
            InfiniteSurfaceVariant.Default,
            InfiniteSurfaceVariant.Outlined -> None
        }
    }

    @Composable
    fun cardElevation(variant: InfiniteSurfaceVariant, showShadow: Boolean): CardElevation {
        return CardDefaults.cardElevation(defaultElevation = elevation(variant, showShadow))
    }
}
