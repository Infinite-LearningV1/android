package com.example.infinite_track.presentation.screen.attendance

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
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
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.material3.rememberBottomSheetScaffoldState
import androidx.compose.material3.rememberStandardBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.navigation.NavController
import androidx.navigation.compose.currentBackStackEntryAsState
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
import com.example.infinite_track.presentation.design.components.status.InfiniteSnackbarHost
import com.example.infinite_track.presentation.design.components.status.InfiniteSnackbarVisuals
import com.example.infinite_track.presentation.design.components.status.InfiniteInlineAlert
import com.example.infinite_track.presentation.design.tokens.InfiniteSemantic
import com.example.infinite_track.presentation.navigation.Screen
import com.example.infinite_track.presentation.screen.attendance.components.AttendanceTopBar
import com.example.infinite_track.presentation.screen.attendance.permission.AttendancePermissionPanelHost
import com.example.infinite_track.presentation.screen.attendance.permission.AttendancePermissionReadinessEvent
import com.example.infinite_track.presentation.screen.attendance.permission.AttendancePermissionReadinessViewModel
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
    viewModel: AttendanceViewModel = hiltViewModel(),
    permissionViewModel: AttendancePermissionReadinessViewModel = hiltViewModel()
) {
    var mapViewInstance by remember { mutableStateOf<MapView?>(null) }
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    var hasPreciseLocationPermission by remember(context) {
        mutableStateOf(context.hasPreciseLocationPermission())
    }

    DisposableEffect(lifecycleOwner, context) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                hasPreciseLocationPermission = context.hasPreciseLocationPermission()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    // Observasi state dari ViewModel yang sudah disederhanakan
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val permissionUiState by permissionViewModel.uiState.collectAsStateWithLifecycle()
    var showPermissionPanel by rememberSaveable { mutableStateOf(false) }
    var initialPermissionCheckHandled by rememberSaveable { mutableStateOf(false) }
    val locationPermissionHelper = LocalLocationPermissionHelper.current

    LaunchedEffect(
        permissionUiState.isLoading,
        permissionUiState.canContinue,
        initialPermissionCheckHandled
    ) {
        if (
            shouldAutoOpenPermissionPanel(
                isLoading = permissionUiState.isLoading,
                canContinue = permissionUiState.canContinue,
                initialCheckHandled = initialPermissionCheckHandled
            )
        ) {
            initialPermissionCheckHandled = true
            showPermissionPanel = true
        } else if (!permissionUiState.isLoading && !initialPermissionCheckHandled) {
            initialPermissionCheckHandled = true
        }
    }

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

    LaunchedEffect(viewModel) {
        viewModel.transientFeedback.collect { feedback ->
            val result = scaffoldState.snackbarHostState.showSnackbar(
                InfiniteSnackbarVisuals(
                    message = feedback.message,
                    semantic = feedback.semantic,
                    actionLabel = feedback.actionLabel,
                    duration = feedback.duration.toMaterialDuration()
                )
            )
            if (
                result == SnackbarResult.ActionPerformed &&
                feedback.action == AttendanceTransientFeedbackAction.NAVIGATE_HOME
            ) {
                navController.navigate(Screen.Home.route) {
                    popUpTo(Screen.Home.route) { inclusive = false }
                }
            }
        }
    }

    // =======================================================
    // NEW: State-driven LaunchedEffect for Navigation
    // =======================================================
    LaunchedEffect(uiState.navigationTarget) {
        uiState.navigationTarget?.let { target ->
            when (target) {
                is NavigationTarget.FaceScanner -> {
                    val action = when (target.intent) {
                        AttendanceActionIntent.CHECK_IN -> "checkin"
                        AttendanceActionIntent.CHECK_OUT -> "checkout"
                    }
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
                snackbarHost = {
                    InfiniteSnackbarHost(hostState = scaffoldState.snackbarHostState)
                },
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
                                actionState = uiState.actionState,
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
                AttendanceLocationPermissionGate(
                    hasPreciseLocationPermission = hasPreciseLocationPermission,
                    isAttendanceContentReady = true,
                    onStartLocationUpdates = viewModel::startLocationUpdates,
                    onOpenPermissionPanel = {
                        permissionViewModel.onEvent(AttendancePermissionReadinessEvent.ScreenResumed)
                        showPermissionPanel = true
                    },
                    modifier = Modifier.fillMaxSize()
                ) {
                    // Fullscreen Map dengan data dari ViewModel - Updated with WFO, WFH, and WFA locations
                    AttendanceMap(
                        hasPreciseLocationPermission = hasPreciseLocationPermission,
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
                        onFocusLocationClicked = { viewModel.onFocusLocationClicked() },
                        onPermissionClicked = {
                            permissionViewModel.onEvent(AttendancePermissionReadinessEvent.ScreenResumed)
                            showPermissionPanel = true
                        }
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

    AttendancePermissionPanelHost(
        visible = showPermissionPanel,
        onDismissRequest = { showPermissionPanel = false },
        viewModel = permissionViewModel
    )
}

private fun AttendanceTransientFeedbackDuration.toMaterialDuration(): SnackbarDuration = when (this) {
    AttendanceTransientFeedbackDuration.SHORT -> SnackbarDuration.Short
    AttendanceTransientFeedbackDuration.LONG -> SnackbarDuration.Long
}

@Preview(showBackground = true)
@Composable
fun AttendanceScreenPreview() {
    Infinite_TrackTheme {
        Box(modifier = Modifier.fillMaxSize()) {
            AttendanceMap(
                hasPreciseLocationPermission = false,
                modifier = Modifier.fillMaxSize(),
                onMapReady = { }
            )
            AttendanceTopBar(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .statusBarsPadding()
                    .padding(horizontal = 20.dp),
                onBackClicked = { },
                onFocusLocationClicked = { },
                onPermissionClicked = { }
            )
        }
    }
}

@Composable
internal fun AttendancePermissionRevocationRecovery(
    onOpenPermissionPanel: () -> Unit,
    modifier: Modifier = Modifier
) {
    InfiniteInlineAlert(
        title = "Lokasi presisi tidak tersedia",
        message = "Akses lokasi berubah saat Attendance dibuka. Pulihkan dari panel akses untuk melanjutkan.",
        semantic = InfiniteSemantic.Warning,
        actionLabel = "Kelola akses",
        onAction = onOpenPermissionPanel,
        modifier = modifier
    )
}

@Composable
internal fun AttendanceLocationPermissionGate(
    hasPreciseLocationPermission: Boolean,
    isAttendanceContentReady: Boolean,
    onStartLocationUpdates: () -> Unit,
    onOpenPermissionPanel: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit
) {
    val currentOnStartLocationUpdates by rememberUpdatedState(onStartLocationUpdates)

    LaunchedEffect(isAttendanceContentReady, hasPreciseLocationPermission) {
        if (isAttendanceContentReady && hasPreciseLocationPermission) {
            currentOnStartLocationUpdates()
        }
    }

    Box(modifier = modifier) {
        content()
        if (!hasPreciseLocationPermission) {
            AttendancePermissionRevocationRecovery(
                onOpenPermissionPanel = onOpenPermissionPanel,
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .statusBarsPadding()
                    .padding(top = 80.dp, start = 16.dp, end = 16.dp)
                    .testTag("attendancePermissionRevocationRecovery")
            )
        }
    }
}

private fun Context.hasPreciseLocationPermission(): Boolean =
    ContextCompat.checkSelfPermission(
        this,
        Manifest.permission.ACCESS_FINE_LOCATION
    ) == PackageManager.PERMISSION_GRANTED

internal fun shouldAutoOpenPermissionPanel(
    isLoading: Boolean,
    canContinue: Boolean,
    initialCheckHandled: Boolean
): Boolean = !isLoading && !canContinue && !initialCheckHandled
