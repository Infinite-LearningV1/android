package com.example.infinite_track.data.mapper.attendance

import com.example.infinite_track.domain.model.attendance.AttendanceSubmitFailure
import org.junit.Assert.assertEquals
import org.junit.Test
import java.io.IOException
import java.net.SocketTimeoutException

class AttendanceApiFailureMapperTest {

    @Test
    fun `already checked out message maps to AlreadyCheckedOut`() {
        assertEquals(
            AttendanceSubmitFailure.AlreadyCheckedOut,
            AttendanceApiFailureMapper.mapHttp(400, "Anda sudah melakukan check-out hari ini")
        )
    }

    @Test
    fun `duplicate check-in message maps to DuplicateAttendance`() {
        assertEquals(
            AttendanceSubmitFailure.DuplicateAttendance,
            AttendanceApiFailureMapper.mapHttp(400, "Anda sudah melakukan check-in hari ini")
        )
    }

    @Test
    fun `outside radius message maps to OutsideAllowedRadius`() {
        assertEquals(
            AttendanceSubmitFailure.OutsideAllowedRadius,
            AttendanceApiFailureMapper.mapHttp(
                400,
                "Anda berada di luar radius lokasi yang diizinkan untuk check-out"
            )
        )
    }

    @Test
    fun `booking rejection message maps to WfaBookingRejected`() {
        assertEquals(
            AttendanceSubmitFailure.WfaBookingRejected,
            AttendanceApiFailureMapper.mapHttp(400, "Booking WFA Anda belum disetujui")
        )
    }

    @Test
    fun `booking required message maps to WfaBookingRequired`() {
        assertEquals(
            AttendanceSubmitFailure.WfaBookingRequired,
            AttendanceApiFailureMapper.mapHttp(400, "Booking wajib untuk check-in WFA")
        )
    }

    @Test
    fun `5xx maps to ServerUnavailable regardless of message`() {
        assertEquals(
            AttendanceSubmitFailure.ServerUnavailable,
            AttendanceApiFailureMapper.mapHttp(503, "Internal error detail that must not leak")
        )
    }

    @Test
    fun `unrecognized 4xx with message maps to BackendRejected carrying only the message`() {
        val failure = AttendanceApiFailureMapper.mapHttp(422, "Absensi ditolak kebijakan kantor")
        assertEquals(
            AttendanceSubmitFailure.BackendRejected("Absensi ditolak kebijakan kantor"),
            failure
        )
    }

    @Test
    fun `unrecognized 4xx without message maps to Unknown`() {
        assertEquals(AttendanceSubmitFailure.Unknown, AttendanceApiFailureMapper.mapHttp(400, null))
        assertEquals(AttendanceSubmitFailure.Unknown, AttendanceApiFailureMapper.mapHttp(400, "  "))
    }

    @Test
    fun `IOException maps to NetworkUnavailable`() {
        assertEquals(
            AttendanceSubmitFailure.NetworkUnavailable,
            AttendanceApiFailureMapper.mapThrowable(SocketTimeoutException("timeout"))
        )
        assertEquals(
            AttendanceSubmitFailure.NetworkUnavailable,
            AttendanceApiFailureMapper.mapThrowable(IOException("no route"))
        )
    }

    @Test
    fun `non-IO throwable maps to Unknown`() {
        assertEquals(
            AttendanceSubmitFailure.Unknown,
            AttendanceApiFailureMapper.mapThrowable(IllegalStateException("boom"))
        )
    }
}
