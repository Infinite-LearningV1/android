package com.example.infinite_track.data.soucre.local.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.attendanceDataStore: DataStore<Preferences> by preferencesDataStore(name = "attendance_session")

@Singleton
class AttendancePreference internal constructor(
    private val dataStore: DataStore<Preferences>
) {
    @Inject
    constructor(
        @ApplicationContext context: Context
    ) : this(context.attendanceDataStore)

    companion object {
        private val ACTIVE_ATTENDANCE_ID_KEY = intPreferencesKey("active_attendance_id")
        private val ATTENDANCE_SESSION_STATE_KEY = stringPreferencesKey("attendance_session_state_key")
    }

    suspend fun saveActiveAttendanceId(id: Int) {
        dataStore.edit { preferences ->
            preferences[ACTIVE_ATTENDANCE_ID_KEY] = id
        }
    }

    fun getActiveAttendanceId(): Flow<Int?> = dataStore.data.map { preferences ->
        preferences[ACTIVE_ATTENDANCE_ID_KEY]
    }

    suspend fun clearActiveAttendanceId() {
        dataStore.edit { preferences ->
            preferences.remove(ACTIVE_ATTENDANCE_ID_KEY)
        }
    }

    suspend fun saveAttendanceSessionStateKey(key: String?) {
        dataStore.edit { preferences ->
            if (key.isNullOrBlank()) {
                preferences.remove(ATTENDANCE_SESSION_STATE_KEY)
            } else {
                preferences[ATTENDANCE_SESSION_STATE_KEY] = key
            }
        }
    }

    fun getAttendanceSessionStateKey(): Flow<String?> = dataStore.data.map { preferences ->
        preferences[ATTENDANCE_SESSION_STATE_KEY]
    }

    suspend fun clearAttendanceSessionState() {
        dataStore.edit { preferences ->
            preferences.remove(ACTIVE_ATTENDANCE_ID_KEY)
            preferences.remove(ATTENDANCE_SESSION_STATE_KEY)
        }
    }
}
