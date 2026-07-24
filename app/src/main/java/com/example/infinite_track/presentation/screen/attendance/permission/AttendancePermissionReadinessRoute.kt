package com.example.infinite_track.presentation.screen.attendance.permission

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.app.ActivityCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.infinite_track.domain.model.attendance.permission.AttendanceAccess
import com.example.infinite_track.domain.model.attendance.permission.AttendancePermissionRequestOutcome
import com.example.infinite_track.presentation.design.components.status.InfiniteSnackbarHost
import com.example.infinite_track.presentation.design.components.status.InfiniteSnackbarVisuals
import com.example.infinite_track.presentation.design.components.status.InfiniteSnackbarTimeout

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AttendancePermissionPanelHost(
    visible: Boolean,
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: AttendancePermissionReadinessViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val activity = remember(context) { context.findActivity() }
    val lifecycleOwner = LocalLifecycleOwner.current
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val latestOnDismissRequest by rememberUpdatedState(onDismissRequest)
    val snackbarHostState = remember { SnackbarHostState() }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val requestedPermissions = remember { mutableSetOf<String>() }

    val preciseLocationLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { result ->
        val permission = Manifest.permission.ACCESS_FINE_LOCATION
        val granted = result[permission] == true
        val wasRequested = requestedPermissions.remove(permission)
        requestedPermissions.remove(Manifest.permission.ACCESS_COARSE_LOCATION)
        viewModel.onEvent(
            AttendancePermissionReadinessEvent.PermissionResultReceived(
                access = AttendanceAccess.PRECISE_LOCATION,
                outcome = permissionOutcome(
                    activity = activity,
                    permission = permission,
                    granted = granted,
                    wasRequested = wasRequested
                )
            )
        )
    }

    val cameraLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        val permission = Manifest.permission.CAMERA
        viewModel.onEvent(
            AttendancePermissionReadinessEvent.PermissionResultReceived(
                access = AttendanceAccess.CAMERA,
                outcome = permissionOutcome(
                    activity = activity,
                    permission = permission,
                    granted = granted,
                    wasRequested = requestedPermissions.remove(permission)
                )
            )
        )
    }

    val notificationLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        val permission = Manifest.permission.POST_NOTIFICATIONS
        viewModel.onEvent(
            AttendancePermissionReadinessEvent.PermissionResultReceived(
                access = AttendanceAccess.NOTIFICATION,
                outcome = permissionOutcome(
                    activity = activity,
                    permission = permission,
                    granted = granted,
                    wasRequested = requestedPermissions.remove(permission)
                )
            )
        )
    }

    val backgroundLocationLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        val permission = Manifest.permission.ACCESS_BACKGROUND_LOCATION
        viewModel.onEvent(
            AttendancePermissionReadinessEvent.PermissionResultReceived(
                access = AttendanceAccess.BACKGROUND_LOCATION,
                outcome = permissionOutcome(
                    activity = activity,
                    permission = permission,
                    granted = granted,
                    wasRequested = requestedPermissions.remove(permission)
                )
            )
        )
    }

    val applicationSettingsLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) {
        viewModel.onEvent(AttendancePermissionReadinessEvent.ReturnedFromSettings)
    }

    val deviceLocationSettingsLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) {
        viewModel.onEvent(AttendancePermissionReadinessEvent.ReturnedFromSettings)
    }

    DisposableEffect(lifecycleOwner, viewModel) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                viewModel.onEvent(AttendancePermissionReadinessEvent.ScreenResumed)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    LaunchedEffect(viewModel) {
        viewModel.effects.collect { effect ->
            when (effect) {
                AttendancePermissionReadinessEffect.RequestPreciseLocation -> {
                    requestedPermissions += Manifest.permission.ACCESS_FINE_LOCATION
                    requestedPermissions += Manifest.permission.ACCESS_COARSE_LOCATION
                    preciseLocationLauncher.launch(
                        arrayOf(
                            Manifest.permission.ACCESS_FINE_LOCATION,
                            Manifest.permission.ACCESS_COARSE_LOCATION
                        )
                    )
                }

                AttendancePermissionReadinessEffect.RequestCamera -> {
                    requestedPermissions += Manifest.permission.CAMERA
                    cameraLauncher.launch(Manifest.permission.CAMERA)
                }

                AttendancePermissionReadinessEffect.RequestNotification -> {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        requestedPermissions += Manifest.permission.POST_NOTIFICATIONS
                        notificationLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                    } else {
                        viewModel.onEvent(
                            AttendancePermissionReadinessEvent.PermissionResultReceived(
                                AttendanceAccess.NOTIFICATION,
                                AttendancePermissionRequestOutcome.GRANTED
                            )
                        )
                    }
                }

                AttendancePermissionReadinessEffect.RequestBackgroundLocation -> {
                    when {
                        Build.VERSION.SDK_INT < Build.VERSION_CODES.Q -> {
                            viewModel.onEvent(
                                AttendancePermissionReadinessEvent.PermissionResultReceived(
                                    AttendanceAccess.BACKGROUND_LOCATION,
                                    AttendancePermissionRequestOutcome.GRANTED
                                )
                            )
                        }

                        Build.VERSION.SDK_INT == Build.VERSION_CODES.Q -> {
                            requestedPermissions += Manifest.permission.ACCESS_BACKGROUND_LOCATION
                            backgroundLocationLauncher.launch(
                                Manifest.permission.ACCESS_BACKGROUND_LOCATION
                            )
                        }

                        else -> launchApplicationSettings(
                            context = context,
                            onFailure = {
                                viewModel.onEvent(
                                    AttendancePermissionReadinessEvent.SettingsLaunchFailed(
                                        AttendanceSettingsDestination.APPLICATION
                                    )
                                )
                            },
                            launch = applicationSettingsLauncher::launch
                        )
                    }
                }

                is AttendancePermissionReadinessEffect.OpenApplicationSettings -> {
                    launchApplicationSettings(
                        context = context,
                        onFailure = {
                            viewModel.onEvent(
                                AttendancePermissionReadinessEvent.SettingsLaunchFailed(
                                    AttendanceSettingsDestination.APPLICATION
                                )
                            )
                        },
                        launch = applicationSettingsLauncher::launch
                    )
                }

                AttendancePermissionReadinessEffect.OpenDeviceLocationSettings -> {
                    launchSettings(
                        intent = Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS),
                        onFailure = {
                            viewModel.onEvent(
                                AttendancePermissionReadinessEvent.SettingsLaunchFailed(
                                    AttendanceSettingsDestination.DEVICE_LOCATION
                                )
                            )
                        },
                        launch = deviceLocationSettingsLauncher::launch
                    )
                }

                AttendancePermissionReadinessEffect.ClosePermissionPanel -> {
                    latestOnDismissRequest()
                }

                is AttendancePermissionReadinessEffect.ShowSnackbar -> {
                    val feedback = effect.feedback
                    val result = snackbarHostState.showSnackbar(
                        feedback.toInfiniteSnackbarVisuals()
                    )
                    if (result == SnackbarResult.ActionPerformed && feedback.action != null) {
                        viewModel.onEvent(
                            AttendancePermissionReadinessEvent.SnackbarActionClicked(
                                feedbackId = feedback.id,
                                action = feedback.action
                            )
                        )
                    } else {
                        viewModel.onEvent(
                            AttendancePermissionReadinessEvent.SnackbarFinished(feedback.id)
                        )
                    }
                }
            }
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        if (visible) {
            ModalBottomSheet(
                onDismissRequest = { latestOnDismissRequest() },
                sheetState = sheetState,
                containerColor = Color.White,
                scrimColor = Color.Black.copy(alpha = 0.34f),
                dragHandle = null
            ) {
                AttendancePermissionPanelContent(
                    uiState = uiState,
                    onEvent = viewModel::onEvent,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
        InfiniteSnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .imePadding()
                .padding(horizontal = 16.dp, vertical = 16.dp)
        )
    }
}

