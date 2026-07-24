package com.example.infinite_track.presentation.screen.attendance

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.BottomSheetScaffold
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
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
import com.example.infinite_track.R
import com.example.infinite_track.domain.model.attendance.TargetLocationId
import com.example.infinite_track.domain.model.attendance.TargetLocationResolution
import com.example.infinite_track.domain.model.location.LocationResult
import com.example.infinite_track.presentation.components.button.attendance.AttendanceBottomSheetContent
import com.example.infinite_track.presentation.components.button.attendance.AttendancePreparationEvent
import com.example.infinite_track.presentation.components.empty.ErrorAnimation
import com.example.infinite_track.presentation.components.loading.LoadingAnimation
import com.example.infinite_track.presentation.components.maps.MarkerView
import com.example.infinite_track.presentation.components.dialog.LocationPermissionDialog
import com.example.infinite_track.utils.LocalLocationPermissionHelper
import com.example.infinite_track.utils.LocationPermissionHelper
import com.example.infinite_track.presentation.components.maps.MarkerViewWfa
import com.example.infinite_track.presentation.design.components.status.InfiniteSnackbarHost
import com.example.infinite_track.presentation.design.components.status.InfiniteSnackbarVisuals
import com.example.infinite_track.presentation.design.components.status.InfiniteSnackbarTimeout
import com.example.infinite_track.presentation.design.components.status.InfiniteInlineAlert
import com.example.infinite_track.presentation.design.tokens.InfiniteColors
import com.example.infinite_track.presentation.design.tokens.InfiniteSemantic
import com.example.infinite_track.presentation.navigation.Screen
import com.example.infinite_track.presentation.map.adapter.AttendanceMap
import com.example.infinite_track.presentation.map.mapper.AttendanceMapUiMapper
import com.example.infinite_track.presentation.map.model.AttendanceMapEvent
import com.example.infinite_track.presentation.map.model.MapCameraEffect
import com.example.infinite_track.presentation.map.model.MapMarkerRole
import com.example.infinite_track.presentation.screen.attendance.components.AttendanceTopBar
import com.example.infinite_track.presentation.screen.attendance.preparation.AttendancePreparationState
import com.example.infinite_track.presentation.screen.attendance.preparation.AttendancePreparationPrimaryAction
import com.example.infinite_track.presentation.screen.attendance.preparation.AttendancePreparationUiMapper
import com.example.infinite_track.presentation.screen.attendance.preparation.AttendancePreparationTextResolver
import com.example.infinite_track.presentation.screen.attendance.preparation.AttendancePrimaryActionUiCombiner
import com.example.infinite_track.presentation.screen.attendance.preparation.WfaDiscoveryState
import com.example.infinite_track.presentation.screen.attendance.permission.AttendancePermissionPanelHost
import com.example.infinite_track.presentation.screen.attendance.permission.AttendancePermissionReadinessEvent
import com.example.infinite_track.presentation.screen.attendance.permission.AttendancePermissionReadinessViewModel
import com.example.infinite_track.presentation.theme.Infinite_TrackTheme
import com.example.infinite_track.utils.UiState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AttendanceScreen(
    navController: NavController,
    viewModel: AttendanceViewModel = hiltViewModel(),
    permissionViewModel: AttendancePermissionReadinessViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val attendanceLocale = LocalConfiguration.current.locales[0]
    val attendanceStrings = remember(context, attendanceLocale) {
        object : AttendancePreparationTextResolver {
            override val locale = attendanceLocale

            override fun text(resourceId: Int, vararg formatArgs: Any): String =
                context.getString(resourceId, *formatArgs)
        }
    }
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
    var cameraEffect by remember { mutableStateOf<MapCameraEffect?>(null) }
    var selectedTargetMarkerId by remember { mutableStateOf<TargetLocationId?>(null) }
    var showPermissionPanel by rememberSaveable { mutableStateOf(false) }
    var initialPermissionCheckHandled by rememberSaveable { mutableStateOf(false) }
    val locationPermissionHelper = LocalLocationPermissionHelper.current

    LaunchedEffect(uiState.preparation.selectedMode) {
        selectedTargetMarkerId = null
    }

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
                feedback.toInfiniteSnackbarVisuals()
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

    LaunchedEffect(viewModel) {
        viewModel.mapCameraEffects.collect { effect ->
            cameraEffect = effect
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
            val preparation = uiState.preparation
            val preparationUiModel = AttendancePrimaryActionUiCombiner.combine(
                preparation = AttendancePreparationUiMapper.map(
                    preparation = preparation,
                    strings = attendanceStrings
                ),
                actionState = uiState.actionState
            )
            val discovery = preparation.wfaDiscovery as? WfaDiscoveryState.Content
            val selectedWfaMarker = discovery?.recommendations?.firstOrNull {
                it.stableKey == discovery.selectedKey
            }
            val selectedTargetMarker = AttendanceSelectionTransition.resolvedTargetForInteraction(
                preparation = preparation,
                selectedTargetId = selectedTargetMarkerId
            )

            // Tampilkan konten utama dengan BottomSheet
            BottomSheetScaffold(
                scaffoldState = scaffoldState,
                snackbarHost = {
                    InfiniteSnackbarHost(hostState = scaffoldState.snackbarHostState)
                },
                containerColor = InfiniteColors.Transparent,
                contentColor = InfiniteColors.Text,
                sheetContainerColor = InfiniteColors.AttendanceReportGlassSurface,
                sheetContentColor = InfiniteColors.Text,
                sheetShape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
                sheetTonalElevation = 8.dp,
                sheetPeekHeight = 120.dp,
                sheetDragHandle = {
                    BottomSheetDefaults.DragHandle(
                        color = InfiniteColors.AttendanceReportBodyText.copy(alpha = 0.42f)
                    )
                },
                sheetContent = {
                    AttendanceBottomSheetContent(
                        model = preparationUiModel,
                        onEvent = { event ->
                            when (event) {
                                is AttendancePreparationEvent.ModeSelected ->
                                    viewModel.onWorkModeSelected(event.mode)
                                AttendancePreparationEvent.SearchWfaLocation ->
                                    navController.navigate(Screen.LocationSearch.route)
                                AttendancePreparationEvent.PickWfaLocationOnMap ->
                                    viewModel.onMapPickRequested()
                                is AttendancePreparationEvent.PrimaryActionClicked -> {
                                    when (event.action) {
                                        AttendancePreparationPrimaryAction.WAIT -> Unit
                                        AttendancePreparationPrimaryAction.CONTINUE_TO_FACE_VERIFICATION,
                                        AttendancePreparationPrimaryAction.SUBMIT_ATTENDANCE ->
                                            viewModel.onAttendanceButtonClicked()
                                        AttendancePreparationPrimaryAction.REFRESH_STATUS ->
                                            viewModel.onAttendanceStatusRefreshRequested()
                                        AttendancePreparationPrimaryAction.REFRESH_PROFILE ->
                                            viewModel.onAttendanceProfileRefreshRequested()
                                        AttendancePreparationPrimaryAction.RETRY_WFA_DISCOVERY ->
                                            viewModel.onWfaDiscoveryRetryRequested()
                                        AttendancePreparationPrimaryAction.REFRESH_LOCATION ->
                                            viewModel.onFocusLocationClicked()
                                        AttendancePreparationPrimaryAction.FOCUS_TARGET ->
                                            viewModel.onMapReady()
                                        AttendancePreparationPrimaryAction.OPEN_WFA_BOOKING ->
                                            viewModel.onBookingClicked()
                                        AttendancePreparationPrimaryAction.OPEN_WFA_REQUESTS ->
                                            navController.navigate(Screen.Wfa.route)
                                        AttendancePreparationPrimaryAction.CONTACT_ADMIN ->
                                            navController.navigate(Screen.ContactUs.route)
                                    }
                                }
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(
                                width = 1.dp,
                                color = InfiniteColors.AttendanceReportGlassBorder,
                                shape = RoundedCornerShape(
                                    topStart = 24.dp,
                                    topEnd = 24.dp
                                )
                            )
                    )
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
                    val mapUiState = AttendanceMapUiMapper.map(
                        uiState.preparation,
                        hasPreciseLocationPermission
                    )

                    AttendanceMap(
                        state = mapUiState,
                        cameraEffect = cameraEffect,
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(
                            start = 16.dp,
                            top = 96.dp,
                            end = 16.dp,
                            bottom = 176.dp
                        ),
                        onEvent = { event ->
                            when (event) {
                                AttendanceMapEvent.Ready -> viewModel.onMapReady()
                                is AttendanceMapEvent.CameraIdle -> viewModel.onMapIdle(
                                    centerPoint = event.center,
                                    origin = event.origin
                                )
                                is AttendanceMapEvent.MarkerClicked -> {
                                    when (event.marker.role) {
                                        MapMarkerRole.CURRENT_LOCATION -> Unit
                                        MapMarkerRole.AUTHORITATIVE_TARGET ->
                                            (uiState.preparation.targetResolution
                                                as? TargetLocationResolution.Resolved)
                                            ?.target
                                            ?.let { selectedTargetMarkerId = it.targetId }
                                        MapMarkerRole.WFA_RECOMMENDATION ->
                                            (uiState.preparation.wfaDiscovery
                                                as? WfaDiscoveryState.Content)
                                            ?.recommendations
                                            .orEmpty()
                                            .firstOrNull {
                                                AttendanceMapUiMapper.recommendationMarkerId(it) == event.marker.id
                                            }
                                            ?.let(viewModel::onWfaMarkerClicked)
                                        MapMarkerRole.SEARCH_PREVIEW -> Unit
                                    }
                                }
                            }
                        }
                    )

                    // Top bar with location focus button - fixed parameters
                    AttendanceTopBar(
                        modifier = Modifier.statusBarsPadding(),
                        onBackClicked = { navController.navigateUp() },
                        onFocusLocationClicked = { viewModel.onFocusLocationClicked() },
                        onPermissionClicked = {
                            permissionViewModel.onEvent(AttendancePermissionReadinessEvent.ScreenResumed)
                            showPermissionPanel = true
                        }
                    )

                    // Pick on Map Crosshair - shows static pin in center when Pick on Map mode is active
                    AnimatedVisibility(
                        visible = AttendanceSelectionTransition.isMapPickEnabled(preparation),
                        modifier = Modifier.align(Alignment.Center)
                    ) {
                        Icon(
                            imageVector = Icons.Default.LocationOn,
                            contentDescription = stringResource(
                                R.string.attendance_map_pick_location
                            ),
                            tint = Color.Red,
                            modifier = Modifier.size(32.dp)
                        )
                    }

                    selectedTargetMarker?.let { selectedMarker ->
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(top = 80.dp),
                            contentAlignment = Alignment.TopCenter
                        ) {
                            MarkerView(
                                title = selectedMarker.displayName,
                                description = stringResource(
                                    R.string.attendance_marker_category,
                                    selectedMarker.mode.shortLabel
                                ),
                                radius = stringResource(
                                    R.string.attendance_marker_radius,
                                    selectedMarker.radius.value.toInt()
                                ),
                                coordinates = "${selectedMarker.coordinate.latitude}, ${selectedMarker.coordinate.longitude}",
                                onClose = { selectedTargetMarkerId = null }
                            )
                        }
                    }

                    // Display WFA marker details when clicked
                    selectedWfaMarker?.let {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(top = 80.dp, start = 16.dp, end = 16.dp),
                            contentAlignment = Alignment.TopCenter
                        ) {
                            MarkerViewWfa(
                                recommendation = it,
                                onClick = { viewModel.onDismissWfaMarkerInfo() }
                            )
                        }
                    }

                    // Loading overlay for WFA recommendations
                    if (preparation.wfaDiscovery is WfaDiscoveryState.Loading) {
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

internal fun AttendanceTransientFeedbackDuration.toInfiniteSnackbarTimeout():
    InfiniteSnackbarTimeout = when (this) {
    AttendanceTransientFeedbackDuration.SHORT -> InfiniteSnackbarTimeout.SHORT
    AttendanceTransientFeedbackDuration.LONG -> InfiniteSnackbarTimeout.LONG
}

internal fun AttendanceTransientFeedback.toInfiniteSnackbarVisuals() =
    InfiniteSnackbarVisuals(
        message = message,
        semantic = semantic,
        actionLabel = actionLabel,
        autoDismissTimeoutMillis = duration.toInfiniteSnackbarTimeout().baseMillis
    )

@Preview(showBackground = true)
@Composable
fun AttendanceScreenPreview() {
    Infinite_TrackTheme {
        Box(modifier = Modifier.fillMaxSize()) {
            AttendanceMap(
                state = com.example.infinite_track.presentation.map.model.MapUiState(
                    hasPreciseLocationPermission = false
                ),
                cameraEffect = null,
                modifier = Modifier.fillMaxSize(),
                onEvent = { }
            )
            AttendanceTopBar(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .statusBarsPadding(),
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
        title = stringResource(R.string.attendance_permission_revoked_title),
        message = stringResource(R.string.attendance_permission_revoked_message),
        semantic = InfiniteSemantic.Warning,
        actionLabel = stringResource(R.string.attendance_permission_manage),
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
