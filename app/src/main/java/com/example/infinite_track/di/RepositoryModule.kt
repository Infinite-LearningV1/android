package com.example.infinite_track.di

import android.content.Context
import android.util.Log
import com.example.infinite_track.data.repository.attendance.AttendanceHistoryRepositoryImpl
import com.example.infinite_track.data.repository.attendance.AttendanceRepositoryImpl
import com.example.infinite_track.data.repository.auth.AuthRepositoryImpl
import com.example.infinite_track.data.repository.booking.BookingRepositoryImpl
import com.example.infinite_track.data.repository.contact.ContactRepositoryImpl
import com.example.infinite_track.data.repository.localization.LocalizationRepositoryImpl
import com.example.infinite_track.data.repository.location.LocationRepositoryImpl
import com.example.infinite_track.data.repository.profile.ProfileRepositoryImpl
import com.example.infinite_track.data.repository.wfa.WfaRepositoryImpl
import com.example.infinite_track.data.soucre.local.preferences.AttendancePreference
import com.example.infinite_track.data.soucre.local.preferences.LocalizationPreference
import com.example.infinite_track.data.soucre.local.preferences.UserPreference
import com.example.infinite_track.data.soucre.local.room.UserDao
import com.example.infinite_track.data.soucre.network.retrofit.ApiService
import com.example.infinite_track.data.soucre.network.retrofit.MapboxApiService
import com.example.infinite_track.domain.repository.AttendanceHistoryRepository
import com.example.infinite_track.domain.repository.AttendanceRepository
import com.example.infinite_track.domain.repository.AuthRepository
import com.example.infinite_track.domain.repository.BookingRepository
import com.example.infinite_track.domain.repository.ContactRepository
import com.example.infinite_track.domain.repository.LocalizationRepository
import com.example.infinite_track.domain.repository.LocationRepository
import com.example.infinite_track.domain.repository.ProfileRepository
import com.example.infinite_track.domain.repository.WfaRepository
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.gson.Gson
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object RepositoryModule {

    @Provides
    @Singleton
    fun provideAuthRepository(
        userPreference: UserPreference,
        apiService: ApiService,
        userDao: UserDao
    ): AuthRepository {
        return AuthRepositoryImpl(userPreference, apiService, userDao)
    }

    @Provides
    @Singleton
    fun provideAttendanceRepository(
        apiService: ApiService,
        attendancePreference: AttendancePreference
    ): AttendanceRepository {
        try {
            return AttendanceRepositoryImpl(
                apiService, attendancePreference
            )
        } catch (e: Exception) {
            Log.e("RepositoryModule", "Error providing AttendanceRepository: ${e.message}", e)
            throw e
        }
    }

    @Provides
    @Singleton
    fun provideContactRepository(): ContactRepository {
        return ContactRepositoryImpl()
    }

    @Provides
    @Singleton
    fun provideProfileRepository(
        apiService: ApiService,
        userDao: UserDao
    ): ProfileRepository {
        return ProfileRepositoryImpl(apiService, userDao)
    }

    @Provides
    @Singleton
    fun provideAttendanceHistoryRepository(
        apiService: ApiService
    ): AttendanceHistoryRepository {
        return AttendanceHistoryRepositoryImpl(apiService)
    }

    @Provides
    @Singleton
    fun provideLocationRepository(
        @ApplicationContext context: Context,
        fusedLocationProviderClient: FusedLocationProviderClient,
        mapboxApiService: MapboxApiService
    ): LocationRepository {
        return LocationRepositoryImpl(
            context,
            fusedLocationProviderClient,
            mapboxApiService
        )
    }

    @Provides
    @Singleton
    fun provideWfaRepository(
        apiService: ApiService
    ): WfaRepository {
        return WfaRepositoryImpl(apiService)
    }

    @Provides
    @Singleton
    fun provideBookingRepository(
        apiService: ApiService,
        gson: Gson
    ): BookingRepository {
        return BookingRepositoryImpl(apiService, gson)
    }

    @Provides
    @Singleton
    fun provideLocalizationRepository(
        localizationPreference: LocalizationPreference
    ): LocalizationRepository {
        return LocalizationRepositoryImpl(localizationPreference)
    }
}