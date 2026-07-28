package com.example.infinite_track.presentation.navigation

import androidx.compose.material3.Text
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.navigation.compose.ComposeNavigator
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.testing.TestNavHostController
import androidx.test.core.app.ApplicationProvider
import androidx.test.espresso.Espresso.pressBack
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.infinite_track.domain.model.booking.SubmittedWfaRequest
import com.example.infinite_track.domain.model.booking.WfaCandidateLocation
import com.example.infinite_track.domain.model.booking.WfaRequestConfig
import com.example.infinite_track.domain.model.booking.WfaRequestDraft
import com.example.infinite_track.domain.model.booking.WfaRequestReason
import com.example.infinite_track.domain.model.booking.WfaRequestStatus
import com.example.infinite_track.presentation.screen.attendance.wfa_request.WfaEmployeeSummary
import com.example.infinite_track.presentation.screen.attendance.wfa_request.WfaRequestEffect
import com.example.infinite_track.presentation.screen.attendance.wfa_request.WfaRequestEvent
import com.example.infinite_track.presentation.screen.attendance.wfa_request.WfaRequestFlowController
import com.example.infinite_track.presentation.screen.attendance.wfa_request.WfaRequestPhase
import com.example.infinite_track.presentation.screen.attendance.wfa_request.WfaRequestUiState
import com.example.infinite_track.presentation.theme.Infinite_TrackTheme
import java.time.LocalDate
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class WfaRequestNavigationTest {
    @get:Rule val composeRule = createComposeRule()

    @Test
    fun graphSharesControllerAndHandlesReviewBackResultAndExit() {
        val controller = FakeWfaRequestFlowController()
        val parentEntryIds = mutableListOf<String>()
        lateinit var navController: TestNavHostController
        composeRule.setContent {
            navController = TestNavHostController(ApplicationProvider.getApplicationContext()).apply {
                navigatorProvider.addNavigator(ComposeNavigator())
            }
            Infinite_TrackTheme {
                NavHost(navController, startDestination = Screen.Attendance.route) {
                    composable(Screen.Attendance.route) { Text("Attendance host") }
                    wfaRequestNavGraph(navController) { parentEntry ->
                        parentEntryIds += parentEntry.id
                        controller
                    }
                }
                LaunchedEffect(Unit) {
                    navController.navigate(Screen.WfaRequestFlow.createRoute(-0.9, 119.8))
                }
            }
        }

        composeRule.onNodeWithTag("wfaReviewAction").performScrollTo().performClick()
        composeRule.waitForIdle()
        assertEquals(Screen.WfaRequestReview.route, navController.currentDestination?.route)

        pressBack()
        composeRule.waitForIdle()
        assertEquals(Screen.WfaRequestForm.route, navController.currentDestination?.route)
        assertEquals(WfaRequestPhase.Editing, controller.uiState.value.phase)

        composeRule.onNodeWithTag("wfaReviewAction").performScrollTo().performClick()
        composeRule.onNodeWithTag("wfaConfirmAction").performScrollTo().performClick()
        composeRule.waitForIdle()
        assertEquals(Screen.WfaRequestResult.route, navController.currentDestination?.route)
        composeRule.onNodeWithText("ID booking: 77").assertIsDisplayed()
        assertEquals(1, parentEntryIds.distinct().size)

        composeRule.onNodeWithTag("wfaDoneAction").performScrollTo().performClick()
        composeRule.waitForIdle()
        assertEquals(Screen.Attendance.route, navController.currentDestination?.route)
        composeRule.onNodeWithText("Attendance host").assertIsDisplayed()
    }

    @Test
    fun reviewCloseReturnsToEditableFormAndPreservesGraphScopedDraft() {
        val controller = FakeWfaRequestFlowController()
        lateinit var navController: TestNavHostController
        composeRule.setContent {
            navController = TestNavHostController(ApplicationProvider.getApplicationContext()).apply {
                navigatorProvider.addNavigator(ComposeNavigator())
            }
            Infinite_TrackTheme {
                NavHost(navController, startDestination = Screen.Attendance.route) {
                    composable(Screen.Attendance.route) { Text("Attendance host") }
                    wfaRequestNavGraph(navController) { controller }
                }
                LaunchedEffect(Unit) {
                    navController.navigate(Screen.WfaRequestFlow.createRoute(-0.9, 119.8))
                }
            }
        }

        composeRule.onNodeWithTag("wfaReviewAction").performScrollTo().performClick()
        composeRule.waitForIdle()
        assertEquals(Screen.WfaRequestReview.route, navController.currentDestination?.route)

        composeRule.onNodeWithContentDescription("Tutup").performClick()
        composeRule.waitForIdle()
        assertEquals(Screen.WfaRequestForm.route, navController.currentDestination?.route)
        assertEquals(WfaRequestPhase.Editing, controller.uiState.value.phase)
        assertEquals("Butuh ruang tenang", controller.uiState.value.draft.notes)
        composeRule.onNodeWithText("Butuh ruang tenang").performScrollTo().assertIsDisplayed()
    }

    @Test
    fun resultCloseExitsToAttendanceWhenNotSubmitting() {
        val controller = FakeWfaRequestFlowController()
        lateinit var navController: TestNavHostController
        composeRule.setContent {
            navController = TestNavHostController(ApplicationProvider.getApplicationContext()).apply {
                navigatorProvider.addNavigator(ComposeNavigator())
            }
            Infinite_TrackTheme {
                NavHost(navController, startDestination = Screen.Attendance.route) {
                    composable(Screen.Attendance.route) { Text("Attendance host") }
                    wfaRequestNavGraph(navController) { controller }
                }
                LaunchedEffect(Unit) {
                    navController.navigate(Screen.WfaRequestFlow.createRoute(-0.9, 119.8))
                }
            }
        }

        composeRule.onNodeWithTag("wfaReviewAction").performScrollTo().performClick()
        composeRule.onNodeWithTag("wfaConfirmAction").performScrollTo().performClick()
        composeRule.waitForIdle()
        assertEquals(Screen.WfaRequestResult.route, navController.currentDestination?.route)

        composeRule.onNodeWithContentDescription("Tutup").performClick()
        composeRule.waitForIdle()
        assertEquals(Screen.Attendance.route, navController.currentDestination?.route)
        composeRule.onNodeWithText("Attendance host").assertIsDisplayed()
    }
}

