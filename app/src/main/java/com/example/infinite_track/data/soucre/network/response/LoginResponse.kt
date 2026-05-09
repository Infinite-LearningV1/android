package com.example.infinite_track.data.soucre.network.response

import com.google.gson.annotations.SerializedName

data class LoginResponse(
    @SerializedName("success") val success: Boolean,
    @SerializedName("data") val data: UserData,
    @SerializedName("message") val message: String
)

data class UserData(
    @SerializedName("id") val id: Int,
    @SerializedName("full_name") val fullName: String,
    @SerializedName("email") val email: String,
    @SerializedName("role_name") val roleName: String,
    @SerializedName("position_name") val positionName: String,
    @SerializedName("program_name") val programName: String,
    @SerializedName("division_name") val divisionName: String,
    @SerializedName("nip_nim") val nipNim: String,
    @SerializedName("phone") val phone: String,
    @SerializedName("photo") val photo: String,
    @SerializedName("photo_updated_at") val photoUpdatedAt: String,
    @SerializedName("location") val location: LocationData,
    @SerializedName("token") val token: String
)

data class LocationData(
    @SerializedName("latitude") val latitude: Double,
    @SerializedName("longitude") val longitude: Double,
    @SerializedName("radius") val radius: Int,
    @SerializedName("description") val description: String,
    @SerializedName("category_name") val categoryName: String
)
