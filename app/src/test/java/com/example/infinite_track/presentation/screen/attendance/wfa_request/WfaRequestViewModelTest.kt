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
import com.example.infinite_track.domain.model.location.CurrentLocation
import com.example.infinite_track.domain.model.location.CurrentLocationResult
import com.example.infinite_track.domain.model.location.DistanceMeters
import com.example.infinite_track.domain.model.location.GeoCoordinate
import com.example.infinite_track.domain.model.wfa.WfaFacilityAvailability
import com.example.infinite_track.domain.model.wfa.WfaFacilityEvidence
import com.example.infinite_track.domain.model.wfa.WfaRecommendation
import com.example.infinite_track.domain.model.wfa.WfaRecommendationFailure
import com.example.infinite_track.domain.model.wfa.WfaRecommendationQuery
import com.example.infinite_track.domain.model.wfa.WfaRecommendationResult
import com.example.infinite_track.domain.model.wfa.WfaRecommendationStatus
import com.example.infinite_track.domain.repository.AuthRepository
import com.example.infinite_track.domain.repository.AuthRefreshResult
import com.example.infinite_track.domain.repository.BookingRepository
import com.example.infinite_track.domain.repository.ProfileSyncResult
import com.example.infinite_track.domain.repository.WfaRepository
import com.example.infinite_track.domain.repository.location.CurrentLocationRepository
import com.example.infinite_track.domain.use_case.auth.GetLoggedInUserUseCase
import com.example.infinite_track.domain.use_case.booking.LoadWfaRequestConfigUseCase
import com.example.infinite_track.domain.use_case.booking.SubmitWfaRequestUseCase
import com.example.infinite_track.domain.use_case.booking.ValidateWfaRequestDraftUseCase
import com.example.infinite_track.domain.use_case.location.GetCurrentLocationUseCase
import com.example.infinite_track.domain.use_case.wfa.GetWfaRecommendationsUseCase
import com.example.infinite_track.domain.validation.WfaScheduleDatePolicy
import com.example.infinite_track.testing.MainDispatcherRule
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withContext
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset

class WfaRequestViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val datePolicy = WfaScheduleDatePolicy.fixed(
        Clock.fixed(Instant.parse("2026-08-02T02:00:00Z"), ZoneOffset.UTC)
    )

    @Test
    fun `bootstrap defaults to tomorrow and loads recommendations automatically`() = runTest {
        val recommendations = FakeRecommendationRepository()
        val viewModel = createViewModel(recommendations = recommendations)

        advanceUntilIdle()

        assertEquals(LocalDate.of(2026, 8, 3), viewModel.uiState.value.draft.scheduleDate)
        assertEquals(1, recommendations.queries.size)
        assertEquals(LocalDate.of(2026, 8, 3), recommendations.queries.single().scheduleDate)
        assertTrue(viewModel.uiState.value.recommendationState is WfaRequestRecommendationState.Content)
        assertEquals(WfaRequestPhase.Editing, viewModel.uiState.value.phase)
    }

    @Test
    fun `empty saved state needs no route coordinates`() = runTest {
        val viewModel = createViewModel(savedStateHandle = SavedStateHandle())

        advanceUntilIdle()

        assertEquals(WfaRequestPhase.Editing, viewModel.uiState.value.phase)
        assertNull(viewModel.uiState.value.failure)
    }

    @Test
    fun `date change clears selected location and starts one new query`() = runTest {
        val recommendations = FakeRecommendationRepository()
        val viewModel = createViewModel(recommendations = recommendations)
        advanceUntilIdle()
        viewModel.onEvent(WfaRequestEvent.RecommendationSelected("place-1"))
        assertNotNull(viewModel.uiState.value.draft.location)

        viewModel.onEvent(WfaRequestEvent.ScheduleDateChanged(LocalDate.of(2026, 8, 4)))
        advanceUntilIdle()

        assertNull(viewModel.uiState.value.draft.location)
        assertEquals(2, recommendations.queries.size)
        assertEquals(LocalDate.of(2026, 8, 4), recommendations.queries.last().scheduleDate)
        assertNull((viewModel.uiState.value.recommendationState as WfaRequestRecommendationState.Content).selectedKey)
    }

    @Test
    fun `old deferred response cannot overwrite a newer date`() = runTest {
        val oldGate = CompletableDeferred<WfaRecommendationResult>()
        val recommendations = FakeRecommendationRepository { query ->
            if (query.scheduleDate == LocalDate.of(2026, 8, 3)) {
                withContext(NonCancellable) { oldGate.await() }
            } else {
                success(query.scheduleDate, listOf(recommendation("new-place")))
            }
        }
        val viewModel = createViewModel(recommendations = recommendations)
        runCurrent()

        viewModel.onEvent(WfaRequestEvent.ScheduleDateChanged(LocalDate.of(2026, 8, 4)))
        runCurrent()
        oldGate.complete(success(LocalDate.of(2026, 8, 3), listOf(recommendation("old-place"))))
        advanceUntilIdle()

        val content = viewModel.uiState.value.recommendationState as WfaRequestRecommendationState.Content
        assertEquals("new-place", content.recommendations.single().stableKey)
        assertEquals(LocalDate.of(2026, 8, 4), viewModel.uiState.value.draft.scheduleDate)
    }

    @Test
    fun `retry preserves reason and notes`() = runTest {
        var fail = true
        val recommendations = FakeRecommendationRepository { query ->
            if (fail) WfaRecommendationResult.Failure(WfaRecommendationFailure.NetworkUnavailable)
            else success(query.scheduleDate)
        }
        val viewModel = createViewModel(recommendations = recommendations)
        advanceUntilIdle()
        viewModel.onEvent(WfaRequestEvent.ReasonSelected(1L))
        viewModel.onEvent(WfaRequestEvent.NotesChanged("Tetap ada"))

        fail = false
        viewModel.onEvent(WfaRequestEvent.RetryRecommendationsClicked)
        advanceUntilIdle()

        assertEquals(1L, viewModel.uiState.value.draft.reasonId)
        assertEquals("Tetap ada", viewModel.uiState.value.draft.notes)
        assertTrue(viewModel.uiState.value.recommendationState is WfaRequestRecommendationState.Content)
    }

    @Test
    fun `current location failure is distinct and does not query repository`() = runTest {
        val recommendations = FakeRecommendationRepository()
        val viewModel = createViewModel(
            recommendations = recommendations,
            currentLocation = CurrentLocationResult.Failure.PermissionDenied
        )

        advanceUntilIdle()

        val failure = viewModel.uiState.value.recommendationState as WfaRequestRecommendationState.Failure
        assertSame(WfaRecommendationFailure.CurrentLocationUnavailable, failure.failure)
        assertTrue(recommendations.queries.isEmpty())
    }

    @Test
    fun `repeated identical load while in flight is guarded`() = runTest {
        val gate = CompletableDeferred<WfaRecommendationResult>()
        val recommendations = FakeRecommendationRepository { gate.await() }
        val viewModel = createViewModel(recommendations = recommendations)
        runCurrent()

        viewModel.onEvent(WfaRequestEvent.RetryRecommendationsClicked)
        runCurrent()

        assertEquals(1, recommendations.queries.size)
        assertTrue(viewModel.uiState.value.recommendationState is WfaRequestRecommendationState.Loading)
        gate.complete(success(LocalDate.of(2026, 8, 3)))
        advanceUntilIdle()
    }

    @Test
    fun `invalid date performs no repository call`() = runTest {
        val recommendations = FakeRecommendationRepository()
        val viewModel = createViewModel(recommendations = recommendations)
        advanceUntilIdle()

        viewModel.onEvent(WfaRequestEvent.ScheduleDateChanged(LocalDate.of(2026, 8, 2)))
        advanceUntilIdle()

        assertEquals(1, recommendations.queries.size)
        assertEquals(LocalDate.of(2026, 8, 3), viewModel.uiState.value.draft.scheduleDate)
        assertNotNull(viewModel.uiState.value.fieldErrors.scheduleDate)
    }

    @Test
    fun `non ranked candidate remains selectable`() = runTest {
        val recommendations = FakeRecommendationRepository {
            success(it.scheduleDate, listOf(recommendation(status = WfaRecommendationStatus.InsufficientFacilityData)))
        }
        val viewModel = createViewModel(recommendations = recommendations)
        advanceUntilIdle()

        viewModel.onEvent(WfaRequestEvent.RecommendationSelected("place-1"))

        assertEquals("Tempat place-1", viewModel.uiState.value.draft.location?.displayName)
        assertEquals("place-1", (viewModel.uiState.value.recommendationState as WfaRequestRecommendationState.Content).selectedKey)
    }

    @Test
    fun `valid review preserves exact draft and emits OpenReview once`() = runTest {
        val viewModel = createViewModel()
        advanceUntilIdle()
        viewModel.onEvent(WfaRequestEvent.RecommendationSelected("place-1"))
        viewModel.onEvent(WfaRequestEvent.ReasonSelected(1L))
        viewModel.onEvent(WfaRequestEvent.NotesChanged("Pertemuan project"))
        val effect = async(start = CoroutineStart.UNDISPATCHED) { viewModel.effects.first() }

        viewModel.onEvent(WfaRequestEvent.ReviewClicked)

        assertEquals(WfaRequestPhase.ReadyForReview, viewModel.uiState.value.phase)
        assertEquals("Pertemuan project", viewModel.uiState.value.draft.notes)
        assertSame(WfaRequestEffect.OpenReview, effect.await())
    }

    @Test
    fun `repeated confirm while in flight submits once and failure preserves draft`() = runTest {
        val gate = CompletableDeferred<WfaRequestResult>()
        val booking = FakeBookingRepository(submitGate = gate)
        val viewModel = createViewModel(booking = booking)
        advanceUntilIdle()
        viewModel.onEvent(WfaRequestEvent.RecommendationSelected("place-1"))
        viewModel.onEvent(WfaRequestEvent.ReasonSelected(1L))
        viewModel.onEvent(WfaRequestEvent.NotesChanged("Tetap ada"))
        viewModel.onEvent(WfaRequestEvent.ReviewClicked)

        viewModel.onEvent(WfaRequestEvent.SubmitConfirmed)
        viewModel.onEvent(WfaRequestEvent.SubmitConfirmed)
        runCurrent()
        assertEquals(1, booking.submitCalls)

        gate.complete(WfaRequestResult.Failure(WfaRequestFailure.NetworkUnavailable))
        advanceUntilIdle()
        assertEquals(WfaRequestPhase.Failure, viewModel.uiState.value.phase)
        assertEquals("Tetap ada", viewModel.uiState.value.draft.notes)
    }

    @Test
    fun `network config failure can retry into editing`() = runTest {
        val booking = FakeBookingRepository(initialConfigFailure = WfaRequestFailure.NetworkUnavailable)
        val viewModel = createViewModel(booking = booking)
        advanceUntilIdle()
        assertEquals(WfaRequestPhase.Failure, viewModel.uiState.value.phase)

        booking.makeConfigAvailable()
        viewModel.onEvent(WfaRequestEvent.RetryConfigClicked)
        advanceUntilIdle()

        assertEquals(WfaRequestPhase.Editing, viewModel.uiState.value.phase)
        assertEquals(2, booking.configCalls)
    }

    private fun createViewModel(
        booking: FakeBookingRepository = FakeBookingRepository(),
        recommendations: FakeRecommendationRepository = FakeRecommendationRepository(),
        currentLocation: CurrentLocationResult = successfulCurrentLocation(),
        savedStateHandle: SavedStateHandle = SavedStateHandle()
    ) = WfaRequestViewModel(
        loadConfig = LoadWfaRequestConfigUseCase(booking),
        validateDraft = ValidateWfaRequestDraftUseCase(datePolicy),
        submitRequest = SubmitWfaRequestUseCase(booking),
        getLoggedInUser = GetLoggedInUserUseCase(FakeAuthRepository()),
        getCurrentLocation = GetCurrentLocationUseCase(FakeCurrentLocationRepository(currentLocation)),
        getRecommendations = GetWfaRecommendationsUseCase(recommendations),
        datePolicy = datePolicy,
        savedStateHandle = savedStateHandle
    )
}

