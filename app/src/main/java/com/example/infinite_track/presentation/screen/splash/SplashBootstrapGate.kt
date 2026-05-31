package com.example.infinite_track.presentation.screen.splash

import com.example.infinite_track.domain.manager.SessionManager

internal class SplashBootstrapGate(
    private val sessionManager: SessionManager
) {
    private var bootstrapRunning: Boolean = false

    suspend fun runBootstrapIfIdle(block: suspend () -> Unit): Boolean {
        if (!acquireBootstrap()) {
            return false
        }

        try {
            block()
        } finally {
            releaseBootstrap()
        }

        return true
    }

    @Synchronized
    private fun acquireBootstrap(): Boolean {
        if (bootstrapRunning) {
            return false
        }
        bootstrapRunning = true
        return true
    }

    @Synchronized
    private fun releaseBootstrap() {
        bootstrapRunning = false
    }

    suspend fun runTerminalLogoutIfOwner(logout: suspend () -> Unit) {
        if (sessionManager.beginSessionExpiryHandling()) {
            logout()
        }
    }
}
