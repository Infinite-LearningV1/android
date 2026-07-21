package com.example.infinite_track.presentation.screen.profile.details.about.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.infinite_track.domain.model.about.AboutTimelineItem
import com.example.infinite_track.presentation.design.tokens.InfiniteColors

@Composable
fun AboutTimelineSection(
    timeline: List<AboutTimelineItem>,
    modifier: Modifier = Modifier
) {
    GlassSectionCard(modifier = modifier) {
        AboutSectionTitle(
            title = "Development Timeline",
            icon = Icons.Outlined.Schedule,
            tint = InfiniteColors.AboutPurple
        )
        Column(verticalArrangement = Arrangement.spacedBy(0.dp)) {
            timeline.forEachIndexed { index, item ->
                TimelineEntry(
                    item = item,
                    isFirst = index == 0,
                    isLast = index == timeline.lastIndex
                )
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
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(14.dp),
        verticalAlignment = Alignment.Top
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Canvas(modifier = Modifier.size(width = 3.dp, height = if (isFirst) 6.dp else 10.dp)) {
                if (!isFirst) {
                    drawLine(
                        color = InfiniteColors.AboutPurpleSoft.copy(alpha = 0.28f),
                        start = Offset(size.width / 2, 0f),
                        end = Offset(size.width / 2, size.height),
                        strokeWidth = size.width
                    )
                }
            }
            AboutGlowDot(color = InfiniteColors.AboutPurple, size = 16.dp)
            Canvas(modifier = Modifier.size(width = 3.dp, height = if (isLast) 6.dp else 34.dp)) {
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
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(top = 2.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = "${item.period} — ${item.title}",
                style = MaterialTheme.typography.titleSmall,
                color = InfiniteColors.AccountHubTitle,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = item.description,
                style = MaterialTheme.typography.bodySmall,
                color = InfiniteColors.AccountHubBodyText
            )
        }
    }
}
