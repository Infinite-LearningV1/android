package com.example.infinite_track.domain.use_case.auth

import com.example.infinite_track.domain.manager.SessionManager
import javax.inject.Inject

class ForceReauthUseCase @Inject constructor(
    private val sessionManager: SessionManager,
    private val clearAuthenticatedRuntimeUseCase: ClearAuthenticatedRuntimeUseCase
) {
    suspend operator fun invoke(reason: SessionManager.ReauthReason) {
        if (!sessionManager.beginSessionExpiryHandling()) return
        clearAuthenticatedRuntimeUseCase()
        sessionManager.triggerForcedReauth(reason)
    }
}
