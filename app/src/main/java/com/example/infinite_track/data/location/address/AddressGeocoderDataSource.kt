package com.example.infinite_track.data.location.address

import com.example.infinite_track.domain.model.location.GeoCoordinate

interface AddressGeocoderDataSource {
    suspend fun getAddresses(coordinate: GeoCoordinate): List<PlatformAddressSnapshot>
}

data class PlatformAddressSnapshot(
    val featureName: String?,
    val thoroughfare: String?,
    val subThoroughfare: String?,
    val locality: String?,
    val subAdminArea: String?,
    val adminArea: String?,
    val postalCode: String?,
    val countryName: String?,
    val formattedLines: List<String>
)
