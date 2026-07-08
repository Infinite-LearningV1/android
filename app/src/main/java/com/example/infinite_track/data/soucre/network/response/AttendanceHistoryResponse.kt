package com.example.infinite_track.data.soucre.network.response

import com.google.gson.annotations.SerializedName

data class AttendanceHistoryResponse(
    @SerializedName("success") val success: Boolean,
    @SerializedName("data") val data: AttendanceHistoryData,
    @SerializedName("message") val message: String
)

data class AttendanceHistoryData(
    @SerializedName("period") val period: AttendancePeriodDto,
    @SerializedName("summary") val summary: AttendanceSummary,
    @SerializedName("attendances") val attendances: List<AttendanceItem>,
    @SerializedName("pagination") val pagination: Pagination
)

data class AttendancePeriodDto(
    @SerializedName("type") val type: String,
    @SerializedName("label") val label: String,
    @SerializedName("start_date") val startDate: String,
    @SerializedName("end_date") val endDate: String
)

data class AttendanceSummary(
    @SerializedName("total_ontime") val totalOntime: Int,
    @SerializedName("total_late") val totalLate: Int,
    @SerializedName("total_early") val totalEarly: Int,
    @SerializedName("total_alpha") val totalAlpha: Int,
    @SerializedName("total_wfo") val totalWfo: Int,
    @SerializedName("total_wfa") val totalWfa: Int,
    @SerializedName("total_wfh") val totalWfh: Int,
    @SerializedName("total_work_hours") val totalWorkHours: Double?,
    @SerializedName("total_present") val totalPresent: Int,
    @SerializedName("total_absent") val totalAbsent: Int,
    @SerializedName("total_counted_days") val totalCountedDays: Int,
    @SerializedName("total_working_days") val totalWorkingDays: Int?,
    @SerializedName("attendance_rate") val attendanceRate: Int?,
    @SerializedName("attendance_rate_label") val attendanceRateLabel: String?,
    @SerializedName("attendance_rate_denominator") val attendanceRateDenominator: String?,
    @SerializedName("total_work_hours_label") val totalWorkHoursLabel: String?,
    @SerializedName("mode_distribution") val modeDistribution: AttendanceModeDistributionDto?
)

data class AttendanceModeDistributionDto(
    @SerializedName("total") val total: Int,
    @SerializedName("wfo") val wfo: AttendanceModeMetricDto,
    @SerializedName("wfa") val wfa: AttendanceModeMetricDto,
    @SerializedName("wfh") val wfh: AttendanceModeMetricDto
)

data class AttendanceModeMetricDto(
    @SerializedName("key") val key: String,
    @SerializedName("label") val label: String,
    @SerializedName("count") val count: Int,
    @SerializedName("percentage") val percentage: Int
)

data class AttendanceItem(
    @SerializedName("id_attendance") val idAttendance: Int,
    @SerializedName("attendance_date") val attendanceDate: String,
    @SerializedName("date") val date: String,
    @SerializedName("monthYear") val monthYear: String,
    @SerializedName("date_label") val dateLabel: String?,
    @SerializedName("mode_key") val modeKey: String?,
    @SerializedName("mode_label") val modeLabel: String?,
    @SerializedName("time_in") val timeIn: String,
    @SerializedName("time_out") val timeOut: String?,
    @SerializedName("time_range") val timeRange: String?,
    @SerializedName("raw_time_in") val rawTimeIn: String?,
    @SerializedName("raw_time_out") val rawTimeOut: String?,
    @SerializedName("raw_time_range") val rawTimeRange: String?,
    @SerializedName("work_hour") val workHour: String?,
    @SerializedName("work_hour_raw") val workHourRaw: Double?,
    @SerializedName("status_key") val statusKey: String?,
    @SerializedName("status_label") val statusLabel: String?,
    @SerializedName("display_badge_key") val displayBadgeKey: String?,
    @SerializedName("display_badge_label") val displayBadgeLabel: String?,
    @SerializedName("location_label") val locationLabel: String?,
    @SerializedName("category") val category: String,
    @SerializedName("status") val status: String,
    @SerializedName("location") val location: String,
    @SerializedName("notes") val notes: String?
)

data class Pagination(
    @SerializedName("current_page") val currentPage: Int,
    @SerializedName("requested_page") val requestedPage: Int? = null,
    @SerializedName("page_was_clamped") val pageWasClamped: Boolean? = null,
    @SerializedName("total_pages") val totalPages: Int,
    @SerializedName("total_items") val totalItems: Int,
    @SerializedName("items_per_page") val itemsPerPage: Int,
    @SerializedName("has_next_page") val hasNextPage: Boolean,
    @SerializedName("has_prev_page") val hasPrevPage: Boolean
)
