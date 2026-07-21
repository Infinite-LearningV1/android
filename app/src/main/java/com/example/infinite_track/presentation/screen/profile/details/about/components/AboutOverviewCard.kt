package com.example.infinite_track.presentation.screen.profile.details.about.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Article
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.LocationOn
import androidx.compose.material.icons.rounded.QueryStats
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.example.infinite_track.domain.model.about.AboutFeatureChip
import com.example.infinite_track.domain.model.about.AboutOverview
import com.example.infinite_track.presentation.design.components.data.InfiniteSectionHeader
import com.example.infinite_track.presentation.design.tokens.InfiniteColors

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AboutOverviewCard(
    overview: AboutOverview,
    modifier: Modifier = Modifier
) {
    GlassSectionCard(modifier = modifier) {
        InfiniteSectionHeader(
            title = overview.title,
            subtitle = "Project purpose and scope",
            leadingIcon = Icons.AutoMirrored.Rounded.Article
        )
        Text(
            text = overview.description,
            style = MaterialTheme.typography.bodyMedium,
            color = InfiniteColors.AccountHubBodyText
        )
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            overview.featureChips.forEachIndexed { index, chip ->
                OverviewFeatureChip(
                    chip = chip,
                    icon = when (index % 3) {
                        0 -> Icons.Rounded.CalendarMonth
                        1 -> Icons.Rounded.LocationOn
                        else -> Icons.Rounded.QueryStats
                    }
                )
            }
        }
    }
}

@Composable
private fun OverviewFeatureChip(
    chip: AboutFeatureChip,
    icon: ImageVector
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = InfiniteColors.Surface.copy(alpha = 0.64f),
        border = BorderStroke(1.dp, InfiniteColors.Surface.copy(alpha = 0.88f))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (chip.label.contains("Location")) InfiniteColors.AboutCyan else InfiniteColors.Primary,
                modifier = Modifier.size(18.dp)
            )
            Text(
                text = chip.label,
                style = MaterialTheme.typography.labelLarge,
                color = InfiniteColors.AccountHubTitle
            )
        }
    }
}
