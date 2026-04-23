package com.example.infinite_track.data.soucre.network.response

import com.google.gson.annotations.SerializedName

data class RefreshSessionResponse(
    @SerializedName("success") val success: Boolean,
    @SerializedName("data") val data: RefreshSessionData,
    @SerializedName("message") val message: String
)

data class RefreshSessionData(
    @SerializedName("id") val id: Int,
    @SerializedName("token") val token: String,
    @SerializedName("refresh_token") val refreshToken: String? = null
)
