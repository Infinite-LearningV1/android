package com.example.infinite_track.presentation.main

import android.Manifest
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.camera.core.ExperimentalGetImage
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.core.content.ContextCompat
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.example.infinite_track.domain.manager.SessionManager
import com.example.infinite_track.domain.repository.LocalizationRepository
import com.example.infinite_track.presentation.navigation.AppNavigator
import com.example.infinite_track.presentation.screen.splash.SplashNavigationState
import com.example.infinite_track.presentation.screen.splash.SplashViewModel
import com.example.infinite_track.presentation.theme.Infinite_TrackTheme
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

	private val requestNotificationPermission =
		registerForActivityResult(ActivityResultContracts.RequestPermission()) { /* no-op */ }

	@ExperimentalGetImage
	override fun onCreate(savedInstanceState: Bundle?) {
		// Install splash screen BEFORE super.onCreate()
		val splashScreen = installSplashScreen()

		// Set keep on screen condition - keep splash screen visible while in Loading state
		splashScreen.setKeepOnScreenCondition {
			viewModel.navigationState.value is SplashNavigationState.Loading
		}

		super.onCreate(savedInstanceState)
		enableEdgeToEdge()

		// Apply saved language before composing UI
		runBlocking {
			val savedLanguage = localizationRepository.getSelectedLanguage().first()
			updateAppLanguage(this@MainActivity, savedLanguage)
		}

		requestPostNotificationsIfNeeded()

		setContent {
			Infinite_TrackTheme {
				InfiniteTrackApp(
					appNavigator = appNavigator,
					sessionManager = sessionManager
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

	private fun requestPostNotificationsIfNeeded() {
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
			val granted = ContextCompat.checkSelfPermission(
				this,
				Manifest.permission.POST_NOTIFICATIONS
			) == android.content.pm.PackageManager.PERMISSION_GRANTED
			if (!granted) {
				requestNotificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
			}
		}
	}
}
