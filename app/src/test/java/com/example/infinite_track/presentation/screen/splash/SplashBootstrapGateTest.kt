package com.example.infinite_track.presentation.screen.splash

import com.example.infinite_track.domain.manager.SessionManager
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SplashBootstrapGateTest {

    @Test
    fun `second bootstrap start is ignored until current cycle finishes`() {
        val gate = SplashBootstrapGate(SessionManager())

        assertTrue(gate.beginBootstrap())
        assertFalse(gate.beginBootstrap())

        gate.finishBootstrap()

        assertTrue(gate.beginBootstrap())
    }

    @Test
    fun `terminal logout runs only once when gate owns session expiry handling`() = runTest {
        val gate = SplashBootstrapGate(SessionManager())
        var logoutCalls = 0

        gate.runTerminalLogoutIfOwner { logoutCalls++ }
        gate.runTerminalLogoutIfOwner { logoutCalls++ }

        assertEquals(1, logoutCalls)
    }

    @Test
    fun `terminal logout is skipped when another path already owns session expiry handling`() = runTest {
        val sessionManager = SessionManager()
        val gate = SplashBootstrapGate(sessionManager)
        var logoutCalls = 0

        assertTrue(sessionManager.beginSessionExpiryHandling())

        gate.runTerminalLogoutIfOwner { logoutCalls++ }

        assertEquals(0, logoutCalls)
    }
}
