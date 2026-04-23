package com.example.infinite_track.domain.manager

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

    private var sessionExpiryHandlingInProgress: Boolean = false

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

    /**
     * Trigger session expiration
     * Dipanggil oleh AuthInterceptor ketika mendapat 401 error
     */
    fun triggerSessionExpired() {
        _sessionExpired.value = true
    }

    /**
     * Reset session expiration state
     * Dipanggil setelah user dismiss dialog atau navigate ke login
     */
    @Synchronized
    fun resetSessionExpired() {
        _sessionExpired.value = false
        sessionExpiryHandlingInProgress = false
    }
}
