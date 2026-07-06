package com.example.infinite_track.data.soucre.network.auth

import com.example.infinite_track.data.soucre.network.retrofit.ApiService

object AuthHeaderNames {
    const val AUTHORIZATION = "Authorization"
    const val CLIENT_TYPE = "X-Client-Type"
    const val CLIENT_TYPE_MOBILE = "mobile"
    const val RETRY_MARKER = "X-Refresh-Retry"
    const val RETRY_MARKER_VALUE = "1"
    const val BOOTSTRAP_AUTH_REQUEST = ApiService.HEADER_BOOTSTRAP_AUTH_REQUEST
    const val BOOTSTRAP_AUTH_REQUEST_VALUE = ApiService.BOOTSTRAP_AUTH_REQUEST_VALUE
}
