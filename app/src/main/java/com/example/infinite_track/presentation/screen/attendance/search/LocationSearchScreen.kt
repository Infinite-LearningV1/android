package com.example.infinite_track.presentation.screen.attendance.search

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.example.infinite_track.R
import com.example.infinite_track.domain.model.location.PlaceSuggestion
import com.example.infinite_track.presentation.components.search.InfiniteTrackSearchBar
import com.example.infinite_track.presentation.design.components.navigation.InfiniteTopBar
import com.example.infinite_track.presentation.design.components.state.InfiniteEmptyState
import com.example.infinite_track.presentation.design.components.state.InfiniteErrorState
import com.example.infinite_track.presentation.design.components.state.InfiniteLoadingState
import com.example.infinite_track.presentation.design.tokens.InfiniteColors
import com.example.infinite_track.presentation.design.tokens.InfiniteIcons
import com.example.infinite_track.presentation.design.tokens.InfiniteSpacing
import com.example.infinite_track.presentation.navigation.LocationSearchResultContract

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
                ?.set(LocationSearchResultContract.RESULT_KEY, location)
            navController.popBackStack()
        }
    }

    LocationSearchContent(
        query = searchQuery,
        state = searchState,
        resolvingPlaceId = (searchState as? SearchUiState.Resolving)?.selectedPlaceId,
        onQueryChange = viewModel::updateSearchQuery,
        onClear = viewModel::clearSearch,
        onRetry = viewModel::retrySearch,
        onSuggestionSelected = viewModel::onSuggestionSelected,
        onBack = navController::popBackStack
    )
}

@Composable
internal fun LocationSearchContent(
    query: String,
    state: SearchUiState,
    resolvingPlaceId: String?,
    onQueryChange: (String) -> Unit,
    onClear: () -> Unit,
    onRetry: () -> Unit,
    onSuggestionSelected: (PlaceSuggestion) -> Unit,
    modifier: Modifier = Modifier,
    onBack: () -> Unit = {}
) {
    val suggestions = when (state) {
        is SearchUiState.Success -> state.suggestions
        is SearchUiState.Resolving -> state.suggestions
        else -> emptyList()
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = InfiniteColors.Transparent,
        topBar = {
            InfiniteTopBar(
                title = stringResource(R.string.wfa_search_title),
                onNavigationClick = onBack
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(
                    horizontal = InfiniteSpacing.Default.lg,
                    vertical = InfiniteSpacing.Default.md
                )
        ) {
            InfiniteTrackSearchBar(
                value = query,
                placeholder = stringResource(R.string.wfa_search_placeholder),
                onChange = onQueryChange,
                onClear = onClear,
                clearContentDescription = stringResource(R.string.wfa_search_clear_content_description),
                fieldContentDescription = stringResource(
                    R.string.wfa_search_field_content_description
                ),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(InfiniteSpacing.Default.md))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                when (state) {
                    SearchUiState.Idle -> SearchGuidance()
                    SearchUiState.Loading -> InfiniteLoadingState(
                        message = stringResource(R.string.wfa_search_loading)
                    )
                    is SearchUiState.Success,
                    is SearchUiState.Resolving -> SearchResults(
                        suggestions = suggestions,
                        resolvingPlaceId = resolvingPlaceId,
                        onSuggestionSelected = onSuggestionSelected
                    )
                    SearchUiState.Empty -> InfiniteEmptyState(
                        title = stringResource(R.string.wfa_search_empty_title),
                        message = stringResource(R.string.wfa_search_empty_body, query)
                    )
                    is SearchUiState.Error -> InfiniteErrorState(
                        title = stringResource(R.string.wfa_search_error_title),
                        message = stringResource(state.messageRes),
                        actionLabel = stringResource(R.string.wfa_search_retry),
                        onAction = onRetry
                    )
                }
            }
        }
    }
}

@Composable
private fun SearchResults(
    suggestions: List<PlaceSuggestion>,
    resolvingPlaceId: String?,
    onSuggestionSelected: (PlaceSuggestion) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .testTag("wfaSearchResults"),
        contentPadding = PaddingValues(bottom = InfiniteSpacing.Default.xl),
        verticalArrangement = Arrangement.spacedBy(InfiniteSpacing.Default.sm)
    ) {
        items(
            items = suggestions,
            key = PlaceSuggestion::placeId,
            contentType = { "wfaPlace" }
        ) { suggestion ->
            WfaPlaceResultCard(
                suggestion = suggestion,
                selected = suggestion.placeId == resolvingPlaceId,
                onClick = { onSuggestionSelected(suggestion) }
            )
        }
    }
}

@Composable
private fun SearchGuidance(
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.padding(InfiniteSpacing.Default.xl),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(InfiniteSpacing.Default.md)
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(MaterialTheme.shapes.large)
                .background(InfiniteColors.Secondary.copy(alpha = 0.14f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = InfiniteIcons.Search,
                contentDescription = null,
                tint = InfiniteColors.Secondary,
                modifier = Modifier.size(24.dp)
            )
        }
        Text(
            text = stringResource(R.string.wfa_search_guidance_title),
            modifier = Modifier.semantics { heading() },
            style = MaterialTheme.typography.titleMedium,
            color = InfiniteColors.Text,
            textAlign = TextAlign.Center
        )
        Text(
            text = stringResource(R.string.wfa_search_guidance_body),
            style = MaterialTheme.typography.bodyMedium,
            color = InfiniteColors.AttendanceReportBodyText,
            textAlign = TextAlign.Center
        )
    }
}
