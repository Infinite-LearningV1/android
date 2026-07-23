package com.example.infinite_track.data.location.address

import android.content.Context
import android.location.Address
import android.location.Geocoder
import android.os.Build
import com.example.infinite_track.domain.model.location.GeoCoordinate
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.IOException
import java.util.Locale
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

class AndroidGeocoderDataSource @Inject constructor(
    @ApplicationContext context: Context
) : AddressGeocoderDataSource {
    private val geocoder = Geocoder(context, Locale("id", "ID"))

    override suspend fun getAddresses(coordinate: GeoCoordinate): List<PlatformAddressSnapshot> {
        if (!Geocoder.isPresent()) throw GeocoderUnavailableException()

        val addresses = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            suspendCancellableCoroutine { continuation ->
                geocoder.getFromLocation(
                    coordinate.latitude,
                    coordinate.longitude,
                    MAX_RESULTS,
                    object : Geocoder.GeocodeListener {
                        override fun onGeocode(addresses: MutableList<Address>) {
                            if (continuation.isActive) continuation.resume(addresses)
                        }

                        override fun onError(errorMessage: String?) {
                            if (continuation.isActive) {
                                continuation.resumeWithException(
                                    IOException("Android Geocoder request failed")
                                )
                            }
                        }
                    }
                )
            }
        } else {
            withContext(Dispatchers.IO) {
                @Suppress("DEPRECATION")
                geocoder.getFromLocation(
                    coordinate.latitude,
                    coordinate.longitude,
                    MAX_RESULTS
                ).orEmpty()
            }
        }

        return addresses.map(::toSnapshot)
    }

    private fun toSnapshot(address: Address): PlatformAddressSnapshot {
        val formattedLines = (0..address.maxAddressLineIndex)
            .mapNotNull(address::getAddressLine)
            .filter(String::isNotBlank)

        return PlatformAddressSnapshot(
            featureName = address.featureName,
            thoroughfare = address.thoroughfare,
            subThoroughfare = address.subThoroughfare,
            locality = address.locality,
            subAdminArea = address.subAdminArea,
            adminArea = address.adminArea,
            postalCode = address.postalCode,
            countryName = address.countryName,
            formattedLines = formattedLines
        )
    }

    private companion object {
        const val MAX_RESULTS = 1
    }
}

class GeocoderUnavailableException : IOException("Android Geocoder service is unavailable")
