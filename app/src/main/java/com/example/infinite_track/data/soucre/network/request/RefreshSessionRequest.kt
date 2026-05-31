package com.example.infinite_track.data.soucre.network.request

import com.google.gson.annotations.SerializedName

data class RefreshRequest(
    @SerializedName("refresh_token") val refreshToken: String
)

typealias RefreshSessionRequest = RefreshRequest
