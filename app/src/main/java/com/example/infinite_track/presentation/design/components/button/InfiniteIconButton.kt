package com.example.infinite_track.presentation.design.components.button

import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.example.infinite_track.presentation.design.tokens.InfiniteColors
import com.example.infinite_track.presentation.design.tokens.InfiniteSize

@Composable
fun InfiniteIconButton(
    icon: ImageVector,
    contentDescription: String?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    size: InfiniteSize = InfiniteSize.Medium,
    enabled: Boolean = true,
    selected: Boolean = false
) {
    val buttonSize = when (size) {
        InfiniteSize.Small -> 36.dp
        InfiniteSize.Medium -> 44.dp
        InfiniteSize.Large -> 52.dp
    }
    IconButton(
        onClick = onClick,
        modifier = modifier.size(buttonSize),
        enabled = enabled,
        colors = IconButtonDefaults.iconButtonColors(
            containerColor = if (selected) InfiniteColors.Primary.copy(alpha = 0.14f) else InfiniteColors.Surface.copy(alpha = 0.42f),
            contentColor = if (selected) InfiniteColors.Primary else InfiniteColors.Text
        )
    ) {
        Icon(imageVector = icon, contentDescription = contentDescription, modifier = Modifier.size(buttonSize * 0.48f))
    }
}
