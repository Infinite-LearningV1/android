package com.example.infinite_track.presentation.screen.attendance

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material3.BottomSheetScaffold
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.rememberBottomSheetScaffoldState
import androidx.compose.material3.rememberStandardBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import androidx.navigation.compose.currentBackStackEntryAsState
import com.example.infinite_track.R
import com.example.infinite_track.domain.model.attendance.WorkMode
import com.example.infinite_track.domain.model.location.LocationResult
import com.example.infinite_track.domain.model.wfa.WfaRecommendation
import com.example.infinite_track.presentation.components.button.attendance.AttendanceBottomSheetContent
import com.example.infinite_track.presentation.components.empty.ErrorAnimation
import com.example.infinite_track.presentation.components.loading.LoadingAnimation
import com.example.infinite_track.presentation.components.maps.AttendanceMap
import com.example.infinite_track.presentation.components.maps.MarkerView
import com.example.infinite_track.presentation.components.dialog.LocationPermissionDialog
import com.example.infinite_track.utils.LocalLocationPermissionHelper
import com.example.infinite_track.utils.LocationPermissionHelper
import com.example.infinite_track.presentation.components.maps.MarkerViewWfa
import com.example.infinite_track.presentation.components.status.InfiniteTrackStatusDialog
import com.example.infinite_track.presentation.components.status.StatusStates
import com.example.infinite_track.presentation.navigation.Screen
import com.example.infinite_track.presentation.screen.attendance.components.AttendanceTopBar
import com.example.infinite_track.presentation.theme.Infinite_TrackTheme
import com.example.infinite_track.utils.UiState
import com.mapbox.geojson.Point
import com.mapbox.maps.CameraOptions
import com.mapbox.maps.MapView
import com.mapbox.maps.MapboxDelicateApi
import com.mapbox.maps.plugin.animation.MapAnimationOptions
import com.mapbox.maps.plugin.animation.flyTo

