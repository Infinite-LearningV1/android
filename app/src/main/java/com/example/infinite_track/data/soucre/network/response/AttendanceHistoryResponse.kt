package com.example.infinite_track.data.soucre.network.response

import com.google.gson.annotations.SerializedName

data class AttendanceHistoryResponse(
    @SerializedName("success") val success: Boolean,
    @SerializedName("data") val data: AttendanceHistoryData,
    @SerializedName("message") val message: String
)

data class AttendanceHistoryData(
    @SerializedName("summary") val summary: AttendanceSummary,
    @SerializedName("attendances") val attendances: List<AttendanceItem>,
    @SerializedName("pagination") val pagination: Pagination
)

data class AttendanceSummary(
    @SerializedName("total_ontime") val totalOntime: Int,
    @SerializedName("total_late") val totalLate: Int,
    @SerializedName("total_alpha") val totalAlpha: Int,
    @SerializedName("total_wfo") val totalWfo: Int,
    @SerializedName("total_wfa") val totalWfa: Int
)

data class AttendanceItem(
    @SerializedName("id_attendance") val idAttendance: Int,
    @SerializedName("attendance_date") val attendanceDate: String,
    @SerializedName("date") val date: String,
    @SerializedName("monthYear") val monthYear: String,
    @SerializedName("time_in") val timeIn: String,
    @SerializedName("time_out") val timeOut: String?,
    @SerializedName("work_hour") val workHour: String?,
    @SerializedName("category") val category: String,
    @SerializedName("status") val status: String,
    @SerializedName("location") val location: String,
    @SerializedName("notes") val notes: String?
)

data class Pagination(
    @SerializedName("current_page") val currentPage: Int,
    @SerializedName("total_pages") val totalPages: Int,
    @SerializedName("total_items") val totalItems: Int,
    @SerializedName("items_per_page") val itemsPerPage: Int,
    @SerializedName("has_next_page") val hasNextPage: Boolean,
    @SerializedName("has_prev_page") val hasPrevPage: Boolean
)
