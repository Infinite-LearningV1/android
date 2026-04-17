# 📉 REKOMENDASI PENGURANGAN UKURAN APK - INFINITE TRACK

## 🎯 MASALAH: APK MASIH TERLALU BESAR

**Ukuran Saat Ini:** ~13-16 MB (setelah optimasi agresif)  
**Target Ideal:** ~8-10 MB  
**Potensi Pengurangan:** **3-6 MB tambahan** (total ~7-10 MB dari baseline)

---

## 📊 ANALISIS LIBRARY BESAR

Berdasarkan audit dependencies, ini adalah library terbesar di project Anda:

| Library             | Estimasi Size | % of APK | Status       | Potensi Penghematan             |
| ------------------- | ------------- | -------- | ------------ | ------------------------------- |
| **Mapbox SDK**      | ~4-5 MB       | 30-35%   | ⚠️ HEAVY     | **-3-4 MB** (ganti Google Maps) |
| **TensorFlow Lite** | ~2-3 MB       | 15-20%   | ⚠️ HEAVY     | **-1-2 MB** (optimize models)   |
| **Google Maps**     | ~1.5-2 MB     | 10-12%   | ⚠️ DUPLICATE | **-1.5 MB** (hapus duplikat)    |
| **Font Files**      | ~1.2 MB       | 7-8%     | ⚠️ LARGE     | **-0.8 MB** (subset/woff2)      |
| **Lottie**          | ~200-400 KB   | 2-3%     | ✅ OK        | Keep (animations needed)        |
| **Firebase**        | ~500 KB       | 3-4%     | ✅ OK        | Keep (messaging needed)         |

---

## 🚨 MASALAH KRITIS: DUPLIKASI MAPS LIBRARY!

### ❌ **ANDA MENGGUNAKAN 2 LIBRARY MAPS SEKALIGUS:**

```kotlin
// Google Maps (TIDAK DIGUNAKAN!)
implementation(libs.maps.compose)           // ~800 KB
implementation(libs.play.services.maps)     // ~1.2 MB
implementation(libs.places)                 // ~500 KB

// Mapbox (YANG BENAR-BENAR DIGUNAKAN)
implementation(libs.mapboxMapsSdk)          // ~4 MB
implementation(libs.mapboxMapsComposeExtension) // ~500 KB
```

**❌ DAMPAK:** Menambah ~2.5 MB untuk library yang TIDAK DIGUNAKAN!

### ✅ **SOLUSI IMMEDIATE: HAPUS GOOGLE MAPS**

Google Maps tidak terpakai di code (hanya Mapbox yang digunakan).

---

## 💡 REKOMENDASI TIER 1: QUICK WINS (IMMEDIATE)

### 1. ✅ **HAPUS GOOGLE MAPS DEPENDENCIES** → **-2.5 MB**

**Action:**

```kotlin
// HAPUS dari build.gradle.kts:
// implementation(libs.maps.compose)           // ← DELETE
// implementation(libs.play.services.maps)     // ← DELETE
// implementation(libs.places)                 // ← DELETE
```

**Justifikasi:**

- Code analysis: Tidak ada import `com.google.android.gms.maps` yang digunakan
- Semua peta menggunakan Mapbox (`com.mapbox.maps`)
- Duplikasi yang tidak perlu

**Savings: 2.5 MB (16-18% reduction!)**

---

### 2. ✅ **OPTIMASI FONT FILES** → **-0.8-1 MB**

**Current:** 4 font files = 1,187 KB (~1.2 MB)

**Masalah:**

```
sf_compact_bold.otf      325 KB
sf_compact_italic.otf    205 KB
sf_compact_medium.otf    335 KB
sf_compact_thin.otf      323 KB
TOTAL:                  1,188 KB
```

**Solusi A: Subset Fonts (Keep only used characters)**

