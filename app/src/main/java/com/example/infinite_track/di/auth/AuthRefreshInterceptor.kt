package com.example.infinite_track.di.auth

import com.example.infinite_track.data.soucre.local.preferences.UserPreference
import com.example.infinite_track.data.soucre.network.response.RefreshErrorResponse
import com.example.infinite_track.domain.manager.SessionManager
import com.example.infinite_track.domain.repository.AuthRefreshException
import com.example.infinite_track.domain.repository.AuthRefreshFailureKind
import com.example.infinite_track.domain.repository.AuthRefreshFailureReason
import com.example.infinite_track.domain.use_case.auth.LogoutUseCase
import com.google.gson.Gson
import com.google.gson.JsonSyntaxException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import okhttp3.Interceptor
import okhttp3.Request
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

        val requestWithToken = originalRequest.newBuilder()
            .header(HEADER_CLIENT_TYPE, CLIENT_TYPE_MOBILE)
            .apply {
                if (!token.isNullOrBlank() && !isAuthEndpoint(originalRequest)) {
                    header(HEADER_AUTHORIZATION, "Bearer $token")
                }
            }
            .build()

        val response = chain.proceed(requestWithToken)

        if (response.code != HTTP_UNAUTHORIZED) {
            return response
        }

        if (isAuthEndpoint(requestWithToken)) {
            return response
        }

        val authCode = parseAuthCode(response)
        forcedReauthReasonFor(authCode)?.let { reason ->
            triggerForcedReauth(reason)
            return response
        }

        if (!shouldAttemptRefresh(requestWithToken, authCode)) {
            return response
        }

        val refreshResult = try {
            runBlocking { refreshSingleFlightCoordinator.runRefresh() }
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
            return response
        }

        return refreshResult.fold(
            onSuccess = {
                response.close()
                val newToken = runBlocking { userPreference.getAuthToken().first() }
                val retriedRequest = originalRequest.newBuilder()
                    .header(HEADER_CLIENT_TYPE, CLIENT_TYPE_MOBILE)
                    .header(HEADER_RETRY_MARKER, RETRY_MARKER_VALUE)
                    .apply {
                        removeHeader(HEADER_AUTHORIZATION)
                        if (!newToken.isNullOrBlank()) {
                            header(HEADER_AUTHORIZATION, "Bearer $newToken")
                        }
                    }
                    .build()

                chain.proceed(retriedRequest)
            },
            onFailure = { throwable ->
                val refreshException = throwable as? AuthRefreshException
                if (refreshException?.kind == AuthRefreshFailureKind.NON_REFRESHABLE) {
                    triggerForcedReauth(reauthReasonFor(refreshException.reason))
                }
                response
            }
        )
    }

    private fun shouldAttemptRefresh(request: Request, authCode: String?): Boolean {
        if (request.header(HEADER_RETRY_MARKER) == RETRY_MARKER_VALUE) {
            return false
        }

        if (isAuthPath(request.url.encodedPath)) {
            return false
        }

        val hasBearerToken = request.header(HEADER_AUTHORIZATION)?.startsWith("Bearer ") == true
        return hasBearerToken && (authCode == null || authCode == AUTH_ACCESS_TOKEN_EXPIRED)
    }

    private fun triggerForcedReauth(reason: SessionManager.ReauthReason) {
        val sessionManager = sessionManagerProvider.get()
        if (sessionManager.isBootstrapSessionInProgress) {
            return
        }

        if (sessionManager.beginSessionExpiryHandling()) {
            runBlocking {
                logoutUseCaseProvider.get().invoke()
            }
            sessionManager.triggerForcedReauth(reason)
        }
    }

    private fun forcedReauthReasonFor(authCode: String?): SessionManager.ReauthReason? {
        return when (authCode) {
            AUTH_SESSION_INACTIVE -> SessionManager.ReauthReason.INACTIVITY_EXPIRED
            AUTH_REFRESH_TOKEN_INVALID -> SessionManager.ReauthReason.REFRESH_INVALID
            AUTH_REFRESH_TOKEN_REVOKED -> SessionManager.ReauthReason.REFRESH_REVOKED
            else -> null
        }
    }

    private fun reauthReasonFor(reason: AuthRefreshFailureReason): SessionManager.ReauthReason {
        return when (reason) {
            AuthRefreshFailureReason.INACTIVITY_EXPIRED -> SessionManager.ReauthReason.INACTIVITY_EXPIRED
            AuthRefreshFailureReason.REFRESH_INVALID -> SessionManager.ReauthReason.REFRESH_INVALID
            AuthRefreshFailureReason.REFRESH_REVOKED -> SessionManager.ReauthReason.REFRESH_REVOKED
            else -> SessionManager.ReauthReason.UNKNOWN
        }
    }

    private fun parseAuthCode(response: Response): String? {
        return try {
            val body = response.peekBody(MAX_ERROR_BODY_BYTES).string()
            if (body.isBlank()) {
                null
            } else {
                Gson().fromJson(body, RefreshErrorResponse::class.java)?.code
            }
        } catch (_: JsonSyntaxException) {
            null
        }
    }

    private fun isAuthEndpoint(request: Request): Boolean {
        return isAuthPath(request.url.encodedPath)
    }

    private fun isAuthPath(path: String): Boolean {
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

    companion object {
        private const val HTTP_UNAUTHORIZED = 401
        private const val HEADER_AUTHORIZATION = "Authorization"
        private const val HEADER_CLIENT_TYPE = "X-Client-Type"
        private const val CLIENT_TYPE_MOBILE = "mobile"
        private const val HEADER_RETRY_MARKER = "X-Refresh-Retry"
        private const val RETRY_MARKER_VALUE = "1"
        private const val MAX_ERROR_BODY_BYTES = 1024 * 1024L

        private const val AUTH_ACCESS_TOKEN_EXPIRED = "AUTH_ACCESS_TOKEN_EXPIRED"
        private const val AUTH_REFRESH_TOKEN_INVALID = "AUTH_REFRESH_TOKEN_INVALID"
        private const val AUTH_REFRESH_TOKEN_REVOKED = "AUTH_REFRESH_TOKEN_REVOKED"
        private const val AUTH_SESSION_INACTIVE = "AUTH_SESSION_INACTIVE"
    }
}
