package com.example.infinite_track.presentation.design.components.data

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.infinite_track.presentation.core.body1
import com.example.infinite_track.presentation.core.body2
import com.example.infinite_track.presentation.design.tokens.InfiniteSemantic
import com.example.infinite_track.presentation.design.tokens.infiniteSemanticColors

enum class InfiniteInfoRowOrientation {
    Horizontal,
    Vertical
}

@Composable
fun InfiniteInfoRow(
    label: String?,
    value: String,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    semantic: InfiniteSemantic = InfiniteSemantic.Neutral,
    orientation: InfiniteInfoRowOrientation = InfiniteInfoRowOrientation.Horizontal,
    statusContent: (@Composable (() -> Unit))? = null
) {
    val colors = infiniteSemanticColors(semantic)
    val leading: @Composable RowScope.() -> Unit = {
        icon?.let { Icon(imageVector = it, contentDescription = null, tint = colors.accent, modifier = Modifier.size(18.dp)) }
    }
    if (orientation == InfiniteInfoRowOrientation.Vertical) {
        Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(4.dp)) {
            if (!label.isNullOrBlank() || icon != null) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    leading()
                    if (!label.isNullOrBlank()) {
                        Text(text = label, style = body2, color = colors.content.copy(alpha = 0.66f))
                    }
                }
            }
            Text(text = value, style = body1, fontWeight = FontWeight.SemiBold, color = colors.content)
            statusContent?.invoke()
        }
    } else {
        Row(
            modifier = modifier,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            leading()
            if (!label.isNullOrBlank()) {
                Text(text = label, style = body2, color = colors.content.copy(alpha = 0.66f), modifier = Modifier.weight(1f))
            }
            statusContent?.invoke() ?: Text(text = value, style = body1, fontWeight = FontWeight.SemiBold, color = colors.content)
        }
    }
}
