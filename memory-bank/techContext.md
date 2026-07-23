# Technical Context: Infinite Track

## Core Technologies & Architecture

### Backend Stack
- **Framework**: Laravel 11 (PHP)
- **Database**: MySQL dengan migrasi dan seeding
- **API**: RESTful API dengan JSON responses
- **Authentication**: JWT tokens untuk mobile app
- **File Storage**: Laravel storage untuk gambar dan dokumen

### Mobile App Stack
- **Framework**: Android Native dengan Jetpack Compose
- **Architecture**: Clean Architecture (Domain-Data-Presentation)
- **Dependency Injection**: Hilt
- **Networking**: Retrofit + OkHttp
- **Local Storage**: Room Database + DataStore
- **Maps**: Google Maps Compose + Places SDK
- **Image Processing**: CameraX untuk face recognition

### Key Libraries & Dependencies
- **UI**: Jetpack Compose + Material3
- **Navigation**: Compose Navigation
- **State Management**: StateFlow + Compose State
- **Location**: Google Location Services
- **Geofencing**: Google Geofencing API
- **Image Loading**: Coil

## Architecture Patterns

### Clean Architecture Implementation
```
presentation/
├── screen/
│   ├── attendance/
│   │   ├── AttendanceScreen.kt
│   │   ├── AttendanceViewModel.kt
│   │   └── face/FaceScannerScreen.kt
│   └── components/
domain/
├── model/
│   ├── attendance/
│   │   ├── AttendanceModel.kt
│   │   ├── AttendanceHistory.kt
│   │   └── AttendanceLocation.kt
│   └── use_case/
│       └── attendance/GetTodayStatusUseCase.kt
data/
├── repository/
│   └── attendance/AttendanceRepositoryImpl.kt
├── mapper/
│   └── attendance/AttendanceMapper.kt
└── source/
    ├── network/
    └── local/
```

### Dependency Injection Structure
- **RepositoryModule**: Menyediakan repository implementations
- **UseCaseModule**: Menyediakan use cases
- **NetworkModule**: Konfigurasi API dan networking
- **DatabaseModule**: Konfigurasi Room database

## ATTENDANCE SYSTEM - DETAIL TEKNIS

### 1. Domain Models

**TodayStatus Model:**
```kotlin
data class TodayStatus(
    val canCheckIn: Boolean,
    val canCheckOut: Boolean?,
    val checkedInAt: String?,
    val checkedOutAt: String?,
    val activeMode: String,
    val activeLocation: Location?,
    val todayDate: String,
    val isHoliday: Boolean,
    val holidayCheckinEnabled: Boolean,
    val currentTime: String,
    val checkinWindow: CheckinWindow,
    val checkoutAutoTime: String
)
```

**Location Model:**
```kotlin
data class Location(
    val locationId: Int,
    val description: String,
    val latitude: Double,
    val longitude: Double,
    val radius: Int,
    val category: String
)
```

**AttendanceRequestModel:**
```kotlin
data class AttendanceRequestModel(
    val categoryId: Int,
    val latitude: Double,
    val longitude: Double,
    val notes: String,
    val bookingId: Int? = null
)
```

### 2. Repository Implementation

**AttendanceRepositoryImpl Features:**
- **Geofence Integration**: Automatic geofence setup/removal
- **Location Validation**: Distance calculation dengan radius checking
- **Session Management**: Active attendance ID tracking
- **Error Handling**: Comprehensive error handling dan logging

**Key Methods:**
- `getTodayStatus()`: Mengambil status kehadiran hari ini
- `checkIn()`: Proses check-in dengan validasi geofence
- `checkOut()`: Proses check-out dengan cleanup geofence
- `sendLocationEvent()`: Mengirim event ENTER/EXIT ke backend

### 3. ViewModel Architecture

**AttendanceViewModel Features:**
- **Reactive State Management**: StateFlow untuk UI state
- **Geofence Integration**: Real-time geofence status monitoring
- **Multi-Mode Support**: WFO, WFH, WFA locations
- **Map Integration**: Google Maps Compose dengan provider-neutral camera effects

