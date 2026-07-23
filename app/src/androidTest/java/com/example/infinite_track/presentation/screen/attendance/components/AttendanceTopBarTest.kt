package com.example.infinite_track.presentation.screen.attendance.components

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.performClick
import com.example.infinite_track.presentation.theme.Infinite_TrackTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class AttendanceTopBarTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun permissionActionRemainsAvailableBesideCurrentLocation() {
        var focusClicks = 0
        var permissionClicks = 0
        composeRule.setContent {
            Infinite_TrackTheme {
                AttendanceTopBar(
                    onBackClicked = {},
                    onFocusLocationClicked = { focusClicks++ },
                    onPermissionClicked = { permissionClicks++ }
                )
            }
        }

        composeRule.onNodeWithContentDescription("Focus Location").performClick()
        composeRule.onNodeWithContentDescription("Kelola izin attendance").performClick()

        composeRule.runOnIdle {
            assertEquals(1, focusClicks)
            assertEquals(1, permissionClicks)
        }
    }
}
