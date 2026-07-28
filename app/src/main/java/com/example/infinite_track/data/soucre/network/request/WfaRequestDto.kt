package com.example.infinite_track.data.soucre.network.request

import com.google.gson.annotations.SerializedName

data class WfaRequestDto(
    @SerializedName("schedule_date") val scheduleDate: String,
    @SerializedName("request_reason_id") val reasonId: Long,
    @SerializedName("request_other_reason") val otherReasonText: String?,
    @SerializedName("notes") val notes: String?,
    @SerializedName("latitude") val latitude: Double,
    @SerializedName("longitude") val longitude: Double
)
