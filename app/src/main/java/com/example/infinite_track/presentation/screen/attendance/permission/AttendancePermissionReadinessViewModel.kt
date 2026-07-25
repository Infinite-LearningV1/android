package com.example.infinite_track.presentation.screen.attendance.permission

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.infinite_track.domain.model.attendance.permission.AttendanceAccess
import com.example.infinite_track.domain.model.attendance.permission.AttendanceAccessStatus
import com.example.infinite_track.domain.model.attendance.permission.AttendanceAccessRequirement
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
import kotlinx.coroutines.flow.first
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

    private val _runtimeReconciliationRequest = MutableStateFlow<RuntimeReconciliationRequest?>(null)
    val runtimeReconciliationRequest: StateFlow<RuntimeReconciliationRequest?> =
        _runtimeReconciliationRequest.asStateFlow()

    private var latestReadiness: AttendancePermissionReadiness? = null
    private var lastTrustworthyReadiness: AttendancePermissionReadiness? = null
    private val requestOutcomes = mutableMapOf<AttendanceAccess, AttendancePermissionRequestOutcome>()
    private var refreshJob: Job? = null
    private var isRefreshing = false
    private var initialRefreshCompleted = false
    private var runtimeReconciliationRequested = false
    private var lastRuntimeReconcileReadiness: AttendancePermissionReadiness? = null
    private var nextRuntimeReconciliationToken = 1L
    private var inFlightAction: AttendancePermissionReadinessEffect? = null
    private var activeFeedback: AttendancePermissionFeedback? = null

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
                refresh(requestRuntimeReconciliation = true)
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
                refresh(
                    requestRuntimeReconciliation = event.access.isGeofenceRuntimeRelevant()
                )
            }
            is AttendancePermissionReadinessEvent.SettingsLaunchFailed -> {
                clearNativeAction()
                showSettingsLaunchFailure(event.destination)
            }
            is AttendancePermissionReadinessEvent.SnackbarFinished -> {
                if (activeFeedback?.id == event.feedbackId) activeFeedback = null
            }
            is AttendancePermissionReadinessEvent.SnackbarActionClicked -> {
                if (activeFeedback?.id == event.feedbackId && activeFeedback?.action == event.action) {
                    activeFeedback = null
                    when (event.action) {
                        AttendancePermissionFeedbackAction.RETRY_REFRESH ->
                            refresh(requestRuntimeReconciliation = true)
                        AttendancePermissionFeedbackAction.OPEN_APPLICATION_SETTINGS -> Unit
                    }
                }
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
        if (inFlightAction != null && !isActionApplicable(inFlightAction!!, effective)) clearNativeAction()
        render()
    }

    private fun onPrimaryActionClicked() {
        val readiness = effectiveReadiness() ?: return
        when (val action = resolveNextAction.forPrimary(readiness)) {
            AttendancePermissionNextAction.ContinueToWorkMode -> {
                if (inFlightAction == null) {
                    emit(AttendancePermissionReadinessEffect.ClosePermissionPanel)
                }
            }
            AttendancePermissionNextAction.RetryRefresh -> refresh()
            else -> emitAction(action)
        }
    }

    private fun onItemClicked(access: AttendanceAccess) {
        val readiness = effectiveReadiness() ?: return
        val entry = readiness.entryOf(access)
        if (
            access.requirement == AttendanceAccessRequirement.OPTIONAL &&
            entry?.status == AttendanceAccessStatus.READY
        ) {
            if (inFlightAction == null) {
                emit(
                    AttendancePermissionReadinessEffect.OpenApplicationSettings(access),
                    trackInFlight = true
                )
            }
        } else {
            emitAction(resolveNextAction.forAccess(readiness, access))
        }
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

    private fun refresh(requestRuntimeReconciliation: Boolean = false) {
        if (requestRuntimeReconciliation) runtimeReconciliationRequested = true
        if (refreshJob?.isActive == true) return
        isRefreshing = true
        refreshJob = viewModelScope.launch {
            var refreshedReadiness: AttendancePermissionReadiness? = null
            render()
            try {
                refreshReadiness()
                refreshedReadiness = observeReadiness().first()
                onReadinessObserved(refreshedReadiness)
            } finally {
                isRefreshing = false
                render()
                refreshedReadiness?.let(::requestRuntimeReconciliationIfNeeded)
            }
        }
    }

    fun onRuntimeReconciliationHandled(token: Long) {
        if (_runtimeReconciliationRequest.value?.token == token) {
            _runtimeReconciliationRequest.value = null
        }
    }

    private fun requestRuntimeReconciliationIfNeeded(refreshedReadiness: AttendancePermissionReadiness) {
        val currentReadiness = refreshedReadiness.applyingRequestOutcomes(requestOutcomes)
        if (!initialRefreshCompleted) {
            initialRefreshCompleted = true
            lastRuntimeReconcileReadiness = currentReadiness
            runtimeReconciliationRequested = false
            return
        }

        val shouldReconcile = runtimeReconciliationRequested &&
            currentReadiness != null &&
            currentReadiness != lastRuntimeReconcileReadiness
        runtimeReconciliationRequested = false
        if (shouldReconcile) {
            lastRuntimeReconcileReadiness = currentReadiness
            _runtimeReconciliationRequest.value = RuntimeReconciliationRequest(
                token = nextRuntimeReconciliationToken++
            )
        }
    }

    private fun AttendanceAccess.isGeofenceRuntimeRelevant(): Boolean = when (this) {
        AttendanceAccess.PRECISE_LOCATION,
        AttendanceAccess.BACKGROUND_LOCATION,
        AttendanceAccess.DEVICE_LOCATION,
        AttendanceAccess.NOTIFICATION -> true
        AttendanceAccess.CAMERA -> false
    }

    private fun effectiveReadiness(): AttendancePermissionReadiness? =
        latestReadiness?.applyingRequestOutcomes(requestOutcomes)

    private fun render(isRefreshing: Boolean = this.isRefreshing) {
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
        readiness: AttendancePermissionReadiness
    ): Boolean = when (effect) {
        AttendancePermissionReadinessEffect.RequestPreciseLocation ->
            resolveNextAction.forAccess(readiness, AttendanceAccess.PRECISE_LOCATION) == AttendancePermissionNextAction.RequestPermission(AttendanceAccess.PRECISE_LOCATION)
        AttendancePermissionReadinessEffect.RequestCamera ->
            resolveNextAction.forAccess(readiness, AttendanceAccess.CAMERA) == AttendancePermissionNextAction.RequestPermission(AttendanceAccess.CAMERA)
        AttendancePermissionReadinessEffect.RequestNotification ->
            resolveNextAction.forAccess(readiness, AttendanceAccess.NOTIFICATION) == AttendancePermissionNextAction.RequestPermission(AttendanceAccess.NOTIFICATION)
        AttendancePermissionReadinessEffect.RequestBackgroundLocation ->
            resolveNextAction.forAccess(readiness, AttendanceAccess.BACKGROUND_LOCATION) == AttendancePermissionNextAction.RequestPermission(AttendanceAccess.BACKGROUND_LOCATION)
        is AttendancePermissionReadinessEffect.OpenApplicationSettings ->
            (
                effect.access.requirement == AttendanceAccessRequirement.OPTIONAL &&
                    readiness.entryOf(effect.access)?.status == AttendanceAccessStatus.READY
                ) ||
                resolveNextAction.forAccess(readiness, effect.access) ==
                AttendancePermissionNextAction.OpenApplicationSettings(effect.access)
        AttendancePermissionReadinessEffect.OpenDeviceLocationSettings ->
            resolveNextAction.forAccess(readiness, AttendanceAccess.DEVICE_LOCATION) == AttendancePermissionNextAction.OpenDeviceLocationSettings
        AttendancePermissionReadinessEffect.ClosePermissionPanel ->
            resolveNextAction.forPrimary(readiness) == AttendancePermissionNextAction.ContinueToWorkMode
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
        if (activeFeedback?.id == id) return
        val feedback = AttendancePermissionFeedback(
                    id = id,
                    message = "Pengaturan tidak dapat dibuka. Silakan coba lagi.",
                    semantic = InfiniteSemantic.Error,
                    duration = AttendanceFeedbackDuration.LONG
                )
        activeFeedback = feedback
        emit(AttendancePermissionReadinessEffect.ShowSnackbar(feedback))
    }
}
