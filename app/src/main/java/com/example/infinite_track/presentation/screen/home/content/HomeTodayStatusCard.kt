package com.example.infinite_track.presentation.screen.home.content

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.infinite_track.domain.model.attendance.TodayStatus
import com.example.infinite_track.presentation.core.body1
import com.example.infinite_track.presentation.core.body2
import com.example.infinite_track.presentation.core.headline4
import com.example.infinite_track.presentation.design.components.data.InfiniteGlassReportCard
import com.example.infinite_track.presentation.design.components.status.InfiniteInlineAlert
import com.example.infinite_track.presentation.design.tokens.InfiniteColors
import com.example.infinite_track.presentation.design.tokens.InfiniteIcons
import com.example.infinite_track.presentation.design.tokens.InfiniteSemantic
import com.example.infinite_track.presentation.screen.home.HomeTodayStatusUiState
import com.example.infinite_track.presentation.theme.Purple_300
import com.example.infinite_track.presentation.theme.Purple_500
import com.example.infinite_track.utils.UiState
import java.time.OffsetDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
fun HomeTodayStatusCard(
    state: HomeTodayStatusUiState,
    currentLocation: String,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        state.warningMessage?.let { warning ->
            InfiniteInlineAlert(
                title = "Status belum terbaru",
                message = warning,
                semantic = InfiniteSemantic.Warning
            )
        }

        when (val statusState = state.status) {
            is UiState.Loading -> TodayStatusLoadingCard()
            is UiState.Success -> TodayStatusSuccessCard(
                todayStatus = statusState.data,
                currentLocation = currentLocation
            )
            is UiState.Error -> TodayStatusErrorCard(message = statusState.errorMessage)
            is UiState.Idle -> TodayStatusErrorCard(message = "Status hari ini belum tersedia.")
        }
    }
}

@Composable
private fun TodayStatusLoadingCard() {
    InfiniteGlassReportCard {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(132.dp),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator(color = InfiniteColors.Primary)
        }
    }
}

@Composable
private fun TodayStatusErrorCard(message: String) {
    InfiniteGlassReportCard {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            TodayStatusHeader()
            Text(
                text = "Status hari ini belum bisa dimuat",
                color = Purple_500,
                style = body1
            )
            Text(
                text = "$message. Tarik ke bawah untuk memuat ulang.",
                color = Purple_300,
                style = body2
            )
        }
    }
}

@Composable
private fun TodayStatusSuccessCard(
    todayStatus: TodayStatus,
    currentLocation: String
) {
    val statusKey = todayStatus.attendanceSessionState?.key.orEmpty().lowercase(Locale.ROOT)
    val metrics = listOf(
        TodayStatusMetric(
            label = "Status",
            value = todayStatus.displayStatus(statusKey),
            icon = InfiniteIcons.Person,
            accent = Purple_500
        ),
        TodayStatusMetric(
            label = "Mode",
            value = displayMode(todayStatus.activeMode),
            icon = InfiniteIcons.Work,
            accent = Purple_500
        ),
        TodayStatusMetric(
            label = "Location",
            value = todayStatus.activeLocation?.description ?: "--",
            icon = InfiniteIcons.Location,
            accent = Purple_500
        ),
        TodayStatusMetric(
            label = "Check-out",
            value = formatTime(todayStatus.checkedOutAt, todayStatus.checkedOutAtIso),
            icon = InfiniteIcons.Time,
            accent = Purple_500
        ),
        TodayStatusMetric(
            label = "Check-in",
            value = formatTime(todayStatus.checkedInAt, todayStatus.checkedInAtIso),
            icon = InfiniteIcons.Time,
            accent = Purple_500
        ),
        TodayStatusMetric(
            label = "Geofence",
            value = geofenceSummary(todayStatus, currentLocation),
            icon = InfiniteIcons.Shield,
            accent = InfiniteColors.Accent,
            badge = true
        ),
        TodayStatusMetric(
            label = "Work Duration",
            value = formatDuration(todayStatus.workDurationSeconds),
            icon = InfiniteIcons.Time,
            accent = Purple_500
        )
    )

    InfiniteGlassReportCard {
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            TodayStatusHeader()
            Column(modifier = Modifier.fillMaxWidth()) {
                metrics.chunked(2).forEachIndexed { rowIndex, rowItems ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(62.dp)
                    ) {
                        rowItems.forEachIndexed { itemIndex, metric ->
                            TodayStatusMetricCell(
                                metric = metric,
                                modifier = Modifier.weight(1f)
                            )
                            if (itemIndex == 0 && rowItems.size > 1) {
                                Box(
                                    modifier = Modifier
                                        .width(1.dp)
                                        .fillMaxHeight()
                                        .background(InfiniteColors.AttendanceReportGlassBorder)
                                )
                            }
                        }
                        if (rowItems.size == 1) {
                            Spacer(modifier = Modifier.weight(1f))
                        }
                    }
                    if (rowIndex < metrics.chunked(2).lastIndex) {
                        HorizontalDivider(color = InfiniteColors.AttendanceReportGlassBorder)
                    }
                }
            }
        }
    }
}

