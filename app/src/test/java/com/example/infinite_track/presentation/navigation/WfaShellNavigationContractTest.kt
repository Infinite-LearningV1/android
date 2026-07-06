package com.example.infinite_track.presentation.navigation

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class WfaShellNavigationContractTest {

    @Test
    fun `staff items use Home History WFA Profile order`() {
        val routes = WfaShellNavigationContract.staffItems().map { it.screen.route }

        assertEquals(
            listOf(
                Screen.Home.route,
                Screen.History.route,
                Screen.Wfa.route,
                Screen.Profile.route
            ),
            routes
        )
    }

    @Test
    fun `internship items use Home History WFA Profile order`() {
        val routes = WfaShellNavigationContract.internshipItems().map { it.screen.route }

        assertEquals(
            listOf(
                Screen.Home.route,
                Screen.History.route,
                Screen.Wfa.route,
                Screen.Profile.route
            ),
            routes
        )
    }

    @Test
    fun `wfa tab stays selected for WFA root and booking details`() {
        val wfaItem = WfaShellNavigationContract.staffItems()[2]

        assertTrue(WfaShellNavigationContract.isSelected(wfaItem, Screen.Wfa.route))
        assertTrue(WfaShellNavigationContract.isSelected(wfaItem, Screen.DetailsMyBooking.route))
        assertFalse(WfaShellNavigationContract.isSelected(wfaItem, Screen.Attendance.route))
        assertFalse(WfaShellNavigationContract.isSelected(wfaItem, Screen.WfaBooking.route))
    }

    @Test
    fun `bottom bar stays hidden on attendance and location flow routes`() {
        assertFalse(WfaShellNavigationContract.shouldShowBottomBar(Screen.Attendance.route))
        assertFalse(WfaShellNavigationContract.shouldShowBottomBar(Screen.LocationSearch.route))
        assertTrue(WfaShellNavigationContract.shouldShowBottomBar(Screen.Wfa.route))
    }
}