private class FakeWfaRequestFlowController : WfaRequestFlowController {
    private val location = WfaCandidateLocation(-0.9, 119.8, "Kafe Taman", "Palu")
    private val mutableState = MutableStateFlow(
        WfaRequestUiState(
            phase = WfaRequestPhase.Editing,
            employee = WfaEmployeeSummary("Alya", "Product"),
            location = location,
            config = WfaRequestConfig(100, listOf(WfaRequestReason(1, "Client meeting", false))),
            draft = WfaRequestDraft(
                LocalDate.of(2026, 8, 4),
                1,
                "",
                "Butuh ruang tenang",
                location
            )
        )
    )
    override val uiState: StateFlow<WfaRequestUiState> = mutableState
    private val effectChannel = Channel<WfaRequestEffect>(Channel.BUFFERED)
    override val effects: Flow<WfaRequestEffect> = effectChannel.receiveAsFlow()

    override fun onEvent(event: WfaRequestEvent) {
        when (event) {
            WfaRequestEvent.ReviewClicked -> {
                mutableState.value = mutableState.value.copy(phase = WfaRequestPhase.ReadyForReview)
                effectChannel.trySend(WfaRequestEffect.OpenReview)
            }
            WfaRequestEvent.EditClicked -> {
                mutableState.value = mutableState.value.copy(phase = WfaRequestPhase.Editing)
                effectChannel.trySend(WfaRequestEffect.ReturnToForm)
            }
            WfaRequestEvent.SubmitConfirmed -> {
                mutableState.value = mutableState.value.copy(
                    phase = WfaRequestPhase.Success,
                    submitResult = SubmittedWfaRequest(
                        77, LocalDate.of(2026, 8, 4), WfaRequestStatus.PENDING,
                        location, "Client meeting", 100, null
                    )
                )
                effectChannel.trySend(WfaRequestEffect.OpenResult)
            }
            else -> Unit
        }
    }
}
