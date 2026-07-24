package com.example.infinite_track.presentation.screen.attendance

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.infinite_track.data.soucre.local.preferences.AttendancePreference
import com.example.infinite_track.domain.model.attendance.AttendancePreparationEligibility
import com.example.infinite_track.domain.model.attendance.AuthoritativeTargetLocation
import com.example.infinite_track.domain.model.attendance.Location
import com.example.infinite_track.domain.model.attendance.TargetLocationResolution
import com.example.infinite_track.domain.model.attendance.TargetRangeStatus
import com.example.infinite_track.domain.model.attendance.TargetRangeUnknownReason
import com.example.infinite_track.domain.model.attendance.TargetResolutionFailure
import com.example.infinite_track.domain.model.attendance.TodayStatus
import com.example.infinite_track.domain.model.attendance.WorkMode
import com.example.infinite_track.domain.model.auth.UserModel
import com.example.infinite_track.domain.model.location.LocationResult
import com.example.infinite_track.domain.model.location.GeoCoordinate
import com.example.infinite_track.domain.model.location.DistanceMeters
import com.example.infinite_track.domain.model.location.AddressResolutionResult
import com.example.infinite_track.domain.model.wfa.WfaRecommendation
import com.example.infinite_track.domain.model.wfa.WfaBookingForDate
import com.example.infinite_track.domain.use_case.attendance.CheckInUseCase
import com.example.infinite_track.domain.use_case.attendance.CheckOutUseCase
import com.example.infinite_track.domain.use_case.attendance.EvaluateAttendancePreparationUseCase
import com.example.infinite_track.domain.use_case.attendance.EvaluateTargetRangeUseCase
import com.example.infinite_track.domain.use_case.attendance.GetTodayStatusUseCase
import com.example.infinite_track.domain.use_case.attendance.ResolveAuthoritativeTargetLocationUseCase
import com.example.infinite_track.domain.use_case.auth.GetLoggedInUserUseCase
import com.example.infinite_track.domain.use_case.auth.RefreshAttendanceProfileUseCase
import com.example.infinite_track.domain.use_case.booking.ResolveTodayApprovedWfaBookingUseCase
import com.example.infinite_track.domain.use_case.booking.ResolveTodayWfaBookingStateUseCase
import com.example.infinite_track.domain.use_case.location.GetCurrentAddressUseCase
import com.example.infinite_track.domain.use_case.location.GetCurrentLocationUseCase
import com.example.infinite_track.domain.model.location.CurrentLocationResult
import com.example.infinite_track.domain.use_case.location.ReverseGeocodeUseCase
import com.example.infinite_track.domain.use_case.wfa.GetWfaRecommendationsUseCase
import com.example.infinite_track.presentation.geofencing.GeofenceManager
import com.example.infinite_track.presentation.geofencing.ReminderGeofenceCandidate
import com.example.infinite_track.presentation.navigation.Screen
import com.example.infinite_track.presentation.map.model.AttendanceMapCameraMoveOrigin
import com.example.infinite_track.presentation.map.model.MapCameraEffect
import com.example.infinite_track.presentation.screen.attendance.preparation.AttendancePreparationReducer
import com.example.infinite_track.presentation.screen.attendance.preparation.LatestSelectionGuard
import com.example.infinite_track.presentation.screen.attendance.preparation.SelectionRequestToken
import com.example.infinite_track.presentation.screen.attendance.preparation.WfaDiscoveryState
import com.example.infinite_track.presentation.screen.attendance.preparation.WfaMapSelectionEffect
import com.example.infinite_track.presentation.screen.attendance.preparation.WfaMapPickInteractionState
import com.example.infinite_track.utils.LocationPermissionHelper
import com.example.infinite_track.utils.UiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import java.util.Locale
import javax.inject.Inject

/**
 * Sealed class untuk merepresentasikan target navigasi
 */
sealed class NavigationTarget {
    data class FaceScanner(val intent: AttendanceActionIntent) : NavigationTarget()
    data class WfaBooking(val route: String) : NavigationTarget()
    data class LocationSearch(val params: String) : NavigationTarget()
}

/**
 * Simplified ViewModel that is fully reactive to geofence state
 * Removed manual GPS tracking and distance calculation logic
 * Uses geofence as the single source of truth for validation
 * UPDATED: Migrated to fully state-driven architecture
 */
