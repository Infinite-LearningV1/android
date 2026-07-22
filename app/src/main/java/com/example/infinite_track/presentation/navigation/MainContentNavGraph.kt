package com.example.infinite_track.presentation.navigation

import androidx.camera.core.ExperimentalGetImage
import androidx.compose.runtime.remember
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.composable
import androidx.navigation.compose.navigation
import androidx.navigation.navArgument
import com.example.infinite_track.data.soucre.dummy.dummyTimeOff
import com.example.infinite_track.presentation.screen.attendance.AttendanceScreen
import com.example.infinite_track.presentation.screen.attendance.booking.WfaBookingScreen
import com.example.infinite_track.presentation.screen.attendance.permission.AttendancePermissionReadinessRoute
import com.example.infinite_track.presentation.screen.attendance.booking.WfaBookingViewModel
import com.example.infinite_track.presentation.screen.attendance.face.FaceScannerScreen
import com.example.infinite_track.presentation.screen.attendance.search.LocationSearchScreen
import com.example.infinite_track.presentation.screen.contact.ContactScreen
import com.example.infinite_track.presentation.screen.contact.ContactsViewModel
import com.example.infinite_track.presentation.screen.history.HistoryScreen
import com.example.infinite_track.presentation.screen.history.HistoryViewModel
import com.example.infinite_track.presentation.screen.home.HomeScreen
import com.example.infinite_track.presentation.screen.home.HomeViewModel
import com.example.infinite_track.presentation.screen.home.details.DetailsMyAttendance
import com.example.infinite_track.presentation.screen.home.details.DetailsMyBooking
import com.example.infinite_track.presentation.screen.home.service.CompanyServiceComingSoonScreen
import com.example.infinite_track.presentation.screen.leave_request.my_leave.MyLeave
import com.example.infinite_track.presentation.screen.leave_request.timeOff.TimeOffScreen
import com.example.infinite_track.presentation.screen.profile.ProfileScreen
import com.example.infinite_track.presentation.screen.profile.details.about.AboutScreen
import com.example.infinite_track.presentation.screen.profile.details.contactUs.ContactUsScreen
import com.example.infinite_track.presentation.screen.profile.details.edit_profile.EditProfile
import com.example.infinite_track.presentation.screen.profile.details.my_document.MyDocumentScreen
import com.example.infinite_track.presentation.screen.profile.details.pay_slip.PaySlipScreen
import com.example.infinite_track.presentation.screen.wfa.WfaHistoryScreen
import com.example.infinite_track.utils.safeNavigate

