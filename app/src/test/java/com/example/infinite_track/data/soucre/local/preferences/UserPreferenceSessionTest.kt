package com.example.infinite_track.data.soucre.local.preferences

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import java.io.File

class UserPreferenceSessionTest {
    private lateinit var tempFile: File
    private lateinit var scope: CoroutineScope
    private lateinit var dataStore: DataStore<Preferences>
    private lateinit var userPreference: UserPreference

    @Before
    fun setUp() {
        tempFile = File.createTempFile("user-session", ".preferences_pb").also { it.delete() }
        scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        dataStore = PreferenceDataStoreFactory.create(scope = scope) { tempFile }
        userPreference = UserPreference(dataStore)
    }

    @After
    fun tearDown() {
        scope.cancel()
        tempFile.delete()
    }

    @Test
    fun `saveSession persists access refresh user id and last refresh timestamp`() = runBlocking {
        userPreference.saveSession(
            token = "access-token-redacted",
            refreshToken = "refresh-token-redacted",
            userId = "147",
            lastRefreshAt = 1_717_000_000_000L
        )

        assertEquals("access-token-redacted", userPreference.getAuthToken().first())
        assertEquals("refresh-token-redacted", userPreference.getRefreshToken().first())
        assertEquals("147", userPreference.getUserId().first())
        assertEquals(1_717_000_000_000L, userPreference.getLastRefreshAt().first())
    }

    @Test
    fun `clearSession removes all persisted session values`() = runBlocking {
        userPreference.saveSession(
            token = "access-token-redacted",
            refreshToken = "refresh-token-redacted",
            userId = "147",
            lastRefreshAt = 1_717_000_000_000L
        )

        userPreference.clearSession()

        assertEquals("", userPreference.getAuthToken().first())
        assertEquals("", userPreference.getRefreshToken().first())
        assertEquals("", userPreference.getUserId().first())
        assertEquals(0L, userPreference.getLastRefreshAt().first())
    }
}
