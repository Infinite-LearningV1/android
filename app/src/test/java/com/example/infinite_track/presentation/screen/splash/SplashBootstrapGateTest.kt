package com.example.infinite_track.presentation.screen.splash

import com.example.infinite_track.domain.manager.SessionManager
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

class SplashBootstrapGateTest {

    @Test
    fun `second bootstrap start is ignored until current cycle finishes`() = runTest {
        val gate = SplashBootstrapGate()
        var bootstrapCalls = 0

        val firstRunStarted = gate.runBootstrapIfIdle {
            bootstrapCalls++

            val secondRunStarted = gate.runBootstrapIfIdle {
                bootstrapCalls++
            }

            assertFalse(secondRunStarted)
        }

        val thirdRunStarted = gate.runBootstrapIfIdle {
            bootstrapCalls++
        }

        assertTrue(firstRunStarted)
        assertTrue(thirdRunStarted)
        assertEquals(2, bootstrapCalls)
    }

    @Test
    fun `bootstrap gate releases when block throws`() = runTest {
        val gate = SplashBootstrapGate()
        val failure = IllegalStateException("boom")
        var bootstrapCalls = 0

        try {
            gate.runBootstrapIfIdle {
                bootstrapCalls++
                throw failure
            }
            fail("Expected bootstrap failure to be rethrown")
        } catch (throwable: IllegalStateException) {
            assertSame(failure, throwable)
        }

        val nextRunStarted = gate.runBootstrapIfIdle {
            bootstrapCalls++
        }

        assertTrue(nextRunStarted)
        assertEquals(2, bootstrapCalls)
    }

    @Test
    fun `bootstrap gate releases when block is cancelled`() = runTest {
        val gate = SplashBootstrapGate()
        val cancellation = CancellationException("cancelled")
        var bootstrapCalls = 0

        try {
            gate.runBootstrapIfIdle {
                bootstrapCalls++
                throw cancellation
            }
            fail("Expected bootstrap cancellation to be rethrown")
        } catch (throwable: CancellationException) {
            assertSame(cancellation, throwable)
        }

        val nextRunStarted = gate.runBootstrapIfIdle {
            bootstrapCalls++
        }

        assertTrue(nextRunStarted)
        assertEquals(2, bootstrapCalls)
    }

    @Test
    fun `terminal logout runs once per bootstrap cycle`() = runTest {
        val gate = SplashBootstrapGate()
        var logoutCalls = 0

        gate.runBootstrapIfIdle {
            gate.runTerminalLogoutIfOwner { logoutCalls++ }
            gate.runTerminalLogoutIfOwner { logoutCalls++ }
        }
        gate.runBootstrapIfIdle {
            gate.runTerminalLogoutIfOwner { logoutCalls++ }
        }

        assertEquals(2, logoutCalls)
    }

    @Test
    fun `bootstrap terminal logout does not consume runtime forced reauth guard`() = runTest {
        val sessionManager = SessionManager()
        val gate = SplashBootstrapGate()
        var logoutCalls = 0

        gate.runBootstrapIfIdle {
            gate.runTerminalLogoutIfOwner { logoutCalls++ }
        }

        assertEquals(1, logoutCalls)
        assertTrue(sessionManager.beginSessionExpiryHandling())
        sessionManager.triggerForcedReauth(ReauthReason.REFRESH_REVOKED)
        assertTrue(sessionManager.sessionExpired.value)
        assertEquals(ReauthReason.REFRESH_REVOKED, sessionManager.reauthReason.value)
    }
}
