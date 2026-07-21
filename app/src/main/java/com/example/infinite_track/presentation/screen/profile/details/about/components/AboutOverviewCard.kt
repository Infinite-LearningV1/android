package com.example.infinite_track.presentation.screen.profile.details.about.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Article
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.LocationOn
import androidx.compose.material.icons.rounded.QueryStats
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.infinite_track.domain.model.about.AboutOverview
import com.example.infinite_track.presentation.design.tokens.InfiniteColors

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AboutOverviewCard(
    overview: AboutOverview,
    modifier: Modifier = Modifier
) {
    GlassSectionCard(modifier = modifier) {
        AboutSectionTitle(
            title = overview.title,
            icon = Icons.AutoMirrored.Rounded.Article,
            tint = InfiniteColors.AboutPurple
        )
        Text(
            text = overview.description,
            style = MaterialTheme.typography.bodyMedium,
            color = InfiniteColors.AccountHubBodyText
        )
        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            overview.featureChips.forEachIndexed { index, chip ->
                val icon = when (index % 3) {
                    0 -> Icons.Rounded.CalendarMonth
                    1 -> Icons.Rounded.LocationOn
                    else -> Icons.Rounded.QueryStats
                }
                val tint = when (index % 3) {
                    0 -> InfiniteColors.AboutPurple
                    1 -> InfiniteColors.AboutCyan
                    else -> InfiniteColors.AboutPurpleSoft
                }
                AboutPillChip(
                    label = chip.label,
                    icon = icon,
                    tint = tint
                )
            }
        }
    }
}

@Composable
fun AboutSectionTitle(
    title: String,
    icon: ImageVector,
    tint: Color,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        AboutSoftIconBadge(
            icon = icon,
            tint = tint,
            size = 34.dp,
            iconSize = 18.dp
        )
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            color = InfiniteColors.AccountHubTitle,
            fontWeight = FontWeight.SemiBold
        )
    }
}
