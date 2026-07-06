package com.example.infinite_track.presentation.screen.attendance.booking

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.navigation.NavHostController
import com.example.infinite_track.presentation.components.dialog.WfaBookingDialog
import com.example.infinite_track.presentation.components.status.InfiniteTrackStatusDialog
import com.example.infinite_track.presentation.components.status.StatusStates
import com.example.infinite_track.presentation.navigation.Screen

@Composable
fun WfaBookingScreen(
    viewModel: WfaBookingViewModel,
    navController: NavHostController
) {
    val uiState by viewModel.uiState.collectAsState()
    var showSuccessDialog by remember { mutableStateOf(false) }
    var errorDialogMessage by remember { mutableStateOf<String?>(null) }

    // Handle successful booking - navigate back to Home
    LaunchedEffect(uiState.isBookingSuccessful) {
        if (uiState.isBookingSuccessful) {
            showSuccessDialog = true
        }
    }

    // Handle error messages from server
    LaunchedEffect(uiState.error) {
        errorDialogMessage = uiState.error
    }


    // Display the WFA Booking Dialog
    WfaBookingDialog(
        showDialog = true,
        fullName = uiState.fullName,
        division = uiState.division,
        address = uiState.address,
        radius = uiState.radius.toString(),
        description = uiState.description,
        schedule = uiState.scheduleDate,
        notes = uiState.notes,
        onRadiusChange = { radiusStr ->
            viewModel.onRadiusChanged(radiusStr.toIntOrNull() ?: 100)
        },
        onDescriptionChange = viewModel::onDescriptionChanged,
        onScheduleChange = viewModel::onScheduleDateChanged,
        onNotesChange = viewModel::onNotesChanged,
        onSendClick = viewModel::onSubmitBooking,
        onDismissRequest = {
            navController.navigateUp()
        }
    )

    InfiniteTrackStatusDialog(
        status = StatusStates.Success,
        title = "Booking Berhasil",
        message = "Permintaan booking WFA Anda telah berhasil dikirim.",
        showDialog = showSuccessDialog,
        onDismiss = { showSuccessDialog = false },
        onConfirm = {
            showSuccessDialog = false
            navController.navigate(Screen.Home.route) {
                popUpTo(Screen.Home.route) {
                    inclusive = true
                }
                launchSingleTop = true
            }
        }
    )

    errorDialogMessage?.let { message ->
        InfiniteTrackStatusDialog(
            status = StatusStates.Error,
            title = "Booking Gagal",
            message = message,
            showDialog = true,
            onDismiss = {
                errorDialogMessage = null
                viewModel.clearError()
            },
            onConfirm = {
                errorDialogMessage = null
                viewModel.clearError()
            }
        )
    }
}
