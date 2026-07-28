package com.example.infinite_track.data.soucre.network.response.booking

import com.google.gson.annotations.SerializedName

data class WfaRequestResponseDto(
    @SerializedName("success") val success: Boolean,
    @SerializedName("message") val message: String?,
    @SerializedName("data") val data: WfaRequestResponseDataDto?
)

data class WfaRequestResponseDataDto(
    @SerializedName("booking_id") val bookingId: Long?,
    @SerializedName("schedule_date") val scheduleDate: String?,
    @SerializedName("status") val status: String?,
    @SerializedName("location") val location: WfaRequestLocationDto?,
    @SerializedName("request_reason") val reason: WfaRequestReasonResultDto?,
    @SerializedName("radius_snapshot", alternate = ["radius_meters"]) val radiusMeters: Int?,
    @SerializedName("created_at", alternate = ["submitted_at"]) val submittedAt: String?,
    @SerializedName("request_reason_label") val reasonLabel: String? = null
)

data class WfaRequestLocationDto(
    @SerializedName("latitude") val latitude: Double?,
    @SerializedName("longitude") val longitude: Double?,
    @SerializedName("description", alternate = ["display_name"]) val displayName: String?,
    @SerializedName("formatted_address", alternate = ["address"]) val formattedAddress: String?
)

data class WfaRequestReasonResultDto(
    @SerializedName("id") val id: Long?,
    @SerializedName("label") val label: String?
)

data class WfaRequestErrorResponseDto(
    @SerializedName("success") val success: Boolean?,
    @SerializedName("message") val message: String?,
    @SerializedName("code") val code: String?,
    @SerializedName("errors") val errors: List<WfaRequestErrorItemDto> = emptyList()
)

data class WfaRequestErrorItemDto(
    @SerializedName("field") val field: String?,
    @SerializedName("code") val code: String?,
    @SerializedName("message") val message: String?
)
