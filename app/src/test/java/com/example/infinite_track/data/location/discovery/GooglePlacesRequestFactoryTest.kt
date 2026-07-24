package com.example.infinite_track.data.location.discovery

import com.example.infinite_track.domain.model.location.GeoCoordinate
import com.google.android.libraries.places.api.model.AutocompleteSessionToken
import com.google.android.libraries.places.api.model.Place
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class GooglePlacesRequestFactoryTest {
    private val factory = GooglePlacesRequestFactory()
    private val token = AutocompleteSessionToken.newInstance()

    @Test
    fun `autocomplete restricts Indonesia and adds optional proximity evidence`() {
        val coordinate = GeoCoordinate(-0.899, 119.877)
        val request = factory.autocomplete("kopi", coordinate, token, cancellationToken = null)

        assertEquals("kopi", request.query)
        assertEquals(listOf("ID"), request.countries)
        assertNotNull(request.locationBias)
        assertEquals(-0.899, request.origin?.latitude ?: 0.0, 0.000001)
        assertEquals(119.877, request.origin?.longitude ?: 0.0, 0.000001)
        assertEquals(token, request.sessionToken)
    }

    @Test
    fun `autocomplete works without location bias`() {
        val request = factory.autocomplete("palu", null, token, cancellationToken = null)

        assertNull(request.locationBias)
        assertNull(request.origin)
    }

    @Test
    fun `details request contains only approved essentials fields`() {
        val request = factory.details("opaque-id", token, cancellationToken = null)

        assertEquals("opaque-id", request.placeId)
        assertEquals(token, request.sessionToken)
        assertEquals(
            setOf(
                Place.Field.ID,
                Place.Field.ADDRESS,
                Place.Field.LAT_LNG
            ),
            request.placeFields.toSet()
        )
        assertTrue(Place.Field.RATING !in request.placeFields)
        assertTrue(Place.Field.PHOTO_METADATAS !in request.placeFields)
        assertTrue(Place.Field.REVIEWS !in request.placeFields)
    }
}
