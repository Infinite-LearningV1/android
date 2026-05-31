package com.example.infinite_track.presentation.screen.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.infinite_track.domain.manager.SessionManager
import com.example.infinite_track.domain.use_case.auth.LoginUseCase
import com.example.infinite_track.utils.UiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class LoginViewModel @Inject constructor(
    private val loginUseCase: LoginUseCase,
    private val sessionManager: SessionManager
) : ViewModel() {

    // Define login state as UiState<Unit> since we only need to track the login process status
    private val _loginState = MutableStateFlow<UiState<Unit>>(UiState.Idle)
    val loginState: StateFlow<UiState<Unit>> = _loginState.asStateFlow()

    private val _reauthBannerMessage = MutableStateFlow<String?>(null)
    val reauthBannerMessage: StateFlow<String?> = _reauthBannerMessage.asStateFlow()

    init {
        viewModelScope.launch {
            sessionManager.reauthReason.collect { reason ->
                _reauthBannerMessage.value = reason?.toBannerMessage()
            }
        }
    }

    /**
     * Login with email and password
     * @param email User's email
     * @param password User's password
     */
    fun login(email: String, password: String) {
        viewModelScope.launch {
            // Set state to loading before API call
            _loginState.value = UiState.Loading

            // Call login use case
            loginUseCase(email, password)
                .onSuccess {
                    // Login successful, update state to success
                    _loginState.value = UiState.Success(Unit)
                }
                .onFailure { exception ->
                    // Login failed, update state to error with message
                    _loginState.value = UiState.Error(exception.message ?: "Unknown error occurred")
                }
        }
    }

    fun dismissReauthBanner() {
        _reauthBannerMessage.value = null
        sessionManager.resetSessionExpired()
    }

    /**
     * Reset login state to idle
     * Call this after navigation to prevent showing dialogs again if user comes back to this screen
     */
    fun resetState() {
        _loginState.value = UiState.Idle
    }

    private fun SessionManager.ReauthReason.toBannerMessage(): String {
        return when (this) {
            SessionManager.ReauthReason.INACTIVITY_EXPIRED ->
                "Sesi tidak aktif lebih dari 48 jam. Silakan login lagi."
            SessionManager.ReauthReason.REFRESH_INVALID ->
                "Sesi tidak valid lagi. Silakan login lagi."
            SessionManager.ReauthReason.REFRESH_REVOKED ->
                "Sesi sudah berakhir. Silakan login lagi."
            SessionManager.ReauthReason.NETWORK_OFFLINE_AT_REFRESH ->
                "Tidak dapat memvalidasi sesi karena jaringan. Coba lagi setelah online."
            SessionManager.ReauthReason.UNKNOWN ->
                "Sesi Anda telah berakhir. Silakan login kembali."
        }
    }
}
