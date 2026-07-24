package com.example.infinite_track.presentation.design.tokens

import com.example.infinite_track.domain.model.attendance.WorkMode
import com.example.infinite_track.presentation.theme.Blue_500
import com.example.infinite_track.presentation.theme.Blue_Accent_500
import com.example.infinite_track.presentation.theme.Orange_500
import org.junit.Assert.assertEquals
import org.junit.Test

class WorkModeVisualTokensTest {
    @Test
    fun `WFO uses existing blue`() =
        assertEquals(Blue_500, WorkModeVisualTokens.color(WorkMode.WFO))

    @Test
    fun `WFH uses existing blue accent`() =
        assertEquals(Blue_Accent_500, WorkModeVisualTokens.color(WorkMode.WFH))

    @Test
    fun `WFA uses existing orange`() =
        assertEquals(Orange_500, WorkModeVisualTokens.color(WorkMode.WFA))
}
