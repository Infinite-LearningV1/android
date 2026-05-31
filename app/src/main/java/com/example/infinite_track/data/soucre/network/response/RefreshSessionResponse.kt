package com.example.infinite_track.data.soucre.network.response

import com.google.gson.annotations.SerializedName

data class RefreshResponse(
    @SerializedName("success") val success: Boolean,
    @SerializedName("data") val data: AuthData,
    @SerializedName("code") val code: String? = null,
    @SerializedName("message") val message: String? = null
)

data class AuthData(
    @SerializedName("id") val id: Int,
    @SerializedName("token") val token: String,
    @SerializedName("refresh_token") val refreshToken: String? = null
)

data class RefreshErrorResponse(
    @SerializedName("success") val success: Boolean,
    @SerializedName("code") val code: String? = null,
    @SerializedName("message") val message: String? = null
)

typealias RefreshSessionResponse = RefreshResponse
typealias RefreshSessionData = AuthData
