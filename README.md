# Sistem Pendukung Keputusan Rekomendasi Bantuan Berbasis Profil Deprivasi Kemiskinan Multidimensi

**Pengembang:** Ayu Amanda Shalsa Putri  
**Institusi:** Politeknik Statistika STIS  
**Tahun:** 2026

## Deskripsi

Repositori ini menggabungkan dua komponen utama penelitian, yaitu pipeline Python untuk mengolah Data Tunggal Sosial dan Ekonomi Nasional (DTSEN) Versi III menjadi profil deprivasi keluarga dan rekomendasi program bantuan, serta aplikasi WebGIS untuk menyajikan hasil rekomendasi pada tingkat keluarga dan wilayah.

Sistem menggunakan pendekatan berbasis aturan dan bobot indikator untuk mencocokkan kondisi keluarga dengan kriteria program bantuan. Hasil pengolahan disajikan sebagai informasi pendukung keputusan dan tidak menggantikan proses verifikasi maupun penetapan penerima bantuan oleh pihak berwenang.

## Komponen Sistem

- **Pipeline Python:** pemrosesan data, pembentukan fitur dan profil deprivasi, pencocokan aturan program, serta perhitungan skor rekomendasi.
- **Backend:** layanan berbasis Kotlin dan Ktor untuk menyediakan data kepada aplikasi WebGIS.
- **Frontend WebGIS:** aplikasi berbasis Vue dan TypeScript untuk menampilkan ringkasan, peta, filter, dan tabel rekomendasi.
- **Basis data:** PostgreSQL untuk mendukung penyimpanan dan pengolahan data sistem.
- **Dokumentasi:** dokumentasi arsitektur sistem dan panduan penggunaan setiap komponen.

## Struktur Repositori

```text
spk-rekomendasi-bantuan/
├── README.md
├── Dokumentasi_Sistem.md
├── python-dtsen/
│   ├── README.md
│   ├── Panduan_Penggunaan_Pipeline.md
│   └── ...
└── webgis-dtsen/
    ├── README.md
    ├── Panduan_Penggunaan_WebGIS.md
    ├── backend/
    ├── frontend/
    └── ...
```

## Dokumentasi

- [Dokumentasi Sistem](Dokumentasi_Sistem.md)
- [Panduan Penggunaan Pipeline](python-dtsen/Panduan_Penggunaan_Pipeline.md)
- [Panduan Penggunaan WebGIS](webgis-dtsen/Panduan_Penggunaan_WebGIS.md)

## Aplikasi Demonstrasi

WebGIS nasional: https://webgis-dtsen-nasional.vercel.app/

## Keamanan dan Batasan

Repositori ini ditujukan untuk mendokumentasikan pengembangan sistem penelitian dan demonstrasi. Sistem tidak menyatakan bahwa rekomendasi telah menjadi keputusan resmi penerima bantuan atau bahwa implementasi telah diterapkan pada lingkungan produksi BPS.

Berkas kredensial, token akses, dan data pribadi yang tidak diizinkan untuk dibagikan tidak boleh disertakan dalam repositori. Gunakan berkas contoh konfigurasi tanpa kredensial serta data yang telah mendapat izin untuk dibagikan.
