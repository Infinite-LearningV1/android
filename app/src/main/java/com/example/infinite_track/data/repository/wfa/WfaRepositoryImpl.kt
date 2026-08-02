package com.example.infinite_track.data.repository.wfa

import com.example.infinite_track.data.mapper.wfa.WfaRecommendationFailureMapper
import com.example.infinite_track.data.mapper.wfa.toDomainResultOrNull
import com.example.infinite_track.data.soucre.network.response.booking.WfaRequestErrorResponseDto
import com.example.infinite_track.data.soucre.network.retrofit.ApiService
import com.example.infinite_track.domain.model.wfa.WfaRecommendationFailure
import com.example.infinite_track.domain.model.wfa.WfaRecommendationQuery
import com.example.infinite_track.domain.model.wfa.WfaRecommendationResult
import com.example.infinite_track.domain.repository.WfaRepository
import com.google.gson.Gson
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import retrofit2.HttpException
import java.time.format.DateTimeFormatter
import javax.inject.Inject

class WfaRepositoryImpl @Inject constructor(
    private val apiService: ApiService,
    private val gson: Gson
) : WfaRepository {

    override suspend fun getRecommendations(
        query: WfaRecommendationQuery
    ): WfaRecommendationResult = withContext(Dispatchers.IO) {
        try {
            apiService.getWfaRecommendations(
                latitude = query.origin.latitude,
                longitude = query.origin.longitude,
                scheduleDate = query.scheduleDate.format(DateTimeFormatter.ISO_LOCAL_DATE)
            ).toDomainResultOrNull()
                ?: WfaRecommendationResult.Failure(WfaRecommendationFailure.Unknown)
        } catch (exception: CancellationException) {
            throw exception
        } catch (exception: HttpException) {
            WfaRecommendationResult.Failure(parseHttpFailure(exception))
        } catch (throwable: Throwable) {
            WfaRecommendationResult.Failure(
                WfaRecommendationFailureMapper.mapThrowable(throwable)
            )
        }
    }

    private fun parseHttpFailure(exception: HttpException): WfaRecommendationFailure {
        val parsed = runCatching {
            exception.response()?.errorBody()?.string()?.let { body ->
                gson.fromJson(body, WfaRequestErrorResponseDto::class.java)
            }
        }.getOrNull()
        val code = parsed?.code ?: parsed?.errors?.firstNotNullOfOrNull { it.code }
        return WfaRecommendationFailureMapper.mapHttp(exception.code(), code)
    }
}
