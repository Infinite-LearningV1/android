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
        if (sessionExpiryHandlingInProgress) {
            return false
        }

        sessionExpiryHandlingInProgress = true
        return true
    }

    fun triggerForcedReauth(reason: ReauthReason) {
        _reauthReason.value = reason
        _sessionExpired.value = true
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
