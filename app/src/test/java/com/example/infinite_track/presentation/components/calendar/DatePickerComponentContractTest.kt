package com.example.infinite_track.presentation.components.calendar

import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class DatePickerComponentContractTest {

    @Test
    fun `shared date picker exposes controlled LocalDate API for feature screens`() {
        val hasControlledLocalDateOverload = Class
            .forName("com.example.infinite_track.presentation.components.calendar.CalendarKt")
            .declaredMethods
            .filter { it.name.startsWith("DatePickerComponent-") }
            .any { method ->
                method.parameterTypes.count { it == LocalDate::class.java } >= 2
            }

        assertTrue(hasControlledLocalDateOverload)
    }
}
