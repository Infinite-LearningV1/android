package com.example.infinite_track.presentation.screen.attendance.wfa_request

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.infinite_track.domain.model.booking.SubmitWfaRequestCommand
import com.example.infinite_track.domain.model.booking.WfaCandidateLocation
import com.example.infinite_track.domain.model.booking.WfaRequestConfigResult
import com.example.infinite_track.domain.model.booking.WfaRequestDraft
import com.example.infinite_track.domain.model.booking.WfaRequestFailure
import com.example.infinite_track.domain.model.booking.WfaRequestFieldErrors
import com.example.infinite_track.domain.model.booking.WfaRequestResult
import com.example.infinite_track.domain.model.booking.WfaRequestValidationResult
import com.example.infinite_track.domain.model.location.AddressResolutionResult
import com.example.infinite_track.domain.model.location.GeoCoordinate
import com.example.infinite_track.domain.use_case.auth.GetLoggedInUserUseCase
import com.example.infinite_track.domain.use_case.booking.LoadWfaRequestConfigUseCase
import com.example.infinite_track.domain.use_case.booking.SubmitWfaRequestUseCase
import com.example.infinite_track.domain.use_case.booking.ValidateWfaRequestDraftUseCase
import com.example.infinite_track.domain.use_case.location.ReverseGeocodeUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.Locale
import javax.inject.Inject

@HiltViewModel
class WfaRequestViewModel @Inject constructor(
    private val loadConfig: LoadWfaRequestConfigUseCase,
    private val validateDraft: ValidateWfaRequestDraftUseCase,
    private val submitRequest: SubmitWfaRequestUseCase,
    private val getLoggedInUser: GetLoggedInUserUseCase,
    private val reverseGeocode: ReverseGeocodeUseCase,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val latitude = savedStateHandle.get<String>("latitude")?.toDoubleOrNull()
    private val longitude = savedStateHandle.get<String>("longitude")?.toDoubleOrNull()

    private val _uiState = MutableStateFlow(WfaRequestUiState())
    val uiState: StateFlow<WfaRequestUiState> = _uiState.asStateFlow()

    private val effectChannel = Channel<WfaRequestEffect>(Channel.BUFFERED)
    val effects = effectChannel.receiveAsFlow()

    private var lastValidatedCommand: SubmitWfaRequestCommand? = null

    init {
        loadInitialData()
    }

    fun onEvent(event: WfaRequestEvent) {
        when (event) {
            is WfaRequestEvent.ScheduleDateChanged -> mutateDraft {
                copy(scheduleDate = event.date)
            }
            is WfaRequestEvent.ReasonSelected -> mutateDraft {
                val selectedIsOther = _uiState.value.config?.reasons
                    ?.firstOrNull { it.id == event.reasonId }
                    ?.isOther == true
                copy(
                    reasonId = event.reasonId,
                    otherReasonText = if (selectedIsOther) otherReasonText else ""
                )
            }
            is WfaRequestEvent.OtherReasonChanged -> mutateDraft {
                copy(otherReasonText = event.value)
            }
            is WfaRequestEvent.NotesChanged -> mutateDraft {
                copy(notes = event.value)
            }
            WfaRequestEvent.ReviewClicked -> reviewDraft()
            WfaRequestEvent.EditClicked -> returnToEditing()
            WfaRequestEvent.SubmitConfirmed -> startSubmit(lastValidatedCommand)
            WfaRequestEvent.RetryConfigClicked -> retryConfig()
            WfaRequestEvent.RetrySubmitClicked -> startSubmit(lastValidatedCommand)
        }
    }

    private fun loadInitialData() {
        viewModelScope.launch {
            val coordinate = createCoordinateOrNull()
            if (coordinate == null) {
                _uiState.update {
                    it.copy(phase = WfaRequestPhase.Failure, failure = WfaRequestFailure.Unknown)
                }
                return@launch
            }

            val user = runCatching { getLoggedInUser().filterNotNull().first() }.getOrNull()
            val employee = user?.let {
                WfaEmployeeSummary(fullName = it.fullName, division = it.divisionName.orEmpty())
            }
            val location = resolveCandidate(coordinate)
            _uiState.update {
                it.copy(
                    employee = employee,
                    location = location,
                    draft = it.draft.copy(location = location)
                )
            }
            loadConfigIntoState()
        }
    }

    private suspend fun loadConfigIntoState() {
        when (val result = loadConfig()) {
            is WfaRequestConfigResult.Success -> _uiState.update {
                it.copy(
                    phase = WfaRequestPhase.Editing,
                    config = result.config,
                    failure = null
                )
            }
            is WfaRequestConfigResult.Failure -> _uiState.update {
                it.copy(
                    phase = WfaRequestPhase.Failure,
                    config = null,
                    failure = result.failure
                )
            }
        }
    }

    private fun retryConfig() {
        if (_uiState.value.config != null || _uiState.value.failure == null) return
        _uiState.update { it.copy(phase = WfaRequestPhase.Loading, failure = null) }
        viewModelScope.launch { loadConfigIntoState() }
    }

    private fun mutateDraft(transform: WfaRequestDraft.() -> WfaRequestDraft) {
        if (_uiState.value.phase != WfaRequestPhase.Editing) return
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

    private fun createCoordinateOrNull(): GeoCoordinate? {
        val lat = latitude ?: return null
        val lng = longitude ?: return null
        return runCatching { GeoCoordinate(lat, lng) }.getOrNull()
    }

    private suspend fun resolveCandidate(coordinate: GeoCoordinate): WfaCandidateLocation {
        return when (val result = runCatching { reverseGeocode(coordinate) }.getOrNull()) {
            is AddressResolutionResult.Resolved -> WfaCandidateLocation(
                latitude = coordinate.latitude,
                longitude = coordinate.longitude,
                displayName = result.address.name?.trim().orEmpty().ifBlank { "Lokasi WFA" },
                formattedAddress = result.address.formattedAddress
            )
            is AddressResolutionResult.CoordinateOnly -> coordinate.toCandidate()
            is AddressResolutionResult.Failed, null -> coordinate.toCandidate()
        }
    }

    private fun GeoCoordinate.toCandidate() = WfaCandidateLocation(
        latitude = latitude,
        longitude = longitude,
        displayName = "Lokasi WFA",
        formattedAddress = "Lat: %.6f, Lng: %.6f".format(Locale.US, latitude, longitude)
    )
}
