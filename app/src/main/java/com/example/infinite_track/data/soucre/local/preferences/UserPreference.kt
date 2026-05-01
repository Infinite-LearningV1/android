package com.example.infinite_track.data.soucre.local.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
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
     * Save session information (token and user ID)
     */
    suspend fun saveSession(token: String, userId: String) {
        dataUserStore.edit { preferences ->
            preferences[AUTH_TOKEN_KEY] = token
            preferences[USER_ID_KEY] = userId
        }
    }

    /**
     * Clear all authentication data
     */
    suspend fun clearAuthData() {
        dataUserStore.edit { preferences ->
            preferences.remove(AUTH_TOKEN_KEY)
            preferences.remove(USER_ID_KEY)
        }
    }

    companion object {
        private val AUTH_TOKEN_KEY = stringPreferencesKey("auth_token")
        private val USER_ID_KEY = stringPreferencesKey("user_id")
    }
}