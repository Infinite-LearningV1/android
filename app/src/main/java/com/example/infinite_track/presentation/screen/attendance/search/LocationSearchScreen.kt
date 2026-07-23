package com.example.infinite_track.presentation.screen.attendance.search

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.example.infinite_track.domain.model.location.PlaceSuggestion
import com.example.infinite_track.presentation.components.button.ButtonBack
import com.example.infinite_track.presentation.components.empty.EmptyListAnimation
import com.example.infinite_track.presentation.components.empty.ErrorAnimation
import com.example.infinite_track.presentation.components.loading.LoadingAnimation
import com.example.infinite_track.presentation.components.maps.LocationItem
import com.example.infinite_track.presentation.components.search.InfiniteTrackSearchBar
import com.example.infinite_track.presentation.core.body1
import com.example.infinite_track.presentation.core.body2
import com.example.infinite_track.presentation.core.headline1
import com.example.infinite_track.presentation.core.headline3
import com.example.infinite_track.presentation.theme.Infinite_TrackTheme
import com.example.infinite_track.presentation.theme.Text
import com.example.infinite_track.presentation.theme.Violet_400

/**
 * Screen untuk pencarian lokasi yang dapat digunakan di bottom sheet
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LocationSearchScreen(
    navController: NavController,
    viewModel: SearchViewModel = hiltViewModel()
) {
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val searchState by viewModel.searchState.collectAsStateWithLifecycle()

    LaunchedEffect(viewModel, navController) {
        viewModel.selectionEvents.collect { location ->
            navController.previousBackStackEntry
                ?.savedStateHandle
                ?.set("selected_location", location)
            navController.popBackStack()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Transparent) // Transparent background
    ) {
        Scaffold(
            containerColor = Color.Transparent, // Transparent scaffold
            topBar = {
                ButtonBack(
                    title = "Cari Lokasi",
                    navController = navController,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                )
            }
        ) { paddingValues ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(horizontal = 16.dp)
                    .padding(top = 12.dp) // 12dp spacing after top bar
                    .statusBarsPadding()
            ) {
                // Search Bar using InfiniteTrackSearchBar
                InfiniteTrackSearchBar(
                    value = searchQuery,
                    placeholder = "Cari lokasi...",
                    onChange = viewModel::updateSearchQuery,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Search Results
                when (val currentSearchState = searchState) {
                    is SearchUiState.Idle -> {
                        SearchPlaceholder()
                    }

                    is SearchUiState.Loading -> {
                        LoadingContent()
                    }

                    is SearchUiState.Success -> {
                        SearchResults(
                            suggestions = currentSearchState.suggestions,
                            onSuggestionClick = viewModel::onSuggestionSelected
                        )
                    }

                    is SearchUiState.Resolving -> LoadingContent()

                    is SearchUiState.Empty -> {
                        EmptySearchResults()
                    }

                    is SearchUiState.Error -> {
                        ErrorContent(
                            message = currentSearchState.message,
                            onRetryClick = viewModel::retrySearch
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SearchResults(
    suggestions: List<PlaceSuggestion>,
    onSuggestionClick: (PlaceSuggestion) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(
            items = suggestions,
            key = PlaceSuggestion::placeId
        ) { suggestion ->
            LocationItem(
                title = suggestion.primaryText,
                subtitle = suggestion.secondaryText,
                onClick = { onSuggestionClick(suggestion) }
            )
        }
    }
}

@Composable
private fun SearchPlaceholder(
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "🔍",
                style = headline1
            )
            Text(
                text = "Mulai mengetik untuk mencari lokasi",
                style = body1,
                color = Violet_400,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun LoadingContent(
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        LoadingAnimation()
    }
}

@Composable
private fun EmptySearchResults(
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            EmptyListAnimation()
            Text(
                text = "Lokasi tidak ditemukan",
                style = headline3,
                color = Text,
                textAlign = TextAlign.Center
            )
            Text(
                text = "Coba gunakan kata kunci yang berbeda",
                style = body2,
                color = Violet_400,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun ErrorContent(
    message: String,
    onRetryClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            ErrorAnimation()
            Text(
                text = "Oops! Terjadi kesalahan",
                style = headline3,
                color = Text,
                textAlign = TextAlign.Center
            )
            Text(
                text = message,
                style = body2,
                color = Violet_400,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun LocationSearchScreenPreview() {
    Infinite_TrackTheme {
        // Preview content would go here
    }
}
