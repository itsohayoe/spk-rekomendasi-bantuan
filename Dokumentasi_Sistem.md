# Dokumentasi Sistem

## Identitas Sistem

| Atribut | Keterangan |
|---|---|
| Nama sistem | Pengembangan Sistem Pendukung Keputusan Rekomendasi Bantuan Berbasis Profil Deprivasi Kemiskinan Multidimensi |
| Pengembang | Ayu Amanda Shalsa Putri |
| Institusi | Politeknik Statistika STIS |
| Sumber data penelitian | DTSEN Versi III |
| Komponen pengolahan | Python dan konfigurasi aturan berbasis JSON |
| Basis data | PostgreSQL |
| Backend layanan | Kotlin dengan Ktor |
| Frontend WebGIS | Vue, TypeScript, Vite, Leaflet, dan Pinia |
| Aplikasi demonstrasi | https://webgis-dtsen-nasional.vercel.app/ |
| Repositori pipeline | https://github.com/itsohayoe/python-dtsen |
| Repositori WebGIS | https://github.com/itsohayoe/webgis-dtsen |

## 1. Gambaran Umum

Sistem Pendukung Keputusan Rekomendasi Bantuan Berbasis Profil Deprivasi Kemiskinan Multidimensi dikembangkan untuk menghubungkan kondisi deprivasi keluarga dalam DTSEN Versi III dengan program bantuan yang relevan. Sistem membentuk profil deprivasi keluarga pada dimensi pendidikan, kesehatan, ekonomi, dan perumahan, kemudian mencocokkan profil tersebut dengan kriteria tujuh program bantuan menggunakan pendekatan berbasis aturan. Hasil rekomendasi disajikan pada tingkat keluarga, sedangkan hasil agregasi digunakan untuk menampilkan kebutuhan dan konsentrasi wilayah melalui WebGIS.

Sistem tidak membangun ulang pemeringkatan kesejahteraan atau menetapkan penerima bantuan secara resmi. Rekomendasi merupakan informasi pendukung keputusan yang perlu dibaca bersama aturan program, kelengkapan data, dan proses verifikasi oleh pihak berwenang. Implementasi yang dijelaskan dalam dokumen ini merupakan sistem penelitian dan demonstrasi; dokumen ini tidak menyatakan bahwa sistem telah diterapkan pada lingkungan produksi BPS.

## 2. Tujuan dan Ruang Lingkup

Tujuan sistem adalah membentuk profil deprivasi keluarga dari variabel DTSEN yang relevan, menghasilkan rekomendasi program bantuan berdasarkan aturan dan bobot indikator, serta menyajikan hasil pada tingkat keluarga dan wilayah. Sistem mencakup pipeline pengolahan data, penyimpanan hasil dalam PostgreSQL, layanan backend untuk menyediakan data, dan antarmuka WebGIS untuk eksplorasi hasil. Tujuh program bantuan yang dipetakan dalam penelitian adalah PIP, PKH, PBI JK, ATENSI, BLT, SEMBAKO, dan BSPS.

Ruang lingkup sistem terbatas pada variabel, indikator, aturan, bobot, serta kriteria program yang diimplementasikan pada penelitian. Sistem tidak menilai dampak penyaluran bantuan dan tidak dapat memastikan kesesuaian rekomendasi dengan penerimaan bantuan aktual apabila data penerimaan aktual tidak tersedia. Pembaruan kebijakan program, metadata, skema data, dan konfigurasi perlu ditinjau sebelum digunakan pada lingkungan lain.

## 3. Arsitektur Sistem

Sistem dibangun sebagai rangkaian komponen dengan pembagian tugas yang jelas. Python menjalankan pengolahan data DTSEN, pembentukan fitur keluarga, pembentukan profil deprivasi, pencocokan kriteria program, dan penghitungan rekomendasi. PostgreSQL menyimpan data sumber yang telah disiapkan serta hasil pengolahan yang digunakan oleh komponen layanan.

