package com.example.infinite_track.presentation.screen.splash

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.infinite_track.domain.use_case.auth.CheckSessionUseCase
import com.example.infinite_track.domain.use_case.auth.ForceReauthUseCase
import com.example.infinite_track.domain.use_case.auth.SessionBootstrapFailure
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

// Navigation states for the SplashScreen
sealed class SplashNavigationState {
    object Loading : SplashNavigationState()
    object TemporaryFailure : SplashNavigationState()
    object NavigateToHome : SplashNavigationState()
    object NavigateToLogin : SplashNavigationState()
}

@HiltViewModel
class SplashViewModel @Inject constructor(
    private val checkSessionUseCase: CheckSessionUseCase,
    private val forceReauthUseCase: ForceReauthUseCase
) : ViewModel() {

    private val bootstrapGate = SplashBootstrapGate()

    // Private mutable state flow for navigation state
    private val _navigationState =
        MutableStateFlow<SplashNavigationState>(SplashNavigationState.Loading)

    // Public immutable state flow for navigation state
    val navigationState: StateFlow<SplashNavigationState> = _navigationState.asStateFlow()

    init {
        checkSession()
    }

    fun retrySessionCheck() {
        _navigationState.value = SplashNavigationState.Loading
        checkSession()
    }

    private fun checkSession() {
        viewModelScope.launch {
            bootstrapGate.runBootstrapIfIdle {
                checkSessionUseCase()
                    .onSuccess {
                        // Session is valid and embedding generation (if needed) successful, navigate to home
                        _navigationState.value = SplashNavigationState.NavigateToHome
                    }
                    .onFailure { exception ->
                        // Session is invalid or embedding generation failed
                        // DON'T show dialog in splash screen - directly navigate to login
                        processSessionFailure(exception)
                    }
            }
        }
    }

    private suspend fun processSessionFailure(exception: Throwable) {
        when (exception) {
            is SessionBootstrapFailure.ReAuthRequired -> {
                try {
                    bootstrapGate.runTerminalLogoutIfOwner {
                        forceReauthUseCase(exception.reason)
                    }
                } finally {
                    _navigationState.value = SplashNavigationState.NavigateToLogin
                }
            }

            is SessionBootstrapFailure.TemporaryFailure -> {
                // Bounded truthful state: bootstrap is currently unavailable; preserve local auth.
                _navigationState.value = SplashNavigationState.TemporaryFailure
            }

            else -> {
                // Unknown bootstrap failure is treated as temporary/bootstrap-unavailable.
                _navigationState.value = SplashNavigationState.TemporaryFailure
            }
        }
    }
}