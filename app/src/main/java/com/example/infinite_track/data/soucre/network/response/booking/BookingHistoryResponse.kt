package com.example.infinite_track.data.soucre.network.response.booking

import com.google.gson.annotations.SerializedName

data class BookingHistoryResponse(
    @SerializedName("success")
    val success: Boolean = false,
    @SerializedName("message")
    val message: String? = null,
    @SerializedName("data")
    val data: BookingData? = null
)

data class BookingData(
    @SerializedName("summary")
    val summary: BookingSummaryData? = null,
    @SerializedName("bookings")
    val bookings: List<BookingItem> = emptyList(),
    @SerializedName("pagination")
    val pagination: PaginationData? = null,
    @SerializedName("filters")
    val filters: FilterData? = null
)

data class BookingSummaryData(
    @SerializedName("total")
    val total: Int = 0,
    @SerializedName("pending")
    val pending: Int = 0,
    @SerializedName("approved")
    val approved: Int = 0,
    @SerializedName("rejected")
    val rejected: Int = 0
)

data class BookingItem(
    @SerializedName("booking_id")
    val bookingId: Int = 0,
    @SerializedName("user_id")
    val userId: Int? = null,
    @SerializedName("user_full_name")
    val userFullName: String? = null,
    @SerializedName("user_email")
    val userEmail: String? = null,
    @SerializedName("user_nip_nim")
    val userNipNim: String? = null,
    @SerializedName("user_position_name")
    val userPositionName: String? = null,
    @SerializedName("user_role_name")
    val userRoleName: String? = null,
    @SerializedName("schedule_date")
    val scheduleDate: String? = null,
    @SerializedName("status")
    val status: String? = null,
    @SerializedName("status_key")
    val statusKey: String? = null,
    @SerializedName("status_label")
    val statusLabel: String? = null,
    @SerializedName("location")
    val location: BookingLocation? = null,
    @SerializedName("notes")
    val notes: String? = null,
    @SerializedName("suitability_score")
    val suitabilityScore: Double? = null,
    @SerializedName("suitability_label")
    val suitabilityLabel: String? = null,
    @SerializedName("created_at")
    val createdAt: String? = null,
    @SerializedName("processed_at")
    val processedAt: String? = null,
    @SerializedName("approved_by")
    val approvedBy: Int? = null
)

data class BookingLocation(
    @SerializedName("location_id")
    val locationId: Int? = null,
    @SerializedName("latitude")
    val latitude: Double? = null,
    @SerializedName("longitude")
    val longitude: Double? = null,
    @SerializedName("radius")
    val radius: Float? = null,
    @SerializedName("description")
    val description: String? = null
)

data class PaginationData(
    @SerializedName("current_page")
    val currentPage: Int = 1,
    @SerializedName("total_pages")
    val totalPages: Int = 1,
    @SerializedName("total_items")
    val totalItems: Int = 0,
    @SerializedName("items_per_page")
    val itemsPerPage: Int = 10,
    @SerializedName("has_next_page")
    val hasNextPage: Boolean = false,
    @SerializedName("has_previous_page")
    val hasPreviousPage: Boolean = false
)

data class FilterData(
    @SerializedName("status")
    val status: String? = null,
    @SerializedName("sort_by")
    val sortBy: String? = null,
    @SerializedName("sort_order")
    val sortOrder: String? = null
)
