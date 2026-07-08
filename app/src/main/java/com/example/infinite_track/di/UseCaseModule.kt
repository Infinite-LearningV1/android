package com.example.infinite_track.di

import com.example.infinite_track.data.face.FaceProcessor
import com.example.infinite_track.data.soucre.local.preferences.AttendancePreference
import com.example.infinite_track.data.soucre.local.preferences.TodayStatusPreference
import com.example.infinite_track.data.soucre.local.preferences.UserPreference
import com.example.infinite_track.domain.manager.SessionManager
import com.example.infinite_track.data.soucre.local.room.UserDao
import com.example.infinite_track.domain.repository.AttendanceHistoryRepository
import com.example.infinite_track.domain.repository.AttendanceReportPdfRepository
import com.example.infinite_track.domain.repository.AttendanceRepository
import com.example.infinite_track.domain.repository.AuthRepository
import com.example.infinite_track.domain.repository.AuthRuntimeCleaner
import com.example.infinite_track.domain.repository.BookingRepository
import com.example.infinite_track.domain.repository.ContactRepository
import com.example.infinite_track.domain.repository.LocalizationRepository
import com.example.infinite_track.domain.repository.LocationRepository
import com.example.infinite_track.domain.repository.ProfileRepository
import com.example.infinite_track.domain.repository.WfaRepository
import com.example.infinite_track.domain.use_case.attendance.CheckInUseCase
import com.example.infinite_track.domain.use_case.attendance.CheckOutUseCase
import com.example.infinite_track.domain.use_case.attendance.GetTodayStatusUseCase
import com.example.infinite_track.domain.use_case.auth.CheckSessionUseCase
import com.example.infinite_track.domain.use_case.auth.ClearAuthenticatedRuntimeUseCase
import com.example.infinite_track.domain.use_case.auth.GenerateAndSaveEmbeddingUseCase
import com.example.infinite_track.domain.use_case.auth.GetLoggedInUserUseCase
import com.example.infinite_track.domain.use_case.auth.LoginUseCase
import com.example.infinite_track.domain.use_case.auth.LogoutUseCase
import com.example.infinite_track.domain.use_case.auth.ValidateForegroundSessionUseCase
import com.example.infinite_track.domain.use_case.auth.VerifyFaceUseCase
import com.example.infinite_track.domain.use_case.booking.GetBookingHistoryUseCase
import com.example.infinite_track.domain.use_case.booking.ResolveTodayApprovedWfaBookingIdUseCase
import com.example.infinite_track.domain.use_case.booking.SubmitWfaBookingUseCase
import com.example.infinite_track.domain.use_case.contact.GetContactsUseCase
import com.example.infinite_track.domain.use_case.history.ExportAttendanceReportPdfUseCase
import com.example.infinite_track.domain.use_case.history.GetAttendanceHistoryUseCase
import com.example.infinite_track.domain.use_case.language.GetSelectedLanguageUseCase
import com.example.infinite_track.domain.use_case.language.SetSelectedLanguageUseCase
import com.example.infinite_track.domain.use_case.location.GetCurrentAddressUseCase
import com.example.infinite_track.domain.use_case.location.GetCurrentCoordinatesUseCase
import com.example.infinite_track.domain.use_case.location.SearchLocationUseCase
import com.example.infinite_track.domain.use_case.profile.UpdateProfileUseCase
import com.example.infinite_track.domain.use_case.wfa.GetWfaRecommendationsUseCase
import com.example.infinite_track.presentation.geofencing.GeofenceManager
import com.google.android.gms.location.FusedLocationProviderClient
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
object UseCaseModule {

    // Provide the Generate and Save Embedding Use Case
    @Provides
    fun provideGenerateAndSaveEmbeddingUseCase(
        faceProcessor: FaceProcessor,
        authRepository: AuthRepository
    ): GenerateAndSaveEmbeddingUseCase {
        return GenerateAndSaveEmbeddingUseCase(faceProcessor, authRepository)
    }

    // Provide the Verify Face Use Case
    @Provides
    fun provideVerifyFaceUseCase(
        faceProcessor: FaceProcessor,
        authRepository: AuthRepository
    ): VerifyFaceUseCase {
        return VerifyFaceUseCase(faceProcessor, authRepository)
    }

    // Provide the Login Use Case
    @Provides
    fun provideLoginUseCase(
        authRepository: AuthRepository,
        faceProcessor: FaceProcessor
    ): LoginUseCase {
        return LoginUseCase(authRepository, faceProcessor)
    }

    // Provide the Check Session Use Case (Updated to use GenerateAndSaveEmbeddingUseCase)
    @Provides
    fun provideCheckSessionUseCase(
        authRepository: AuthRepository,
        generateAndSaveEmbeddingUseCase: GenerateAndSaveEmbeddingUseCase,
        userPreference: UserPreference,
        sessionManager: SessionManager
    ): CheckSessionUseCase {
        return CheckSessionUseCase(
            authRepository,
            generateAndSaveEmbeddingUseCase,
            userPreference,
            sessionManager
        )
    }

    @Provides
    fun provideClearAuthenticatedRuntimeUseCase(
        authRuntimeCleaner: AuthRuntimeCleaner
    ): ClearAuthenticatedRuntimeUseCase {
        return ClearAuthenticatedRuntimeUseCase(authRuntimeCleaner)
    }

