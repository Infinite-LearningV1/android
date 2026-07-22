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
}
