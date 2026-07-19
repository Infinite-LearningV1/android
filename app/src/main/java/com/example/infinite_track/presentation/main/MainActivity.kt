package com.example.infinite_track.presentation.main

import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.camera.core.ExperimentalGetImage
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.runtime.SideEffect
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.example.infinite_track.domain.manager.SessionManager
import com.example.infinite_track.domain.repository.LocalizationRepository
import com.example.infinite_track.presentation.navigation.AppNavigator
import com.example.infinite_track.presentation.screen.splash.SplashViewModel
import com.example.infinite_track.presentation.theme.Infinite_TrackTheme
import com.example.infinite_track.utils.LocationPermissionHelper
import com.example.infinite_track.utils.NotificationHelper
import com.example.infinite_track.utils.updateAppLanguage
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
	// Get SplashViewModel instance
	private val viewModel: SplashViewModel by viewModels()

	// Inject AppNavigator untuk navigasi dari Activity
	@Inject
	lateinit var appNavigator: AppNavigator

	// Inject SessionManager untuk menangani session expiration
	@Inject
	lateinit var sessionManager: SessionManager

	// Inject LocalizationRepository untuk mendapatkan bahasa tersimpan
	@Inject
	lateinit var localizationRepository: LocalizationRepository

	// Location permission helper untuk geofencing
	private lateinit var locationPermissionHelper: LocationPermissionHelper

	/**
	 * Native splash protects process startup / first Compose frame only.
	 * Branded animation duration is owned by Compose SplashScreen.
	 */
	@Volatile
	private var isComposeReady = false

	@ExperimentalGetImage
	override fun onCreate(savedInstanceState: Bundle?) {
		// Install splash screen BEFORE super.onCreate()
		val splashScreen = installSplashScreen()

		// Release native splash once Compose content is ready.
		// Do NOT hold through full session bootstrap Loading.
		splashScreen.setKeepOnScreenCondition { !isComposeReady }

		super.onCreate(savedInstanceState)
		enableEdgeToEdge()

		// Apply saved language before composing UI
		runBlocking {
			val savedLanguage = localizationRepository.getSelectedLanguage().first()
			updateAppLanguage(this@MainActivity, savedLanguage)
		}

		// Initialize location permission helper
		locationPermissionHelper = LocationPermissionHelper(this) { result ->
			Log.d("MainActivity", "Permission result: $result")
			// Permission result akan dihandle di AttendanceViewModel
		}

		// Create notification channel untuk geofencing
		NotificationHelper.createNotificationChannel(this)

		setContent {
			// First successful composition marks readiness for native splash exit.
			SideEffect {
				isComposeReady = true
			}
			Infinite_TrackTheme {
				InfiniteTrackApp(
					appNavigator = appNavigator,
					sessionManager = sessionManager,
					locationPermissionHelper = locationPermissionHelper,
					splashViewModel = viewModel
				)
			}
		}

		// Handle intent saat aplikasi pertama kali dibuka dari notifikasi
		handleIntent(intent)
	}

	override fun onNewIntent(intent: Intent) {
		super.onNewIntent(intent)
		// Handle intent saat aplikasi sudah berjalan di background dan notifikasi diklik
		handleIntent(intent)
	}

	private fun handleIntent(intent: Intent?) {
		// Periksa apakah intent memiliki extra yang kita kirim dari NotificationHelper
		if (intent?.getBooleanExtra("navigate_to_attendance", false) == true) {
			// Gunakan AppNavigator untuk navigasi ke AttendanceScreen
			appNavigator.navigateToAttendance()
			Log.d("MainActivity", "Navigating to AttendanceScreen via AppNavigator")
		}
	}
}
