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

		requestPostNotificationsIfNeeded()

		setContent {
			Infinite_TrackTheme {
				InfiniteTrackApp(
					appNavigator = appNavigator,
					sessionManager = sessionManager
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
