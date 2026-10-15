# Panduan Penggunaan Pipeline Python DTSEN

Panduan ini menjelaskan alur umum menyiapkan dan memeriksa pipeline. Nama skrip, perintah terminal, skema tabel, serta dependensi harus mengikuti versi source code dalam repositori ini.

## 1. Tujuan
Pipeline mengolah data keluarga dan anggota keluarga untuk membentuk fitur tingkat keluarga, indikator deprivasi, dan rekomendasi program bantuan berdasarkan konfigurasi serta bobot yang tersedia.

## 2. Persiapan
1. Baca `README.md` dan Wiki repositori.
2. Periksa struktur direktori dan berkas dependensi.
3. Siapkan Python sesuai kebutuhan proyek.
4. Siapkan PostgreSQL dan hak akses yang sesuai.
5. Gunakan data yang berwenang. Jangan unggah data DTSEN internal atau data pribadi ke repositori publik.

## 3. Konfigurasi
1. Gunakan `.env.example` atau contoh konfigurasi yang tersedia jika ada.
2. Siapkan konfigurasi lokal dan isi parameter koneksi yang sesuai.
3. Jangan unggah `.env` berisi kredensial, token, atau alamat layanan internal.
4. Cocokkan nama tabel, kolom, tipe data, kode kategori, dan identitas keluarga dengan skema yang dirujuk skrip.
5. Periksa konfigurasi JSON: nama fitur, indikator, kelompok kondisi, dan pemetaan program harus konsisten dengan kode.

## 4. Menjalankan pipeline
1. Ikuti urutan eksekusi di README, Wiki, atau skrip orkestrasi pada repositori.
2. Uji pada database dan dataset pengembangan terlebih dahulu.
3. Catat versi kode, konfigurasi, waktu proses, dan tahap terakhir yang berhasil.
4. Jangan menebak nama skrip atau perintah sebelum memeriksa versi repositori.

Secara konseptual, alur mencakup pemuatan data, pembersihan, pembentukan fitur keluarga, pembentukan indikator deprivasi, pemetaan program, penghitungan skor, dan penulisan keluaran. Urutan operasional mengikuti dependensi aktual dalam kode.

## 5. Pemeriksaan keluaran
- Bandingkan jumlah baris masukan dan keluaran tiap tahap.
- Pastikan identitas keluarga tidak hilang atau tergandakan tanpa alasan.
- Periksa contoh indikator bernilai `1`, `0`, dan `NULL`.
- Pastikan nilai kosong tidak berubah menjadi nol secara tidak sengaja.
- Hitung ulang contoh skor secara manual berdasarkan bobot dan nilai yang dipakai.
- Periksa apakah keluaran akhir mengikuti aturan status dan penyaringan program.

## 6. Troubleshooting

| Gejala | Pemeriksaan awal |
|---|---|
| Koneksi database gagal | Periksa host, port, database, pengguna, hak akses, dan status layanan. |
| Tabel/kolom tidak ditemukan | Cocokkan skema dengan nama yang dirujuk skrip. |
| JSON gagal dibaca | Periksa sintaks, tipe nilai, nama fitur, dan kelompok kondisi. |
| Indikator tak terduga | Periksa data sumber, urutan kondisi, AND/OR, dan propagasi `NULL`. |
| Rekomendasi tidak muncul | Periksa indikator pemicu, syarat program, pengecualian, dan filter keluaran. |
| Skor berbeda | Periksa bobot, nilai indikator, perlakuan nilai khusus, dan pembulatan. |
| Proses lambat | Identifikasi tahap terlama, lalu periksa query, join/agregasi, ukuran data, dan memori. |

## 7. Keamanan dan batas penggunaan
- Jangan unggah data rumah tangga rahasia atau kredensial aktif.
- Uji perubahan konfigurasi pada sampel yang dapat diperiksa sebelum pemrosesan besar.
- Dokumentasikan perubahan aturan, bobot, skema, dan versi kode.
- Hasil pipeline adalah rekomendasi berbasis aturan, bukan keputusan resmi penerima bantuan.
- Panduan ini tidak menyatakan pipeline telah diuji pada lingkungan produksi BPS.

## Referensi
- Repositori: https://github.com/itsohayoe/python-dtsen
- Wiki: https://github.com/itsohayoe/python-dtsen/wiki
