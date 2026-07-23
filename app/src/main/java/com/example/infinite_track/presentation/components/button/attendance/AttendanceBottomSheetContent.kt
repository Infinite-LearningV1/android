package com.example.infinite_track.presentation.components.button.attendance

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.infinite_track.domain.model.attendance.Location
import com.example.infinite_track.domain.model.attendance.SelectedTargetLocation
import com.example.infinite_track.domain.model.attendance.WorkMode
import com.example.infinite_track.presentation.components.button.InfiniteTrackButton
import com.example.infinite_track.presentation.components.status.InfiniteTrackInlineAlert
import com.example.infinite_track.presentation.components.status.StatusStates
import com.example.infinite_track.presentation.core.headline4
import com.example.infinite_track.presentation.design.components.status.InfiniteInlineAlertDuration
import com.example.infinite_track.presentation.screen.attendance.AttendanceActionState
import com.example.infinite_track.presentation.screen.attendance.AttendanceBlockReason
import com.example.infinite_track.presentation.theme.Infinite_TrackTheme
import com.example.infinite_track.presentation.theme.Purple_500

/**
 * Komponen utama untuk konten BottomSheet attendance yang merakit semua komponen kecil.
 *
 * @param modifier Modifier untuk styling komponen
 * @param targetLocationInfo Informasi lokasi target yang sudah di-resolve dari mode terpilih.
 * @param currentLocationAddress Alamat lokasi saat ini.
 * @param selectedWorkMode Mode kerja yang dipilih.
 * @param isBookingEnabled Apakah tombol booking dapat diklik
 * @param isCheckInEnabled Apakah tombol check-in dapat diklik
 * @param checkInButtonText Teks pada tombol check-in
 * @param actionState State aksi attendance eksplisit untuk inline guidance Layer 3
 * @param outOfRangeWarningText Teks peringatan ketika di luar jangkauan
 * @param onSearchLocationClick Callback ketika tombol search location diklik (hanya untuk WFA)
 * @param onModeSelected Callback ketika mode kerja dipilih
 * @param onBookingClick Callback ketika tombol booking diklik
 * @param onCheckInClick Callback ketika tombol check-in diklik
 */
@Composable
fun AttendanceBottomSheetContent(
    modifier: Modifier = Modifier,
    targetLocationInfo: SelectedTargetLocation?,
    currentLocationAddress: String,
    selectedWorkMode: WorkMode,
    isBookingEnabled: Boolean,
    isCheckInEnabled: Boolean,
    checkInButtonText: String,
    actionState: AttendanceActionState? = null,
    outOfRangeWarningText: String = "Pilih mode kerja dan lokasi target",
    onSearchLocationClick: () -> Unit = {}, // Untuk navigasi ke LocationSearchScreen
    onModeSelected: (WorkMode) -> Unit,
    onBookingClick: () -> Unit,
    onCheckInClick: () -> Unit
) {
    // Transparent content - no background
    Column(
        modifier = modifier
            .fillMaxWidth()
            .wrapContentHeight()
            .padding(start = 20.dp, end = 20.dp, bottom = 20.dp), // Remove top padding
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        // Search Location Button - Only show when Work From Anywhere is selected
        if (selectedWorkMode == WorkMode.WFA) {
            InfiniteTrackButton(
                label = "Cari Lokasi",
                onClick = onSearchLocationClick,
                modifier = Modifier.fillMaxWidth()
            )
        }

        // Location Information Section dengan subtitle
        LocationInfoSection(
            targetLocationInfo = targetLocationInfo,
            currentLocationAddress = currentLocationAddress
        )

        // Out of Range Warning and Work Mode Selector - Always visible
        Column(
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = outOfRangeWarningText,
                style = headline4,
                color = Purple_500
            )

            WorkModeSelector(
                selectedMode = selectedWorkMode,
                onModeSelected = onModeSelected
            )
        }

        actionState?.let { state ->
            AttendanceActionInlineAlert(actionState = state)
        }

        // Action Buttons
        AttendanceActionButtons(
            isBookingEnabled = isBookingEnabled,
            isCheckInEnabled = isCheckInEnabled,
            checkInButtonText = checkInButtonText,
            onBookingClick = onBookingClick,
            onCheckInClick = onCheckInClick,
            showBookingAction = selectedWorkMode == WorkMode.WFA
        )
    }
}

