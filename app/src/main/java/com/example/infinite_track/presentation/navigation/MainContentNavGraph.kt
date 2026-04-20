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
import com.example.infinite_track.presentation.screen.leave_request.my_leave.MyLeave
import com.example.infinite_track.presentation.screen.leave_request.timeOff.TimeOffScreen
import com.example.infinite_track.presentation.screen.profile.ProfileScreen
import com.example.infinite_track.presentation.screen.profile.details.contactUs.ContactUsScreen
import com.example.infinite_track.presentation.screen.profile.details.edit_profile.EditProfile
import com.example.infinite_track.presentation.screen.profile.details.my_document.MyDocumentScreen
import com.example.infinite_track.presentation.screen.profile.details.pay_slip.PaySlipScreen
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
            navigateAttendance = { navController.safeNavigate(Screen.Attendance.route) },
            navigateTimeOffRequest = { navController.safeNavigate(Screen.TimeOffRequest.route) },
            navigateListMyAttendance = { navController.safeNavigate(Screen.DetailMyAttendance.route) },
            navigateToBookingHistory = { navController.safeNavigate(Screen.DetailsMyBooking.route) }
        )
    }

    // Contact Screen
    composable(Screen.Contact.route) {
        val contactsViewModel: ContactsViewModel = hiltViewModel()
        ContactScreen(viewModel = contactsViewModel)
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

    // Profile Feature Flow
    navigation(
        startDestination = Screen.Profile.route,
        route = Screen.ProfileFlow.route
    ) {
        composable(Screen.Profile.route) {
            ProfileScreen(
                navigateToEditProfile = { navController.safeNavigate(Screen.EditProfile.route) },
                navigateToContactUs = { navController.safeNavigate(Screen.ContactUs.route) },
                navigateToMyDocument = { navController.safeNavigate(Screen.MyDocument.route) },
                navigateToPaySlip = { navController.safeNavigate(Screen.PaySlip.route) },
                navHostController = navController,
                rootNavController = rootNavController
            )
        }

        composable(Screen.EditProfile.route) {
            EditProfile(
                onBackClick = { navController.popBackStack() }
            )
        }
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

    // Contact Us Screen
    composable(Screen.ContactUs.route) {
        ContactUsScreen(
            onBackClick = { navController.popBackStack() }
        )
    }

    // PaySlip and MyDocument screens - these are standalone screens
    composable(Screen.PaySlip.route) {
        PaySlipScreen(
            onBackClick = { navController.popBackStack() }
        )
    }

    composable(Screen.MyDocument.route) {
        MyDocumentScreen(
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
