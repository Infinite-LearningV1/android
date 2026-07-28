package com.example.infinite_track.presentation.screen.attendance.wfa_request

import androidx.lifecycle.SavedStateHandle
import com.example.infinite_track.domain.model.auth.LoginCredentials
import com.example.infinite_track.domain.model.auth.UserModel
import com.example.infinite_track.domain.model.booking.BookingHistoryPage
import com.example.infinite_track.domain.model.booking.SubmitWfaRequestCommand
import com.example.infinite_track.domain.model.booking.WfaRequestConfig
import com.example.infinite_track.domain.model.booking.WfaRequestConfigResult
import com.example.infinite_track.domain.model.booking.WfaRequestFailure
import com.example.infinite_track.domain.model.booking.WfaRequestReason
import com.example.infinite_track.domain.model.booking.WfaRequestResult
import com.example.infinite_track.domain.model.location.AddressResolutionResult
import com.example.infinite_track.domain.model.location.GeoCoordinate
import com.example.infinite_track.domain.model.location.ResolvedAddress
import com.example.infinite_track.domain.repository.AuthRepository
import com.example.infinite_track.domain.repository.AuthRefreshResult
import com.example.infinite_track.domain.repository.BookingRepository
import com.example.infinite_track.domain.repository.ProfileSyncResult
import com.example.infinite_track.domain.repository.location.AddressResolver
import com.example.infinite_track.domain.use_case.auth.GetLoggedInUserUseCase
import com.example.infinite_track.domain.use_case.booking.LoadWfaRequestConfigUseCase
import com.example.infinite_track.domain.use_case.booking.SubmitWfaRequestUseCase
import com.example.infinite_track.domain.use_case.booking.ValidateWfaRequestDraftUseCase
import com.example.infinite_track.domain.use_case.location.ReverseGeocodeUseCase
import com.example.infinite_track.testing.MainDispatcherRule
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertSame
import org.junit.Rule
import org.junit.Test
import java.time.LocalDate

class WfaRequestViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun `initial load reads employee location and config once`() = runTest {
        val repository = WfaRequestViewModelRepositoryFake()
        val authRepository = WfaRequestAuthRepositoryFake()
        val viewModel = createViewModel(repository, authRepository)

        advanceUntilIdle()

        assertEquals(WfaRequestPhase.Editing, viewModel.uiState.value.phase)
        assertEquals("Dina", viewModel.uiState.value.employee?.fullName)
        assertEquals("Infinity Hub", viewModel.uiState.value.location?.displayName)
        assertEquals(100, viewModel.uiState.value.config?.radiusMeters)
        assertEquals(1, authRepository.userFlowReads)
        assertEquals(1, repository.configCalls)
    }

    @Test
    fun `invalid review stays editing with field errors and no effect`() = runTest {
        val viewModel = createViewModel(WfaRequestViewModelRepositoryFake())
        advanceUntilIdle()

        viewModel.onEvent(WfaRequestEvent.ReviewClicked)

        assertEquals(WfaRequestPhase.Editing, viewModel.uiState.value.phase)
        assertNotNull(viewModel.uiState.value.fieldErrors.scheduleDate)
        assertNotNull(viewModel.uiState.value.fieldErrors.reason)
    }

    @Test
    fun `valid review preserves exact draft and emits OpenReview once`() = runTest {
        val viewModel = createViewModel(WfaRequestViewModelRepositoryFake())
        advanceUntilIdle()
        val date = LocalDate.of(2026, 8, 10)
        viewModel.onEvent(WfaRequestEvent.ScheduleDateChanged(date))
        viewModel.onEvent(WfaRequestEvent.ReasonSelected(1L))
        viewModel.onEvent(WfaRequestEvent.NotesChanged("Pertemuan project"))
        val effect = async(start = CoroutineStart.UNDISPATCHED) { viewModel.effects.first() }

        viewModel.onEvent(WfaRequestEvent.ReviewClicked)

        assertEquals(WfaRequestPhase.ReadyForReview, viewModel.uiState.value.phase)
        assertEquals(date, viewModel.uiState.value.draft.scheduleDate)
        assertEquals("Pertemuan project", viewModel.uiState.value.draft.notes)
        assertSame(WfaRequestEffect.OpenReview, effect.await())
    }

    @Test
    fun `repeated confirm while in flight submits once and failure preserves draft`() = runTest {
        val gate = CompletableDeferred<WfaRequestResult>()
        val repository = WfaRequestViewModelRepositoryFake(submitGate = gate)
        val viewModel = createViewModel(repository)
        advanceUntilIdle()
        val date = LocalDate.of(2026, 8, 10)
        viewModel.onEvent(WfaRequestEvent.ScheduleDateChanged(date))
        viewModel.onEvent(WfaRequestEvent.ReasonSelected(1L))
        viewModel.onEvent(WfaRequestEvent.NotesChanged("Tetap ada"))
        viewModel.onEvent(WfaRequestEvent.ReviewClicked)

        viewModel.onEvent(WfaRequestEvent.SubmitConfirmed)
        viewModel.onEvent(WfaRequestEvent.SubmitConfirmed)
        runCurrent()

        assertEquals(WfaRequestPhase.Submitting, viewModel.uiState.value.phase)
        assertEquals(1, repository.submitCalls)

        gate.complete(WfaRequestResult.Failure(WfaRequestFailure.NetworkUnavailable))
        advanceUntilIdle()

        assertEquals(WfaRequestPhase.Failure, viewModel.uiState.value.phase)
        assertEquals("Tetap ada", viewModel.uiState.value.draft.notes)
        assertEquals(WfaRequestFailure.NetworkUnavailable, viewModel.uiState.value.failure)
    }

    private fun createViewModel(
        repository: WfaRequestViewModelRepositoryFake,
        authRepository: WfaRequestAuthRepositoryFake = WfaRequestAuthRepositoryFake()
    ): WfaRequestViewModel = WfaRequestViewModel(
        loadConfig = LoadWfaRequestConfigUseCase(repository),
        validateDraft = ValidateWfaRequestDraftUseCase(),
        submitRequest = SubmitWfaRequestUseCase(repository),
        getLoggedInUser = GetLoggedInUserUseCase(authRepository),
        reverseGeocode = ReverseGeocodeUseCase(WfaRequestAddressResolverFake()),
        savedStateHandle = SavedStateHandle(
            mapOf("latitude" to "-0.9001", "longitude" to "119.877")
        )
    )
}

