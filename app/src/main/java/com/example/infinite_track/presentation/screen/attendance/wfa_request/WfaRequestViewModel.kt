package com.example.infinite_track.presentation.screen.attendance.wfa_request

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.infinite_track.domain.model.booking.SubmitWfaRequestCommand
import com.example.infinite_track.domain.model.booking.WfaCandidateLocation
import com.example.infinite_track.domain.model.booking.WfaRequestConfigResult
import com.example.infinite_track.domain.model.booking.WfaRequestDraft
import com.example.infinite_track.domain.model.booking.WfaRequestFailure
import com.example.infinite_track.domain.model.booking.WfaRequestFieldError
import com.example.infinite_track.domain.model.booking.WfaRequestFieldErrors
import com.example.infinite_track.domain.model.booking.WfaRequestResult
import com.example.infinite_track.domain.model.booking.WfaRequestValidationResult
import com.example.infinite_track.domain.model.location.CurrentLocationResult
import com.example.infinite_track.domain.model.location.GeoCoordinate
import com.example.infinite_track.domain.model.wfa.WfaRecommendation
import com.example.infinite_track.domain.model.wfa.WfaRecommendationFailure
import com.example.infinite_track.domain.model.wfa.WfaRecommendationQuery
import com.example.infinite_track.domain.model.wfa.WfaRecommendationResult
import com.example.infinite_track.domain.use_case.auth.GetLoggedInUserUseCase
import com.example.infinite_track.domain.use_case.booking.LoadWfaRequestConfigUseCase
import com.example.infinite_track.domain.use_case.booking.SubmitWfaRequestUseCase
import com.example.infinite_track.domain.use_case.booking.ValidateWfaRequestDraftUseCase
import com.example.infinite_track.domain.use_case.location.GetCurrentLocationUseCase
import com.example.infinite_track.domain.use_case.wfa.GetWfaRecommendationsUseCase
import com.example.infinite_track.domain.validation.WfaScheduleDatePolicy
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject

