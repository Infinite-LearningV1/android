package com.example.infinite_track.presentation.screen.attendance.search

import com.example.infinite_track.domain.model.location.PlaceSuggestion

/**
 * UI State untuk pencarian lokasi
 * Mendefisikan berbagai state yang mungkin terjadi selama pencarian
 */
sealed class SearchUiState {
    object Idle : SearchUiState()
    object Loading : SearchUiState()
    data class Success(val suggestions: List<PlaceSuggestion>) : SearchUiState()
    data class Resolving(
        val suggestions: List<PlaceSuggestion>,
        val selectedPlaceId: String
    ) : SearchUiState()
    object Empty : SearchUiState()
    data class Error(val message: String) : SearchUiState()
}
