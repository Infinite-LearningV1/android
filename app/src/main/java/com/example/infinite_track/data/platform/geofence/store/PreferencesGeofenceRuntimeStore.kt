package com.example.infinite_track.data.platform.geofence.store

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.google.gson.Gson
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton

private val Context.geofenceRuntimeDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "geofence_runtime_v2"
)

@Singleton
class PreferencesGeofenceRuntimeStore internal constructor(
    private val dataStore: DataStore<Preferences>,
    private val gson: Gson
) : GeofenceRuntimeStore {

    @Inject
    constructor(
        @ApplicationContext context: Context,
        gson: Gson
    ) : this(context.geofenceRuntimeDataStore, gson)

    override suspend fun readSnapshot(): GeofenceRuntimeSnapshot? =
        dataStore.data.first()[SNAPSHOT_KEY].toSnapshotOrNull()

    override suspend fun writeSnapshot(snapshot: GeofenceRuntimeSnapshot) {
        dataStore.edit { preferences ->
            preferences[SNAPSHOT_KEY] = gson.toJson(snapshot)
        }
    }

    override suspend fun claimNotification(
        key: String,
        nowMillis: Long,
        cooldownMillis: Long
    ): Boolean {
        var allowed = false
        dataStore.edit { preferences ->
            val cooldowns = preferences[NOTIFICATION_COOLDOWNS_KEY].orEmpty()
            val lastShownAt = cooldowns
                .firstOrNull { it.startsWith("$key|") }
                ?.substringAfter("|")
                ?.toLongOrNull()

            allowed = lastShownAt == null || nowMillis - lastShownAt >= cooldownMillis
            if (allowed) {
                preferences[NOTIFICATION_COOLDOWNS_KEY] = cooldowns
                    .filterNot { it.startsWith("$key|") }
                    .plus("$key|$nowMillis")
                    .toSet()
            }
        }
        return allowed
    }

    override suspend fun setInsideActiveGeofence(isInside: Boolean) {
        dataStore.edit { preferences ->
            val snapshot = preferences[SNAPSHOT_KEY].toSnapshotOrNull() ?: return@edit
            if (
                snapshot.expectedMode != PersistedGeofenceMode.ACTIVE ||
                snapshot.reconciliationState != PersistedReconciliationState.APPLIED
            ) {
                return@edit
            }

            preferences[SNAPSHOT_KEY] = gson.toJson(
                snapshot.copy(insideActiveGeofence = isInside)
            )
        }
    }

    override suspend fun clear() {
        dataStore.edit { preferences ->
            preferences.remove(SNAPSHOT_KEY)
            preferences.remove(NOTIFICATION_COOLDOWNS_KEY)
        }
    }

    private fun String?.toSnapshotOrNull(): GeofenceRuntimeSnapshot? =
        this
            ?.takeIf { it.isNotBlank() }
            ?.let { json -> runCatching { gson.fromJson(json, GeofenceRuntimeSnapshot::class.java) }.getOrNull() }
            ?.takeIf { it.isValidV2() }

    private fun GeofenceRuntimeSnapshot.isValidV2(): Boolean = runCatching {
        require(schemaVersion == SCHEMA_VERSION)
        require(expectedMode.name.isNotBlank())
        require(reconciliationState.name.isNotBlank())
        expectedRegistrations.forEach { registration ->
            require(registration.requestId.isNotBlank())
            require(registration.logicalId.isNotBlank())
            require(registration.kind.name.isNotBlank())
            require(registration.label.isNotBlank())
        }
        appliedRequestIds.forEach { requestId -> require(requestId.isNotBlank()) }
        true
    }.getOrDefault(false)

    private companion object {
        const val SCHEMA_VERSION = 2
        val SNAPSHOT_KEY = stringPreferencesKey("geofence_runtime_snapshot_v2")
        val NOTIFICATION_COOLDOWNS_KEY = stringSetPreferencesKey(
            "geofence_runtime_notification_cooldowns_v2"
        )
    }
}
