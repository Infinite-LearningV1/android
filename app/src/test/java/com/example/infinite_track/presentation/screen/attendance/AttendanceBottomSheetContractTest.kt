package com.example.infinite_track.presentation.screen.attendance

import java.io.File
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AttendanceBottomSheetContractTest {
    private val appModuleRoot = File(requireNotNull(System.getProperty("user.dir")))

    @Test
    fun `preparation sheet owns search but not WFA recommendation rows`() {
        val source = source(
            "src/main/java/com/example/infinite_track/presentation/components/button/" +
                "attendance/AttendanceBottomSheetContent.kt"
        )

        assertTrue(source.contains("AttendancePreparationEvent.SearchWfaLocation"))
        assertFalse(source.contains("WfaRecommendationSection("))
        assertFalse(source.contains("RecommendationSelected"))
    }

    @Test
    fun `attendance sheet uses topbar surface family without custom gradients`() {
        val source = source(
            "src/main/java/com/example/infinite_track/presentation/screen/attendance/" +
                "AttendanceScreen.kt"
        )

        assertTrue(
            source.contains(
                "sheetContainerColor = InfiniteColors.AttendanceReportGlassSurface"
            )
        )
        assertTrue(source.contains("sheetContentColor = InfiniteColors.Text"))
        assertTrue(source.contains("sheetTonalElevation = 8.dp"))
        assertTrue(source.contains("BottomSheetDefaults.DragHandle("))
        assertTrue(source.contains("InfiniteColors.AttendanceReportGlassBorder"))
        assertFalse(source.contains("Brush.linearGradient("))
        assertFalse(source.contains("Brush.radialGradient("))
        assertFalse(source.contains(".blur("))
    }

    private fun source(path: String): String = appModuleRoot.resolve(path).readText()
}
