package com.example.infinite_track.di.auth

import com.example.infinite_track.domain.repository.AuthRepository
import com.example.infinite_track.domain.repository.RefreshSessionResult
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import javax.inject.Inject
import javax.inject.Provider
import javax.inject.Singleton

@Singleton
class RefreshSingleFlightCoordinator @Inject constructor(
    private val authRepositoryProvider: Provider<AuthRepository>
) {
    private val lock = Mutex()
    private var inFlight: CompletableDeferred<RefreshSessionResult>? = null

    suspend fun refreshOrJoin(): RefreshSessionResult {
        var createdByThisCaller: CompletableDeferred<RefreshSessionResult>? = null

        val deferred = lock.withLock {
            inFlight ?: CompletableDeferred<RefreshSessionResult>().also {
                inFlight = it
                createdByThisCaller = it
            }
        }

        val ownedDeferred = createdByThisCaller
        if (ownedDeferred != null) {
            try {
                ownedDeferred.complete(authRepositoryProvider.get().refreshSession())
            } catch (t: Throwable) {
                ownedDeferred.completeExceptionally(t)
                throw t
            } finally {
                lock.withLock {
                    if (inFlight === ownedDeferred) {
                        inFlight = null
                    }
                }
            }
        }

        return deferred.await()
    }
}
