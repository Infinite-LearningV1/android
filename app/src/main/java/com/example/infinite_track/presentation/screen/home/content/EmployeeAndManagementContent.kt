package com.example.infinite_track.presentation.screen.home.content

import android.annotation.SuppressLint
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.infinite_track.domain.model.attendance.AttendancePeriodInfo
import com.example.infinite_track.domain.model.attendance.AttendanceRecord
import com.example.infinite_track.domain.model.attendance.AttendanceSummaryInfo
import com.example.infinite_track.domain.model.auth.UserModel
import com.example.infinite_track.presentation.components.loading.LoadingAnimation
import com.example.infinite_track.presentation.components.tittle.Location
import com.example.infinite_track.presentation.components.tittle.nameCards
import com.example.infinite_track.presentation.design.components.data.InfiniteAttendanceTimelineSection
import com.example.infinite_track.presentation.screen.home.HomeTodayStatusUiState
import com.example.infinite_track.utils.UiState
import com.example.infinite_track.utils.getCurrentDate

@SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
@Composable
fun EmployeeAndManagerComponent(
    modifier: Modifier = Modifier,
    user: UserModel?,
    attendanceState: UiState<List<AttendanceRecord>>,
    attendanceSummaryState: UiState<AttendanceSummaryInfo>,
    attendancePeriodInfo: AttendancePeriodInfo?,
    todayStatusState: HomeTodayStatusUiState,
    currentLocation: String,
    isLoading: Boolean = false,
    navigateAttendance: () -> Unit,
    navigateTimeOffRequest: () -> Unit,
    navigateListMyAttendance: () -> Unit,
    navigateServiceComingSoon: () -> Unit
) {
    user?.let { userData ->
        val fullImageUrl = userData.photoUrl?.ifEmpty {
            "https://w7.pngwing.com/pngs/177/551/png-transparent-user-interface-design-computer-icons-default-stephen-salazar-graphy-user-interface-design-computer-wallpaper-sphere-thumbnail.png"
        }
            ?: "https://w7.pngwing.com/pngs/177/551/png-transparent-user-interface-design-computer-icons-default-stephen-salazar-graphy-user-interface-design-computer-wallpaper-sphere-thumbnail.png"

        Box(
            modifier = modifier.fillMaxSize()
        ) {
            Column(
                modifier
                    .fillMaxSize()
                    .padding(16.dp)
            ) {
                Location(date = getCurrentDate(), location = currentLocation)

                Spacer(modifier.height(12.dp))

                nameCards(
                    greeting = "Hello",
                    userName = userData.fullName,
                    division = userData.positionName ?: "No Position",
                    profileImage = fullImageUrl
                )

                Spacer(modifier.height(14.dp))

                HomeTodayStatusCard(
                    state = todayStatusState,
                    currentLocation = currentLocation
                )

                Spacer(modifier.height(14.dp))

                CompanyServicesGrid(
                    onAttendanceClick = navigateAttendance,
                    onTimeOffClick = navigateTimeOffRequest,
                    onComingSoonClick = navigateServiceComingSoon
                )

                Spacer(modifier.height(14.dp))

                HomeAttendanceReportSummaryCard(
                    summaryState = attendanceSummaryState,
                    periodInfo = attendancePeriodInfo,
                    onViewReportClick = navigateListMyAttendance
                )

                Spacer(modifier.height(14.dp))

                if (isLoading) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        LoadingAnimation()
                    }
                } else {
                    InfiniteAttendanceTimelineSection(
                        attendanceState = attendanceState,
                        maxItems = 3,
                        showModeLabel = false,
                        title = "Recent Attendance",
                        showExternalHeader = false,
                        showSeeAllAction = true,
                        onSeeAllClick = navigateListMyAttendance
                    )
                }
            }
        }
    }
}