internal fun AttendanceFeedbackDuration.toInfiniteSnackbarTimeout():
    InfiniteSnackbarTimeout = when (this) {
    AttendanceFeedbackDuration.SHORT -> InfiniteSnackbarTimeout.SHORT
    AttendanceFeedbackDuration.LONG -> InfiniteSnackbarTimeout.LONG
}

internal fun AttendancePermissionFeedback.toInfiniteSnackbarVisuals() =
    InfiniteSnackbarVisuals(
        message = message,
        semantic = semantic,
        actionLabel = actionLabel,
        autoDismissTimeoutMillis = duration.toInfiniteSnackbarTimeout().baseMillis
    )

private fun permissionOutcome(
    activity: Activity?,
    permission: String,
    granted: Boolean,
    wasRequested: Boolean
): AttendancePermissionRequestOutcome = classifyPermissionOutcome(
    granted = granted,
    wasRequested = wasRequested,
    shouldShowRationale = activity?.let {
        ActivityCompat.shouldShowRequestPermissionRationale(it, permission)
    } ?: true
)

internal fun classifyPermissionOutcome(
    granted: Boolean,
    wasRequested: Boolean,
    shouldShowRationale: Boolean
): AttendancePermissionRequestOutcome = when {
    granted -> AttendancePermissionRequestOutcome.GRANTED
    !wasRequested || shouldShowRationale -> AttendancePermissionRequestOutcome.DENIED
    else -> AttendancePermissionRequestOutcome.PERMANENTLY_DENIED
}

private fun launchApplicationSettings(
    context: Context,
    onFailure: () -> Unit,
    launch: (Intent) -> Unit
) {
    launchSettings(
        intent = Intent(
            Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
            Uri.fromParts("package", context.packageName, null)
        ),
        onFailure = onFailure,
        launch = launch
    )
}

private fun launchSettings(
    intent: Intent,
    onFailure: () -> Unit,
    launch: (Intent) -> Unit
) {
    try {
        launch(intent)
    } catch (_: RuntimeException) {
        onFailure()
    }
}

private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}
