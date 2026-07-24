package com.example.infinite_track.presentation.screen.attendance

import org.junit.Assert.assertTrue
import org.junit.Test
import java.lang.reflect.Modifier

class AttendancePreparationStateOwnershipTest {

    @Test
    fun `forbidden property scan detects a computed getter`() {
        assertTrue(
            forbiddenExposures(
                type = ComputedGetterFixture::class.java,
                forbiddenNames = setOf("targetLocation")
            ) == setOf("targetLocation")
        )
    }

    @Test
    fun `screen state has one preparation owner`() {
        val forbiddenNames = setOf(
            "targetLocation",
            "wfoLocation",
            "wfhLocation",
            "approvedWfaLocation",
            "selectedTargetLocation",
            "targetLocationMarker",
            "selectedWfaLocation",
            "selectedWfaMarkerInfo",
            "pickedLocation",
            "buttonText",
            "isButtonEnabled",
            "isCheckInMode"
        )

        assertTrue(exposesProperty(AttendanceScreenState::class.java, "preparation"))
        assertTrue(forbiddenExposures(AttendanceScreenState::class.java, forbiddenNames).isEmpty())
    }

    private class ComputedGetterFixture {
        val targetLocation: String
            get() = "derived"
    }

    private fun forbiddenExposures(
        type: Class<*>,
        forbiddenNames: Set<String>
    ): Set<String> = forbiddenNames.filterTo(mutableSetOf()) { name ->
        exposesProperty(type, name)
    }

    private fun exposesProperty(type: Class<*>, propertyName: String): Boolean {
        if (type.declaredFields.any { it.name == propertyName }) return true

        val capitalizedName = propertyName.replaceFirstChar { it.uppercaseChar() }
        val publicMethodNames = setOf(
            propertyName,
            "get$capitalizedName",
            "is$capitalizedName"
        )
        return type.methods.any { method ->
            Modifier.isPublic(method.modifiers) &&
                method.parameterCount == 0 &&
                method.name in publicMethodNames
        }
    }
}
