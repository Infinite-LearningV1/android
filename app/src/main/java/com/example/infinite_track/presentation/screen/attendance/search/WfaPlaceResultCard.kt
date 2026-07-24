package com.example.infinite_track.presentation.screen.attendance.search

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.selectable
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.testTag
import com.example.infinite_track.domain.model.location.PlaceSuggestion
import com.example.infinite_track.presentation.design.components.surface.InfiniteCard
import com.example.infinite_track.presentation.design.tokens.InfiniteColors
import com.example.infinite_track.presentation.design.tokens.InfiniteDensity
import com.example.infinite_track.presentation.design.tokens.InfiniteIcons
import com.example.infinite_track.presentation.design.tokens.InfiniteSemantic
import com.example.infinite_track.presentation.design.tokens.InfiniteSpacing
import com.example.infinite_track.presentation.design.tokens.InfiniteSurfaceVariant
import java.util.Locale

@Composable
internal fun WfaPlaceResultCard(
    suggestion: PlaceSuggestion,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    InfiniteCard(
        modifier = modifier
            .fillMaxWidth()
            .selectable(selected = selected, onClick = onClick)
            .semantics { this.selected = selected }
            .testTag("wfaPlace:${suggestion.placeId}"),
        variant = InfiniteSurfaceVariant.Outlined,
        semantic = if (selected) InfiniteSemantic.Secondary else InfiniteSemantic.Neutral,
        density = InfiniteDensity.Compact,
        selected = selected,
        showShadow = false
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(InfiniteSpacing.Default.md)
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(MaterialTheme.shapes.medium)
                    .background(InfiniteColors.Secondary.copy(alpha = 0.14f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = InfiniteIcons.Location,
                    contentDescription = null,
                    tint = InfiniteColors.Secondary,
                    modifier = Modifier.size(22.dp)
                )
            }

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(InfiniteSpacing.Default.xs)
            ) {
                Text(
                    text = suggestion.primaryText,
                    style = MaterialTheme.typography.titleSmall,
                    color = InfiniteColors.Text,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                suggestion.secondaryText?.takeIf(String::isNotBlank)?.let { address ->
                    Text(
                        text = address,
                        style = MaterialTheme.typography.bodySmall,
                        color = InfiniteColors.AttendanceReportBodyText,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                suggestion.distance?.let { distance ->
                    Text(
                        text = formatDistanceMeters(distance.value),
                        style = MaterialTheme.typography.labelSmall,
                        color = InfiniteColors.Secondary
                    )
                }
            }

            if (selected) {
                Icon(
                    imageVector = InfiniteIcons.Success,
                    contentDescription = null,
                    tint = InfiniteColors.Secondary
                )
            }
        }
    }
}

internal fun formatDistanceMeters(value: Double): String =
    if (value < 1_000.0) {
        "${value.toInt()} m"
    } else {
        String.format(Locale("id", "ID"), "%.1f km", value / 1_000.0)
    }
