package com.example.infinite_track.data.platform.geofence.store

interface GeofenceRuntimeStore {
    suspend fun readSnapshot(): GeofenceRuntimeSnapshot?
    suspend fun writeSnapshot(snapshot: GeofenceRuntimeSnapshot)
    suspend fun claimNotification(key: String, nowMillis: Long, cooldownMillis: Long): Boolean
    suspend fun setInsideActiveGeofence(isInside: Boolean)
    suspend fun clear()
}
