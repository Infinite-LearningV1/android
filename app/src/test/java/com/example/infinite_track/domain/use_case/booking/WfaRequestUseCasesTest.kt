package com.example.infinite_track.domain.use_case.booking

import com.example.infinite_track.domain.model.booking.BookingHistoryPage
import com.example.infinite_track.domain.model.booking.SubmitWfaRequestCommand
import com.example.infinite_track.domain.model.booking.WfaCandidateLocation
import com.example.infinite_track.domain.model.booking.WfaRequestConfig
import com.example.infinite_track.domain.model.booking.WfaRequestConfigResult
import com.example.infinite_track.domain.model.booking.WfaRequestFailure
import com.example.infinite_track.domain.model.booking.WfaRequestReason
import com.example.infinite_track.domain.model.booking.WfaRequestResult
import com.example.infinite_track.domain.repository.BookingRepository
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Test
import java.time.LocalDate

class WfaRequestUseCasesTest {

    @Test
    fun `load returns valid config unchanged`() = runTest {
        val expected = WfaRequestConfigResult.Success(
            WfaRequestConfig(100, listOf(WfaRequestReason(1L, "Client meeting", false)))
        )
        val repository = WfaRequestRepositoryFake(configResult = expected)

        assertSame(expected, LoadWfaRequestConfigUseCase(repository)())
    }

    @Test
    fun `load rejects unusable config`() = runTest {
        val repository = WfaRequestRepositoryFake(
            configResult = WfaRequestConfigResult.Success(WfaRequestConfig(0, emptyList()))
        )

        val result = LoadWfaRequestConfigUseCase(repository)()

        assertEquals(
            WfaRequestFailure.ConfigUnavailable,
            (result as WfaRequestConfigResult.Failure).failure
        )
    }

    @Test
    fun `submit delegates once with same command and returns typed failure`() = runTest {
        val failure = WfaRequestResult.Failure(WfaRequestFailure.NetworkUnavailable)
        val repository = WfaRequestRepositoryFake(submitResult = failure)
        val command = SubmitWfaRequestCommand(
            scheduleDate = LocalDate.of(2026, 8, 10),
            reasonId = 1L,
            otherReasonText = null,
            notes = null,
            location = WfaCandidateLocation(-0.9001, 119.877, "Hub", "Palu")
        )

        val result = SubmitWfaRequestUseCase(repository)(command)

        assertSame(failure, result)
        assertEquals(listOf(command), repository.submittedCommands)
    }
}

private class WfaRequestRepositoryFake(
    private val configResult: WfaRequestConfigResult = WfaRequestConfigResult.Failure(
        WfaRequestFailure.ConfigUnavailable
    ),
    private val submitResult: WfaRequestResult = WfaRequestResult.Failure(WfaRequestFailure.Unknown)
) : BookingRepository {
    val submittedCommands = mutableListOf<SubmitWfaRequestCommand>()

    override suspend fun getWfaRequestConfig(): WfaRequestConfigResult = configResult

    override suspend fun submitWfaRequest(command: SubmitWfaRequestCommand): WfaRequestResult {
        submittedCommands += command
        return submitResult
    }

    override suspend fun getBookingHistory(
        status: String?,
        page: Int,
        limit: Int,
        sortBy: String,
        sortOrder: String
    ): Result<BookingHistoryPage> = error("Not used")

    override suspend fun submitBooking(
        scheduleDate: String,
        latitude: Double,
        longitude: Double,
        radius: Int,
        description: String,
        notes: String
    ): Result<Unit> = error("Not used")
}
