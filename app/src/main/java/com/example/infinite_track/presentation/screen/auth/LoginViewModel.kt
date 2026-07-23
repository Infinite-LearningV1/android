package com.example.infinite_track.presentation.screen.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.infinite_track.domain.manager.SessionManager
import com.example.infinite_track.domain.model.auth.ReauthReason
import com.example.infinite_track.domain.model.auth.UserModel
import com.example.infinite_track.domain.use_case.auth.LoginUseCase
import com.example.infinite_track.presentation.feedback.AppFeedbackEmitter
import com.example.infinite_track.presentation.feedback.AppFeedbackEvent
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ViewModelComponent
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class LoginViewModel @Inject constructor(
    private val loginExecutor: LoginExecutor,
    private val sessionManager: SessionManager,
    private val appFeedbackEmitter: AppFeedbackEmitter
) : ViewModel() {

    private val _uiState = MutableStateFlow<LoginUiState>(LoginUiState.Idle)
    val uiState: StateFlow<LoginUiState> = _uiState.asStateFlow()

    private val _effects = MutableSharedFlow<LoginEffect>(
        replay = 0,
        extraBufferCapacity = 1
    )
    val effects: SharedFlow<LoginEffect> = _effects.asSharedFlow()

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
        if (_uiState.value == LoginUiState.Loading) return
        _uiState.value = LoginUiState.Loading

        viewModelScope.launch {
            loginExecutor.execute(email, password)
                .onSuccess {
                    _uiState.value = LoginUiState.Idle
                    appFeedbackEmitter.emit(AppFeedbackEvent.LOGIN_SUCCESS)
                    _effects.emit(LoginEffect.NavigateHome)
                }
                .onFailure { exception ->
                    _uiState.value = LoginUiState.Failure(
                        exception.message ?: "Unknown error occurred"
                    )
                }
        }
    }

    fun dismissReauthBanner() {
        _reauthBannerMessage.value = null
        sessionManager.resetSessionExpired()
    }

    fun dismissFailure(message: String) {
        if (_uiState.value == LoginUiState.Failure(message)) {
            _uiState.value = LoginUiState.Idle
        }
    }

    private fun ReauthReason.toBannerMessage(): String {
        return toReauthUiCopy().bannerMessage
    }
}

sealed interface LoginEffect {
    data object NavigateHome : LoginEffect
}

sealed interface LoginUiState {
    data object Idle : LoginUiState
    data object Loading : LoginUiState
    data class Failure(val message: String) : LoginUiState
}

fun interface LoginExecutor {
    suspend fun execute(email: String, password: String): Result<UserModel>
}

class LoginUseCaseExecutor @Inject constructor(
    private val loginUseCase: LoginUseCase
) : LoginExecutor {
    override suspend fun execute(email: String, password: String): Result<UserModel> =
        loginUseCase(email, password)
}

@Module
@InstallIn(ViewModelComponent::class)
abstract class LoginExecutorModule {
    @Binds
    abstract fun bindLoginExecutor(implementation: LoginUseCaseExecutor): LoginExecutor
}
