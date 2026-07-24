package com.example.infinite_track.domain.model.location

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

/**
 * Model domain sederhana untuk hasil pencarian lokasi
 * Hanya berisi informasi yang benar-benar dibutuhkan oleh UI
 * Menggunakan @Parcelize untuk mendukung pengiriman data antar screen
 */
@Parcelize
data class LocationResult(
    val placeName: String,
    val address: String,
    val latitude: Double,
    val longitude: Double,
    val placeId: String? = null
) : Parcelable