private class FakeRecommendationRepository(
    private val responder: suspend (WfaRecommendationQuery) -> WfaRecommendationResult = {
        success(it.scheduleDate)
    }
) : WfaRepository {
    val queries = mutableListOf<WfaRecommendationQuery>()

    override suspend fun getRecommendations(query: WfaRecommendationQuery): WfaRecommendationResult {
        queries += query
        return responder(query)
    }
}

private class FakeCurrentLocationRepository(
    private val result: CurrentLocationResult
) : CurrentLocationRepository {
    override suspend fun getCurrentLocation(): CurrentLocationResult = result
}

private class FakeBookingRepository(
    private val submitGate: CompletableDeferred<WfaRequestResult>? = null,
    initialConfigFailure: WfaRequestFailure? = null
) : BookingRepository {
    var configCalls = 0
    var submitCalls = 0
    private var configResult: WfaRequestConfigResult = initialConfigFailure?.let {
        WfaRequestConfigResult.Failure(it)
    } ?: availableConfig()

    fun makeConfigAvailable() { configResult = availableConfig() }

    override suspend fun getWfaRequestConfig(): WfaRequestConfigResult {
        configCalls += 1
        return configResult
    }

    override suspend fun submitWfaRequest(command: SubmitWfaRequestCommand): WfaRequestResult {
        submitCalls += 1
        return submitGate?.await() ?: WfaRequestResult.Failure(WfaRequestFailure.Unknown)
    }

    override suspend fun getBookingHistory(
        status: String?, page: Int, limit: Int, sortBy: String, sortOrder: String
    ): Result<BookingHistoryPage> = error("Not used")
}

