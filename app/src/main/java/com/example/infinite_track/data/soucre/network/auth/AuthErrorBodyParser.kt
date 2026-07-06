package com.example.infinite_track.data.soucre.network.auth

import com.example.infinite_track.data.soucre.network.response.RefreshErrorResponse
import com.google.gson.Gson
import com.google.gson.JsonSyntaxException
import okhttp3.Response

class AuthErrorBodyParser(
    private val gson: Gson = Gson()
) {
    fun parseAuthCode(response: Response): String? {
        return try {
            val body = response.peekBody(MAX_ERROR_BODY_BYTES).string()
            if (body.isBlank()) {
                null
            } else {
                gson.fromJson(body, RefreshErrorResponse::class.java)?.code
            }
        } catch (_: JsonSyntaxException) {
            null
        }
    }

    private companion object {
        private const val MAX_ERROR_BODY_BYTES = 1024 * 1024L
    }
}
