package com.example.infinite_track.presentation.screen.attendance

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.infinite_track.data.soucre.local.preferences.AttendancePreference
import com.example.infinite_track.domain.model.attendance.AttendanceRequestModel
import com.example.infinite_track.domain.model.attendance.Location
import com.example.infinite_track.domain.model.attendance.TodayStatus
import com.example.infinite_track.domain.model.attendance.WorkMode
import com.example.infinite_track.domain.model.booking.BookingHistoryItem
import com.example.infinite_track.domain.model.location.LocationResult
import com.example.infinite_track.domain.model.location.GeoCoordinate
import com.example.infinite_track.domain.model.location.DistanceMeters
import com.example.infinite_track.domain.model.location.AddressResolutionResult
import com.example.infinite_track.domain.model.wfa.WfaRecommendation
import com.example.infinite_track.domain.use_case.attendance.CheckInUseCase
import com.example.infinite_track.domain.use_case.attendance.CheckOutUseCase
import com.example.infinite_track.domain.use_case.attendance.EvaluateWorkModeEligibilityUseCase
import com.example.infinite_track.domain.use_case.attendance.GetTodayStatusUseCase
import com.example.infinite_track.domain.use_case.attendance.ResolveSelectedTargetLocationUseCase
import com.example.infinite_track.domain.use_case.auth.GetLoggedInUserUseCase
import com.example.infinite_track.domain.use_case.booking.ResolveTodayApprovedWfaBookingIdUseCase
import com.example.infinite_track.domain.use_case.booking.ResolveTodayApprovedWfaBookingUseCase
import com.example.infinite_track.domain.use_case.location.GetCurrentAddressUseCase
import com.example.infinite_track.domain.use_case.location.GetCurrentLocationUseCase
import com.example.infinite_track.domain.model.location.CurrentLocationResult
import com.example.infinite_track.domain.use_case.location.ReverseGeocodeUseCase
import com.example.infinite_track.domain.use_case.wfa.GetWfaRecommendationsUseCase
import com.example.infinite_track.presentation.geofencing.GeofenceManager
import com.example.infinite_track.presentation.geofencing.ReminderGeofenceCandidate
import com.example.infinite_track.presentation.navigation.Screen
import com.example.infinite_track.presentation.map.model.MapCameraEffect
import com.example.infinite_track.utils.LocationPermissionHelper
import com.example.infinite_track.utils.UiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
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
    private val resolveTodayApprovedWfaBookingIdUseCase: ResolveTodayApprovedWfaBookingIdUseCase,
    private val resolveTodayApprovedWfaBookingUseCase: ResolveTodayApprovedWfaBookingUseCase,
    private val resolveSelectedTargetLocationUseCase: ResolveSelectedTargetLocationUseCase,
    private val evaluateWorkModeEligibilityUseCase: EvaluateWorkModeEligibilityUseCase,
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

    private val _mapCameraEffects = MutableSharedFlow<MapCameraEffect>(
        replay = 0,
        extraBufferCapacity = 1
    )
    val mapCameraEffects: SharedFlow<MapCameraEffect> = _mapCameraEffects.asSharedFlow()
    private var nextMapCameraEffectId = 0L

    // Job for UI-focused location updates (display purposes only)
    private var displayLocationJob: Job? = null

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
                val approvedWfaLocation = if (
                    selectedMode == WorkMode.WFA && todayStatus.todayDate.isNotBlank()
                ) {
                    resolveTodayApprovedWfaBookingUseCase(todayStatus.todayDate)
                        .getOrNull()
                        ?.toAttendanceLocation()
                } else {
                    null
                }

                val nextState = _uiState.value.copy(
                    todayStatus = todayStatus,
                    targetLocation = todayStatus.activeLocation,
                    wfoLocation = todayStatus.activeLocation, // WFO location from today status
                    targetLocationMarker = todayStatus.activeLocation,
                    selectedWorkMode = selectedMode,
                    isWfaModeActive = selectedMode == WorkMode.WFA,
                    approvedWfaLocation = approvedWfaLocation,
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
        return copy(
            actionState = actionState,
            buttonText = actionState.ctaLabel,
            isButtonEnabled = actionState.isCtaEnabled,
            isCheckInMode = actionState.legacyIsCheckInMode
        )
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

                // Extract WFH location from user profile if available
                val wfhLocation = if (user?.latitude != null && user.longitude != null) {
                    Location(
                        locationId = user.id, // Use user ID as location ID
                        latitude = user.latitude,
                        longitude = user.longitude,
                        radius = user.radius ?: 100, // Default 100m radius if not specified
                        description = user.locationDescription ?: "Work From Home Location",
                        category = user.locationCategoryName ?: "Home"
                    )
                } else {
                    null // No home location data available
                }

                _uiState.value = _uiState.value.copy(
                    wfhLocation = wfhLocation
                )
                refreshResolvedActionState()

                if (_uiState.value.selectedWorkMode == WorkMode.WFH) {
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

            val candidates = buildReminderCandidates(todayStatus, state.wfhLocation)
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
        try {
            // Update current address for display - menggunakan Geocoding API yang sudah diperbaiki
            when (val addressResult = getCurrentAddressUseCase()) {
                is AddressResolutionResult.Resolved -> {
                    _uiState.value = _uiState.value.copy(
                        currentUserAddress = addressResult.address.formattedAddress
                    )
                }
                is AddressResolutionResult.CoordinateOnly -> {
                    _uiState.value = _uiState.value.copy(
                        currentUserAddress = addressResult.coordinate.toDisplayText()
                    )
                }
                is AddressResolutionResult.Failed -> {
                    _uiState.value = _uiState.value.copy(currentUserAddress = "Mengambil alamat...")
                }
            }

            when (val current = getCurrentLocationUseCase()) {
                is CurrentLocationResult.Success -> {
                    _uiState.value = _uiState.value.copy(
                        currentUserLatitude = current.location.coordinate.latitude,
                        currentUserLongitude = current.location.coordinate.longitude
                    )
                }
                is CurrentLocationResult.Failure -> {
                    Log.w(TAG, "Failed to get display coordinates: $current")
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Unexpected error in updateDisplayLocation", e)
        }
    }

    /**
     * Handle work mode selection without mutating active monitoring geofences.
     */
    fun onWorkModeSelected(mode: WorkMode) {
        Log.d(TAG, "Work mode selected: ${mode.shortLabel}")

        _uiState.value = _uiState.value.copy(
            selectedWorkMode = mode,
            isWfaModeActive = mode == WorkMode.WFA
        )
        refreshResolvedActionState()

        if (mode == WorkMode.WFA) {
            _uiState.value = _uiState.value.copy(
                selectedWfaLocation = null,
                pickedLocation = null
            )
            onEnterPickOnMapMode()
            fetchWfaRecommendations()
        } else {
            onExitPickOnMapMode()
        }

        resolveAndApplyTargetForMode(mode)
    }

    private fun resolveAndApplyTargetForMode(mode: WorkMode = _uiState.value.selectedWorkMode) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isEvaluatingWorkMode = true,
                isButtonEnabled = false
            )

            val state = _uiState.value
            val target = resolveSelectedTargetLocationUseCase(
                mode = mode,
                wfoLocation = state.wfoLocation,
                wfhLocation = state.wfhLocation,
                approvedWfaLocation = state.approvedWfaLocation
            )

            val eligibility = evaluateWorkModeEligibilityUseCase(
                mode = mode,
                selectedTargetLocation = target,
                todayDate = state.todayStatus?.todayDate
            )

            val nextState = _uiState.value.copy(
                selectedTargetLocation = target,
                targetLocation = target.location,
                targetLocationMarker = target.location,
                workModeEligibility = eligibility,
                isEvaluatingWorkMode = false
            )
            _uiState.value = nextState.withResolvedActionStatePreservingInFlightSubmit()

            target.location?.let { animateMapToTarget(it) }
        }
    }

    private fun animateMapToTarget(location: Location, zoomLevel: Double = 15.0) {
        _mapCameraEffects.tryEmit(
            MapCameraEffect.Focus(
                id = nextMapCameraEffectId++,
                coordinate = location.coordinate,
                zoom = zoomLevel.toFloat()
            )
        )
    }

    /**
     * Fetch WFA recommendations based on current user location
     * Menggunakan GPS real-time, bukan lokasi WFH yang tersimpan
     */
    private fun fetchWfaRecommendations() {
        viewModelScope.launch {
            try {
                _uiState.value = _uiState.value.copy(isLoadingWfaRecommendations = true)
                val coordinate = when (val current = getCurrentLocationUseCase()) {
                    is CurrentLocationResult.Success -> current.location.coordinate
                    is CurrentLocationResult.Failure -> cachedCurrentCoordinateOrNull()
                }
                if (coordinate == null) {
                    _uiState.value = _uiState.value.copy(
                        wfaRecommendations = emptyList(),
                        isLoadingWfaRecommendations = false
                    )
                    return@launch
                }

                getWfaRecommendationsUseCase(
                    coordinate.latitude,
                    coordinate.longitude
                ).onSuccess { recommendations ->
                    _uiState.value = _uiState.value.copy(wfaRecommendations = recommendations)
                    if (recommendations.isNotEmpty()) {
                        _mapCameraEffects.tryEmit(
                            MapCameraEffect.Fit(
                                id = nextMapCameraEffectId++,
                                coordinates = recommendations.map(WfaRecommendation::coordinate)
                            )
                        )
                    }
                }.onFailure { exception ->
                    Log.e(TAG, "Failed to fetch WFA recommendations", exception)
                    _uiState.value = _uiState.value.copy(wfaRecommendations = emptyList())
                }
                _uiState.value = _uiState.value.copy(isLoadingWfaRecommendations = false)
            } catch (e: Exception) {
                Log.e(TAG, "Unexpected error in fetchWfaRecommendations", e)
                _uiState.value = _uiState.value.copy(
                    wfaRecommendations = emptyList(),
                    isLoadingWfaRecommendations = false
                )
            }
        }
    }

    /**
     * Handle WFA marker click - Updated to show marker details
     */
    fun onWfaMarkerClicked(recommendation: WfaRecommendation) {
        Log.d(TAG, "WFA Marker clicked: ${recommendation.name}")
        _uiState.value = _uiState.value.copy(
            selectedWfaLocation = recommendation,
            selectedWfaMarkerInfo = recommendation // Show marker details
        )
        refreshResolvedActionState()
    }

    /**
     * Handle WFA marker info dismissal
     */
    fun onDismissWfaMarkerInfo() {
        Log.d(TAG, "WFA marker info dialog dismissed")
        _uiState.value = _uiState.value.copy(selectedWfaMarkerInfo = null)
    }

    /**
     * Handle marker info dialog dismissal
     */
    fun onDismissMarkerInfo() {
        Log.d(TAG, "Marker info dialog dismissed")
        _uiState.value = _uiState.value.copy(
            selectedMarkerInfo = null,
            selectedWfaMarkerInfo = null // Also dismiss WFA marker info
        )
    }

    /**
     * Handle booking button click
     */
    fun onBookingClicked() {
        if (_uiState.value.isWfaModeActive) {
            _uiState.value.selectedWfaLocation?.let { wfaLocation ->
                Log.d(TAG, "Booking WFA location: ${wfaLocation.name}")
                // Navigate to WFA booking screen with location data (latitude and longitude only)
                val route = Screen.WfaBooking.createRoute(
                    latitude = wfaLocation.latitude,
                    longitude = wfaLocation.longitude
                    // address is no longer sent - WfaBookingViewModel will fetch it
                )
                viewModelScope.launch {
                    _uiState.value = _uiState.value.copy(
                        navigationTarget = NavigationTarget.WfaBooking(route)
                    )
                }
            } ?: run {
                Log.w(TAG, "Booking clicked in WFA mode but no location selected.")
            }
        } else {
            Log.d(TAG, "Booking clicked for mode: ${_uiState.value.selectedWorkMode.shortLabel}")
        }
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

        if (intent == null || !_uiState.value.isButtonEnabled) {
            Log.d(TAG, "Attendance button clicked but action is not ready: $currentActionState")
            return
        }

        Log.d(TAG, "Attendance button clicked - $intent")

        viewModelScope.launch {
            if (intent == AttendanceActionIntent.CHECK_IN && _uiState.value.isWfaModeActive) {
                val scheduleDateIso = _uiState.value.todayStatus?.todayDate
                if (scheduleDateIso.isNullOrBlank()) {
                    _uiState.value = _uiState.value.withActionState(
                        AttendanceActionState.Blocked(
                            reason = AttendanceBlockReason.WFA_BOOKING_REQUIRED,
                            title = "Booking WFA belum disetujui",
                            message = "Tanggal attendance hari ini tidak tersedia untuk memvalidasi booking WFA."
                        )
                    )
                    return@launch
                }

                resolveTodayApprovedWfaBookingIdUseCase(scheduleDateIso)
                    .onFailure {
                        _uiState.value = _uiState.value.withActionState(
                            AttendanceActionState.Blocked(
                                reason = AttendanceBlockReason.WFA_BOOKING_REQUIRED,
                                title = "Booking WFA belum disetujui",
                                message = "Booking WFA yang disetujui diperlukan sebelum absen dari lokasi WFA."
                            )
                        )
                    }
                    .getOrNull() ?: return@launch
            }

            val verifyingState = AttendanceActionState.VerifyingFace(intent)
            _uiState.value = _uiState.value.copy(
                navigationTarget = NavigationTarget.FaceScanner(intent)
            ).withActionState(verifyingState)
        }
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
            ?: _uiState.value.takeIf { it.isButtonEnabled }?.let { state ->
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

                val selectedMode = _uiState.value.selectedWorkMode
                val target = _uiState.value.selectedTargetLocation ?: resolveSelectedTargetLocationUseCase(
                    mode = selectedMode,
                    wfoLocation = _uiState.value.wfoLocation,
                    wfhLocation = _uiState.value.wfhLocation,
                    approvedWfaLocation = _uiState.value.approvedWfaLocation
                )
                val targetLocation = target.location

                if (targetLocation == null) {
                    val message = target.unavailableReason
                        ?: "Target location not available for ${selectedMode.shortLabel}. Please try again."
                    _uiState.value = _uiState.value.withActionState(
                        AttendanceActionState.Blocked(
                            reason = AttendanceBlockReason.TARGET_LOCATION_UNAVAILABLE,
                            title = "Target location tidak tersedia",
                            message = message
                        )
                    )
                    return@launch
                }

                val eligibility = _uiState.value.workModeEligibility ?: evaluateWorkModeEligibilityUseCase(
                    mode = selectedMode,
                    selectedTargetLocation = target,
                    todayDate = _uiState.value.todayStatus?.todayDate
                )

                if (!eligibility.canContinueToFaceVerification) {
                    _uiState.value = _uiState.value.withActionState(
                        AttendanceActionResolver.resolve(
                            _uiState.value.copy(workModeEligibility = eligibility)
                        )
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

                val bookingId = if (selectedMode == WorkMode.WFA) {
                    eligibility.approvedWfaBookingId ?: run {
                        val scheduleDateIso = _uiState.value.todayStatus?.todayDate
                        if (scheduleDateIso.isNullOrBlank()) {
                            val message = publishTransientFeedback(
                                AttendanceTransientFeedbackKind.ATTENDANCE_ERROR
                            ).message
                            _uiState.value = _uiState.value.withActionState(
                                AttendanceActionState.RetryableFailure(
                                    intent = intent,
                                    title = "Booking WFA belum disetujui",
                                    message = message
                                )
                            )
                            return@launch
                        }

                        resolveTodayApprovedWfaBookingIdUseCase(scheduleDateIso)
                            .getOrElse {
                                val message = publishTransientFeedback(
                                    AttendanceTransientFeedbackKind.ATTENDANCE_ERROR
                                ).message
                                _uiState.value = _uiState.value.withActionState(
                                    AttendanceActionState.RetryableFailure(
                                        intent = intent,
                                        title = "Booking WFA belum disetujui",
                                        message = message
                                    )
                                )
                                return@launch
                            }
                    }
                } else {
                    null
                }

                val attendanceRequest = try {
                    AttendanceCheckInRequestFactory.create(
                        workMode = selectedMode,
                        bookingId = bookingId
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

                // Call CheckInUseCase with both request and target location
                checkInUseCase(attendanceRequest, targetLocation).onSuccess { activeSession ->
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
     * Handle map marker click
     */
    fun onMarkerClicked(location: Location) {
        Log.d(TAG, "Marker clicked for location: ${location.description}")
        _uiState.value = _uiState.value.copy(selectedMarkerInfo = location)
    }

    /**
     * Handle focus location button click
     * This should ONLY focus on current user location, not work mode locations
     * Always gets fresh location data when clicked
     */
    fun onFocusLocationClicked() {
        viewModelScope.launch {
            try {
                when (val current = getCurrentLocationUseCase()) {
                    is CurrentLocationResult.Success -> {
                        val coordinate = current.location.coordinate
                        _uiState.value = _uiState.value.copy(
                            currentUserLatitude = coordinate.latitude,
                            currentUserLongitude = coordinate.longitude
                        )
                        _mapCameraEffects.tryEmit(
                            MapCameraEffect.Focus(
                                id = nextMapCameraEffectId++,
                                coordinate = coordinate,
                                zoom = 15f
                            )
                        )
                    }
                    is CurrentLocationResult.Failure -> {
                        Log.e(TAG, "Failed to get current GPS location: $current")
                        publishTransientFeedback(AttendanceTransientFeedbackKind.LOCATION_ERROR)
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "=== UNEXPECTED ERROR ===")
                Log.e(TAG, "Error in onFocusLocationClicked", e)
                publishTransientFeedback(AttendanceTransientFeedbackKind.LOCATION_ERROR)
            }
        }
    }

    private fun cachedCurrentCoordinateOrNull(): GeoCoordinate? {
        val latitude = _uiState.value.currentUserLatitude ?: return null
        val longitude = _uiState.value.currentUserLongitude ?: return null
        return runCatching { GeoCoordinate(latitude, longitude) }.getOrNull()
    }

    /**
     * Called when the map is ready to receive commands.
     */
    fun onMapReady() {
        Log.d(TAG, "Map is ready, focusing to selected target location")
        viewModelScope.launch {
            val location = _uiState.value.selectedTargetLocation?.location ?: _uiState.value.wfoLocation
            location?.let { target ->
                animateMapToTarget(target)
                Log.d(TAG, "Initial camera focus sent to selected target location")
            } ?: run {
                Log.w(TAG, "Target location not available for initial focus")
            }
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
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                selectedWfaLocation = WfaRecommendation(
                    name = location.placeName,
                    address = location.address,
                    latitude = location.latitude,
                    longitude = location.longitude,
                    score = 0.0, // Default score for manually selected location
                    label = "Manual Selection", // Default label for manually selected location
                    category = "Custom", // Default category for manually selected location
                    distance = 0.0 // Distance will be calculated based on current location
                ),
                selectedWorkMode = WorkMode.WFA,
                isWfaModeActive = true
            )
            refreshResolvedActionState()
        }
    }

    /**
     * Enter Pick on Map mode - enables crosshair and map interaction
     */
    fun onEnterPickOnMapMode() {
        _uiState.value = _uiState.value.copy(
            isPickOnMapModeActive = true,
            pickedLocation = null // Reset any previously picked location
        )
    }

    /**
     * Exit Pick on Map mode - disables crosshair and map interaction
     */
    fun onExitPickOnMapMode() {
        Log.d(TAG, "Exiting Pick on Map mode")
        _uiState.value = _uiState.value.copy(
            isPickOnMapModeActive = false,
            pickedLocation = null
        )
    }

    /**
     * Handle map idle event - called when user stops moving the map
     * Performs reverse geocoding for the center point of the map
     */
    fun onMapIdle(centerPoint: GeoCoordinate) {
        // Only perform reverse geocoding if Pick on Map mode is active
        if (!_uiState.value.isPickOnMapModeActive) return

        viewModelScope.launch {
            try {
                Log.d(
                    TAG,
                    "Map idle detected in Pick on Map mode: ${centerPoint.latitude}, ${centerPoint.longitude}"
                )

                // Perform reverse geocoding for the center point
                when (val result = reverseGeocodeUseCase(centerPoint)) {
                    is AddressResolutionResult.Resolved -> applyPickedLocation(
                        coordinate = centerPoint,
                        placeName = result.address.name ?: "Lokasi dipilih",
                        address = result.address.formattedAddress
                    )
                    is AddressResolutionResult.CoordinateOnly -> applyPickedLocation(
                        coordinate = result.coordinate,
                        placeName = "Lokasi dipilih",
                        address = result.coordinate.toDisplayText()
                    )
                    is AddressResolutionResult.Failed -> {
                        _uiState.value = _uiState.value.copy(
                            pickedLocation = null,
                            selectedWfaLocation = null,
                            error = "Gagal mendapatkan detail lokasi. Periksa koneksi Anda."
                        )
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Unexpected error in onMapIdle", e)
            }
        }
    }

    private fun applyPickedLocation(
        coordinate: GeoCoordinate,
        placeName: String,
        address: String
    ) {
        val locationResult = LocationResult(
            placeName = placeName,
            address = address,
            latitude = coordinate.latitude,
            longitude = coordinate.longitude
        )
        _uiState.value = _uiState.value.copy(
            pickedLocation = locationResult,
            selectedWfaLocation = WfaRecommendation(
                name = placeName,
                address = address,
                coordinate = coordinate,
                score = 0.0,
                label = "Picked on Map",
                category = "Manual Selection",
                distance = 0.0
            )
        )
    }

    private fun BookingHistoryItem.toAttendanceLocation(): Location? {
        val latitude = latitude ?: return null
        val longitude = longitude ?: return null
        val coordinate = runCatching { GeoCoordinate(latitude, longitude) }.getOrNull() ?: return null
        return Location(
            locationId = locationId ?: bookingId,
            coordinate = coordinate,
            radius = radiusMeters?.toInt() ?: 100,
            description = locationDescription,
            category = "WFA"
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

        // Removed onCleared geofence removal to keep geofence active until explicit checkout
    }
}
