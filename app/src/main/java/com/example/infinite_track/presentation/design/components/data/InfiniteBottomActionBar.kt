package com.example.infinite_track.presentation.design.components.data

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.example.infinite_track.presentation.design.components.button.InfiniteButton
import com.example.infinite_track.presentation.design.components.button.InfiniteButtonState
import com.example.infinite_track.presentation.design.components.button.InfiniteButtonVariant
import com.example.infinite_track.presentation.design.components.surface.InfiniteSurface
import com.example.infinite_track.presentation.design.tokens.InfiniteDensity
import com.example.infinite_track.presentation.design.tokens.InfiniteSemantic
import com.example.infinite_track.presentation.design.tokens.InfiniteSize
import com.example.infinite_track.presentation.design.tokens.InfiniteSurfaceVariant

data class InfiniteAction(
    val text: String,
    val icon: ImageVector? = null,
    val variant: InfiniteButtonVariant = InfiniteButtonVariant.Primary,
    val state: InfiniteButtonState = InfiniteButtonState.Enabled,
    val onClick: () -> Unit
)

enum class InfiniteActionLayout {
    Horizontal,
    Vertical
}

@Composable
fun InfiniteBottomActionBar(
    primaryAction: InfiniteAction,
    modifier: Modifier = Modifier,
    secondaryAction: InfiniteAction? = null,
    tertiaryAction: InfiniteAction? = null,
    layout: InfiniteActionLayout = InfiniteActionLayout.Horizontal
) {
    val actions = listOfNotNull(secondaryAction, tertiaryAction, primaryAction)
    InfiniteSurface(
        modifier = modifier.fillMaxWidth(),
        variant = InfiniteSurfaceVariant.Glass,
        semantic = InfiniteSemantic.Neutral,
        size = InfiniteSize.Medium,
        density = InfiniteDensity.Compact
    ) {
        if (layout == InfiniteActionLayout.Vertical) {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                actions.forEach { action -> ActionButton(action = action, fullWidth = true) }
            }
        } else {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                actions.forEach { action -> ActionButton(action = action, modifier = Modifier.weight(1f), fullWidth = true) }
            }
        }
    }
}

@Composable
private fun ActionButton(
    action: InfiniteAction,
    modifier: Modifier = Modifier,
    fullWidth: Boolean
) {
    InfiniteButton(
        text = action.text,
        onClick = action.onClick,
        modifier = modifier,
        variant = action.variant,
        state = action.state,
        leadingIcon = action.icon,
        fullWidth = fullWidth
    )
}
