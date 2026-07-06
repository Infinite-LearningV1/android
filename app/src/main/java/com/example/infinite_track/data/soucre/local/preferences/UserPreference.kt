package com.example.infinite_track.data.soucre.local.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

val Context.dataUserStore: DataStore<Preferences> by preferencesDataStore(name = "user")

@Singleton
class UserPreference @Inject constructor(private val dataUserStore: DataStore<Preferences>) {

    /**
     * Get the authentication token as a Flow
     */
    fun getAuthToken(): Flow<String> {
        return dataUserStore.data.map { preferences ->
            preferences[AUTH_TOKEN_KEY] ?: ""
        }
    }

    /**
     * Get the user ID as a Flow
     */
    fun getUserId(): Flow<String> {
        return dataUserStore.data.map { preferences ->
            preferences[USER_ID_KEY] ?: ""
        }
    }

    /**
     * Save session information (access token, user ID, and optional refresh token)
     */
    suspend fun saveSession(
        token: String,
        userId: String,
        refreshToken: String? = null,
        lastRefreshAt: Long = 0L
    ) {
        dataUserStore.edit { preferences ->
            preferences[AUTH_TOKEN_KEY] = token
            preferences[USER_ID_KEY] = userId
            preferences[LAST_REFRESH_AT_KEY] = lastRefreshAt
            if (refreshToken.isNullOrBlank()) {
                preferences.remove(REFRESH_TOKEN_KEY)
            } else {
                preferences[REFRESH_TOKEN_KEY] = refreshToken
            }
        }
    }

    /**
     * Get refresh token as a Flow
     */
    fun getRefreshToken(): Flow<String> {
        return dataUserStore.data.map { preferences ->
            preferences[REFRESH_TOKEN_KEY] ?: ""
        }
    }

    /**
     * Get last successful refresh timestamp as epoch millis.
     */
    fun getLastRefreshAt(): Flow<Long> {
        return dataUserStore.data.map { preferences ->
            preferences[LAST_REFRESH_AT_KEY] ?: 0L
        }
    }

    fun getLastProfileSyncAt(): Flow<Long> {
        return dataUserStore.data.map { preferences ->
            preferences[LAST_PROFILE_SYNC_AT_KEY] ?: 0L
        }
    }

    suspend fun saveLastProfileSyncAt(timestampMillis: Long) {
        dataUserStore.edit { preferences ->
            preferences[LAST_PROFILE_SYNC_AT_KEY] = timestampMillis
        }
    }

    /**
     * Clear all authentication data
     */
    suspend fun clearSession() {
        dataUserStore.edit { preferences ->
            preferences.remove(AUTH_TOKEN_KEY)
            preferences.remove(USER_ID_KEY)
            preferences.remove(REFRESH_TOKEN_KEY)
            preferences.remove(LAST_REFRESH_AT_KEY)
            preferences.remove(LAST_PROFILE_SYNC_AT_KEY)
        }
    }

    suspend fun clearAuthData() {
        clearSession()
    }

    companion object {
        private val AUTH_TOKEN_KEY = stringPreferencesKey("auth_token")
        private val USER_ID_KEY = stringPreferencesKey("user_id")
        private val REFRESH_TOKEN_KEY = stringPreferencesKey("refresh_token")
        private val LAST_REFRESH_AT_KEY = longPreferencesKey("last_refresh_at")
        private val LAST_PROFILE_SYNC_AT_KEY = longPreferencesKey("last_profile_sync_at")
    }
}