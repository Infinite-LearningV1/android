package com.example.infinite_track.data.platform.geofence.store

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.google.gson.Gson
import com.google.gson.JsonParser
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.io.File

class PreferencesGeofenceRuntimeStoreTest {
    private lateinit var tempFile: File
    private lateinit var scope: CoroutineScope
    private lateinit var dataStore: DataStore<Preferences>
    private lateinit var store: PreferencesGeofenceRuntimeStore

    @Before
    fun setUp() {
        tempFile = File.createTempFile("geofence-runtime", ".preferences_pb").also { it.delete() }
        scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        dataStore = PreferenceDataStoreFactory.create(scope = scope) { tempFile }
        store = PreferencesGeofenceRuntimeStore(dataStore, Gson())
    }

    @After
    fun tearDown() {
        scope.cancel()
        tempFile.delete()
    }

    @Test
    fun `writes and reads one v2 snapshot atomically`() = runTest {
        val snapshot = activeSnapshot()

        store.writeSnapshot(snapshot)

        assertEquals(snapshot, store.readSnapshot())
    }

    @Test
    fun `corrupt snapshot returns null instead of throwing`() = runTest {
        dataStore.edit { preferences ->
            preferences[stringPreferencesKey("geofence_runtime_snapshot_v2")] = "{\"schemaVersion\":2}"
        }

        assertNull(store.readSnapshot())
    }

    @Test
    fun `snapshot requires explicit presence of every schema field`() = runTest {
        val snapshot = activeSnapshot(
            effectiveDateIso = null,
            attendanceId = null,
            sessionStateKey = null,
            failureCategory = null
        )
        store.writeSnapshot(snapshot)
        val persisted = JsonParser.parseString(
            dataStore.data.first()[stringPreferencesKey("geofence_runtime_snapshot_v2")]
        ).asJsonObject

        assertTrue(SNAPSHOT_FIELD_NAMES.all(persisted::has))
        SNAPSHOT_FIELD_NAMES.forEach { fieldName ->
            val missingField = persisted.deepCopy().apply { remove(fieldName) }
            dataStore.edit { preferences ->
                preferences[stringPreferencesKey("geofence_runtime_snapshot_v2")] = missingField.toString()
            }

            assertNull("missing $fieldName", store.readSnapshot())
        }
    }

    @Test
    fun `applied snapshot rejects missing or mismatched applied request ids`() = runTest {
        store.writeSnapshot(activeSnapshot(appliedRequestIds = emptySet()))
        assertNull(store.readSnapshot())

        store.writeSnapshot(activeSnapshot(appliedRequestIds = setOf("unexpected-request")))
        assertNull(store.readSnapshot())
    }

    @Test
    fun `claim cooldown allows first suppresses second and allows expiry`() = runTest {
        assertTrue(store.claimNotification("active-enter", nowMillis = 1_000L, cooldownMillis = 100L))
        assertFalse(store.claimNotification("active-enter", nowMillis = 1_050L, cooldownMillis = 100L))
        assertTrue(store.claimNotification("active-enter", nowMillis = 1_100L, cooldownMillis = 100L))
    }

    @Test
    fun `set inside updates only current applied active snapshot`() = runTest {
        store.writeSnapshot(activeSnapshot(insideActiveGeofence = false))
        store.setInsideActiveGeofence(true)
        assertEquals(true, store.readSnapshot()?.insideActiveGeofence)

        val applying = activeSnapshot(
            reconciliationState = PersistedReconciliationState.APPLYING,
            insideActiveGeofence = false
        )
        store.writeSnapshot(applying)
        store.setInsideActiveGeofence(true)
        assertEquals(applying, store.readSnapshot())

        val reminder = activeSnapshot(
            expectedMode = PersistedGeofenceMode.REMINDER,
            insideActiveGeofence = false
        )
        store.writeSnapshot(reminder)
        store.setInsideActiveGeofence(true)
        assertEquals(reminder, store.readSnapshot())
    }

    @Test
    fun `clear removes snapshot and cooldown state`() = runTest {
        store.writeSnapshot(activeSnapshot())
        assertTrue(store.claimNotification("active-enter", nowMillis = 1_000L, cooldownMillis = 100L))

        store.clear()

        assertNull(store.readSnapshot())
        assertTrue(store.claimNotification("active-enter", nowMillis = 1_050L, cooldownMillis = 100L))
    }

    private fun activeSnapshot(
        expectedMode: PersistedGeofenceMode = PersistedGeofenceMode.ACTIVE,
        reconciliationState: PersistedReconciliationState = PersistedReconciliationState.APPLIED,
        insideActiveGeofence: Boolean = false,
        effectiveDateIso: String? = "2026-07-25",
        attendanceId: Int? = 42,
        sessionStateKey: String? = "active",
        appliedRequestIds: Set<String> = setOf("active-42"),
        failureCategory: String? = null
    ) = GeofenceRuntimeSnapshot(
        generation = 7L,
        effectiveDateIso = effectiveDateIso,
        expectedMode = expectedMode,
        attendanceId = attendanceId,
        sessionStateKey = sessionStateKey,
        expectedRegistrations = listOf(
            PersistedGeofenceRegistration(
                requestId = "active-42",
                logicalId = "office",
                kind = PersistedRegistrationKind.ACTIVE,
                label = "Office",
                latitude = -0.898,
                longitude = 119.87,
                radiusMeters = 100.0,
                attendanceId = attendanceId
            )
        ),
        appliedRequestIds = appliedRequestIds,
        reconciliationState = reconciliationState,
        insideActiveGeofence = insideActiveGeofence,
        failureCategory = failureCategory,
        updatedAtEpochMillis = 1_000L
    )

    private companion object {
        val SNAPSHOT_FIELD_NAMES = setOf(
            "schemaVersion",
            "generation",
            "effectiveDateIso",
            "expectedMode",
            "attendanceId",
            "sessionStateKey",
            "expectedRegistrations",
            "appliedRequestIds",
            "reconciliationState",
            "insideActiveGeofence",
            "failureCategory",
            "updatedAtEpochMillis"
        )
    }
}
