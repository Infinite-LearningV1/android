package com.example.infinite_track.data.soucre.network

import com.example.infinite_track.data.soucre.network.request.RefreshRequest
import com.example.infinite_track.data.soucre.network.response.LoginResponse
import com.example.infinite_track.data.soucre.network.response.RefreshErrorResponse
import com.example.infinite_track.data.soucre.network.response.RefreshResponse
import com.google.gson.Gson
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AuthApiContractTest {
    private val gson = Gson()

    @Test
    fun `mobile login response parses required refresh token from json data`() {
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
                "token": "access-token-redacted",
                "refresh_token": "refresh-token-redacted"
              }
            }
        """.trimIndent()

        val response = gson.fromJson(json, LoginResponse::class.java)

        assertTrue(response.success)
        assertEquals("access-token-redacted", response.data.token)
        assertEquals("refresh-token-redacted", response.data.refreshToken)
    }

    @Test
    fun `refresh request serializes refresh token for backend json contract`() {
        val json = gson.toJson(RefreshRequest(refreshToken = "refresh-token-redacted"))

        assertEquals("{\"refresh_token\":\"refresh-token-redacted\"}", json)
    }

    @Test
    fun `refresh success response parses access and rotated refresh token`() {
        val json = """
            {
              "success": true,
              "code": null,
              "message": "Refresh success",
              "data": {
                "id": 147,
                "token": "new-access-token-redacted",
                "refresh_token": "new-refresh-token-redacted"
              }
            }
        """.trimIndent()

        val response = gson.fromJson(json, RefreshResponse::class.java)

        assertTrue(response.success)
        assertEquals(147, response.data.id)
        assertEquals("new-access-token-redacted", response.data.token)
        assertEquals("new-refresh-token-redacted", response.data.refreshToken)
        assertEquals("Refresh success", response.message)
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
