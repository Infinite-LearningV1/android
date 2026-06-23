package com.example.infinite_track.presentation.main

import android.util.Log
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import com.example.infinite_track.di.ApplicationCoroutineScope
import com.example.infinite_track.domain.manager.SessionManager
import com.example.infinite_track.domain.use_case.auth.ForegroundSessionValidationResult
import com.example.infinite_track.domain.use_case.auth.LogoutUseCase
import com.example.infinite_track.domain.use_case.auth.ValidateForegroundSessionUseCase
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Provider
import javax.inject.Singleton

@Singleton
class ForegroundSessionLifecycleObserver internal constructor(
    private val validateForegroundSessionUseCase: ValidateForegroundSessionUseCase,
    private val sessionManager: SessionManager,
    private val logoutUseCaseProvider: Provider<LogoutUseCase>,
    private val applicationScope: CoroutineScope,
    private val gate: ForegroundSessionResumeGate = ForegroundSessionResumeGate(),
    private val unexpectedFailureLogger: (Throwable) -> Unit = { throwable ->
        Log.w(TAG, "Foreground session validation failed unexpectedly; preserving local session state", throwable)
    }
) : DefaultLifecycleObserver {

    @Inject
    constructor(
        validateForegroundSessionUseCase: ValidateForegroundSessionUseCase,
        sessionManager: SessionManager,
        logoutUseCaseProvider: Provider<LogoutUseCase>,
        @ApplicationCoroutineScope applicationScope: CoroutineScope
    ) : this(
        validateForegroundSessionUseCase = validateForegroundSessionUseCase,
        sessionManager = sessionManager,
        logoutUseCaseProvider = logoutUseCaseProvider,
        applicationScope = applicationScope
    )

    internal var validationCount: Int = 0
        private set

    override fun onStart(owner: LifecycleOwner) {
        if (!gate.tryAcquire(sessionManager.isBootstrapSessionInProgress)) {
            return
        }

        applicationScope.launch {
            try {
                try {
                    validationCount += 1
                    when (val result = validateForegroundSessionUseCase()) {
                        ForegroundSessionValidationResult.Skipped -> Unit
                        ForegroundSessionValidationResult.Valid -> Unit
                        is ForegroundSessionValidationResult.TemporaryFailure -> Unit
                        is ForegroundSessionValidationResult.ReauthRequired -> {
                            if (sessionManager.beginSessionExpiryHandling()) {
                                try {
                                    logoutUseCaseProvider.get().invoke()
                                } finally {
                                    sessionManager.triggerForcedReauth(result.reason)
                                }
                            }
                        }
                    }
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    unexpectedFailureLogger(e)
                    // Treat unexpected validator failures like temporary foreground transport failures.
                }
            } finally {
                gate.release()
            }
        }
    }

    private companion object {
        const val TAG = "ForegroundSession"
    }
}
