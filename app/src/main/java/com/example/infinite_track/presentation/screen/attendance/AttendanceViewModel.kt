package com.example.infinite_track.presentation.screen.attendance

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.infinite_track.data.soucre.local.preferences.AttendancePreference
import com.example.infinite_track.domain.model.attendance.AttendanceRequestModel
import com.example.infinite_track.domain.model.attendance.Location
import com.example.infinite_track.domain.model.attendance.TodayStatus
import com.example.infinite_track.domain.model.location.LocationResult
import com.example.infinite_track.domain.model.wfa.WfaRecommendation
import com.example.infinite_track.domain.use_case.attendance.CheckInUseCase
import com.example.infinite_track.domain.use_case.attendance.CheckOutUseCase
import com.example.infinite_track.domain.use_case.attendance.GetTodayStatusUseCase
import com.example.infinite_track.domain.use_case.auth.GetLoggedInUserUseCase
import com.example.infinite_track.domain.use_case.location.GetCurrentAddressUseCase
import com.example.infinite_track.domain.use_case.location.GetCurrentCoordinatesUseCase
import com.example.infinite_track.domain.use_case.location.ReverseGeocodeUseCase
import com.example.infinite_track.domain.use_case.wfa.GetWfaRecommendationsUseCase
import com.example.infinite_track.presentation.geofencing.GeofenceManager
import com.example.infinite_track.presentation.navigation.Screen
import com.example.infinite_track.utils.LocationPermissionHelper
import com.example.infinite_track.utils.UiState
import com.mapbox.geojson.Point
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Sealed class untuk merepresentasikan target navigasi
 */
sealed class NavigationTarget {
    data class FaceScanner(val isCheckIn: Boolean) : NavigationTarget()
    data class WfaBooking(val route: String) : NavigationTarget()
    data class LocationSearch(val params: String) : NavigationTarget()
}

/**
 * Sealed class untuk merepresentasikan state dialog
 */
sealed class DialogState {
    data class Success(val message: String) : DialogState()
    data class Error(val message: String) : DialogState()
    data class LocationError(val message: String) : DialogState()
}

/**
 * Sealed class untuk map animation commands
 */
sealed class MapAnimationTarget {
    data class AnimateToLocation(val point: Point, val zoomLevel: Double) : MapAnimationTarget()
    data class AnimateToFitBounds(val points: List<Point>) : MapAnimationTarget()
    object ShowLocationError : MapAnimationTarget()
}

/**
 * Simplified state object focused on reactive geofence integration
 * Now supports WFO, WFH, and WFA location markers with Pick on Map functionality
 * Updated for state-driven architecture
 */
