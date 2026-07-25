package com.example.infinite_track.data.platform.geofence

import com.example.infinite_track.data.platform.geofence.store.PersistedRegistrationKind
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Test

class GeofenceRequestIdCodecTest {

    private val codec = GeofenceRequestIdCodec()

    @Test
    fun `reminder id has exact deterministic format and bounded length`() {
        val requestId = codec.encode(
            generation = 35,
            kind = PersistedRegistrationKind.REMINDER,
            logicalId = "reminder:primary:11"
        )

        assertEquals("gf2:z:r:18e9994e5a22a4195451", requestId)
        assertEquals(28, requestId.length)
    }

    @Test
    fun `decode recovers generation and reminder kind`() {
        assertEquals(
            DecodedGeofenceRequestId(35, PersistedRegistrationKind.REMINDER),
            codec.decode("gf2:z:r:18e9994e5a22a4195451")
        )
    }

    @Test
    fun `decode recovers base36 generation and active kind`() {
        val requestId = codec.encode(
            generation = 36,
            kind = PersistedRegistrationKind.ACTIVE,
            logicalId = "active:91:office:11"
        )

        assertEquals("gf2:10:a:96f1c4e01f18cb7bdda2", requestId)
        assertEquals(
            DecodedGeofenceRequestId(36, PersistedRegistrationKind.ACTIVE),
            codec.decode(requestId)
        )
    }

    @Test
    fun `different logical ids produce different hashes`() {
        val first = codec.encode(1, PersistedRegistrationKind.REMINDER, "reminder:primary:11")
        val second = codec.encode(1, PersistedRegistrationKind.REMINDER, "reminder:primary:12")

        assertNotEquals(first, second)
    }

    @Test
    fun `decode rejects malformed or unsupported ids`() {
        assertNull(codec.decode("gf1:1:r:18e9994e5a22a4195451"))
        assertNull(codec.decode("gf2:-1:r:18e9994e5a22a4195451"))
        assertNull(codec.decode("gf2:1:x:18e9994e5a22a4195451"))
        assertNull(codec.decode("gf2:1:r:not-a-valid-hash"))
    }

    @Test
    fun `decode rejects leading zero generation representations`() {
        assertNull(codec.decode("gf2:00:r:18e9994e5a22a4195451"))
        assertNull(codec.decode("gf2:0z:r:18e9994e5a22a4195451"))
    }
}
