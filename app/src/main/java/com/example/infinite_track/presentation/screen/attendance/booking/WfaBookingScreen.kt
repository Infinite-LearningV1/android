package com.example.infinite_track.presentation.screen.attendance.booking

import android.R
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavHostController
import com.example.infinite_track.presentation.components.dialog.WfaBookingDialog
import com.example.infinite_track.presentation.navigation.model.Screen
import com.example.infinite_track.utils.DialogHelper

@Composable
fun WfaBookingScreen(
    viewModel: WfaBookingViewModel,
    navController: NavHostController
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    // Handle successful booking - navigate back to Home
    LaunchedEffect(uiState.isBookingSuccessful) {
        if (uiState.isBookingSuccessful) {
            // Show success dialog first
            DialogHelper.showDialogSuccess(
                context = context,
                title = "Booking Berhasil",
                textContent = "Permintaan booking WFA Anda telah berhasil dikirim.",
                imageRes = R.drawable.ic_dialog_info
            ) {
                // Navigate to home after user acknowledges success
                navController.navigate(Screen.Home.route) {
                    popUpTo(Screen.Home.route) {
                        inclusive = true
                    }
                    launchSingleTop = true
                }
            }
        }
    }

    // Handle error messages from server
    LaunchedEffect(uiState.error) {
        uiState.error?.let { errorMessage ->
            DialogHelper.showDialogError(
                context = context,
                title = "Booking Gagal",
                textContent = errorMessage
            ) {
                viewModel.clearError() // Clear error after user acknowledges
            }
        }
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
}
