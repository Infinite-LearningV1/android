package com.example.infinite_track.presentation.screen.splash

internal class SplashBootstrapGate {
    private var bootstrapRunning: Boolean = false
    private var terminalLogoutCompleted: Boolean = false

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
        terminalLogoutCompleted = false
        return true
    }

    @Synchronized
    private fun releaseBootstrap() {
        bootstrapRunning = false
    }

    suspend fun runTerminalLogoutIfOwner(logout: suspend () -> Unit) {
        if (acquireTerminalLogout()) {
            logout()
        }
    }

    @Synchronized
    private fun acquireTerminalLogout(): Boolean {
        if (terminalLogoutCompleted) {
            return false
        }
        terminalLogoutCompleted = true
        return true
    }
}