@Composable
private fun AttendanceActionInlineAlert(
    actionState: AttendanceActionState
) {
    when (actionState) {
        is AttendanceActionState.Blocked -> {
            val status = when (actionState.reason) {
                AttendanceBlockReason.SERVER_RESTRICTION,
                AttendanceBlockReason.UNKNOWN -> StatusStates.Error
                else -> StatusStates.Warning
            }
            InfiniteTrackInlineAlert(
                status = status,
                title = actionState.title,
                message = actionState.message
            )
        }

        is AttendanceActionState.VerifyingFace -> {
            InfiniteTrackInlineAlert(
                status = StatusStates.Info,
                title = "Verifikasi wajah",
                message = "Membuka scanner wajah untuk melanjutkan absensi.",
                duration = InfiniteInlineAlertDuration.Persistent
            )
        }

        is AttendanceActionState.Submitting -> {
            InfiniteTrackInlineAlert(
                status = StatusStates.Info,
                title = "Mengirim absensi",
                message = actionState.message,
                duration = InfiniteInlineAlertDuration.Persistent
            )
        }

        is AttendanceActionState.RetryableFailure -> {
            InfiniteTrackInlineAlert(
                status = StatusStates.Error,
                title = actionState.title,
                message = actionState.message
            )
        }

        AttendanceActionState.Completed -> {
            InfiniteTrackInlineAlert(
                status = StatusStates.Info,
                title = "Absensi selesai",
                message = "Anda sudah absen hari ini."
            )
        }

        AttendanceActionState.Loading,
        is AttendanceActionState.Ready,
        is AttendanceActionState.Success -> Unit
    }
}

private fun previewTargetLocation(mode: WorkMode): SelectedTargetLocation {
    return SelectedTargetLocation(
        mode = mode,
        location = Location(
            locationId = mode.categoryId,
            description = "Jl. Sudirman No. 123, Jakarta Pusat, DKI Jakarta",
            latitude = 0.0,
            longitude = 0.0,
            radius = 100,
            category = mode.shortLabel
        ),
        displayName = "Jl. Sudirman No. 123, Jakarta Pusat, DKI Jakarta",
        description = mode.shortLabel,
        isAvailable = true
    )
}

@Preview(showBackground = true)
@Composable
private fun AttendanceBottomSheetContentPreview() {
    Infinite_TrackTheme {
        Column {
            // Preview when in range
            AttendanceBottomSheetContent(
                targetLocationInfo = previewTargetLocation(WorkMode.WFH),
                currentLocationAddress = "Jl. Thamrin No. 456, Jakarta Pusat, DKI Jakarta",
                selectedWorkMode = WorkMode.WFH,
                isBookingEnabled = true,
                isCheckInEnabled = true,
                checkInButtonText = "Check In",
                onModeSelected = {},
                onBookingClick = {},
                onCheckInClick = {}
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun AttendanceBottomSheetContentOutOfRangePreview() {
    Infinite_TrackTheme {
        Column {
            // Preview when out of range
            AttendanceBottomSheetContent(
                targetLocationInfo = previewTargetLocation(WorkMode.WFA),
                currentLocationAddress = "Jl. Kemang No. 789, Jakarta Selatan, DKI Jakarta",
                selectedWorkMode = WorkMode.WFA,
                isBookingEnabled = false,
                isCheckInEnabled = true,
                checkInButtonText = "Check In (WFA)",
                onModeSelected = {},
                onBookingClick = {},
                onCheckInClick = {}
            )
        }
    }
}