    // Provide the Logout Use Case
    @Provides
    fun provideLogoutUseCase(
        authRepository: AuthRepository,
        clearAuthenticatedRuntimeUseCase: ClearAuthenticatedRuntimeUseCase
    ): LogoutUseCase {
        return LogoutUseCase(authRepository, clearAuthenticatedRuntimeUseCase)
    }

    @Provides
    fun provideValidateForegroundSessionUseCase(
        authRepository: AuthRepository,
        userPreference: UserPreference,
        sessionManager: SessionManager
    ): ValidateForegroundSessionUseCase {
        return ValidateForegroundSessionUseCase(authRepository, userPreference, sessionManager)
    }

    // Provide the Contacts Use Case
    @Provides
    fun provideGetContactsUseCase(contactRepository: ContactRepository): GetContactsUseCase {
        return GetContactsUseCase(contactRepository)
    }

    // Provide the GetLoggedInUser Use Case
    @Provides
    fun provideGetLoggedInUserUseCase(authRepository: AuthRepository): GetLoggedInUserUseCase {
        return GetLoggedInUserUseCase(authRepository)
    }

    // Provide the Language Use Cases
    @Provides
    fun provideGetSelectedLanguageUseCase(localizationRepository: LocalizationRepository): GetSelectedLanguageUseCase {
        return GetSelectedLanguageUseCase(localizationRepository)
    }

    @Provides
    fun provideSetSelectedLanguageUseCase(localizationRepository: LocalizationRepository): SetSelectedLanguageUseCase {
        return SetSelectedLanguageUseCase(localizationRepository)
    }

    // Provide the Update Profile Use Case
    @Provides
    fun provideUpdateProfileUseCase(profileRepository: ProfileRepository): UpdateProfileUseCase {
        return UpdateProfileUseCase(profileRepository)
    }

    // Provide the Attendance History Use Case
    @Provides
    fun provideGetAttendanceHistoryUseCase(
        attendanceHistoryRepository: AttendanceHistoryRepository
    ): GetAttendanceHistoryUseCase {
        return GetAttendanceHistoryUseCase(attendanceHistoryRepository)
    }

    @Provides
    fun provideExportAttendanceReportPdfUseCase(
        attendanceReportPdfRepository: AttendanceReportPdfRepository
    ): ExportAttendanceReportPdfUseCase {
        return ExportAttendanceReportPdfUseCase(attendanceReportPdfRepository)
    }

    // Provide the Get Current Address Use Case
    @Provides
    fun provideGetCurrentAddressUseCase(locationRepository: LocationRepository): GetCurrentAddressUseCase {
        return GetCurrentAddressUseCase(locationRepository)
    }

    // Provide the Get Current Coordinates Use Case
    @Provides
    fun provideGetCurrentCoordinatesUseCase(
        fusedLocationProviderClient: FusedLocationProviderClient,
        userDao: UserDao
    ): GetCurrentCoordinatesUseCase {
        return GetCurrentCoordinatesUseCase(fusedLocationProviderClient, userDao)
    }

    // Provide the Get Today Status Use Case
    @Provides
    fun provideGetTodayStatusUseCase(attendanceRepository: AttendanceRepository): GetTodayStatusUseCase {
        return GetTodayStatusUseCase(attendanceRepository)
    }

    // Provide the WFA Recommendations Use Case
    @Provides
    fun provideGetWfaRecommendationsUseCase(
        wfaRepository: WfaRepository
    ): GetWfaRecommendationsUseCase {
        return GetWfaRecommendationsUseCase(wfaRepository)
    }

    // Provide the Search Location Use Case
    @Provides
    fun provideSearchLocationUseCase(locationRepository: LocationRepository): SearchLocationUseCase {
        return SearchLocationUseCase(locationRepository)
    }

    // Provide the Get Booking History Use Case
    @Provides
    fun provideGetBookingHistoryUseCase(
        bookingRepository: BookingRepository
    ): GetBookingHistoryUseCase {
        return GetBookingHistoryUseCase(bookingRepository)
    }

    // Provide the Submit WFA Booking Use Case
    @Provides
    fun provideSubmitWfaBookingUseCase(
        bookingRepository: BookingRepository
    ): SubmitWfaBookingUseCase {
        return SubmitWfaBookingUseCase(bookingRepository)
    }

    @Provides
    fun provideResolveTodayApprovedWfaBookingIdUseCase(
        bookingRepository: BookingRepository
    ): ResolveTodayApprovedWfaBookingIdUseCase {
        return ResolveTodayApprovedWfaBookingIdUseCase(bookingRepository)
    }

    // Provide the Check In Use Case
    @Provides
    fun provideCheckInUseCase(
        attendanceRepository: AttendanceRepository,
        getCurrentCoordinatesUseCase: GetCurrentCoordinatesUseCase,
        geofenceManager: GeofenceManager,
        attendancePreference: AttendancePreference
    ): CheckInUseCase {
        return CheckInUseCase(attendanceRepository, getCurrentCoordinatesUseCase, geofenceManager, attendancePreference)
    }

    // Provide the Check Out Use Case
    @Provides
    fun provideCheckOutUseCase(
        attendanceRepository: AttendanceRepository,
        getCurrentCoordinatesUseCase: GetCurrentCoordinatesUseCase,
        geofenceManager: GeofenceManager,
        attendancePreference: AttendancePreference
    ): CheckOutUseCase {
        return CheckOutUseCase(attendanceRepository, getCurrentCoordinatesUseCase, geofenceManager, attendancePreference)
    }
}