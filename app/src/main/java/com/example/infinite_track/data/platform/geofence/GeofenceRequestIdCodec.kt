package com.example.infinite_track.data.platform.geofence

import com.example.infinite_track.data.platform.geofence.store.PersistedRegistrationKind
import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import javax.inject.Inject

data class DecodedGeofenceRequestId(
    val generation: Long,
    val kind: PersistedRegistrationKind
)

class GeofenceRequestIdCodec @Inject constructor() {

    fun encode(
        generation: Long,
        kind: PersistedRegistrationKind,
        logicalId: String
    ): String {
        require(generation >= 0) { "Generation must not be negative" }
        require(logicalId.isNotBlank()) { "Logical ID must not be blank" }
        val kindCode = when (kind) {
            PersistedRegistrationKind.REMINDER -> "r"
            PersistedRegistrationKind.ACTIVE -> "a"
        }
        val hash = MessageDigest.getInstance("SHA-256")
            .digest(logicalId.toByteArray(StandardCharsets.UTF_8))
            .take(HASH_HEX_LENGTH / 2)
            .joinToString(separator = "") { byte -> "%02x".format(byte.toInt() and 0xff) }
        return "$PREFIX:${generation.toString(36)}:$kindCode:$hash"
    }

    fun decode(requestId: String): DecodedGeofenceRequestId? {
        val match = REQUEST_ID_PATTERN.matchEntire(requestId) ?: return null
        val generationToken = match.groupValues[1]
        val generation = generationToken.toLongOrNull(36) ?: return null
        if (generationToken != generation.toString(36)) return null
        val kind = when (match.groupValues[2]) {
            "r" -> PersistedRegistrationKind.REMINDER
            "a" -> PersistedRegistrationKind.ACTIVE
            else -> return null
        }
        return DecodedGeofenceRequestId(generation, kind)
    }

    private companion object {
        const val PREFIX = "gf2"
        const val HASH_HEX_LENGTH = 20
        val REQUEST_ID_PATTERN = Regex("^gf2:([0-9a-z]+):([ra]):([0-9a-f]{20})$")
    }
}
