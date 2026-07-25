package com.example.infinite_track.data.platform.geofence

import com.example.infinite_track.data.platform.geofence.store.PersistedRegistrationKind
import com.example.infinite_track.domain.model.geofence.GeofenceRuntimeReadiness
import com.example.infinite_track.domain.model.location.DistanceMeters
import com.example.infinite_track.domain.model.location.GeoCoordinate
import kotlinx.coroutines.flow.Flow

data class PlatformGeofenceRegistration(
    val requestId: String,
    val logicalId: String,
    val kind: PersistedRegistrationKind,
    val label: String,
    val coordinate: GeoCoordinate,
    val radius: DistanceMeters,
    val attendanceId: Int?
)

interface GeofencingPlatformClient {
    fun observeReadiness(): Flow<GeofenceRuntimeReadiness>
    suspend fun removeOwnedGeofences()
    suspend fun addAll(registrations: List<PlatformGeofenceRegistration>)
}
