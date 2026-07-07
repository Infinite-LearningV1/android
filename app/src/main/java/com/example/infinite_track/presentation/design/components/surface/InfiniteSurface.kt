package com.example.infinite_track.presentation.design.components.surface

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.example.infinite_track.presentation.design.tokens.InfiniteColors
import com.example.infinite_track.presentation.design.tokens.InfiniteDensity
import com.example.infinite_track.presentation.design.tokens.InfiniteElevation
import com.example.infinite_track.presentation.design.tokens.InfiniteSemantic
import com.example.infinite_track.presentation.design.tokens.InfiniteSize
import com.example.infinite_track.presentation.design.tokens.InfiniteSpacing
import com.example.infinite_track.presentation.design.tokens.InfiniteSurfaceVariant
import com.example.infinite_track.presentation.design.tokens.infiniteSemanticColors

@Composable
fun InfiniteSurface(
    modifier: Modifier = Modifier,
    variant: InfiniteSurfaceVariant = InfiniteSurfaceVariant.Glass,
    semantic: InfiniteSemantic = InfiniteSemantic.Neutral,
    size: InfiniteSize = InfiniteSize.Medium,
    density: InfiniteDensity = InfiniteDensity.Comfortable,
    selected: Boolean = false,
    enabled: Boolean = true,
    clickable: Boolean = false,
    showBorder: Boolean = true,
    showShadow: Boolean = true,
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    val semanticColors = infiniteSemanticColors(semantic)
    val shape = com.example.infinite_track.presentation.design.tokens.InfiniteRadius.shape(size)
    val backgroundModifier = if (variant == InfiniteSurfaceVariant.Glass || variant == InfiniteSurfaceVariant.SoftGradient) {
        Modifier.background(InfiniteColors.GlassGradient, shape)
    } else {
        Modifier
    }
    val clickAction = onClick
    val clickModifier = if (clickable && enabled && clickAction != null) {
        Modifier.clickable { clickAction() }
    } else Modifier

    Card(
        modifier = modifier
            .then(backgroundModifier)
            .then(clickModifier),
        shape = shape,
        colors = CardDefaults.cardColors(
            containerColor = when (variant) {
                InfiniteSurfaceVariant.StatusTint -> semanticColors.container
                InfiniteSurfaceVariant.Default -> InfiniteColors.Surface
                InfiniteSurfaceVariant.Outlined -> InfiniteColors.Surface.copy(alpha = 0.72f)
                InfiniteSurfaceVariant.Elevated -> InfiniteColors.Surface
                InfiniteSurfaceVariant.Glass,
                InfiniteSurfaceVariant.SoftGradient -> InfiniteColors.Surface.copy(alpha = 0.58f)
            },
            contentColor = semanticColors.content,
            disabledContainerColor = InfiniteColors.Surface.copy(alpha = 0.42f),
            disabledContentColor = semanticColors.content.copy(alpha = 0.45f)
        ),
        border = if (showBorder || selected) {
            com.example.infinite_track.presentation.design.tokens.InfiniteBorder.stroke(
                semantic = semantic,
                selected = selected,
                enabled = enabled
            )
        } else null,
        elevation = InfiniteElevation.cardElevation(variant, showShadow)
    ) {
        Column(
            modifier = Modifier.padding(InfiniteSpacing.contentPadding(size, density)),
            content = content
        )
    }
}
