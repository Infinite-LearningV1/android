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
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
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
) {
    val userProfile by viewModel.userProfileState.collectAsState()
    val attendanceState by viewModel.topAttendanceHistoryState.collectAsState()
    val currentLocation by viewModel.currentAddressState.collectAsState()
    val todayStatusState by viewModel.todayStatusState.collectAsState()

    val isLoading = attendanceState is UiState.Loading
    val scrollState = rememberScrollState()
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
                            .verticalScroll(scrollState)
                    ) {
                        when (userProfile?.roleName) {
                            "Internship" -> {
                                InternshipContent(
                                    user = userProfile,
                                    currentLocation = currentLocation,
                                    attendanceState = attendanceState,
                                    todayStatusState = todayStatusState,
                                    navigateAttendance = navigateAttendance,
                                    navigateToListMyAttendance = navigateListMyAttendance,
                                    refreshDashboard = { viewModel.refreshDashboard(forceRefresh = true) }
                                )
                            }

                            "Admin", "Employee", "Management" -> {
                                EmployeeAndManagerComponent(
                                    user = userProfile,
                                    attendanceState = attendanceState,
                                    todayStatusState = todayStatusState,
                                    currentLocation = currentLocation,
                                    isLoading = isLoading,
                                    navigateAttendance = navigateAttendance,
                                    navigateTimeOffRequest = navigateTimeOffRequest,
                                    navigateListMyAttendance = navigateListMyAttendance,
                                    refreshDashboard = { viewModel.refreshDashboard(forceRefresh = true) }
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
