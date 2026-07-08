package com.example.infinite_track.presentation.screen.home.content

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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.infinite_track.domain.model.attendance.TodayStatus
import com.example.infinite_track.presentation.components.loading.InlineRefreshingIndicator
import com.example.infinite_track.presentation.design.components.button.InfiniteButton
import com.example.infinite_track.presentation.design.components.button.InfiniteButtonState
import com.example.infinite_track.presentation.design.components.button.InfiniteButtonVariant
import com.example.infinite_track.presentation.design.components.data.InfiniteSectionHeader
import com.example.infinite_track.presentation.design.components.status.InfiniteInlineAlert
import com.example.infinite_track.presentation.design.components.status.InfiniteStatusPill
import com.example.infinite_track.presentation.design.components.status.InfiniteStatusVariant
import com.example.infinite_track.presentation.design.components.surface.InfiniteCard
import com.example.infinite_track.presentation.design.tokens.InfiniteColors
import com.example.infinite_track.presentation.design.tokens.InfiniteDensity
import com.example.infinite_track.presentation.design.tokens.InfiniteIcons
import com.example.infinite_track.presentation.design.tokens.InfiniteSemantic
import com.example.infinite_track.presentation.design.tokens.InfiniteSize
import com.example.infinite_track.presentation.design.tokens.InfiniteSurfaceVariant
import com.example.infinite_track.presentation.screen.home.HomeTodayStatusUiState
import com.example.infinite_track.utils.UiState
import java.time.OffsetDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
fun HomeTodayStatusCard(
    state: HomeTodayStatusUiState,
    currentLocation: String,
    onAttendanceClick: () -> Unit,
    onRefreshClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        InfiniteSectionHeader(
            title = "Today Status",
            subtitle = "Status attendance hari ini dari backend",
            leadingIcon = InfiniteIcons.Calendar,
            trailingText = "Refresh",
            trailingIcon = InfiniteIcons.Refresh,
            onTrailingClick = if (state.isRefreshing) null else onRefreshClick
        )

        if (state.isRefreshing) {
            InlineRefreshingIndicator(message = "Refreshing today status...")
        }

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
                currentLocation = currentLocation,
                isRefreshing = state.isRefreshing,
                onAttendanceClick = onAttendanceClick
            )
            is UiState.Error -> TodayStatusErrorCard(
                message = statusState.errorMessage,
                onRefreshClick = onRefreshClick
            )
            is UiState.Idle -> TodayStatusErrorCard(
                message = "Status hari ini belum tersedia.",
                onRefreshClick = onRefreshClick
            )
        }
    }
}

@Composable
private fun TodayStatusLoadingCard() {
    InfiniteCard(
        modifier = Modifier.fillMaxWidth(),
        variant = InfiniteSurfaceVariant.SoftGradient,
        semantic = InfiniteSemantic.Primary,
        size = InfiniteSize.Large,
        density = InfiniteDensity.Spacious
    ) {
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
private fun TodayStatusErrorCard(
    message: String,
    onRefreshClick: () -> Unit
) {
    InfiniteCard(
        modifier = Modifier.fillMaxWidth(),
        variant = InfiniteSurfaceVariant.StatusTint,
        semantic = InfiniteSemantic.Warning,
        size = InfiniteSize.Large,
        density = InfiniteDensity.Spacious
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(
                text = "Status hari ini belum bisa dimuat",
                color = InfiniteColors.Text,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = message,
                color = InfiniteColors.Text.copy(alpha = 0.72f)
            )
            InfiniteButton(
                text = "Coba lagi",
                onClick = onRefreshClick,
                variant = InfiniteButtonVariant.Outlined,
                size = InfiniteSize.Small
            )
        }
    }
}

@Composable
private fun TodayStatusSuccessCard(
    todayStatus: TodayStatus,
    currentLocation: String,
    isRefreshing: Boolean,
    onAttendanceClick: () -> Unit
) {
    val statusKey = todayStatus.attendanceSessionState?.key.orEmpty().lowercase(Locale.ROOT)
    val action = todayStatus.actionState(statusKey)

    InfiniteCard(
        modifier = Modifier.fillMaxWidth(),
        variant = InfiniteSurfaceVariant.SoftGradient,
        semantic = todayStatus.semantic(statusKey),
        size = InfiniteSize.Large,
        density = InfiniteDensity.Spacious
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Attendance target",
                        color = InfiniteColors.Text.copy(alpha = 0.62f)
                    )
                    Text(
                        text = todayStatus.activeLocation?.description ?: "Target belum tersedia",
                        color = InfiniteColors.Text,
                        fontWeight = FontWeight.Bold,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                InfiniteStatusPill(
                    label = todayStatus.displayStatus(statusKey),
                    variant = todayStatus.statusVariant(statusKey),
                    size = InfiniteSize.Small
                )
            }

            StatusMetricGrid(
                items = listOf(
                    TodayStatusMetric("Status", todayStatus.displayStatus(statusKey)),
                    TodayStatusMetric("Mode", todayStatus.activeMode.ifBlank { "--" }),
                    TodayStatusMetric("Lokasi Target", todayStatus.activeLocation?.description ?: "--"),
                    TodayStatusMetric("Check-in", formatTime(todayStatus.checkedInAt, todayStatus.checkedInAtIso)),
                    TodayStatusMetric("Check-out", formatTime(todayStatus.checkedOutAt, todayStatus.checkedOutAtIso)),
                    TodayStatusMetric("Durasi Kerja", formatDuration(todayStatus.workDurationSeconds))
                )
            )

            GeofenceSummary(
                targetLocation = todayStatus.activeLocation?.description,
                currentLocation = currentLocation
            )

            InfiniteButton(
                text = action.label,
                onClick = onAttendanceClick,
                variant = action.variant,
                size = InfiniteSize.Medium,
                state = if (action.enabled && !isRefreshing) InfiniteButtonState.Enabled else InfiniteButtonState.Disabled,
                fullWidth = true
            )
        }
    }
}

