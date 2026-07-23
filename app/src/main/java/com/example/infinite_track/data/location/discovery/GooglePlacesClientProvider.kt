package com.example.infinite_track.data.location.discovery

import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import com.google.android.libraries.places.api.Places
import com.google.android.libraries.places.api.net.PlacesClient
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GooglePlacesClientProvider @Inject constructor(
    @ApplicationContext private val context: Context
) {
    fun get(): PlacesClient {
        val apiKey = mapsApiKeyFromManifest()
        if (apiKey.length < 20) {
            throw PlacesConfigurationException()
        }

        if (!Places.isInitialized()) {
            Places.initialize(context, apiKey, Locale("id", "ID"))
        }
        return Places.createClient(context)
    }

    private fun mapsApiKeyFromManifest(): String {
        val applicationInfo = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            context.packageManager.getApplicationInfo(
                context.packageName,
                PackageManager.ApplicationInfoFlags.of(PackageManager.GET_META_DATA.toLong())
            )
        } else {
            @Suppress("DEPRECATION")
            context.packageManager.getApplicationInfo(
                context.packageName,
                PackageManager.GET_META_DATA
            )
        }

        return applicationInfo.metaData
            ?.getString(MAPS_API_KEY_METADATA_NAME)
            ?.trim()
            .orEmpty()
    }

    private companion object {
        const val MAPS_API_KEY_METADATA_NAME = "com.google.android.geo.API_KEY"
    }
}

class PlacesConfigurationException : IllegalStateException("Google Places is not configured")
