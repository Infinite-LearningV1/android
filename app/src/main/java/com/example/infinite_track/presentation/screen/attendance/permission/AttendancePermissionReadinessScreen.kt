package com.example.infinite_track.presentation.screen.attendance.permission

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.LocationManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Tune
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.infinite_track.presentation.design.components.button.InfiniteButton
import com.example.infinite_track.presentation.design.components.button.InfiniteButtonState
import com.example.infinite_track.presentation.design.components.button.InfiniteButtonVariant
import com.example.infinite_track.presentation.design.components.data.InfiniteSectionHeader
import com.example.infinite_track.presentation.design.components.navigation.InfiniteTopBar
import com.example.infinite_track.presentation.design.components.surface.InfiniteCard
import com.example.infinite_track.presentation.design.tokens.InfiniteColors
import com.example.infinite_track.presentation.design.tokens.InfiniteDensity
import com.example.infinite_track.presentation.design.tokens.InfiniteSurfaceVariant
import com.example.infinite_track.presentation.theme.Infinite_TrackTheme

@Composable
fun AttendancePermissionReadinessScreen(
    onBackClick: () -> Unit,
    onContinueToWorkMode: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    var uiState by remember { mutableStateOf(context.readAttendancePermissionState()) }

    fun refreshReadiness() {
        uiState = context.readAttendancePermissionState()
    }

    val foregroundLocationLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { refreshReadiness() }

    val cameraLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { refreshReadiness() }

    val notificationLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { refreshReadiness() }

    val backgroundLocationLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { refreshReadiness() }

    val settingsLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { refreshReadiness() }

    DisposableEffect(lifecycleOwner, context) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                refreshReadiness()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    LaunchedEffect(Unit) {
        refreshReadiness()
    }

    fun openAppSettings() {
        val intent = Intent(
            Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
            Uri.fromParts("package", context.packageName, null)
        )
        settingsLauncher.launch(intent)
    }

    fun requestAction(action: AttendancePermissionAction) {
        when (action) {
            AttendancePermissionAction.REQUEST_FOREGROUND_LOCATION -> foregroundLocationLauncher.launch(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                )
            )
            AttendancePermissionAction.REQUEST_CAMERA -> cameraLauncher.launch(Manifest.permission.CAMERA)
            AttendancePermissionAction.OPEN_DEVICE_LOCATION_SETTINGS -> settingsLauncher.launch(
                Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)
            )
            AttendancePermissionAction.REQUEST_NOTIFICATION -> {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    notificationLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                } else {
                    refreshReadiness()
                }
            }
            AttendancePermissionAction.REQUEST_BACKGROUND_LOCATION -> {
                when {
                    Build.VERSION.SDK_INT < Build.VERSION_CODES.Q -> refreshReadiness()
                    Build.VERSION.SDK_INT == Build.VERSION_CODES.Q -> backgroundLocationLauncher.launch(
                        Manifest.permission.ACCESS_BACKGROUND_LOCATION
                    )
                    else -> openAppSettings()
                }
            }
            AttendancePermissionAction.CONTINUE_TO_WORK_MODE -> onContinueToWorkMode()
        }
    }

    AttendancePermissionReadinessContent(
        uiState = uiState,
        modifier = modifier,
        onBackClick = onBackClick,
        onPrimaryClick = {
            val nextAction = uiState.nextRequiredAction
            if (uiState.canContinueToWorkMode || nextAction == AttendancePermissionAction.CONTINUE_TO_WORK_MODE) {
                onContinueToWorkMode()
            } else if (nextAction != null) {
                requestAction(nextAction)
            }
        },
        onForegroundLocationClick = { requestAction(AttendancePermissionAction.REQUEST_FOREGROUND_LOCATION) },
        onCameraClick = { requestAction(AttendancePermissionAction.REQUEST_CAMERA) },
        onNotificationClick = { requestAction(AttendancePermissionAction.REQUEST_NOTIFICATION) },
        onBackgroundLocationClick = { requestAction(AttendancePermissionAction.REQUEST_BACKGROUND_LOCATION) },
        onDeviceLocationSettingsClick = { requestAction(AttendancePermissionAction.OPEN_DEVICE_LOCATION_SETTINGS) }
    )
}

