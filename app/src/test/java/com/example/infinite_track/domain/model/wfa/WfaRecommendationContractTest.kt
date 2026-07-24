package com.example.infinite_track.domain.model.wfa

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class WfaRecommendationContractTest {

    @Test
    fun `recommendation exposes canonical fields without transitional aliases`() {
        val methodNames = WfaRecommendation::class.java.declaredMethods.map { it.name }.toSet()

        assertTrue(
            setOf("getScore", "getLabel", "getDistance").none(methodNames::contains)
        )
        assertEquals(1, WfaRecommendation::class.java.declaredConstructors.size)
    }
}
