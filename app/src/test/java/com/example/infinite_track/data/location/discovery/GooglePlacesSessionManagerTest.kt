package com.example.infinite_track.data.location.discovery

import org.junit.Assert.assertNotSame
import org.junit.Assert.assertSame
import org.junit.Test

class GooglePlacesSessionManagerTest {
    @Test
    fun `current token is stable throughout one search session`() {
        val manager = GooglePlacesSessionManager()

        assertSame(manager.current(), manager.current())
    }

    @Test
    fun `renew starts a different session`() {
        val manager = GooglePlacesSessionManager()
        val first = manager.current()

        manager.renew()

        assertNotSame(first, manager.current())
    }
}
