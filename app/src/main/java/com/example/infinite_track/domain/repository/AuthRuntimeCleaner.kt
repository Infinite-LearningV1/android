package com.example.infinite_track.domain.repository

fun interface AuthRuntimeCleaner {
    suspend fun clearAuthenticatedRuntime()
}
