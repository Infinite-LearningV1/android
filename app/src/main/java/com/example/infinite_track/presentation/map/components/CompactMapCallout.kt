package com.example.infinite_track.presentation.map.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.outlined.MyLocation
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.infinite_track.domain.model.attendance.WorkMode
import com.example.infinite_track.R
import com.example.infinite_track.presentation.design.tokens.InfiniteColors
import com.example.infinite_track.presentation.design.tokens.InfiniteIcons
import com.example.infinite_track.presentation.design.tokens.InfiniteSpacing
import com.example.infinite_track.presentation.design.tokens.WorkModeVisualTokens
import com.example.infinite_track.presentation.map.model.MapMarkerCategory
import com.example.infinite_track.presentation.map.model.MapMarkerRecommendationInfo
import com.example.infinite_track.presentation.format.formatDistanceMeters
import kotlin.math.roundToInt

@Composable
internal fun CompactMapCallout(
    title: String,
    category: MapMarkerCategory,
    recommendationInfo: MapMarkerRecommendationInfo? = null,
    modifier: Modifier = Modifier
) {
    val activeLocale = LocalConfiguration.current.locales[0]
    Column(
        modifier = modifier.testTag("compactMapCallout"),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Column(
            modifier = Modifier
                .widthIn(min = 184.dp, max = 260.dp)
                .clip(MaterialTheme.shapes.large)
                .background(InfiniteColors.AttendanceReportGlassSurface)
                .border(
                    width = 1.dp,
                    color = InfiniteColors.AttendanceReportGlassBorder,
                    shape = MaterialTheme.shapes.large
                )
                .padding(
                    horizontal = InfiniteSpacing.Default.md,
                    vertical = InfiniteSpacing.Default.sm
                ),
            verticalArrangement = Arrangement.spacedBy(InfiniteSpacing.Default.sm)
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(InfiniteSpacing.Default.sm),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = category.icon(),
                    contentDescription = null,
                    tint = category.color(),
                    modifier = Modifier.size(InfiniteSpacing.Default.xl)
                )
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    color = InfiniteColors.Text,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
            recommendationInfo?.let { info ->
                CalloutMetadataRow(
                    label = stringResource(R.string.attendance_map_callout_category),
                    value = info.category
                )
                CalloutMetadataRow(
                    label = stringResource(R.string.attendance_map_callout_distance),
                    value = formatDistanceMeters(info.distance.value, activeLocale)
                )
                CalloutMetadataRow(
                    label = stringResource(R.string.attendance_map_callout_rating),
                    value = stringResource(
                        R.string.attendance_map_callout_rating_value,
                        formatFuzzyAhpRatingPercent(info.fuzzyAhpScore),
                        info.suitabilityLabel
                    ),
                    leadingIcon = Icons.Default.Star
                )
            }
        }
        Box(
            modifier = Modifier
                .offset(y = (-5).dp)
                .size(InfiniteSpacing.Default.md)
                .rotate(45f)
                .background(InfiniteColors.AttendanceReportGlassSurface)
                .border(
                    width = 1.dp,
                    color = InfiniteColors.AttendanceReportGlassBorder
                )
        )
    }
}

@Composable
private fun CalloutMetadataRow(
    label: String,
    value: String,
    leadingIcon: ImageVector? = null
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(InfiniteSpacing.Default.xs),
        verticalAlignment = Alignment.CenterVertically
    ) {
        leadingIcon?.let {
            Icon(
                imageVector = it,
                contentDescription = null,
                tint = InfiniteColors.Warning,
                modifier = Modifier.size(14.dp)
            )
        }
        Text(
            text = "$label:",
            style = MaterialTheme.typography.labelSmall,
            color = InfiniteColors.AttendanceReportMutedText
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodySmall,
            color = InfiniteColors.AttendanceReportBodyText,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

internal fun formatFuzzyAhpRatingPercent(score: Double): Int {
    val normalized = if (score.isFinite()) score.coerceIn(0.0, 1.0) else 0.0
    return (normalized * 100).roundToInt()
}

internal fun MapMarkerCategory.color(): Color = when (this) {
    MapMarkerCategory.CURRENT_LOCATION -> InfiniteColors.Primary
    MapMarkerCategory.WFO -> WorkModeVisualTokens.color(WorkMode.WFO)
    MapMarkerCategory.WFH -> WorkModeVisualTokens.color(WorkMode.WFH)
    MapMarkerCategory.WFA -> WorkModeVisualTokens.color(WorkMode.WFA)
}

internal fun MapMarkerCategory.icon(): ImageVector = when (this) {
    MapMarkerCategory.CURRENT_LOCATION -> Icons.Outlined.MyLocation
    MapMarkerCategory.WFO -> InfiniteIcons.Work
    MapMarkerCategory.WFH -> Icons.Default.Home
    MapMarkerCategory.WFA -> InfiniteIcons.Location
}
