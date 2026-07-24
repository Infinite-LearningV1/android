package com.example.infinite_track.presentation.screen.attendance.search

import com.example.infinite_track.domain.model.location.CurrentLocationResult
import com.example.infinite_track.domain.model.location.GeoCoordinate
import com.example.infinite_track.domain.model.location.LocationResult
import com.example.infinite_track.domain.model.location.PlaceDetails
import com.example.infinite_track.domain.model.location.PlaceDetailsResult
import com.example.infinite_track.domain.model.location.PlaceDiscoveryFailure
import com.example.infinite_track.domain.model.location.PlaceSearchResult
import com.example.infinite_track.domain.model.location.PlaceSuggestion
import com.example.infinite_track.domain.repository.location.CurrentLocationRepository
import com.example.infinite_track.domain.repository.location.PlaceDiscoveryRepository
import com.example.infinite_track.domain.use_case.location.GetCurrentLocationUseCase
import com.example.infinite_track.domain.use_case.location.ResolvePlaceDetailsUseCase
import com.example.infinite_track.domain.use_case.location.SearchPlacesUseCase
import com.example.infinite_track.testing.MainDispatcherRule
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SearchViewModelTest {

    @get:Rule
    val dispatcherRule = MainDispatcherRule()

    @Test
    fun `query shorter than two characters never searches`() =
        runTest(dispatcherRule.dispatcher) {
            val repository = RecordingPlaceRepository()
            val viewModel = viewModel(repository)

            viewModel.updateSearchQuery("k")
            advanceTimeBy(400)
            runCurrent()

            assertEquals(emptyList<String>(), repository.queries)
            assertEquals(SearchUiState.Idle, viewModel.searchState.value)
        }

    @Test
    fun `valid query waits four hundred milliseconds`() =
        runTest(dispatcherRule.dispatcher) {
            val repository = RecordingPlaceRepository()
            val viewModel = viewModel(repository)

            viewModel.updateSearchQuery("kopi")
            advanceTimeBy(399)
            runCurrent()
            assertEquals(emptyList<String>(), repository.queries)

            advanceTimeBy(1)
            runCurrent()
            assertEquals(listOf("kopi"), repository.queries)
        }

    @Test
    fun `newer query supersedes older unfinished result`() =
        runTest(dispatcherRule.dispatcher) {
            val first = CompletableDeferred<PlaceSearchResult>()
            val repository = RecordingPlaceRepository(firstResult = first)
            val viewModel = viewModel(repository)

            viewModel.updateSearchQuery("kopi")
            advanceTimeBy(400)
            runCurrent()
            viewModel.updateSearchQuery("kopi palu")
            advanceTimeBy(400)
            runCurrent()

            assertEquals(listOf("kopi", "kopi palu"), repository.queries)
            val success = viewModel.searchState.value as SearchUiState.Success
            assertEquals("kopi palu", success.suggestions.single().primaryText)
        }

    @Test
    fun `query change cancels an unfinished retry`() =
        runTest(dispatcherRule.dispatcher) {
            val retry = CompletableDeferred<PlaceSearchResult>()
            val repository = RecordingPlaceRepository(
                queuedResults = ArrayDeque(
                    listOf(
                        PlaceSearchResult.Failure(PlaceDiscoveryFailure.NETWORK),
                        retry
                    )
                )
            )
            val viewModel = viewModel(repository)

            viewModel.updateSearchQuery("kopi")
            advanceTimeBy(400)
            runCurrent()
            viewModel.retrySearch()
            runCurrent()

            viewModel.updateSearchQuery("kantor")
            advanceTimeBy(400)
            runCurrent()

            retry.complete(successFor("stale retry"))
            runCurrent()

            val success = viewModel.searchState.value as SearchUiState.Success
            assertEquals("kantor", success.suggestions.single().primaryText)
        }

    @Test
    fun `normalized identical edit keeps unfinished retry owned`() =
        runTest(dispatcherRule.dispatcher) {
            val retry = CompletableDeferred<PlaceSearchResult>()
            val repository = RecordingPlaceRepository(
                queuedResults = ArrayDeque(
                    listOf(
                        PlaceSearchResult.Failure(PlaceDiscoveryFailure.NETWORK),
                        retry
                    )
                )
            )
            val viewModel = viewModel(repository)

            viewModel.updateSearchQuery("kopi")
            advanceTimeBy(400)
            runCurrent()
            viewModel.retrySearch()
            runCurrent()
            assertEquals(SearchUiState.Loading, viewModel.searchState.value)

            viewModel.updateSearchQuery("kopi ")
            retry.complete(successFor("kopi"))
            runCurrent()

            assertEquals("kopi ", viewModel.searchQuery.value)
            val success = viewModel.searchState.value as SearchUiState.Success
            assertEquals("kopi", success.suggestions.single().primaryText)
        }

    @Test
    fun `clear cancels retry abandons session and returns to idle`() =
        runTest(dispatcherRule.dispatcher) {
            val retry = CompletableDeferred<PlaceSearchResult>()
            val repository = RecordingPlaceRepository(
                queuedResults = ArrayDeque(
                    listOf(
                        PlaceSearchResult.Failure(PlaceDiscoveryFailure.NETWORK),
                        retry
                    )
                )
            )
            val viewModel = viewModel(repository)

            viewModel.updateSearchQuery("kopi")
            advanceTimeBy(400)
            runCurrent()
            viewModel.retrySearch()
            runCurrent()

            viewModel.clearSearch()
            retry.complete(successFor("stale retry"))
            runCurrent()

            assertEquals("", viewModel.searchQuery.value)
            assertEquals(SearchUiState.Idle, viewModel.searchState.value)
            assertEquals(1, repository.abandonedSessions)
        }

    @Test
    fun `selection resolves complete suggestion and emits selected location`() =
        runTest(dispatcherRule.dispatcher) {
            val suggestion = suggestionFor("Kopi Palu")
            val repository = RecordingPlaceRepository(
                resolvedDetails = PlaceDetails(
                    placeId = suggestion.placeId,
                    displayName = suggestion.primaryText,
                    formattedAddress = suggestion.secondaryText,
                    coordinate = GeoCoordinate(-0.899, 119.87)
                )
            )
            val viewModel = viewModel(repository)
            val selected = async(UnconfinedTestDispatcher(testScheduler)) {
                viewModel.selectionEvents.first()
            }

            viewModel.updateSearchQuery("kopi")
            advanceTimeBy(400)
            runCurrent()
            viewModel.onSuggestionSelected(suggestion)
            runCurrent()

            assertEquals(suggestion, repository.resolvedSuggestion)
            assertEquals(
                LocationResult(
                    placeName = "Kopi Palu",
                    address = "Palu",
                    latitude = -0.899,
                    longitude = 119.87
                ),
                selected.await()
            )
        }

    @Test
    fun `query edit cancels unfinished selection resolution without stale event`() =
        runTest(dispatcherRule.dispatcher) {
            val deferredResolution = CompletableDeferred<PlaceDetailsResult>()
            val repository = RecordingPlaceRepository(
                deferredResolution = deferredResolution
            )
            val viewModel = viewModel(repository)
            val selections = mutableListOf<LocationResult>()
            backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
                viewModel.selectionEvents.toList(selections)
            }

            viewModel.updateSearchQuery("kopi")
            advanceTimeBy(400)
            runCurrent()
            val suggestion =
                (viewModel.searchState.value as SearchUiState.Success).suggestions.single()
            viewModel.onSuggestionSelected(suggestion)
            runCurrent()

            viewModel.updateSearchQuery("kantor")
            deferredResolution.complete(
                PlaceDetailsResult.Success(
                    PlaceDetails(
                        placeId = suggestion.placeId,
                        displayName = suggestion.primaryText,
                        formattedAddress = suggestion.secondaryText,
                        coordinate = GeoCoordinate(-0.899, 119.87)
                    )
                )
            )
            runCurrent()

            assertEquals(emptyList<LocationResult>(), selections)
        }

    private fun viewModel(repository: RecordingPlaceRepository) = SearchViewModel(
        searchPlaces = SearchPlacesUseCase(repository),
        resolvePlaceDetails = ResolvePlaceDetailsUseCase(repository),
        getCurrentLocation = GetCurrentLocationUseCase(
            object : CurrentLocationRepository {
                override suspend fun getCurrentLocation(): CurrentLocationResult =
                    CurrentLocationResult.Failure.Unavailable
            }
        )
    )

    private class RecordingPlaceRepository(
        private val firstResult: CompletableDeferred<PlaceSearchResult>? = null,
        private val queuedResults: ArrayDeque<Any> = ArrayDeque(),
        private val resolvedDetails: PlaceDetails? = null,
        private val deferredResolution: CompletableDeferred<PlaceDetailsResult>? = null
    ) : PlaceDiscoveryRepository {
        val queries = mutableListOf<String>()
        var resolvedSuggestion: PlaceSuggestion? = null
        var abandonedSessions = 0

        override suspend fun search(
            query: String,
            proximity: GeoCoordinate?
        ): PlaceSearchResult {
            queries += query
            if (queries.size == 1 && firstResult != null) return firstResult.await()
            if (queuedResults.isNotEmpty()) {
                return when (val queued = queuedResults.removeFirst()) {
                    is PlaceSearchResult -> queued
                    is CompletableDeferred<*> -> {
                        @Suppress("UNCHECKED_CAST")
                        (queued as CompletableDeferred<PlaceSearchResult>).await()
                    }
                    else -> error("Unsupported queued result")
                }
            }
            return successFor(query)
        }

        override suspend fun resolve(suggestion: PlaceSuggestion): PlaceDetailsResult {
            resolvedSuggestion = suggestion
            deferredResolution?.let { return it.await() }
            return resolvedDetails?.let(PlaceDetailsResult::Success)
                ?: PlaceDetailsResult.Failure(PlaceDiscoveryFailure.UNAVAILABLE)
        }

        override fun abandonSession() {
            abandonedSessions += 1
        }
    }

    private companion object {
        fun successFor(query: String): PlaceSearchResult =
            PlaceSearchResult.Success(listOf(suggestionFor(query)))

        fun suggestionFor(query: String) =
            PlaceSuggestion(
                placeId = query,
                primaryText = query,
                secondaryText = "Palu",
                distance = null
            )
    }
}