```bash
# Install pyftsubset
pip install fonttools

# Subset untuk karakter Latin + Indonesia saja
pyftsubset sf_compact_bold.otf \
  --output-file=sf_compact_bold_subset.ttf \
  --unicodes="U+0020-007F,U+00A0-00FF" \
  --layout-features="kern,liga" \
  --flavor=woff2

# Repeat untuk semua font files
```

**Expected result:** 325 KB → ~80 KB per file (75% reduction)  
**Total savings:** ~800 KB

**Solusi B: Gunakan System Fonts (Recommended!)**

```kotlin
// Ganti custom fonts dengan system fonts
val fontFamily = FontFamily(
    Font(android.R.font.sans_serif_medium, FontWeight.Medium),
    Font(android.R.font.sans_serif_bold, FontWeight.Bold),
    // ...
)
```

**Savings: 1.2 MB (100% - hapus semua font files!)**

---

### 3. ✅ **GANTI MAPBOX DENGAN GOOGLE MAPS** → **-3-4 MB**

**WHY:** Mapbox SDK jauh lebih besar dari Google Maps!

**Current:**

```
Mapbox SDK:                ~4 MB
Mapbox Compose Extension:  ~500 KB
TOTAL:                     ~4.5 MB
```

**Alternative:**

```
Google Maps SDK:           ~1.2 MB
Maps Compose:              ~800 KB
TOTAL:                     ~2 MB
```

**Savings: 2.5 MB**

**Migration Path:**

```kotlin
// 1. Hapus Mapbox dependencies
// implementation(libs.mapboxMapsSdk)
// implementation(libs.mapboxMapsComposeExtension)

// 2. Keep Google Maps (sudah ada!)
implementation(libs.maps.compose)
implementation(libs.play.services.maps)

// 3. Migrasi code (1-2 hari effort)
// - AttendanceMap.kt: Ganti MapView Mapbox → GoogleMap Compose
// - MapUtils.kt: Ganti Mapbox annotations → Google Maps markers
```

**Effort:** Medium (1-2 hari refactoring)  
**Impact:** High (3-4 MB reduction!)

---

## 💡 REKOMENDASI TIER 2: MEDIUM EFFORT

### 4. ⚠️ **OPTIMASI TENSORFLOW LITE MODELS** → **-1-2 MB**

**Current TensorFlow Lite setup:**

```kotlin
implementation(libs.tensorflow.lite.task.vision)  // ~1.5 MB
implementation(libs.tensorflow.lite)              // ~500 KB
implementation(libs.tensorflow.lite.metadata)     // ~200 KB
implementation(libs.tensorflow.lite.support)      // ~300 KB
implementation(libs.tensorflow.lite.gpu)          // ~500 KB
TOTAL:                                            ~3 MB
```

**Optimizations:**

#### A. **Model Quantization** (jika belum dilakukan)

```bash
# Convert float32 model to int8 (75% smaller)
tflite_convert \
  --input_model=face_recognition.tflite \
  --output_model=face_recognition_quantized.tflite \
  --quantize=true
```

**Expected:** Model size 2 MB → 500 KB (75% reduction)

#### B. **Remove Unused TF Lite Components**

```kotlin
// JIKA tidak pakai GPU delegate, hapus:
// implementation(libs.tensorflow.lite.gpu)  // -500 KB

// JIKA tidak pakai Task API, hapus:
// implementation(libs.tensorflow.lite.task.vision)  // -1.5 MB
```

**Verify usage:**

```bash
grep -r "GpuDelegate\|Task" app/src/main/java/
```

**Potential savings: 1-2 MB**

---

### 5. ⚠️ **SPLIT APK BY ABI** → **-2-3 MB per variant**

**Current:** Single APK dengan 4 ABIs

```kotlin
ndk {
    abiFilters.addAll(listOf(
        "armeabi-v7a",  // ~32-bit ARM
        "arm64-v8a",    // ~64-bit ARM (most devices)
        "x86",          // ~32-bit Intel (emulators)
        "x86_64"        // ~64-bit Intel (rare)
    ))
}
```

