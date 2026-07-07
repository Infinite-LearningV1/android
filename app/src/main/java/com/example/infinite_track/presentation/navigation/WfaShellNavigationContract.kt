package com.example.infinite_track.presentation.navigation

import com.example.infinite_track.R

/**
 * Source-of-truth for the main shell bottom bar after INF-224.
 *
 * The primary tabs are Home | History | WFA | Profile. The WFA tab is a
 * read-oriented booking/request history and status surface; WFA request creation,
 * check-in, and check-out remain owned by the attendance flow.
 */
object WfaShellNavigationContract {
    private val hiddenRoutes = setOf(
        Screen.Attendance.route,
        Screen.EditProfile.route,
        Screen.ContactUs.route,
        Screen.PaySlip.route,
        Screen.MyDocument.route,
        Screen.DetailMyAttendance.route,
        Screen.DetailListTimeOff.route,
        Screen.TimeOffRequest.route,
        Screen.TimeOffReq.route,
        Screen.MyLeave.route,
        Screen.FAQ.route,
        Screen.FaceScanner.route,
        Screen.LocationSearch.route,
        Screen.WfaBooking.route
    )

    private val wfaSelectedRoutes = setOf(
        Screen.Wfa.route,
        Screen.DetailsMyBooking.route
    )

    fun staffItems(): List<NavigationItem> = listOf(
        NavigationItem(
            tittle = R.string.bottom_menu_home,
            selectedIcon = R.drawable.ic_menu_home_selected,
            unselectedIcon = R.drawable.ic_menu_home,
            screen = Screen.Home
        ),
        NavigationItem(
            tittle = R.string.bottom_menu_history,
            selectedIcon = R.drawable.ic_history_selected,
            unselectedIcon = R.drawable.ic_history,
            screen = Screen.History
        ),
        NavigationItem(
            tittle = R.string.bottom_menu_wfa,
            selectedIcon = R.drawable.ic_contact_selected,
            unselectedIcon = R.drawable.ic_contact,
            screen = Screen.Wfa
        ),
        NavigationItem(
            tittle = R.string.bottom_menu_profile,
            selectedIcon = R.drawable.ic_profile_selected,
            unselectedIcon = R.drawable.ic_profile,
            screen = Screen.Profile
        )
    )

    fun internshipItems(): List<NavigationItem> = staffItems()

    fun shouldShowBottomBar(route: String?): Boolean = route !in hiddenRoutes

    fun isSelected(item: NavigationItem, currentRoute: String?): Boolean {
        return when (item.screen) {
            Screen.History -> currentRoute == Screen.History.route || currentRoute == Screen.HistoryFlow.route || currentRoute == Screen.DetailMyAttendance.route
            Screen.Profile -> currentRoute == Screen.Profile.route || currentRoute == Screen.ProfileFlow.route || currentRoute == Screen.EditProfile.route || currentRoute == Screen.Contact.route || currentRoute == Screen.ContactUs.route || currentRoute == Screen.PaySlip.route || currentRoute == Screen.MyDocument.route
            Screen.Wfa -> currentRoute in wfaSelectedRoutes
            else -> currentRoute == item.screen.route
        }
    }
}
