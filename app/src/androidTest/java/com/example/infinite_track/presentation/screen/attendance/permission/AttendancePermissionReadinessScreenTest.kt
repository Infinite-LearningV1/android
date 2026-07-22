package com.example.infinite_track.presentation.screen.attendance.permission

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertHeightIsAtLeast
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.infinite_track.domain.model.attendance.permission.AttendanceAccess
import com.example.infinite_track.presentation.design.tokens.InfiniteSemantic
import com.example.infinite_track.presentation.theme.Infinite_TrackTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AttendancePermissionReadinessScreenTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun defaultLoadingRendersOneRequiredProgressSummary() {
        render(AttendancePermissionReadinessUiState())

        composeRule.onNodeWithText("Memeriksa kesiapan akses").assertIsDisplayed()
        composeRule.onAllNodesWithText("0/3 akses wajib siap").assertCountEquals(1)
    }

    @Test
    fun readinessVariantsRenderTypedRequiredAndOptionalStates() {
        val variants = listOf(
            partialState() to "Perlu diatur",
            readyState(optionalReady = false) to "Terbatas",
            readyState(optionalReady = true) to "Siap",
            partialState(firstStatus = "Izin ditolak", firstSemantic = InfiniteSemantic.Warning) to "Izin ditolak",
            partialState(firstStatus = "Izin diblokir", firstSemantic = InfiniteSemantic.Error, firstAction = "Buka pengaturan") to "Izin diblokir",
            partialState(optionalStatus = "Terbatas") to "Terbatas",
            partialState(optionalStatus = "Tidak diperlukan di perangkat ini", optionalAction = null, optionalReady = true) to "Tidak diperlukan di perangkat ini",
            partialState(deviceStatus = "GPS belum aktif", deviceAction = "Buka pengaturan") to "GPS belum aktif",
            partialState(
                recoverableFailure = PermissionGuidanceUiModel(
                    title = "Status akses belum dapat diperiksa",
                    message = "Periksa kembali status akses sebelum melanjutkan absensi.",
                    semantic = InfiniteSemantic.Error,
                    actionLabel = "Coba lagi",
                    action = PermissionGuidanceAction.RETRY_REFRESH
                )
            ) to "Status akses belum dapat diperiksa"
        )

        val currentState = mutableStateOf(variants.first().first)
        composeRule.setContent {
            Infinite_TrackTheme {
                AttendancePermissionReadinessScreen(
                    uiState = currentState.value,
                    onEvent = {},
                    onBackClick = {}
                )
            }
        }

        variants.forEach { (state, expectedCopy) ->
            composeRule.runOnIdle { currentState.value = state }
            composeRule.waitForIdle()
            composeRule.onAllNodesWithText(expectedCopy, substring = true)[0].assertExists()
        }
    }

    @Test
    fun requiredProgressIgnoresOptionalRowsAndAppearsExactlyOnce() {
        render(partialState(requiredReadyCount = 2))

        composeRule.onAllNodesWithText("2/3 akses wajib siap").assertCountEquals(1)
        composeRule.onNodeWithText("Akses wajib").assertIsDisplayed()
        composeRule.onNodeWithText("Pengingat opsional").assertIsDisplayed()
    }

    @Test
    fun retryRowAndPrimaryActionsEmitTypedEvents() {
        val events = mutableListOf<AttendancePermissionReadinessEvent>()
        render(
            partialState(
                recoverableFailure = PermissionGuidanceUiModel(
                    title = "Status akses belum dapat diperiksa",
                    message = "Periksa kembali status akses sebelum melanjutkan absensi.",
                    semantic = InfiniteSemantic.Error,
                    actionLabel = "Coba lagi",
                    action = PermissionGuidanceAction.RETRY_REFRESH
                )
            ),
            onEvent = events::add
        )

        composeRule.onAllNodesWithText("Minta izin")[0].performScrollTo().performClick()
        composeRule.onNodeWithText("Coba lagi").performClick()
        composeRule.onNodeWithText("Lanjutkan Setup").performScrollTo().performClick()

        assertEquals(
            listOf(
                AttendancePermissionReadinessEvent.PermissionItemClicked(AttendanceAccess.PRECISE_LOCATION),
                AttendancePermissionReadinessEvent.RetryRefresh,
                AttendancePermissionReadinessEvent.PrimaryActionClicked
            ),
            events
        )
    }

    @Test
    fun rowSemanticsAnnounceRequirementStateAndAction() {
        val item = permissionItem(
            access = AttendanceAccess.PRECISE_LOCATION,
            status = "Lokasi presisi diperlukan",
            action = "Minta izin",
            semantic = InfiniteSemantic.Warning
        )
        render(partialState(requiredItems = listOf(item) + readyRequiredItems().drop(1)))

        composeRule.onNodeWithContentDescription(item.title)
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.StateDescription, item.stateDescription))
            .assertIsDisplayed()
    }

    @Test
    fun width320AndFontScaleTwoKeepRowAndPrimaryActionsUsable() {
        composeRule.setContent {
            CompositionLocalProvider(LocalDensity provides Density(1f, 2f)) {
                Infinite_TrackTheme {
                    Box(
                        Modifier
                            .width(320.dp)
                            .fillMaxSize()
                            .testTag("screenHost")
                    ) {
                        AttendancePermissionReadinessScreen(
                            uiState = partialState(),
                            onEvent = {},
                            onBackClick = {}
                        )
                    }
                }
            }
        }

        val rowAction = composeRule.onAllNodesWithText("Minta izin")[0]
            .performScrollTo()
            .assertHeightIsAtLeast(48.dp)
            .assertIsDisplayed()
        val actionBounds = rowAction.getUnclippedBoundsInRoot()
        val hostBounds = composeRule.onNodeWithTag("screenHost").getUnclippedBoundsInRoot()
        assertTrue(actionBounds.left >= hostBounds.left && actionBounds.right <= hostBounds.right)

        composeRule.onNodeWithText("Lanjutkan Setup")
            .performScrollTo()
            .assertHeightIsAtLeast(48.dp)
            .assertIsDisplayed()
    }

    private fun render(
        state: AttendancePermissionReadinessUiState,
        onEvent: (AttendancePermissionReadinessEvent) -> Unit = {}
    ) {
        composeRule.setContent {
            Infinite_TrackTheme {
                AttendancePermissionReadinessScreen(
                    uiState = state,
                    onEvent = onEvent,
                    onBackClick = {}
                )
            }
        }
    }

    private fun partialState(
        requiredReadyCount: Int = 0,
        firstStatus: String = "Perlu diatur",
        firstSemantic: InfiniteSemantic = InfiniteSemantic.Primary,
        firstAction: String? = "Minta izin",
        deviceStatus: String = "Perlu diatur",
        deviceAction: String? = "Buka pengaturan",
        optionalStatus: String = "Terbatas",
        optionalAction: String? = "Aktifkan",
        optionalReady: Boolean = false,
        requiredItems: List<PermissionItemUiModel> = listOf(
            permissionItem(AttendanceAccess.PRECISE_LOCATION, firstStatus, firstAction, firstSemantic),
            permissionItem(AttendanceAccess.CAMERA, "Perlu diatur", "Minta izin", InfiniteSemantic.Primary),
            permissionItem(AttendanceAccess.DEVICE_LOCATION, deviceStatus, deviceAction, InfiniteSemantic.Warning)
        ),
        recoverableFailure: PermissionGuidanceUiModel? = null
    ) = AttendancePermissionReadinessUiState(
        isLoading = false,
        requiredItems = requiredItems,
        optionalItems = listOf(
            permissionItem(AttendanceAccess.NOTIFICATION, optionalStatus, optionalAction, InfiniteSemantic.Warning, optionalReady),
            permissionItem(AttendanceAccess.BACKGROUND_LOCATION, optionalStatus, "Atur akses", InfiniteSemantic.Warning, optionalReady)
        ),
        requiredReadyCount = requiredReadyCount,
        requiredTotalCount = 3,
        primaryActionLabel = "Lanjutkan Setup",
        primaryActionEnabled = true,
        recoverableFailure = recoverableFailure
    )

    private fun readyState(optionalReady: Boolean): AttendancePermissionReadinessUiState =
        AttendancePermissionReadinessUiState(
            isLoading = false,
            requiredItems = readyRequiredItems(),
            optionalItems = listOf(
                permissionItem(AttendanceAccess.NOTIFICATION, if (optionalReady) "Siap" else "Terbatas", if (optionalReady) null else "Aktifkan", if (optionalReady) InfiniteSemantic.Success else InfiniteSemantic.Warning, optionalReady),
                permissionItem(AttendanceAccess.BACKGROUND_LOCATION, if (optionalReady) "Siap" else "Terbatas", if (optionalReady) null else "Atur akses", if (optionalReady) InfiniteSemantic.Success else InfiniteSemantic.Warning, optionalReady)
            ),
            requiredReadyCount = 3,
            requiredTotalCount = 3,
            canContinue = true,
            primaryActionLabel = "Lanjut ke Mode Kerja",
            primaryActionEnabled = true
        )

    private fun readyRequiredItems() = listOf(
        permissionItem(AttendanceAccess.PRECISE_LOCATION, "Siap", null, InfiniteSemantic.Success, true),
        permissionItem(AttendanceAccess.CAMERA, "Siap", null, InfiniteSemantic.Success, true),
        permissionItem(AttendanceAccess.DEVICE_LOCATION, "Siap", null, InfiniteSemantic.Success, true)
    )

    private fun permissionItem(
        access: AttendanceAccess,
        status: String,
        action: String?,
        semantic: InfiniteSemantic,
        ready: Boolean = false
    ): PermissionItemUiModel {
        val title = when (access) {
            AttendanceAccess.PRECISE_LOCATION -> "Lokasi presisi"
            AttendanceAccess.CAMERA -> "Kamera"
            AttendanceAccess.DEVICE_LOCATION -> "Lokasi perangkat aktif"
            AttendanceAccess.NOTIFICATION -> "Notifikasi pengingat"
            AttendanceAccess.BACKGROUND_LOCATION -> "Lokasi latar belakang"
        }
        val requirement = if (access in setOf(
                AttendanceAccess.PRECISE_LOCATION,
                AttendanceAccess.CAMERA,
                AttendanceAccess.DEVICE_LOCATION
            )
        ) "Wajib" else "Opsional"
        return PermissionItemUiModel(
            access = access,
            title = title,
            supportingText = "Deskripsi $title",
            requirementLabel = requirement,
            statusLabel = status,
            actionLabel = action,
            iconKey = when (access) {
                AttendanceAccess.PRECISE_LOCATION -> PermissionIconKey.LOCATION
                AttendanceAccess.CAMERA -> PermissionIconKey.CAMERA
                AttendanceAccess.DEVICE_LOCATION -> PermissionIconKey.DEVICE_LOCATION
                AttendanceAccess.NOTIFICATION -> PermissionIconKey.NOTIFICATION
                AttendanceAccess.BACKGROUND_LOCATION -> PermissionIconKey.BACKGROUND_LOCATION
            },
            semantic = semantic,
            stateDescription = "$title, ${requirement.lowercase()}, $status${action?.let { ", aksi $it" }.orEmpty()}",
            isReady = ready
        )
    }
}
