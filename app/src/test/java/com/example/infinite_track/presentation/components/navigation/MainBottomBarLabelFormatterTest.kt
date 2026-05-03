package com.example.infinite_track.presentation.components.navigation

import org.junit.Assert.assertEquals
import org.junit.Test

class MainBottomBarLabelFormatterTest {

    @Test
    fun `capitalizes lowercase single-word labels`() {
        assertEquals("History", formatBottomBarLabel("history"))
    }

    @Test
    fun `capitalizes lowercase multi-word labels`() {
        assertEquals("My Leave", formatBottomBarLabel("my leave"))
    }

    @Test
    fun `keeps already formatted labels unchanged`() {
        assertEquals("Home", formatBottomBarLabel("Home"))
    }
}
