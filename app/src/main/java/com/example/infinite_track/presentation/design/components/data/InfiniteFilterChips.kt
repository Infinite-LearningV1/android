package com.example.infinite_track.presentation.design.components.data

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.infinite_track.presentation.design.tokens.InfiniteColors
import com.example.infinite_track.presentation.design.tokens.InfiniteSize

data class InfiniteFilterOption(
    val key: String,
    val label: String,
    val icon: ImageVector? = null
)

@Composable
fun InfiniteFilterChips(
    options: List<InfiniteFilterOption>,
    selectedKey: String,
    onSelected: (String) -> Unit,
    modifier: Modifier = Modifier,
    size: InfiniteSize = InfiniteSize.Medium
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        options.forEach { option ->
            val selected = option.key == selectedKey
            FilterChip(
                selected = selected,
                onClick = { onSelected(option.key) },
                label = {
                    Text(
                        text = option.label,
                        fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium
                    )
                },
                leadingIcon = option.icon?.let { icon ->
                    { Icon(imageVector = icon, contentDescription = null) }
                },
                shape = RoundedCornerShape(18.dp),
                border = BorderStroke(
                    width = 1.dp,
                    color = if (selected) Color.Transparent else InfiniteColors.Surface.copy(alpha = 0.92f)
                ),
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = InfiniteColors.Primary,
                    selectedLabelColor = InfiniteColors.Surface,
                    selectedLeadingIconColor = InfiniteColors.Surface,
                    containerColor = InfiniteColors.Surface.copy(alpha = 0.68f),
                    labelColor = InfiniteColors.Text,
                    iconColor = InfiniteColors.Primary
                )
            )
        }
    }
}
