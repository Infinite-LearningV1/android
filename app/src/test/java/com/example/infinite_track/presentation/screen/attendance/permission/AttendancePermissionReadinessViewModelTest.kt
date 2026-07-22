package com.example.infinite_track.presentation.screen.attendance.permission

import com.example.infinite_track.domain.model.attendance.permission.AttendanceAccess
import com.example.infinite_track.domain.model.attendance.permission.AttendanceAccessReadiness
import com.example.infinite_track.domain.model.attendance.permission.AttendanceAccessRecovery
import com.example.infinite_track.domain.model.attendance.permission.AttendanceAccessStatus
import com.example.infinite_track.domain.model.attendance.permission.AttendancePermissionFailure
import com.example.infinite_track.domain.model.attendance.permission.AttendancePermissionInspectionIssue
import com.example.infinite_track.domain.model.attendance.permission.AttendancePermissionReadiness
import com.example.infinite_track.domain.model.attendance.permission.AttendancePermissionRequestOutcome
import com.example.infinite_track.domain.repository.AttendancePermissionRepository
import com.example.infinite_track.domain.use_case.attendance.permission.ObserveAttendancePermissionReadinessUseCase
import com.example.infinite_track.domain.use_case.attendance.permission.RefreshAttendancePermissionReadinessUseCase
import com.example.infinite_track.domain.use_case.attendance.permission.ResolveNextAttendancePermissionActionUseCase
import com.example.infinite_track.testing.MainDispatcherRule
import kotlinx.coroutines.async
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class AttendancePermissionReadinessViewModelTest {
    @get:Rule val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun `initial refresh observes and maps readiness`() = runTest {
        val repository = FakeRepository(partialReadiness())
        val viewModel = viewModel(repository)

        advanceUntilIdle()

        assertEquals(1, repository.refreshCount)
        assertFalse(viewModel.uiState.value.isLoading)
        assertEquals(2, viewModel.uiState.value.requiredReadyCount)
        assertEquals("Lanjutkan Setup", viewModel.uiState.value.primaryActionLabel)
    }

    @Test
    fun `primary action follows required ordering and emits one native request`() = runTest {
        val viewModel = viewModel(FakeRepository(partialReadiness()))
        advanceUntilIdle()
        val effect = async(UnconfinedTestDispatcher(testScheduler)) { viewModel.effects.first() }

        viewModel.onEvent(AttendancePermissionReadinessEvent.PrimaryActionClicked)

        assertEquals(AttendancePermissionReadinessEffect.RequestCamera, effect.await())
    }

    @Test
    fun `optional item action emits its request without blocking manual attendance`() = runTest {
        val viewModel = viewModel(FakeRepository(allRequiredReady(notification = AttendanceAccessStatus.DEGRADED)))
        advanceUntilIdle()
        val effect = async(UnconfinedTestDispatcher(testScheduler)) { viewModel.effects.first() }

        viewModel.onEvent(AttendancePermissionReadinessEvent.PermissionItemClicked(AttendanceAccess.NOTIFICATION))

        assertEquals(AttendancePermissionReadinessEffect.RequestNotification, effect.await())
        assertTrue(viewModel.uiState.value.canContinue)
    }

    @Test
    fun `optional request guard survives unchanged observation and clears on callback`() = runTest {
        val repository = FakeRepository(allRequiredReady(notification = AttendanceAccessStatus.DEGRADED))
        val viewModel = viewModel(repository)
        advanceUntilIdle()
        val effects = Channel<AttendancePermissionReadinessEffect>(Channel.UNLIMITED)
        backgroundScope.launch { viewModel.effects.collect { effects.send(it) } }
        viewModel.onEvent(AttendancePermissionReadinessEvent.PermissionItemClicked(AttendanceAccess.NOTIFICATION))
        advanceUntilIdle()
        assertEquals(AttendancePermissionReadinessEffect.RequestNotification, effects.receive())
        repository.readiness.value = allRequiredReady(notification = AttendanceAccessStatus.DEGRADED)
        viewModel.onEvent(AttendancePermissionReadinessEvent.PermissionItemClicked(AttendanceAccess.NOTIFICATION))
        advanceUntilIdle()
        assertFalse(effects.tryReceive().isSuccess)
        viewModel.onEvent(AttendancePermissionReadinessEvent.PermissionResultReceived(AttendanceAccess.NOTIFICATION, AttendancePermissionRequestOutcome.DENIED))
        advanceUntilIdle()
        viewModel.onEvent(AttendancePermissionReadinessEvent.PermissionItemClicked(AttendanceAccess.NOTIFICATION))
        assertEquals(AttendancePermissionReadinessEffect.RequestNotification, effects.receive())
    }

    @Test
    fun `navigation handled permits a later CTA navigation`() = runTest {
        val viewModel = viewModel(FakeRepository(allRequiredReady()))
        advanceUntilIdle()
        val effects = Channel<AttendancePermissionReadinessEffect>(Channel.UNLIMITED)
        backgroundScope.launch { viewModel.effects.collect { effects.send(it) } }
        viewModel.onEvent(AttendancePermissionReadinessEvent.PrimaryActionClicked)
        advanceUntilIdle(); assertEquals(AttendancePermissionReadinessEffect.NavigateToWorkMode, effects.receive())
        viewModel.onEvent(AttendancePermissionReadinessEvent.NavigationHandled)
        viewModel.onEvent(AttendancePermissionReadinessEvent.PrimaryActionClicked)
        advanceUntilIdle(); assertEquals(AttendancePermissionReadinessEffect.NavigateToWorkMode, effects.receive())
    }

    @Test
    fun `blocking callback resets pending navigation after fresh OS readiness`() = runTest {
        val repository = FakeRepository(allRequiredReady())
        val viewModel = viewModel(repository); advanceUntilIdle()
        val effects = Channel<AttendancePermissionReadinessEffect>(Channel.UNLIMITED)
        backgroundScope.launch { viewModel.effects.collect { effects.send(it) } }
        viewModel.onEvent(AttendancePermissionReadinessEvent.PrimaryActionClicked); advanceUntilIdle()
        assertEquals(AttendancePermissionReadinessEffect.NavigateToWorkMode, effects.receive())
        viewModel.onEvent(AttendancePermissionReadinessEvent.PermissionResultReceived(AttendanceAccess.CAMERA, AttendancePermissionRequestOutcome.DENIED))
        repository.readiness.value = allRequiredReady(); advanceUntilIdle()
        viewModel.onEvent(AttendancePermissionReadinessEvent.PrimaryActionClicked); advanceUntilIdle()
        assertEquals(AttendancePermissionReadinessEffect.NavigateToWorkMode, effects.receive())
    }

    @Test
    fun `settings guard clears on return and launch failure`() = runTest {
        val viewModel = viewModel(FakeRepository(partialReadiness())); advanceUntilIdle()
        val effects = Channel<AttendancePermissionReadinessEffect>(Channel.UNLIMITED)
        backgroundScope.launch { viewModel.effects.collect { effects.send(it) } }
        viewModel.onEvent(AttendancePermissionReadinessEvent.PermissionResultReceived(AttendanceAccess.CAMERA, AttendancePermissionRequestOutcome.PERMANENTLY_DENIED))
        viewModel.onEvent(AttendancePermissionReadinessEvent.PrimaryActionClicked); advanceUntilIdle()
        assertEquals(AttendancePermissionReadinessEffect.OpenApplicationSettings(AttendanceAccess.CAMERA), effects.receive())
        viewModel.onEvent(AttendancePermissionReadinessEvent.ReturnedFromSettings)
        viewModel.onEvent(AttendancePermissionReadinessEvent.PrimaryActionClicked); advanceUntilIdle()
        assertEquals(AttendancePermissionReadinessEffect.OpenApplicationSettings(AttendanceAccess.CAMERA), effects.receive())
        viewModel.onEvent(AttendancePermissionReadinessEvent.SettingsLaunchFailed(AttendanceSettingsDestination.APPLICATION)); advanceUntilIdle()
        assertTrue(effects.receive() is AttendancePermissionReadinessEffect.ShowSnackbar)
        viewModel.onEvent(AttendancePermissionReadinessEvent.PrimaryActionClicked); advanceUntilIdle()
        assertEquals(AttendancePermissionReadinessEffect.OpenApplicationSettings(AttendanceAccess.CAMERA), effects.receive())
    }

    @Test
    fun `stale feedback events do not clear active feedback`() = runTest {
        val viewModel = viewModel(FakeRepository(partialReadiness())); advanceUntilIdle()
        val effects = Channel<AttendancePermissionReadinessEffect>(Channel.UNLIMITED)
        backgroundScope.launch { viewModel.effects.collect { effects.send(it) } }
        viewModel.onEvent(AttendancePermissionReadinessEvent.SettingsLaunchFailed(AttendanceSettingsDestination.APPLICATION)); advanceUntilIdle()
        val feedback = (effects.receive() as AttendancePermissionReadinessEffect.ShowSnackbar).feedback
        viewModel.onEvent(AttendancePermissionReadinessEvent.SnackbarActionClicked(feedback.id, AttendancePermissionFeedbackAction.RETRY_REFRESH))
        viewModel.onEvent(AttendancePermissionReadinessEvent.SnackbarFinished("wrong"))
        viewModel.onEvent(AttendancePermissionReadinessEvent.SettingsLaunchFailed(AttendanceSettingsDestination.APPLICATION)); advanceUntilIdle()
        assertFalse(effects.tryReceive().isSuccess)
        viewModel.onEvent(AttendancePermissionReadinessEvent.SnackbarFinished(feedback.id))
        viewModel.onEvent(AttendancePermissionReadinessEvent.SettingsLaunchFailed(AttendanceSettingsDestination.APPLICATION)); advanceUntilIdle()
        assertTrue(effects.receive() is AttendancePermissionReadinessEffect.ShowSnackbar)
    }

    @Test
    fun `permanent denial overlay opens application settings`() = runTest {
        val viewModel = viewModel(FakeRepository(partialReadiness()))
        advanceUntilIdle()
        viewModel.onEvent(AttendancePermissionReadinessEvent.PermissionResultReceived(
            AttendanceAccess.CAMERA, AttendancePermissionRequestOutcome.PERMANENTLY_DENIED
        ))
        val effect = async(UnconfinedTestDispatcher(testScheduler)) { viewModel.effects.first() }

        viewModel.onEvent(AttendancePermissionReadinessEvent.PrimaryActionClicked)

        assertEquals(
            AttendancePermissionReadinessEffect.OpenApplicationSettings(AttendanceAccess.CAMERA),
            effect.await()
        )
    }

    @Test
    fun `gps action opens device settings`() = runTest {
        val viewModel = viewModel(FakeRepository(allRequiredReady(device = AttendanceAccessStatus.DEVICE_LOCATION_DISABLED)))
        advanceUntilIdle()
        val effect = async(UnconfinedTestDispatcher(testScheduler)) { viewModel.effects.first() }

        viewModel.onEvent(AttendancePermissionReadinessEvent.PrimaryActionClicked)

        assertEquals(AttendancePermissionReadinessEffect.OpenDeviceLocationSettings, effect.await())
    }

    @Test
    fun `all required ready navigates only after one CTA and suppresses duplicate CTA`() = runTest {
        val viewModel = viewModel(FakeRepository(allRequiredReady()))
        advanceUntilIdle()
        val effects = Channel<AttendancePermissionReadinessEffect>(Channel.UNLIMITED)
        backgroundScope.launch { viewModel.effects.collect { effects.send(it) } }

        viewModel.onEvent(AttendancePermissionReadinessEvent.PrimaryActionClicked)
        advanceUntilIdle()
        assertEquals(AttendancePermissionReadinessEffect.NavigateToWorkMode, effects.receive())
        viewModel.onEvent(AttendancePermissionReadinessEvent.PrimaryActionClicked)
        advanceUntilIdle()

        assertFalse(effects.tryReceive().isSuccess)
    }

    @Test
    fun `observing ready state does not navigate and optional inspection issue remains non blocking`() = runTest {
        val repository = FakeRepository(allRequiredReady())
        val viewModel = viewModel(repository)
        advanceUntilIdle()
        val effects = Channel<AttendancePermissionReadinessEffect>(Channel.UNLIMITED)
        backgroundScope.launch { viewModel.effects.collect { effects.send(it) } }
        repository.readiness.value = allRequiredReady(issues = listOf(optionalIssue()))
        advanceUntilIdle()

        assertTrue(viewModel.uiState.value.canContinue)
        assertFalse(effects.tryReceive().isSuccess)
    }

    @Test
    fun `required inspection issue retries refresh and keeps last trustworthy content`() = runTest {
        val repository = FakeRepository(allRequiredReady())
        val viewModel = viewModel(repository)
        advanceUntilIdle()
        repository.readiness.value = readiness(
            camera = AttendanceAccessStatus.ACTION_REQUIRED,
            device = AttendanceAccessStatus.ACTION_REQUIRED,
            issues = listOf(requiredIssue())
        )
        advanceUntilIdle()

        viewModel.onEvent(AttendancePermissionReadinessEvent.PrimaryActionClicked)
        advanceUntilIdle()

        assertEquals(2, repository.refreshCount)
        assertEquals(3, viewModel.uiState.value.requiredReadyCount)
        assertEquals("Siap", viewModel.uiState.value.requiredItems[1].statusLabel)
        assertFalse(viewModel.uiState.value.canContinue)
        assertEquals("Coba lagi", viewModel.uiState.value.primaryActionLabel)
        assertTrue(viewModel.uiState.value.recoverableFailure != null)
    }

    @Test
    fun `callback settings return and retry trigger refresh while duplicate resume is suppressed`() = runTest {
        val repository = ControlledRepository(partialReadiness())
        val viewModel = viewModel(repository)
        advanceUntilIdle()
        assertEquals(1, repository.refreshCount)
        assertTrue(viewModel.uiState.value.isRefreshing)

        viewModel.onEvent(AttendancePermissionReadinessEvent.ScreenResumed)
        viewModel.onEvent(AttendancePermissionReadinessEvent.ScreenResumed)
        advanceUntilIdle()
        assertEquals(1, repository.refreshCount)
        repository.release(); advanceUntilIdle()
        assertFalse(viewModel.uiState.value.isRefreshing)

        viewModel.onEvent(AttendancePermissionReadinessEvent.PermissionResultReceived(
            AttendanceAccess.CAMERA, AttendancePermissionRequestOutcome.DENIED
        ))
        advanceUntilIdle(); assertEquals(2, repository.refreshCount)
        assertTrue(viewModel.uiState.value.isRefreshing)
        repository.release(); advanceUntilIdle(); assertFalse(viewModel.uiState.value.isRefreshing)
        viewModel.onEvent(AttendancePermissionReadinessEvent.ReturnedFromSettings)
        advanceUntilIdle(); assertEquals(3, repository.refreshCount)
        repository.release(); advanceUntilIdle(); assertFalse(viewModel.uiState.value.isRefreshing)
        viewModel.onEvent(AttendancePermissionReadinessEvent.RetryRefresh)
        advanceUntilIdle()
        assertEquals(4, repository.refreshCount)
        repository.release(); advanceUntilIdle(); assertFalse(viewModel.uiState.value.isRefreshing)
    }

    @Test
    fun `request evidence is non authoritative and clears when OS snapshot is ready or optional`() = runTest {
        val repository = FakeRepository(partialReadiness())
        val viewModel = viewModel(repository); advanceUntilIdle()
        viewModel.onEvent(AttendancePermissionReadinessEvent.PermissionResultReceived(AttendanceAccess.CAMERA, AttendancePermissionRequestOutcome.GRANTED))
        advanceUntilIdle(); assertFalse(viewModel.uiState.value.cameraGranted)
        viewModel.onEvent(AttendancePermissionReadinessEvent.PermissionResultReceived(AttendanceAccess.CAMERA, AttendancePermissionRequestOutcome.DENIED))
        advanceUntilIdle(); assertEquals("Izin ditolak", viewModel.uiState.value.requiredItems[1].statusLabel)
        repository.readiness.value = allRequiredReady(); advanceUntilIdle(); assertTrue(viewModel.uiState.value.cameraGranted)
        repository.readiness.value = allRequiredReady(notification = AttendanceAccessStatus.DEGRADED); advanceUntilIdle()
        viewModel.onEvent(AttendancePermissionReadinessEvent.PermissionResultReceived(AttendanceAccess.NOTIFICATION, AttendancePermissionRequestOutcome.DENIED))
        repository.readiness.value = allRequiredReady(notification = AttendanceAccessStatus.NOT_REQUIRED_ON_DEVICE); advanceUntilIdle()
        assertTrue(viewModel.uiState.value.canContinue)
        assertEquals("Tidak diperlukan di perangkat ini", viewModel.uiState.value.optionalItems[0].statusLabel)
    }

    @Test
    fun `optional inspection issue CTA navigates exactly once`() = runTest {
        val viewModel = viewModel(FakeRepository(allRequiredReady(issues = listOf(optionalIssue())))); advanceUntilIdle()
        val effects = Channel<AttendancePermissionReadinessEffect>(Channel.UNLIMITED)
        backgroundScope.launch { viewModel.effects.collect { effects.send(it) } }
        viewModel.onEvent(AttendancePermissionReadinessEvent.PrimaryActionClicked); advanceUntilIdle()
        assertEquals(AttendancePermissionReadinessEffect.NavigateToWorkMode, effects.receive())
        viewModel.onEvent(AttendancePermissionReadinessEvent.PrimaryActionClicked); advanceUntilIdle()
        assertFalse(effects.tryReceive().isSuccess)
    }

    @Test
    fun `settings failure emits long error feedback once until snackbar finishes`() = runTest {
        val viewModel = viewModel(FakeRepository(partialReadiness()))
        advanceUntilIdle()
        val first = async(UnconfinedTestDispatcher(testScheduler)) { viewModel.effects.first() }

        viewModel.onEvent(AttendancePermissionReadinessEvent.SettingsLaunchFailed(AttendanceSettingsDestination.APPLICATION))

        val feedback = (first.await() as AttendancePermissionReadinessEffect.ShowSnackbar).feedback
        assertEquals(AttendanceFeedbackDuration.LONG, feedback.duration)
        viewModel.onEvent(AttendancePermissionReadinessEvent.SettingsLaunchFailed(AttendanceSettingsDestination.APPLICATION))
        viewModel.onEvent(AttendancePermissionReadinessEvent.SnackbarFinished(feedback.id))
        val second = async(UnconfinedTestDispatcher(testScheduler)) { viewModel.effects.first() }
        viewModel.onEvent(AttendancePermissionReadinessEvent.SettingsLaunchFailed(AttendanceSettingsDestination.APPLICATION))
        assertTrue(second.await() is AttendancePermissionReadinessEffect.ShowSnackbar)
    }

    private fun viewModel(repository: AttendancePermissionRepository) = AttendancePermissionReadinessViewModel(
        ObserveAttendancePermissionReadinessUseCase(repository),
        RefreshAttendancePermissionReadinessUseCase(repository),
        ResolveNextAttendancePermissionActionUseCase(),
        AttendancePermissionReadinessUiMapper()
    )

    private class FakeRepository(initial: AttendancePermissionReadiness) : AttendancePermissionRepository {
        val readiness = MutableStateFlow(initial)
        var refreshCount = 0
        override fun observeReadiness() = readiness
        override suspend fun refreshReadiness() { refreshCount++ }
    }

    private class ControlledRepository(initial: AttendancePermissionReadiness) : AttendancePermissionRepository {
        val readiness = MutableStateFlow(initial)
        private val releases = Channel<Unit>(Channel.UNLIMITED)
        var refreshCount = 0
        override fun observeReadiness() = readiness
        override suspend fun refreshReadiness() { refreshCount++; releases.receive() }
        suspend fun release() { releases.send(Unit) }
    }

    private fun partialReadiness() = readiness(camera = AttendanceAccessStatus.ACTION_REQUIRED)
    private fun allRequiredReady(
        device: AttendanceAccessStatus = AttendanceAccessStatus.READY,
        notification: AttendanceAccessStatus = AttendanceAccessStatus.READY,
        issues: List<AttendancePermissionInspectionIssue> = emptyList()
    ) = readiness(device = device, notification = notification, issues = issues)

    private fun readiness(
        camera: AttendanceAccessStatus = AttendanceAccessStatus.READY,
        device: AttendanceAccessStatus = AttendanceAccessStatus.READY,
        notification: AttendanceAccessStatus = AttendanceAccessStatus.READY,
        issues: List<AttendancePermissionInspectionIssue> = emptyList()
    ) = AttendancePermissionReadiness(
        entries = listOf(
            entry(AttendanceAccess.PRECISE_LOCATION, AttendanceAccessStatus.READY),
            entry(AttendanceAccess.CAMERA, camera),
            entry(AttendanceAccess.DEVICE_LOCATION, device),
            entry(AttendanceAccess.NOTIFICATION, notification),
            entry(AttendanceAccess.BACKGROUND_LOCATION, AttendanceAccessStatus.READY)
        ),
        inspectionIssues = issues
    )

    private fun entry(access: AttendanceAccess, status: AttendanceAccessStatus) = AttendanceAccessReadiness(
        access = access,
        status = status,
        recovery = when (status) {
            AttendanceAccessStatus.ACTION_REQUIRED, AttendanceAccessStatus.DENIED, AttendanceAccessStatus.DEGRADED -> AttendanceAccessRecovery.REQUEST_PERMISSION
            AttendanceAccessStatus.PERMANENTLY_DENIED -> AttendanceAccessRecovery.OPEN_APPLICATION_SETTINGS
            AttendanceAccessStatus.DEVICE_LOCATION_DISABLED -> AttendanceAccessRecovery.OPEN_DEVICE_LOCATION_SETTINGS
            AttendanceAccessStatus.READY, AttendanceAccessStatus.NOT_REQUIRED_ON_DEVICE -> AttendanceAccessRecovery.NONE
        }
    )

    private fun requiredIssue() = AttendancePermissionInspectionIssue(
        AttendancePermissionFailure.PLATFORM_STATE_UNAVAILABLE, setOf(AttendanceAccess.CAMERA)
    )
    private fun optionalIssue() = AttendancePermissionInspectionIssue(
        AttendancePermissionFailure.PLATFORM_STATE_UNAVAILABLE, setOf(AttendanceAccess.NOTIFICATION)
    )
}
