# WFA Booking Guard & Prefill Design

Tanggal: 2026-04-23
Status: Draft — approved in discussion, pending written spec review

## Ringkasan
Perubahan ini menyelesaikan dua issue Android Foundation pada flow WFA booking:
- `INF-84` — duplicate-submit guard
- `INF-85` — bounded snapshot read untuk user prefill

Desain ini mempertahankan scope tetap kecil. Perubahan hanya menyentuh disiplin state pada layar booking agar submit tidak bisa dipicu berulang saat request masih berjalan, dan agar prefill `fullName` / `division` dibaca sekali saat layar dibuka alih-alih mengikuti stream profile yang hidup terus.

## Tujuan
- Mencegah duplicate submit pada transaksi booking WFA saat request masih in-flight
- Mengubah prefill user menjadi snapshot sekali baca saat screen dibuka
- Menjaga state layar booking tetap bounded dan mudah dipahami
- Mempertahankan perilaku UI yang ada sejauh tidak bertentangan dengan dua tujuan di atas

## Non-goals
- Tidak mengubah contract booking inti seperti `booking_id`
- Tidak mengubah downstream refresh behavior setelah booking sukses
- Tidak merombak state machine layar booking menjadi arsitektur baru
- Tidak mengubah UX dialog sukses/gagal selain penyelarasan minimal dengan state submit

## Konteks implementasi saat ini
Implementasi saat ini berada di `app/src/main/java/com/example/infinite_track/presentation/screen/attendance/booking/WfaBookingViewModel.kt`.

Temuan utama:
- `loadUserData()` masih memakai `getLoggedInUserUseCase().collect { ... }`
- `onSubmitBooking()` langsung memulai request baru tanpa early-return guard eksplisit saat `isLoading == true`
- UI layar booking di `WfaBookingScreen.kt` sudah membaca `uiState` dan meneruskan `onSendClick = viewModel::onSubmitBooking`

Konfirmasi desain dari diskusi:
- prefill `fullName` dan `division` harus dibaca sekali saat screen dibuka
- perubahan profile di background tidak perlu mengubah layar booking yang sudah terbuka

## Pendekatan yang dipilih
Desain memilih pendekatan minimal pada ViewModel.

Alasan:
- Dua issue ini sama-sama menyasar disiplin state layar booking
- `isLoading` yang sudah ada cukup untuk menjadi sumber guard submit
- Tidak perlu menambah submit state baru karena scope task hanya hardening kecil, bukan redesign flow

## Keputusan desain

### 1. Prefill user menjadi one-shot snapshot
`WfaBookingViewModel` akan memuat data user sekali saat inisialisasi layar, lalu menyalin `fullName` dan `division` ke `uiState`.

Perilaku final:
- screen dibuka
- ViewModel mengambil snapshot user login sekali
- `fullName` dan `division` diisi ke state
- tidak ada long-lived collection yang tetap aktif untuk prefill ini

Konsekuensi yang disengaja:
- jika profile user berubah saat layar booking sedang terbuka, field prefill di layar ini tidak ikut berubah
- layar booking diperlakukan sebagai transactional surface dengan snapshot state yang bounded

### 2. Duplicate-submit guard memakai `isLoading`
`onSubmitBooking()` akan memiliki guard paling awal:
- jika `uiState.isLoading == true`, submit baru diabaikan
- jika `uiState.isLoading == false`, submit boleh lanjut

Perilaku final:
- tap pertama memulai submit dan mengubah `isLoading = true`
- repeated tap selama request berjalan tidak memicu request tambahan
- saat result kembali, `isLoading` kembali `false`
- setelah itu submit baru boleh dilakukan lagi bila user masih berada di layar

### 3. UI tetap selaras dengan state submit
`WfaBookingScreen.kt` dan komponen dialog terkait akan dicek agar path submit benar-benar selaras dengan guard ini.

Target minimal:
- tombol kirim atau callback submit tidak menghasilkan multiple request saat loading
- guard utama tetap berada di ViewModel, bukan hanya bergantung pada disabled state di UI

