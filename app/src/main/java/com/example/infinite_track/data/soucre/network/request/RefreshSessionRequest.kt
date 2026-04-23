package com.example.infinite_track.data.soucre.network.request

import com.google.gson.annotations.SerializedName

data class RefreshSessionRequest(
    @SerializedName("refresh_token") val refreshToken: String
)
