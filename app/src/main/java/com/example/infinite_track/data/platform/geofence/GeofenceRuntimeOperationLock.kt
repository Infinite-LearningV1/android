package com.example.infinite_track.data.platform.geofence

import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/** Serializes runtime reconciliation, logout, and accepted receiver side effects. */
@Singleton
class GeofenceRuntimeOperationLock @Inject constructor() {
    private val mutex = Mutex()

    suspend fun <T> withOperation(block: suspend () -> T): T = mutex.withLock { block() }
}
