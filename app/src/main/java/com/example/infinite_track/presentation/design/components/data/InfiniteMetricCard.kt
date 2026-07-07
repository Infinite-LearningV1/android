package com.example.infinite_track.presentation.design.components.data

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.infinite_track.presentation.design.components.surface.InfiniteCard
import com.example.infinite_track.presentation.design.tokens.InfiniteSemantic
import com.example.infinite_track.presentation.design.tokens.InfiniteSize
import com.example.infinite_track.presentation.design.tokens.InfiniteSurfaceVariant
import com.example.infinite_track.presentation.design.tokens.infiniteSemanticColors

@Composable
fun InfiniteMetricCard(
    title: String,
    value: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    icon: ImageVector? = null,
    semantic: InfiniteSemantic = InfiniteSemantic.Primary,
    size: InfiniteSize = InfiniteSize.Medium,
    variant: InfiniteSurfaceVariant = InfiniteSurfaceVariant.Glass,
    onClick: (() -> Unit)? = null
) {
    val colors = infiniteSemanticColors(semantic)
    InfiniteCard(
        modifier = modifier,
        variant = variant,
        semantic = semantic,
        size = size,
        clickable = onClick != null,
        onClick = onClick
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
            icon?.let { Icon(imageVector = it, contentDescription = null, tint = colors.accent, modifier = Modifier.size(28.dp)) }
            androidx.compose.foundation.layout.Column {
                Text(text = title, color = colors.content.copy(alpha = 0.70f))
                Text(text = value, color = colors.content, fontWeight = FontWeight.Bold)
                subtitle?.let { Text(text = it, color = colors.content.copy(alpha = 0.58f)) }
            }
            Spacer(modifier = Modifier.weight(1f))
        }
    }
}
