package com.example.infinite_track.presentation.screen.splash

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.infinite_track.domain.use_case.auth.CheckSessionUseCase
import com.example.infinite_track.domain.use_case.auth.LogoutUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

// Navigation states for the SplashScreen
sealed class SplashNavigationState {
    object Loading : SplashNavigationState()
    object NavigateToHome : SplashNavigationState()
    object NavigateToLogin : SplashNavigationState()
}

@HiltViewModel
class SplashViewModel @Inject constructor(
    private val checkSessionUseCase: CheckSessionUseCase,
    private val logoutUseCase: LogoutUseCase,
    @ApplicationContext private val context: Context
) : ViewModel() {

    // Private mutable state flow for navigation state
    private val _navigationState =
        MutableStateFlow<SplashNavigationState>(SplashNavigationState.Loading)

    // Public immutable state flow for navigation state
    val navigationState: StateFlow<SplashNavigationState> = _navigationState.asStateFlow()

    init {
        checkSession()
    }

    private fun checkSession() {
        viewModelScope.launch {
            checkSessionUseCase()
                .onSuccess {
                    // Session is valid and embedding generation (if needed) successful, navigate to home
                    _navigationState.value = SplashNavigationState.NavigateToHome
                }
                .onFailure { exception ->
                    // Session is invalid or embedding generation failed
                    // DON'T show dialog in splash screen - directly navigate to login
                    handleSessionFailure(exception)
                }
        }
    }

    private suspend fun handleSessionFailure(exception: Throwable) {
        try {
            // Clear any existing session data
            logoutUseCase()

            // Directly navigate to login without showing dialog
            // This prevents BadTokenException in splash screen
            _navigationState.value = SplashNavigationState.NavigateToLogin

        } catch (e: Exception) {
            // Even if logout fails, still navigate to login
            _navigationState.value = SplashNavigationState.NavigateToLogin
        }
    }
}