package com.example.infinite_track.presentation.screen.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.infinite_track.domain.model.auth.UserModel
import com.example.infinite_track.domain.use_case.auth.GetLoggedInUserUseCase
import com.example.infinite_track.domain.use_case.auth.LogoutOutcome
import com.example.infinite_track.domain.use_case.auth.LogoutUseCase
import com.example.infinite_track.domain.use_case.language.GetSelectedLanguageUseCase
import com.example.infinite_track.domain.use_case.language.SetSelectedLanguageUseCase
import com.example.infinite_track.presentation.feedback.AppFeedbackEmitter
import com.example.infinite_track.presentation.feedback.AppFeedbackEvent
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
    private val setSelectedLanguageUseCase: SetSelectedLanguageUseCase,
    private val appFeedbackEmitter: AppFeedbackEmitter
) : ViewModel() {

    // Profile state
    private val _profileState = MutableStateFlow<UiState<UserModel>>(UiState.Loading)
    val profileState: StateFlow<UiState<UserModel>> = _profileState.asStateFlow()

    // Language state
    private val _languageState = MutableStateFlow("en")
    val languageState: StateFlow<String> = _languageState.asStateFlow()

    // Language dialog state
    private val _showLanguageDialog = MutableStateFlow(false)
    val showLanguageDialog: StateFlow<Boolean> = _showLanguageDialog.asStateFlow()

    private val _uiState = MutableStateFlow<ProfileLogoutUiState>(ProfileLogoutUiState.Idle)
    val uiState: StateFlow<ProfileLogoutUiState> = _uiState.asStateFlow()

    private val _effects = MutableStateFlow<ProfileEffect?>(null)
    val effects: StateFlow<ProfileEffect?> = _effects.asStateFlow()

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

    fun confirmLogout() {
        if (_uiState.value == ProfileLogoutUiState.Submitting) return
        _uiState.value = ProfileLogoutUiState.Submitting

        viewModelScope.launch {
            when (val outcome = logoutUseCase()) {
                LogoutOutcome.Success -> completeLogout(AppFeedbackEvent.LOGOUT_SUCCESS)
                LogoutOutcome.SuccessWithRemoteWarning -> {
                    completeLogout(AppFeedbackEvent.LOGOUT_REMOTE_WARNING)
                }

                is LogoutOutcome.LocalCleanupFailed -> {
                    _uiState.value = ProfileLogoutUiState.LocalCleanupFailure(outcome.cause)
                }
            }
        }
    }

    fun retryLogout() {
        if (_uiState.value is ProfileLogoutUiState.LocalCleanupFailure) {
            confirmLogout()
        }
    }

    fun consumeEffect(expectedEffect: ProfileEffect) {
        if (_effects.value == expectedEffect) {
            _effects.value = null
        }
    }

    private fun completeLogout(feedbackEvent: AppFeedbackEvent) {
        _uiState.value = ProfileLogoutUiState.Idle
        appFeedbackEmitter.emit(feedbackEvent)
        _effects.value = ProfileEffect.NavigateToLogin
    }
}

sealed interface ProfileEffect {
    data object NavigateToLogin : ProfileEffect
}

sealed interface ProfileLogoutUiState {
    data object Idle : ProfileLogoutUiState
    data object Submitting : ProfileLogoutUiState
    data class LocalCleanupFailure(val cause: Throwable) : ProfileLogoutUiState
}
