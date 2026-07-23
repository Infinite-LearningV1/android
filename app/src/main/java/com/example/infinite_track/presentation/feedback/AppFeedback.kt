package com.example.infinite_track.presentation.feedback

import androidx.compose.material3.SnackbarDuration
import com.example.infinite_track.presentation.design.components.status.InfiniteSnackbarVisuals
import com.example.infinite_track.presentation.design.tokens.InfiniteSemantic
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow

enum class AppFeedbackEvent {
    LOGIN_SUCCESS,
    LOGOUT_SUCCESS,
    LOGOUT_REMOTE_WARNING
}

fun interface AppFeedbackEmitter {
    fun emit(event: AppFeedbackEvent)
}

@Singleton
class AppFeedbackController @Inject constructor() : AppFeedbackEmitter {
    private val _events = MutableSharedFlow<AppFeedbackEvent>(
        replay = 0,
        extraBufferCapacity = 1
    )
    val events: SharedFlow<AppFeedbackEvent> = _events.asSharedFlow()

    override fun emit(event: AppFeedbackEvent) {
        _events.tryEmit(event)
    }
}

fun AppFeedbackEvent.toSnackbarVisuals(): InfiniteSnackbarVisuals = when (this) {
    AppFeedbackEvent.LOGIN_SUCCESS -> InfiniteSnackbarVisuals(
        title = "Login successful",
        message = "Welcome back to Infinite Track.",
        semantic = InfiniteSemantic.Success,
        duration = SnackbarDuration.Short
    )

    AppFeedbackEvent.LOGOUT_SUCCESS -> InfiniteSnackbarVisuals(
        title = "Logout successful",
        message = "You have been logged out safely.",
        semantic = InfiniteSemantic.Success,
        duration = SnackbarDuration.Short
    )

    AppFeedbackEvent.LOGOUT_REMOTE_WARNING -> InfiniteSnackbarVisuals(
        title = "Logged out on this device",
        message = "The remote session could not be ended, but local data was cleared.",
        semantic = InfiniteSemantic.Warning,
        duration = SnackbarDuration.Long
    )
}

@Module
@InstallIn(SingletonComponent::class)
abstract class AppFeedbackModule {
    @Binds
    abstract fun bindAppFeedbackEmitter(
        controller: AppFeedbackController
    ): AppFeedbackEmitter
}