@Composable
private fun StatusMetricGrid(items: List<TodayStatusMetric>) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        items.chunked(2).forEach { rowItems ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                rowItems.forEach { item ->
                    TodayStatusMetricItem(
                        metric = item,
                        modifier = Modifier.weight(1f)
                    )
                }
                if (rowItems.size == 1) {
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun TodayStatusMetricItem(
    metric: TodayStatusMetric,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(18.dp))
            .background(InfiniteColors.Surface.copy(alpha = 0.62f))
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(
            text = metric.label,
            color = InfiniteColors.Text.copy(alpha = 0.58f),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Text(
            text = metric.value,
            color = InfiniteColors.Text,
            fontWeight = FontWeight.SemiBold,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun GeofenceSummary(
    targetLocation: String?,
    currentLocation: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(InfiniteColors.Primary.copy(alpha = 0.08f))
            .padding(12.dp),
        verticalAlignment = Alignment.Top
    ) {
        Icon(
            imageVector = InfiniteIcons.Location,
            contentDescription = null,
            tint = InfiniteColors.Primary,
            modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.width(10.dp))
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                text = "Geofence / target summary",
                color = InfiniteColors.Text,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = "Target: ${targetLocation ?: "belum tersedia"}. Lokasi perangkat: $currentLocation",
                color = InfiniteColors.Text.copy(alpha = 0.68f)
            )
        }
    }
}

private data class TodayStatusMetric(
    val label: String,
    val value: String
)

private data class TodayStatusAction(
    val label: String,
    val enabled: Boolean,
    val variant: InfiniteButtonVariant
)

private fun TodayStatus.actionState(statusKey: String): TodayStatusAction {
    return when {
        statusKey == "not_started" && canCheckIn -> TodayStatusAction(
            label = "Mulai Check-in",
            enabled = true,
            variant = InfiniteButtonVariant.Primary
        )
        statusKey == "active" && canCheckOut && activeAttendanceId != null -> TodayStatusAction(
            label = "Lanjut Check-out",
            enabled = true,
            variant = InfiniteButtonVariant.Warning
        )
        statusKey == "completed" -> TodayStatusAction(
            label = "Attendance selesai",
            enabled = false,
            variant = InfiniteButtonVariant.Success
        )
        statusKey == "unavailable" -> TodayStatusAction(
            label = "Attendance tidak tersedia",
            enabled = false,
            variant = InfiniteButtonVariant.Outlined
        )
        else -> TodayStatusAction(
            label = "Buka Attendance",
            enabled = canCheckIn || canCheckOut,
            variant = InfiniteButtonVariant.Outlined
        )
    }
}

private fun TodayStatus.displayStatus(statusKey: String): String {
    attendanceSessionState?.label?.takeIf { it.isNotBlank() }?.let { return it }
    return when (statusKey) {
        "not_started" -> "Belum check-in"
        "active" -> "Sesi aktif"
        "completed" -> "Selesai"
        "unavailable" -> "Tidak tersedia"
        else -> statusKey.takeIf { it.isNotBlank() } ?: "Unknown"
    }
}

private fun TodayStatus.statusVariant(statusKey: String): InfiniteStatusVariant {
    return when (statusKey) {
        "active" -> InfiniteStatusVariant.Active
        "completed" -> InfiniteStatusVariant.Completed
        "not_started" -> InfiniteStatusVariant.NotStarted
        "unavailable" -> InfiniteStatusVariant.Unavailable
        else -> InfiniteStatusVariant.Unknown
    }
}

private fun TodayStatus.semantic(statusKey: String): InfiniteSemantic {
    return when (statusKey) {
        "active", "completed" -> InfiniteSemantic.Success
        "not_started" -> InfiniteSemantic.Info
        "unavailable" -> InfiniteSemantic.Warning
        else -> InfiniteSemantic.Neutral
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
        ?: "--"
}

private fun formatDuration(seconds: Long?): String {
    if (seconds == null || seconds <= 0L) return "--"
    val hours = seconds / 3600
    val minutes = (seconds % 3600) / 60
    return when {
        hours > 0 && minutes > 0 -> "${hours}j ${minutes}m"
        hours > 0 -> "${hours}j"
        minutes > 0 -> "${minutes}m"
        else -> "<1m"
    }
}
