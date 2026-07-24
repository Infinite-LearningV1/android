package com.example.infinite_track.presentation.feedback

import androidx.annotation.StringRes
import com.example.infinite_track.R
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

enum class AppFeedbackTimeout(val baseMillis: Long) {
    SHORT(4_000L),
    LONG(8_000L)
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

data class AppFeedbackResourceModel(
    @StringRes val titleRes: Int,
    @StringRes val messageRes: Int,
    val semantic: InfiniteSemantic,
    val timeout: AppFeedbackTimeout
)

fun AppFeedbackEvent.toResourceModel(): AppFeedbackResourceModel = when (this) {
    AppFeedbackEvent.LOGIN_SUCCESS -> AppFeedbackResourceModel(
        titleRes = R.string.app_feedback_login_success_title,
        messageRes = R.string.app_feedback_login_success_message,
        semantic = InfiniteSemantic.Success,
        timeout = AppFeedbackTimeout.SHORT
    )

    AppFeedbackEvent.LOGOUT_SUCCESS -> AppFeedbackResourceModel(
        titleRes = R.string.app_feedback_logout_success_title,
        messageRes = R.string.app_feedback_logout_success_message,
        semantic = InfiniteSemantic.Success,
        timeout = AppFeedbackTimeout.SHORT
    )

    AppFeedbackEvent.LOGOUT_REMOTE_WARNING -> AppFeedbackResourceModel(
        titleRes = R.string.app_feedback_logout_remote_warning_title,
        messageRes = R.string.app_feedback_logout_remote_warning_message,
        semantic = InfiniteSemantic.Warning,
        timeout = AppFeedbackTimeout.LONG
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
