package com.example.infinite_track.data.soucre.network.retrofit

import com.example.infinite_track.data.soucre.network.request.RefreshSessionRequest
import com.example.infinite_track.data.soucre.network.response.LogoutResponse
import com.example.infinite_track.data.soucre.network.response.RefreshSessionResponse
import retrofit2.http.Body
import retrofit2.http.Headers
import retrofit2.http.POST
import javax.inject.Singleton

@Singleton
interface AuthSessionApiService {
    @Headers("X-Client-Type: android")
    @POST("api/auth/refresh")
    suspend fun refreshSession(@Body request: RefreshSessionRequest): RefreshSessionResponse

    @POST("api/auth/logout")
    suspend fun logout(): LogoutResponse
}