Backend menggunakan Kotlin dan Ktor untuk menerima permintaan dari WebGIS dan menyediakan data rekomendasi keluarga, kebutuhan program bantuan, serta informasi konsentrasi antarwilayah. Struktur backend membagi tanggung jawab ke dalam route, service, repository, dan data transfer object (DTO): route menerima permintaan, service mengatur proses layanan, repository mengambil data dari PostgreSQL, dan DTO membentuk struktur respons. Frontend WebGIS menggunakan Vue dan teknologi pendukung untuk menampilkan ringkasan, tabel, peta, serta interaksi filter.

Alur data utama dimulai dari DTSEN yang diproses oleh pipeline Python. Hasil pembentukan fitur, profil deprivasi, dan rekomendasi disimpan dalam PostgreSQL. Backend mengambil data sesuai permintaan pengguna dan mengirimkan respons kepada WebGIS, sedangkan WebGIS menyajikan informasi keluarga dan agregasi wilayah. Koneksi dan konfigurasi pada lingkungan demonstrasi mengikuti pengaturan aplikasi yang tersedia; hal ini tidak berarti bahwa sistem telah tersambung ke lingkungan produksi BPS.

## 4. Pipeline Pengolahan Data dan Rekomendasi

### 4.1 Pemuatan dan persiapan data

Pipeline menggunakan data keluarga dan data anggota keluarga. Pengolahan dijalankan secara bertahap, termasuk pengambilan data per provinsi agar proses tidak harus memuat seluruh data ke memori sekaligus. Nilai dibersihkan dan dipersiapkan sesuai aturan pengolahan, dengan tetap membedakan nilai yang diketahui dari nilai yang tidak tersedia.

### 4.2 Pembentukan fitur dan profil deprivasi

Pipeline membentuk fitur pada tingkat keluarga dari atribut rumah tangga dan karakteristik anggota keluarga yang relevan. Fitur tersebut digunakan untuk mengevaluasi sebelas indikator deprivasi pada empat dimensi: pendidikan, kesehatan, ekonomi, dan perumahan. Aturan variabel, fitur, indikator, serta pemetaan program dikelola melalui konfigurasi JSON agar perubahan aturan tertentu dapat dilakukan tanpa mengubah seluruh alur utama.

### 4.3 Pencocokan program

Profil deprivasi dicocokkan dengan kriteria sasaran tujuh program bantuan, yaitu PIP, PKH, PBI JK, ATENSI, BLT, SEMBAKO, dan BSPS. Satu keluarga dapat memperoleh rekomendasi pada lebih dari satu program apabila kondisi yang teridentifikasi memenuhi aturan yang relevan. Syarat tambahan dan pengecualian mengikuti konfigurasi serta implementasi pipeline.

### 4.4 Pembobotan dan skor

Bobot indikator program ditentukan melalui penilaian ahli dengan pendekatan Analytical Hierarchy Process (AHP). Pipeline menggunakan bobot tersebut bersama nilai indikator dan kontribusi deprivasi untuk menghitung skor rekomendasi. Kelompok prioritas dibentuk menggunakan Natural Breaks sesuai implementasi penelitian; kelompok tersebut merupakan pengelompokan skor rekomendasi, bukan pengganti desil kesejahteraan resmi.

### 4.5 Penanganan nilai yang tidak tersedia

Nilai indikator yang tidak diketahui dipertahankan sebagai NULL sesuai aturan pengolahan dan tidak otomatis disamakan dengan nilai 0. Pada aturan gabungan yang menggunakan propagasi nilai, nilai 1 menjadi hasil apabila terdapat kondisi yang terpenuhi; jika tidak terdapat nilai 1 tetapi masih ada NULL, hasil tetap NULL; nilai 0 diberikan apabila kondisi relevan semuanya tidak terpenuhi. Perlakuan nilai pada skor mengikuti aturan khusus yang diterapkan dalam implementasi pipeline.

### 4.6 Penyaringan keluaran

Keluaran rekomendasi mengikuti status program, aturan tambahan, serta pengecualian yang diterapkan pipeline, termasuk pemeriksaan kondisi pekerjaan tertentu. Karena itu, hasil perlu diperiksa bersama indikator pemicu, aturan program, bobot, dan kelengkapan data. Rekomendasi bersifat analitis dan bukan keputusan resmi penerima bantuan.

## 5. Pengolahan Informasi Wilayah