@Composable
private fun AttendancePermissionReadinessContent(
    uiState: AttendancePermissionReadinessUiState,
    onBackClick: () -> Unit,
    onPrimaryClick: () -> Unit,
    onForegroundLocationClick: () -> Unit,
    onCameraClick: () -> Unit,
    onNotificationClick: () -> Unit,
    onBackgroundLocationClick: () -> Unit,
    onDeviceLocationSettingsClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(InfiniteColors.AttendanceReportBackground)
    ) {
        Box(
            modifier = Modifier
                .size(280.dp)
                .align(Alignment.TopEnd)
                .padding(top = 24.dp)
                .background(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            InfiniteColors.Primary.copy(alpha = 0.24f),
                            Color.Transparent
                        )
                    ),
                    shape = CircleShape
                )
                .blur(32.dp)
        )
        Box(
            modifier = Modifier
                .size(260.dp)
                .align(Alignment.BottomStart)
                .background(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            InfiniteColors.Accent.copy(alpha = 0.18f),
                            Color.Transparent
                        )
                    ),
                    shape = CircleShape
                )
                .blur(36.dp)
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            InfiniteTopBar(
                title = "Attendance",
                navigationIcon = Icons.AutoMirrored.Filled.ArrowBack,
                navigationContentDescription = "Kembali",
                onNavigationClick = onBackClick
            )
            PermissionHeroCard(uiState = uiState)
            PermissionProgressHeader(uiState = uiState)

            InfiniteCard(
                variant = InfiniteSurfaceVariant.Glass,
                density = InfiniteDensity.Comfortable,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    InfiniteSectionHeader(
                        title = "Akses wajib",
                        subtitle = "Harus siap sebelum memilih mode kerja",
                        leadingIcon = Icons.Default.Shield
                    )
                    PermissionMissionRow(
                        title = "Lokasi presisi",
                        description = "Dipakai untuk membaca posisi saat absensi.",
                        icon = Icons.Default.LocationOn,
                        isGranted = uiState.foregroundLocationGranted,
                        isRequired = true,
                        actionLabel = if (uiState.foregroundLocationGranted) null else "Minta izin",
                        onActionClick = if (uiState.foregroundLocationGranted) null else onForegroundLocationClick
                    )
                    PermissionMissionRow(
                        title = "Kamera",
                        description = "Dipakai untuk verifikasi wajah saat check-in/check-out.",
                        icon = Icons.Default.PhotoCamera,
                        isGranted = uiState.cameraGranted,
                        isRequired = true,
                        actionLabel = if (uiState.cameraGranted) null else "Minta izin",
                        onActionClick = if (uiState.cameraGranted) null else onCameraClick
                    )
                    PermissionMissionRow(
                        title = "Lokasi perangkat aktif",
                        description = "Pastikan Location/GPS perangkat menyala.",
                        icon = Icons.Default.Settings,
                        isGranted = uiState.deviceLocationEnabled,
                        isRequired = true,
                        actionLabel = if (uiState.deviceLocationEnabled) null else "Buka pengaturan",
                        onActionClick = if (uiState.deviceLocationEnabled) null else onDeviceLocationSettingsClick
                    )
                }
            }

            InfiniteCard(
                variant = InfiniteSurfaceVariant.Glass,
                density = InfiniteDensity.Comfortable,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    InfiniteSectionHeader(
                        title = "Opsional untuk pengingat",
                        subtitle = "Tidak memblokir absensi manual",
                        leadingIcon = Icons.Default.Notifications,
                        trailingText = "Degraded OK"
                    )
                    PermissionMissionRow(
                        title = "Notifikasi pengingat",
                        description = "Membantu mengingatkan aktivitas absensi.",
                        icon = Icons.Default.Notifications,
                        isGranted = uiState.notificationGranted,
                        isRequired = false,
                        actionLabel = if (uiState.notificationGranted) null else "Aktifkan",
                        onActionClick = if (uiState.notificationGranted) null else onNotificationClick
                    )
                    PermissionMissionRow(
                        title = "Lokasi latar belakang",
                        description = "Mengaktifkan reminder geofence dan monitoring aktif.",
                        icon = Icons.Default.Tune,
                        isGranted = uiState.backgroundLocationGranted,
                        isRequired = false,
                        actionLabel = if (uiState.backgroundLocationGranted) null else "Atur akses",
                        onActionClick = if (uiState.backgroundLocationGranted) null else onBackgroundLocationClick
                    )
                }
            }

            PermissionReadinessInfoBox(uiState = uiState)

            InfiniteButton(
                text = if (uiState.canContinueToWorkMode) "Lanjut ke Mode Kerja" else "Lanjutkan Setup",
                onClick = onPrimaryClick,
                modifier = Modifier.fillMaxWidth(),
                variant = InfiniteButtonVariant.Primary,
                state = InfiniteButtonState.Enabled,
                fullWidth = true
            )

            Spacer(modifier = Modifier.height(12.dp))
        }
    }
}

private fun Context.readAttendancePermissionState(): AttendancePermissionReadinessUiState =
    toAttendancePermissionReadinessUiState(
        foregroundLocationGranted = isPermissionGranted(Manifest.permission.ACCESS_FINE_LOCATION),
        cameraGranted = isPermissionGranted(Manifest.permission.CAMERA),
        deviceLocationEnabled = isDeviceLocationEnabled(),
        notificationGranted = Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            isPermissionGranted(Manifest.permission.POST_NOTIFICATIONS),
        backgroundLocationGranted = Build.VERSION.SDK_INT < Build.VERSION_CODES.Q ||
            isPermissionGranted(Manifest.permission.ACCESS_BACKGROUND_LOCATION)
    )

private fun Context.isPermissionGranted(permission: String): Boolean =
    ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED

private fun Context.isDeviceLocationEnabled(): Boolean {
    val locationManager = getSystemService(Context.LOCATION_SERVICE) as? LocationManager ?: return false
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
        locationManager.isLocationEnabled
    } else {
        locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) ||
            locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)
    }
}

@Preview(showBackground = true)
@Composable
private fun AttendancePermissionReadinessContentPreview() {
    Infinite_TrackTheme {
        AttendancePermissionReadinessContent(
            uiState = toAttendancePermissionReadinessUiState(
                foregroundLocationGranted = true,
                cameraGranted = true,
                deviceLocationEnabled = false,
                notificationGranted = false,
                backgroundLocationGranted = false
            ),
            onBackClick = {},
            onPrimaryClick = {},
            onForegroundLocationClick = {},
            onCameraClick = {},
            onNotificationClick = {},
            onBackgroundLocationClick = {},
            onDeviceLocationSettingsClick = {}
        )
    }
}
