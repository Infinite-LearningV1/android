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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.infinite_track.domain.model.attendance.WorkMode
import com.example.infinite_track.presentation.design.tokens.InfiniteColors
import com.example.infinite_track.presentation.design.tokens.InfiniteIcons
import com.example.infinite_track.presentation.design.tokens.InfiniteSpacing
import com.example.infinite_track.presentation.design.tokens.WorkModeVisualTokens
import com.example.infinite_track.presentation.map.model.MapMarkerCategory

@Composable
internal fun CompactMapCallout(
    title: String,
    category: MapMarkerCategory,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.testTag("compactMapCallout"),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(
            modifier = Modifier
                .widthIn(max = 220.dp)
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
