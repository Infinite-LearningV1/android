package com.example.infinite_track.data.soucre.network.auth

import com.example.infinite_track.data.soucre.local.preferences.UserPreference
import com.example.infinite_track.domain.model.auth.ReauthReason
import com.example.infinite_track.domain.repository.AuthRefreshException
import com.example.infinite_track.domain.repository.AuthRefreshFailureKind
import com.example.infinite_track.domain.repository.AuthRefreshFailureReason
import com.example.infinite_track.domain.use_case.auth.ForceReauthUseCase
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
    private val forceReauthUseCaseProvider: Provider<ForceReauthUseCase>,
    private val authErrorBodyParser: AuthErrorBodyParser = AuthErrorBodyParser()
) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val originalRequest = chain.request()
        val isBootstrapAuthRequest = originalRequest.header(AuthHeaderNames.BOOTSTRAP_AUTH_REQUEST) == AuthHeaderNames.BOOTSTRAP_AUTH_REQUEST_VALUE
        val token = runBlocking { userPreference.getAuthToken().first() }

        val requestWithToken = originalRequest.newBuilder()
            .removeHeader(AuthHeaderNames.BOOTSTRAP_AUTH_REQUEST)
            .header(AuthHeaderNames.CLIENT_TYPE, AuthHeaderNames.CLIENT_TYPE_MOBILE)
            .apply {
                if (!token.isNullOrBlank() && !AuthEndpointMatcher.isAuthEndpoint(originalRequest)) {
                    header(AuthHeaderNames.AUTHORIZATION, "Bearer $token")
                }
            }
            .build()

        val response = chain.proceed(requestWithToken)

        if (response.code != HTTP_UNAUTHORIZED) {
            return response
        }

        if (AuthEndpointMatcher.isAuthEndpoint(requestWithToken)) {
            return response
        }

        val authCode = authErrorBodyParser.parseAuthCode(response)
        forcedReauthReasonFor(authCode)?.let { reason ->
            triggerForcedReauth(reason, suppressGlobalHandling = isBootstrapAuthRequest)
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
                    .removeHeader(AuthHeaderNames.BOOTSTRAP_AUTH_REQUEST)
                    .header(AuthHeaderNames.CLIENT_TYPE, AuthHeaderNames.CLIENT_TYPE_MOBILE)
                    .header(AuthHeaderNames.RETRY_MARKER, AuthHeaderNames.RETRY_MARKER_VALUE)
                    .apply {
                        removeHeader(AuthHeaderNames.AUTHORIZATION)
                        if (!newToken.isNullOrBlank()) {
                            header(AuthHeaderNames.AUTHORIZATION, "Bearer $newToken")
                        }
                    }
                    .build()

                chain.proceed(retriedRequest)
            },
            onFailure = { throwable ->
                val refreshException = throwable as? AuthRefreshException
                if (refreshException?.kind == AuthRefreshFailureKind.NON_REFRESHABLE) {
                    triggerForcedReauth(
                        reason = reauthReasonFor(refreshException.reason),
                        suppressGlobalHandling = isBootstrapAuthRequest
                    )
                }
                response
            }
        )
    }

    private fun shouldAttemptRefresh(request: Request, authCode: String?): Boolean {
        if (request.header(AuthHeaderNames.RETRY_MARKER) == AuthHeaderNames.RETRY_MARKER_VALUE) {
            return false
        }

        if (AuthEndpointMatcher.isAuthPath(request.url.encodedPath)) {
            return false
        }

        val hasBearerToken = request.header(AuthHeaderNames.AUTHORIZATION)?.startsWith("Bearer ") == true
        return hasBearerToken && (authCode == null || authCode == AUTH_ACCESS_TOKEN_EXPIRED)
    }

    private fun triggerForcedReauth(
        reason: ReauthReason,
        suppressGlobalHandling: Boolean
    ) {
        if (suppressGlobalHandling) {
            return
        }

        runBlocking {
            forceReauthUseCaseProvider.get().invoke(reason)
        }
    }

    private fun forcedReauthReasonFor(authCode: String?): ReauthReason? {
        return when (authCode) {
            AUTH_SESSION_INACTIVE -> ReauthReason.INACTIVITY_EXPIRED
            AUTH_REFRESH_TOKEN_INVALID -> ReauthReason.REFRESH_INVALID
            AUTH_REFRESH_TOKEN_REVOKED -> ReauthReason.REFRESH_REVOKED
            else -> null
        }
    }

    private fun reauthReasonFor(reason: AuthRefreshFailureReason): ReauthReason {
        return when (reason) {
            AuthRefreshFailureReason.INACTIVITY_EXPIRED -> ReauthReason.INACTIVITY_EXPIRED
            AuthRefreshFailureReason.REFRESH_INVALID -> ReauthReason.REFRESH_INVALID
            AuthRefreshFailureReason.REFRESH_REVOKED -> ReauthReason.REFRESH_REVOKED
            else -> ReauthReason.UNKNOWN
        }
    }

    companion object {
        private const val HTTP_UNAUTHORIZED = 401
        private const val AUTH_ACCESS_TOKEN_EXPIRED = "AUTH_ACCESS_TOKEN_EXPIRED"
        private const val AUTH_REFRESH_TOKEN_INVALID = "AUTH_REFRESH_TOKEN_INVALID"
        private const val AUTH_REFRESH_TOKEN_REVOKED = "AUTH_REFRESH_TOKEN_REVOKED"
        private const val AUTH_SESSION_INACTIVE = "AUTH_SESSION_INACTIVE"
    }
}
