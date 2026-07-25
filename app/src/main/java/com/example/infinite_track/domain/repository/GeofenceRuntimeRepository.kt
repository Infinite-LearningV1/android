package com.example.infinite_track.domain.repository

import com.example.infinite_track.domain.model.geofence.GeofenceRuntimeMode
import com.example.infinite_track.domain.model.geofence.GeofenceRuntimeReadiness
import com.example.infinite_track.domain.model.geofence.GeofenceRuntimeResult
import kotlinx.coroutines.flow.Flow

interface GeofenceRuntimeRepository {
    suspend fun reconcile(mode: GeofenceRuntimeMode): GeofenceRuntimeResult
    suspend fun clearForLogout(): GeofenceRuntimeResult
    fun observeReadiness(): Flow<GeofenceRuntimeReadiness>
}