private class WfaRequestViewModelRepositoryFake(
    private val submitGate: CompletableDeferred<WfaRequestResult>? = null
) : BookingRepository {
    var configCalls = 0
    var submitCalls = 0

    override suspend fun getWfaRequestConfig(): WfaRequestConfigResult {
        configCalls += 1
        return WfaRequestConfigResult.Success(
            WfaRequestConfig(100, listOf(WfaRequestReason(1L, "Client meeting", false)))
        )
    }

    override suspend fun submitWfaRequest(command: SubmitWfaRequestCommand): WfaRequestResult {
        submitCalls += 1
        return submitGate?.await() ?: WfaRequestResult.Failure(WfaRequestFailure.Unknown)
    }

    override suspend fun getBookingHistory(
        status: String?, page: Int, limit: Int, sortBy: String, sortOrder: String
    ): Result<BookingHistoryPage> = error("Not used")

}

private class WfaRequestAuthRepositoryFake : AuthRepository {
    var userFlowReads = 0

    override fun getLoggedInUser(): Flow<UserModel?> {
        userFlowReads += 1
        return flowOf(
            UserModel(
                id = 1,
                fullName = "Dina",
                email = "dina@example.test",
                roleName = "Employee",
                positionName = null,
                programName = null,
                divisionName = "Mobile",
                nipNim = "EMP-1",
                phone = null,
                photoUrl = null,
                photoUpdatedAt = null,
                latitude = null,
                longitude = null,
                radius = null,
                locationDescription = null,
                locationCategoryName = null
            )
        )
    }

    override suspend fun refreshSession(): Result<AuthRefreshResult> = error("Not used")
    override suspend fun login(credentials: LoginCredentials): Result<UserModel> = error("Not used")
    override suspend fun syncUserProfile(): ProfileSyncResult = error("Not used")
    @Suppress("DEPRECATION")
    override suspend fun logout(): Result<Unit> = error("Not used")
    override suspend fun saveFaceEmbedding(userId: Int, embedding: ByteArray): Result<Unit> = error("Not used")
}

private class WfaRequestAddressResolverFake : AddressResolver {
    override suspend fun resolve(coordinate: GeoCoordinate): AddressResolutionResult =
        AddressResolutionResult.Resolved(
            ResolvedAddress(
                coordinate = coordinate,
                name = "Infinity Hub",
                formattedAddress = "Palu"
            )
        )
}
