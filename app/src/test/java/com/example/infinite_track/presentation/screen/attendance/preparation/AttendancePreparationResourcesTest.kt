package com.example.infinite_track.presentation.screen.attendance.preparation

import java.io.File
import java.io.StringReader
import javax.xml.parsers.DocumentBuilderFactory
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.xml.sax.InputSource

class AttendancePreparationResourcesTest {

    @Test
    fun `default and Indonesian resources define every attendance preparation key`() {
        listOf("values", "values-in").forEach { directory ->
            val file = resourceFile(directory)
            assertTrue("Missing resource file: ${file.path}", file.isFile)
            assertEquals(
                "Missing attendance preparation resources in ${file.path}",
                emptySet<String>(),
                REQUIRED_KEYS - declaredStringValues(file).keys
            )
        }
    }

    @Test
    fun `default attendance preparation copy is English and Indonesian copy is localized`() {
        assertEquals(
            "Choose Work Mode",
            declaredStringValues(resourceFile("values"))["attendance_work_mode_heading"]
        )
        assertEquals(
            "Pilih Mode Kerja",
            declaredStringValues(resourceFile("values-in"))["attendance_work_mode_heading"]
        )
    }

    private fun declaredStringValues(file: File): Map<String, String> {
        val factory = DocumentBuilderFactory.newInstance().apply {
            setFeature("http://apache.org/xml/features/disallow-doctype-decl", true)
            setFeature("http://xml.org/sax/features/external-general-entities", false)
            setFeature("http://xml.org/sax/features/external-parameter-entities", false)
            isXIncludeAware = false
            isExpandEntityReferences = false
        }
        val nodes = factory.newDocumentBuilder()
            .parse(InputSource(StringReader(file.readText())))
            .getElementsByTagName("string")
        return buildMap {
            (0 until nodes.length).forEach { index ->
                val node = nodes.item(index)
                val name = node.attributes?.getNamedItem("name")?.nodeValue
                if (name != null) put(name, node.textContent)
            }
        }
    }

    private fun resourceFile(directory: String): File {
        val moduleRelative = File("src/main/res/$directory/strings.xml")
        if (moduleRelative.exists()) return moduleRelative
        return File("app/src/main/res/$directory/strings.xml")
    }

    private companion object {
        val REQUIRED_KEYS = setOf(
            "attendance_work_mode_heading",
            "attendance_work_mode_supporting",
            "attendance_wfo_supporting",
            "attendance_wfh_supporting",
            "attendance_wfa_supporting",
            "attendance_target_heading",
            "attendance_target_source_status_today",
            "attendance_target_source_admin_profile",
            "attendance_target_source_approved_wfa",
            "attendance_target_booking_status_approved",
            "attendance_target_booking_date",
            "attendance_range_inside",
            "attendance_range_outside",
            "attendance_range_unavailable",
            "attendance_range_stale"
        )
    }
}