@HiltViewModel
class WfaRequestViewModel @Inject constructor(
    private val loadConfig: LoadWfaRequestConfigUseCase,
    private val validateDraft: ValidateWfaRequestDraftUseCase,
    private val submitRequest: SubmitWfaRequestUseCase,
    private val getLoggedInUser: GetLoggedInUserUseCase,
    private val getCurrentLocation: GetCurrentLocationUseCase,
    private val getRecommendations: GetWfaRecommendationsUseCase,
    private val datePolicy: WfaScheduleDatePolicy,
    @Suppress("UNUSED_PARAMETER") savedStateHandle: SavedStateHandle
) : ViewModel(), WfaRequestFlowController {

    private val _uiState = MutableStateFlow(WfaRequestUiState())
    override val uiState: StateFlow<WfaRequestUiState> = _uiState.asStateFlow()

    private val effectChannel = Channel<WfaRequestEffect>(Channel.BUFFERED)
    override val effects: Flow<WfaRequestEffect> = effectChannel.receiveAsFlow()

    private var lastValidatedCommand: SubmitWfaRequestCommand? = null
    private var configLoadFailed = false
    private var recommendationJob: Job? = null
    private var nextRecommendationRequestId = 0L
    private var activeQuery: WfaRecommendationQuery? = null
    private var activeScheduleDate: LocalDate? = null

    init {
        loadInitialData()
    }

    override fun onEvent(event: WfaRequestEvent) {
        when (event) {
            is WfaRequestEvent.ScheduleDateChanged -> onScheduleDateChanged(event.date)
            is WfaRequestEvent.ReasonSelected -> mutateDraft {
                val selectedIsOther = _uiState.value.config?.reasons
                    ?.firstOrNull { it.id == event.reasonId }
                    ?.isOther == true
                copy(
                    reasonId = event.reasonId,
                    otherReasonText = if (selectedIsOther) otherReasonText else ""
                )
            }
            is WfaRequestEvent.OtherReasonChanged -> mutateDraft { copy(otherReasonText = event.value) }
            is WfaRequestEvent.NotesChanged -> mutateDraft { copy(notes = event.value) }
            is WfaRequestEvent.RecommendationSelected -> selectRecommendation(event.stableKey)
            is WfaRequestEvent.ManualLocationSelected -> selectManualLocation(event.location)
            WfaRequestEvent.RetryRecommendationsClicked -> retryRecommendations()
            WfaRequestEvent.ReviewClicked -> reviewDraft()
            WfaRequestEvent.EditClicked -> returnToEditing()
            WfaRequestEvent.SubmitConfirmed -> startSubmit(lastValidatedCommand)
            WfaRequestEvent.RetryConfigClicked -> retryConfig()
            WfaRequestEvent.RetrySubmitClicked -> startSubmit(lastValidatedCommand)
        }
    }

    private fun loadInitialData() {
        val minimumDate = datePolicy.minimumDate()
        _uiState.update {
            it.copy(
                minimumScheduleDate = minimumDate,
                draft = it.draft.copy(scheduleDate = minimumDate)
            )
        }
        viewModelScope.launch {
            val user = runCatching { getLoggedInUser().filterNotNull().first() }.getOrNull()
            val employee = user?.let {
                WfaEmployeeSummary(fullName = it.fullName, division = it.divisionName.orEmpty())
            }
            if (employee == null) {
                _uiState.update {
                    it.copy(phase = WfaRequestPhase.Failure, failure = WfaRequestFailure.BootstrapUnavailable)
                }
                return@launch
            }
            _uiState.update { it.copy(employee = employee) }
            if (loadConfigIntoState()) loadRecommendations(force = true)
        }
    }

    private suspend fun loadConfigIntoState(): Boolean {
        return when (val result = loadConfig()) {
            is WfaRequestConfigResult.Success -> {
                configLoadFailed = false
                _uiState.update {
                    it.copy(phase = WfaRequestPhase.Editing, config = result.config, failure = null)
                }
                true
            }
            is WfaRequestConfigResult.Failure -> {
                configLoadFailed = true
                _uiState.update {
                    it.copy(phase = WfaRequestPhase.Failure, config = null, failure = result.failure)
                }
                false
            }
        }
    }

    private fun retryConfig() {
        if (!configLoadFailed || _uiState.value.config != null) return
        _uiState.update { it.copy(phase = WfaRequestPhase.Loading, failure = null) }
        viewModelScope.launch {
            if (loadConfigIntoState() &&
                _uiState.value.recommendationState is WfaRequestRecommendationState.Initializing
            ) {
                loadRecommendations(force = true)
            }
        }
    }

    private fun onScheduleDateChanged(date: LocalDate?) {
        if (_uiState.value.phase != WfaRequestPhase.Editing) return
        if (date == null || !datePolicy.isSelectable(date)) {
            _uiState.update {
                it.copy(
                    fieldErrors = it.fieldErrors.copy(
                        scheduleDate = WfaRequestFieldError.FUTURE_DATE_REQUIRED
                    )
                )
            }
            return
        }

        recommendationJob?.cancel()
        nextRecommendationRequestId += 1
        activeQuery = null
        activeScheduleDate = null
        lastValidatedCommand = null
        _uiState.update {
            it.copy(
                location = null,
                draft = it.draft.copy(scheduleDate = date, location = null),
                currentCoordinate = null,
                recommendationState = WfaRequestRecommendationState.Initializing,
                fieldErrors = it.fieldErrors.copy(scheduleDate = null, location = null),
                failure = null
            )
        }
        loadRecommendations(force = true)
    }

    private fun loadRecommendations(force: Boolean) {
        val date = _uiState.value.draft.scheduleDate ?: return
        if (!datePolicy.isSelectable(date)) return
        if (recommendationJob?.isActive == true && activeScheduleDate == date) return
        if (!force && activeQuery?.scheduleDate == date) return

        recommendationJob?.cancel()
        val requestId = ++nextRecommendationRequestId
        activeScheduleDate = date
        _uiState.update {
            it.copy(recommendationState = WfaRequestRecommendationState.Loading)
        }
        recommendationJob = viewModelScope.launch {
            val coordinate = try {
                when (val current = getCurrentLocation()) {
                    is CurrentLocationResult.Success -> current.location.coordinate
                    is CurrentLocationResult.Failure -> null
                }
            } catch (cancellation: CancellationException) {
                throw cancellation
            } catch (_: Throwable) {
                null
            }
            if (coordinate == null) {
                applyFailure(
                    requestId,
                    date,
                    WfaRecommendationFailure.CurrentLocationUnavailable
                )
                return@launch
            }

            val query = WfaRecommendationQuery(coordinate, date)
            activeQuery = query
            when (val result = getRecommendations(query)) {
                is WfaRecommendationResult.Success -> applySuccess(requestId, date, coordinate, result)
                is WfaRecommendationResult.Failure -> applyFailure(requestId, date, result.failure)
            }
        }
    }

    private fun applySuccess(
        requestId: Long,
        date: LocalDate,
        coordinate: GeoCoordinate,
        result: WfaRecommendationResult.Success
    ) {
        if (!isCurrentRequest(requestId, date)) return
        _uiState.update {
            it.copy(
                currentCoordinate = coordinate,
                recommendationState = if (result.recommendations.isEmpty()) {
                    WfaRequestRecommendationState.Empty
                } else {
                    WfaRequestRecommendationState.Content(result.recommendations)
                }
            )
        }
    }

    private fun applyFailure(
        requestId: Long,
        date: LocalDate,
        failure: WfaRecommendationFailure
    ) {
        if (!isCurrentRequest(requestId, date)) return
        _uiState.update {
            it.copy(
                currentCoordinate = null,
                recommendationState = WfaRequestRecommendationState.Failure(
                    failure = failure,
                    retryable = failure.isRetryable()
                )
            )
        }
    }

    private fun isCurrentRequest(requestId: Long, date: LocalDate): Boolean =
        requestId == nextRecommendationRequestId && _uiState.value.draft.scheduleDate == date

    private fun retryRecommendations() {
        if (_uiState.value.phase != WfaRequestPhase.Editing) return
        if (_uiState.value.recommendationState !is WfaRequestRecommendationState.Failure) return
        activeQuery = null
        loadRecommendations(force = true)
    }

    private fun selectRecommendation(stableKey: String) {
        if (_uiState.value.phase != WfaRequestPhase.Editing) return
        val content = _uiState.value.recommendationState as? WfaRequestRecommendationState.Content
            ?: return
        val recommendation = content.recommendations.firstOrNull { it.stableKey == stableKey }
            ?: return
        setSelectedLocation(
            location = recommendation.toCandidateLocation(),
            recommendationState = content.copy(selectedKey = stableKey)
        )
    }

    private fun selectManualLocation(location: WfaCandidateLocation) {
        if (_uiState.value.phase != WfaRequestPhase.Editing || !location.hasValidCoordinates) return
        val recommendationState = when (val current = _uiState.value.recommendationState) {
            is WfaRequestRecommendationState.Content -> current.copy(selectedKey = null)
            else -> current
        }
        setSelectedLocation(location, recommendationState)
    }

    private fun setSelectedLocation(
        location: WfaCandidateLocation,
        recommendationState: WfaRequestRecommendationState
    ) {
        lastValidatedCommand = null
        _uiState.update {
            it.copy(
                location = location,
                draft = it.draft.copy(location = location),
                recommendationState = recommendationState,
                fieldErrors = it.fieldErrors.copy(location = null),
                failure = null
            )
        }
    }

    private fun mutateDraft(transform: WfaRequestDraft.() -> WfaRequestDraft) {
        if (_uiState.value.phase != WfaRequestPhase.Editing) return
        lastValidatedCommand = null
        _uiState.update {
            it.copy(
                draft = it.draft.transform(),
                fieldErrors = WfaRequestFieldErrors(),
                failure = null
            )
        }
    }

    private fun reviewDraft() {
        val state = _uiState.value
        if (state.phase != WfaRequestPhase.Editing) return
        val config = state.config ?: return
        when (val result = validateDraft(state.draft, config)) {
            is WfaRequestValidationResult.Invalid -> _uiState.update {
                it.copy(fieldErrors = result.errors)
            }
            is WfaRequestValidationResult.Valid -> {
                lastValidatedCommand = result.command
                _uiState.update {
                    it.copy(
                        phase = WfaRequestPhase.ReadyForReview,
                        draft = it.draft.copy(
                            scheduleDate = result.command.scheduleDate,
                            reasonId = result.command.reasonId,
                            otherReasonText = result.command.otherReasonText.orEmpty(),
                            notes = result.command.notes.orEmpty(),
                            location = result.command.location
                        ),
                        fieldErrors = WfaRequestFieldErrors(),
                        failure = null
                    )
                }
                effectChannel.trySend(WfaRequestEffect.OpenReview)
            }
        }
    }

    private fun returnToEditing() {
        val phase = _uiState.value.phase
        if (phase !in setOf(
                WfaRequestPhase.ReadyForReview,
                WfaRequestPhase.Reviewing,
                WfaRequestPhase.Failure
            )
        ) return
        lastValidatedCommand = null
        _uiState.update {
            it.copy(
                phase = WfaRequestPhase.Editing,
                fieldErrors = WfaRequestFieldErrors(),
                failure = null,
                submitResult = null
            )
        }
        effectChannel.trySend(WfaRequestEffect.ReturnToForm)
    }

    private fun startSubmit(command: SubmitWfaRequestCommand?) {
        val state = _uiState.value
        val maySubmit = state.phase == WfaRequestPhase.ReadyForReview ||
            state.phase == WfaRequestPhase.Reviewing ||
            state.phase == WfaRequestPhase.Failure
        if (!maySubmit || command == null || state.phase == WfaRequestPhase.Submitting) return

        _uiState.update { it.copy(phase = WfaRequestPhase.Submitting, failure = null) }
        viewModelScope.launch {
            when (val result = submitRequest(command)) {
                is WfaRequestResult.Success -> _uiState.update {
                    it.copy(
                        phase = WfaRequestPhase.Success,
                        submitResult = result.request,
                        failure = null
                    )
                }
                is WfaRequestResult.Failure -> _uiState.update {
                    it.copy(
                        phase = WfaRequestPhase.Failure,
                        failure = result.failure,
                        submitResult = null
                    )
                }
            }
            effectChannel.send(WfaRequestEffect.OpenResult)
        }
    }
}

private fun WfaRecommendation.toCandidateLocation() = WfaCandidateLocation(
    latitude = coordinate.latitude,
    longitude = coordinate.longitude,
    displayName = name,
    formattedAddress = address
)

private fun WfaRecommendationFailure.isRetryable(): Boolean = when (this) {
    WfaRecommendationFailure.InvalidScheduleDate,
    WfaRecommendationFailure.DuplicateBooking -> false
    WfaRecommendationFailure.CurrentLocationUnavailable,
    WfaRecommendationFailure.NetworkUnavailable,
    WfaRecommendationFailure.ProviderUnavailable,
    WfaRecommendationFailure.ServerUnavailable,
    WfaRecommendationFailure.Unknown -> true
}

interface WfaRequestFlowController {
    val uiState: StateFlow<WfaRequestUiState>
    val effects: Flow<WfaRequestEffect>
    fun onEvent(event: WfaRequestEvent)
}