Ini penting agar transactional safety tidak tergantung timing render Compose atau repeated tap behavior pada device.

## Perubahan file yang diharapkan

### `WfaBookingViewModel.kt`
Perubahan utama ada di file ini.

Yang akan diubah:
- ganti pattern prefill dari stream collection menjadi one-shot retrieval
- tambahkan early-return duplicate-submit guard di `onSubmitBooking()`
- pertahankan alur success / failure yang sudah ada kecuali penyesuaian kecil yang diperlukan agar state tetap konsisten

Yang tidak diubah:
- contract `SubmitWfaBookingUseCase`
- format payload booking selain yang sudah ada
- struktur `WfaBookingState` kecuali jika ada kebutuhan kecil yang benar-benar langsung mendukung dua issue ini

### `WfaBookingScreen.kt`
File ini akan ditinjau untuk memastikan path submit UI selaras dengan `isLoading`.

Perubahan hanya dilakukan bila perlu untuk menjaga konsistensi perilaku tombol kirim terhadap state loading.

### `WfaBookingDialog.kt`
File ini hanya akan disentuh jika komponen submit belum menghormati state loading dengan baik. Jika perilaku saat ini sudah cukup selaras, file ini tidak perlu diubah.

## Data flow final

### Initial load
1. ViewModel dibuat
2. koordinat dari `SavedStateHandle` dimasukkan ke state
3. prefill user dibaca sekali
4. reverse geocode tetap berjalan untuk mengisi alamat
5. `fullName`, `division`, dan `address` terisi ke state sesuai hasil load masing-masing

### Submit pertama
1. user menekan tombol kirim
2. ViewModel membaca state saat ini
3. jika `isLoading == false`, ViewModel set `isLoading = true`
4. tanggal diformat seperti perilaku saat ini
5. request booking dikirim sekali
6. result success/failure mengembalikan `isLoading = false`

### Rapid repeated tap
1. user menekan tombol kirim lagi saat request pertama belum selesai
2. ViewModel melihat `isLoading == true`
3. submit kedua langsung diabaikan
4. tidak ada request kedua ke backend

## Error handling
Desain ini tidak mengubah taxonomy error booking yang lebih besar.

Perilaku yang dipertahankan:
- failure dari repository/use case tetap masuk ke `uiState.error`
- dialog error tetap menggunakan path yang sudah ada
- exception tak terduga tetap dikonversi ke error message umum

Perubahan yang diwajibkan:
- duplicate-submit yang diabaikan tidak boleh dianggap error
- ignored repeated submit tidak perlu memunculkan dialog atau toast

## Verification
Verification minimum untuk task ini:

### INF-84
- repeated tap saat request submit masih berjalan tidak menghasilkan multiple request
- `isLoading` menahan submit lanjutan sampai result pertama selesai
- setelah success, state kembali stabil dan success path tetap berjalan
- setelah failure, `isLoading` kembali `false` dan user bisa submit ulang

### INF-85
- `fullName` dan `division` tetap terisi saat screen dibuka
- prefill tidak lagi bergantung pada `collect` yang hidup terus
- tidak ada repeated state update yang tidak perlu dari stream profile selama layar terbuka

## Risiko dan batas perubahan
Risiko utama task ini kecil karena perubahan terlokalisasi pada layar booking.

Hal yang perlu dijaga:
- one-shot prefill tidak boleh memutus kemampuan layar untuk mengisi `fullName` dan `division`
- guard submit tidak boleh memblokir retry setelah request selesai
- perubahan tidak boleh melebar ke issue lain seperti `booking_id` contract atau downstream refresh contract

## Hasil akhir yang diharapkan
Setelah implementasi:
- layar WFA booking punya prefill snapshot yang bounded
- submit booking menjadi duplicate-safe selama request in-flight
- reasoning state layar booking lebih sederhana
- scope perubahan tetap kecil dan fokus pada INF-84 + INF-85
