package com.example.infinite_track.presentation.screen.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
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
    val isLoadingMore: Boolean = false,
    val canLoadMore: Boolean = true,
    val error: String? = null,
    val selectedPeriod: String = "all",
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
            _uiState.update { it.copy(
                selectedPeriod = newPeriod,
                records = emptyList(),
                summary = null,
                currentPage = 1
            )}

            // Reload data with new filter
            loadHistory(isRefresh = true)
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
        _uiState.update { it.copy(
            currentPage = 1,
            records = emptyList()
        )}
        loadHistory(isRefresh = true)
    }

    /**
     * Load attendance history with the current settings
     * @param isRefresh Whether to refresh the data (true) or append to existing data (false)
     */
    private fun loadHistory(isRefresh: Boolean) {
        // Cancel any ongoing loading job
        loadingJob?.cancel()

        // Update state to show loading
        _uiState.update { it.copy(
            isLoading = isRefresh,
            isLoadingMore = !isRefresh,
            error = null
        )}

        // Start a new loading job
        loadingJob = viewModelScope.launch {
            getAttendanceHistoryUseCase(
                period = uiState.value.selectedPeriod,
                page = uiState.value.currentPage,
                limit = uiState.value.pageSize
            ).onSuccess { historyPage ->
                // Update state with the loaded data
                _uiState.update { currentState ->
                    currentState.copy(
                        isLoading = false,
                        isLoadingMore = false,
                        summary = historyPage.summary,
                        records = if (isRefresh) historyPage.records else currentState.records + historyPage.records,
                        canLoadMore = historyPage.pagination.hasNextPage
                    )
                }
            }.onFailure { error ->
                // Update state to show error
                _uiState.update { it.copy(
                    isLoading = false,
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
            _uiState.update { it.copy(
                pageSize = size,
                currentPage = 1,
                records = emptyList()
            )}
            loadHistory(isRefresh = true)
        }
    }
}