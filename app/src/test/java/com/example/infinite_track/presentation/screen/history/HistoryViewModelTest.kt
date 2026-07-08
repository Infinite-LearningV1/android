package com.example.infinite_track.presentation.screen.history

import com.example.infinite_track.data.soucre.network.response.Pagination
import com.example.infinite_track.domain.model.attendance.AttendanceHistoryPage
import com.example.infinite_track.domain.model.attendance.AttendancePeriod
import com.example.infinite_track.domain.model.attendance.AttendanceRecord
import com.example.infinite_track.domain.model.attendance.AttendanceSummaryInfo
import com.example.infinite_track.domain.repository.AttendanceHistoryRepository
import com.example.infinite_track.domain.use_case.history.GetAttendanceHistoryUseCase
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class HistoryViewModelTest {

    @Test
    fun onFilterChanged_customDoesNotRequestBackendAndClearsReportState() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        Dispatchers.setMain(dispatcher)
        try {
            val repository = FakeAttendanceHistoryRepository()
            val viewModel = HistoryViewModel(GetAttendanceHistoryUseCase(repository))
            advanceUntilIdle()

            viewModel.onFilterChanged(AttendancePeriod.CUSTOM)
            advanceUntilIdle()

            assertEquals(listOf(AttendancePeriod.MONTHLY), repository.requestedPeriods)
            assertEquals(AttendancePeriod.CUSTOM, viewModel.uiState.value.selectedPeriod)
            assertEquals(emptyList<AttendanceRecord>(), viewModel.uiState.value.records)
            assertNull(viewModel.uiState.value.summary)
            assertFalse(viewModel.uiState.value.canLoadMore)
            assertFalse(viewModel.uiState.value.isLoading)
            assertFalse(viewModel.uiState.value.isLoadingMore)
        } finally {
            Dispatchers.resetMain()
        }
    }

    @Test
    fun onFilterChanged_customIgnoresStaleMonthlyResponse() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        Dispatchers.setMain(dispatcher)
        try {
            val repository = DelayedAttendanceHistoryRepository()
            val viewModel = HistoryViewModel(GetAttendanceHistoryUseCase(repository))
            runCurrent()

            viewModel.onFilterChanged(AttendancePeriod.CUSTOM)
            repository.response.complete(Result.success(repository.pageWithRecord()))
            advanceUntilIdle()

            assertEquals(listOf(AttendancePeriod.MONTHLY), repository.requestedPeriods)
            assertEquals(AttendancePeriod.CUSTOM, viewModel.uiState.value.selectedPeriod)
            assertEquals(emptyList<AttendanceRecord>(), viewModel.uiState.value.records)
            assertNull(viewModel.uiState.value.summary)
            assertFalse(viewModel.uiState.value.canLoadMore)
        } finally {
            Dispatchers.resetMain()
        }
    }

    private class FakeAttendanceHistoryRepository : AttendanceHistoryRepository {
        val requestedPeriods = mutableListOf<String>()

        override suspend fun getAttendanceHistory(
            period: String,
            page: Int,
            limit: Int
        ): Result<AttendanceHistoryPage> {
            requestedPeriods += period
            return Result.success(emptyPage())
        }

        private fun emptyPage(): AttendanceHistoryPage {
            return AttendanceHistoryPage(
                summary = AttendanceSummaryInfo(
                    totalOntime = 0,
                    totalLate = 0,
                    totalAlpha = 0,
                    totalWfo = 0,
                    totalWfa = 0
                ),
                records = emptyList(),
                pagination = Pagination(
                    currentPage = 1,
                    totalPages = 1,
                    totalItems = 0,
                    itemsPerPage = 10,
                    hasNextPage = false,
                    hasPrevPage = false
                )
            )
        }
    }

    private class DelayedAttendanceHistoryRepository : AttendanceHistoryRepository {
        val requestedPeriods = mutableListOf<String>()
        val response = CompletableDeferred<Result<AttendanceHistoryPage>>()

        override suspend fun getAttendanceHistory(
            period: String,
            page: Int,
            limit: Int
        ): Result<AttendanceHistoryPage> {
            requestedPeriods += period
            return response.await()
        }

        fun pageWithRecord(): AttendanceHistoryPage {
            return AttendanceHistoryPage(
                summary = AttendanceSummaryInfo(
                    totalOntime = 1,
                    totalLate = 0,
                    totalAlpha = 0,
                    totalWfo = 1,
                    totalWfa = 0
                ),
                records = listOf(
                    AttendanceRecord(
                        id = 1,
                        date = "08",
                        monthYear = "Jul 2026",
                        timeIn = "08:00",
                        timeOut = "17:00",
                        workHour = "08:00",
                        attendanceDate = "2026-07-08",
                        category = "WFO",
                        status = "on_time",
                        location = "Palu"
                    )
                ),
                pagination = Pagination(
                    currentPage = 1,
                    totalPages = 1,
                    totalItems = 1,
                    itemsPerPage = 10,
                    hasNextPage = false,
                    hasPrevPage = false
                )
            )
        }
    }
}
