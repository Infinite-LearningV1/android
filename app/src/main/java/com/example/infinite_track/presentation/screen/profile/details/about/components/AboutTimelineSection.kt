package com.example.infinite_track.presentation.screen.profile.details.about.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.unit.dp
import com.example.infinite_track.domain.model.about.AboutTimelineItem
import com.example.infinite_track.presentation.design.components.data.InfiniteSectionHeader
import com.example.infinite_track.presentation.design.tokens.InfiniteColors

@Composable
fun AboutTimelineSection(
    timeline: List<AboutTimelineItem>,
    modifier: Modifier = Modifier
) {
    GlassSectionCard(modifier = modifier) {
        InfiniteSectionHeader(
            title = "Development Timeline",
            subtitle = "From discovery to refinement",
            leadingIcon = Icons.Rounded.Schedule
        )
        Column(verticalArrangement = Arrangement.spacedBy(0.dp)) {
            timeline.forEachIndexed { index, item ->
                TimelineEntry(
                    item = item,
                    isFirst = index == 0,
                    isLast = index == timeline.lastIndex
                )
                if (index != timeline.lastIndex) {
                    HorizontalDivider(
                        modifier = Modifier.padding(start = 50.dp),
                        color = InfiniteColors.AccountHubDividerColor
                    )
                }
            }
        }
    }
}

@Composable
private fun TimelineEntry(
    item: AboutTimelineItem,
    isFirst: Boolean,
    isLast: Boolean
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 10.dp),
        horizontalArrangement = Arrangement.spacedBy(14.dp),
        verticalAlignment = Alignment.Top
    ) {
        TimelineMarker(isFirst = isFirst, isLast = isLast)
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = item.period,
                style = MaterialTheme.typography.labelLarge,
                color = InfiniteColors.Primary
            )
            Text(
                text = item.title,
                style = MaterialTheme.typography.titleSmall,
                color = InfiniteColors.AccountHubTitle
            )
            Text(
                text = item.description,
                style = MaterialTheme.typography.bodySmall,
                color = InfiniteColors.AccountHubBodyText
            )
        }
    }
}

@Composable
private fun TimelineMarker(
    isFirst: Boolean,
    isLast: Boolean
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Canvas(modifier = Modifier.size(width = 4.dp, height = 10.dp)) {
            if (!isFirst) {
                drawLine(
                    color = InfiniteColors.AboutPurpleSoft.copy(alpha = 0.28f),
                    start = Offset(size.width / 2, 0f),
                    end = Offset(size.width / 2, size.height),
                    strokeWidth = size.width
                )
            }
        }
        Box(
            modifier = Modifier
                .size(20.dp)
                .background(InfiniteColors.AboutPurpleSoft.copy(alpha = 0.16f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .size(10.dp)
                    .background(InfiniteColors.Primary, CircleShape)
            )
        }
        Canvas(modifier = Modifier.size(width = 4.dp, height = if (isLast) 4.dp else 24.dp)) {
            if (!isLast) {
                drawLine(
                    color = InfiniteColors.AboutPurpleSoft.copy(alpha = 0.28f),
                    start = Offset(size.width / 2, 0f),
                    end = Offset(size.width / 2, size.height),
                    strokeWidth = size.width
                )
            }
        }
    }
}
