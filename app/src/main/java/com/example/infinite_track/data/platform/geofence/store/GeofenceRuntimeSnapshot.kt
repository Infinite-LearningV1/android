package com.example.infinite_track.data.platform.geofence.store

enum class PersistedGeofenceMode { DISABLED, REMINDER, ACTIVE, COMPLETED }

enum class PersistedReconciliationState { APPLYING, APPLIED, DEGRADED }

enum class PersistedRegistrationKind { REMINDER, ACTIVE }

data class PersistedGeofenceRegistration(
    val requestId: String,
    val logicalId: String,
    val kind: PersistedRegistrationKind,
    val label: String,
    val latitude: Double,
    val longitude: Double,
    val radiusMeters: Double,
    val attendanceId: Int?
)

data class GeofenceRuntimeSnapshot(
    val schemaVersion: Int = 2,
    val generation: Long,
    val effectiveDateIso: String?,
    val expectedMode: PersistedGeofenceMode,
    val attendanceId: Int?,
    val sessionStateKey: String?,
    val expectedRegistrations: List<PersistedGeofenceRegistration>,
    val appliedRequestIds: Set<String>,
    val reconciliationState: PersistedReconciliationState,
    val insideActiveGeofence: Boolean,
    val failureCategory: String?,
    val updatedAtEpochMillis: Long
)
