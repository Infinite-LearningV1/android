package com.example.infinite_track.domain.use_case.auth

import com.example.infinite_track.domain.manager.SessionManager
import com.example.infinite_track.domain.model.auth.ReauthReason
import kotlinx.coroutines.CancellationException
import javax.inject.Inject

class ForceReauthUseCase internal constructor(
    private val sessionManager: SessionManager,
    private val clearAuthenticatedRuntime: suspend () -> Unit
) {
    @Inject
    constructor(
        sessionManager: SessionManager,
        clearAuthenticatedRuntimeUseCase: ClearAuthenticatedRuntimeUseCase
    ) : this(sessionManager, clearAuthenticatedRuntimeUseCase::invoke)

    suspend operator fun invoke(reason: ReauthReason) {
        val handlingAttempt = sessionManager.beginSessionExpiryHandlingAttempt() ?: return

        var cancellation: CancellationException? = null
        try {
            clearAuthenticatedRuntime()
        } catch (e: CancellationException) {
            cancellation = e
        } catch (_: Exception) {
            // Forced re-auth is driven by a terminal backend/session outcome. Best-effort
            // local cleanup failures must not replace that outcome for interceptor or
            // foreground callers; ClearAuthenticatedRuntimeUseCase still attempts every
            // cleanup step before surfacing its aggregate failure here.
        } finally {
            sessionManager.completeSessionExpiryHandling(handlingAttempt, reason)
        }

        cancellation?.let { throw it }
    }
}
