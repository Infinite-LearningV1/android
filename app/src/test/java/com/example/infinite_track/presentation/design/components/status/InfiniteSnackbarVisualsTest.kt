package com.example.infinite_track.presentation.design.components.status

import androidx.compose.material3.SnackbarDuration
import com.example.infinite_track.presentation.design.tokens.InfiniteSemantic
import org.junit.Assert.assertEquals
import org.junit.Test

class InfiniteSnackbarVisualsTest {

    @Test
    fun `success informational primary secondary and neutral visuals default to short`() {
        listOf(
            InfiniteSemantic.Success,
            InfiniteSemantic.Info,
            InfiniteSemantic.Primary,
            InfiniteSemantic.Secondary,
            InfiniteSemantic.Neutral
        ).forEach { semantic ->
            assertEquals(SnackbarDuration.Short, semantic.defaultSnackbarDuration())
        }
    }

    @Test
    fun `warning and error visuals default to long`() {
        listOf(InfiniteSemantic.Warning, InfiniteSemantic.Error).forEach { semantic ->
            assertEquals(SnackbarDuration.Long, semantic.defaultSnackbarDuration())
        }
    }

    @Test
    fun `explicit snackbar duration is preserved`() {
        val visuals = InfiniteSnackbarVisuals(
            message = "Tetap tampil sampai diselesaikan",
            semantic = InfiniteSemantic.Warning,
            duration = SnackbarDuration.Indefinite
        )

        assertEquals(SnackbarDuration.Indefinite, visuals.duration)
    }

    @Test
    fun `snackbar title is optional and preserved when supplied`() {
        assertEquals(
            null,
            InfiniteSnackbarVisuals(
                message = "Saved",
                semantic = InfiniteSemantic.Success
            ).title
        )
        assertEquals(
            "Attendance saved",
            InfiniteSnackbarVisuals(
                message = "Check-in recorded.",
                semantic = InfiniteSemantic.Success,
                title = "Attendance saved"
            ).title
        )
    }

    @Test
    fun `root feedback timeout contract uses exact four and eight second baselines`() {
        assertEquals(4_000L, resolveSnackbarTimeoutMillis(4_000L))
        assertEquals(8_000L, resolveSnackbarTimeoutMillis(8_000L))
    }

    @Test
    fun `accessibility recommendation may extend but never shorten snackbar timeout`() {
        assertEquals(
            12_000L,
            resolveSnackbarTimeoutMillis(8_000L) { 12_000L }
        )
        assertEquals(
            8_000L,
            resolveSnackbarTimeoutMillis(8_000L) { 2_000L }
        )
    }
}
