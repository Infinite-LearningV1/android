package com.example.infinite_track.data.soucre.network.response

import com.google.gson.annotations.SerializedName

data class AuthPayload(
    @SerializedName("access_token") val accessToken: String? = null,
    @SerializedName("refresh_token") val refreshToken: String? = null
)