Rekomendasi keluarga menjadi hasil utama sistem. Data keluarga kemudian diagregasi menurut wilayah untuk menghitung proporsi keluarga yang memperoleh rekomendasi dibandingkan jumlah keluarga pada wilayah terkait. Sistem juga menggunakan Location Quotient (LQ) untuk menelaah konsentrasi berdasarkan dimensi deprivasi dan program bantuan dengan wilayah acuan yang dipilih.

Informasi wilayah dapat ditelusuri kembali menurut wilayah dan program untuk memeriksa keluarga yang membentuk agregasi tersebut. Hasil konsentrasi dan agregasi dipengaruhi cakupan data, tingkat wilayah, parameter acuan, serta filter yang digunakan. Interpretasi peta perlu mempertimbangkan konteks tersebut dan tidak digunakan sendirian untuk menetapkan kelayakan keluarga.

## 6. Aplikasi WebGIS

WebGIS merupakan antarmuka untuk mengeksplorasi hasil rekomendasi keluarga dan informasi agregat wilayah. Backend Kotlin/Ktor menyediakan data dari PostgreSQL, sedangkan frontend Vue menyajikan hasil dalam halaman Beranda, Peta Konsentrasi, Peta Rekomendasi, dan Tabel Rekomendasi.

### 6.1 Beranda

Beranda menyajikan ringkasan data dan rekomendasi yang disediakan oleh layanan backend. Pengguna dapat menggunakan filter wilayah, desil, prioritas, atau asesmen sesuai pilihan yang tersedia untuk meninjau ringkasan. Angka yang tampil harus dibaca dengan memperhatikan cakupan data dan filter aktif.

### 6.2 Peta Konsentrasi

Peta Konsentrasi menyajikan informasi konsentrasi wilayah berdasarkan indikator atau program yang tersedia. Pengguna dapat memilih tingkat wilayah dan parameter acuan untuk membandingkan pola konsentrasi. Klasifikasi warna dan nilai peta harus ditafsirkan berdasarkan legenda, parameter, serta cakupan data yang digunakan.

### 6.3 Peta Rekomendasi

Peta Rekomendasi menyajikan persebaran rekomendasi pada wilayah yang tersedia. Pengguna dapat memilih wilayah atau objek peta untuk menelusuri ringkasan dan daftar terkait sesuai interaksi yang disediakan aplikasi. Hasil yang ditampilkan mengikuti filter serta data yang tersedia pada layanan.

### 6.4 Tabel Rekomendasi

Tabel Rekomendasi menyajikan hasil secara terstruktur pada tingkat wilayah dan keluarga sesuai tampilan aplikasi. Pengguna dapat menggunakan filter dan pencarian untuk mempersempit hasil, kemudian meninjau program, status, skor, kelompok prioritas, atau informasi lain yang tersedia pada tabel. Jika hasil tidak muncul, periksa filter, kata kunci, serta koneksi layanan sebelum menyimpulkan bahwa data rekomendasi tidak tersedia.

Aplikasi WebGIS nasional untuk demonstrasi dapat diakses melalui https://webgis-dtsen-nasional.vercel.app/. Akses demonstrasi tersebut tidak boleh dianggap sebagai bukti penerapan atau integrasi dengan lingkungan produksi BPS.

## 7. Struktur Keluaran

Secara fungsional, keluaran pipeline mencakup data fitur keluarga, profil atau status deprivasi indikator, dan rekomendasi program. Rekomendasi memuat informasi yang digunakan untuk menelusuri program yang direkomendasikan, status, skor, kelompok prioritas, dan indikator pendukung sesuai struktur keluaran implementasi. Nama tabel dan kolom yang persis harus merujuk pada skema basis data serta kode versi yang diserahkan.

Keluaran wilayah mencakup ringkasan jumlah atau proporsi rekomendasi dan informasi konsentrasi yang digunakan pada WebGIS. Untuk pemeriksaan, cocokkan beberapa sampel keluarga dari data fitur hingga indikator dan rekomendasi, lalu telusuri apakah agregasi wilayah konsisten dengan data tingkat keluarga. Pemeriksaan sampel membantu verifikasi alur, tetapi tidak dengan sendirinya membuktikan validitas seluruh data sumber.

