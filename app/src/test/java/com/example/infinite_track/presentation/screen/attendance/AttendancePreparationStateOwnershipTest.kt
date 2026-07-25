package com.example.infinite_track.presentation.screen.attendance

import java.io.File
import org.junit.Assert.assertTrue
import org.junit.Test
import java.lang.reflect.Modifier

class AttendancePreparationStateOwnershipTest {

    private val appModuleRoot = File(requireNotNull(System.getProperty("user.dir")))


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

    @Test
    fun `attendance view model has no legacy geofence runtime ownership`() {
        val forbiddenDependencyNames = setOf(
            "GeofenceManager",
            "AttendancePreference",
            "ReminderGeofenceCandidate",
            "GeofencingClient"
        )
        val constructorDependencies = AttendanceViewModel::class.java
            .declaredConstructors
            .single()
            .parameterTypes
            .map { it.name }

        assertTrue(
            constructorDependencies.none { dependency ->
                forbiddenDependencyNames.any(dependency::contains)
            }
        )

        val imports = File(
            appModuleRoot,
            "src/main/java/com/example/infinite_track/presentation/screen/attendance/AttendanceViewModel.kt"
        ).readLines().filter { it.trimStart().startsWith("import ") }

        assertTrue(
            imports.none { imported ->
                forbiddenDependencyNames.any(imported::contains)
            }
        )
    }

    @Test
    fun `auth unavailable leaves attendance local runtime projection to global reauth`() {
        val source = File(
            appModuleRoot,
            "src/main/java/com/example/infinite_track/presentation/screen/attendance/AttendanceViewModel.kt"
        ).readText()
        val authUnavailableBranch = source.substringAfter(
            "is RefreshAndReconcileGeofenceRuntimeResult.AuthUnavailable -> {"
        ).substringBefore("                }")

        assertTrue(authUnavailableBranch.isNotBlank())
        assertTrue(!authUnavailableBranch.contains("_uiState"))
        assertTrue(!authUnavailableBranch.contains("geofenceRuntime"))
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
