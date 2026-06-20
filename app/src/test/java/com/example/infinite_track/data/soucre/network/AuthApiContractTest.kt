package com.example.infinite_track.data.soucre.network

import com.example.infinite_track.data.soucre.network.request.RefreshRequest
import com.example.infinite_track.data.soucre.network.response.LoginResponse
import com.example.infinite_track.data.soucre.network.response.RefreshErrorResponse
import com.example.infinite_track.data.soucre.network.response.RefreshResponse
import com.example.infinite_track.data.soucre.network.retrofit.AuthSessionApiService
import com.google.gson.Gson
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import retrofit2.http.Headers

class AuthApiContractTest {
    private val gson = Gson()

    @Test
    fun `mobile login response parses primary auth token payload`() {
        val json = """
            {
              "success": true,
              "message": "Login success",
              "data": {
                "id": 147,
                "full_name": "Redacted User",
                "email": "redacted@example.test",
                "role_name": "Student",
                "position_name": "Learner",
                "program_name": "Infinite Learning",
                "division_name": "Mobile",
                "nip_nim": "NIM-REDACTED",
                "phone": "0000000000",
                "photo": "https://example.test/avatar.png",
                "photo_updated_at": "2026-05-30T00:00:00Z",
                "location": {
                  "latitude": -6.2,
                  "longitude": 106.8,
                  "radius": 100,
                  "description": "Redacted office",
                  "category_name": "Office"
                },
                "auth": {
                  "access_token": "primary-access-token-redacted",
                  "refresh_token": "primary-refresh-token-redacted"
                }
              }
            }
        """.trimIndent()

        val response = gson.fromJson(json, LoginResponse::class.java)

        assertTrue(response.success)
        assertEquals("primary-access-token-redacted", response.data.resolvedAccessToken())
        assertEquals("primary-refresh-token-redacted", response.data.resolvedRefreshToken())
    }

    @Test
    fun `mobile login response falls back to legacy direct token fields`() {
        val json = """
            {
              "success": true,
              "message": "Login success",
              "data": {
                "id": 147,
                "full_name": "Redacted User",
                "email": "redacted@example.test",
                "role_name": "Student",
                "position_name": "Learner",
                "program_name": "Infinite Learning",
                "division_name": "Mobile",
                "nip_nim": "NIM-REDACTED",
                "phone": "0000000000",
                "photo": "https://example.test/avatar.png",
                "photo_updated_at": "2026-05-30T00:00:00Z",
                "location": {
                  "latitude": -6.2,
                  "longitude": 106.8,
                  "radius": 100,
                  "description": "Redacted office",
                  "category_name": "Office"
                },
                "token": "legacy-access-token-redacted",
                "refresh_token": "legacy-refresh-token-redacted"
              }
            }
        """.trimIndent()

        val response = gson.fromJson(json, LoginResponse::class.java)

        assertTrue(response.success)
        assertEquals("legacy-access-token-redacted", response.data.resolvedAccessToken())
        assertEquals("legacy-refresh-token-redacted", response.data.resolvedRefreshToken())
    }

    @Test
    fun `refresh request serializes refresh token for backend json contract`() {
        val json = gson.toJson(RefreshRequest(refreshToken = "refresh-token-redacted"))

        assertEquals("{\"refresh_token\":\"refresh-token-redacted\"}", json)
    }

    @Test
    fun `refresh success response parses primary auth token payload`() {
        val json = """
            {
              "success": true,
              "code": null,
              "message": "Refresh success",
              "data": {
                "id": 147,
                "auth": {
                  "access_token": "primary-new-access-token-redacted",
                  "refresh_token": "primary-new-refresh-token-redacted"
                }
              }
            }
        """.trimIndent()

        val response = gson.fromJson(json, RefreshResponse::class.java)

        assertTrue(response.success)
        assertEquals(147, response.data.id)
        assertEquals("primary-new-access-token-redacted", response.data.resolvedAccessToken())
        assertEquals("primary-new-refresh-token-redacted", response.data.resolvedRefreshToken())
        assertEquals("Refresh success", response.message)
    }

    @Test
    fun `refresh success response falls back to legacy direct token fields`() {
        val json = """
            {
              "success": true,
              "code": null,
              "message": "Refresh success",
              "data": {
                "id": 147,
                "token": "legacy-new-access-token-redacted",
                "refresh_token": "legacy-new-refresh-token-redacted"
              }
            }
        """.trimIndent()

        val response = gson.fromJson(json, RefreshResponse::class.java)

        assertTrue(response.success)
        assertEquals(147, response.data.id)
        assertEquals("legacy-new-access-token-redacted", response.data.resolvedAccessToken())
        assertEquals("legacy-new-refresh-token-redacted", response.data.resolvedRefreshToken())
    }

    @Test
    fun `auth session refresh endpoint uses canonical mobile client type header`() {
        val method = AuthSessionApiService::class.java.getMethod(
            "refreshSession",
            com.example.infinite_track.data.soucre.network.request.RefreshSessionRequest::class.java
        )
        val headers = method.getAnnotation(Headers::class.java)?.value?.toList().orEmpty()

        assertTrue(headers.contains("X-Client-Type: mobile"))
        assertFalse(headers.any { it.equals("X-Client-Type: android", ignoreCase = true) })
    }

    @Test
    fun `refresh error response parses auth code for 401 handling`() {
        val json = """
            {
              "success": false,
              "code": "AUTH_SESSION_INACTIVE",
              "message": "Session inactive for more than 48 hours"
            }
        """.trimIndent()

        val response = gson.fromJson(json, RefreshErrorResponse::class.java)

        assertFalse(response.success)
        assertEquals("AUTH_SESSION_INACTIVE", response.code)
        assertEquals("Session inactive for more than 48 hours", response.message)
    }
}