**State Management:**
```kotlin
data class AttendanceScreenState(
    val uiState: UiState<Unit> = UiState.Loading,
    val todayStatus: TodayStatus? = null,
    val wfoLocation: Location? = null,
    val wfhLocation: Location? = null,
    val wfaRecommendations: List<WfaRecommendation> = emptyList(),
    val selectedWorkMode: String = "Work From Office",
    val isUserInsideGeofence: Boolean = false,
    val currentUserAddress: String = "",
    val currentUserLatitude: Double? = null,
    val currentUserLongitude: Double? = null
)
```

### 4. UI Components Architecture

**AttendanceScreen:**
- **Google Maps Integration**: Real-time map dengan multi-marker support
- **Bottom Sheet**: Attendance controls dengan dynamic content
- **Reactive Updates**: Real-time location dan geofence status updates

**AttendanceBottomSheetContent:**
- **Work Mode Selector**: Dynamic work mode selection
- **Location Information**: Current dan target location display
- **Action Buttons**: Booking dan check-in/out controls
- **Search Integration**: WFA location search functionality

**AttendanceActionButtons:**
- **State Management**: Enabled/disabled berdasarkan conditions
- **Dynamic Text**: Check-in/out button text berdasarkan status
- **Booking Integration**: Booking location functionality

### 5. Geofencing Implementation

**GeofenceManager Integration:**
- **Automatic Setup**: Geofence creation saat check-in
- **Event Monitoring**: ENTER/EXIT event handling
- **Reactive Status**: Real-time geofence status di DataStore
- **Cleanup**: Automatic geofence removal saat check-out

**Location Validation:**
- **Distance Calculation**: Menggunakan calculateDistance utility
- **Radius Validation**: Multi-radius support untuk berbagai lokasi
- **Real-time Monitoring**: Continuous location updates

### 6. Data Flow

**Check-in Flow:**
1. User selects work mode (WFO/WFH/WFA)
2. System validates current location vs target location
3. Geofence validation menggunakan reactive state
4. Face recognition verification (optional)
5. API call untuk check-in
6. Geofence monitoring setup
7. UI state update dengan success/error

**Check-out Flow:**
1. System checks active attendance session
2. API call untuk check-out
3. Geofence monitoring cleanup
4. Session cleanup di local storage
5. UI state update

### 7. Error Handling & Logging

**Comprehensive Error Handling:**
- **Network Errors**: Retry mechanism dengan exponential backoff
- **Location Errors**: Fallback strategies untuk location services
- **Geofence Errors**: Graceful degradation tanpa blocking main flow
- **Validation Errors**: User-friendly error messages

**Logging Strategy:**
- **Debug Logging**: Detailed logs untuk development
- **Error Tracking**: Centralized error logging
- **Performance Monitoring**: Location updates dan API calls

### 8. Security & Validation

**Multi-layer Validation:**
- **Geofence Validation**: Primary validation menggunakan geofence
- **Face Recognition**: Secondary validation dengan liveness detection
- **Location Verification**: Server-side location validation
- **Session Management**: Secure session handling dengan JWT

**Data Protection:**
- **Encrypted Storage**: Sensitive data encryption
- **API Security**: JWT token authentication
- **Location Privacy**: Minimal location data storage

## Integration Points

### Backend API Integration
- **Endpoints**: `/api/attendance/today-status`, `/api/attendance/checkin`, `/api/attendance/checkout`
- **Request Format**: JSON dengan location data dan metadata
- **Response Format**: Standardized API response dengan success/error handling

### Maps Integration
- **Google Maps Compose**: Map rendering dengan business locations
- **Marker Management**: Dynamic marker creation untuk WFO/WFH/WFA
- **Camera Controls**: Smooth camera animations dan bounds fitting

### Geofencing Integration
- **Google Geofencing API**: Native Android geofencing
- **DataStore Integration**: Reactive geofence status storage
- **Background Monitoring**: Efficient battery usage dengan proper lifecycle management

## Performance Considerations

### Battery Optimization
- **Location Updates**: Intelligent location update intervals
- **Geofence Efficiency**: Optimized geofence radius dan monitoring
- **Background Tasks**: Minimal background processing

### Memory Management
- **StateFlow**: Efficient state management
- **Image Processing**: Optimized image handling untuk face recognition
- **Cache Strategy**: Strategic caching untuk location data

### Network Optimization
- **Request Batching**: Efficient API calls
- **Retry Logic**: Intelligent retry dengan exponential backoff
- **Offline Support**: Partial offline functionality untuk critical features
