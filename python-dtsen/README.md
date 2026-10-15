# Pipeline DTSEN dan Rekomendasi Bantuan

Repositori ini berisi kode pengolahan data dan pembentukan rekomendasi bantuan berbasis profil deprivasi kemiskinan multidimensi keluarga menggunakan Data Tunggal Sosial dan Ekonomi Nasional (DTSEN).

## Tujuan

Pipeline ini mengolah data keluarga dan anggota keluarga, membentuk fitur pada tingkat keluarga, menghitung indikator deprivasi, lalu menghasilkan rekomendasi program bantuan berdasarkan aturan dan bobot indikator yang dikonfigurasi.

Dimensi deprivasi yang digunakan meliputi pendidikan, kesehatan, ekonomi, dan perumahan. Program bantuan yang didukung dalam penelitian mencakup PIP, PKH, PBI JK, ATENSI, BLT, SEMBAKO, dan BSPS. Hasil aktual bergantung pada data, konfigurasi aturan, dan versi kode yang digunakan.

## Struktur proyek

```text
python-dtsen/
├── config/                 # Konfigurasi aturan dan pemetaan
├── database/               # Koneksi dan skrip struktur basis data
├── pipeline/               # Pengolahan fitur, deprivasi, dan rekomendasi
├── data/                   # Direktori data lokal (jangan unggah data sensitif)
├── archive/                # Arsip lokal; periksa sebelum dibagikan
├── main.py                 # Titik masuk aplikasi, jika digunakan
├── requirements.txt        # Dependensi Python
└── README.md
```

Struktur aktual dapat berbeda sesuai versi proyek. Direktori data dan arsip tidak harus berisi berkas pada repositori publik.

## Teknologi

- Python
- PostgreSQL
- SQLAlchemy dan psycopg2
- Konfigurasi aturan berbasis JSON

Daftar dependensi Python terdapat pada `requirements.txt`.

## Persiapan lingkungan

Persyaratan:
- Python yang kompatibel dengan dependensi proyek.
- PostgreSQL yang dapat diakses.
- Basis data dengan skema dan tabel yang sesuai dengan skrip pada repositori.
- Data DTSEN yang penggunaannya telah mendapat izin.

Buat dan aktifkan virtual environment dari direktori proyek:

```powershell
py -m venv .venv
.\.venv\Scripts\Activate.ps1
python -m pip install -r requirements.txt
```

Jika PowerShell menolak aktivasi virtual environment, pengguna dapat menjalankan Python dari `.venv` secara langsung atau mengikuti kebijakan eksekusi yang berlaku pada perangkat.

## Konfigurasi koneksi basis data

Buat berkas `.env` di root proyek berdasarkan konfigurasi lokal yang diperlukan. Contoh di bawah adalah template, bukan kredensial yang dapat langsung digunakan:

```dotenv
DB_HOST=localhost
DB_PORT=5432
DB_NAME=nama_database
DB_USER=nama_pengguna
DB_PASSWORD=ganti_dengan_password_lokal
DB_SCHEMA=dtsen
```

Sesuaikan host, port, nama basis data, pengguna, password, dan skema dengan lingkungan masing-masing. Jangan commit `.env` atau membagikan password. Berkas `.env.example` boleh disimpan sebagai contoh tanpa kredensial asli.

## Menjalankan pipeline

Titik masuk yang tersedia pada salinan proyek ini adalah `main.py`. Sebelum menjalankan, periksa argumen atau alur yang diterapkan oleh skrip tersebut dan pastikan koneksi basis data, tabel, serta berkas konfigurasi telah disiapkan.

```powershell
python main.py
```

Jika proses dijalankan melalui skrip tertentu di direktori `pipeline/`, gunakan urutan dan argumen yang ditetapkan oleh implementasi proyek. Jangan menjalankan pipeline terhadap basis data produksi tanpa otorisasi dan pencadangan yang sesuai.

## Keluaran

Pipeline dirancang untuk membentuk atau memperbarui data fitur keluarga, indikator deprivasi, dan hasil rekomendasi bantuan pada basis data sesuai konfigurasi proyek. Nama tabel, cakupan pemrosesan, dan keluaran akhir mengikuti skrip SQL serta kode yang digunakan.

## Privasi dan keamanan

- Jangan mengunggah data DTSEN asli, data identitas keluarga, hasil ekspor basis data, atau cadangan basis data.
- Jangan menyimpan password, token, atau kredensial dalam kode sumber.
- Pastikan izin penggunaan data terpenuhi sebelum memproses atau membagikannya.
- Data contoh yang dibagikan harus sudah diperiksa dan tidak mengungkap informasi pribadi.

## Catatan

Repositori ini merupakan bagian dari penelitian akademik. Hasil rekomendasi adalah bahan pendukung analisis dan tidak menggantikan verifikasi, ketentuan, maupun keputusan resmi penyelenggara program bantuan.
