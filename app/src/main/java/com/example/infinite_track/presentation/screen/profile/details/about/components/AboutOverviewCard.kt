package com.example.infinite_track.presentation.screen.profile.details.about.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.BarChart
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material3.Icon
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
            icon = Icons.Outlined.Description,
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
                val icon = when {
                    chip.label.contains("Attendance", ignoreCase = true) -> Icons.Outlined.CalendarMonth
                    chip.label.contains("Location", ignoreCase = true) -> Icons.Outlined.LocationOn
                    chip.label.contains("Fuzzy", ignoreCase = true) ||
                        chip.label.contains("AHP", ignoreCase = true) -> Icons.Outlined.BarChart
                    index % 3 == 0 -> Icons.Outlined.CalendarMonth
                    index % 3 == 1 -> Icons.Outlined.LocationOn
                    else -> Icons.Outlined.BarChart
                }
                val tint = when {
                    chip.label.contains("Location", ignoreCase = true) -> InfiniteColors.AboutCyan
                    chip.label.contains("Fuzzy", ignoreCase = true) ||
                        chip.label.contains("AHP", ignoreCase = true) -> InfiniteColors.AboutPurpleSoft
                    else -> InfiniteColors.AboutPurple
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
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = tint,
            modifier = Modifier.size(20.dp)
        )
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            color = InfiniteColors.AccountHubTitle,
            fontWeight = FontWeight.SemiBold
        )
    }
}
