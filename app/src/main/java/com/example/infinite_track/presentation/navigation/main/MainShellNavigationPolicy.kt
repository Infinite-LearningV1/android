package com.example.infinite_track.presentation.navigation.main

import com.example.infinite_track.R
import com.example.infinite_track.presentation.navigation.model.NavigationItem
import com.example.infinite_track.presentation.navigation.model.Screen

data class FabConfig(
    val iconRes: Int,
    val destination: Screen
)

object MainShellNavigationPolicy {
    private val hiddenBottomBarRoutes = setOf(
        Screen.Attendance.route,
        Screen.DetailMyAttendance.route,
        Screen.DetailListTimeOff.route,
        Screen.EditProfile.route,
        Screen.TimeOffRequest.route,
        Screen.TimeOffReq.route,
        Screen.FaceScanner.route,
        Screen.LocationSearch.route,
        Screen.FAQ.route
    )

    private val supportedBottomBarRoles = setOf(
        "Employee",
        "Admin",
        "Management",
        "Internship"
    )

    private val historySelectedRoutes = setOf(
        Screen.History.route,
        Screen.HistoryFlow.route,
        Screen.DetailMyAttendance.route
    )

    private val profileSelectedRoutes = setOf(
        Screen.Profile.route,
        Screen.ProfileFlow.route,
        Screen.EditProfile.route
    )

    private val defaultBottomBarItems = listOf(
        NavigationItem(
            labelRes = R.string.bottom_menu_home,
            selectedIcon = R.drawable.ic_menu_home_selected,
            unselectedIcon = R.drawable.ic_menu_home,
            screen = Screen.Home
        ),
        NavigationItem(
            labelRes = R.string.bottom_menu_history,
            selectedIcon = R.drawable.ic_history_selected,
            unselectedIcon = R.drawable.ic_history,
            screen = Screen.History
        ),
        NavigationItem(
            labelRes = R.string.bottom_menu_contact,
            selectedIcon = R.drawable.ic_contact_selected,
            unselectedIcon = R.drawable.ic_contact,
            screen = Screen.Contact
        ),
        NavigationItem(
            labelRes = R.string.bottom_menu_profile,
            selectedIcon = R.drawable.ic_profile_selected,
            unselectedIcon = R.drawable.ic_profile,
            screen = Screen.Profile
        )
    )

    fun bottomBarItemsForRole(userRole: String): List<NavigationItem> {
        return if (userRole in supportedBottomBarRoles) {
            defaultBottomBarItems
        } else {
            emptyList()
        }
    }

    fun shouldShowBottomBar(currentRoute: String?): Boolean {
        return currentRoute !in hiddenBottomBarRoutes
    }

    fun isSelected(item: NavigationItem, currentRoute: String?): Boolean {
        if (currentRoute == null) return false

        return when (item.screen.route) {
            Screen.History.route -> currentRoute in historySelectedRoutes
            Screen.Profile.route -> currentRoute in profileSelectedRoutes
            else -> currentRoute == item.screen.route
        }
    }

    fun fabConfigForRole(userRole: String): FabConfig? {
        return when (userRole) {
            "Internship" -> FabConfig(
                iconRes = R.drawable.ic_intern_fab,
                destination = Screen.Attendance
            )
            "Management" -> FabConfig(
                iconRes = R.drawable.ic_management,
                destination = Screen.TimeOffReq
            )
            else -> null
        }
    }
}
