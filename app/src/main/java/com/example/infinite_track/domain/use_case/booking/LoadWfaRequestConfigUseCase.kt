package com.example.infinite_track.domain.use_case.booking

import com.example.infinite_track.domain.model.booking.WfaRequestConfigResult
import com.example.infinite_track.domain.model.booking.WfaRequestFailure
import com.example.infinite_track.domain.repository.BookingRepository
import javax.inject.Inject

class LoadWfaRequestConfigUseCase @Inject constructor(
    private val repository: BookingRepository
) {
    suspend operator fun invoke(): WfaRequestConfigResult =
        when (val result = repository.getWfaRequestConfig()) {
            is WfaRequestConfigResult.Success -> {
                val config = result.config
                if (config.radiusMeters > 0 && config.reasons.isNotEmpty()) {
                    result
                } else {
                    WfaRequestConfigResult.Failure(WfaRequestFailure.ConfigUnavailable)
                }
            }
            is WfaRequestConfigResult.Failure -> result
        }
}
