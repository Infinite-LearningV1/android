package com.example.infinite_track.data.soucre.local.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.infinite_track.domain.model.attendance.TodayStatus
import com.google.gson.Gson
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.todayStatusDataStore: DataStore<Preferences> by preferencesDataStore(name = "today_status")

data class CachedTodayStatusPayload(
    val userId: String,
    val todayDate: String,
    val attendanceSessionStateId: Int?,
    val attendanceSessionStateKey: String?,
    val activeAttendanceId: Int?,
    val fetchedAtMillis: Long,
    val ttlSeconds: Int,
    val status: TodayStatus
)

@Singleton
class TodayStatusPreference internal constructor(
    private val dataStore: DataStore<Preferences>,
    private val gson: Gson
) {
    @Inject
    constructor(
        @ApplicationContext context: Context,
        gson: Gson
    ) : this(
        dataStore = context.todayStatusDataStore,
        gson = gson
    )

    suspend fun saveTodayStatusCache(payload: CachedTodayStatusPayload) {
        dataStore.edit { preferences ->
            preferences[CACHE_JSON_KEY] = gson.toJson(payload)
        }
    }

    fun getTodayStatusCache(): Flow<CachedTodayStatusPayload?> {
        return dataStore.data.map { preferences ->
            preferences[CACHE_JSON_KEY]
                ?.takeIf { it.isNotBlank() }
                ?.let { json ->
                    runCatching { gson.fromJson(json, CachedTodayStatusPayload::class.java) }.getOrNull()
                }
        }
    }

    suspend fun clearTodayStatusCache() {
        dataStore.edit { preferences ->
            preferences.remove(CACHE_JSON_KEY)
        }
    }

    companion object {
        private val CACHE_JSON_KEY = stringPreferencesKey("today_status_cache_json")
    }
}
