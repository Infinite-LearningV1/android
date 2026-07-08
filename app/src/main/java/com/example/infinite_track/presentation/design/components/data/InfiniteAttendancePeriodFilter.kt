package com.example.infinite_track.presentation.design.components.data

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.infinite_track.domain.model.attendance.AttendancePeriod

val attendanceReportPeriodOptions = listOf(
    InfiniteFilterOption(AttendancePeriod.DAILY, "Daily"),
    InfiniteFilterOption(AttendancePeriod.WEEKLY, "Weekly"),
    InfiniteFilterOption(AttendancePeriod.MONTHLY, "Monthly"),
    InfiniteFilterOption(AttendancePeriod.CUSTOM, "Custom")
)

@Composable
fun InfiniteAttendancePeriodFilterCard(
    selectedPeriod: String,
    onPeriodSelected: (String) -> Unit,
    modifier: Modifier = Modifier,
    title: String = "Period Filter",
    subtitle: String? = "Choose the reporting period",
    options: List<InfiniteFilterOption> = attendanceReportPeriodOptions
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        InfiniteSectionHeader(
            title = title,
            subtitle = subtitle
        )
        InfiniteAttendancePeriodFilter(
            selectedPeriod = selectedPeriod,
            onPeriodSelected = onPeriodSelected,
            options = options
        )
    }
}

@Composable
fun InfiniteAttendancePeriodFilter(
    selectedPeriod: String,
    onPeriodSelected: (String) -> Unit,
    modifier: Modifier = Modifier,
    options: List<InfiniteFilterOption> = attendanceReportPeriodOptions
) {
    InfiniteFilterChips(
        options = options,
        selectedKey = selectedPeriod,
        onSelected = onPeriodSelected,
        modifier = modifier
    )
}
