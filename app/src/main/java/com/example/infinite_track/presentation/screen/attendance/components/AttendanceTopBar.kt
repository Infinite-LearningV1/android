package com.example.infinite_track.presentation.screen.attendance.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.MyLocation
import androidx.compose.material.icons.outlined.Security
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.infinite_track.presentation.design.components.navigation.InfiniteTopBar
import com.example.infinite_track.presentation.design.components.navigation.InfiniteTopBarActionButton
import com.example.infinite_track.presentation.theme.Infinite_TrackTheme

@Composable
fun AttendanceTopBar(
    modifier: Modifier = Modifier,
    onBackClicked: () -> Unit,
    onFocusLocationClicked: () -> Unit,
    onPermissionClicked: () -> Unit
) {
    InfiniteTopBar(
        title = "Attendance",
        modifier = modifier,
        navigationIcon = Icons.AutoMirrored.Outlined.ArrowBack,
        navigationContentDescription = "Back",
        onNavigationClick = onBackClicked,
        actionIcon = Icons.Outlined.MyLocation,
        actionContentDescription = "Focus Location",
        onActionClick = onFocusLocationClicked,
        actions = {
            Spacer(modifier = Modifier.width(4.dp))
            InfiniteTopBarActionButton(
                icon = Icons.Outlined.Security,
                contentDescription = "Kelola izin attendance",
                onClick = onPermissionClicked
            )
        }
    )
}

@Preview(showBackground = true)
@Composable
fun AttendanceTopBarPreview() {
    Infinite_TrackTheme {
        AttendanceTopBar(
            onBackClicked = {},
            onFocusLocationClicked = {},
            onPermissionClicked = {}
        )
    }
}
