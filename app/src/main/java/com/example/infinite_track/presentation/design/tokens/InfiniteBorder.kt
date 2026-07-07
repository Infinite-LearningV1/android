package com.example.infinite_track.presentation.design.tokens

import androidx.compose.foundation.BorderStroke
import androidx.compose.ui.unit.dp

object InfiniteBorder {
    val Hairline = 1.dp
    val Selected = 1.5.dp

    fun stroke(
        semantic: InfiniteSemantic,
        selected: Boolean = false,
        enabled: Boolean = true
    ): BorderStroke {
        val colors = InfiniteBorderColors.resolve(semantic)
        val alpha = if (enabled) 1f else 0.35f
        return BorderStroke(
            width = if (selected) Selected else Hairline,
            color = colors.copy(alpha = alpha)
        )
    }
}

private object InfiniteBorderColors {
    fun resolve(semantic: InfiniteSemantic) = when (semantic) {
        InfiniteSemantic.Primary -> InfiniteColors.Primary.copy(alpha = 0.34f)
        InfiniteSemantic.Secondary -> InfiniteColors.Secondary.copy(alpha = 0.45f)
        InfiniteSemantic.Success -> InfiniteColors.Success.copy(alpha = 0.45f)
        InfiniteSemantic.Info -> InfiniteColors.Info.copy(alpha = 0.30f)
        InfiniteSemantic.Warning -> InfiniteColors.Warning.copy(alpha = 0.55f)
        InfiniteSemantic.Error -> InfiniteColors.SoftAlert.copy(alpha = 0.42f)
        InfiniteSemantic.Neutral -> InfiniteColors.Neutral.copy(alpha = 0.20f)
    }
}
