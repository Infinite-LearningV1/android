package com.example.infinite_track.presentation.screen.attendance

import com.example.infinite_track.domain.model.auth.UserModel
import com.example.infinite_track.domain.repository.ProfileSyncResult
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

class AttendancePreparationRefreshCoordinatorTest {

    @Test
    fun `status recovery forces server refresh`() = runTest {
        val forceRefreshValues = mutableListOf<Boolean>()
        val coordinator = coordinator(
            fetchStatus = { forceRefreshValues += it },
            refreshProfile = { error("Not used") }
        )

        coordinator.refreshStatus()

        assertEquals(listOf(true), forceRefreshValues)
    }

    @Test
    fun `profile success updates latest profile before re-resolution`() = runTest {
        val user = sampleUser()
        var latestProfile: UserModel? = null
        var profileAtResolution: UserModel? = null
        val coordinator = AttendancePreparationRefreshCoordinator(
            fetchStatus = {},
            requestProfileRefresh = { ProfileSyncResult.Success(user) },
            applyProfile = { latestProfile = it },
            resolveWfh = { profileAtResolution = latestProfile },
            preserveProfileRecovery = { error("Failure recovery must not run") }
        )

        coordinator.refreshProfile()

        assertSame(user, latestProfile)
        assertSame(user, profileAtResolution)
    }

    @Test
    fun `typed profile failures preserve recovery without stale re-resolution`() = runTest {
        val existingProfile = sampleUser()
        listOf(
            ProfileSyncResult.TemporaryFailure(message = "offline"),
            ProfileSyncResult.Unauthorized()
        ).forEach { failure ->
            var latestProfile: UserModel? = existingProfile
            var resolved = false
            var recoveryPreserved = false
            val coordinator = AttendancePreparationRefreshCoordinator(
                fetchStatus = {},
                requestProfileRefresh = { failure },
                applyProfile = { latestProfile = it },
                resolveWfh = { resolved = true },
                preserveProfileRecovery = { recoveryPreserved = true }
            )

            coordinator.refreshProfile()

            assertSame(existingProfile, latestProfile)
            assertTrue(recoveryPreserved)
            assertEquals(false, resolved)
        }
    }

    private fun coordinator(
        fetchStatus: suspend (Boolean) -> Unit,
        refreshProfile: suspend () -> ProfileSyncResult
    ) = AttendancePreparationRefreshCoordinator(
        fetchStatus = fetchStatus,
        requestProfileRefresh = refreshProfile,
        applyProfile = {},
        resolveWfh = {},
        preserveProfileRecovery = {}
    )

    private fun sampleUser() = UserModel(
        id = 7,
        fullName = "Ada",
        email = "ada@example.com",
        roleName = "Employee",
        positionName = null,
        programName = null,
        divisionName = null,
        nipNim = "007",
        phone = null,
        photoUrl = null,
        photoUpdatedAt = null,
        latitude = -0.89,
        longitude = 119.87,
        radius = 100,
        locationDescription = "Rumah",
        locationCategoryName = "WFH"
    )
}
