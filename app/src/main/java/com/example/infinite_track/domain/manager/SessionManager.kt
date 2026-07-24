package com.example.infinite_track.domain.manager

import com.example.infinite_track.domain.model.auth.ReauthReason
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Manager untuk menangani state sesi aplikasi
 * Termasuk notifikasi ketika sesi berakhir (401 error)
 */
@Singleton
class SessionManager @Inject constructor() {
    private val _sessionExpired = MutableStateFlow(false)
    val sessionExpired: StateFlow<Boolean> = _sessionExpired.asStateFlow()

    private val _reauthReason = MutableStateFlow<ReauthReason?>(null)
    val reauthReason: StateFlow<ReauthReason?> = _reauthReason.asStateFlow()

    private var sessionExpiryHandlingInProgress: Boolean = false
    private var bootstrapSessionDepth: Int = 0
    private var authLifecycleGeneration: Long = 0L
    private var intentionalLogoutActive: Boolean = false

    val isBootstrapSessionInProgress: Boolean
        @Synchronized get() = bootstrapSessionDepth > 0

    @Synchronized
    fun beginBootstrapSession() {
        bootstrapSessionDepth += 1
    }

    @Synchronized
    fun endBootstrapSession() {
        if (bootstrapSessionDepth > 0) {
            bootstrapSessionDepth -= 1
        }
    }

    /**
     * Try to acquire single-flight guard for session-expired handling.
     * Returns true only for the first caller until resetSessionExpired is invoked.
     */
    @Synchronized
    fun beginSessionExpiryHandling(): Boolean {
        return beginSessionExpiryHandlingAttempt() != null
    }

    @Synchronized
    internal fun beginSessionExpiryHandlingAttempt(): SessionExpiryHandlingAttempt? {
        if (sessionExpiryHandlingInProgress || intentionalLogoutActive) return null

        sessionExpiryHandlingInProgress = true
        return SessionExpiryHandlingAttempt(authLifecycleGeneration)
    }

    @Synchronized
    internal fun completeSessionExpiryHandling(
        attempt: SessionExpiryHandlingAttempt,
        reason: ReauthReason
    ): Boolean {
        if (
            attempt.authLifecycleGeneration != authLifecycleGeneration ||
            intentionalLogoutActive ||
            !sessionExpiryHandlingInProgress
        ) {
            return false
        }

        _reauthReason.value = reason
        _sessionExpired.value = true
        return true
    }

    @Synchronized
    fun triggerForcedReauth(reason: ReauthReason): Boolean {
        if (intentionalLogoutActive) return false

        _reauthReason.value = reason
        _sessionExpired.value = true
        return true
    }

    fun recordBootstrapReauth(reason: ReauthReason) {
        _reauthReason.value = reason
    }

    /**
     * Trigger session expiration
     * Dipanggil oleh AuthInterceptor ketika mendapat 401 error
     */
    @Deprecated("Use triggerForcedReauth(reason) so callers preserve the re-auth reason.")
    fun triggerSessionExpired() {
        triggerForcedReauth(ReauthReason.UNKNOWN)
    }

    fun resetReauthReason() {
        _reauthReason.value = null
    }

    @Synchronized
    internal fun beginIntentionalLogout(): IntentionalLogoutAttempt {
        val restoreExpiryGuardOnFailure =
            sessionExpiryHandlingInProgress && _sessionExpired.value
        authLifecycleGeneration += 1L
        intentionalLogoutActive = true
        sessionExpiryHandlingInProgress = false
        return IntentionalLogoutAttempt(
            authLifecycleGeneration = authLifecycleGeneration,
            restoreExpiryGuardOnFailure = restoreExpiryGuardOnFailure
        )
    }

    @Synchronized
    internal fun cancelIntentionalLogout(attempt: IntentionalLogoutAttempt) {
        if (attempt.authLifecycleGeneration != authLifecycleGeneration) return

        intentionalLogoutActive = false
        sessionExpiryHandlingInProgress = attempt.restoreExpiryGuardOnFailure
    }

    @Synchronized
    internal fun completeIntentionalLogout(attempt: IntentionalLogoutAttempt) {
        if (attempt.authLifecycleGeneration != authLifecycleGeneration) return

        _sessionExpired.value = false
        _reauthReason.value = null
        sessionExpiryHandlingInProgress = false
        // Keep terminal 401 handling suppressed until a new login succeeds. Requests from
        // the previous authenticated generation can still finish after local cleanup.
        intentionalLogoutActive = true
    }

    @Synchronized
    fun onAuthenticatedSessionStarted() {
        authLifecycleGeneration += 1L
        intentionalLogoutActive = false
        _sessionExpired.value = false
        _reauthReason.value = null
        sessionExpiryHandlingInProgress = false
    }

    /**
     * Reset session expiration state
     * Dipanggil setelah user dismiss dialog, atau setelah local cleanup pada manual logout
     * berhasil dan sebelum feedback/navigation ke Login.
     */
    @Synchronized
    fun resetSessionExpired() {
        _sessionExpired.value = false
        resetReauthReason()
        sessionExpiryHandlingInProgress = false
    }
}

internal data class SessionExpiryHandlingAttempt(
    val authLifecycleGeneration: Long
)

internal data class IntentionalLogoutAttempt(
    val authLifecycleGeneration: Long,
    val restoreExpiryGuardOnFailure: Boolean
)
