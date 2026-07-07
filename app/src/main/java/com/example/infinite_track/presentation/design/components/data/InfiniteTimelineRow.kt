package com.example.infinite_track.presentation.design.components.data

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.infinite_track.presentation.design.components.status.InfiniteStatusPill
import com.example.infinite_track.presentation.design.components.status.InfiniteStatusVariant
import com.example.infinite_track.presentation.design.tokens.InfiniteColors

enum class TimelineConnectorPosition {
    None,
    First,
    Middle,
    Last
}

@Composable
fun InfiniteTimelineRow(
    dateLabel: String,
    modeLabel: String,
    timeRange: String,
    statusLabel: String,
    statusVariant: InfiniteStatusVariant,
    modifier: Modifier = Modifier,
    leadingIcon: ImageVector? = Icons.Rounded.CalendarMonth,
    connectorPosition: TimelineConnectorPosition = TimelineConnectorPosition.None,
    onClick: (() -> Unit)? = null,
    supportingContent: (@Composable ColumnScope.() -> Unit)? = null,
    trailingContent: (@Composable RowScope.() -> Unit)? = null
) {
    Row(
        modifier = modifier
            .clickable(enabled = onClick != null) { onClick?.invoke() }
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        TimelineMarker(connectorPosition = connectorPosition, leadingIcon = leadingIcon)
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(text = dateLabel, fontWeight = FontWeight.SemiBold, color = InfiniteColors.Text)
            Text(text = modeLabel, color = InfiniteColors.Text.copy(alpha = 0.66f))
            Text(text = timeRange, color = InfiniteColors.Text.copy(alpha = 0.56f))
            supportingContent?.invoke(this)
        }
        InfiniteStatusPill(label = statusLabel, variant = statusVariant)
        trailingContent?.invoke(this)
    }
}

@Composable
private fun TimelineMarker(
    connectorPosition: TimelineConnectorPosition,
    leadingIcon: ImageVector?
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Canvas(modifier = Modifier.size(2.dp, 10.dp)) {
            if (connectorPosition == TimelineConnectorPosition.Middle || connectorPosition == TimelineConnectorPosition.Last) {
                drawLine(InfiniteColors.Primary.copy(alpha = 0.35f), Offset(size.width / 2, 0f), Offset(size.width / 2, size.height), strokeWidth = size.width)
            }
        }
        leadingIcon?.let { Icon(imageVector = it, contentDescription = null, tint = InfiniteColors.Primary, modifier = Modifier.size(22.dp)) }
        Canvas(modifier = Modifier.size(2.dp, 10.dp)) {
            if (connectorPosition == TimelineConnectorPosition.Middle || connectorPosition == TimelineConnectorPosition.First) {
                drawLine(InfiniteColors.Primary.copy(alpha = 0.35f), Offset(size.width / 2, 0f), Offset(size.width / 2, size.height), strokeWidth = size.width)
            }
        }
    }
}
