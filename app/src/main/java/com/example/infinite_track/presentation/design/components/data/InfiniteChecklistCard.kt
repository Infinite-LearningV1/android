package com.example.infinite_track.presentation.design.components.data

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.example.infinite_track.presentation.design.components.status.semanticIcon
import com.example.infinite_track.presentation.design.components.surface.InfiniteCard
import com.example.infinite_track.presentation.design.tokens.InfiniteSemantic
import com.example.infinite_track.presentation.design.tokens.InfiniteSpacing

data class InfiniteChecklistItem(
    val text: String,
    val semantic: InfiniteSemantic = InfiniteSemantic.Success
)

@Composable
fun InfiniteChecklistCard(
    title: String,
    items: List<InfiniteChecklistItem>,
    modifier: Modifier = Modifier
) {
    InfiniteCard(modifier = modifier.fillMaxWidth()) {
        InfiniteSectionHeader(title = title)
        Column(verticalArrangement = Arrangement.spacedBy(InfiniteSpacing.Default.sm)) {
            items.forEach { item ->
                InfiniteInfoRow(
                    label = null,
                    value = item.text,
                    icon = semanticIcon(item.semantic),
                    semantic = item.semantic
                )
            }
        }
    }
}
