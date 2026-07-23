package com.example.infinite_track.presentation.screen.attendance.permission

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertContentDescriptionEquals
import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertHasNoClickAction
import androidx.compose.ui.test.assertHeightIsAtLeast
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.junit4.createComposeRule
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
    fun permissionAccessCardExposesStateAndAction() {
        val item = permissionItem(
            AttendanceAccess.CAMERA,
            status = "Perlu diatur",
            action = "Minta izin",
            semantic = InfiniteSemantic.Primary
        )
        composeRule.setContent {
            Infinite_TrackTheme {
                PermissionAccessCard(
                    item = item,
                    onActionClick = {}
                )
            }
        }

        composeRule.onNodeWithTag("permission-access-card-camera")
            .assertContentDescriptionEquals("Kamera")
            .assert(
                SemanticsMatcher.expectValue(
                    SemanticsProperties.StateDescription,
                    "Kamera, wajib, Perlu diatur, aksi Minta izin"
                )
            )
            .assertHasClickAction()
        composeRule.onNodeWithText("Minta izin").assertIsDisplayed()
    }

    @Test
    fun loadingPanelDoesNotRenderPermissionItems() {
        render(AttendancePermissionReadinessUiState())

        composeRule.onNodeWithText("Lokasi presisi").assertDoesNotExist()
        composeRule.onNodeWithText("Akses wajib sudah siap").assertDoesNotExist()
        composeRule.onNodeWithText("Memeriksa kesiapan akses...").assertIsDisplayed()
    }

    @Test
    fun requiredCardsExposeStateAndIncompleteItemsAreActionable() {
        val events = mutableListOf<AttendancePermissionReadinessEvent>()
        render(
            partialState(
                requiredReadyCount = 1,
                requiredItems = listOf(
                    permissionItem(
                        AttendanceAccess.PRECISE_LOCATION,
                        "Siap",
                        null,
                        InfiniteSemantic.Success,
                        true
                    ),
                    permissionItem(
                        AttendanceAccess.CAMERA,
                        "Perlu diatur",
                        "Minta izin",
                        InfiniteSemantic.Primary
                    ),
                    permissionItem(
                        AttendanceAccess.DEVICE_LOCATION,
                        "Perlu diatur",
                        "Buka pengaturan",
                        InfiniteSemantic.Warning
                    )
                )
            ),
            onEvent = events::add
        )

        composeRule.onNodeWithTag("permission-access-card-precise_location")
            .assert(
                SemanticsMatcher.expectValue(
                    SemanticsProperties.StateDescription,
                    "Lokasi presisi, wajib, Siap"
                )
            )
        composeRule.onNodeWithTag("permission-access-card-camera")
            .assertHasClickAction()
            .performClick()
        composeRule.onNodeWithTag("permission-access-card-device_location")
            .assertHasClickAction()
        assertEquals(
            listOf(
                AttendancePermissionReadinessEvent.PermissionItemClicked(
                    AttendanceAccess.CAMERA
                )
            ),
            events
        )
    }

    @Test
    fun futureRequiredStepShowsStatusWithoutUnavailableActionSemantics() {
        val item = permissionItem(
            AttendanceAccess.DEVICE_LOCATION,
            status = "Perlu diatur",
            action = "Buka pengaturan",
            semantic = InfiniteSemantic.Warning
        )
        composeRule.setContent {
            Infinite_TrackTheme {
                PermissionAccessCard(
                    item = item,
                    onActionClick = null
                )
            }
        }

        composeRule.onNodeWithTag("permission-access-card-device_location")
            .assertContentDescriptionEquals("Lokasi perangkat aktif")
            .assert(
                SemanticsMatcher.expectValue(
                    SemanticsProperties.StateDescription,
                    "Lokasi perangkat aktif, wajib, Perlu diatur, aksi Buka pengaturan"
                )
            )
            .assertHasNoClickAction()
    }

    @Test
    fun optionalPermissionsAreCollapsedUntilExplicitlyExpanded() {
        render(partialState())

        composeRule.onNodeWithText("Notifikasi pengingat").assertDoesNotExist()
        composeRule.onNodeWithTag("permission-optional-toggle")
            .assert(
                SemanticsMatcher.expectValue(
                    SemanticsProperties.StateDescription,
                    "Diciutkan"
                )
            )
            .performClick()
        composeRule.onNodeWithText("Notifikasi pengingat").assertIsDisplayed()
        composeRule.onNodeWithTag("permission-optional-toggle")
            .assert(
                SemanticsMatcher.expectValue(
                    SemanticsProperties.StateDescription,
                    "Diperluas"
                )
            )
    }

    @Test
    fun readyPanelShowsReadySummary() {
        render(readyState(optionalReady = false))

        composeRule.onNodeWithText("Akses attendance siap").assertIsDisplayed()
    }

    @Test
    fun width320AndFontScaleTwoKeepActiveCardActionAndPrimaryButtonUsable() {
        composeRule.setContent {
            CompositionLocalProvider(LocalDensity provides Density(1f, 2f)) {
                Infinite_TrackTheme {
                    Box(
                        Modifier
                            .width(320.dp)
                            .fillMaxSize()
                            .testTag("screenHost")
                    ) {
                        AttendancePermissionPanelContent(
                            uiState = partialState(),
                            onEvent = {}
                        )
                    }
                }
            }
        }

        val activeCardAction = composeRule.onNodeWithTag(
            "permission-access-card-precise_location"
        )
            .performScrollTo()
            .assertHeightIsAtLeast(48.dp)
            .assertIsDisplayed()
        val hostBounds = composeRule.onNodeWithTag("screenHost").getUnclippedBoundsInRoot()
        val cardActionBounds = activeCardAction.getUnclippedBoundsInRoot()
        assertTrue(
            cardActionBounds.left >= hostBounds.left &&
                cardActionBounds.right <= hostBounds.right
        )
        assertTrue(
            cardActionBounds.top >= hostBounds.top &&
                cardActionBounds.bottom <= hostBounds.bottom
        )

        val primaryButton = composeRule.onNodeWithText("Lanjutkan Setup")
            .performScrollTo()
            .assertHeightIsAtLeast(48.dp)
            .assertIsDisplayed()
        val primaryButtonBounds = primaryButton.getUnclippedBoundsInRoot()
        assertTrue(
            primaryButtonBounds.left >= hostBounds.left &&
                primaryButtonBounds.right <= hostBounds.right
        )
        assertTrue(
            primaryButtonBounds.top >= hostBounds.top &&
                primaryButtonBounds.bottom <= hostBounds.bottom
        )
    }

    private fun render(
        state: AttendancePermissionReadinessUiState,
        onEvent: (AttendancePermissionReadinessEvent) -> Unit = {}
    ) {
        composeRule.setContent {
            Infinite_TrackTheme {
                AttendancePermissionPanelContent(
                    uiState = state,
                    onEvent = onEvent
                )
            }
        }
    }

    private fun partialState(
        requiredReadyCount: Int = 0,
        requiredItems: List<PermissionItemUiModel> = listOf(
            permissionItem(
                AttendanceAccess.PRECISE_LOCATION,
                "Perlu diatur",
                "Minta izin",
                InfiniteSemantic.Primary
            ),
            permissionItem(
                AttendanceAccess.CAMERA,
                "Perlu diatur",
                "Minta izin",
                InfiniteSemantic.Primary
            ),
            permissionItem(
                AttendanceAccess.DEVICE_LOCATION,
                "Perlu diatur",
                "Buka pengaturan",
                InfiniteSemantic.Warning
            )
        )
    ) = AttendancePermissionReadinessUiState(
        isLoading = false,
        requiredItems = requiredItems,
        optionalItems = listOf(
            permissionItem(
                AttendanceAccess.NOTIFICATION,
                "Terbatas",
                "Aktifkan",
                InfiniteSemantic.Warning
            ),
            permissionItem(
                AttendanceAccess.BACKGROUND_LOCATION,
                "Terbatas",
                "Atur akses",
                InfiniteSemantic.Warning
            )
        ),
        requiredReadyCount = requiredReadyCount,
        requiredTotalCount = 3,
        primaryActionLabel = "Lanjutkan Setup",
        primaryActionEnabled = true,
        contextualGuidance = PermissionGuidanceUiModel(
            title = "Pengingat opsional",
            message = "Notifikasi dan lokasi latar belakang membantu pengingat.",
            semantic = InfiniteSemantic.Info
        )
    )

    private fun readyState(optionalReady: Boolean): AttendancePermissionReadinessUiState =
        AttendancePermissionReadinessUiState(
            isLoading = false,
            requiredItems = readyRequiredItems(),
            optionalItems = listOf(
                permissionItem(
                    AttendanceAccess.NOTIFICATION,
                    if (optionalReady) "Siap" else "Terbatas",
                    if (optionalReady) null else "Aktifkan",
                    if (optionalReady) InfiniteSemantic.Success else InfiniteSemantic.Warning,
                    optionalReady
                ),
                permissionItem(
                    AttendanceAccess.BACKGROUND_LOCATION,
                    if (optionalReady) "Siap" else "Terbatas",
                    if (optionalReady) null else "Atur akses",
                    if (optionalReady) InfiniteSemantic.Success else InfiniteSemantic.Warning,
                    optionalReady
                )
            ),
            requiredReadyCount = 3,
            requiredTotalCount = 3,
            canContinue = true,
            primaryActionLabel = "Selesai",
            primaryActionEnabled = true
        )

    private fun readyRequiredItems() = listOf(
        permissionItem(
            AttendanceAccess.PRECISE_LOCATION,
            "Siap",
            null,
            InfiniteSemantic.Success,
            true
        ),
        permissionItem(
            AttendanceAccess.CAMERA,
            "Siap",
            null,
            InfiniteSemantic.Success,
            true
        ),
        permissionItem(
            AttendanceAccess.DEVICE_LOCATION,
            "Siap",
            null,
            InfiniteSemantic.Success,
            true
        )
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
        val requirement = if (
            access in setOf(
                AttendanceAccess.PRECISE_LOCATION,
                AttendanceAccess.CAMERA,
                AttendanceAccess.DEVICE_LOCATION
            )
        ) {
            "Wajib"
        } else {
            "Opsional"
        }
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
            stateDescription = "$title, ${requirement.lowercase()}, $status${
                action?.let { ", aksi $it" }.orEmpty()
            }",
            isReady = ready
        )
    }
}
