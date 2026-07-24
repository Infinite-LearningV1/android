package com.example.infinite_track.presentation.components.button.attendance

import java.io.File
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class WorkModeSelectorTokenContractTest {
    @Test
    fun `work mode option visuals consume the central work mode color mapping`() {
        val source = File(
            requireNotNull(System.getProperty("user.dir")),
            "src/main/java/com/example/infinite_track/presentation/components/button/attendance/WorkModeSelector.kt"
        ).readText()

        assertTrue(source.contains("WorkModeVisualTokens.color(option.mode)"))
        assertFalse(source.contains("semanticColors.accent"))
    }
}
