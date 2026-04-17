package com.example.infinite_track.utils

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

/**
 * Helper class untuk menangani kompleksitas permission lokasi di Android,
 * terutama untuk background location permission yang diperlukan geofencing
 */
class LocationPermissionHelper(
    private val activity: ComponentActivity,
    private val onPermissionResult: (PermissionResult) -> Unit
) {
    
    sealed class PermissionResult {
        object AllPermissionsGranted : PermissionResult()
        object ForegroundPermissionDenied : PermissionResult()
        object BackgroundPermissionDenied : PermissionResult()
        object PermanentlyDenied : PermissionResult()
    }

    private var currentStep = PermissionStep.FOREGROUND
    
    private enum class PermissionStep {
        FOREGROUND, BACKGROUND, SETTINGS
    }

    // Launcher untuk foreground location permissions
    private val foregroundLocationLauncher: ActivityResultLauncher<Array<String>> =
        activity.registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissions ->
            handleForegroundPermissionResult(permissions)
        }

    // Launcher untuk background location permission (Android 10+)
    private val backgroundLocationLauncher: ActivityResultLauncher<String> =
        activity.registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
            handleBackgroundPermissionResult(isGranted)
        }

    // Launcher untuk membuka app settings
    private val settingsLauncher: ActivityResultLauncher<Intent> =
        activity.registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            // Re-check permissions after returning from settings
            checkAndRequestPermissions()
        }

    /**
     * Main method untuk memulai proses permission request
     */
    fun checkAndRequestPermissions() {
        when {
            hasAllRequiredPermissions() -> {
                onPermissionResult(PermissionResult.AllPermissionsGranted)
            }
            !hasForegroundLocationPermission() -> {
                requestForegroundLocationPermissions()
            }
            !hasBackgroundLocationPermission() -> {
                requestBackgroundLocationPermission()
            }
        }
    }

    /**
     * Check apakah semua permission yang diperlukan sudah diberikan
     */
    private fun hasAllRequiredPermissions(): Boolean {
        return hasForegroundLocationPermission() && hasBackgroundLocationPermission()
    }

    /**
     * Check foreground location permissions
     */
    private fun hasForegroundLocationPermission(): Boolean {
        val fineLocation = ContextCompat.checkSelfPermission(
            activity,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        val coarseLocation = ContextCompat.checkSelfPermission(
            activity,
            Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        return fineLocation || coarseLocation
    }

    /**
     * Check background location permission
     */
    private fun hasBackgroundLocationPermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ContextCompat.checkSelfPermission(
                activity,
                Manifest.permission.ACCESS_BACKGROUND_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            // Android 9 dan dibawah tidak memerlukan background location permission terpisah
            true
        }
    }

    /**
     * Request foreground location permissions (step 1)
     */
    private fun requestForegroundLocationPermissions() {
        currentStep = PermissionStep.FOREGROUND
        
        val permissions = arrayOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
        )
        
        foregroundLocationLauncher.launch(permissions)
    }

    /**
     * Request background location permission (step 2) - Only for Android 10+
     */
    private fun requestBackgroundLocationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            currentStep = PermissionStep.BACKGROUND
            backgroundLocationLauncher.launch(Manifest.permission.ACCESS_BACKGROUND_LOCATION)
        } else {
            // Android 9 dan dibawah tidak memerlukan background permission terpisah
            onPermissionResult(PermissionResult.AllPermissionsGranted)
        }
    }

    /**
     * Handle hasil foreground permission request
     */
    private fun handleForegroundPermissionResult(permissions: Map<String, Boolean>) {
        val fineLocationGranted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true
        val coarseLocationGranted = permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true

        when {
            fineLocationGranted || coarseLocationGranted -> {
                // Foreground permission berhasil, lanjut ke background permission
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    requestBackgroundLocationPermission()
                } else {
                    onPermissionResult(PermissionResult.AllPermissionsGranted)
                }
            }
            shouldShowRationale() -> {
                onPermissionResult(PermissionResult.ForegroundPermissionDenied)
            }
            else -> {
                onPermissionResult(PermissionResult.PermanentlyDenied)
            }
        }
    }

    /**
     * Handle hasil background permission request
     */
    private fun handleBackgroundPermissionResult(isGranted: Boolean) {
        when {
            isGranted -> {
                onPermissionResult(PermissionResult.AllPermissionsGranted)
            }
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && 
            ActivityCompat.shouldShowRequestPermissionRationale(
                activity, 
                Manifest.permission.ACCESS_BACKGROUND_LOCATION
            ) -> {
                onPermissionResult(PermissionResult.BackgroundPermissionDenied)
            }
            else -> {
                onPermissionResult(PermissionResult.PermanentlyDenied)
            }
        }
    }

    /**
     * Check apakah harus show rationale untuk foreground permissions
     */
    private fun shouldShowRationale(): Boolean {
        return ActivityCompat.shouldShowRequestPermissionRationale(
            activity,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) || ActivityCompat.shouldShowRequestPermissionRationale(
            activity,
            Manifest.permission.ACCESS_COARSE_LOCATION
        )
    }

    /**
     * Buka app settings untuk manual permission grant
     */
    fun openAppSettings() {
        currentStep = PermissionStep.SETTINGS
        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
            data = Uri.fromParts("package", activity.packageName, null)
        }
        settingsLauncher.launch(intent)
    }

    /**
     * Get user-friendly permission status message
     */
    fun getPermissionStatusMessage(): String {
        return when {
            hasAllRequiredPermissions() -> "Semua izin lokasi telah diberikan"
            !hasForegroundLocationPermission() -> "Izin lokasi diperlukan untuk fitur absensi"
            !hasBackgroundLocationPermission() -> "Izin lokasi latar belakang diperlukan untuk pemantauan area kerja"
            else -> "Status izin tidak diketahui"
        }
    }
}