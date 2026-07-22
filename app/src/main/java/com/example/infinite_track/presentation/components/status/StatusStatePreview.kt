package com.example.infinite_track.presentation.components.status

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.infinite_track.presentation.theme.Infinite_TrackTheme
import com.example.infinite_track.presentation.design.components.status.InfiniteInlineAlert
import com.example.infinite_track.presentation.design.tokens.InfiniteSemantic

@Preview(showBackground = true)
@Composable
private fun StatusStateInlineAlertPreview() {
    Infinite_TrackTheme {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            InfiniteTrackInlineAlert(
                status = StatusStates.Success,
                title = "Success",
                message = "Your request has been completed."
            )
            InfiniteTrackInlineAlert(
                status = StatusStates.Error,
                title = "Error",
                message = "Something went wrong. Please try again."
            )
            InfiniteTrackInlineAlert(
                status = StatusStates.Warning,
                title = "Warning",
                message = "Please confirm before continuing."
            )
            InfiniteTrackInlineAlert(
                status = StatusStates.Info,
                title = "Info",
                message = "Here is an update for your current flow."
            )
        }
    }
}

@Preview(name = "Persistent recovery 320dp", showBackground = true, widthDp = 320, fontScale = 2f)
@Composable
private fun StatusStatePersistentRecoveryPreview() {
    Infinite_TrackTheme {
        InfiniteInlineAlert("Connection lost", "This inline feedback stays until dismissed.", InfiniteSemantic.Error, "Retry", {}, onDismiss = {})
    }
}

@Preview(showBackground = true)
@Composable
private fun StatusStateDialogPreview() {
    Infinite_TrackTheme {
        InfiniteTrackStatusDialog(
            status = StatusStates.Success,
            title = "Complete your Profile",
            message = "Please head to Setting and complete your profile",
            showDialog = true,
            modifier = Modifier.fillMaxWidth(),
            onDismiss = {},
            onConfirm = {}
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun StatusStateLogoutConfirmPreview() {
    Infinite_TrackTheme {
        InfiniteTrackConfirmDialog(
            status = StatusStates.Warning,
            title = "Log out",
            message = "Are you sure you want to log out?",
            showDialog = true,
            confirmText = "Log out",
            cancelText = "Cancel",
            isDestructive = true,
            onDismiss = {},
            onConfirm = {}
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun StatusStateCheckInConfirmPreview() {
    Infinite_TrackTheme {
        InfiniteTrackConfirmDialog(
            status = StatusStates.Info,
            title = "Confirm Check-in",
            message = "Are you sure you want to check in now?",
            showDialog = true,
            confirmText = "Check in",
            cancelText = "Cancel",
            onDismiss = {},
            onConfirm = {}
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun StatusStateCheckoutConfirmPreview() {
    Infinite_TrackTheme {
        InfiniteTrackConfirmDialog(
            status = StatusStates.Info,
            title = "Confirm Checkout",
            message = "Are you sure you want to check out now?",
            showDialog = true,
            confirmText = "Check out",
            cancelText = "Cancel",
            onDismiss = {},
            onConfirm = {}
        )
    }
}
