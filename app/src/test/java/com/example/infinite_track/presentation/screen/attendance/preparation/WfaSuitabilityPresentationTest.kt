package com.example.infinite_track.presentation.screen.attendance.preparation

import com.example.infinite_track.presentation.design.tokens.InfiniteSemantic
import org.junit.Assert.assertEquals
import org.junit.Test

class WfaSuitabilityPresentationTest {

    @Test
    fun `canonical score maps to percentage and positive semantic`() {
        val presentation = WfaSuitabilityPresentationMapper.map(0.91)

        assertEquals(91, presentation.percentage)
        assertEquals(InfiniteSemantic.Success, presentation.semantic)
    }

    @Test
    fun `semantic boundaries follow normalized score bands`() {
        assertEquals(InfiniteSemantic.Error, WfaSuitabilityPresentationMapper.map(0.0).semantic)
        assertEquals(InfiniteSemantic.Error, WfaSuitabilityPresentationMapper.map(0.59).semantic)
        assertEquals(InfiniteSemantic.Warning, WfaSuitabilityPresentationMapper.map(0.60).semantic)
        assertEquals(InfiniteSemantic.Warning, WfaSuitabilityPresentationMapper.map(0.79).semantic)
        assertEquals(InfiniteSemantic.Success, WfaSuitabilityPresentationMapper.map(0.80).semantic)
        assertEquals(InfiniteSemantic.Success, WfaSuitabilityPresentationMapper.map(1.0).semantic)
    }

    @Test
    fun `out of range and non finite scores are safely bounded`() {
        assertEquals(0, WfaSuitabilityPresentationMapper.map(-0.25).percentage)
        assertEquals(100, WfaSuitabilityPresentationMapper.map(1.25).percentage)
        assertEquals(0, WfaSuitabilityPresentationMapper.map(Double.NaN).percentage)
    }
}
