package com.example.infinite_track.presentation.design.components.surface

import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.example.infinite_track.presentation.design.tokens.InfiniteDensity
import com.example.infinite_track.presentation.design.tokens.InfiniteSemantic
import com.example.infinite_track.presentation.design.tokens.InfiniteSize
import com.example.infinite_track.presentation.design.tokens.InfiniteSurfaceVariant

@Composable
fun InfiniteCard(
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
    InfiniteSurface(
        modifier = modifier,
        variant = variant,
        semantic = semantic,
        size = size,
        density = density,
        selected = selected,
        enabled = enabled,
        clickable = clickable,
        showBorder = showBorder,
        showShadow = showShadow,
        onClick = onClick,
        content = content
    )
}
