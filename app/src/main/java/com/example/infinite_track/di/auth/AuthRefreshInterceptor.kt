package com.example.infinite_track.di.auth

import com.example.infinite_track.data.soucre.local.preferences.UserPreference
import com.example.infinite_track.domain.manager.SessionManager
import com.example.infinite_track.domain.repository.RefreshSessionResult
import com.example.infinite_track.domain.use_case.auth.LogoutUseCase
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.Interceptor
import okhttp3.Response
import javax.inject.Inject
import javax.inject.Provider
import javax.inject.Singleton

@Singleton
class AuthRefreshInterceptor @Inject constructor(
    private val userPreference: UserPreference,
    private val refreshSingleFlightCoordinator: RefreshSingleFlightCoordinator,
    private val logoutUseCaseProvider: Provider<LogoutUseCase>,
    private val sessionManagerProvider: Provider<SessionManager>
) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val originalRequest = chain.request()
        val token = runBlocking { userPreference.getAuthToken().first() }

        val requestWithToken = originalRequest.newBuilder().apply {
            if (!token.isNullOrBlank() && !isAuthEndpoint(originalRequest)) {
                header(HEADER_AUTHORIZATION, "Bearer $token")
            }
        }.build()

        val response = chain.proceed(requestWithToken)

        if (response.code != HTTP_UNAUTHORIZED) {
            return response
        }

        if (!shouldAttemptRefresh(requestWithToken)) {
            return response
        }

        val refreshResult = try {
            runBlocking { refreshSingleFlightCoordinator.refreshOrJoin() }
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
            return response
        }

        return when (refreshResult) {
            RefreshSessionResult.Success -> {
                response.close()
                val newToken = runBlocking { userPreference.getAuthToken().first() }
                val retriedRequest = originalRequest.newBuilder()
                    .header(HEADER_RETRY_MARKER, RETRY_MARKER_VALUE)
                    .apply {
                        removeHeader(HEADER_AUTHORIZATION)
                        if (!newToken.isNullOrBlank()) {
                            header(HEADER_AUTHORIZATION, "Bearer $newToken")
                        }
                    }
                    .build()

                chain.proceed(retriedRequest)
            }

            is RefreshSessionResult.ReAuthRequired -> {
                val sessionManager = sessionManagerProvider.get()
                if (sessionManager.beginSessionExpiryHandling()) {
                    runBlocking {
                        logoutUseCaseProvider.get().invoke()
                    }
                    sessionManager.triggerSessionExpired()
                }
                response
            }

            is RefreshSessionResult.TemporaryFailure -> {
                response
            }
        }
    }

    private fun shouldAttemptRefresh(request: okhttp3.Request): Boolean {
        if (request.header(HEADER_RETRY_MARKER) == RETRY_MARKER_VALUE) {
            return false
        }

        val url = request.url.toString()
        if (isRefreshRequest(url) || isLogoutRequest(url) || isLoginRequest(url)) {
            return false
        }

        return request.header(HEADER_AUTHORIZATION)?.startsWith("Bearer ") == true
    }

    private fun isAuthEndpoint(request: okhttp3.Request): Boolean {
        val path = request.url.encodedPath
        return isRefreshPath(path) || isLogoutPath(path) || isLoginPath(path)
    }

    private fun isRefreshRequest(url: String): Boolean {
        return isRefreshPath(extractPath(url))
    }

    private fun isLogoutRequest(url: String): Boolean {
        return isLogoutPath(extractPath(url))
    }

    private fun isLoginRequest(url: String): Boolean {
        return isLoginPath(extractPath(url))
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

    private fun extractPath(url: String): String {
        return url.toHttpUrlOrNull()?.encodedPath ?: url.substringBefore('?')
    }

    companion object {
        private const val HTTP_UNAUTHORIZED = 401
        private const val HEADER_AUTHORIZATION = "Authorization"
        private const val HEADER_RETRY_MARKER = "X-Refresh-Retry"
        private const val RETRY_MARKER_VALUE = "1"
    }
}
