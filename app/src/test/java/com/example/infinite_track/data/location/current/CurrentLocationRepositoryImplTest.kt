package com.example.infinite_track.data.location.current

import com.example.infinite_track.domain.model.location.CurrentLocationResult
import com.example.infinite_track.domain.model.location.DistanceMeters
import com.example.infinite_track.domain.model.location.GeoCoordinate
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Test

class CurrentLocationRepositoryImplTest {

    private val mapper = AndroidLocationMapper()

    @Test
    fun `maps platform snapshot without requiring provider name`() = runBlocking {
        val repository = repositoryReturning(
            PlatformLocationSnapshot(
                latitude = -0.89,
                longitude = 119.87,
                accuracyMeters = 7.5,
                capturedAtEpochMillis = 1234L,
                provider = null,
                isMock = false
            )
        )

        val result = repository.getCurrentLocation() as CurrentLocationResult.Success

        assertEquals(GeoCoordinate(-0.89, 119.87), result.location.coordinate)
        assertEquals(DistanceMeters(7.5), result.location.accuracy)
        assertEquals(1234L, result.location.capturedAtEpochMillis)
        assertEquals(null, result.location.provider)
    }

    @Test
    fun `returns unavailable when provider has no location`() = runBlocking {
        val result = repositoryReturning(null).getCurrentLocation()

        assertSame(CurrentLocationResult.Failure.Unavailable, result)
    }

    @Test
    fun `returns invalid coordinate for non geographic provider result`() = runBlocking {
        val result = repositoryReturning(
            PlatformLocationSnapshot(
                latitude = 91.0,
                longitude = 119.87,
                accuracyMeters = null,
                capturedAtEpochMillis = 0L,
                provider = "fused",
                isMock = false
            )
        ).getCurrentLocation()

        assertSame(CurrentLocationResult.Failure.InvalidCoordinate, result)
    }

    @Test
    fun `maps security exception to permission denied`() = runBlocking {
        val repository = repositoryThrowing(SecurityException("missing permission"))

        assertSame(
            CurrentLocationResult.Failure.PermissionDenied,
            repository.getCurrentLocation()
        )
    }

    @Test
    fun `maps provider exception without exposing it`() = runBlocking {
        val repository = repositoryThrowing(IllegalStateException("provider details"))

        assertSame(CurrentLocationResult.Failure.ProviderError, repository.getCurrentLocation())
    }

    @Test(expected = CancellationException::class)
    fun `propagates cancellation`() {
        runBlocking {
            repositoryThrowing(CancellationException("cancelled")).getCurrentLocation()
        }
    }

    private fun repositoryReturning(snapshot: PlatformLocationSnapshot?): CurrentLocationRepositoryImpl {
        return CurrentLocationRepositoryImpl(
            dataSource = object : CurrentLocationDataSource {
                override suspend fun getCurrentLocation(): PlatformLocationSnapshot? = snapshot
            },
            mapper = mapper
        )
    }

    private fun repositoryThrowing(error: Throwable): CurrentLocationRepositoryImpl {
        return CurrentLocationRepositoryImpl(
            dataSource = object : CurrentLocationDataSource {
                override suspend fun getCurrentLocation(): PlatformLocationSnapshot? = throw error
            },
            mapper = mapper
        )
    }
}
