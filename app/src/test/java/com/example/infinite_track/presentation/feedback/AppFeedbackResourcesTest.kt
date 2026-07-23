package com.example.infinite_track.presentation.feedback

import java.io.File
import java.io.StringReader
import javax.xml.parsers.DocumentBuilderFactory
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.xml.sax.InputSource

class AppFeedbackResourcesTest {

    @Test
    fun `default and Indonesian resources define every app feedback key`() {
        listOf("values", "values-in").forEach { directory ->
            val file = resourceFile(directory)

            assertTrue("Missing resource file: ${file.path}", file.isFile)
            val declaredNames = declaredStringNames(file.readText())

            assertEquals(
                "Missing app feedback resources in ${file.path}",
                emptySet<String>(),
                REQUIRED_APP_FEEDBACK_KEYS - declaredNames
            )
        }
    }

    @Test
    fun `resource parser ignores string tags inside XML comments`() {
        val xml = """
            <resources>
                <!-- <string name="commented_out">Fallback only</string> -->
                <string name="active">Active</string>
            </resources>
        """.trimIndent()

        assertEquals(setOf("active"), declaredStringNames(xml))
    }

    @Test
    fun `Indonesian logout success title uses the precise localized copy`() {
        assertEquals(
            "Berhasil keluar dari perangkat ini",
            declaredStringValues(resourceFile("values-in"))["app_feedback_logout_success_title"]
        )
    }

    private fun declaredStringNames(xml: String): Set<String> {
        return declaredStringValues(xml).keys
    }

    private fun declaredStringValues(file: File): Map<String, String> =
        declaredStringValues(file.readText())

    private fun declaredStringValues(xml: String): Map<String, String> {
        val factory = DocumentBuilderFactory.newInstance().apply {
            setFeature("http://apache.org/xml/features/disallow-doctype-decl", true)
            setFeature("http://xml.org/sax/features/external-general-entities", false)
            setFeature("http://xml.org/sax/features/external-parameter-entities", false)
            isXIncludeAware = false
            isExpandEntityReferences = false
        }
        val stringNodes = factory.newDocumentBuilder()
            .parse(InputSource(StringReader(xml)))
            .getElementsByTagName("string")

        return buildMap {
            (0 until stringNodes.length).forEach { index ->
                val node = stringNodes.item(index)
                val name = node.attributes?.getNamedItem("name")?.nodeValue
                if (name != null) {
                    put(name, node.textContent)
                }
            }
        }
    }

    private fun resourceFile(directory: String): File {
        val moduleRelative = File("src/main/res/$directory/strings.xml")
        if (moduleRelative.exists()) return moduleRelative
        return File("app/src/main/res/$directory/strings.xml")
    }

    private companion object {
        val REQUIRED_APP_FEEDBACK_KEYS = setOf(
            "app_feedback_login_success_title",
            "app_feedback_login_success_message",
            "app_feedback_logout_success_title",
            "app_feedback_logout_success_message",
            "app_feedback_logout_remote_warning_title",
            "app_feedback_logout_remote_warning_message"
        )
    }
}
