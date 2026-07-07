package com.example.infinite_track.presentation.screen.wfa

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.infinite_track.domain.use_case.booking.GetBookingHistoryUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class WfaRequestsViewModel @Inject constructor(
    private val getBookingHistoryUseCase: GetBookingHistoryUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(WfaRequestsUiState())
    val uiState: StateFlow<WfaRequestsUiState> = _uiState

    init {
        loadRequests(WfaRequestStatusFilter.All)
    }

    fun onFilterSelected(filter: WfaRequestStatusFilter) {
        if (_uiState.value.selectedFilter == filter && !_uiState.value.isEmpty) return
        loadRequests(filter)
    }

    fun retry() {
        loadRequests(_uiState.value.selectedFilter)
    }

    fun refresh() {
        val currentState = _uiState.value
        if (currentState.isLoading || currentState.isRefreshing) return
        loadRequests(currentState.selectedFilter, isRefresh = true)
    }

    fun loadMore() {
        val currentState = _uiState.value
        if (currentState.isLoading || currentState.isRefreshing || currentState.isLoadingMore || !currentState.pagination.hasNextPage) return

        viewModelScope.launch {
            _uiState.update { it.copy(isLoadingMore = true, errorMessage = null) }
            val nextPage = currentState.pagination.currentPage + 1
            getBookingHistoryUseCase(
                status = currentState.selectedFilter.key,
                page = nextPage,
                limit = currentState.pagination.itemsPerPage,
                sortBy = "created_at",
                sortOrder = "DESC"
            ).onSuccess { page ->
                _uiState.update {
                    it.copy(
                        summary = page.summary,
                        bookings = it.bookings + page.bookings,
                        pagination = page.pagination,
                        isLoadingMore = false,
                        errorMessage = null
                    )
                }
            }.onFailure { error ->
                _uiState.update {
                    it.copy(
                        isLoadingMore = false,
                        errorMessage = error.message ?: "Unable to load more WFA requests."
                    )
                }
            }
        }
    }

    private fun loadRequests(
        filter: WfaRequestStatusFilter,
        isRefresh: Boolean = false
    ) {
        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    selectedFilter = filter,
                    bookings = if (isRefresh) it.bookings else emptyList(),
                    isLoading = !isRefresh,
                    isRefreshing = isRefresh,
                    isLoadingMore = false,
                    errorMessage = null
                )
            }

            getBookingHistoryUseCase(
                status = filter.key,
                page = 1,
                limit = 10,
                sortBy = "created_at",
                sortOrder = "DESC"
            ).onSuccess { page ->
                _uiState.update {
                    it.copy(
                        selectedFilter = filter,
                        summary = page.summary,
                        bookings = page.bookings,
                        pagination = page.pagination,
                        isLoading = false,
                        isRefreshing = false,
                        isLoadingMore = false,
                        errorMessage = null
                    )
                }
            }.onFailure { error ->
                _uiState.update {
                    it.copy(
                        selectedFilter = filter,
                        isLoading = false,
                        isRefreshing = false,
                        isLoadingMore = false,
                        errorMessage = error.message ?: "Unable to load WFA requests."
                    )
                }
            }
        }
    }
}
