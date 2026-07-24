package com.example.infinite_track.domain.manager

import com.example.infinite_track.domain.model.auth.ReauthReason
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SessionManagerTest {
    @Test
    fun `triggerForcedReauth exposes reason and marks legacy session expired`() {
        val sessionManager = SessionManager()

        sessionManager.triggerForcedReauth(ReauthReason.INACTIVITY_EXPIRED)

        assertTrue(sessionManager.sessionExpired.value)
        assertEquals(ReauthReason.INACTIVITY_EXPIRED, sessionManager.reauthReason.value)
    }

    @Test
    fun `reset clears forced reauth reason and releases single flight guard`() {
        val sessionManager = SessionManager()

        assertTrue(sessionManager.beginSessionExpiryHandling())
        assertFalse(sessionManager.beginSessionExpiryHandling())
        sessionManager.triggerForcedReauth(ReauthReason.REFRESH_REVOKED)

        sessionManager.resetSessionExpired()

        assertFalse(sessionManager.sessionExpired.value)
        assertEquals(null, sessionManager.reauthReason.value)
        assertTrue(sessionManager.beginSessionExpiryHandling())
    }

    @Test
    fun `manual logout reset clears stale unknown reauth before login observes it`() {
        val sessionManager = SessionManager()
        assertTrue(sessionManager.beginSessionExpiryHandling())
        sessionManager.triggerForcedReauth(ReauthReason.UNKNOWN)

        sessionManager.resetSessionExpired()

        assertFalse(sessionManager.sessionExpired.value)
        assertEquals(null, sessionManager.reauthReason.value)
        assertTrue(sessionManager.beginSessionExpiryHandling())
    }

    @Suppress("DEPRECATION")
    @Test
    fun `legacy triggerSessionExpired maps to unknown reauth reason`() {
        val sessionManager = SessionManager()

        sessionManager.triggerSessionExpired()

        assertTrue(sessionManager.sessionExpired.value)
        assertEquals(ReauthReason.UNKNOWN, sessionManager.reauthReason.value)
    }

    @Test
    fun `bootstrap session guard stays active until all owners end`() {
        val sessionManager = SessionManager()

        assertFalse(sessionManager.isBootstrapSessionInProgress)

        sessionManager.beginBootstrapSession()
        sessionManager.beginBootstrapSession()

        assertTrue(sessionManager.isBootstrapSessionInProgress)

        sessionManager.endBootstrapSession()

        assertTrue(sessionManager.isBootstrapSessionInProgress)

        sessionManager.endBootstrapSession()

        assertFalse(sessionManager.isBootstrapSessionInProgress)
    }

    @Test
    fun `extra bootstrap end is harmless`() {
        val sessionManager = SessionManager()

        sessionManager.endBootstrapSession()

        assertFalse(sessionManager.isBootstrapSessionInProgress)
    }
}
