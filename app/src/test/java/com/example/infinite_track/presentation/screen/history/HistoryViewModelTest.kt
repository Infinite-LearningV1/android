package com.example.infinite_track.presentation.screen.history

import com.example.infinite_track.data.soucre.network.response.Pagination
import com.example.infinite_track.domain.model.attendance.AttendanceHistoryPage
import com.example.infinite_track.domain.model.attendance.AttendanceModeDistributionInfo
import com.example.infinite_track.domain.model.attendance.AttendanceModeMetricInfo
import com.example.infinite_track.domain.model.attendance.AttendancePeriod
import com.example.infinite_track.domain.model.attendance.AttendancePeriodInfo
import com.example.infinite_track.domain.model.attendance.AttendanceRecord
import com.example.infinite_track.domain.model.attendance.AttendanceReportPdfResult
import com.example.infinite_track.domain.model.attendance.AttendanceSummaryInfo
import com.example.infinite_track.domain.repository.AttendanceHistoryRepository
import com.example.infinite_track.domain.repository.AttendanceReportPdfRepository
import com.example.infinite_track.domain.use_case.history.ExportAttendanceReportPdfUseCase
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

    companion object {
        private fun periodInfo(): AttendancePeriodInfo {
            return AttendancePeriodInfo(
                type = AttendancePeriod.MONTHLY,
                label = "This Month",
                startDate = "2026-07-01",
                endDate = "2026-07-31"
            )
        }

        private fun summaryInfo(
            totalOntime: Int,
            totalLate: Int,
            totalAlpha: Int,
            totalWfo: Int,
            totalWfa: Int,
            totalWfh: Int,
            totalPresent: Int,
            totalAbsent: Int,
            totalCountedDays: Int,
            attendanceRate: Int?,
            attendanceRateLabel: String?,
            totalWorkHoursLabel: String?
        ): AttendanceSummaryInfo {
            return AttendanceSummaryInfo(
                totalOntime = totalOntime,
                totalLate = totalLate,
                totalEarly = 0,
                totalAlpha = totalAlpha,
                totalWfo = totalWfo,
                totalWfa = totalWfa,
                totalWfh = totalWfh,
                totalWorkHours = null,
                totalPresent = totalPresent,
                totalAbsent = totalAbsent,
                totalCountedDays = totalCountedDays,
                totalWorkingDays = null,
                attendanceRate = attendanceRate,
                attendanceRateLabel = attendanceRateLabel,
                attendanceRateDenominator = "total_counted_days",
                totalWorkHoursLabel = totalWorkHoursLabel,
                modeDistribution = AttendanceModeDistributionInfo(
                    total = totalCountedDays,
                    wfo = AttendanceModeMetricInfo("wfo", "WFO", totalWfo, 100),
                    wfa = AttendanceModeMetricInfo("wfa", "WFA", totalWfa, 0),
                    wfh = AttendanceModeMetricInfo("wfh", "WFH", totalWfh, 0)
                )
            )
        }
    }

    @Test
    fun onFilterChanged_customDoesNotRequestBackendAndClearsReportState() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        Dispatchers.setMain(dispatcher)
        try {
            val repository = FakeAttendanceHistoryRepository()
            val viewModel = HistoryViewModel(GetAttendanceHistoryUseCase(repository), ExportAttendanceReportPdfUseCase(FakeAttendanceReportPdfRepository()))
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
            val viewModel = HistoryViewModel(GetAttendanceHistoryUseCase(repository), ExportAttendanceReportPdfUseCase(FakeAttendanceReportPdfRepository()))
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
                period = periodInfo(),
                summary = summaryInfo(
                    totalOntime = 0,
                    totalLate = 0,
                    totalAlpha = 0,
                    totalWfo = 0,
                    totalWfa = 0,
                    totalWfh = 0,
                    totalPresent = 0,
                    totalAbsent = 0,
                    totalCountedDays = 0,
                    attendanceRate = null,
                    attendanceRateLabel = null,
                    totalWorkHoursLabel = null
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
                period = periodInfo(),
                summary = summaryInfo(
                    totalOntime = 1,
                    totalLate = 0,
                    totalAlpha = 0,
                    totalWfo = 1,
                    totalWfa = 0,
                    totalWfh = 0,
                    totalPresent = 1,
                    totalAbsent = 0,
                    totalCountedDays = 1,
                    attendanceRate = 100,
                    attendanceRateLabel = "100%",
                    totalWorkHoursLabel = "8h"
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
                        dateLabel = "08 Jul 2026",
                        modeKey = "wfo",
                        modeLabel = "WFO",
                        timeRange = "08:00 - 17:00",
                        statusKey = "ontime",
                        statusLabel = "On Time",
                        displayBadgeKey = "ontime",
                        displayBadgeLabel = "On Time",
                        locationLabel = "Palu",
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

    private class FakeAttendanceReportPdfRepository : AttendanceReportPdfRepository {
        override suspend fun previewAttendanceReportPdf(
            period: String,
            startDate: String?,
            endDate: String?,
            timezone: String?
        ): Result<AttendanceReportPdfResult> = Result.failure(NotImplementedError())

        override suspend fun exportAttendanceReportPdf(
            period: String,
            startDate: String?,
            endDate: String?,
            timezone: String?
        ): Result<AttendanceReportPdfResult> = Result.failure(NotImplementedError())
    }

}
