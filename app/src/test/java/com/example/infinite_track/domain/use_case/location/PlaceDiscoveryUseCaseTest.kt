package com.example.infinite_track.domain.use_case.location

import com.example.infinite_track.domain.model.location.GeoCoordinate
import com.example.infinite_track.domain.model.location.PlaceDetails
import com.example.infinite_track.domain.model.location.PlaceDetailsResult
import com.example.infinite_track.domain.model.location.PlaceDiscoveryFailure
import com.example.infinite_track.domain.model.location.PlaceSearchResult
import com.example.infinite_track.domain.model.location.PlaceSuggestion
import com.example.infinite_track.domain.repository.location.PlaceDiscoveryRepository
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PlaceDiscoveryUseCaseTest {

    @Test
    fun `search keeps duplicate display names with distinct opaque place ids`() = runBlocking {
        val repository = FakePlaceDiscoveryRepository(
            searchResult = PlaceSearchResult.Success(
                listOf(
                    suggestion("place-a", "Kopi Kita"),
                    suggestion("place-b", "Kopi Kita")
                )
            )
        )
        val proximity = GeoCoordinate(-0.89, 119.87)

        val result = SearchPlacesUseCase(repository)("  kopi  ", proximity)
            as PlaceSearchResult.Success

        assertEquals(listOf("place-a", "place-b"), result.suggestions.map { it.placeId })
        assertEquals("kopi", repository.lastQuery)
        assertEquals(proximity, repository.lastProximity)
    }

    @Test
    fun `blank search avoids provider request`() = runBlocking {
        val repository = FakePlaceDiscoveryRepository()

        val result = SearchPlacesUseCase(repository)("   ") as PlaceSearchResult.Success

        assertTrue(result.suggestions.isEmpty())
        assertEquals(null, repository.lastQuery)
    }

    @Test
    fun `resolve rejects blank place id`() = runBlocking {
        val repository = FakePlaceDiscoveryRepository()

        val result = ResolvePlaceDetailsUseCase(repository)(" ") as PlaceDetailsResult.Failure

        assertEquals(PlaceDiscoveryFailure.INVALID_REQUEST, result.reason)
        assertEquals(null, repository.lastResolvedId)
    }

    @Test
    fun `resolve forwards opaque place id and coordinate`() = runBlocking {
        val details = PlaceDetails(
            placeId = "opaque-id",
            displayName = "Kopi Kita",
            formattedAddress = "Palu",
            coordinate = GeoCoordinate(-0.90, 119.88)
        )
        val repository = FakePlaceDiscoveryRepository(
            detailsResult = PlaceDetailsResult.Success(details)
        )

        val result = ResolvePlaceDetailsUseCase(repository)("opaque-id")
            as PlaceDetailsResult.Success

        assertEquals(details, result.details)
        assertEquals("opaque-id", repository.lastResolvedId)
    }

    private fun suggestion(id: String, name: String) = PlaceSuggestion(
        placeId = id,
        primaryText = name,
        secondaryText = "Palu",
        distance = null
    )

    private class FakePlaceDiscoveryRepository(
        private val searchResult: PlaceSearchResult = PlaceSearchResult.Success(emptyList()),
        private val detailsResult: PlaceDetailsResult = PlaceDetailsResult.Failure(
            PlaceDiscoveryFailure.UNAVAILABLE
        )
    ) : PlaceDiscoveryRepository {
        var lastQuery: String? = null
        var lastProximity: GeoCoordinate? = null
        var lastResolvedId: String? = null

        override suspend fun search(
            query: String,
            proximity: GeoCoordinate?
        ): PlaceSearchResult {
            lastQuery = query
            lastProximity = proximity
            return searchResult
        }

        override suspend fun resolve(placeId: String): PlaceDetailsResult {
            lastResolvedId = placeId
            return detailsResult
        }

        override fun abandonSession() = Unit
    }
}
