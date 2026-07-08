package com.example.infinite_track.presentation.screen.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.infinite_track.domain.model.attendance.AttendancePeriod
import com.example.infinite_track.domain.model.attendance.AttendanceRecord
import com.example.infinite_track.domain.model.attendance.AttendanceSummaryInfo
import com.example.infinite_track.domain.use_case.history.GetAttendanceHistoryUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Data class to hold the entire UI state for the History screen
 */
data class HistoryScreenState(
    val isLoading: Boolean = true,
    val isRefreshing: Boolean = false,
    val isLoadingMore: Boolean = false,
    val canLoadMore: Boolean = true,
    val error: String? = null,
    val selectedPeriod: String = AttendancePeriod.MONTHLY,
    val summary: AttendanceSummaryInfo? = null,
    val records: List<AttendanceRecord> = emptyList(),
    val currentPage: Int = 1,
    val pageSize: Int = 10
)

@HiltViewModel
class HistoryViewModel @Inject constructor(
    private val getAttendanceHistoryUseCase: GetAttendanceHistoryUseCase
) : ViewModel() {

    // Single state flow for the entire UI state
    private val _uiState = MutableStateFlow(HistoryScreenState())
    val uiState: StateFlow<HistoryScreenState> = _uiState.asStateFlow()

    // To track and cancel previous loading jobs
    private var loadingJob: Job? = null

    init {
        // Load initial data
        loadHistory(isRefresh = true)
    }

    /**
     * Set the period filter and reload data
     * @param newPeriod The new period to filter by (e.g., "daily", "weekly", "monthly")
     */
    fun onFilterChanged(newPeriod: String) {
        if (newPeriod != uiState.value.selectedPeriod) {
            loadingJob?.cancel()

            _uiState.update { it.copy(
                selectedPeriod = newPeriod,
                records = emptyList(),
                summary = null,
                currentPage = 1,
                canLoadMore = newPeriod != AttendancePeriod.CUSTOM,
                isLoading = false,
                isRefreshing = false,
                isLoadingMore = false,
                error = null
            )}

            // Custom range is visible for the report contract, but it needs a date range picker
            // before it can request backend data honestly.
            if (newPeriod != AttendancePeriod.CUSTOM) {
                loadHistory(isRefresh = true)
            }
        }
    }

    /**
     * Load the next page of attendance history
     */
    fun loadNextPage() {
        val currentState = uiState.value

        if (!currentState.isLoadingMore && currentState.canLoadMore) {
            _uiState.update { it.copy(
                currentPage = it.currentPage + 1
            )}
            loadHistory(isRefresh = false)
        }
    }

    /**
     * Refresh the attendance history (reload from first page)
     */
    fun refreshHistory() {
        val currentState = uiState.value
        if (currentState.selectedPeriod == AttendancePeriod.CUSTOM || currentState.isLoading || currentState.isRefreshing) {
            if (currentState.selectedPeriod == AttendancePeriod.CUSTOM) {
                _uiState.update { it.copy(
                    currentPage = 1,
                    records = emptyList(),
                    summary = null,
                    canLoadMore = false,
                    isLoading = false,
                    isRefreshing = false,
                    isLoadingMore = false,
                    error = null
                )}
            }
            return
        }

        _uiState.update { it.copy(currentPage = 1) }
        loadHistory(isRefresh = true, isPullRefresh = true)
    }

    /**
     * Load attendance history with the current settings
     * @param isRefresh Whether to refresh the data (true) or append to existing data (false)
     */
    private fun loadHistory(
        isRefresh: Boolean,
        isPullRefresh: Boolean = false
    ) {
        // Cancel any ongoing loading job
        loadingJob?.cancel()

        val requestedPeriod = uiState.value.selectedPeriod
        val requestedPage = uiState.value.currentPage
        val requestedPageSize = uiState.value.pageSize

        // Update state to show loading
        _uiState.update { it.copy(
            isLoading = isRefresh && !isPullRefresh,
            isRefreshing = isPullRefresh,
            isLoadingMore = !isRefresh,
            error = null
        )}

        // Start a new loading job
        loadingJob = viewModelScope.launch {
            getAttendanceHistoryUseCase(
                period = requestedPeriod,
                page = requestedPage,
                limit = requestedPageSize
            ).onSuccess { historyPage ->
                if (uiState.value.selectedPeriod != requestedPeriod) return@onSuccess

                // Update state with the loaded data
                _uiState.update { currentState ->
                    currentState.copy(
                        isLoading = false,
                        isRefreshing = false,
                        isLoadingMore = false,
                        summary = historyPage.summary,
                        records = if (isRefresh) historyPage.records else currentState.records + historyPage.records,
                        canLoadMore = historyPage.pagination.hasNextPage
                    )
                }
            }.onFailure { error ->
                if (uiState.value.selectedPeriod != requestedPeriod) return@onFailure

                // Update state to show error
                _uiState.update { it.copy(
                    isLoading = false,
                    isRefreshing = false,
                    isLoadingMore = false,
                    canLoadMore = false,
                    error = error.message ?: "Unknown error occurred"
                )}
            }
        }
    }

    /**
     * Set the page size and reload data
     * @param size New page size
     */
    fun setPageSize(size: Int) {
        if (size != uiState.value.pageSize) {
            loadingJob?.cancel()

            _uiState.update { it.copy(
                pageSize = size,
                currentPage = 1,
                records = emptyList(),
                summary = null,
                canLoadMore = it.selectedPeriod != AttendancePeriod.CUSTOM,
                isLoading = false,
                isRefreshing = false,
                isLoadingMore = false,
                error = null
            )}

            if (uiState.value.selectedPeriod != AttendancePeriod.CUSTOM) {
                loadHistory(isRefresh = true)
            }
        }
    }
}