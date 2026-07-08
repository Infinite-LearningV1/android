package com.example.infinite_track.presentation.components.button.attendance

import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.infinite_track.presentation.components.button.ButtonStyle
import com.example.infinite_track.presentation.components.button.StatefulButton
import com.example.infinite_track.presentation.core.headline4
import com.example.infinite_track.presentation.theme.Blue_700
import com.example.infinite_track.presentation.theme.Infinite_TrackTheme

/**
 * Komponen yang berisi tombol-tombol aksi untuk attendance (Booking dan Check-in).
 *
 * @param isBookingEnabled Apakah tombol booking dapat diklik
 * @param isCheckInEnabled Apakah tombol check-in dapat diklik
 * @param checkInButtonText Teks yang ditampilkan pada tombol check-in
 * @param onBookingClick Callback ketika tombol booking diklik
 * @param onCheckInClick Callback ketika tombol check-in diklik
 */
@Composable
fun AttendanceActionButtons(
    isBookingEnabled: Boolean,
    isCheckInEnabled: Boolean,
    checkInButtonText: String,
    onBookingClick: () -> Unit,
    onCheckInClick: () -> Unit,
    modifier: Modifier = Modifier,
    showBookingAction: Boolean = true
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        if (showBookingAction) {
            StatefulButton(
                text = "Booking Location",
                style = ButtonStyle.Outlined,
                enabled = isBookingEnabled,
                onClick = onBookingClick
            )
        }

        StatefulButton(
            text = checkInButtonText,
            style = ButtonStyle.Elevated,
            enabled = isCheckInEnabled,
            onClick = onCheckInClick
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun AttendanceActionButtonsPreview() {
    Infinite_TrackTheme {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "Attendance Action Buttons",
                style = headline4,
                color = Blue_700
            )

            AttendanceActionButtons(
                isBookingEnabled = true,
                isCheckInEnabled = true,
                checkInButtonText = "Check In",
                onBookingClick = {},
                onCheckInClick = {}
            )

            AttendanceActionButtons(
                isBookingEnabled = false,
                isCheckInEnabled = false,
                checkInButtonText = "Check Out",
                onBookingClick = {},
                onCheckInClick = {}
            )
        }
    }
}
