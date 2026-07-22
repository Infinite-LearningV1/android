package com.example.infinite_track.presentation.screen.attendance

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.example.infinite_track.presentation.components.maps.AttendanceMap
import com.example.infinite_track.presentation.theme.Infinite_TrackTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class AttendancePermissionRevocationTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun revokedPreciseLocationRendersPassiveFallbackWithoutMapContent() {
        composeRule.setContent {
            Infinite_TrackTheme {
                AttendanceMap(hasPreciseLocationPermission = false)
            }
        }

        composeRule.onNodeWithText("Lokasi presisi belum siap").assertIsDisplayed()
        composeRule.onNodeWithText("Mapbox", substring = true).assertDoesNotExist()
    }

    @Test
    fun persistentRecoveryActionReturnsToPermissionReadiness() {
        var navigations = 0
        composeRule.setContent {
            Infinite_TrackTheme {
                AttendancePermissionRevocationRecovery(
                    onNavigatePermissionReadiness = { navigations++ }
                )
            }
        }

        composeRule.onNodeWithText("Kembali ke kesiapan").performClick()
        composeRule.runOnIdle { assertEquals(1, navigations) }
    }
}