@OptIn(ExperimentalMaterial3Api::class, MapboxDelicateApi::class)
@Composable
fun AttendanceScreen(
    navController: NavController,
    viewModel: AttendanceViewModel = hiltViewModel()
) {
    var mapViewInstance by remember { mutableStateOf<MapView?>(null) }

    // Observasi state dari ViewModel yang sudah disederhanakan
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val locationPermissionHelper = LocalLocationPermissionHelper.current

    // Handle hasil pencarian lokasi dari LocationSearchScreen
    val selectedLocation = navController.currentBackStackEntry
        ?.savedStateHandle
        ?.get<LocationResult>("selected_location")

    // Process hasil pencarian lokasi
    LaunchedEffect(selectedLocation) {
        selectedLocation?.let { location ->
            // Kirim lokasi terpilih ke ViewModel untuk diproses
            viewModel.onLocationSelected(location)
            // Hapus state agar tidak diproses lagi saat re-komposisi
            navController.currentBackStackEntry
                ?.savedStateHandle
                ?.remove<LocationResult>("selected_location")
        }
    }

    // BottomSheet state
    val bottomSheetState = rememberStandardBottomSheetState(
        skipHiddenState = false
    )
    val scaffoldState = rememberBottomSheetScaffoldState(
        bottomSheetState = bottomSheetState
    )

    // =======================================================
    // NEW: State-driven LaunchedEffect for Navigation
    // =======================================================
    LaunchedEffect(uiState.navigationTarget) {
        uiState.navigationTarget?.let { target ->
            when (target) {
                is NavigationTarget.FaceScanner -> {
                    val action = if (target.isCheckIn) "checkin" else "checkout"
                    val route = Screen.FaceScanner.createRoute(action)
                    navController.navigate(route)
                    android.util.Log.d(
                        "AttendanceScreen",
                        "Navigating to face scanner for $action"
                    )
                }
                is NavigationTarget.WfaBooking -> {
                    navController.navigate(target.route)
                    android.util.Log.d(
                        "AttendanceScreen",
                        "Navigating to WFA booking screen with route: ${target.route}"
                    )
                }
                is NavigationTarget.LocationSearch -> {
                    navController.navigate(target.params)
                    android.util.Log.d(
                        "AttendanceScreen",
                        "Navigating to location search with params: ${target.params}"
                    )
                }
            }
            // Notify ViewModel that navigation has been handled
            viewModel.onNavigationHandled()
        }
    }

    // =======================================================
    // NEW: State-driven LaunchedEffect for Map Animation
    // =======================================================
    LaunchedEffect(uiState.mapAnimationTarget) {
        uiState.mapAnimationTarget?.let { animationTarget ->
            when (animationTarget) {
                is MapAnimationTarget.AnimateToLocation -> {
                    mapViewInstance?.let { mapView ->
                        val cameraOptions = CameraOptions.Builder()
                            .center(animationTarget.point)
                            .zoom(animationTarget.zoomLevel)
                            .pitch(0.0)
                            .bearing(0.0)
                            .build()

                        mapView.mapboxMap.flyTo(
                            cameraOptions,
                            MapAnimationOptions.Builder()
                                .duration(1200L)
                                .build()
                        )

                        android.util.Log.d(
                            "AttendanceScreen",
                            "Camera animated to ${animationTarget.point.latitude()}, ${animationTarget.point.longitude()} with zoom ${animationTarget.zoomLevel}"
                        )
                    }
                }

                is MapAnimationTarget.AnimateToFitBounds -> {
                    mapViewInstance?.let { mapView ->
                        if (animationTarget.points.isNotEmpty()) {
                            val cameraOptions = mapView.mapboxMap.cameraForCoordinates(
                                coordinates = animationTarget.points,
                                camera = CameraOptions.Builder().build(),
                                coordinatesPadding = com.mapbox.maps.EdgeInsets(
                                    50.0,
                                    50.0,
                                    50.0,
                                    50.0
                                ),
                                maxZoom = null,
                                offset = null
                            )

                            mapView.mapboxMap.flyTo(
                                cameraOptions,
                                MapAnimationOptions.Builder()
                                    .duration(1500L)
                                    .build()
                            )

                            android.util.Log.d(
                                "AttendanceScreen",
                                "Camera animated to fit ${animationTarget.points.size} WFA locations"
                            )
                        }
                    }
                }

                is MapAnimationTarget.ShowLocationError -> {
                    android.util.Log.e(
                        "AttendanceScreen",
                        "Failed to get current location for focus"
                    )
                    // Could show a Toast or Snackbar here
                }
            }
            // Notify ViewModel that map animation has been handled
            viewModel.onMapAnimationHandled()
        }
    }

    // Start location updates when UI is ready and data is loaded successfully
    LaunchedEffect(uiState.uiState) {
        if (uiState.uiState is UiState.Success) {
            // Only start location updates after data is loaded and UI is ready
            viewModel.startLocationUpdates()
            android.util.Log.d("AttendanceScreen", "Location updates started after UI ready")
        }
    }

    // Penanganan state utama berdasarkan UiState dengan smart cast fix
    when (val currentUiState = uiState.uiState) {
        is UiState.Idle -> {
            // Initial idle state - could show a splash or continue to loading
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                LoadingAnimation()
            }
        }

        is UiState.Loading -> {
            // Tampilkan loading animation
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                LoadingAnimation()
            }
        }

        is UiState.Error -> {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    ErrorAnimation()
                    Text(
                        text = currentUiState.errorMessage,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.error,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(16.dp)
                    )
                }
            }
        }

        is UiState.Success -> {
            // Tampilkan konten utama dengan BottomSheet
            BottomSheetScaffold(
                scaffoldState = scaffoldState,
                containerColor = Color.Black.copy(alpha = 0.1f),
                contentColor = Color.Transparent,
                sheetContainerColor = Color.Transparent,
                sheetContentColor = Color.Unspecified,
                sheetShape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
                sheetPeekHeight = 120.dp,
                sheetDragHandle = null,
                sheetContent = {
                    // Enhanced Liquid Glass Background Container
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
                            .background(
                                brush = Brush.linearGradient(
                                    colors = listOf(
                                        Color.White.copy(alpha = 0.98f),
                                        Color.White.copy(alpha = 0.95f),
                                        Color(0xFFE3F2FD).copy(alpha = 0.92f),
                                        Color(0xFFBBDEFB).copy(alpha = 0.88f)
                                    )
                                )
                            )
                    ) {
                        // Glass effect overlays
                        Box(
                            modifier = Modifier
                                .matchParentSize()
                                .background(
                                    brush = Brush.radialGradient(
                                        colors = listOf(
                                            Color.White.copy(alpha = 0.6f),
                                            Color.Transparent,
                                            Color(0xFF81D4FA).copy(alpha = 0.3f)
                                        ),
                                        radius = 1000f
                                    )
                                )
                                .blur(2.dp)
                        )

                        Box(
                            modifier = Modifier
                                .matchParentSize()
                                .background(
                                    brush = Brush.linearGradient(
                                        colors = listOf(
                                            Color.Transparent,
                                            Color.White.copy(alpha = 0.4f),
                                            Color.Transparent
                                        )
                                    )
                                )
                        )

                        // Content with drag handle
                        Column(modifier = Modifier.fillMaxWidth()) {
                            // Custom drag handle
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 12.dp, bottom = 8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Box(
                                    modifier = Modifier
                                        .width(40.dp)
                                        .height(4.dp)
                                        .clip(RoundedCornerShape(2.dp))
                                        .background(Color.Gray.copy(alpha = 0.3f))
                                )
                            }

                            AttendanceBottomSheetContent(
                                modifier = Modifier.padding(top = 0.dp),
                                targetLocationInfo = uiState.selectedTargetLocation,
                                currentLocationAddress = uiState.currentUserAddress.ifEmpty { "Mengambil lokasi saat ini..." },
                                selectedWorkMode = uiState.selectedWorkMode,
                                isBookingEnabled = uiState.selectedWorkMode == WorkMode.WFA && uiState.selectedTargetLocation?.location != null,
                                isCheckInEnabled = uiState.isButtonEnabled,
                                checkInButtonText = uiState.buttonText,
                                blockingMessage = uiState.workModeEligibility?.blockingReason,
                                onSearchLocationClick = {
                                    navController.navigate("location_search")
                                },
                                onModeSelected = { mode -> viewModel.onWorkModeSelected(mode) },
                                onBookingClick = { viewModel.onBookingClicked() },
                                onCheckInClick = { viewModel.onAttendanceButtonClicked() }
                            )
                        }
                    }
                }
            ) { _ -> // Renamed paddingValues to _ to indicate it's intentionally unused
                Box(modifier = Modifier.fillMaxSize()) {
                    // Fullscreen Map dengan data dari ViewModel - Updated with WFO, WFH, and WFA locations
                    AttendanceMap(
                        modifier = Modifier.fillMaxSize(),
                        wfoLocation = if (uiState.isWfaModeActive) null else uiState.wfoLocation, // Hide WFO when WFA active
                        wfhLocation = if (uiState.isWfaModeActive) null else uiState.wfhLocation, // Hide WFH when WFA active
                        wfaRecommendations = uiState.wfaRecommendations, // WFA recommendations
                        selectedWfaLocation = uiState.selectedWfaLocation, // Selected WFA location
                        targetLocation = uiState.targetLocationMarker, // Keep for backward compatibility
                        currentUserLocation = uiState.currentUserLatitude?.let { lat ->
                            uiState.currentUserLongitude?.let { lng ->
                                Point.fromLngLat(lng, lat)
                            }
                        },
                        onMarkerClick = { location -> viewModel.onMarkerClicked(location) },
                        onWfaMarkerClick = { recommendation: WfaRecommendation ->
                            viewModel.onWfaMarkerClicked(
                                recommendation
                            )
                        }, // Handle WFA marker clicks
                        onMapReady = { mapView ->
                            mapViewInstance = mapView
                            // Notify ViewModel that map is ready for initial focus
                            viewModel.onMapReady()
                        },
                        onCameraIdle = { point ->
                            viewModel.onMapIdle(point)
                        } // Handle Pick on Map functionality
                    )

                    // Top bar with location focus button - fixed parameters
                    AttendanceTopBar(
                        modifier = Modifier
                            .statusBarsPadding()
                            .padding(16.dp),
                        onBackClicked = { navController.navigateUp() },
                        onFocusLocationClicked = { viewModel.onFocusLocationClicked() }
                    )

                    // Pick on Map Crosshair - shows static pin in center when Pick on Map mode is active
                    AnimatedVisibility(
                        visible = uiState.isPickOnMapModeActive,
                        modifier = Modifier.align(Alignment.Center)
                    ) {
                        Icon(
                            imageVector = Icons.Default.LocationOn,
                            contentDescription = "Pick Location",
                            tint = Color.Red,
                            modifier = Modifier.size(32.dp)
                        )
                    }

                    uiState.selectedMarkerInfo?.let { selectedMarker ->
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(top = 80.dp),
                            contentAlignment = Alignment.TopCenter
                        ) {
                            MarkerView(
                                title = selectedMarker.description,
                                description = "Kategori: ${selectedMarker.category}",
                                radius = "${selectedMarker.radius} meter",
                                coordinates = "${selectedMarker.latitude}, ${selectedMarker.longitude}",
                                onClose = { viewModel.onDismissMarkerInfo() }
                            )
                        }
                    }

                    // Display WFA marker details when clicked
                    uiState.selectedWfaMarkerInfo?.let { selectedWfaMarker ->
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(top = 80.dp, start = 16.dp, end = 16.dp),
                            contentAlignment = Alignment.TopCenter
                        ) {
                            MarkerViewWfa(
                                recommendation = selectedWfaMarker,
                                onClick = { viewModel.onDismissWfaMarkerInfo() }
                            )
                        }
                    }

                    // Loading overlay for WFA recommendations
                    if (uiState.isLoadingWfaRecommendations) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(Color.Black.copy(alpha = 0.3f)),
                            contentAlignment = Alignment.Center
                        ) {
                            LoadingAnimation()
                        }
                    }
                }
            }
        }
    }

    uiState.activeDialog?.let { dialog ->
        when (dialog) {
            is DialogState.Success -> {
                InfiniteTrackStatusDialog(
                    status = StatusStates.Success,
                    title = "Absensi Berhasil",
                    message = dialog.message,
                    showDialog = true,
                    imageRes = R.drawable.icon_success,
                    onDismiss = { viewModel.onDialogDismissed() },
                    onConfirm = {
                        navController.navigate(Screen.Home.route) {
                            popUpTo(Screen.Home.route) {
                                inclusive = false
                            }
                        }
                        viewModel.onDialogDismissed()
                    }
                )
            }

            is DialogState.Error -> {
                InfiniteTrackStatusDialog(
                    status = StatusStates.Error,
                    title = "Absensi Gagal",
                    message = dialog.message,
                    showDialog = true,
                    onDismiss = { viewModel.onDialogDismissed() },
                    onConfirm = { viewModel.onDialogDismissed() }
                )
            }

            is DialogState.LocationError -> {
                InfiniteTrackStatusDialog(
                    status = StatusStates.Error,
                    title = "Error Lokasi",
                    message = dialog.message,
                    showDialog = true,
                    onDismiss = { viewModel.onDialogDismissed() },
                    onConfirm = { viewModel.onDialogDismissed() }
                )
            }
        }
    }

    val currentBackStackEntry by navController.currentBackStackEntryAsState()
    val fallbackFaceVerificationResult = remember { mutableStateOf<String?>(null) }
    val faceVerificationResult by currentBackStackEntry
        ?.savedStateHandle
        ?.getStateFlow<String?>(FACE_VERIFICATION_RESULT_KEY, null)
        ?.collectAsStateWithLifecycle()
        ?: fallbackFaceVerificationResult

    LaunchedEffect(faceVerificationResult, currentBackStackEntry) {
        faceVerificationResult?.let { resultValue ->
            val parsedFaceVerificationResult = FaceVerificationResult.fromSavedState(resultValue)
            if (parsedFaceVerificationResult != null) {
                viewModel.onFaceVerificationResult(parsedFaceVerificationResult)
            } else {
                android.util.Log.e(
                    "AttendanceScreen",
                    "Unknown face verification result payload: $resultValue"
                )
                viewModel.onUnexpectedFaceVerificationResult()
            }
            currentBackStackEntry
                ?.savedStateHandle
                ?.remove<String>(FACE_VERIFICATION_RESULT_KEY)
        }
    }

    val defensivePermissionResult = uiState.permissionResult
    val shouldShowDefensivePermissionDialog = uiState.showPermissionDialog &&
        defensivePermissionResult != null &&
        defensivePermissionResult != LocationPermissionHelper.PermissionResult.BackgroundPermissionDenied

    // Permission Dialog for defensive foreground/settings recovery only.
    // Background location is optional/degraded and stays owned by the readiness screen.
    if (shouldShowDefensivePermissionDialog) {
        LocationPermissionDialog(
            permissionResult = defensivePermissionResult!!,
            onRequestPermission = {
                locationPermissionHelper?.checkAndRequestPermissions()
            },
            onOpenSettings = {
                locationPermissionHelper?.openAppSettings()
            },
            onDismiss = {
                viewModel.onDismissPermissionDialog()
            }
        )
    }
}

@Preview(showBackground = true)
@Composable
fun AttendanceScreenPreview() {
    Infinite_TrackTheme {
        Box(modifier = Modifier.fillMaxSize()) {
            AttendanceMap(
                modifier = Modifier.fillMaxSize(),
                onMapReady = { }
            )
            AttendanceTopBar(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .statusBarsPadding()
                    .padding(horizontal = 20.dp),
                onBackClicked = { },
                onFocusLocationClicked = { }
            )
        }
    }
}
