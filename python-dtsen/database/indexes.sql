-- ============================================================
-- INDEX DATABASE
-- Schema: dtsen
-- ============================================================


-- ============================================================
-- DATA DEPRIVASI
-- ============================================================

CREATE INDEX IF NOT EXISTS idx_data_deprivasi_id_keluarga
ON dtsen.data_deprivasi
USING btree (id_keluarga);

CREATE INDEX IF NOT EXISTS idx_data_deprivasi_indikator_status
ON dtsen.data_deprivasi
USING btree (indikator, status);

CREATE INDEX IF NOT EXISTS idx_data_deprivasi_wilayah_desil
ON dtsen.data_deprivasi
USING btree (
    kode_provinsi,
    kode_kabupaten_kota,
    kode_kecamatan,
    kode_kelurahan_desa,
    desil_nasional
);


-- ============================================================
-- DATA REKOMENDASI
-- ============================================================

CREATE INDEX IF NOT EXISTS idx_data_rekomendasi_id_keluarga
ON dtsen.data_rekomendasi
USING btree (id_keluarga);

CREATE INDEX IF NOT EXISTS idx_data_rekomendasi_program_status
ON dtsen.data_rekomendasi
USING btree (program, status);


-- ============================================================
-- DATASET ANGGOTA KELUARGA
-- ============================================================

CREATE INDEX IF NOT EXISTS idx_dataset_anggota_id_keluarga
ON dtsen.dataset_anggota_keluarga
USING btree (id_keluarga);


-- ============================================================
-- DATASET FEATURE
-- ============================================================

CREATE INDEX IF NOT EXISTS idx_dataset_feature_desil
ON dtsen.dataset_feature
USING btree (desil_nasional);

CREATE INDEX IF NOT EXISTS idx_dataset_feature_wilayah_desil
ON dtsen.dataset_feature
USING btree (
    kode_provinsi,
    kode_kabupaten_kota,
    kode_kecamatan,
    kode_kelurahan_desa,
    desil_nasional
);

CREATE INDEX IF NOT EXISTS idx_feature_desa
ON dtsen.dataset_feature
USING btree (kode_kelurahan_desa);

CREATE INDEX IF NOT EXISTS idx_feature_desil
ON dtsen.dataset_feature
USING btree (desil_nasional);

CREATE INDEX IF NOT EXISTS idx_feature_id_keluarga
ON dtsen.dataset_feature
USING btree (id_keluarga);


-- ============================================================
-- DATASET KELUARGA
-- ============================================================

CREATE INDEX IF NOT EXISTS idx_dataset_keluarga_desil_v3
ON dtsen.dataset_keluarga
USING btree (desil_nasional_v3);

CREATE INDEX IF NOT EXISTS idx_dataset_keluarga_id_keluarga
ON dtsen.dataset_keluarga
USING btree (id_keluarga);

CREATE INDEX IF NOT EXISTS idx_dataset_keluarga_wilayah
ON dtsen.dataset_keluarga
USING btree (
    kode_provinsi,
    kode_kabupaten_kota,
    kode_kecamatan,
    kode_kelurahan_desa
);


-- ============================================================
-- REFERENSI WILAYAH
-- ============================================================

CREATE INDEX IF NOT EXISTS idx_ref_wilayah_desa
ON dtsen.ref_wilayah
USING btree (kode_kelurahan_desa);

CREATE INDEX IF NOT EXISTS idx_ref_wilayah_kab_desa
ON dtsen.ref_wilayah
USING btree (
    kode_kabupaten_kota,
    kode_kelurahan_desa
);

CREATE INDEX IF NOT EXISTS idx_ref_wilayah_kabupaten
ON dtsen.ref_wilayah
USING btree (kode_kabupaten_kota);

CREATE INDEX IF NOT EXISTS idx_ref_wilayah_kec_desa
ON dtsen.ref_wilayah
USING btree (
    kode_kecamatan,
    kode_kelurahan_desa
);

CREATE INDEX IF NOT EXISTS idx_ref_wilayah_kecamatan
ON dtsen.ref_wilayah
USING btree (kode_kecamatan);

CREATE INDEX IF NOT EXISTS idx_ref_wilayah_provinsi
ON dtsen.ref_wilayah
USING btree (kode_provinsi);