**Problem:** Native libraries (TensorFlow, CameraX) dikompilasi untuk semua ABIs  
**Impact:** 4x ukuran untuk .so files!

**Solution: APK Splits**

```kotlin
android {
    splits {
        abi {
            enable = true
            reset()
            include("armeabi-v7a", "arm64-v8a")  // Keep only ARM
            universalApk = false  // Don't create universal APK
        }
    }
}
```

**Result:**

- APK arm64-v8a: ~8-10 MB (most modern devices)
- APK armeabi-v7a: ~9-11 MB (older devices)
- vs Universal APK: ~13-16 MB

**Savings per device: 3-6 MB!**

---

## 💡 REKOMENDASI TIER 3: ADVANCED

### 6. 🔧 **DYNAMIC FEATURE MODULES**

Split fitur yang jarang digunakan ke module terpisah:

```kotlin
// Feature yang bisa di-split:
// 1. Face Recognition (jika tidak semua user pakai)
// 2. WFA Booking (fitur optional)
// 3. Document/Payslip viewer

// Structure:
app/                    // Base APK (~6-8 MB)
├── feature-face/       // Dynamic feature (~2 MB)
├── feature-wfa/        // Dynamic feature (~1 MB)
└── feature-documents/  // Dynamic feature (~1 MB)
```

**Download on-demand saat user butuh!**

---

### 7. 🔧 **ANDROID APP BUNDLE (.aab)**

Gunakan App Bundle untuk distribusi:

```bash
# Build AAB instead of APK
./gradlew :app:bundleRelease

# Generate universal APK for testing
java -jar bundletool.jar build-apks \
  --bundle=app/build/outputs/bundle/release/app-release.aab \
  --output=app.apks \
  --mode=universal
```

**Benefits:**

- Google Play automatically optimizes for each device
- Users download only needed resources
- ~15-20% smaller downloads on average

---

## 📋 ACTION PLAN - PRIORITIZED

### 🚀 **PHASE 1: IMMEDIATE (0-1 hari) - Target: -4 MB**

1. **HAPUS Google Maps** (HIGHEST PRIORITY!)

   ```bash
   # Edit build.gradle.kts
   # Comment out atau delete:
   # - implementation(libs.maps.compose)
   # - implementation(libs.play.services.maps)
   # - implementation(libs.places)
   ```

   **Savings: 2.5 MB**

2. **Gunakan System Fonts**

   ```bash
   # Hapus semua .otf files dari res/font/
   rm app/src/main/res/font/*.otf
   # Update Typography.kt untuk pakai system fonts
   ```

   **Savings: 1.2 MB**

3. **Enable ABI Splits**
   ```kotlin
   // Add to build.gradle.kts
   splits { abi { enable = true } }
   ```
   **Savings: 3+ MB per device type**

**TOTAL: ~4 MB reduction immediately!**

---

### 🎯 **PHASE 2: SHORT-TERM (2-5 hari) - Target: -3 MB**

4. **Migrasi Mapbox → Google Maps**

   - Refactor AttendanceMap.kt
   - Update MapUtils.kt
   - Test semua fitur maps

   **Savings: 2.5 MB**

5. **Optimasi TensorFlow Lite**

   - Quantize model (float32 → int8)
   - Remove unused TF Lite components

   **Savings: 0.5-1 MB**

**TOTAL: ~3.5 MB additional reduction**

---

### 🔮 **PHASE 3: LONG-TERM (1-2 minggu) - Target: -2 MB**

6. **Dynamic Feature Modules**

   - Split face recognition module
   - Split WFA booking module

   **Savings: 2 MB from base APK**

7. **Android App Bundle**

   - Convert distribusi ke .aab
   - Setup Play Console (if needed)

   **Savings: 15-20% average**

---

## 📊 PROJECTED APK SIZE

