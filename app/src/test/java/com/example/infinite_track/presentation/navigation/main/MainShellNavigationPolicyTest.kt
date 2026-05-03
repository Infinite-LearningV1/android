package com.example.infinite_track.presentation.navigation.main

import com.example.infinite_track.R
import com.example.infinite_track.presentation.navigation.model.Screen
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class MainShellNavigationPolicyTest {

    @Test
    fun `staff roles use home history contact profile order`() {
        val expectedRoutes = listOf(
            Screen.Home.route,
            Screen.History.route,
            Screen.Contact.route,
            Screen.Profile.route
        )

        assertEquals(
            expectedRoutes,
            MainShellNavigationPolicy.bottomBarItemsForRole("Employee").map { it.screen.route }
        )
        assertEquals(
            expectedRoutes,
            MainShellNavigationPolicy.bottomBarItemsForRole("Admin").map { it.screen.route }
        )
        assertEquals(
            expectedRoutes,
            MainShellNavigationPolicy.bottomBarItemsForRole("Management").map { it.screen.route }
        )
    }

    @Test
    fun `internship role uses home history contact profile order`() {
        val expectedRoutes = listOf(
            Screen.Home.route,
            Screen.History.route,
            Screen.Contact.route,
            Screen.Profile.route
        )

        assertEquals(
            expectedRoutes,
            MainShellNavigationPolicy.bottomBarItemsForRole("Internship").map { it.screen.route }
        )
    }

    @Test
    fun `bottom bar items map to existing menu string resources`() {
        val expectedLabels = listOf(
            R.string.bottom_menu_home,
            R.string.bottom_menu_history,
            R.string.bottom_menu_contact,
            R.string.bottom_menu_profile
        )

        assertEquals(
            expectedLabels,
            MainShellNavigationPolicy.bottomBarItemsForRole("Employee").map { it.labelRes }
        )
    }

    @Test
    fun `bottom bar is hidden on shell detail and flow routes that currently suppress navigation`() {
        assertFalse(MainShellNavigationPolicy.shouldShowBottomBar(Screen.Attendance.route))
        assertFalse(MainShellNavigationPolicy.shouldShowBottomBar(Screen.DetailMyAttendance.route))
        assertFalse(MainShellNavigationPolicy.shouldShowBottomBar(Screen.DetailListTimeOff.route))
        assertFalse(MainShellNavigationPolicy.shouldShowBottomBar(Screen.EditProfile.route))
        assertFalse(MainShellNavigationPolicy.shouldShowBottomBar(Screen.TimeOffRequest.route))
        assertFalse(MainShellNavigationPolicy.shouldShowBottomBar(Screen.TimeOffReq.route))
        assertFalse(MainShellNavigationPolicy.shouldShowBottomBar(Screen.FaceScanner.route))
        assertFalse(MainShellNavigationPolicy.shouldShowBottomBar(Screen.LocationSearch.route))
        assertFalse(MainShellNavigationPolicy.shouldShowBottomBar(Screen.FAQ.route))
        assertTrue(MainShellNavigationPolicy.shouldShowBottomBar(Screen.History.route))
        assertTrue(MainShellNavigationPolicy.shouldShowBottomBar(Screen.Contact.route))
    }

    @Test
    fun `history tab stays selected for history detail routes`() {
        val historyItem = MainShellNavigationPolicy.bottomBarItemsForRole("Employee")[1]

        assertTrue(MainShellNavigationPolicy.isSelected(historyItem, Screen.History.route))
        assertTrue(MainShellNavigationPolicy.isSelected(historyItem, Screen.HistoryFlow.route))
        assertTrue(MainShellNavigationPolicy.isSelected(historyItem, Screen.DetailMyAttendance.route))
        assertFalse(MainShellNavigationPolicy.isSelected(historyItem, Screen.Contact.route))
    }

    @Test
    fun `profile tab stays selected for profile detail routes`() {
        val profileItem = MainShellNavigationPolicy.bottomBarItemsForRole("Employee")[3]

        assertTrue(MainShellNavigationPolicy.isSelected(profileItem, Screen.Profile.route))
        assertTrue(MainShellNavigationPolicy.isSelected(profileItem, Screen.ProfileFlow.route))
        assertTrue(MainShellNavigationPolicy.isSelected(profileItem, Screen.EditProfile.route))
        assertFalse(MainShellNavigationPolicy.isSelected(profileItem, Screen.Home.route))
    }

    @Test
    fun `fab mapping preserves current repo behavior`() {
        assertEquals(
            Screen.Attendance.route,
            MainShellNavigationPolicy.fabConfigForRole("Internship")?.destination?.route
        )
        assertEquals(
            Screen.TimeOffReq.route,
            MainShellNavigationPolicy.fabConfigForRole("Management")?.destination?.route
        )
        assertNull(MainShellNavigationPolicy.fabConfigForRole("Employee"))
        assertNull(MainShellNavigationPolicy.fabConfigForRole("Admin"))
    }
}
