package com.example.infinite_track.domain.use_case.booking

import com.example.infinite_track.domain.model.booking.SubmitWfaRequestCommand
import com.example.infinite_track.domain.model.booking.WfaRequestResult
import com.example.infinite_track.domain.repository.BookingRepository
import javax.inject.Inject

class SubmitWfaRequestUseCase @Inject constructor(
    private val repository: BookingRepository
) {
    suspend operator fun invoke(command: SubmitWfaRequestCommand): WfaRequestResult =
        repository.submitWfaRequest(command)
}
