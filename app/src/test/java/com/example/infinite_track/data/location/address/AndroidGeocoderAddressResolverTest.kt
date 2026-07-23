package com.example.infinite_track.data.location.address

import com.example.infinite_track.domain.model.location.AddressResolutionResult
import com.example.infinite_track.domain.model.location.GeoCoordinate
import java.io.IOException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Test

class AndroidGeocoderAddressResolverTest {
    private val coordinate = GeoCoordinate(-6.2, 106.816666)
    private val mapper = AndroidAddressMapper()

    @Test
    fun `complete address prefers formatted Android address line`() = runTest {
        val resolver = resolverReturning(
            snapshot(
                featureName = "Monumen Nasional",
                formattedLines = listOf("Gambir, Jakarta Pusat, DKI Jakarta")
            )
        )

        val result = resolver.resolve(coordinate) as AddressResolutionResult.Resolved

        assertEquals("Monumen Nasional", result.address.name)
        assertEquals("Gambir, Jakarta Pusat, DKI Jakarta", result.address.formattedAddress)
        assertEquals(coordinate, result.address.coordinate)
    }

    @Test
    fun `partial address is assembled without blank parts`() = runTest {
        val resolver = resolverReturning(
            snapshot(
                thoroughfare = "Jalan Medan Merdeka",
                locality = "Jakarta Pusat",
                adminArea = "DKI Jakarta"
            )
        )

        val result = resolver.resolve(coordinate) as AddressResolutionResult.Resolved

        assertEquals("Jalan Medan Merdeka", result.address.name)
        assertEquals(
            "Jalan Medan Merdeka, Jakarta Pusat, DKI Jakarta",
            result.address.formattedAddress
        )
    }

    @Test
    fun `empty result returns coordinate only`() = runTest {
        val resolver = AndroidGeocoderAddressResolver(
            dataSource = FakeDataSource(result = emptyList()),
            mapper = mapper
        )

        assertEquals(
            AddressResolutionResult.CoordinateOnly(coordinate),
            resolver.resolve(coordinate)
        )
    }

    @Test
    fun `unavailable service maps to typed failure`() = runTest {
        val resolver = resolverThrowing(GeocoderUnavailableException())

        assertSame(
            AddressResolutionResult.Failed.ServiceUnavailable,
            resolver.resolve(coordinate)
        )
    }

    @Test
    fun `io failure maps to unavailable`() = runTest {
        val resolver = resolverThrowing(IOException("offline"))

        assertSame(
            AddressResolutionResult.Failed.ServiceUnavailable,
            resolver.resolve(coordinate)
        )
    }

    @Test
    fun `invalid provider coordinate maps to typed failure`() = runTest {
        val resolver = resolverThrowing(IllegalArgumentException("invalid"))

        assertSame(
            AddressResolutionResult.Failed.InvalidCoordinate,
            resolver.resolve(coordinate)
        )
    }

    @Test(expected = CancellationException::class)
    fun `cancellation propagates`() = runTest {
        resolverThrowing(CancellationException("cancelled")).resolve(coordinate)
        Unit
    }

    private fun resolverReturning(snapshot: PlatformAddressSnapshot) =
        AndroidGeocoderAddressResolver(FakeDataSource(listOf(snapshot)), mapper)

    private fun resolverThrowing(error: Throwable) =
        AndroidGeocoderAddressResolver(FakeDataSource(error = error), mapper)

    private fun snapshot(
        featureName: String? = null,
        thoroughfare: String? = null,
        locality: String? = null,
        adminArea: String? = null,
        formattedLines: List<String> = emptyList()
    ) = PlatformAddressSnapshot(
        featureName = featureName,
        thoroughfare = thoroughfare,
        subThoroughfare = null,
        locality = locality,
        subAdminArea = null,
        adminArea = adminArea,
        postalCode = null,
        countryName = null,
        formattedLines = formattedLines
    )

    private class FakeDataSource(
        private val result: List<PlatformAddressSnapshot> = emptyList(),
        private val error: Throwable? = null
    ) : AddressGeocoderDataSource {
        override suspend fun getAddresses(
            coordinate: GeoCoordinate
        ): List<PlatformAddressSnapshot> {
            error?.let { throw it }
            return result
        }
    }
}
