package com.example.infinite_track.presentation.components.popUp

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.example.infinite_track.R
import com.example.infinite_track.presentation.core.body1
import com.example.infinite_track.presentation.core.body2
import com.example.infinite_track.presentation.core.headline0
import com.example.infinite_track.presentation.core.headline4
import com.example.infinite_track.presentation.components.status.StatusStateSpec
import com.example.infinite_track.presentation.theme.Blue_700
import com.example.infinite_track.presentation.theme.Infinite_TrackTheme

@Composable
fun AttendanceConfirmPopup(
    modifier: Modifier = Modifier,
    status: String,
    showDialog: Boolean,
    onDismiss: () -> Unit,
) {
    val statusSpec = when (status.lowercase()) {
        "confirmed" -> StatusStateSpec(id = "success", value = "Confirmed")
        "overtime" -> StatusStateSpec(id = "info", value = "You Worked Over Time")
        "late" -> StatusStateSpec(id = "warning", value = "You Are Late!")
        else -> StatusStateSpec(id = "error", value = "Status Not Found!")
    }
    val popupImage = when (status.lowercase()) {
        "confirmed" -> R.drawable.ic_attendance_confirmed
        "overtime" -> R.drawable.ic_attendance_overtime
        "late" -> R.drawable.ic_attendance_late
        else -> R.drawable.ic_profile
    }
    val popupText = statusSpec.value
    val imageModifier = when (status.lowercase()) {
        "confirmed" -> Modifier.size(147.dp)
        "overtime" -> Modifier
            .height(144.dp)
            .width(216.dp)
        "late" -> Modifier
            .height(71.dp)
            .width(80.dp)
        else -> Modifier.size(147.dp)
    }
    val textStyle = when (status.lowercase()) {
        "confirmed" -> headline4
        "overtime" -> headline4
        "late" -> headline0
        else -> headline4
    }
    val textColor = when (status.lowercase()) {
        "confirmed" -> Blue_700
        "overtime" -> Blue_700
        "late" -> Color(0xFFD42323)
        else -> Blue_700
    }

    // Logika untuk menampilkan popup
    if (showDialog) {
        Dialog(onDismissRequest = { showDialog }) {
            Box(
                modifier = Modifier
                    .height(290.dp)
                    .width(272.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFFF3ECFF)),
                contentAlignment = Alignment.Center
            ){
                Column(
                    modifier = Modifier
                        .padding(16.dp)
                        .fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Image(
                        painter = painterResource(id = popupImage),
                        contentDescription = "icon attendance confirmed",
                        modifier = imageModifier
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = popupText,
                        style = textStyle,
                        color = textColor,
                    )
                    Spacer(modifier = Modifier.height(20.dp))
                    Text(
                        text = "Your attendance has been confirmed",
                        style = body2
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Button(
                        modifier = Modifier
                            .height(39.dp)
                            .width(203.dp),
                        shape = RoundedCornerShape(6.dp),
                        onClick = { onDismiss() }
                    ) {
                        Text(
                            text = "OK",
                            style = body1
                        )
                    }
                }
            }
        }
    }
}

@Preview(showSystemUi = true)
@Composable
private fun PreviewAttendanceConfirmPopup() {
    Infinite_TrackTheme {
        var showDialog by remember { mutableStateOf(true) }
        Box(
            modifier = Modifier
                .fillMaxSize(),
            contentAlignment = Alignment.Center
        ){
            AttendanceConfirmPopup(
                status = "overtime",
                showDialog = showDialog,
                onDismiss = {showDialog = false}
            )
        }
    }
}