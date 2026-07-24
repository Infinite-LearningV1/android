package com.example.infinite_track.presentation.screen.attendance.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.infinite_track.domain.model.location.CurrentLocationResult
import com.example.infinite_track.domain.model.location.GeoCoordinate
import com.example.infinite_track.domain.model.location.LocationResult
import com.example.infinite_track.domain.model.location.PlaceDetailsResult
import com.example.infinite_track.domain.model.location.PlaceDiscoveryFailure
import com.example.infinite_track.domain.model.location.PlaceSearchResult
import com.example.infinite_track.domain.model.location.PlaceSuggestion
import com.example.infinite_track.domain.use_case.location.GetCurrentLocationUseCase
import com.example.infinite_track.domain.use_case.location.ResolvePlaceDetailsUseCase
import com.example.infinite_track.domain.use_case.location.SearchPlacesUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import javax.inject.Inject

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class SearchViewModel @Inject constructor(
    private val searchPlaces: SearchPlacesUseCase,
    private val resolvePlaceDetails: ResolvePlaceDetailsUseCase,
    private val getCurrentLocation: GetCurrentLocationUseCase
) : ViewModel() {

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _searchState = MutableStateFlow<SearchUiState>(SearchUiState.Idle)
    val searchState: StateFlow<SearchUiState> = _searchState.asStateFlow()

    private val _selectionEvents = MutableSharedFlow<LocationResult>(extraBufferCapacity = 1)
    val selectionEvents: SharedFlow<LocationResult> = _selectionEvents.asSharedFlow()

    private var proximity: GeoCoordinate? = null
    private var retryJob: Job? = null
    private var resolveJob: Job? = null

    init {
        observeQuery()
        loadOptionalProximity()
    }

    fun updateSearchQuery(query: String) {
        retryJob?.cancel()
        _searchQuery.value = query
    }

    fun onSuggestionSelected(suggestion: PlaceSuggestion) {
        val suggestions = when (val state = _searchState.value) {
            is SearchUiState.Success -> state.suggestions
            is SearchUiState.Resolving -> state.suggestions
            else -> return
        }

        resolveJob?.cancel()
        resolveJob = viewModelScope.launch {
            _searchState.value = SearchUiState.Resolving(suggestions, suggestion.placeId)
            when (val result = resolvePlaceDetails(suggestion)) {
                is PlaceDetailsResult.Success -> {
                    val details = result.details
                    _selectionEvents.emit(
                        LocationResult(
                            placeName = details.displayName,
                            address = details.formattedAddress.orEmpty(),
                            latitude = details.coordinate.latitude,
                            longitude = details.coordinate.longitude
                        )
                    )
                }

                is PlaceDetailsResult.Failure -> {
                    _searchState.value = SearchUiState.Error(result.reason.toUserMessage())
                }
            }
        }
    }

    fun clearSearch() {
        retryJob?.cancel()
        resolveJob?.cancel()
        searchPlaces.abandonSession()
        _searchQuery.value = ""
        _searchState.value = SearchUiState.Idle
    }

    fun retrySearch() {
        val query = _searchQuery.value.trim()
        if (query.length < MIN_QUERY_LENGTH) return

        retryJob?.cancel()
        retryJob = viewModelScope.launch {
            _searchState.value = SearchUiState.Loading
            _searchState.value = searchStateFor(query)
        }
    }

    private fun observeQuery() {
        _searchQuery
            .map(String::trim)
            .distinctUntilChanged()
            .flatMapLatest { query ->
                flow {
                    if (query.length < MIN_QUERY_LENGTH) {
                        emit(SearchUiState.Idle)
                    } else {
                        delay(DEBOUNCE_DELAY)
                        emit(SearchUiState.Loading)
                        emit(searchStateFor(query))
                    }
                }
            }
            .onEach { _searchState.value = it }
            .launchIn(viewModelScope)
    }

    private fun loadOptionalProximity() {
        viewModelScope.launch {
            proximity = when (val result = getCurrentLocation()) {
                is CurrentLocationResult.Success -> result.location.coordinate
                is CurrentLocationResult.Failure -> null
            }
        }
    }

    private suspend fun searchStateFor(query: String): SearchUiState =
        when (val result = searchPlaces(query, proximity)) {
            is PlaceSearchResult.Success -> {
                if (result.suggestions.isEmpty()) {
                    SearchUiState.Empty
                } else {
                    SearchUiState.Success(result.suggestions)
                }
            }

            is PlaceSearchResult.Failure -> {
                SearchUiState.Error(result.reason.toUserMessage())
            }
        }

    private fun PlaceDiscoveryFailure.toUserMessage(): String = when (this) {
        PlaceDiscoveryFailure.CONFIGURATION -> "Pencarian lokasi belum dikonfigurasi."
        PlaceDiscoveryFailure.AUTHENTICATION -> "Akses pencarian lokasi ditolak."
        PlaceDiscoveryFailure.QUOTA -> "Batas pencarian lokasi sedang tercapai. Coba lagi nanti."
        PlaceDiscoveryFailure.NETWORK -> "Jaringan bermasalah. Periksa koneksi lalu coba lagi."
        PlaceDiscoveryFailure.INVALID_REQUEST -> "Kata kunci atau lokasi tidak valid."
        PlaceDiscoveryFailure.UNAVAILABLE -> "Pencarian lokasi sedang tidak tersedia."
    }

    override fun onCleared() {
        retryJob?.cancel()
        resolveJob?.cancel()
        searchPlaces.abandonSession()
        super.onCleared()
    }

    private companion object {
        const val DEBOUNCE_DELAY = 400L
        const val MIN_QUERY_LENGTH = 2
    }
}
