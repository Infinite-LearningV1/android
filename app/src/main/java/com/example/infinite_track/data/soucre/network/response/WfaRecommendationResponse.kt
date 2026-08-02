package com.example.infinite_track.data.soucre.network.response

import com.google.gson.annotations.SerializedName

data class WfaRecommendationResponse(
    @SerializedName("success") val success: Boolean,
    @SerializedName("message") val message: String,
    @SerializedName("data") val data: WfaData,
    @SerializedName("meta") val meta: WfaRecommendationMetaDto? = null
)

data class WfaData(
    @SerializedName("schedule_date") val scheduleDate: String,
    @SerializedName("timezone") val timezone: String,
    @SerializedName("work_window") val workWindow: WorkWindowDto?,
    @SerializedName("recommendations") val recommendations: List<RecommendationItem>
)

data class WorkWindowDto(
    @SerializedName("start") val start: String?,
    @SerializedName("end") val end: String?
)

data class WfaRecommendationMetaDto(
    @SerializedName("search_radius_meters") val searchRadiusMeters: Double? = null,
    @SerializedName("candidates_found") val candidatesFound: Int? = null,
    @SerializedName("candidates_returned") val candidatesReturned: Int? = null
)

data class RecommendationItem(
    @SerializedName("place_id") val placeId: String?,
    @SerializedName("name") val name: String,
    @SerializedName("address") val address: String,
    @SerializedName("latitude") val latitude: Double,
    @SerializedName("longitude") val longitude: Double,
    @SerializedName("distance_meters") val distanceMeters: Double,
    @SerializedName("place_type") val placeType: String,
    @SerializedName("status") val status: String,
    @SerializedName("final_rank") val finalRank: Int?,
    @SerializedName("final_score") val finalScore: Double?,
    @SerializedName("final_label") val finalLabel: String?,
    @SerializedName("facility_score") val facilityScore: Double?,
    @SerializedName("facility_confidence") val facilityConfidence: Int,
    @SerializedName("facilities") val facilities: FacilityEvidenceDto
)

data class FacilityEvidenceDto(
    @SerializedName("internet_access") val internetAccess: Boolean?,
    @SerializedName("opening_hours") val openingHours: Boolean?,
    @SerializedName("toilets") val toilets: Boolean?,
    @SerializedName("air_conditioning") val airConditioning: Boolean?,
    @SerializedName("wheelchair_accessibility") val wheelchairAccessibility: Boolean?
)