## 8. Pengujian dan Evaluasi

Evaluasi penelitian mencakup sensitivity analysis, black-box testing, end-to-end testing, dan performance testing. Sensitivity analysis digunakan untuk menelaah pengaruh kriteria deprivasi dan bobot AHP terhadap hasil rekomendasi, termasuk perbandingan dengan pembobotan sama rata. Black-box testing memeriksa fungsi berdasarkan masukan dan hasil yang diharapkan, sedangkan end-to-end testing memeriksa keterhubungan alur dari pengolahan hingga penyajian.

Pada pengujian kinerja yang dilaporkan dalam skripsi, pipeline memproses 99.747 keluarga dalam waktu sekitar 137 detik. Waktu respons rata-rata layanan backend berada pada rentang 0,57–5,20 detik pada kondisi pengujian yang digunakan. Angka tersebut adalah hasil pengujian pada lingkungan dan kondisi penelitian, bukan jaminan kinerja pada perangkat keras, ukuran data, atau lingkungan deployment yang berbeda.

## 9. Pemeliharaan dan Penanganan Masalah

| Gejala | Pemeriksaan awal |
|---|---|
| Pipeline gagal terhubung ke basis data | Periksa konfigurasi koneksi, hak akses, status PostgreSQL, dan nama skema. Jangan menyimpan kredensial pada berkas yang dipublikasikan. |
| Tabel atau kolom tidak ditemukan | Cocokkan skema basis data dengan query dan kode pipeline/backend. |
| Konfigurasi JSON gagal dibaca | Periksa sintaks JSON, nama variabel/fitur, kode kategori, dan struktur aturan. |
| Nilai indikator tidak sesuai | Telusuri data sumber, urutan pemeriksaan kondisi, operator AND/OR, dan perlakuan NULL. |
| Rekomendasi atau skor berbeda dari perkiraan | Periksa indikator pemicu, syarat program, bobot AHP, aturan skor, dan pengelompokan prioritas. |
| WebGIS tidak menampilkan data | Periksa filter, status backend, konfigurasi endpoint, koneksi basis data, dan respons layanan. |
| Peta kosong atau tidak lengkap | Periksa tingkat wilayah, ketersediaan geometri, parameter acuan, dan cakupan data. |

Saat melaporkan masalah, sertakan tahap atau halaman yang bermasalah, pesan kesalahan, versi kode, dan langkah reproduksi. Jangan memasukkan kredensial, token, data keluarga, atau informasi internal ke laporan publik. Perubahan kode dan konfigurasi sebaiknya dicatat agar hasil dapat ditelusuri kembali.

## 10. Keamanan Data dan Batas Penerapan

DTSEN dapat memuat informasi sosial ekonomi keluarga yang tidak untuk dipublikasikan secara terbuka. Repositori publik hanya boleh berisi kode, dokumentasi, konfigurasi nonrahasia, dan dataset yang memang diizinkan untuk dibagikan. Data keluarga internal harus dikeluarkan dari repositori; apabila diperlukan untuk demonstrasi, gunakan data dummy yang telah disamarkan dan tidak memungkinkan identifikasi ulang.

Sistem dikembangkan dan dievaluasi pada lingkungan penelitian. Penerapan pada lingkungan lain memerlukan penyesuaian konfigurasi, verifikasi skema dan metadata, pemeriksaan aturan program terkini, pengujian ulang, serta persetujuan pengelola data dan sistem. Hasil rekomendasi merupakan dukungan analisis; keputusan resmi penerima bantuan tetap berada pada pihak yang berwenang.

## 11. Repositori dan Berkas Pendukung

- Pipeline Python: https://github.com/itsohayoe/python-dtsen
- WebGIS: https://github.com/itsohayoe/webgis-dtsen
- WebGIS demonstrasi nasional: https://webgis-dtsen-nasional.vercel.app/

README pada repositori masing-masing menjelaskan komponen dan cara memulai proyek. Panduan penggunaan pipeline dan panduan penggunaan WebGIS disediakan sebagai berkas terpisah agar langkah menjalankan pengolahan data tidak tercampur dengan langkah menggunakan antarmuka.
