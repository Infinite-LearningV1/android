package com.example.infinite_track.presentation.screen.home.content

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.infinite_track.domain.model.attendance.AttendanceRecord
import com.example.infinite_track.domain.model.auth.UserModel
import com.example.infinite_track.presentation.components.tittle.Location
import com.example.infinite_track.presentation.components.tittle.nameCards
import com.example.infinite_track.presentation.design.components.data.InfiniteAttendanceTimelineSection
import com.example.infinite_track.presentation.screen.home.HomeTodayStatusUiState
import com.example.infinite_track.utils.UiState
import com.example.infinite_track.utils.getCurrentDate

@Composable
fun InternshipContent(
    modifier: Modifier = Modifier,
    user: UserModel?,
    currentLocation: String,
    attendanceState: UiState<List<AttendanceRecord>>,
    todayStatusState: HomeTodayStatusUiState,
    navigateToListMyAttendance: () -> Unit
) {
    Box(
        modifier = modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxHeight()
                .padding(start = 20.dp, top = 20.dp, end = 20.dp, bottom = 12.dp)
        ) {
            Location(
                date = getCurrentDate(),
                location = currentLocation
            )
            Spacer(modifier = Modifier.height(12.dp))

            user?.let { userData ->
                nameCards(
                    greeting = "Hello",
                    userName = userData.fullName,
                    division = userData.positionName ?: "Position",
                    profileImage = userData.photoUrl?.ifEmpty {
                        "https://w7.pngwing.com/pngs/177/551/png-transparent-user-interface-design-computer-icons-default-stephen-salazar-graphy-user-interface-design-computer-wallpaper-sphere-thumbnail.png"
                    }
                        ?: "https://w7.pngwing.com/pngs/177/551/png-transparent-user-interface-design-computer-icons-default-stephen-salazar-graphy-user-interface-design-computer-wallpaper-sphere-thumbnail.png"
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            HomeTodayStatusCard(
                state = todayStatusState,
                currentLocation = currentLocation
            )

            Spacer(modifier = Modifier.height(14.dp))

            InfiniteAttendanceTimelineSection(
                attendanceState = attendanceState,
                maxItems = 3,
                onSeeAllClick = navigateToListMyAttendance
            )
        }
    }
}
