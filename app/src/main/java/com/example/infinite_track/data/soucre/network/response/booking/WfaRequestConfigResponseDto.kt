package com.example.infinite_track.data.soucre.network.response.booking

import com.google.gson.annotations.SerializedName

data class WfaRequestConfigResponseDto(
    @SerializedName("success") val success: Boolean,
    @SerializedName("message") val message: String?,
    @SerializedName("data") val data: WfaRequestConfigDataDto?
)

data class WfaRequestConfigDataDto(
    @SerializedName("radius_meters") val radiusMeters: Int?,
    @SerializedName("reasons") val reasons: List<WfaRequestReasonDto> = emptyList()
)

data class WfaRequestReasonDto(
    @SerializedName("id") val id: Long?,
    @SerializedName("label") val label: String?,
    @SerializedName("is_other") val isOther: Boolean = false
)
