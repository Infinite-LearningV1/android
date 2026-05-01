package com.example.infinite_track.presentation.main

import android.Manifest
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.camera.core.ExperimentalGetImage
import androidx.core.content.ContextCompat
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.example.infinite_track.domain.manager.SessionManager
import com.example.infinite_track.domain.repository.LocalizationRepository
import com.example.infinite_track.presentation.navigation.AppNavigator
import com.example.infinite_track.presentation.screen.splash.SplashNavigationState
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
	private val viewModel: SplashViewModel by viewModels()

	@Inject
	lateinit var appNavigator: AppNavigator

	@Inject
	lateinit var sessionManager: SessionManager

	@Inject
	lateinit var localizationRepository: LocalizationRepository

	// Location permission helper untuk geofencing
	private lateinit var locationPermissionHelper: LocationPermissionHelper

	private val requestNotificationPermission =
		registerForActivityResult(ActivityResultContracts.RequestPermission()) { /* no-op */ }

	@ExperimentalGetImage
	override fun onCreate(savedInstanceState: Bundle?) {
		val splashScreen = installSplashScreen()
		splashScreen.setKeepOnScreenCondition {
			viewModel.navigationState.value is SplashNavigationState.Loading
		}

		super.onCreate(savedInstanceState)
		enableEdgeToEdge()

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

		requestPostNotificationsIfNeeded()

		setContent {
			Infinite_TrackTheme {
				InfiniteTrackApp(
					appNavigator = appNavigator,
					sessionManager = sessionManager,
					locationPermissionHelper = locationPermissionHelper
				)
			}
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

