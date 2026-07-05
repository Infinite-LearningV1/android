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

class UserPreferenceProfileFreshnessTest {
    private lateinit var tempFile: File
    private lateinit var scope: CoroutineScope
    private lateinit var dataStore: DataStore<Preferences>
    private lateinit var userPreference: UserPreference

    @Before
    fun setUp() {
        tempFile = File.createTempFile("user-profile-freshness", ".preferences_pb").also { it.delete() }
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
    fun `profile sync timestamp defaults to zero persists and clearSession removes it`() = runBlocking {
        assertEquals(0L, userPreference.getLastProfileSyncAt().first())

        userPreference.saveLastProfileSyncAt(1_720_000_000_000L)

        assertEquals(1_720_000_000_000L, userPreference.getLastProfileSyncAt().first())

        userPreference.clearSession()

        assertEquals(0L, userPreference.getLastProfileSyncAt().first())
    }
}
