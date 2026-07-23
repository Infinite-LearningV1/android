package com.example.infinite_track.domain.model.location

/**
 * Non-wrapping geographic bounds. Antimeridian-spanning bounds must be split by the caller.
 */
data class GeoBounds(
    val southWest: GeoCoordinate,
    val northEast: GeoCoordinate
) {
    init {
        require(southWest.latitude <= northEast.latitude) {
            "South-west latitude must not exceed north-east latitude"
        }
        require(southWest.longitude <= northEast.longitude) {
            "South-west longitude must not exceed north-east longitude"
        }
    }

    val center: GeoCoordinate
        get() = GeoCoordinate(
            latitude = (southWest.latitude + northEast.latitude) / 2.0,
            longitude = (southWest.longitude + northEast.longitude) / 2.0
        )
}