@Composable
private fun TodayStatusHeader() {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = InfiniteIcons.Calendar,
            contentDescription = null,
            tint = Purple_500,
            modifier = Modifier.size(22.dp)
        )
        Text(
            text = "Today Status",
            color = Purple_500,
            style = headline4
        )
    }
}

@Composable
private fun TodayStatusMetricCell(
    metric: TodayStatusMetric,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxHeight()
            .padding(horizontal = 8.dp, vertical = 6.dp),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(
                imageVector = metric.icon,
                contentDescription = null,
                tint = metric.accent,
                modifier = Modifier.size(16.dp)
            )
            Text(
                text = metric.label,
                style = body2,
                color = Purple_300,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        if (metric.badge) {
            GeofenceBadge(text = metric.value)
        } else {
            Text(
                text = metric.value,
                style = headline4,
                color = metric.accent,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun GeofenceBadge(text: String) {
    Box(
        modifier = Modifier
            .background(InfiniteColors.Accent.copy(alpha = 0.12f), CircleShape)
            .padding(horizontal = 10.dp, vertical = 3.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            color = InfiniteColors.Accent,
            style = body2,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

private data class TodayStatusMetric(
    val label: String,
    val value: String,
    val icon: ImageVector,
    val accent: Color = Purple_500,
    val badge: Boolean = false
)

private fun TodayStatus.displayStatus(statusKey: String): String {
    attendanceSessionState?.label?.takeIf { it.isNotBlank() }?.let { return it }
    return when (statusKey) {
        "not_started" -> "Belum check-in"
        "active" -> "Active Session"
        "completed" -> "Completed"
        "unavailable" -> "Unavailable"
        else -> statusKey.takeIf { it.isNotBlank() } ?: "Unknown"
    }
}

private fun displayMode(mode: String): String {
    return when {
        mode.equals("wfo", ignoreCase = true) -> "WFO"
        mode.equals("wfh", ignoreCase = true) -> "WFH"
        mode.contains("office", ignoreCase = true) -> "WFO"
        mode.contains("home", ignoreCase = true) -> "WFH"
        mode.isBlank() -> "--"
        else -> mode
    }
}

private fun geofenceSummary(todayStatus: TodayStatus, currentLocation: String): String {
    return when {
        todayStatus.activeLocation != null && currentLocation.isNotBlank() -> "Target area"
        todayStatus.activeLocation != null -> "Target set"
        else -> "--"
    }
}

private fun formatTime(primary: String?, iso: String?): String {
    primary?.takeIf { it.isNotBlank() }?.let { return it }
    return iso
        ?.takeIf { it.isNotBlank() }
        ?.let { value ->
            runCatching {
                OffsetDateTime.parse(value).format(DateTimeFormatter.ofPattern("HH:mm"))
            }.getOrElse { value }
        }
        ?: "--:--"
}

private fun formatDuration(seconds: Long?): String {
    if (seconds == null || seconds <= 0L) return "--"
    val hours = seconds / 3600
    val minutes = (seconds % 3600) / 60
    return when {
        hours > 0 && minutes > 0 -> "%02dh %02dm".format(hours, minutes)
        hours > 0 -> "%02dh".format(hours)
        minutes > 0 -> "%02dm".format(minutes)
        else -> "<1m"
    }
}