data class AttendanceScreenState(
    val uiState: UiState<Unit> = UiState.Loading,
    val todayStatus: TodayStatus? = null,
    val targetLocation: Location? = null, // Keep for backward compatibility
    val wfoLocation: Location? = null,    // Work From Office location
    val wfhLocation: Location? = null,    // Work From Home location
    val wfaRecommendations: List<WfaRecommendation> = emptyList(), // WFA recommendations
    val selectedWfaLocation: WfaRecommendation? = null, // Selected WFA location
    val selectedWfaMarkerInfo: WfaRecommendation? = null, // For showing WFA marker details
    val isWfaModeActive: Boolean = false, // Flag for WFA mode
    val isLoadingWfaRecommendations: Boolean = false, // Loading state for WFA recommendations
    val currentUserAddress: String = "",
    val currentUserLatitude: Double? = null,
    val currentUserLongitude: Double? = null,
    val isBookingEnabled: Boolean = false,
    val selectedWorkMode: String = "Work From Office",
    // Map-specific properties
    val targetLocationMarker: Location? = null,
    val selectedMarkerInfo: Location? = null,
    // Pick on Map properties
    val pickedLocation: LocationResult? = null, // Location picked by user on map
    val isPickOnMapModeActive: Boolean = false, // Flag for Pick on Map mode
    val error: String? = null, // Error message for network failures
    // Attendance button state
    val buttonText: String = "Loading...",
    val isButtonEnabled: Boolean = false,
    val isCheckInMode: Boolean = true, // Track whether we're in check-in or check-out mode

    // State-driven properties for navigation and dialogs
    val navigationTarget: NavigationTarget? = null, // Navigation commands
    val activeDialog: DialogState? = null, // Dialog state
    val mapAnimationTarget: MapAnimationTarget? = null // Map animation commands
)

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
    private val getCurrentCoordinatesUseCase: GetCurrentCoordinatesUseCase,
    private val getWfaRecommendationsUseCase: GetWfaRecommendationsUseCase,
    private val reverseGeocodeUseCase: ReverseGeocodeUseCase,
    private val attendancePreference: AttendancePreference,
    private val geofenceManager: GeofenceManager,
    private val getLoggedInUserUseCase: GetLoggedInUserUseCase,
    // Add UseCase dependencies for attendance operations
    private val checkInUseCase: CheckInUseCase,
    private val checkOutUseCase: CheckOutUseCase
) : ViewModel() {

    // Main UI state - now the SINGLE source of truth
    private val _uiState = MutableStateFlow(AttendanceScreenState())
    val uiState: StateFlow<AttendanceScreenState> = _uiState.asStateFlow()

    // Job for UI-focused location updates (display purposes only)
    private var displayLocationJob: Job? = null

    companion object {
        private const val TAG = "AttendanceViewModel"
        private const val DISPLAY_UPDATE_INTERVAL = 10000L // 10 seconds for UI updates
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

    /**
     * Called by UI after dialog is dismissed
     * Resets activeDialog to null
     */
    fun onDialogDismissed() {
        _uiState.value = _uiState.value.copy(activeDialog = null)
    }

    /**
     * Called by UI after map animation is completed
     * Resets mapAnimationTarget to null
     */
    fun onMapAnimationHandled() {
        _uiState.value = _uiState.value.copy(mapAnimationTarget = null)
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
                // Register reminder geofences for WFO/WFH if available (prefix with reminder:)
                _uiState.value.wfoLocation?.let { loc ->
                    geofenceManager.addReminderGeofence(
                        id = "reminder:" + loc.locationId,
                        latitude = loc.latitude,
                        longitude = loc.longitude,
                        radius = loc.radius.toFloat()
                    )
                }
                _uiState.value.wfhLocation?.let { loc ->
                    geofenceManager.addReminderGeofence(
                        id = "reminder:wfh:" + loc.locationId,
                        latitude = loc.latitude,
                        longitude = loc.longitude,
                        radius = loc.radius.toFloat()
                    )
                }

            } catch (e: Exception) {
                Log.e(TAG, "Error initializing data", e)
                _uiState.value = _uiState.value.copy(
                    uiState = UiState.Error("Failed to initialize attendance data: ${e.message}")
                )
            }
        }
    }

    /**
     * Fetch today status to get WFO location
     * FIXED: Now uses isButtonEnabled from calculateDynamicButtonState directly
     */
    private suspend fun fetchTodayStatus() {
        try {
            getTodayStatusUseCase().onSuccess { todayStatus ->
                Log.d(TAG, "Today status fetched successfully: $todayStatus")

                val isBookingEnabled = todayStatus.activeMode.isNotEmpty()
                val selectedMode = todayStatus.activeMode.ifEmpty { "Work From Office" }

                // Calculate button state based on today's status
                val (buttonText, isButtonEnabled, isCheckInMode) = calculateDynamicButtonState(
                    todayStatus
                )

                _uiState.value = _uiState.value.copy(
                    todayStatus = todayStatus,
                    targetLocation = todayStatus.activeLocation,
                    wfoLocation = todayStatus.activeLocation, // WFO location from today status
                    targetLocationMarker = todayStatus.activeLocation,
                    isBookingEnabled = isBookingEnabled,
                    selectedWorkMode = selectedMode,
                    // FIXED: Now uses isButtonEnabled from calculateDynamicButtonState
                    buttonText = buttonText,
                    isButtonEnabled = isButtonEnabled,
                    isCheckInMode = isCheckInMode,
                    uiState = UiState.Success(Unit)
                )

                // Setup geofence for active location (validation purposes)
                todayStatus.activeLocation?.let { location ->
                    setupGeofence(location)
                }

                // Send initial camera focus event to WFO location
                todayStatus.activeLocation?.let { location ->
                    val wfoPoint = Point.fromLngLat(location.longitude, location.latitude)
                    viewModelScope.launch {
                        _uiState.value = _uiState.value.copy(
                            mapAnimationTarget = MapAnimationTarget.AnimateToLocation(
                                wfoPoint,
                                15.0
                            )
                        )
                    }
                    Log.d(TAG, "Initial camera focus event sent to WFO location")
                }

                Log.d(TAG, "WFO location updated: ${todayStatus.activeLocation}")
                Log.d(
                    TAG,
                    "Button state updated: $buttonText, enabled: $isButtonEnabled, mode: $isCheckInMode"
                )

                // Register/refresh reminder geofences (WFO/WFH) after fetching status
                try {
                    todayStatus.activeLocation?.let { loc ->
                        geofenceManager.addReminderGeofence(
                            id = "reminder:" + loc.locationId,
                            latitude = loc.latitude,
                            longitude = loc.longitude,
                            radius = loc.radius.toFloat()
                        )
                    }
                    _uiState.value.wfhLocation?.let { loc ->
                        geofenceManager.addReminderGeofence(
                            id = "reminder:wfh:" + loc.locationId,
                            latitude = loc.latitude,
                            longitude = loc.longitude,
                            radius = loc.radius.toFloat()
                        )
                    }
                } catch (e: Exception) {
                    Log.w(TAG, "Failed to add reminder geofences", e)
                }

            }.onFailure { exception ->
                Log.e(TAG, "Failed to fetch today status", exception)
                _uiState.value = _uiState.value.copy(
                    uiState = UiState.Error("Failed to load attendance status: ${exception.message}"),
                    buttonText = "Error",
                    isButtonEnabled = false
                )
            }
        } catch (e: Exception) {
            Log.e(TAG, "Unexpected error in fetchTodayStatus", e)
            _uiState.value = _uiState.value.copy(
                uiState = UiState.Error("Unexpected error: ${e.message}"),
                buttonText = "Error",
                isButtonEnabled = false
            )
        }
    }

    /**
     * Calculate dynamic button state based on today's status
     * Returns Triple(buttonText, isEnabled, isCheckInMode)
     * FIXED: Tombol selalu aktif kecuali sudah selesai absensi - validasi lokasi diserahkan ke backend
     */
    private fun calculateDynamicButtonState(todayStatus: TodayStatus): Triple<String, Boolean, Boolean> {
        return when {
            // PRIORITAS 1: Belum check-in sama sekali (checked_in_at == null)
            // Selalu aktif - biarkan backend yang validasi lokasi
            todayStatus.checkedInAt == null -> {
                Triple("Check-in di sini", true, true)
            }

            // PRIORITAS 2: Sudah check-in, bisa check-out (checked_in_at != null && can_check_out == true)
            // Selalu aktif - biarkan backend yang validasi lokasi
            todayStatus.checkedInAt != null && todayStatus.canCheckOut -> {
                Triple("Check-out di sini", true, false)
            }

            // PRIORITAS 3: Sudah selesai absensi hari ini
            // Hanya kondisi ini yang tombolnya nonaktif
            else -> {
                Triple("Anda sudah absen hari ini", false, false)
            }
        }
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

                Log.d(TAG, "WFH location updated: $wfhLocation")
            }
        } catch (e: Exception) {
            Log.w(TAG, "Unexpected error in fetchUserHomeLocation", e)
            // Continue without WFH location
        }
    }

    /**
     * Setup geofence for validation (background monitoring)
     * UPDATED: Enhanced with permission checking and user feedback
     */
    private fun setupGeofence(location: Location) {
        try {
            Log.d(TAG, "Setting up geofence for location: ${location.description}")
            Log.d(
                TAG,
                "Location details - ID: ${location.locationId}, Lat: ${location.latitude}, Lng: ${location.longitude}, Radius: ${location.radius}m"
            )

            // Check if all required permissions are granted
            if (!geofenceManager.hasAllRequiredPermissions()) {
                Log.w(TAG, "Missing location permissions for geofencing")
                
                // Determine which permission is missing
                val permissionResult = when {
                    !geofenceManager.hasForegroundLocationPermission() -> 
                        LocationPermissionHelper.PermissionResult.ForegroundPermissionDenied
                    !geofenceManager.hasBackgroundLocationPermission() -> 
                        LocationPermissionHelper.PermissionResult.BackgroundPermissionDenied
                    else -> LocationPermissionHelper.PermissionResult.PermanentlyDenied
                }
                
                _uiState.value = _uiState.value.copy(
                    showPermissionDialog = true,
                    permissionResult = permissionResult,
                    permissionMessage = geofenceManager.getPermissionStatusMessage()
                )
                return
            }

            geofenceManager.addGeofence(
                id = location.locationId.toString(),
                latitude = location.latitude,
                longitude = location.longitude,
                radius = location.radius.toFloat(),
                onPermissionError = { errorMessage ->
                    Log.e(TAG, "Permission error during geofence setup: $errorMessage")
                    _uiState.value = _uiState.value.copy(
                        showPermissionDialog = true,
                        permissionMessage = errorMessage
                    )
                }
            )

            Log.d(TAG, "Geofence setup request sent for location: ${location.description}")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to setup geofence for location: ${location.description}", e)
        }
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
            getCurrentAddressUseCase().onSuccess { address ->
                _uiState.value = _uiState.value.copy(currentUserAddress = address)
                Log.d(TAG, "Display address updated: $address")
            }.onFailure { exception ->
                Log.w(TAG, "Failed to get display address", exception)
                // Set fallback address jika gagal
                _uiState.value = _uiState.value.copy(currentUserAddress = "Mengambil alamat...")
            }

            // Update current coordinates for map display - menggunakan database fallback untuk display
            getCurrentCoordinatesUseCase(useRealTimeGPS = false).onSuccess { coordinates ->
                val (latitude, longitude) = coordinates
                _uiState.value = _uiState.value.copy(
                    currentUserLatitude = latitude,
                    currentUserLongitude = longitude
                )
                Log.d(TAG, "Display coordinates updated: $latitude, $longitude")
            }.onFailure { exception ->
                Log.w(TAG, "Failed to get display coordinates", exception)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Unexpected error in updateDisplayLocation", e)
        }
    }

    /**
     * Handle work mode selection with Pick on Map integration
     * UPDATED: Added geofence cleanup when switching modes
     */
    fun onWorkModeSelected(mode: String) {
        Log.d(TAG, "Work mode selected: $mode")

        // Clean up any existing geofence before switching modes
        Log.d(TAG, "Cleaning up geofences before mode switch...")
        geofenceManager.removeAllGeofences()

        _uiState.value = _uiState.value.copy(
            selectedWorkMode = mode,
            isWfaModeActive = mode == "WFA" || mode == "Work From Anywhere"
        )

        viewModelScope.launch {
            when (mode) {
                "WFA", "Work From Anywhere" -> {
                    // Reset previous WFA selection and enter Pick on Map mode
                    _uiState.value = _uiState.value.copy(
                        selectedWfaLocation = null,
                        pickedLocation = null
                    )
                    onEnterPickOnMapMode()
                    fetchWfaRecommendations()
                }

                "Work From Home", "WFH" -> {
                    onExitPickOnMapMode()
                    _uiState.value.wfhLocation?.let { wfhLocation ->
                        // Setup geofence for WFH location
                        setupGeofence(wfhLocation)

                        val wfhPoint = Point.fromLngLat(wfhLocation.longitude, wfhLocation.latitude)
                        _uiState.value = _uiState.value.copy(
                            mapAnimationTarget = MapAnimationTarget.AnimateToLocation(
                                point = wfhPoint,
                                zoomLevel = 15.0
                            )
                        )
                        Log.d(TAG, "Auto-focusing camera to WFH location and setting up geofence")
                    }
                }

                "Work From Office", "WFO" -> {
                    onExitPickOnMapMode()
                    _uiState.value.wfoLocation?.let { wfoLocation ->
                        // Setup geofence for WFO location
                        setupGeofence(wfoLocation)

                        val wfoPoint = Point.fromLngLat(wfoLocation.longitude, wfoLocation.latitude)
                        _uiState.value = _uiState.value.copy(
                            mapAnimationTarget = MapAnimationTarget.AnimateToLocation(
                                point = wfoPoint,
                                zoomLevel = 15.0
                            )
                        )
                        Log.d(TAG, "Auto-focusing camera to WFO location and setting up geofence")
                    }
                }
            }
        }
    }

    /**
     * Fetch WFA recommendations based on current user location
     * Menggunakan GPS real-time, bukan lokasi WFH yang tersimpan
     */
    private fun fetchWfaRecommendations() {
        viewModelScope.launch {
            try {
                Log.d(TAG, "Fetching WFA recommendations - getting fresh GPS location...")

                // Set loading state for WFA recommendations
                _uiState.value = _uiState.value.copy(isLoadingWfaRecommendations = true)

                // PENTING: Gunakan GPS real-time untuk WFA recommendations
                getCurrentCoordinatesUseCase(useRealTimeGPS = true).onSuccess { coordinates ->
                    val (lat, lng) = coordinates

                    Log.d(TAG, "Using GPS real-time location for WFA: $lat, $lng")

                    getWfaRecommendationsUseCase(lat, lng).onSuccess { recommendations ->
                        _uiState.value = _uiState.value.copy(wfaRecommendations = recommendations)

                        // Send event to zoom out and show all recommendations
                        if (recommendations.isNotEmpty()) {
                            val points =
                                recommendations.map { Point.fromLngLat(it.longitude, it.latitude) }
                            _uiState.value = _uiState.value.copy(
                                mapAnimationTarget = MapAnimationTarget.AnimateToFitBounds(points)
                            )
                            Log.d(
                                TAG,
                                "WFA recommendations fetched: ${recommendations.size} locations"
                            )
                        } else {
                            Log.w(TAG, "No WFA recommendations found for GPS location: $lat, $lng")
                        }
                    }.onFailure { exception ->
                        Log.e(TAG, "Failed to fetch WFA recommendations", exception)
                        // Keep empty list on error
                        _uiState.value = _uiState.value.copy(wfaRecommendations = emptyList())
                    }

                }.onFailure { exception ->
                    Log.e(TAG, "Failed to get GPS location for WFA recommendations", exception)
                    // Fallback to cached location if GPS fails
                    val lat = _uiState.value.currentUserLatitude ?: return@launch
                    val lng = _uiState.value.currentUserLongitude ?: return@launch

                    Log.w(TAG, "GPS failed, using cached location for WFA: $lat, $lng")

                    getWfaRecommendationsUseCase(lat, lng).onSuccess { recommendations ->
                        _uiState.value = _uiState.value.copy(wfaRecommendations = recommendations)
                        if (recommendations.isNotEmpty()) {
                            val points =
                                recommendations.map { Point.fromLngLat(it.longitude, it.latitude) }
                            _uiState.value = _uiState.value.copy(
                                mapAnimationTarget = MapAnimationTarget.AnimateToFitBounds(points)
                            )
                            Log.d(
                                TAG,
                                "WFA recommendations fetched with cached location: ${recommendations.size} locations"
                            )
                        }
                    }.onFailure { exception ->
                        Log.e(
                            TAG,
                            "Failed to fetch WFA recommendations with cached location",
                            exception
                        )
                        _uiState.value = _uiState.value.copy(wfaRecommendations = emptyList())
                    }
                }

                // Reset loading state
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

        // Focus camera on selected WFA location
        viewModelScope.launch {
            val point = Point.fromLngLat(recommendation.longitude, recommendation.latitude)
            _uiState.value = _uiState.value.copy(
                mapAnimationTarget = MapAnimationTarget.AnimateToLocation(
                    point,
                    16.0
                )
            ) // Zoom closer for selected marker
        }
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
            Log.d(TAG, "Booking clicked for mode: ${_uiState.value.selectedWorkMode}")
            // TODO: Implement booking logic for WFO/WFH
        }
    }

    /**
     * Handle attendance button click - navigates to face scanner
     * FIXED: Uses new Screen.FaceScanner.createRoute with action parameter
     */
    fun onAttendanceButtonClicked() {
        // Use the reactive StateFlow value instead of recalculating
        val isEnabled = _uiState.value.isButtonEnabled
        val buttonText = _uiState.value.buttonText

        if (!isEnabled) {
            Log.d(TAG, "Attendance button clicked but not enabled (geofence or server restriction)")
            return
        }

        val isCheckIn = buttonText.contains("Check-in", ignoreCase = true)
        val action = if (isCheckIn) "checkin" else "checkout"

        Log.d(TAG, "Attendance button clicked - $action")

        // Update isCheckInMode state before navigation
        _uiState.value = _uiState.value.copy(isCheckInMode = isCheckIn)

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                navigationTarget = NavigationTarget.FaceScanner(isCheckIn)
            )
        }
    }

    /**
     * Handle face verification result - GATEWAY after face verification
     * Called from FaceScannerScreen when verification is complete
     */
    fun onFaceVerificationResult(isSuccess: Boolean) {
        Log.d(TAG, "Face verification result: $isSuccess")

        if (!isSuccess) {
            Log.d(TAG, "Face verification failed - aborting attendance process")
            _uiState.value = _uiState.value.copy(
                activeDialog = DialogState.Error("Verifikasi wajah gagal. Silakan coba lagi.")
            )
            return
        }

        // Check current mode and proceed accordingly
        if (_uiState.value.isCheckInMode) {
            proceedWithCheckIn()
        } else {
            proceedWithCheckOut()
        }
    }

    /**
     * Proceed with check-in after successful face verification
     * FIXED: Added proper error message extraction from server response
     */
    private fun proceedWithCheckIn() {
        viewModelScope.launch {
            try {
                Log.d(TAG, "Proceeding with check-in after face verification")

                // Clear any previous error
                _uiState.value = _uiState.value.copy(activeDialog = null)

                // Determine target location based on selected work mode
                val targetLocation = when (_uiState.value.selectedWorkMode) {
                    "Work From Home", "WFH" -> _uiState.value.wfhLocation
                    "Work From Office", "WFO" -> _uiState.value.wfoLocation
                    "WFA", "Work From Anywhere" -> {
                        // For WFA, convert selectedWfaLocation to Location object
                        _uiState.value.selectedWfaLocation?.let { wfaLocation ->
                            Location(
                                locationId = 0, // WFA locations don't have fixed IDs
                                latitude = wfaLocation.latitude,
                                longitude = wfaLocation.longitude,
                                radius = 100, // Default radius for WFA
                                description = wfaLocation.name,
                                category = wfaLocation.category
                            )
                        }
                    }

                    else -> _uiState.value.wfoLocation // Default to WFO
                }

                if (targetLocation == null) {
                    _uiState.value = _uiState.value.copy(
                        activeDialog = DialogState.Error("Target location not available for ${_uiState.value.selectedWorkMode}. Please try again.")
                    )
                    return@launch
                }

                // Get user info for the request
                getLoggedInUserUseCase().collect { user ->
                    if (user == null) {
                        _uiState.value = _uiState.value.copy(
                            activeDialog = DialogState.Error("User information not available. Please try again.")
                        )
                        return@collect
                    }

                    // Map work mode to correct category_id
                    val categoryId = when (_uiState.value.selectedWorkMode) {
                        "Work From Office", "WFO" -> 1  // Work From Office
                        "Work From Home", "WFH" -> 2    // Work From Home
                        "WFA", "Work From Anywhere" -> 3 // Work From Anywhere
                        else -> 1 // Default to WFO
                    }

                    // Create attendance request model with proper parameters
                    val attendanceRequest = AttendanceRequestModel(
                        categoryId = categoryId, // Use correct category ID based on work mode
                        latitude = 0.0, // Will be updated by UseCase with real-time GPS
                        longitude = 0.0, // Will be updated by UseCase with real-time GPS
                        notes = "Check-in via mobile app",
                        bookingId = null, // No booking for regular check-in
                        type = "checkin"
                    )

                    // Call CheckInUseCase with both request and target location
                    checkInUseCase(attendanceRequest, targetLocation).onSuccess { activeSession ->
                        Log.d(TAG, "Check-in successful: $activeSession")

                        // Refresh today's status to get updated data
                        fetchTodayStatus()

                        // Send success event to UI with appropriate message
                        _uiState.value = _uiState.value.copy(
                            activeDialog = DialogState.Success("Check-in berhasil! Selamat bekerja hari ini.")
                        )

                    }.onFailure { exception ->
                        Log.e(TAG, "Check-in failed: ${exception.message}", exception)

                        // Extract the actual error message from the exception
                        val errorMessage = exception.message ?: "Check-in gagal. Silakan coba lagi."

                        // Send error event to UI with the actual server message
                        _uiState.value = _uiState.value.copy(
                            activeDialog = DialogState.Error(errorMessage)
                        )
                    }
                }

            } catch (e: Exception) {
                Log.e(TAG, "Error in proceedWithCheckIn", e)
                _uiState.value = _uiState.value.copy(
                    activeDialog = DialogState.Error("Unexpected error during check-in: ${e.message}")
                )
            }
        }
    }

    /**
     * Proceed with check-out after successful face verification
     * FIXED: Added proper error message extraction from server response
     */
    private fun proceedWithCheckOut() {
        viewModelScope.launch {
            try {
                Log.d(TAG, "Proceeding with check-out after face verification")

                // Clear any previous error
                _uiState.value = _uiState.value.copy(activeDialog = null)

                // Call CheckOutUseCase - it will handle everything internally
                checkOutUseCase().onSuccess { activeSession ->
                    Log.d(TAG, "Check-out successful: $activeSession")

                    fetchTodayStatus()

                    // Send success event to UI with appropriate message
                    _uiState.value = _uiState.value.copy(
                        activeDialog = DialogState.Success("Check-out berhasil! Terima kasih atas kerja keras Anda hari ini.")
                    )

                }.onFailure { exception ->
                    Log.e(TAG, "Check-out failed: ${exception.message}", exception)

                    // Extract the actual error message from the exception
                    val errorMessage = exception.message ?: "Check-out gagal. Silakan coba lagi."

                    // Send error event to UI with the actual server message
                    _uiState.value = _uiState.value.copy(
                        activeDialog = DialogState.Error(errorMessage)
                    )
                }

            } catch (e: Exception) {
                Log.e(TAG, "Error in proceedWithCheckOut", e)
                _uiState.value = _uiState.value.copy(
                    activeDialog = DialogState.Error("Unexpected error during check-out: ${e.message}")
                )
            }
        }
    }

    /**
     * Clear error message
     */
    fun clearError() {
        _uiState.value = _uiState.value.copy(activeDialog = null)
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
        Log.d(TAG, "=== FOCUS LOCATION BUTTON CLICKED ===")
        Log.d(TAG, "Current work mode: ${_uiState.value.selectedWorkMode}")
        Log.d(TAG, "Current WFH location: ${_uiState.value.wfhLocation}")
        Log.d(TAG, "Getting fresh GPS coordinates...")

        viewModelScope.launch {
            try {
                // PENTING: Gunakan GPS real-time, bukan dari database
                getCurrentCoordinatesUseCase(useRealTimeGPS = true).onSuccess { coordinates ->
                    val (latitude, longitude) = coordinates

                    Log.d(TAG, "=== FRESH GPS COORDINATES RECEIVED ===")
                    Log.d(TAG, "GPS Real-time Latitude: $latitude")
                    Log.d(TAG, "GPS Real-time Longitude: $longitude")
                    Log.d(TAG, "WFH Latitude: ${_uiState.value.wfhLocation?.latitude}")
                    Log.d(TAG, "WFH Longitude: ${_uiState.value.wfhLocation?.longitude}")

                    // Pastikan koordinat berbeda dari WFH
                    if (latitude == _uiState.value.wfhLocation?.latitude &&
                        longitude == _uiState.value.wfhLocation?.longitude
                    ) {
                        Log.w(TAG, "WARNING: GPS coordinates sama dengan WFH location!")
                        Log.w(TAG, "Ini mungkin karena GPS masih menggunakan cached location")
                    }

                    // Update state untuk immediate display
                    _uiState.value = _uiState.value.copy(
                        currentUserLatitude = latitude,
                        currentUserLongitude = longitude
                    )

                    // Send map animation event
                    val focusPoint = Point.fromLngLat(longitude, latitude)
                    _uiState.value = _uiState.value.copy(
                        mapAnimationTarget = MapAnimationTarget.AnimateToLocation(
                            point = focusPoint,
                            zoomLevel = 15.0
                        )
                    )

                    Log.d(TAG, "=== MAP ANIMATION SENT ===")
                    Log.d(TAG, "Focus point: ${focusPoint.latitude()}, ${focusPoint.longitude()}")
                    Log.d(TAG, "This should be your CURRENT GPS location, NOT your home location!")
                }.onFailure { exception ->
                    Log.e(TAG, "=== GPS LOCATION FAILED ===")
                    Log.e(TAG, "Failed to get current GPS location: ${exception.message}")
                    _uiState.value = _uiState.value.copy(
                        mapAnimationTarget = MapAnimationTarget.ShowLocationError
                    )
                }
            } catch (e: Exception) {
                Log.e(TAG, "=== UNEXPECTED ERROR ===")
                Log.e(TAG, "Error in onFocusLocationClicked: ${e.message}")
                _uiState.value = _uiState.value.copy(
                    mapAnimationTarget = MapAnimationTarget.ShowLocationError
                )
            }
        }
    }

    /**
     * Called when the map is ready to receive commands
     * This will trigger initial camera focus to WFO location
     */
    fun onMapReady() {
        Log.d(TAG, "Map is ready, focusing to WFO location")
        viewModelScope.launch {
            _uiState.value.wfoLocation?.let { wfoLocation ->
                val wfoPoint = Point.fromLngLat(wfoLocation.longitude, wfoLocation.latitude)
                _uiState.value = _uiState.value.copy(
                    mapAnimationTarget = MapAnimationTarget.AnimateToLocation(
                        point = wfoPoint,
                        zoomLevel = 15.0
                    )
                )
                Log.d(TAG, "Initial camera focus sent to WFO location")
            } ?: run {
                Log.w(TAG, "WFO location not available for initial focus")
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
                isWfaModeActive = true
            )

            // Animate map to the selected location
            _uiState.value = _uiState.value.copy(
                mapAnimationTarget = MapAnimationTarget.AnimateToLocation(
                    Point.fromLngLat(location.longitude, location.latitude),
                    15.0
                )
            )
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
    fun onMapIdle(centerPoint: Point) {
        // Only perform reverse geocoding if Pick on Map mode is active
        if (!_uiState.value.isPickOnMapModeActive) return

        viewModelScope.launch {
            try {
                Log.d(
                    TAG,
                    "Map idle detected in Pick on Map mode: ${centerPoint.latitude()}, ${centerPoint.longitude()}"
                )

                // Perform reverse geocoding for the center point
                reverseGeocodeUseCase(
                    latitude = centerPoint.latitude(),
                    longitude = centerPoint.longitude()
                ).onSuccess { locationResult ->
                    Log.d(TAG, "Reverse geocoding successful: ${locationResult.placeName}")

                    // Update the picked location
                    _uiState.value = _uiState.value.copy(
                        pickedLocation = locationResult,
                        selectedWfaLocation = WfaRecommendation(
                            name = locationResult.placeName,
                            address = locationResult.address,
                            latitude = locationResult.latitude,
                            longitude = locationResult.longitude,
                            score = 0.0,
                            label = "Picked on Map",
                            category = "Manual Selection",
                            distance = 0.0
                        )
                    )
                }.onFailure { exception ->
                    Log.e(TAG, "Reverse geocoding failed", exception)

                    _uiState.value = _uiState.value.copy(
                        pickedLocation = null, // Ensure picked location is null
                        selectedWfaLocation = null,
                        error = "Gagal mendapatkan detail lokasi. Periksa koneksi Anda."
                    )
                }
            } catch (e: Exception) {
                Log.e(TAG, "Unexpected error in onMapIdle", e)
            }
        }
    }

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

        // If permissions are granted, retry geofence setup
        if (result == LocationPermissionHelper.PermissionResult.AllPermissionsGranted) {
            // Retry setting up geofence for current selected location
            when (_uiState.value.selectedWorkMode) {
                "Work From Home", "WFH" -> _uiState.value.wfhLocation?.let { setupGeofence(it) }
                "Work From Office", "WFO" -> _uiState.value.wfoLocation?.let { setupGeofence(it) }
                else -> _uiState.value.selectedWfaLocation?.let { wfaLocation ->
                    val location = Location(
                        locationId = 0, // WFA doesn't have fixed ID
                        latitude = wfaLocation.latitude,
                        longitude = wfaLocation.longitude,
                        radius = 100, // Default radius for WFA
                        description = wfaLocation.name,
                        category = wfaLocation.category
                    )
                    setupGeofence(location)
                }
            }
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