package com.example.infinite_track.data.soucre.network.response.booking

import com.google.gson.annotations.SerializedName

data class BookingHistoryResponse(
    @SerializedName("success")
    val success: Boolean,
    @SerializedName("message")
    val message: String,
    @SerializedName("data")
    val data: BookingData
)

data class BookingData(
    @SerializedName("bookings")
    val bookings: List<BookingItem>,
    @SerializedName("pagination")
    val pagination: PaginationData,
    @SerializedName("filters")
    val filters: FilterData
)

data class BookingItem(
    @SerializedName("booking_id")
    val bookingId: Int,
    @SerializedName("user_id")
    val userId: Int,
    @SerializedName("user_full_name")
    val userFullName: String?,
    @SerializedName("user_email")
    val userEmail: String?,
    @SerializedName("user_nip_nim")
    val userNipNim: String?,
    @SerializedName("user_position_name")
    val userPositionName: String?,
    @SerializedName("user_role_name")
    val userRoleName: String?,
    @SerializedName("schedule_date")
    val scheduleDate: String,
    @SerializedName("status")
    val status: String,
    @SerializedName("location")
    val location: BookingLocation,
    @SerializedName("notes")
    val notes: String?,
    @SerializedName("suitability_score")
    val suitabilityScore: Float?,
    @SerializedName("suitability_label")
    val suitabilityLabel: String?,
    @SerializedName("created_at")
    val createdAt: String,
    @SerializedName("processed_at")
    val processedAt: String?,
    @SerializedName("approved_by")
    val approvedBy: Int?
)

data class BookingLocation(
    @SerializedName("location_id")
    val locationId: Int,
    @SerializedName("latitude")
    val latitude: Double,
    @SerializedName("longitude")
    val longitude: Double,
    @SerializedName("radius")
    val radius: Float,
    @SerializedName("description")
    val description: String
)

data class PaginationData(
    @SerializedName("current_page")
    val currentPage: Int,
    @SerializedName("total_pages")
    val totalPages: Int,
    @SerializedName("total_items")
    val totalItems: Int,
    @SerializedName("items_per_page")
    val itemsPerPage: Int,
    @SerializedName("has_next_page")
    val hasNextPage: Boolean,
    @SerializedName("has_previous_page")
    val hasPreviousPage: Boolean
)

data class FilterData(
    @SerializedName("status")
    val status: String?,
    @SerializedName("sort_by")
    val sortBy: String,
    @SerializedName("sort_order")
    val sortOrder: String
)
