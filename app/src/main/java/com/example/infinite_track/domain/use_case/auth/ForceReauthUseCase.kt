package com.example.infinite_track.domain.use_case.auth

import com.example.infinite_track.domain.manager.SessionManager
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

    suspend operator fun invoke(reason: SessionManager.ReauthReason) {
        if (!sessionManager.beginSessionExpiryHandling()) return

        try {
            clearAuthenticatedRuntime()
        } finally {
            sessionManager.triggerForcedReauth(reason)
        }
    }
}
