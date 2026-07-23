package com.example.infinite_track.data.location.discovery

import android.content.Context
import com.example.infinite_track.BuildConfig
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
        val apiKey = BuildConfig.MAPS_API_KEY.trim()
        if (apiKey.length < 20) {
            throw PlacesConfigurationException()
        }

        if (!Places.isInitialized()) {
            Places.initialize(context, apiKey, Locale("id", "ID"))
        }
        return Places.createClient(context)
    }
}

class PlacesConfigurationException : IllegalStateException("Google Places is not configured")