@HiltViewModel
class AttendanceViewModel @Inject constructor(
    private val getTodayStatusUseCase: GetTodayStatusUseCase,
    private val getCurrentAddressUseCase: GetCurrentAddressUseCase,
    private val getCurrentLocationUseCase: GetCurrentLocationUseCase,
    private val getWfaRecommendationsUseCase: GetWfaRecommendationsUseCase,
    private val reverseGeocodeUseCase: ReverseGeocodeUseCase,
    private val attendancePreference: AttendancePreference,
    private val geofenceManager: GeofenceManager,
    private val getLoggedInUserUseCase: GetLoggedInUserUseCase,
    private val refreshAttendanceProfileUseCase: RefreshAttendanceProfileUseCase,
    private val resolveTodayApprovedWfaBookingUseCase: ResolveTodayApprovedWfaBookingUseCase,
    private val resolveTodayWfaBookingStateUseCase: ResolveTodayWfaBookingStateUseCase,
    private val resolveAuthoritativeTargetLocationUseCase: ResolveAuthoritativeTargetLocationUseCase,
    private val evaluateTargetRangeUseCase: EvaluateTargetRangeUseCase,
    private val evaluateAttendancePreparationUseCase: EvaluateAttendancePreparationUseCase,
    // Add UseCase dependencies for attendance operations
    private val checkInUseCase: CheckInUseCase,
    private val checkOutUseCase: CheckOutUseCase
) : ViewModel() {

    // Main UI state - now the SINGLE source of truth
    private val _uiState = MutableStateFlow(AttendanceScreenState())
    val uiState: StateFlow<AttendanceScreenState> = _uiState.asStateFlow()

    private val _transientFeedback = MutableSharedFlow<AttendanceTransientFeedback>(
        replay = 0,
        extraBufferCapacity = 1
    )
    val transientFeedback: SharedFlow<AttendanceTransientFeedback> =
        _transientFeedback.asSharedFlow()
    private var nextTransientFeedbackId = 0L

    private val _mapCameraEffect = MutableStateFlow<MapCameraEffect?>(null)
    val mapCameraEffect: StateFlow<MapCameraEffect?> = _mapCameraEffect.asStateFlow()
    private var nextMapCameraEffectId = 0L
    private var nextMapPickSessionId = 0L

    // Job for UI-focused location updates (display purposes only)
    private var displayLocationJob: Job? = null
    private var modeResolutionJob: Job? = null
    private var recommendationJob: Job? = null
    private val latestSelectionGuard = LatestSelectionGuard()
    private var latestSelectionRequest: SelectionRequestToken =
        latestSelectionGuard.next(WorkMode.WFO)
    private var latestProfile: UserModel? = null
    private val preparationRefreshCoordinator = AttendancePreparationRefreshCoordinator(
        fetchStatus = ::fetchTodayStatus,
        requestProfileRefresh = { refreshAttendanceProfileUseCase() },
        applyProfile = { user -> latestProfile = user },
        resolveWfh = {
            if (_uiState.value.preparation.selectedMode == WorkMode.WFH) {
                resolveAndApplyTargetForMode(WorkMode.WFH)
            }
            refreshReminderGeofencesIfNeeded()
        },
        preserveProfileRecovery = ::preserveProfileRefreshRecovery
    )

    companion object {
        private const val TAG = "AttendanceViewModel"
        private const val DISPLAY_UPDATE_INTERVAL = 10000L // 10 seconds for UI updates
        private const val SESSION_STATE_ACTIVE = "active"
        private const val SESSION_STATE_NOT_STARTED = "not_started"
    }

    init {
        initializeData()
    }

    // ===========================================
    // Functions for consuming state events
    // ===========================================

    /**
     * Called by UI after navigation is handled
     * Resets navigationTarget to null
     */
    fun onNavigationHandled() {
        _uiState.value = _uiState.value.copy(navigationTarget = null)
    }

    private fun publishTransientFeedback(
        kind: AttendanceTransientFeedbackKind
    ): AttendanceTransientFeedback {
        nextTransientFeedbackId += 1
        val feedback = AttendanceTransientFeedbackFactory.create(
            id = nextTransientFeedbackId,
            kind = kind
        )
        if (!_transientFeedback.tryEmit(feedback)) {
            viewModelScope.launch { _transientFeedback.emit(feedback) }
        }
        return feedback
    }

    private fun publishMapCameraEffect(effect: MapCameraEffect) {
        _mapCameraEffect.value = effect
    }

    fun onMapCameraEffectConsumed(effectId: Long) {
        val pending = _mapCameraEffect.value ?: return
        if (pending.id == effectId) {
            _mapCameraEffect.compareAndSet(pending, null)
        }
    }

    private fun publishExplicitWfaSelectionFocus(
        request: SelectionRequestToken
    ): Boolean {
        if (!latestSelectionGuard.isCurrent(request, WorkMode.WFA)) return false
        val coordinate = WfaMapSelectionEffect.explicitSelectionCoordinate(
            _uiState.value.preparation
        ) ?: return false
        publishMapCameraEffect(
            WfaMapSelectionEffect.focus(
                id = nextMapCameraEffectId++,
                coordinate = coordinate
            )
        )
        return true
    }

    /**
     * Initialize data by fetching both WFO and WFH locations
     */
    private fun initializeData() {
        viewModelScope.launch {
            try {
                _uiState.value = _uiState.value.copy(uiState = UiState.Loading)

                // Fetch both locations concurrently
                fetchTodayStatus()
                fetchUserHomeLocation()

                // Don't start location updates automatically - let UI control this
                refreshReminderGeofencesIfNeeded()

            } catch (e: Exception) {
                Log.e(TAG, "Error initializing data", e)
                _uiState.value = _uiState.value.copy(
                    uiState = UiState.Error("Data absensi belum dapat dimuat. Silakan coba lagi.")
                )
            }
        }
    }

    /**
     * Fetch today status to get WFO location and resolve explicit attendance action state.
     */
    private suspend fun fetchTodayStatus(forceRefresh: Boolean = false) {
        try {
            getTodayStatusUseCase(forceRefresh).onSuccess { todayStatus ->
                Log.d(
                    TAG,
                    "Today status fetched successfully: mode=${todayStatus.activeMode}, canCheckIn=${todayStatus.canCheckIn}, canCheckOut=${todayStatus.canCheckOut}, state=${todayStatus.attendanceSessionState?.key}"
                )

                val selectedMode = WorkMode.fromRaw(todayStatus.activeMode) ?: WorkMode.WFO

                val nextState = _uiState.value.copy(
                    todayStatus = todayStatus,
                    uiState = UiState.Success(Unit)
                )
                _uiState.value = nextState.withResolvedActionStatePreservingInFlightSubmit()

                resolveAndApplyTargetForMode(selectedMode)

                Log.d(TAG, "WFO location updated: ${todayStatus.activeLocation}")
                Log.d(
                    TAG,
                    "Attendance action state updated: ${_uiState.value.actionState}"
                )

                refreshReminderGeofencesIfNeeded()

            }.onFailure { exception ->
                Log.e(TAG, "Failed to fetch today status", exception)
                val message = "Status absensi belum dapat dimuat. Silakan coba lagi."
                _uiState.value = _uiState.value.copy(
                    uiState = UiState.Error(message)
                ).withActionState(
                    AttendanceActionState.RetryableFailure(
                        intent = null,
                        title = "Status absensi gagal dimuat",
                        message = message
                    )
                )
            }
        } catch (e: Exception) {
            Log.e(TAG, "Unexpected error in fetchTodayStatus", e)
            val message = "Status absensi belum dapat dimuat. Silakan coba lagi."
            _uiState.value = _uiState.value.copy(
                uiState = UiState.Error(message)
            ).withActionState(
                AttendanceActionState.RetryableFailure(
                    intent = null,
                    title = "Status absensi gagal dimuat",
                    message = message
                )
            )
        }
    }

    private fun AttendanceScreenState.withActionState(
        actionState: AttendanceActionState
    ): AttendanceScreenState {
        return copy(actionState = actionState)
    }

    private fun AttendanceScreenState.withResolvedActionStatePreservingInFlightSubmit(): AttendanceScreenState {
        val currentActionState = _uiState.value.actionState
        val inFlightState = currentActionState as? AttendanceActionState.VerifyingFace
            ?: currentActionState as? AttendanceActionState.Submitting

        return if (inFlightState != null) {
            withActionState(inFlightState)
        } else {
            withActionState(AttendanceActionResolver.resolve(this))
        }
    }

    private fun refreshResolvedActionState() {
        _uiState.value = _uiState.value.withResolvedActionStatePreservingInFlightSubmit()
    }

    /**
     * Fetch user home location from logged in user data
     */
    private suspend fun fetchUserHomeLocation() {
        try {
            getLoggedInUserUseCase().collect { user ->
                Log.d(TAG, "User data fetched successfully")
                latestProfile = user

                val wfhLocation = user.toWfhAttendanceLocation()

                if (_uiState.value.preparation.selectedMode == WorkMode.WFH) {
                    resolveAndApplyTargetForMode(WorkMode.WFH)
                }

                Log.d(TAG, "WFH location updated: $wfhLocation")
                refreshReminderGeofencesIfNeeded()
            }
        } catch (e: Exception) {
            Log.w(TAG, "Unexpected error in fetchUserHomeLocation", e)
            // Continue without WFH location
        }
    }

    private fun refreshReminderGeofencesIfNeeded() {
        viewModelScope.launch {
            val state = _uiState.value
            val todayStatus = state.todayStatus ?: return@launch
            val sessionKey = todayStatus.attendanceSessionState?.key
            val shouldRegisterReminders = todayStatus.activeAttendanceId == null &&
                sessionKey == SESSION_STATE_NOT_STARTED &&
                todayStatus.canCheckIn

            attendancePreference.saveAttendanceSessionStateKey(sessionKey)

            if (!shouldRegisterReminders) {
                Log.d(
                    TAG,
                    "Reminder geofence skipped: attendanceId=${todayStatus.activeAttendanceId}, state=$sessionKey, canCheckIn=${todayStatus.canCheckIn}"
                )
                return@launch
            }

            val candidates = buildReminderCandidates(
                todayStatus,
                latestProfile.toWfhAttendanceLocation()
            )
            Log.d(
                TAG,
                "Reminder candidates built: " + candidates.joinToString { "${it.id}:${it.modeKey}:${it.source}" }
            )
            geofenceManager.registerReminderGeofences(candidates)
        }
    }

    private suspend fun buildReminderCandidates(
        todayStatus: TodayStatus,
        wfhLocation: Location?
    ): List<ReminderGeofenceCandidate> {
        val candidates = mutableListOf<ReminderGeofenceCandidate>()

        todayStatus.activeLocation?.let { primary ->
            candidates += ReminderGeofenceCandidate(
                id = "reminder:primary:${primary.locationId}",
                modeKey = WorkMode.fromRaw(todayStatus.activeMode)?.shortLabel?.lowercase() ?: WorkMode.WFO.shortLabel.lowercase(),
                label = primary.description,
                coordinate = primary.coordinate,
                radius = DistanceMeters(primary.radius.toDouble()),
                source = "status-today.active_location"
            )
        }

        wfhLocation?.let { home ->
            candidates += ReminderGeofenceCandidate(
                id = "reminder:wfh:user_home:${home.locationId}",
                modeKey = WorkMode.WFH.shortLabel.lowercase(),
                label = home.description,
                coordinate = home.coordinate,
                radius = DistanceMeters(home.radius.toDouble()),
                source = "/me"
            )
        }

        val todayDate = todayStatus.todayDate
        if (todayDate.isNotBlank()) {
            resolveTodayApprovedWfaBookingUseCase(todayDate).onSuccess { booking ->
                val latitude = booking.latitude
                val longitude = booking.longitude
                if (latitude != null && longitude != null && !isDuplicateWithPrimary(todayStatus.activeLocation, latitude, longitude)) {
                    val coordinate = runCatching { GeoCoordinate(latitude, longitude) }.getOrNull()
                    if (coordinate != null) candidates += ReminderGeofenceCandidate(
                        id = "reminder:wfa:${booking.bookingId}:${booking.locationId ?: 0}",
                        modeKey = WorkMode.WFA.shortLabel.lowercase(),
                        label = booking.locationDescription,
                        coordinate = coordinate,
                        radius = DistanceMeters((booking.radiusMeters ?: 100f).toDouble()),
                        source = "approved-wfa-booking"
                    )
                }
            }.onFailure {
                Log.d(TAG, "No approved WFA reminder candidate")
            }
        }

        return candidates.distinctBy { it.id }
    }

    private fun isDuplicateWithPrimary(primary: Location?, latitude: Double, longitude: Double): Boolean {
        if (primary == null) return false
        return kotlin.math.abs(primary.latitude - latitude) < 0.00001 &&
            kotlin.math.abs(primary.longitude - longitude) < 0.00001
    }

    /**
     * Start location updates for display purposes only (UI updates)
     * This runs only while ViewModel is active for better UX
     */
    private fun startDisplayLocationUpdates() {
        displayLocationJob?.cancel()

        displayLocationJob = viewModelScope.launch {
            while (true) {
                try {
                    updateDisplayLocation()
                    delay(DISPLAY_UPDATE_INTERVAL)
                } catch (e: Exception) {
                    Log.e(TAG, "Error in display location updates", e)
                    delay(DISPLAY_UPDATE_INTERVAL)
                }
            }
        }
    }

    /**
     * Update location data for display purposes only
     * Does not affect validation logic
     */
    private suspend fun updateDisplayLocation() {
        val selectionRequest = latestSelectionRequest
        try {
            // Update current address for display - menggunakan Geocoding API yang sudah diperbaiki
            when (val addressResult = getCurrentAddressUseCase()) {
                is AddressResolutionResult.Resolved -> {
                    if (!latestSelectionGuard.isCurrent(
                            selectionRequest,
                            _uiState.value.preparation.selectedMode
                        )
                    ) return
                    _uiState.value = _uiState.value.copy(
                        currentUserAddress = addressResult.address.formattedAddress
                    )
                }
                is AddressResolutionResult.CoordinateOnly -> {
                    if (!latestSelectionGuard.isCurrent(
                            selectionRequest,
                            _uiState.value.preparation.selectedMode
                        )
                    ) return
                    _uiState.value = _uiState.value.copy(
                        currentUserAddress = addressResult.coordinate.toDisplayText()
                    )
                }
                is AddressResolutionResult.Failed -> {
                    if (!latestSelectionGuard.isCurrent(
                            selectionRequest,
                            _uiState.value.preparation.selectedMode
                        )
                    ) return
                    _uiState.value = _uiState.value.copy(currentUserAddress = "Mengambil alamat...")
                }
            }

            when (val current = getCurrentLocationUseCase()) {
                is CurrentLocationResult.Success -> {
                    if (latestSelectionGuard.isCurrent(
                            selectionRequest,
                            _uiState.value.preparation.selectedMode
                        )
                    ) {
                        _uiState.value = _uiState.value
                            .withCurrentLocation(current)
                            .withResolvedActionStatePreservingInFlightSubmit()
                    }
                }
                is CurrentLocationResult.Failure -> {
                    if (latestSelectionGuard.isCurrent(
                            selectionRequest,
                            _uiState.value.preparation.selectedMode
                        )
                    ) {
                        _uiState.value = _uiState.value
                            .withCurrentLocation(current)
                            .withResolvedActionStatePreservingInFlightSubmit()
                    }
                    Log.w(TAG, "Failed to get display coordinates: $current")
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Unexpected error in updateDisplayLocation", e)
        }
    }

    private fun AttendanceScreenState.withCurrentLocation(
        current: CurrentLocationResult
    ): AttendanceScreenState = copy(
        preparation = AttendanceCurrentLocationTransition.apply(
            preparation = preparation,
            current = current,
            nowEpochMillis = System.currentTimeMillis(),
            evaluateRange = evaluateTargetRangeUseCase,
            evaluateEligibility = evaluateAttendancePreparationUseCase
        )
    )

    /**
     * Handle work mode selection without mutating active monitoring geofences.
     */
    fun onWorkModeSelected(mode: WorkMode) {
        Log.d(TAG, "Work mode selected: ${mode.shortLabel}")
        resolveAndApplyTargetForMode(mode)
    }

    fun onAttendanceStatusRefreshRequested() {
        viewModelScope.launch {
            preparationRefreshCoordinator.refreshStatus()
        }
    }

    fun onAttendanceProfileRefreshRequested() {
        viewModelScope.launch {
            preparationRefreshCoordinator.refreshProfile()
        }
    }

    fun onWfaDiscoveryRetryRequested() {
        resolveAndApplyTargetForMode(WorkMode.WFA)
    }

    private fun preserveProfileRefreshRecovery() {
        if (_uiState.value.preparation.selectedMode != WorkMode.WFH) return
        val resolution = TargetLocationResolution.Failed(
            mode = WorkMode.WFH,
            failure = TargetResolutionFailure.PROFILE_REFRESH_FAILED
        )
        val eligibility = evaluateAttendancePreparationUseCase(
            resolution = resolution,
            rangeStatus = TargetRangeStatus.Unknown(
                TargetRangeUnknownReason.CURRENT_LOCATION_UNAVAILABLE
            )
        )
        _uiState.value = _uiState.value.copy(
            preparation = _uiState.value.preparation.copy(
                targetResolution = resolution,
                rangeStatus = null,
                eligibility = eligibility
            )
        ).withResolvedActionStatePreservingInFlightSubmit()
    }

    private fun resolveAndApplyTargetForMode(
        mode: WorkMode = _uiState.value.preparation.selectedMode
    ) {
        modeResolutionJob?.cancel()
        recommendationJob?.cancel()

        val request = latestSelectionGuard.next(mode)
        latestSelectionRequest = request
        val resolvingPreparation = _uiState.value.preparation.copy(
            selectedMode = mode,
            targetResolution = TargetLocationResolution.Resolving(mode),
            rangeStatus = null,
            wfaDiscovery = if (mode == WorkMode.WFA) {
                WfaDiscoveryState.Loading
            } else {
                WfaDiscoveryState.Hidden
            },
            eligibility = AttendancePreparationEligibility.Resolving
        )
        _uiState.value = AttendanceSelectionTransition.beginSelection(
            state = _uiState.value,
            preparation = resolvingPreparation
        ).withResolvedActionStatePreservingInFlightSubmit()

        modeResolutionJob = viewModelScope.launch {
            try {
                val profile = if (mode == WorkMode.WFH) {
                    latestProfile ?: getLoggedInUserUseCase().firstOrNull()
                } else {
                    latestProfile
                }
                val current = getCurrentLocationUseCase()
                val booking = if (mode == WorkMode.WFA) {
                    val scheduleDate = _uiState.value.todayStatus?.todayDate
                    if (scheduleDate.isNullOrBlank()) {
                        WfaBookingForDate.NotRequested
                    } else {
                        resolveTodayWfaBookingStateUseCase(scheduleDate)
                    }
                } else {
                    WfaBookingForDate.NotRequested
                }
                val resolution = resolveAuthoritativeTargetLocationUseCase(
                    mode = mode,
                    todayStatus = _uiState.value.todayStatus,
                    profile = profile,
                    wfaBooking = booking
                )
                val range = (resolution as? TargetLocationResolution.Resolved)?.let { resolved ->
                    evaluateTargetRangeUseCase(
                        target = resolved.target,
                        current = current,
                        nowEpochMillis = System.currentTimeMillis()
                    )
                }
                val eligibility = evaluateAttendancePreparationUseCase(
                    resolution = resolution,
                    rangeStatus = range ?: TargetRangeStatus.Unknown(
                        TargetRangeUnknownReason.CURRENT_LOCATION_UNAVAILABLE
                    )
                )

                if (!latestSelectionGuard.isCurrent(request, mode)) return@launch
                val nextPreparation = _uiState.value.preparation.copy(
                    selectedMode = mode,
                    targetResolution = resolution,
                    currentLocation = current,
                    rangeStatus = range,
                    eligibility = eligibility
                )
                _uiState.value = _uiState.value.copy(
                    preparation = nextPreparation
                ).withResolvedActionStatePreservingInFlightSubmit()

                if (!latestSelectionGuard.isCurrent(request, mode)) return@launch
                (resolution as? TargetLocationResolution.Resolved)?.target?.let { target ->
                    animateMapToTarget(target, request)
                }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Exception) {
                Log.e(TAG, "Failed to resolve attendance preparation for ${mode.shortLabel}", error)
                if (!latestSelectionGuard.isCurrent(request, mode)) return@launch
                val resolution = TargetLocationResolution.Failed(
                    mode = mode,
                    failure = mode.resolutionFailure()
                )
                val eligibility = evaluateAttendancePreparationUseCase(
                    resolution,
                    TargetRangeStatus.Unknown(TargetRangeUnknownReason.CURRENT_LOCATION_UNAVAILABLE)
                )
                _uiState.value = _uiState.value.copy(
                    preparation = _uiState.value.preparation.copy(
                        targetResolution = resolution,
                        rangeStatus = null,
                        eligibility = eligibility
                    )
                ).withResolvedActionStatePreservingInFlightSubmit()
            }
        }

        if (mode == WorkMode.WFA) {
            fetchWfaRecommendations(request)
        }
    }

    private fun WorkMode.resolutionFailure(): TargetResolutionFailure = when (this) {
        WorkMode.WFO -> TargetResolutionFailure.STATUS_REFRESH_FAILED
        WorkMode.WFH -> TargetResolutionFailure.PROFILE_REFRESH_FAILED
        WorkMode.WFA -> TargetResolutionFailure.BOOKING_REFRESH_FAILED
    }

    private fun animateMapToTarget(
        target: AuthoritativeTargetLocation,
        request: SelectionRequestToken,
        zoomLevel: Double = 15.0
    ): Boolean {
        val preparation = _uiState.value.preparation
        if (!latestSelectionGuard.isCurrent(request, preparation.selectedMode)) return false
        if (WfaMapSelectionEffect.explicitSelectionCoordinate(preparation) != null) return false
        if (
            AttendanceSelectionTransition.resolvedTargetForInteraction(
                preparation = preparation,
                selectedTargetId = target.targetId
            ) == null
        ) return false
        _uiState.value = _uiState.value.copy(
            preparation = AttendanceSelectionTransition.cancelMapPick(
                _uiState.value.preparation
            )
        )
        publishMapCameraEffect(
            MapCameraEffect.Focus(
                id = nextMapCameraEffectId++,
                coordinate = target.coordinate,
                zoom = zoomLevel.toFloat()
            )
        )
        return true
    }

    /**
     * Fetch WFA recommendations based on current user location
     * Menggunakan GPS real-time, bukan lokasi WFH yang tersimpan
     */
    private fun fetchWfaRecommendations(request: SelectionRequestToken) {
        recommendationJob = viewModelScope.launch {
            try {
                val coordinate = when (val current = getCurrentLocationUseCase()) {
                    is CurrentLocationResult.Success -> current.location.coordinate
                    is CurrentLocationResult.Failure -> cachedCurrentCoordinateOrNull()
                }
                if (coordinate == null) {
                    if (!latestSelectionGuard.isCurrent(request, WorkMode.WFA)) return@launch
                    _uiState.value = _uiState.value.copy(
                        preparation = _uiState.value.preparation.copy(
                            wfaDiscovery = WfaDiscoveryState.Failure(retryable = true)
                        )
                    )
                    return@launch
                }

                getWfaRecommendationsUseCase(
                    coordinate.latitude,
                    coordinate.longitude
                ).onSuccess { recommendations ->
                    if (!latestSelectionGuard.isCurrent(request, WorkMode.WFA)) return@onSuccess
                    val currentDiscovery = _uiState.value.preparation.wfaDiscovery
                        as? WfaDiscoveryState.Content
                    val discovery = if (recommendations.isEmpty() && currentDiscovery?.searchPreview == null) {
                        WfaDiscoveryState.Empty
                    } else {
                        WfaDiscoveryState.Content(
                            recommendations = recommendations,
                            selectedKey = currentDiscovery?.selectedKey
                                ?.takeIf { selected -> recommendations.any { it.stableKey == selected } },
                            searchPreview = currentDiscovery?.searchPreview
                        )
                    }
                    _uiState.value = _uiState.value.copy(
                        preparation = _uiState.value.preparation.copy(wfaDiscovery = discovery)
                    )
                    val shouldAutoFit = WfaMapSelectionEffect.shouldAutoFitRecommendations(
                        _uiState.value.preparation
                    )
                    if (recommendations.isNotEmpty() && shouldAutoFit) {
                        if (!latestSelectionGuard.isCurrent(request, WorkMode.WFA)) return@onSuccess
                        _uiState.value = _uiState.value.copy(
                            preparation = AttendanceSelectionTransition.cancelMapPick(
                                _uiState.value.preparation
                            )
                        )
                        publishMapCameraEffect(
                            MapCameraEffect.Fit(
                                id = nextMapCameraEffectId++,
                                coordinates = recommendations.map(WfaRecommendation::coordinate)
                            )
                        )
                    }
                }.onFailure { exception ->
                    Log.e(TAG, "Failed to fetch WFA recommendations", exception)
                    if (!latestSelectionGuard.isCurrent(request, WorkMode.WFA)) return@onFailure
                    _uiState.value = _uiState.value.copy(
                        preparation = _uiState.value.preparation.copy(
                            wfaDiscovery = WfaDiscoveryState.Failure(retryable = true)
                        )
                    )
                }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (e: Exception) {
                Log.e(TAG, "Unexpected error in fetchWfaRecommendations", e)
                if (!latestSelectionGuard.isCurrent(request, WorkMode.WFA)) return@launch
                _uiState.value = _uiState.value.copy(
                    preparation = _uiState.value.preparation.copy(
                        wfaDiscovery = WfaDiscoveryState.Failure(retryable = true)
                    )
                )
            }
        }
    }

    /**
     * Handle WFA marker click - Updated to show marker details
     */
    fun onWfaMarkerClicked(recommendation: WfaRecommendation) {
        Log.d(TAG, "WFA Marker clicked: ${recommendation.name}")
        val request = latestSelectionRequest
        if (!latestSelectionGuard.isCurrent(request, WorkMode.WFA)) return
        val preparation = AttendanceSelectionTransition.cancelMapPick(
            _uiState.value.preparation
        )
        _uiState.value = _uiState.value.copy(
            preparation = AttendancePreparationReducer.selectRecommendation(
                preparation,
                recommendation
            )
        )
        if (!publishExplicitWfaSelectionFocus(request)) return
        refreshResolvedActionState()
    }

    /**
     * Handle booking button click
     */
    fun onBookingClicked() {
        val request = latestSelectionRequest
        val preparation = _uiState.value.preparation
        if (preparation.selectedMode != WorkMode.WFA) {
            Log.d(TAG, "Booking clicked outside WFA mode")
            return
        }
        val discovery = preparation.wfaDiscovery as? WfaDiscoveryState.Content
        val selectedRecommendation = discovery?.recommendations?.firstOrNull {
            it.stableKey == discovery.selectedKey
        }
        val coordinate = selectedRecommendation?.coordinate
            ?: discovery?.searchPreview?.let { preview ->
                runCatching { GeoCoordinate(preview.latitude, preview.longitude) }.getOrNull()
            }
        if (coordinate == null) {
            Log.w(TAG, "Booking clicked in WFA mode but no preview is selected.")
            return
        }
        val route = Screen.WfaBooking.createRoute(
            latitude = coordinate.latitude,
            longitude = coordinate.longitude
        )
        val navigationTarget = AttendanceSelectionTransition.wfaBookingNavigationTarget(
            preparation = _uiState.value.preparation,
            selectionIsCurrent = latestSelectionGuard.isCurrent(request, WorkMode.WFA),
            route = route
        ) ?: return
        _uiState.value = _uiState.value.copy(
            navigationTarget = navigationTarget
        )
    }

    /**
     * Handle attendance button click using explicit action state.
     */
    fun onAttendanceButtonClicked() {
        val currentActionState = _uiState.value.actionState
        val intent = when (currentActionState) {
            is AttendanceActionState.Ready -> currentActionState.intent
            is AttendanceActionState.RetryableFailure -> currentActionState.intent
            else -> null
        }

        if (intent == null || !_uiState.value.actionState.isCtaEnabled) {
            Log.d(TAG, "Attendance button clicked but action is not ready: $currentActionState")
            return
        }

        Log.d(TAG, "Attendance button clicked - $intent")

        val verifyingState = AttendanceActionState.VerifyingFace(intent)
        _uiState.value = _uiState.value.copy(
            navigationTarget = NavigationTarget.FaceScanner(intent)
        ).withActionState(verifyingState)
    }

    /**
     * Handle face verification result - GATEWAY after face verification.
     * Face success only moves to backend submission; backend result remains authoritative.
     */
    fun onFaceVerificationResult(result: FaceVerificationResult) {
        Log.d(TAG, "Face verification result: $result")

        if (_uiState.value.actionState is AttendanceActionState.Submitting) {
            Log.d(TAG, "Ignoring face verification result because attendance submit is already in flight")
            return
        }

        val intent = (_uiState.value.actionState as? AttendanceActionState.VerifyingFace)?.intent
            ?: _uiState.value.takeIf { it.actionState.isCtaEnabled }?.let { state ->
                (state.actionState as? AttendanceActionState.Ready)?.intent
                    ?: (state.actionState as? AttendanceActionState.RetryableFailure)?.intent
            }

        if (intent == null) {
            Log.w(TAG, "Face verification result received without active attendance intent: $result")
            _uiState.value = _uiState.value.withActionState(
                AttendanceActionState.RetryableFailure(
                    intent = null,
                    title = "Verifikasi wajah tidak dapat diproses",
                    message = "Status aksi absensi tidak tersedia. Silakan muat ulang halaman absensi."
                )
            )
            return
        }

        if (result.submitsAttendance) {
            val submittingState = AttendanceActionState.Submitting(
                intent = intent,
                message = intent.submittingMessage()
            )
            _uiState.value = _uiState.value.withActionState(submittingState)
            when (intent) {
                AttendanceActionIntent.CHECK_IN -> proceedWithCheckIn(intent)
                AttendanceActionIntent.CHECK_OUT -> proceedWithCheckOut(intent)
            }
            return
        }

        result.attendanceErrorMessage?.let { errorMessage ->
            Log.d(TAG, "Face verification did not submit attendance: $result")
            publishTransientFeedback(
                when (result) {
                    FaceVerificationResult.TIMEOUT -> AttendanceTransientFeedbackKind.FACE_TIMEOUT
                    else -> AttendanceTransientFeedbackKind.FACE_FAILED
                }
            )
            _uiState.value = _uiState.value.withActionState(
                AttendanceActionState.RetryableFailure(
                    intent = intent,
                    title = "Verifikasi wajah gagal",
                    message = errorMessage
                )
            )
            return
        }

        Log.d(TAG, "Face verification cancelled - attendance action returns to ready")
        _uiState.value = _uiState.value.withActionState(
            AttendanceActionState.Ready(
                intent = intent,
                label = intent.readyLabel()
            )
        )
    }

    fun onUnexpectedFaceVerificationResult() {
        Log.e(TAG, "Unexpected face verification result payload received")
        val intent = (_uiState.value.actionState as? AttendanceActionState.VerifyingFace)?.intent
        val message = publishTransientFeedback(
            AttendanceTransientFeedbackKind.FACE_UNKNOWN
        ).message
        _uiState.value = _uiState.value.withActionState(
            AttendanceActionState.RetryableFailure(
                intent = intent,
                title = "Verifikasi wajah gagal",
                message = message
            )
        )
    }

    /**
     * Proceed with check-in after successful face verification
     * FIXED: Added proper error message extraction from server response
     */
    private fun proceedWithCheckIn(intent: AttendanceActionIntent) {
        viewModelScope.launch {
            try {
                Log.d(TAG, "Proceeding with check-in after face verification")

                val initialSubmittingState = _uiState.value.actionState as? AttendanceActionState.Submitting
                if (initialSubmittingState?.intent != intent) {
                    Log.w(TAG, "Check-in submit blocked because action state is not Submitting($intent): ${_uiState.value.actionState}")
                    return@launch
                }

                val preparation = _uiState.value.preparation
                val selectedMode = preparation.selectedMode
                val resolvedTarget = (preparation.targetResolution as? TargetLocationResolution.Resolved)
                    ?.target
                val eligibility = preparation.eligibility

                if (resolvedTarget == null || eligibility !is AttendancePreparationEligibility.Ready) {
                    _uiState.value = _uiState.value.withActionState(
                        AttendanceActionResolver.resolve(_uiState.value)
                    )
                    return@launch
                }
                // Get user info once for this mutation; long-running collection can replay submit.
                val user = getLoggedInUserUseCase().firstOrNull()
                if (user == null) {
                    val message = publishTransientFeedback(
                        AttendanceTransientFeedbackKind.ATTENDANCE_ERROR
                    ).message
                    _uiState.value = _uiState.value.withActionState(
                        AttendanceActionState.RetryableFailure(
                            intent = intent,
                            title = "Check-in gagal",
                            message = message
                        )
                    )
                    return@launch
                }

                val attendanceRequest = try {
                    AttendanceCheckInRequestFactory.create(
                        workMode = selectedMode,
                        authoritativeTarget = resolvedTarget
                    )
                } catch (_: IllegalArgumentException) {
                    val message = publishTransientFeedback(
                        AttendanceTransientFeedbackKind.ATTENDANCE_ERROR
                    ).message
                    _uiState.value = _uiState.value.withActionState(
                        AttendanceActionState.RetryableFailure(
                            intent = intent,
                            title = "Check-in gagal",
                            message = message
                        )
                    )
                    return@launch
                }

                val submittingState = _uiState.value.actionState as? AttendanceActionState.Submitting
                if (submittingState?.intent != intent) {
                    Log.w(TAG, "Check-in submit blocked because action state is not Submitting($intent): ${_uiState.value.actionState}")
                    return@launch
                }

                checkInUseCase(attendanceRequest).onSuccess { activeSession ->
                    Log.d(TAG, "Check-in successful: $activeSession")

                    val successMessage = publishTransientFeedback(
                        AttendanceTransientFeedbackKind.CHECK_IN_SUCCESS
                    ).message
                    _uiState.value = _uiState.value.withActionState(
                        AttendanceActionState.Success(
                            intent = intent,
                            message = successMessage
                        )
                    )

                    // Finish on backend-resolved state so snackbar auto-dismiss leaves a usable screen.
                    fetchTodayStatus(forceRefresh = true)

                }.onFailure { exception ->
                    Log.e(TAG, "Check-in failed", exception)

                    val errorMessage = publishTransientFeedback(
                        AttendanceTransientFeedbackKind.ATTENDANCE_ERROR
                    ).message

                    _uiState.value = _uiState.value.withActionState(
                        AttendanceActionState.RetryableFailure(
                            intent = intent,
                            title = "Check-in gagal",
                            message = errorMessage
                        )
                    )
                }

            } catch (e: Exception) {
                Log.e(TAG, "Error in proceedWithCheckIn", e)
                val message = publishTransientFeedback(
                    AttendanceTransientFeedbackKind.ATTENDANCE_ERROR
                ).message
                _uiState.value = _uiState.value.withActionState(
                    AttendanceActionState.RetryableFailure(
                        intent = intent,
                        title = "Check-in gagal",
                        message = message
                    )
                )
            }
        }
    }

    /**
     * Proceed with check-out after successful face verification
     * FIXED: Added proper error message extraction from server response
     */
    private fun proceedWithCheckOut(intent: AttendanceActionIntent) {
        viewModelScope.launch {
            try {
                Log.d(TAG, "Proceeding with check-out after face verification")

                val submittingState = _uiState.value.actionState as? AttendanceActionState.Submitting
                if (submittingState?.intent != intent) {
                    Log.w(TAG, "Check-out submit blocked because action state is not Submitting($intent): ${_uiState.value.actionState}")
                    return@launch
                }

                val attendanceId = _uiState.value.todayStatus?.activeAttendanceId

                // Call CheckOutUseCase - it will refresh status if needed, get real-time GPS, and fallback to preference
                checkOutUseCase(attendanceId).onSuccess { activeSession ->
                    Log.d(TAG, "Check-out successful: $activeSession")

                    val successMessage = publishTransientFeedback(
                        AttendanceTransientFeedbackKind.CHECK_OUT_SUCCESS
                    ).message
                    _uiState.value = _uiState.value.withActionState(
                        AttendanceActionState.Success(
                            intent = intent,
                            message = successMessage
                        )
                    )

                    // Finish on backend-resolved state so snackbar auto-dismiss leaves a usable screen.
                    fetchTodayStatus(forceRefresh = true)

                }.onFailure { exception ->
                    Log.e(TAG, "Check-out failed", exception)

                    val errorMessage = publishTransientFeedback(
                        AttendanceTransientFeedbackKind.ATTENDANCE_ERROR
                    ).message

                    _uiState.value = _uiState.value.withActionState(
                        AttendanceActionState.RetryableFailure(
                            intent = intent,
                            title = "Check-out gagal",
                            message = errorMessage
                        )
                    )
                }

            } catch (e: Exception) {
                Log.e(TAG, "Error in proceedWithCheckOut", e)
                val message = publishTransientFeedback(
                    AttendanceTransientFeedbackKind.ATTENDANCE_ERROR
                ).message
                _uiState.value = _uiState.value.withActionState(
                    AttendanceActionState.RetryableFailure(
                        intent = intent,
                        title = "Check-out gagal",
                        message = message
                    )
                )
            }
        }
    }

    /**
     * Handle focus location button click
     * This should ONLY focus on current user location, not work mode locations
     * Always gets fresh location data when clicked
     */
    fun onFocusLocationClicked() {
        val selectionRequest = latestSelectionRequest
        viewModelScope.launch {
            try {
                when (val current = getCurrentLocationUseCase()) {
                    is CurrentLocationResult.Success -> {
                        if (!latestSelectionGuard.isCurrent(
                                selectionRequest,
                                _uiState.value.preparation.selectedMode
                            )
                        ) return@launch
                        val coordinate = current.location.coordinate
                        _uiState.value = _uiState.value
                            .withCurrentLocation(current)
                            .withResolvedActionStatePreservingInFlightSubmit()
                        if (!latestSelectionGuard.isCurrent(
                                selectionRequest,
                                _uiState.value.preparation.selectedMode
                            )
                        ) return@launch
                        _uiState.value = _uiState.value.copy(
                            preparation = AttendanceSelectionTransition.cancelMapPick(
                                _uiState.value.preparation
                            )
                        )
                        publishMapCameraEffect(
                            MapCameraEffect.Focus(
                                id = nextMapCameraEffectId++,
                                coordinate = coordinate,
                                zoom = 15f
                            )
                        )
                    }
                    is CurrentLocationResult.Failure -> {
                        if (!latestSelectionGuard.isCurrent(
                                selectionRequest,
                                _uiState.value.preparation.selectedMode
                            )
                        ) return@launch
                        _uiState.value = _uiState.value
                            .withCurrentLocation(current)
                            .withResolvedActionStatePreservingInFlightSubmit()
                        Log.e(TAG, "Failed to get current GPS location: $current")
                        publishTransientFeedback(AttendanceTransientFeedbackKind.LOCATION_ERROR)
                    }
                }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (e: Exception) {
                Log.e(TAG, "=== UNEXPECTED ERROR ===")
                Log.e(TAG, "Error in onFocusLocationClicked", e)
                if (!latestSelectionGuard.isCurrent(
                        selectionRequest,
                        _uiState.value.preparation.selectedMode
                    )
                ) return@launch
                publishTransientFeedback(AttendanceTransientFeedbackKind.LOCATION_ERROR)
            }
        }
    }

    private fun cachedCurrentCoordinateOrNull(): GeoCoordinate? {
        val current = _uiState.value.preparation.currentLocation
            as? CurrentLocationResult.Success
        return current?.location?.coordinate
    }

    /**
     * Called when the map is ready to receive commands.
     */
    fun onMapReady() {
        if (_mapCameraEffect.value != null) return
        Log.d(TAG, "Map is ready, focusing to selected target location")
        val request = latestSelectionRequest
        val preparation = _uiState.value.preparation
        if (publishExplicitWfaSelectionFocus(request)) {
            Log.d(TAG, "Map focus restored to the explicit WFA selection")
            return
        }
        val target = (preparation.targetResolution as? TargetLocationResolution.Resolved)
            ?.target
        if (target != null && animateMapToTarget(target, request)) {
            Log.d(TAG, "Initial camera focus sent to selected target location")
        } else {
            Log.w(TAG, "Target location not available for initial focus")
        }
    }

    /**
     * Start location updates for display purposes
     * This should be called by UI when location permissions are granted
     */
    fun startLocationUpdates() {
        Log.d(TAG, "Starting location updates for display")
        startDisplayLocationUpdates()
    }

    /**
     * Handle selected location from LocationSearchScreen
     */
    fun onLocationSelected(location: LocationResult) {
        val request = latestSelectionRequest
        if (!latestSelectionGuard.isCurrent(request, WorkMode.WFA)) return
        val preparation = AttendanceSelectionTransition.cancelMapPick(
            _uiState.value.preparation
        )
        _uiState.value = _uiState.value.copy(
            preparation = AttendancePreparationReducer.selectSearchPreview(
                preparation,
                location
            )
        )
        publishExplicitWfaSelectionFocus(request)
    }

    /** Starts one explicit user-owned map-pick session. Camera-idle is ignored otherwise. */
    fun onMapPickRequested() {
        val preparation = _uiState.value.preparation
        if (preparation.selectedMode != WorkMode.WFA) return
        nextMapPickSessionId += 1
        _uiState.value = _uiState.value.copy(
            preparation = AttendanceSelectionTransition.beginMapPick(
                preparation = preparation,
                sessionId = nextMapPickSessionId
            )
        )
    }

    /**
     * Handle map idle event - called when user stops moving the map
     * Performs reverse geocoding for the center point of the map
     */
    fun onMapIdle(
        centerPoint: GeoCoordinate,
        origin: AttendanceMapCameraMoveOrigin
    ) {
        val mapPickSession = AttendanceSelectionTransition.mapPickSessionForCameraIdle(
            preparation = _uiState.value.preparation,
            origin = origin
        ) ?: return
        val request = latestSelectionRequest
        if (!latestSelectionGuard.isCurrent(request, WorkMode.WFA)) return

        viewModelScope.launch {
            try {
                Log.d(
                    TAG,
                    "Map idle detected in Pick on Map mode: ${centerPoint.latitude}, ${centerPoint.longitude}"
                )

                // Perform reverse geocoding for the center point
                when (val result = reverseGeocodeUseCase(centerPoint)) {
                    is AddressResolutionResult.Resolved -> applyPickedLocation(
                        request = request,
                        mapPickSession = mapPickSession,
                        coordinate = centerPoint,
                        placeName = result.address.name ?: result.address.formattedAddress,
                        address = result.address.formattedAddress
                    )
                    is AddressResolutionResult.CoordinateOnly -> applyPickedLocation(
                        request = request,
                        mapPickSession = mapPickSession,
                        coordinate = result.coordinate,
                        placeName = result.coordinate.toDisplayText(),
                        address = result.coordinate.toDisplayText()
                    )
                    is AddressResolutionResult.Failed -> {
                        if (!latestSelectionGuard.isCurrent(request, WorkMode.WFA)) return@launch
                        publishTransientFeedback(AttendanceTransientFeedbackKind.LOCATION_ERROR)
                    }
                }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (e: Exception) {
                Log.e(TAG, "Unexpected error in onMapIdle", e)
            }
        }
    }

    private fun applyPickedLocation(
        request: SelectionRequestToken,
        mapPickSession: WfaMapPickInteractionState.Active,
        coordinate: GeoCoordinate,
        placeName: String,
        address: String
    ) {
        if (!latestSelectionGuard.isCurrent(request, WorkMode.WFA)) return
        val locationResult = LocationResult(
            placeName = placeName,
            address = address,
            latitude = coordinate.latitude,
            longitude = coordinate.longitude
        )
        val consumedPreparation = AttendanceSelectionTransition.consumeMapPick(
            preparation = _uiState.value.preparation,
            session = mapPickSession
        ) ?: return
        _uiState.value = _uiState.value.copy(
            preparation = AttendancePreparationReducer.selectSearchPreview(
                consumedPreparation,
                locationResult
            )
        )
    }

    private fun UserModel?.toWfhAttendanceLocation(): Location? {
        val user = this ?: return null
        val latitude = user.latitude ?: return null
        val longitude = user.longitude ?: return null
        return Location(
            locationId = user.id,
            latitude = latitude,
            longitude = longitude,
            radius = user.radius ?: 100,
            description = user.locationDescription ?: "Work From Home Location",
            category = user.locationCategoryName ?: "Home"
        )
    }

    private fun GeoCoordinate.toDisplayText(): String =
        "Lat: %.6f, Lng: %.6f".format(Locale.US, latitude, longitude)

    // ===========================================
    // Permission Dialog Handling
    // ===========================================

    /**
     * Handle permission dialog result
     */
    fun onPermissionDialogResult(result: LocationPermissionHelper.PermissionResult) {
        _uiState.value = _uiState.value.copy(
            showPermissionDialog = false,
            permissionResult = result
        )

        if (result == LocationPermissionHelper.PermissionResult.AllPermissionsGranted) {
            resolveAndApplyTargetForMode()
        }
    }

    /**
     * Dismiss permission dialog
     */
    fun onDismissPermissionDialog() {
        _uiState.value = _uiState.value.copy(
            showPermissionDialog = false,
            permissionMessage = ""
        )
    }

    override fun onCleared() {
        super.onCleared()
        displayLocationJob?.cancel()
        modeResolutionJob?.cancel()
        recommendationJob?.cancel()

        // Removed onCleared geofence removal to keep geofence active until explicit checkout
    }
}