@ExperimentalGetImage
fun NavGraphBuilder.mainContentNavGraph(
    navController: NavHostController,
    rootNavController: NavHostController
) {
    // Home Screen
    composable(Screen.Home.route) {
        val homeViewModel: HomeViewModel = hiltViewModel()

        HomeScreen(
            viewModel = homeViewModel,
            navigateAttendance = { navController.safeNavigate(Screen.AttendancePermissionReadiness.route) },
            navigateTimeOffRequest = { navController.safeNavigate(Screen.TimeOffRequest.route) },
            navigateListMyAttendance = { navController.safeNavigate(Screen.History.route) },
            navigateServiceComingSoon = { navController.safeNavigate(Screen.CompanyServiceComingSoon.route) }
        )
    }

    // Contact Screen
    composable(Screen.Contact.route) {
        val contactsViewModel: ContactsViewModel = hiltViewModel()
        ContactScreen(
            onBackClick = { navController.popBackStack() },
            viewModel = contactsViewModel
        )
    }

    // History Feature Flow with Shared ViewModel
    navigation(
        startDestination = Screen.History.route,
        route = Screen.HistoryFlow.route
    ) {
        composable(Screen.History.route) { entry ->
            // Get parent backstack entry to share ViewModel between screens in this flow
            val parentEntry = remember(entry) {
                navController.getBackStackEntry(Screen.HistoryFlow.route)
            }

            // Use the shared ViewModel instance
            val historyViewModel: HistoryViewModel = hiltViewModel(parentEntry)

            HistoryScreen(
                viewModel = historyViewModel
            )
        }

        composable(Screen.DetailMyAttendance.route) { entry ->
            // Get parent backstack entry to share ViewModel between screens in this flow
            val parentEntry = remember(entry) {
                navController.getBackStackEntry(Screen.HistoryFlow.route)
            }

            // Use the shared ViewModel instance
            val historyViewModel: HistoryViewModel = hiltViewModel(parentEntry)

            DetailsMyAttendance(
                viewModel = historyViewModel,
                onBackClick = { navController.popBackStack() }
            )
        }
    }

    composable(Screen.Wfa.route) {
        WfaHistoryScreen()
    }

    composable(Screen.CompanyServiceComingSoon.route) {
        CompanyServiceComingSoonScreen(
            onBackClick = { navController.popBackStack() }
        )
    }

    // Profile Feature Flow
    navigation(
        startDestination = Screen.Profile.route,
        route = Screen.ProfileFlow.route
    ) {
        composable(Screen.Profile.route) {
            ProfileScreen(
                navigateToEditProfile = { navController.navigateToProfileDetail(Screen.EditProfile.route) },
                navigateToContactUs = { navController.navigateToProfileDetail(Screen.ContactUs.route) },
                navigateToContacts = { navController.navigate(Screen.Contact.route) },
                navigateToMyDocument = { navController.navigateToProfileDetail(Screen.MyDocument.route) },
                navigateToPaySlip = { navController.navigateToProfileDetail(Screen.PaySlip.route) },
                navigateToAbout = { navController.navigateToProfileDetail(Screen.About.route) },
                navHostController = navController,
                rootNavController = rootNavController
            )
        }

        composable(Screen.EditProfile.route) {
            EditProfile(
                onBackClick = { navController.navigateBackToProfile() }
            )
        }

        composable(Screen.ContactUs.route) {
            ContactUsScreen(
                onBackClick = { navController.navigateBackToProfile() }
            )
        }

        composable(Screen.PaySlip.route) {
            PaySlipScreen(
                onBackClick = { navController.navigateBackToProfile() }
            )
        }

        composable(Screen.MyDocument.route) {
            MyDocumentScreen(
                onBackClick = { navController.navigateBackToProfile() }
            )
        }

        composable(Screen.About.route) {
            AboutScreen(
                onBackClick = { navController.navigateBackToProfile() }
            )
        }
    }

    // Attendance permission readiness gate
    composable(Screen.AttendancePermissionReadiness.route) {
        AttendancePermissionReadinessRoute(
            onBackClick = { navController.popBackStack() },
            onContinueToWorkMode = { navController.safeNavigate(Screen.Attendance.route) }
        )
    }

    // Attendance Screen
    composable(Screen.Attendance.route) {
        AttendanceScreen(navController = navController)
    }

    // Location Search Screen - untuk pencarian lokasi attendance
    composable(Screen.LocationSearch.route) {
        LocationSearchScreen(
            navController = navController,
            onLocationSelected = {
                // Handle location selection - bisa dikembangkan untuk menyimpan lokasi terpilih
                // Untuk sementara, kembali ke AttendanceScreen
                navController.navigateUp()
            }
        )
    }

    // Time Off Related Screens
    composable(Screen.TimeOffRequest.route) {
        TimeOffScreen(
            cards = dummyTimeOff,
            onBackClick = { navController.popBackStack() }
        )
    }

    composable(Screen.TimeOffReq.route) {
        TimeOffScreen(
            cards = dummyTimeOff,
            onBackClick = { navController.popBackStack() }
        )
    }

    // My Leave Screen
    composable(Screen.MyLeave.route) {
        MyLeave(
            onBackClick = { navController.popBackStack() }
        )
    }

    // Details My Booking Screen - moved from AppNavGraph to MainContentNavGraph
    composable(Screen.DetailsMyBooking.route) {
        DetailsMyBooking(
            viewModel = hiltViewModel(),
            onBackClick = {
                navController.popBackStack()
            }
        )
    }

    // WFA Booking Screen
    composable(
        route = Screen.WfaBooking.route,
        arguments = listOf(
            navArgument("latitude") { type = NavType.FloatType },
            navArgument("longitude") { type = NavType.FloatType }
        )
    ) { backStackEntry ->
        val viewModel: WfaBookingViewModel = hiltViewModel()
        WfaBookingScreen(
            viewModel = viewModel,
            navController = navController
        )
    }

    // Face Scanner Screen - MOVED from AppNavGraph to MainContentNavGraph
    composable(
        route = Screen.FaceScanner.route,
        arguments = listOf(
            navArgument("action") {
                type = NavType.StringType
                defaultValue = "checkin"
            }
        )
    ) { backStackEntry ->
        val action = backStackEntry.arguments?.getString("action") ?: "checkin"

        FaceScannerScreen(
            action = action,
            navController = navController
        )
    }

    // Note: Commented out screens like Attendance, LeaveRequest, and FAQ
    // should be implemented here if needed
}

private fun NavHostController.navigateToProfileDetail(route: String) {
    navigate(route) {
        launchSingleTop = true
    }
}

private fun NavHostController.navigateBackToProfile() {
    if (!popBackStack(Screen.Profile.route, inclusive = false)) {
        navigate(Screen.Profile.route) {
            popUpTo(Screen.ProfileFlow.route) {
                inclusive = false
            }
            launchSingleTop = true
        }
    }
}
