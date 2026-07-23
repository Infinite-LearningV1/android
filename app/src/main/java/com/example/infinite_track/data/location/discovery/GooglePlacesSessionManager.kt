package com.example.infinite_track.data.location.discovery

import com.google.android.libraries.places.api.model.AutocompleteSessionToken
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GooglePlacesSessionManager @Inject constructor() {
    @Volatile
    private var token: AutocompleteSessionToken = AutocompleteSessionToken.newInstance()

    fun current(): AutocompleteSessionToken = token

    @Synchronized
    fun renew() {
        token = AutocompleteSessionToken.newInstance()
    }
}