| Phase       | Action                                  | APK Size     | Reduction |
| ----------- | --------------------------------------- | ------------ | --------- |
| **Current** | With aggressive optimization            | ~13-16 MB    | Baseline  |
| **Phase 1** | Remove Google Maps + Fonts + ABI splits | **~8-10 MB** | **-5 MB** |
| **Phase 2** | Mapbox→Google Maps + TF optimization    | **~6-8 MB**  | **-3 MB** |
| **Phase 3** | Dynamic features + AAB                  | **~5-7 MB**  | **-2 MB** |

**FINAL TARGET: 5-7 MB base APK** (65-78% reduction dari 20 MB!)

---

## 🛠️ IMPLEMENTATION SCRIPT

### Script untuk Phase 1 (Quick Win):

```bash
#!/bin/bash
# apk-reduction-phase1.sh

echo "🚀 APK Size Reduction - Phase 1"

# 1. Backup current build.gradle.kts
cp app/build.gradle.kts app/build.gradle.kts.backup

# 2. Comment out Google Maps dependencies
sed -i 's/implementation(libs.maps.compose)/\/\/ implementation(libs.maps.compose) \/\/ REMOVED: Unused (using Mapbox)/' app/build.gradle.kts
sed -i 's/implementation(libs.play.services.maps)/\/\/ implementation(libs.play.services.maps) \/\/ REMOVED: Unused/' app/build.gradle.kts
sed -i 's/implementation(libs.places)/\/\/ implementation(libs.places) \/\/ REMOVED: Unused/' app/build.gradle.kts

# 3. Delete font files
echo "Removing custom fonts..."
rm -f app/src/main/res/font/*.otf

# 4. Add ABI splits
cat >> app/build.gradle.kts << 'EOF'

    // APK Splits for smaller per-device downloads
    splits {
        abi {
            enable = true
            reset()
            include("armeabi-v7a", "arm64-v8a")
            universalApk = false
        }
    }
EOF

echo "✅ Phase 1 complete!"
echo "📊 Expected reduction: ~4-5 MB"
echo "🔨 Next: ./gradlew clean assembleRelease"
```

---

## ⚠️ VERIFICATION CHECKLIST

Before deploying reduced APK:

- [ ] Build successful
- [ ] No missing dependencies errors
- [ ] Maps still work (Mapbox)
- [ ] Fonts render correctly (system fonts)
- [ ] Face recognition works
- [ ] Camera works
- [ ] All critical features tested
- [ ] APK size < 10 MB
- [ ] Install & launch on real device

---

## 🔍 BONUS: ANALYZE APK

Gunakan Android Studio APK Analyzer:

```bash
# Build release APK
./gradlew assembleRelease

# Analyze
# Android Studio > Build > Analyze APK...
# Select: app/build/outputs/apk/release/app-release-unsigned.apk
```

**Look for:**

1. **Largest files** - Target for optimization
2. **Duplicate resources** - Remove duplicates
3. **Unused code** - Verify R8 is working
4. **Large native libs** - Check ABI splits working

---

## 📞 SUMMARY

### 🎯 **IMMEDIATE ACTION (DO THIS NOW!):**

1. **Hapus Google Maps dependencies** (2.5 MB savings)
2. **Hapus custom fonts, pakai system fonts** (1.2 MB savings)
3. **Enable ABI splits** (3+ MB savings per device)

**Total immediate savings: ~7 MB!**

### 📈 **Expected Results:**

| Metric              | Before   | After Phase 1    | After All Phases |
| ------------------- | -------- | ---------------- | ---------------- |
| **Universal APK**   | 13-16 MB | 8-10 MB          | N/A              |
| **arm64-v8a APK**   | N/A      | 6-8 MB           | 5-7 MB           |
| **armeabi-v7a APK** | N/A      | 7-9 MB           | 6-8 MB           |
| **Reduction**       | Baseline | **-38% to -50%** | **-56% to -65%** |

---

**🎉 KESIMPULAN:**

Aplikasi Anda bisa dikurangi dari **~15 MB menjadi ~6-8 MB** dengan langkah-langkah di atas!

**Prioritas tertinggi:** Hapus Google Maps dependencies (unused!) dan custom fonts = **instant -3.7 MB!**
