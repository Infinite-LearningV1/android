package com.example.infinite_track.presentation.screen.home

import android.annotation.SuppressLint
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.infinite_track.presentation.components.loading.InlineRefreshingIndicator
import com.example.infinite_track.presentation.components.loading.LoadingAnimation
import com.example.infinite_track.presentation.screen.home.content.EmployeeAndManagerComponent
import com.example.infinite_track.presentation.screen.home.content.InternshipContent
import com.example.infinite_track.utils.UiState

@SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
@Composable
fun HomeScreen(
    modifier: Modifier = Modifier,
    viewModel: HomeViewModel,
    navigateAttendance: () -> Unit = {},
    navigateTimeOffRequest: () -> Unit = {},
    navigateListMyAttendance: () -> Unit = {},
    navigateServiceComingSoon: () -> Unit = {},
) {
    val userProfile by viewModel.userProfileState.collectAsState()
    val attendanceState by viewModel.topAttendanceHistoryState.collectAsState()
    val attendanceSummaryState by viewModel.topAttendanceSummaryState.collectAsState()
    val attendancePeriodInfo by viewModel.topAttendancePeriodInfo.collectAsState()
    val currentLocation by viewModel.currentAddressState.collectAsState()
    val todayStatusState by viewModel.todayStatusState.collectAsState()

    val isLoading = attendanceState is UiState.Loading
    val scrollState = rememberScrollState()
    val refreshDragDistance = remember { mutableStateOf(0f) }
    val pullToRefreshConnection = remember(scrollState, todayStatusState.isRefreshing, isLoading) {
        object : NestedScrollConnection {
            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                if (available.y > 0 && scrollState.value == 0 && !todayStatusState.isRefreshing && !isLoading) {
                    refreshDragDistance.value += available.y
                    if (refreshDragDistance.value >= HomePullToRefreshThresholdPx) {
                        refreshDragDistance.value = 0f
                        viewModel.refreshDashboard(forceRefresh = true)
                    }
                }
                if (available.y < 0) {
                    refreshDragDistance.value = 0f
                }
                return Offset.Zero
            }
        }
    }
    val lifecycleOwner = LocalLifecycleOwner.current
    val hasHandledInitialResume = remember { mutableStateOf(false) }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                if (hasHandledInitialResume.value) {
                    viewModel.refreshDashboard(forceRefresh = true)
                } else {
                    hasHandledInitialResume.value = true
                }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = Color.Transparent,
        content = {
            Box(
                modifier = modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                if (userProfile == null) {
                    LoadingAnimation()
                } else {
                    Column(
                        modifier = modifier
                            .fillMaxSize()
                            .nestedScroll(pullToRefreshConnection)
                            .verticalScroll(scrollState)
                    ) {
                        if (todayStatusState.isRefreshing) {
                            InlineRefreshingIndicator(message = "Refreshing today status...")
                        }

                        when (userProfile?.roleName) {
                            "Internship" -> {
                                InternshipContent(
                                    user = userProfile,
                                    currentLocation = currentLocation,
                                    attendanceState = attendanceState,
                                    attendanceSummaryState = attendanceSummaryState,
                                    attendancePeriodInfo = attendancePeriodInfo,
                                    todayStatusState = todayStatusState,
                                    navigateToListMyAttendance = navigateListMyAttendance
                                )
                            }

                            "Admin", "Employee", "Management" -> {
                                EmployeeAndManagerComponent(
                                    user = userProfile,
                                    attendanceState = attendanceState,
                                    attendanceSummaryState = attendanceSummaryState,
                                    attendancePeriodInfo = attendancePeriodInfo,
                                    todayStatusState = todayStatusState,
                                    currentLocation = currentLocation,
                                    isLoading = isLoading,
                                    navigateAttendance = navigateAttendance,
                                    navigateTimeOffRequest = navigateTimeOffRequest,
                                    navigateListMyAttendance = navigateListMyAttendance,
                                    navigateServiceComingSoon = navigateServiceComingSoon
                                )
                            }

                            else -> {
                                Text("Unknown user role: ${userProfile?.roleName}")
                            }
                        }
                    }
                }
            }
        }
    )
}

private const val HomePullToRefreshThresholdPx = 160f
