package com.example.infinite_track.presentation.screen.splash

import com.example.infinite_track.domain.manager.SessionManager

internal class SplashBootstrapGate(
    private val sessionManager: SessionManager
) {
    private var bootstrapRunning: Boolean = false

    @Synchronized
    fun beginBootstrap(): Boolean {
        if (bootstrapRunning) {
            return false
        }
        bootstrapRunning = true
        return true
    }

    @Synchronized
    fun finishBootstrap() {
        bootstrapRunning = false
    }

    suspend fun runTerminalLogoutIfOwner(logout: suspend () -> Unit) {
        if (sessionManager.beginSessionExpiryHandling()) {
            logout()
        }
    }
}
