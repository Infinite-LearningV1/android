package com.example.infinite_track.presentation.screen.history

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.infinite_track.data.mapper.attendance.toReportDateLabel
import com.example.infinite_track.data.mapper.attendance.toReportStatus
import com.example.infinite_track.data.mapper.attendance.toReportTimeRangeLabel
import com.example.infinite_track.data.mapper.attendance.toReportWorkHourLabel
import com.example.infinite_track.domain.model.attendance.AttendanceRecord
import com.example.infinite_track.presentation.design.components.data.AttendanceHistoryTimelinePill
import com.example.infinite_track.presentation.design.components.data.connectorPositionFor
import com.example.infinite_track.presentation.design.components.data.toTimelineVariant
import com.example.infinite_track.presentation.theme.attendanceModeColor

internal fun LazyListScope.attendanceHistoryTimelineItems(
    records: List<AttendanceRecord>,
    focusByKey: Map<String, Float>,
    timelineProgress: Float,
    motionEnabled: Boolean
) {
    itemsIndexed(
        items = records,
        key = { _, record -> historyItemKey(record.id) },
        contentType = { _, _ -> "history-record" }
    ) { index, record ->
        val status = record.toReportStatus()
        val key = historyItemKey(record.id)
        AttendanceHistoryTimelinePill(
            nodeLabel = record.date,
            dateLabel = record.toReportDateLabel(),
            timeRange = record.toReportTimeRangeLabel(),
            supportingText = record.location?.takeIf(String::isNotBlank)
                ?: record.toReportWorkHourLabel(),
            statusLabel = status.label,
            statusVariant = status.kind.toTimelineVariant(),
            connectorPosition = connectorPositionFor(index, records.size),
            connectorAccent = resolveHistoryConnectorAccent(index, records.size, timelineProgress),
            focusFraction = focusByKey[key] ?: 0f,
            motionEnabled = motionEnabled,
            modeAccentColor = attendanceModeColor(record.modeKey ?: record.modeLabel),
            modifier = Modifier.padding(vertical = 2.dp)
        )
    }
}
