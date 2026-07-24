package com.example.infinite_track.presentation.components.button.attendance

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.infinite_track.R
import com.example.infinite_track.presentation.design.components.status.InfiniteStatusPill
import com.example.infinite_track.presentation.design.components.status.InfiniteStatusVariant
import com.example.infinite_track.presentation.design.components.surface.InfiniteCard
import com.example.infinite_track.presentation.design.tokens.InfiniteColors
import com.example.infinite_track.presentation.design.tokens.InfiniteIcons
import com.example.infinite_track.presentation.design.tokens.InfiniteSemantic
import com.example.infinite_track.presentation.design.tokens.InfiniteSize
import com.example.infinite_track.presentation.design.tokens.InfiniteSpacing
import com.example.infinite_track.presentation.design.tokens.InfiniteSurfaceVariant
import com.example.infinite_track.presentation.design.tokens.infiniteSemanticColors
import com.example.infinite_track.presentation.screen.attendance.preparation.TargetLocationSummaryUiModel
import com.example.infinite_track.presentation.theme.Infinite_TrackTheme

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun TargetLocationSummary(
    model: TargetLocationSummaryUiModel,
    modifier: Modifier = Modifier
) {
    val colors = infiniteSemanticColors(InfiniteSemantic.Primary)
    val heading = stringResource(R.string.attendance_target_heading)
    val semanticSummary = buildList {
        add("$heading ${model.displayName}")
        add(model.sourceLabel)
        add(model.radiusText)
        model.distanceText?.let(::add)
        model.rangeText?.let(::add)
        model.bookingStatusText?.let(::add)
        model.bookingDateText?.let(::add)
    }.joinToString(separator = ", ")

    InfiniteCard(
        modifier = modifier
            .fillMaxWidth()
            .semantics(mergeDescendants = true) {
                contentDescription = semanticSummary
            },
        variant = InfiniteSurfaceVariant.Default,
        semantic = InfiniteSemantic.Primary,
        showShadow = false
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(InfiniteSpacing.Default.md),
            verticalAlignment = Alignment.Top
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(MaterialTheme.shapes.medium)
                    .background(colors.container),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = InfiniteIcons.Location,
                    contentDescription = null,
                    modifier = Modifier.size(InfiniteSpacing.Default.xl),
                    tint = colors.accent
                )
            }

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(InfiniteSpacing.Default.xs)
            ) {
                Text(
                    text = heading,
                    style = MaterialTheme.typography.labelMedium,
                    color = InfiniteColors.AttendanceReportMutedText
                )
                Text(
                    text = model.displayName,
                    style = MaterialTheme.typography.titleMedium,
                    color = InfiniteColors.Text
                )
                Row(
                    horizontalArrangement = Arrangement.spacedBy(InfiniteSpacing.Default.xs),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = InfiniteIcons.Shield,
                        contentDescription = null,
                        modifier = Modifier.size(InfiniteSpacing.Default.lg),
                        tint = colors.accent
                    )
                    Text(
                        text = model.sourceLabel,
                        modifier = Modifier.weight(1f),
                        style = MaterialTheme.typography.bodySmall,
                        color = InfiniteColors.AttendanceReportBodyText
                    )
                }
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(InfiniteSpacing.Default.xs),
                    verticalArrangement = Arrangement.spacedBy(InfiniteSpacing.Default.xs)
                ) {
                    InfiniteStatusPill(
                        label = model.radiusText,
                        variant = InfiniteStatusVariant.Neutral,
                        size = InfiniteSize.Small,
                        leadingIcon = InfiniteIcons.Location
                    )
                    model.distanceText?.let { distance ->
                        InfiniteStatusPill(
                            label = distance,
                            variant = InfiniteStatusVariant.Neutral,
                            size = InfiniteSize.Small,
                            leadingIcon = InfiniteIcons.Location
                        )
                    }
                    model.rangeText?.let { range ->
                        InfiniteStatusPill(
                            label = range,
                            variant = when (model.rangeSemantic) {
                                InfiniteSemantic.Success -> InfiniteStatusVariant.Inside
                                InfiniteSemantic.Error -> InfiniteStatusVariant.Outside
                                else -> InfiniteStatusVariant.Unknown
                            },
                            size = InfiniteSize.Small,
                            leadingIcon = when (model.rangeSemantic) {
                                InfiniteSemantic.Success -> InfiniteIcons.Success
                                InfiniteSemantic.Error -> InfiniteIcons.Error
                                else -> InfiniteIcons.Info
                            }
                        )
                    }
                    model.bookingStatusText?.let { status ->
                        InfiniteStatusPill(
                            label = status,
                            variant = InfiniteStatusVariant.Approved,
                            size = InfiniteSize.Small,
                            leadingIcon = InfiniteIcons.Success
                        )
                    }
                    model.bookingDateText?.let { date ->
                        InfiniteStatusPill(
                            label = date,
                            variant = InfiniteStatusVariant.Neutral,
                            size = InfiniteSize.Small,
                            leadingIcon = InfiniteIcons.Calendar
                        )
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true, widthDp = 320)
@Composable
private fun TargetLocationSummaryPreview() {
    Infinite_TrackTheme {
        TargetLocationSummary(
            model = TargetLocationSummaryUiModel(
                displayName = "Infinite Track Office Palu",
                sourceLabel = "Today's attendance status",
                radiusText = "Radius 100 m",
                distanceText = "Distance 25 m",
                rangeText = "Inside range"
            )
        )
    }
}
