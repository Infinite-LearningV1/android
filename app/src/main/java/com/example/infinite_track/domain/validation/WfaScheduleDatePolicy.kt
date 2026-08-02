package com.example.infinite_track.domain.validation

import java.time.Clock
import java.time.LocalDate
import java.time.ZoneId
import javax.inject.Inject

class WfaScheduleDatePolicy private constructor(
    private val clock: Clock
) {
    @Inject
    constructor() : this(Clock.system(JAKARTA_ZONE))

    fun today(): LocalDate = LocalDate.now(clock.withZone(JAKARTA_ZONE))

    fun minimumDate(today: LocalDate = today()): LocalDate = today.plusDays(1)

    fun isSelectable(
        date: LocalDate,
        today: LocalDate = today()
    ): Boolean = date.isAfter(today)

    companion object {
        val JAKARTA_ZONE: ZoneId = ZoneId.of("Asia/Jakarta")

        fun fixed(clock: Clock): WfaScheduleDatePolicy = WfaScheduleDatePolicy(clock)
    }
}
