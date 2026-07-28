package com.example.infinite_track.presentation.navigation

import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.composable
import androidx.navigation.compose.navigation
import androidx.navigation.navArgument
import com.example.infinite_track.presentation.screen.attendance.wfa_request.WfaRequestEffect
import com.example.infinite_track.presentation.screen.attendance.wfa_request.WfaRequestFormScreen
import com.example.infinite_track.presentation.screen.attendance.wfa_request.WfaRequestFlowController
import com.example.infinite_track.presentation.screen.attendance.wfa_request.WfaRequestResultScreen
import com.example.infinite_track.presentation.screen.attendance.wfa_request.WfaRequestReviewScreen
import com.example.infinite_track.presentation.screen.attendance.wfa_request.WfaRequestUiState
import com.example.infinite_track.presentation.screen.attendance.wfa_request.WfaRequestViewModel
import kotlinx.coroutines.flow.collect

fun NavGraphBuilder.wfaRequestNavGraph(
    navController: NavHostController,
    controllerFactory: @Composable (NavBackStackEntry) -> WfaRequestFlowController = { parentEntry ->
        hiltViewModel<WfaRequestViewModel>(parentEntry)
    }
) {
    navigation(
        startDestination = Screen.WfaRequestForm.route,
        route = Screen.WfaRequestFlow.route,
        arguments = listOf(
            navArgument("latitude") { type = NavType.StringType },
            navArgument("longitude") { type = NavType.StringType }
        )
    ) {
        composable(Screen.WfaRequestForm.route) { entry ->
            WfaRequestRoute(entry, navController, controllerFactory) { state, viewModel ->
                WfaRequestFormScreen(
                    uiState = state,
                    onEvent = viewModel::onEvent,
                    onBack = { navController.popBackStack() }
                )
            }
        }
        composable(Screen.WfaRequestReview.route) { entry ->
            WfaRequestRoute(entry, navController, controllerFactory) { state, viewModel ->
                BackHandler {
                    if (state.phase != com.example.infinite_track.presentation.screen.attendance.wfa_request.WfaRequestPhase.Submitting) {
                        viewModel.onEvent(com.example.infinite_track.presentation.screen.attendance.wfa_request.WfaRequestEvent.EditClicked)
                    }
                }
                WfaRequestReviewScreen(
                    uiState = state,
                    onEvent = viewModel::onEvent,
                    onBack = { viewModel.onEvent(com.example.infinite_track.presentation.screen.attendance.wfa_request.WfaRequestEvent.EditClicked) }
                )
            }
        }
        composable(Screen.WfaRequestResult.route) { entry ->
            WfaRequestRoute(entry, navController, controllerFactory) { state, viewModel ->
                BackHandler {
                    if (state.phase != com.example.infinite_track.presentation.screen.attendance.wfa_request.WfaRequestPhase.Submitting) {
                        navController.returnToAttendance()
                    }
                }
                WfaRequestResultScreen(
                    uiState = state,
                    onEvent = viewModel::onEvent,
                    onDone = { navController.returnToAttendance() },
                    onHome = { navController.returnToHome() },
                    onBack = { navController.returnToAttendance() }
                )
            }
        }
    }
}

@Composable
private fun WfaRequestRoute(
    entry: NavBackStackEntry,
    navController: NavHostController,
    controllerFactory: @Composable (NavBackStackEntry) -> WfaRequestFlowController,
    content: @Composable (WfaRequestUiState, WfaRequestFlowController) -> Unit
) {
    val parentEntry = remember(entry) {
        navController.getBackStackEntry(Screen.WfaRequestFlow.route)
    }
    val viewModel = controllerFactory(parentEntry)
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(viewModel, entry) {
        viewModel.effects.collect { effect ->
            when (effect) {
                WfaRequestEffect.OpenReview -> navController.navigate(Screen.WfaRequestReview.route) {
                    launchSingleTop = true
                }
                WfaRequestEffect.OpenResult -> navController.navigate(Screen.WfaRequestResult.route) {
                    launchSingleTop = true
                }
                WfaRequestEffect.ReturnToForm -> {
                    if (!navController.popBackStack(Screen.WfaRequestForm.route, inclusive = false)) {
                        navController.navigate(Screen.WfaRequestForm.route) { launchSingleTop = true }
                    }
                }
                WfaRequestEffect.ReturnToAttendance -> navController.returnToAttendance()
                WfaRequestEffect.ReturnHome -> navController.returnToHome()
                is WfaRequestEffect.Announce -> Unit
            }
        }
    }
    content(uiState, viewModel)
}

private fun NavHostController.returnToAttendance() {
    if (!popBackStack(Screen.Attendance.route, inclusive = false)) {
        navigate(Screen.Attendance.route) {
            popUpTo(Screen.WfaRequestFlow.route) { inclusive = true }
            launchSingleTop = true
        }
    }
}

private fun NavHostController.returnToHome() {
    if (!popBackStack(Screen.Home.route, inclusive = false)) {
        navigate(Screen.Home.route) {
            popUpTo(Screen.WfaRequestFlow.route) { inclusive = true }
            launchSingleTop = true
        }
    }
}
