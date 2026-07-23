package com.example.infinite_track.data.location.address

import com.example.infinite_track.domain.model.location.GeoCoordinate
import com.example.infinite_track.domain.model.location.ResolvedAddress
import javax.inject.Inject

class AndroidAddressMapper @Inject constructor() {
    fun toDomain(
        coordinate: GeoCoordinate,
        snapshot: PlatformAddressSnapshot
    ): ResolvedAddress? {
        val formattedAddress = snapshot.formattedLines.firstOrNull()
            ?: listOfNotNull(
                listOfNotNull(snapshot.subThoroughfare, snapshot.thoroughfare)
                    .joinToString(" ")
                    .takeIf(String::isNotBlank),
                snapshot.locality,
                snapshot.subAdminArea,
                snapshot.adminArea,
                snapshot.postalCode,
                snapshot.countryName
            ).distinct().joinToString(", ").takeIf(String::isNotBlank)
            ?: return null

        return ResolvedAddress(
            coordinate = coordinate,
            name = snapshot.featureName?.takeIf(String::isNotBlank)
                ?: snapshot.thoroughfare?.takeIf(String::isNotBlank),
            formattedAddress = formattedAddress
        )
    }
}
