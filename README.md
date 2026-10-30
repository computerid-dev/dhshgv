# Mobil GAD (com.mobil.gad)

Project Android Studio untuk mencatat beberapa kendaraan, jadwal ganti oli, pajak, dan KIR.

## Fitur
- Data banyak kendaraan (nama dan nomor polisi)
- Tanggal jadwal oli, pajak, dan KIR
- Alarm notifikasi pukul 09.00, tujuh hari sebelum setiap tanggal
- Data tersimpan lokal di HP
- Cetak laporan melalui menu print Android
- Pengingat dijadwalkan ulang setelah HP dinyalakan kembali

## Membuat APK
1. Ekstrak ZIP.
2. Buka Android Studio, pilih **Open**, lalu pilih folder `MobilGAD`.
3. Tunggu Gradle Sync selesai.
4. Pilih **Build > Build Bundle(s) / APK(s) > Build APK(s)**.
5. APK debug biasanya ada di `app/build/outputs/apk/debug/app-debug.apk`.
6. Instal APK di HP dan izinkan notifikasi.

## Catatan
Alarm disetel pukul 09.00 waktu lokal. Isi tanggal target penggantian oli berikutnya, bukan tanggal oli terakhir. Data tersimpan lokal di HP dan belum tersinkron ke cloud.

## Build lewat GitHub Actions
1. Upload isi folder ini ke repository GitHub (branch `main`).
2. Buka tab **Actions**, workflow **Build APK Mobil GAD** berjalan otomatis (atau jalankan manual lewat *Run workflow*).
3. Setelah hijau, unduh APK dari bagian **Artifacts** (`Mobil-GAD-debug-apk`).

Versi yang dipakai: runner ubuntu-24.04, JDK 17 (Temurin), Gradle 8.10.2, Android Gradle Plugin 8.6.1, Kotlin 2.0.20, compileSdk/targetSdk 35, minSdk 23.
