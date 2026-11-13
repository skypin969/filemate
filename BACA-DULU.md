# Filemate: cara membuka dan memasang

Folder ini berisi kode lengkap aplikasi Filemate. Ada dua cara membangun APK-nya. Cara A (GitHub) paling mudah, karena tidak perlu install apa pun. Cara B (Android Studio) untuk nanti, kalau sudah terbiasa.

## Cara A: lewat GitHub (disarankan)

1. Buka github.com, lalu pilih **New** (ikon tambah di pojok kanan atas, lalu **New repository**).
2. Isi nama, misalnya `filemate`. Pilih **Private** supaya kode tidak terlihat orang lain. Tekan **Create repository**.
3. Buka zip, lalu masuk ke folder `filemate`. Pilih **semua isinya** (bukan folder `filemate`-nya), lalu seret ke halaman repository yang baru dibuat. Tunggu sampai selesai, lalu tekan **Commit changes**.
4. Pastikan folder `.github` ikut terunggah. Ini folder tersembunyi, jadi kadang terlewat. Kalau tidak ada, pilih **Add file → Create new file**, ketik `.github/workflows/build-apk.yml` di kolom nama, lalu salin isi berkas `build-apk.yml` dari dalam zip ke kolomnya. Tekan **Commit changes**.
5. Buka tab **Actions** di repository. Pilih **Build APK**, lalu **Run workflow** (kalau belum jalan otomatis). Tunggu sekitar 5 sampai 10 menit.
6. Kalau ikon hijau muncul, klik hasil build, lalu unduh **Filemate-debug** di bagian Artifacts. Di dalamnya ada file APK. Salin ke HP lalu pasang.
7. Kalau ikon merah muncul, buka log build, salin bagian yang berwarna merah, dan kirim ke saya untuk diperbaiki.

## Cara B: lewat Android Studio (opsional)

### 1. Siapkan Android Studio

1. Unduh Android Studio dari developer.android.com/studio.
2. Pasang dengan pengaturan standar.
3. Saat pertama dibuka, biarkan Android Studio mengunduh SDK Android. Proses ini butuh internet.

### 2. Buka proyek

1. Pilih **File → Open**, lalu pilih folder `filemate` (folder yang berisi `settings.gradle.kts`).
2. Tunggu sampai proses **Gradle sync** di bawah selesai. Pertama kali bisa beberapa menit.
3. Jika Android Studio menanyakan soal Gradle atau versi, pilih opsi yang **direkomendasikan**.
4. Jika ada pesan merah, salin isinya dan kirim ke saya.

### 3. Jalankan di HP

1. Di HP, buka **Pengaturan → Tentang ponsel**, lalu ketuk **Nomor build** tujuh kali.
2. Buka **Opsi pengembang**, lalu aktifkan **USB debugging**.
3. Colokkan HP ke komputer, lalu tekan tombol ▶ (Run) di Android Studio.

### 4. Membuat file APK untuk dipasang

1. Pilih **Build → Build APK(s)**.
2. File APK ada di `app/build/outputs/apk/debug/app-debug.apk`. Salin ke HP lalu pasang.

## Yang perlu Anda tahu

- **Belum dikompilasi.** Kode ini dibuat tanpa Android SDK, jadi belum pernah dijalankan. Mungkin ada kesalahan kecil saat pertama dibuka. Itu wajar, dan bisa langsung diperbaiki.
- **Ikon.** Ikon aplikasi memakai gambar yang Anda pilih. File `playstore-icon-512.png` disiapkan untuk diunggah ke Google Play Console.
- **Akses root.** Pilihan "Akses root" di menu samping hanya berfungsi di HP yang sudah di-root (Magisk atau KernelSU) dan izin superuser sudah diberikan.
- **Bongkar APK.** Hasilnya masuk ke folder `Filemate/<nama>` di penyimpanan HP. AndroidManifest ditampilkan sebagai teks dan disimpan sebagai `AndroidManifest.txt` untuk dibaca.
- **Rakit ulang APK.** Hanya mengemas ulang isi folder. Mengubah kode (`classes.dex`) atau resource yang perlu dikompilasi ulang belum didukung. Mengubah `AndroidManifest.txt` juga belum berpengaruh ke APK.
- **Tanda tangan APK.** Memakai kunci yang dibuat di Android Keystore HP. Jika Filemate dihapus, kunci itu ikut hilang. Akibatnya, APK yang ditandatangani dengan kunci lama tidak bisa ditimpa dengan tanda tangan baru.
- **Folder sistem.** Setiap perubahan di folder sistem (`/system`, `/data`, dan sejenisnya) meminta konfirmasi, dan tombolnya aktif hanya setelah Anda mencentang bahwa Anda paham risikonya.
- **Google Play.** Izin "Akses semua berkas" ditinjau Google dan wajib diisi formulir deklarasi. Nama "Filemate" juga sudah dipakai aplikasi lain yang mirip, jadi cek lagi sebelum publikasi.

## Isi folder

| Lokasi | Isi |
|---|---|
| `app/src/main/java/com/filemate/app/MainActivity.kt` | Titik masuk aplikasi |
| `.../ui/` | Semua layar, dialog, dan tampilan |
| `.../ui/FileManagerViewModel.kt` | Otak aplikasi: panel, pilihan, operasi berkas |
| `.../data/` | Membaca folder, operasi berkas, dan perintah root |
| `.../apk/` | Alat APK: pembaca manifest, bongkar, rakit ulang, tanda tangan |
| `.../mcp/` | Layanan APK MCP: server, daftar alat untuk aplikasi AI |
| `.../net/` | Server FTP, server HTTP, Transfer LAN, dan layanan penjaga latar belakang |
| `.../ui/Tools*.kt` | Alat di menu samping: toolbox, jaringan, desibel, perekam aktivitas, analisis kelas, dan lainnya |
| `app/src/main/res/values/` | Teks Bahasa Indonesia |
| `app/src/main/res/values-en/` | Teks English |
