package com.example.infinite_track.presentation.components.button.attendance

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
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
import com.example.infinite_track.presentation.design.tokens.InfiniteIcons
import com.example.infinite_track.presentation.design.tokens.InfiniteSemantic
import com.example.infinite_track.presentation.design.tokens.InfiniteSize
import com.example.infinite_track.presentation.design.tokens.InfiniteSpacing
import com.example.infinite_track.presentation.design.tokens.InfiniteSurfaceVariant
import com.example.infinite_track.presentation.screen.attendance.preparation.TargetLocationSummaryUiModel
import com.example.infinite_track.presentation.theme.Infinite_TrackTheme
import com.example.infinite_track.presentation.theme.White

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun TargetLocationSummary(
    model: TargetLocationSummaryUiModel,
    modifier: Modifier = Modifier
) {
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
        variant = InfiniteSurfaceVariant.PrimarySolid,
        semantic = InfiniteSemantic.Primary,
        showBorder = false,
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
                    .background(White.copy(alpha = 0.14f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = InfiniteIcons.Location,
                    contentDescription = null,
                    modifier = Modifier.size(InfiniteSpacing.Default.xl),
                    tint = White
                )
            }

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(InfiniteSpacing.Default.xs)
            ) {
                Text(
                    text = heading,
                    style = MaterialTheme.typography.labelMedium,
                    color = White.copy(alpha = 0.72f)
                )
                Text(
                    text = model.displayName,
                    style = MaterialTheme.typography.titleMedium,
                    color = White
                )
                Row(
                    horizontalArrangement = Arrangement.spacedBy(InfiniteSpacing.Default.xs),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = InfiniteIcons.Shield,
                        contentDescription = null,
                        modifier = Modifier.size(InfiniteSpacing.Default.lg),
                        tint = White
                    )
                    Text(
                        text = model.sourceLabel,
                        modifier = Modifier.weight(1f),
                        style = MaterialTheme.typography.bodySmall,
                        color = White.copy(alpha = 0.82f)
                    )
                }
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(InfiniteSpacing.Default.xs),
                    verticalArrangement = Arrangement.spacedBy(InfiniteSpacing.Default.xs)
                ) {
                    TargetLocationMetadataPill(
                        label = model.radiusText,
                        leadingIcon = InfiniteIcons.Location
                    )
                    model.distanceText?.let { distance ->
                        TargetLocationMetadataPill(
                            label = distance,
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
                        TargetLocationMetadataPill(
                            label = date,
                            leadingIcon = InfiniteIcons.Calendar
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun TargetLocationMetadataPill(
    label: String,
    leadingIcon: androidx.compose.ui.graphics.vector.ImageVector,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.semantics { contentDescription = label },
        shape = MaterialTheme.shapes.extraLarge,
        color = White.copy(alpha = 0.14f),
        contentColor = White,
        border = BorderStroke(1.dp, White.copy(alpha = 0.24f))
    ) {
        Row(
            modifier = Modifier
                .background(White.copy(alpha = 0.02f))
                .sizeIn(minHeight = 24.dp)
                .padding(horizontal = 7.dp, vertical = 3.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = leadingIcon,
                contentDescription = null,
                modifier = Modifier.size(12.dp),
                tint = White
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = White
            )
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
