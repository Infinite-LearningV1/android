package com.example.infinite_track.data.soucre.network.auth

import okhttp3.Request

object AuthEndpointMatcher {
    fun isAuthEndpoint(request: Request): Boolean {
        return isAuthPath(request.url.encodedPath)
    }

    fun isAuthPath(path: String): Boolean {
        return isRefreshPath(path) || isLogoutPath(path) || isLoginPath(path)
    }

    private fun isRefreshPath(path: String): Boolean {
        return path == "/api/auth/refresh" || path.endsWith("/auth/refresh") || path.endsWith("/refresh")
    }

    private fun isLogoutPath(path: String): Boolean {
        return path == "/api/auth/logout" || path.endsWith("/auth/logout") || path.endsWith("/logout")
    }

    private fun isLoginPath(path: String): Boolean {
        return path == "/api/auth/login" || path.endsWith("/auth/login") || path.endsWith("/login")
    }
}