private class FakeAuthRepository : AuthRepository {
    override fun getLoggedInUser(): Flow<UserModel?> = flowOf(
        UserModel(
            id = 1, fullName = "Dina", email = "dina@example.test", roleName = "Employee",
            positionName = null, programName = null, divisionName = "Mobile", nipNim = "EMP-1",
            phone = null, photoUrl = null, photoUpdatedAt = null, latitude = null, longitude = null,
            radius = null, locationDescription = null, locationCategoryName = null
        )
    )

    override suspend fun refreshSession(): Result<AuthRefreshResult> = error("Not used")
    override suspend fun login(credentials: LoginCredentials): Result<UserModel> = error("Not used")
    override suspend fun syncUserProfile(): ProfileSyncResult = error("Not used")
    @Suppress("DEPRECATION")
    override suspend fun logout(): Result<Unit> = error("Not used")
    override suspend fun saveFaceEmbedding(userId: Int, embedding: ByteArray): Result<Unit> = error("Not used")
}

private fun successfulCurrentLocation(): CurrentLocationResult = CurrentLocationResult.Success(
    CurrentLocation(
        coordinate = GeoCoordinate(-0.9001, 119.877), accuracy = null,
        capturedAtEpochMillis = 1L, provider = "test", isMock = false
    )
)

private fun availableConfig() = WfaRequestConfigResult.Success(
    WfaRequestConfig(100, listOf(WfaRequestReason(1L, "Client meeting", false)))
)

private fun success(
    date: LocalDate,
    recommendations: List<WfaRecommendation> = listOf(recommendation())
) = WfaRecommendationResult.Success(date, "Asia/Jakarta", recommendations, null)

private fun recommendation(
    key: String = "place-1",
    status: WfaRecommendationStatus = WfaRecommendationStatus.Ranked
) = WfaRecommendation(
    stableKey = key,
    placeId = key,
    name = "Tempat $key",
    address = "Palu",
    coordinate = GeoCoordinate(-0.901, 119.878),
    placeType = "cafe",
    distanceMeters = DistanceMeters(100.0),
    status = status,
    finalRank = if (status == WfaRecommendationStatus.Ranked) 1 else null,
    finalScore = if (status == WfaRecommendationStatus.Ranked) 88.0 else null,
    finalLabel = if (status == WfaRecommendationStatus.Ranked) "Direkomendasikan" else null,
    facilityScore = null,
    facilityConfidence = 0,
    facilities = WfaFacilityEvidence(
        internetAccess = WfaFacilityAvailability.UNKNOWN,
        openingHours = WfaFacilityAvailability.UNKNOWN,
        toilets = WfaFacilityAvailability.UNKNOWN,
        airConditioning = WfaFacilityAvailability.UNKNOWN,
        wheelchairAccessibility = WfaFacilityAvailability.UNKNOWN
    )
)
