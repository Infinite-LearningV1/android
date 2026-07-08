package com.example.infinite_track.presentation.screen.home.content

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.infinite_track.domain.model.attendance.TodayStatus
import com.example.infinite_track.presentation.design.components.status.InfiniteInlineAlert
import com.example.infinite_track.presentation.design.components.status.InfiniteStatusVariant
import com.example.infinite_track.presentation.design.tokens.InfiniteColors
import com.example.infinite_track.presentation.design.tokens.InfiniteIcons
import com.example.infinite_track.presentation.design.tokens.InfiniteSemantic
import com.example.infinite_track.presentation.screen.home.HomeTodayStatusUiState
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
    TodayStatusSurface {
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
    TodayStatusSurface {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            TodayStatusHeader()
            Text(
                text = "Status hari ini belum bisa dimuat",
                color = InfiniteColors.Text,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = "$message. Tarik ke bawah untuk memuat ulang.",
                color = InfiniteColors.Text.copy(alpha = 0.68f)
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
            valueColor = InfiniteColors.Primary
        ),
        TodayStatusMetric(
            label = "Mode",
            value = displayMode(todayStatus.activeMode),
            icon = InfiniteIcons.Work
        ),
        TodayStatusMetric(
            label = "Location",
            value = todayStatus.activeLocation?.description ?: "--",
            icon = InfiniteIcons.Location
        ),
        TodayStatusMetric(
            label = "Check-out",
            value = formatTime(todayStatus.checkedOutAt, todayStatus.checkedOutAtIso),
            icon = InfiniteIcons.Time
        ),
        TodayStatusMetric(
            label = "Check-in",
            value = formatTime(todayStatus.checkedInAt, todayStatus.checkedInAtIso),
            icon = InfiniteIcons.Time
        ),
        TodayStatusMetric(
            label = "Geofence",
            value = geofenceSummary(todayStatus, currentLocation),
            icon = InfiniteIcons.Shield,
            badge = true
        ),
        TodayStatusMetric(
            label = "Work Duration",
            value = formatDuration(todayStatus.workDurationSeconds),
            icon = InfiniteIcons.Time
        )
    )

    TodayStatusSurface {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            TodayStatusHeader()
            Column(verticalArrangement = Arrangement.spacedBy(0.dp)) {
                metrics.chunked(2).forEachIndexed { rowIndex, rowItems ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        rowItems.forEach { metric ->
                            TodayStatusMetricItem(
                                metric = metric,
                                modifier = Modifier.weight(1f)
                            )
                        }
                        if (rowItems.size == 1) {
                            Spacer(modifier = Modifier.weight(1f))
                        }
                    }
                    if (rowIndex < metrics.chunked(2).lastIndex) {
                        Spacer(modifier = Modifier.height(8.dp))
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
            tint = InfiniteColors.Primary,
            modifier = Modifier.size(22.dp)
        )
        Text(
            text = "Today Status",
            color = InfiniteColors.Text,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun TodayStatusMetricItem(
    metric: TodayStatusMetric,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .background(InfiniteColors.Surface.copy(alpha = 0.36f), RoundedCornerShape(14.dp))
            .padding(horizontal = 10.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = metric.icon,
            contentDescription = null,
            tint = InfiniteColors.Text.copy(alpha = 0.86f),
            modifier = Modifier.size(22.dp)
        )
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(1.dp)
        ) {
            Text(
                text = metric.label,
                color = InfiniteColors.Text.copy(alpha = 0.52f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            if (metric.badge) {
                GeofenceBadge(text = metric.value)
            } else {
                Text(
                    text = metric.value,
                    color = metric.valueColor,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
private fun GeofenceBadge(text: String) {
    Box(
        modifier = Modifier
            .background(InfiniteColors.Accent.copy(alpha = 0.62f), RoundedCornerShape(999.dp))
            .padding(horizontal = 12.dp, vertical = 5.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            color = InfiniteColors.Text,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun TodayStatusSurface(content: @Composable () -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        color = InfiniteColors.Surface.copy(alpha = 0.72f),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.84f)),
        shadowElevation = 8.dp
    ) {
        Box(
            modifier = Modifier
                .background(Color.White.copy(alpha = 0.16f))
                .padding(14.dp)
        ) {
            content()
        }
    }
}

private data class TodayStatusMetric(
    val label: String,
    val value: String,
    val icon: ImageVector,
    val valueColor: Color = InfiniteColors.Text,
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
