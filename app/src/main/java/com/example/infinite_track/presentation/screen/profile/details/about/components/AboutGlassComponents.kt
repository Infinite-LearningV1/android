package com.example.infinite_track.presentation.screen.profile.details.about.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.infinite_track.presentation.design.components.surface.InfiniteCard
import com.example.infinite_track.presentation.design.tokens.InfiniteSemantic
import com.example.infinite_track.presentation.design.tokens.InfiniteSize
import com.example.infinite_track.presentation.design.tokens.InfiniteSurfaceVariant

@Composable
fun GlassSectionCard(
    modifier: Modifier = Modifier,
    semantic: InfiniteSemantic = InfiniteSemantic.Neutral,
    content: @Composable ColumnScope.() -> Unit
) {
    InfiniteCard(
        modifier = modifier.fillMaxWidth(),
        variant = InfiniteSurfaceVariant.Glass,
        semantic = semantic,
        size = InfiniteSize.Large
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            content = content
        )
    }
}
