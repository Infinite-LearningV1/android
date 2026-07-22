package com.example.infinite_track.presentation.screen.attendance.permission

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.infinite_track.domain.model.attendance.permission.AttendanceAccess
import com.example.infinite_track.domain.model.attendance.permission.AttendanceAccessStatus
import com.example.infinite_track.domain.model.attendance.permission.AttendancePermissionNextAction
import com.example.infinite_track.domain.model.attendance.permission.AttendancePermissionReadiness
import com.example.infinite_track.domain.model.attendance.permission.AttendancePermissionRequestOutcome
import com.example.infinite_track.domain.use_case.attendance.permission.ObserveAttendancePermissionReadinessUseCase
import com.example.infinite_track.domain.use_case.attendance.permission.RefreshAttendancePermissionReadinessUseCase
import com.example.infinite_track.domain.use_case.attendance.permission.ResolveNextAttendancePermissionActionUseCase
import com.example.infinite_track.presentation.design.tokens.InfiniteSemantic
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch

@HiltViewModel
class AttendancePermissionReadinessViewModel @Inject constructor(
    private val observeReadiness: ObserveAttendancePermissionReadinessUseCase,
    private val refreshReadiness: RefreshAttendancePermissionReadinessUseCase,
    private val resolveNextAction: ResolveNextAttendancePermissionActionUseCase,
    private val uiMapper: AttendancePermissionReadinessUiMapper
) : ViewModel() {
    private val _uiState = MutableStateFlow(AttendancePermissionReadinessUiState())
    val uiState: StateFlow<AttendancePermissionReadinessUiState> = _uiState.asStateFlow()

    private val _effects = MutableSharedFlow<AttendancePermissionReadinessEffect>(extraBufferCapacity = 1)
    val effects: SharedFlow<AttendancePermissionReadinessEffect> = _effects.asSharedFlow()

    private var latestReadiness: AttendancePermissionReadiness? = null
    private var lastTrustworthyReadiness: AttendancePermissionReadiness? = null
    private val requestOutcomes = mutableMapOf<AttendanceAccess, AttendancePermissionRequestOutcome>()
    private var refreshJob: Job? = null
    private var inFlightAction: AttendancePermissionReadinessEffect? = null
    private var navigationPending = false
    private var activeFeedbackId: String? = null

    init {
        viewModelScope.launch {
            observeReadiness().collect(::onReadinessObserved)
        }
        refresh()
    }

    fun onEvent(event: AttendancePermissionReadinessEvent) {
        when (event) {
            AttendancePermissionReadinessEvent.ScreenResumed,
            AttendancePermissionReadinessEvent.ReturnedFromSettings,
            AttendancePermissionReadinessEvent.RetryRefresh -> {
                clearNativeAction()
                refresh()
            }
            AttendancePermissionReadinessEvent.PrimaryActionClicked -> onPrimaryActionClicked()
            is AttendancePermissionReadinessEvent.PermissionItemClicked -> onItemClicked(event.access)
            is AttendancePermissionReadinessEvent.PermissionResultReceived -> {
                clearNativeAction()
                if (event.outcome != AttendancePermissionRequestOutcome.GRANTED) {
                    requestOutcomes[event.access] = event.outcome
                } else {
                    requestOutcomes.remove(event.access)
                }
                render()
                refresh()
            }
            is AttendancePermissionReadinessEvent.SettingsLaunchFailed -> {
                clearNativeAction()
                showSettingsLaunchFailure(event.destination)
            }
            is AttendancePermissionReadinessEvent.SnackbarFinished -> {
                if (activeFeedbackId == event.feedbackId) activeFeedbackId = null
            }
            is AttendancePermissionReadinessEvent.SnackbarActionClicked -> {
                if (activeFeedbackId == event.feedbackId) {
                    activeFeedbackId = null
                    when (event.action) {
                        AttendancePermissionFeedbackAction.RETRY_REFRESH -> refresh()
                        AttendancePermissionFeedbackAction.OPEN_APPLICATION_SETTINGS -> Unit
                    }
                }
            }
            AttendancePermissionReadinessEvent.NavigationHandled -> {
                navigationPending = false
                clearNativeAction()
            }
        }
    }

    private fun onReadinessObserved(readiness: AttendancePermissionReadiness) {
        latestReadiness = readiness
        readiness.entries.forEach { entry ->
            if (entry.status == AttendanceAccessStatus.READY || entry.status == AttendanceAccessStatus.NOT_REQUIRED_ON_DEVICE) {
                requestOutcomes.remove(entry.access)
            }
        }
        if (readiness.inspectionIssues.none { it.blocksManualAttendance }) {
            lastTrustworthyReadiness = readiness
        }
        val effective = effectiveReadiness() ?: return
        val next = resolveNextAction.forPrimary(effective)
        if (inFlightAction != null && !isActionApplicable(inFlightAction!!, next)) clearNativeAction()
        if (!effective.canEnterAttendance) navigationPending = false
        render()
    }

    private fun onPrimaryActionClicked() {
        val readiness = effectiveReadiness() ?: return
        when (val action = resolveNextAction.forPrimary(readiness)) {
            AttendancePermissionNextAction.ContinueToWorkMode -> {
                if (!navigationPending && inFlightAction == null) {
                    navigationPending = true
                    emit(AttendancePermissionReadinessEffect.NavigateToWorkMode, trackInFlight = true)
                }
            }
            AttendancePermissionNextAction.RetryRefresh -> refresh()
            else -> emitAction(action)
        }
    }

    private fun onItemClicked(access: AttendanceAccess) {
        val readiness = effectiveReadiness() ?: return
        emitAction(resolveNextAction.forAccess(readiness, access))
    }

    private fun emitAction(action: AttendancePermissionNextAction) {
        val effect = when (action) {
            is AttendancePermissionNextAction.RequestPermission -> when (action.access) {
                AttendanceAccess.PRECISE_LOCATION -> AttendancePermissionReadinessEffect.RequestPreciseLocation
                AttendanceAccess.CAMERA -> AttendancePermissionReadinessEffect.RequestCamera
                AttendanceAccess.NOTIFICATION -> AttendancePermissionReadinessEffect.RequestNotification
                AttendanceAccess.BACKGROUND_LOCATION -> AttendancePermissionReadinessEffect.RequestBackgroundLocation
                AttendanceAccess.DEVICE_LOCATION -> null
            }
            is AttendancePermissionNextAction.OpenApplicationSettings ->
                AttendancePermissionReadinessEffect.OpenApplicationSettings(action.access)
            AttendancePermissionNextAction.OpenDeviceLocationSettings -> AttendancePermissionReadinessEffect.OpenDeviceLocationSettings
            AttendancePermissionNextAction.RetryRefresh -> {
                refresh(); null
            }
            AttendancePermissionNextAction.ContinueToWorkMode,
            AttendancePermissionNextAction.None -> null
        }
        if (effect != null && inFlightAction == null) emit(effect, trackInFlight = true)
    }

    private fun refresh() {
        if (refreshJob?.isActive == true) return
        render(isRefreshing = true)
        refreshJob = viewModelScope.launch {
            try {
                refreshReadiness()
            } finally {
                render(isRefreshing = false)
            }
        }
    }

    private fun effectiveReadiness(): AttendancePermissionReadiness? =
        latestReadiness?.applyingRequestOutcomes(requestOutcomes)

    private fun render(isRefreshing: Boolean = refreshJob?.isActive == true) {
        val current = effectiveReadiness() ?: return
        val next = resolveNextAction.forPrimary(current)
        val hasRequiredInspectionFailure = current.inspectionIssues.any { it.blocksManualAttendance }
        if (hasRequiredInspectionFailure && lastTrustworthyReadiness != null) {
            val stale = uiMapper.map(lastTrustworthyReadiness!!, next, isRefreshing)
            val failure = uiMapper.map(current, next, isRefreshing)
            _uiState.value = stale.copy(
                isRefreshing = isRefreshing,
                canContinue = false,
                primaryActionLabel = failure.primaryActionLabel,
                primaryActionEnabled = failure.primaryActionEnabled,
                contextualGuidance = failure.contextualGuidance,
                recoverableFailure = failure.recoverableFailure
            )
        } else {
            _uiState.value = uiMapper.map(current, next, isRefreshing)
        }
    }

    private fun isActionApplicable(
        effect: AttendancePermissionReadinessEffect,
        next: AttendancePermissionNextAction
    ): Boolean = when (effect) {
        AttendancePermissionReadinessEffect.RequestPreciseLocation ->
            next == AttendancePermissionNextAction.RequestPermission(AttendanceAccess.PRECISE_LOCATION)
        AttendancePermissionReadinessEffect.RequestCamera ->
            next == AttendancePermissionNextAction.RequestPermission(AttendanceAccess.CAMERA)
        AttendancePermissionReadinessEffect.RequestNotification ->
            next == AttendancePermissionNextAction.RequestPermission(AttendanceAccess.NOTIFICATION)
        AttendancePermissionReadinessEffect.RequestBackgroundLocation ->
            next == AttendancePermissionNextAction.RequestPermission(AttendanceAccess.BACKGROUND_LOCATION)
        is AttendancePermissionReadinessEffect.OpenApplicationSettings ->
            next == AttendancePermissionNextAction.OpenApplicationSettings(effect.access)
        AttendancePermissionReadinessEffect.OpenDeviceLocationSettings ->
            next == AttendancePermissionNextAction.OpenDeviceLocationSettings
        AttendancePermissionReadinessEffect.NavigateToWorkMode ->
            next == AttendancePermissionNextAction.ContinueToWorkMode
        is AttendancePermissionReadinessEffect.ShowSnackbar -> true
    }

    private fun clearNativeAction() {
        inFlightAction = null
    }

    private fun emit(effect: AttendancePermissionReadinessEffect, trackInFlight: Boolean = false) {
        if (trackInFlight) inFlightAction = effect
        viewModelScope.launch { _effects.emit(effect) }
    }

    private fun showSettingsLaunchFailure(destination: AttendanceSettingsDestination) {
        val id = "settings-launch-failed-${destination.name.lowercase()}"
        if (activeFeedbackId == id) return
        activeFeedbackId = id
        emit(
            AttendancePermissionReadinessEffect.ShowSnackbar(
                AttendancePermissionFeedback(
                    id = id,
                    message = "Pengaturan tidak dapat dibuka. Silakan coba lagi.",
                    semantic = InfiniteSemantic.Error,
                    duration = AttendanceFeedbackDuration.LONG
                )
            )
        )
    }
}
