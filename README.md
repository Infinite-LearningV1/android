# Infinite Track

Infinite Track adalah sebuah sistem presensi karyawan berbasis Android yang cerdas dan modern. Tujuan utamanya adalah untuk melampaui aplikasi absensi konvensional dengan mengintegrasikan teknologi canggih untuk memberikan analisis perilaku kerja yang mendalam bagi manajemen dan pengalaman pengguna yang mulus bagi karyawan.

## Core Features

- **Presensi Cerdas**: Menggunakan **Geofencing** untuk notifikasi masuk/keluar area kerja dan **Face Recognition** untuk validasi check-in/check-out yang aman.
- **Fleksibilitas Kerja**: Mendukung mode kerja modern seperti Work From Office (WFO), Work From Home (WFH), dan Work From Anywhere (WFA) dengan sistem booking dan rekomendasi lokasi.
- **Analitik Perilaku**: Memanfaatkan algoritma `Fuzzy Logic` dan `AHP` untuk menghasilkan metrik cerdas seperti skor kelayakan lokasi WFA, indeks kedisiplinan karyawan, dan smart auto check-out.

## Technology Stack & Architecture

### Backend
- **Framework**: Laravel 11 (PHP)
- **Database**: MySQL
- **API**: RESTful API dengan otentikasi JWT

### Android App
- **Bahasa**: Kotlin
- **UI**: Jetpack Compose
- **Arsitektur**: Clean Architecture (MVVM)
- **Dependency Injection**: Hilt
- **Networking**: Retrofit & OkHttp
- **Local Storage**: Room & DataStore
- **Asynchronous**: Kotlin Coroutines & Flow
- **Peta**: Mapbox SDK
- **Kamera & ML**: CameraX & TensorFlow Lite untuk Face Recognition

### Arsitektur Clean Architecture

Proyek ini mengadopsi prinsip Clean Architecture untuk memisahkan *concerns* dan menciptakan basis kode yang tangguh, dapat diuji, dan dapat dipelihara.

```
presentation/  (UI Layer: Composable Screens, ViewModels)
├── screen/
│   ├── attendance/
│   │   ├── AttendanceScreen.kt
│   │   └── AttendanceViewModel.kt
│   └── ...
└── components/
    └── ...

domain/         (Business Logic Layer: Use Cases, Models)
├── model/
│   ├── attendance/
│   │   └── AttendanceModel.kt
│   └── ...
└── use_case/
    └── attendance/
        └── GetTodayStatusUseCase.kt

data/           (Data Layer: Repositories, Data Sources)
├── repository/
│   └── attendance/
│       └── AttendanceRepositoryImpl.kt
├── mapper/
│   └── attendance/
│       └── AttendanceMapper.kt
└── source/
    ├── network/ (Retrofit API Service)
    └── local/   (Room Database, DataStore Preferences)
```

## Getting Started

### Prerequisites
- Android Studio (versi terbaru direkomendasikan)
- JDK 1.8 atau yang lebih baru
- Backend server Infinite Track berjalan dan dapat diakses dari jaringan Anda.

### 1. Clone the Repository
```bash
git clone https://github.com/username/infinite-track.git
cd infinite-track
```

### 2. Setup Configuration

#### a. Mapbox Access Token
1. Buat file bernama `local.properties` di direktori root proyek.
2. Buka `local.properties` dan tambahkan Mapbox Access Token Anda. Ganti `YOUR_MAPBOX_ACCESS_TOKEN` dengan token Anda yang sebenarnya.
   ```properties
   MAPBOX_ACCESS_TOKEN=YOUR_MAPBOX_ACCESS_TOKEN
   ```

#### b. Google Services
Proyek ini menggunakan layanan Google (kemungkinan untuk Firebase Cloud Messaging dan Geofencing).
1. Dapatkan file `google-services.json` Anda dari Firebase console.
2. Tempatkan file `google-services.json` di dalam direktori `app/`.

#### c. Backend API URL
Aplikasi secara cerdas mendeteksi apakah sedang berjalan di emulator atau perangkat fisik untuk menentukan URL backend.
1. Buka file `app/src/main/java/com/example/infinite_track/di/NetworkModule.kt`.
2. Temukan properti `baseUrl`:
   ```kotlin
   private val baseUrl: String
       get() = if (isEmulator()) {
           "http://10.0.2.2:3005/" // Untuk Emulator
       } else {
           "http://192.168.212.197:3005/" // Untuk Perangkat Fisik
       }
   ```
3. **PENTING**: Jika Anda menjalankan aplikasi di perangkat fisik, ubah alamat IP `192.168.212.197` menjadi alamat IP lokal dari mesin tempat backend server Anda berjalan.

### 3. Build and Run the App
1. Buka proyek di Android Studio.
2. Biarkan Gradle menyinkronkan dan mengunduh semua dependensi.
3. Pilih konfigurasi run `app`.
4. Pilih perangkat (emulator atau fisik) dan klik tombol **Run**.

## Project Structure

- **`/app`**: Modul utama aplikasi Android.
  - **`/src/main/java`**: Kode sumber Kotlin, diorganisir berdasarkan fitur dan layer arsitektur.
  - **`/src/main/res`**: Semua resource Android (drawable, layout, font, dll.).
  - **`/src/main/ml`**: Model machine learning (TensorFlow Lite) untuk pengenalan wajah.
- **`/gradle`**: Skrip dan file konfigurasi Gradle.
- **`/memory-bank`**: Dokumentasi internal dan catatan proyek.
