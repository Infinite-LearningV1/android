package com.example.infinite_track.domain.repository

interface AuthRuntimeCleaner {
    suspend fun clearAuthenticatedRuntime()
}
