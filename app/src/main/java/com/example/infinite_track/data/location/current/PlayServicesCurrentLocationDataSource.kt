package com.example.infinite_track.data.location.current

import android.annotation.SuppressLint
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import kotlinx.coroutines.suspendCancellableCoroutine
import javax.inject.Inject
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

class PlayServicesCurrentLocationDataSource @Inject constructor(
    private val client: FusedLocationProviderClient,
    private val mapper: AndroidLocationMapper
) : CurrentLocationDataSource {

    @SuppressLint("MissingPermission")
    override suspend fun getCurrentLocation(): PlatformLocationSnapshot? =
        suspendCancellableCoroutine { continuation ->
            val cancellationTokenSource = CancellationTokenSource()

            try {
                client.getCurrentLocation(
                    Priority.PRIORITY_HIGH_ACCURACY,
                    cancellationTokenSource.token
                )
                    .addOnSuccessListener { location ->
                        if (continuation.isActive) {
                            continuation.resume(location?.let(mapper::toSnapshot))
                        }
                    }
                    .addOnFailureListener { error ->
                        if (continuation.isActive) {
                            continuation.resumeWithException(error)
                        }
                    }
            } catch (error: Throwable) {
                if (continuation.isActive) {
                    continuation.resumeWithException(error)
                }
            }

            continuation.invokeOnCancellation { cancellationTokenSource.cancel() }
        }
}
