package com.example.infinite_track.presentation.screen.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.infinite_track.domain.model.auth.UserModel
import com.example.infinite_track.domain.use_case.auth.GetLoggedInUserUseCase
import com.example.infinite_track.domain.use_case.auth.LogoutUseCase
import com.example.infinite_track.domain.use_case.language.GetSelectedLanguageUseCase
import com.example.infinite_track.domain.use_case.language.SetSelectedLanguageUseCase
import com.example.infinite_track.utils.UiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val getLoggedInUserUseCase: GetLoggedInUserUseCase,
    private val logoutUseCase: LogoutUseCase,
    private val getSelectedLanguageUseCase: GetSelectedLanguageUseCase,
    private val setSelectedLanguageUseCase: SetSelectedLanguageUseCase
) : ViewModel() {

    // Profile state
    private val _profileState = MutableStateFlow<UiState<UserModel>>(UiState.Loading)
    val profileState: StateFlow<UiState<UserModel>> = _profileState.asStateFlow()

    // Language state
    private val _languageState = MutableStateFlow("en")
    val languageState: StateFlow<String> = _languageState.asStateFlow()

    // Dialog states
    private val _showLogoutDialog = MutableStateFlow(false)
    val showLogoutDialog: StateFlow<Boolean> = _showLogoutDialog.asStateFlow()

    // Language dialog state
    private val _showLanguageDialog = MutableStateFlow(false)
    val showLanguageDialog: StateFlow<Boolean> = _showLanguageDialog.asStateFlow()

    // Navigation state
    private val _navigateToLogin = MutableStateFlow(false)
    val navigateToLogin: StateFlow<Boolean> = _navigateToLogin.asStateFlow()

    // Loading state for logout
    private val _isLoggingOut = MutableStateFlow(false)
    val isLoggingOut: StateFlow<Boolean> = _isLoggingOut.asStateFlow()

    init {
        loadUserProfile()
        loadSelectedLanguage()
    }

    private fun loadUserProfile() {
        viewModelScope.launch {
            _profileState.value = UiState.Loading
            getLoggedInUserUseCase()
                .catch { exception ->
                    _profileState.value =
                        UiState.Error(exception.message ?: "Unknown error occurred")
                }
                .collectLatest { user ->
                    _profileState.value = if (user != null) {
                        UiState.Success(user)
                    } else {
                        UiState.Error("User data not found")
                    }
                }
        }
    }

    private fun loadSelectedLanguage() {
        viewModelScope.launch {
            getSelectedLanguageUseCase()
                .catch { exception ->
                    // Just log the error, don't change UI state for language errors
                    android.util.Log.e(
                        "ProfileViewModel",
                        "Error loading language: ${exception.message}"
                    )
                }
                .collectLatest { language ->
                    _languageState.value = language
                }
        }
    }

    // Language dialog management
    fun onLanguageSettingsClicked() {
        _showLanguageDialog.value = true
    }

    fun onLanguageDialogDismiss() {
        _showLanguageDialog.value = false
    }

    // Persist language selection
    fun onUpdateLanguage(language: String) {
        viewModelScope.launch {
            setSelectedLanguageUseCase(language)
        }
    }

    fun onConfirmLogout() {
        viewModelScope.launch {
            _isLoggingOut.value = true
            _showLogoutDialog.value = false

            logoutUseCase()
                .onSuccess {
                    _isLoggingOut.value = false
                    _navigateToLogin.value = true
                }
                .onFailure {
                    // Even if logout fails, we should still navigate to login
                    // as the token is probably invalid or there's a network issue
                    _isLoggingOut.value = false
                    _navigateToLogin.value = true
                }
        }
    }
}
