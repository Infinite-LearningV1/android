package com.example.infinite_track.presentation.main

import android.util.Log
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import com.example.infinite_track.di.ApplicationCoroutineScope
import com.example.infinite_track.domain.manager.SessionManager
import com.example.infinite_track.domain.use_case.auth.ForegroundSessionValidationResult
import com.example.infinite_track.domain.use_case.auth.ForceReauthUseCase
import com.example.infinite_track.domain.use_case.auth.ValidateForegroundSessionUseCase
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Provider
import javax.inject.Singleton

@Singleton
class ForegroundSessionLifecycleObserver private constructor(
    private val validateForegroundSessionUseCase: ValidateForegroundSessionUseCase,
    private val sessionManager: SessionManager,
    private val forceReauthUseCaseProvider: Provider<ForceReauthUseCase>,
    private val applicationScope: CoroutineScope,
    private val gate: ForegroundSessionResumeGate,
    private val unexpectedFailureLogger: (Throwable) -> Unit
) : DefaultLifecycleObserver {

    @Inject
    constructor(
        validateForegroundSessionUseCase: ValidateForegroundSessionUseCase,
        sessionManager: SessionManager,
        forceReauthUseCaseProvider: Provider<ForceReauthUseCase>,
        @ApplicationCoroutineScope applicationScope: CoroutineScope
    ) : this(
        validateForegroundSessionUseCase = validateForegroundSessionUseCase,
        sessionManager = sessionManager,
        forceReauthUseCaseProvider = forceReauthUseCaseProvider,
        applicationScope = applicationScope,
        gate = ForegroundSessionResumeGate(),
        unexpectedFailureLogger = { throwable ->
            Log.w(TAG, "Foreground session validation failed unexpectedly; preserving local session state", throwable)
        }
    )

    companion object {
        private const val TAG = "ForegroundSession"

        internal fun createForTest(
            validateForegroundSessionUseCase: ValidateForegroundSessionUseCase,
            sessionManager: SessionManager,
            forceReauthUseCaseProvider: Provider<ForceReauthUseCase>,
            applicationScope: CoroutineScope,
            gate: ForegroundSessionResumeGate,
            unexpectedFailureLogger: (Throwable) -> Unit = { throwable ->
                Log.w(TAG, "Foreground session validation failed unexpectedly; preserving local session state", throwable)
            }
        ): ForegroundSessionLifecycleObserver {
            return ForegroundSessionLifecycleObserver(
                validateForegroundSessionUseCase = validateForegroundSessionUseCase,
                sessionManager = sessionManager,
                forceReauthUseCaseProvider = forceReauthUseCaseProvider,
                applicationScope = applicationScope,
                gate = gate,
                unexpectedFailureLogger = unexpectedFailureLogger
            )
        }
    }

    internal var validationCount: Int = 0
        private set

    override fun onStart(owner: LifecycleOwner) {
        if (!gate.tryAcquire(sessionManager.isBootstrapSessionInProgress)) {
            return
        }

        applicationScope.launch {
            try {
                validationCount += 1
                when (val result = validateForegroundSessionUseCase()) {
                    ForegroundSessionValidationResult.Skipped -> Unit
                    ForegroundSessionValidationResult.Valid -> Unit
                    is ForegroundSessionValidationResult.TemporaryFailure -> Unit
                    is ForegroundSessionValidationResult.ReauthRequired -> {
                        forceReauthUseCaseProvider.get().invoke(result.reason)
                    }
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                unexpectedFailureLogger(e)
                throw e
            } finally {
                gate.release()
            }
        }
    }
}
