# WebGIS DTSEN — Sistem Rekomendasi Bantuan

Repositori ini berisi aplikasi WebGIS untuk menyajikan profil deprivasi keluarga dan rekomendasi program bantuan yang dibentuk dari data DTSEN. Aplikasi memadukan frontend berbasis Vue dengan backend Kotlin/Ktor dan basis data PostgreSQL.

## Tujuan

Aplikasi membantu pengguna menelusuri ringkasan rekomendasi, konsentrasi kebutuhan bantuan pada tingkat wilayah, serta rincian rekomendasi pada tingkat keluarga. Informasi yang ditampilkan dimaksudkan sebagai bahan pendukung analisis dan perencanaan, bukan penetapan penerima bantuan secara otomatis.

## Struktur proyek

```text
webgis-dtsen/
├── backend/                 # API Kotlin/Ktor
│   ├── src/
│   ├── gradle/
│   ├── build.gradle.kts
│   ├── settings.gradle.kts
│   └── gradlew
├── frontend/                # Antarmuka Vue/Vite
│   ├── src/
│   ├── public/
│   ├── package.json
│   ├── package-lock.json
│   └── vite.config.js
└── README.md
```

## Teknologi

**Backend**
- Kotlin
- Ktor
- Gradle
- PostgreSQL

**Frontend**
- Vue
- TypeScript/JavaScript sesuai berkas sumber
- Vite
- npm

## Persyaratan

- JDK yang sesuai dengan konfigurasi Gradle proyek.
- Node.js dan npm yang kompatibel dengan versi Vite pada `frontend/package.json`.
- PostgreSQL dengan skema dan tabel yang sesuai.
- Python pipeline (repositori terpisah) jika pengguna perlu membentuk atau memperbarui data hasil rekomendasi.

## 1. Konfigurasi backend

Backend membaca konfigurasi basis data dari environment variable berikut:

| Variabel | Keterangan |
|---|---|
| `DB_URL` | JDBC URL PostgreSQL, misalnya `jdbc:postgresql://localhost:5432/nama_database` |
| `DB_USER` | Pengguna PostgreSQL |
| `DB_PASSWORD` | Password pengguna PostgreSQL |

Atur ketiga variabel pada lingkungan tempat backend dijalankan. Jangan menulis kredensial asli ke dalam kode atau mengunggah berkas `.env` yang berisi rahasia.

Periksa konfigurasi port aplikasi di kode backend dan sesuaikan dengan port yang digunakan lingkungan lokal atau layanan hosting. Jika frontend dan backend berjalan pada origin berbeda, tambahkan origin frontend yang benar ke konfigurasi CORS backend.

## 2. Menjalankan backend

Dari direktori `backend`, jalankan:

```powershell
.\gradlew.bat run
```

Pada Windows, gunakan `gradlew.bat`. Pada Linux/macOS, gunakan `./gradlew run`. Pastikan environment variable basis data telah tersedia sebelum aplikasi dijalankan.

## 3. Konfigurasi frontend

Buat `frontend/.env` berdasarkan template `frontend/.env.example` (jika tersedia). Gunakan nama variabel yang dibaca oleh kode frontend. Untuk Vite, variabel yang perlu tersedia di sisi browser harus memakai awalan `VITE_`.

Contoh nama variabel untuk URL API:

```dotenv
VITE_API_BASE_URL=http://localhost:8080/api
```

Nilai tersebut hanya contoh. Sesuaikan port, host, dan path API dengan backend yang sedang digunakan. Sebelum mengganti URL API, pastikan seluruh pemanggilan API pada kode frontend menggunakan variabel konfigurasi yang sama.

## 4. Menjalankan frontend

Dari direktori `frontend`, jalankan:

```powershell
npm ci
npm run dev
```

Untuk membuat build produksi:

```powershell
npm run build
```

Perintah dan skrip yang tersedia mengikuti `frontend/package.json`.

## Integrasi dengan pipeline

Data untuk ditampilkan pada WebGIS disiapkan oleh pipeline pengolahan DTSEN pada repositori `python-dtsen`. Pastikan basis data telah berisi tabel dan data yang diperlukan sebelum menjalankan aplikasi. Nama tabel dan endpoint API mengikuti implementasi backend pada versi repositori yang digunakan.

## Deployment

Untuk deployment, siapkan layanan backend, basis data PostgreSQL, dan hosting frontend. Konfigurasikan environment variable, port layanan, URL API frontend, serta CORS sesuai domain yang digunakan. Jangan menyimpan password atau token deployment pada repositori.

## Keamanan dan penggunaan data

- Jangan mengunggah data DTSEN asli, data identitas keluarga, dump, atau backup basis data.
- Jangan commit file `.env`, kredensial, token, atau password.
- Pastikan data yang digunakan dan dibagikan telah memperoleh izin yang sesuai.
- Rekomendasi sistem tidak menggantikan verifikasi dan ketentuan resmi program bantuan.

## Catatan

Repositori ini merupakan artefak pendukung penelitian akademik mengenai rekomendasi bantuan berbasis profil deprivasi kemiskinan multidimensi.
