package backend.repo

import backend.dto.AreaDto
import java.sql.ResultSet
import javax.sql.DataSource

class ConcentRepo(
    private val areaRepo: AreaRepo,
    private val dataSource: DataSource
) {
    fun getBaseAreaData(
        level: String,
        kodeProvinsi: String?,
        kodeKabkot: String?,
        kodeKecamatan: String?,
        kodeKelurahanDesa: String?
    ): List<AreaDto> {
        return areaRepo.getAreaAggregation(
            level = level,
            kodeProvinsi = kodeProvinsi,
            kodeKabkot = kodeKabkot,
            kodeKecamatan = kodeKecamatan,
            kodeKelurahanDesa = kodeKelurahanDesa,
            desil = emptyList(),
            prioritas = emptyList(),
            asesmen = emptyList()
        )
    }

    fun getDimensionBaseData(
        level: String,
        kodeProvinsi: String?,
        kodeKabkot: String?,
        kodeKecamatan: String?,
        kodeKelurahanDesa: String?
    ): List<DimensionAggRow> {
        val kodeWilayahField =
            when (level) {
                "provinsi" -> "TRIM(f.kode_provinsi)"
                "kabupaten" -> "TRIM(f.kode_kabupaten_kota)"
                "kecamatan" -> "TRIM(f.kode_kecamatan)"
                "desa" -> "TRIM(f.kode_kelurahan_desa)"
                else -> "TRIM(f.kode_kabupaten_kota)"
            }

        val namaWilayahField =
            when (level) {
                "provinsi" -> "MAX(w.provinsi)"
                "kabupaten" -> "MAX(w.kabupaten_kota)"
                "kecamatan" -> "MAX(w.kecamatan)"
                "desa" -> "MAX(w.kelurahan_desa)"
                else -> "MAX(w.kabupaten_kota)"
            }

        val conditions = mutableListOf<String>()
        val params = mutableListOf<Any>()

        if (!kodeProvinsi.isNullOrBlank()) {
            conditions.add("TRIM(f.kode_provinsi) = ?")
            params.add(kodeProvinsi.trim())
        }

        if (!kodeKabkot.isNullOrBlank()) {
            conditions.add("TRIM(f.kode_kabupaten_kota) = ?")
            params.add(kodeKabkot.trim())
        }

        if (!kodeKecamatan.isNullOrBlank()) {
            conditions.add("TRIM(f.kode_kecamatan) = ?")
            params.add(kodeKecamatan.trim())
        }

        if (!kodeKelurahanDesa.isNullOrBlank()) {
            conditions.add("TRIM(f.kode_kelurahan_desa) = ?")
            params.add(kodeKelurahanDesa.trim())
        }

        val wilayahWhere =
            if (conditions.isEmpty()) {
                ""
            } else {
                "AND ${conditions.joinToString(" AND ")}"
            }

        val sql = """
            SELECT
                $kodeWilayahField AS kode_wilayah,
                $namaWilayahField AS nama_wilayah,

                MAX(w.provinsi) AS nama_provinsi,
                MAX(w.kabupaten_kota) AS nama_kabkot,
                MAX(w.kecamatan) AS nama_kecamatan,
                MAX(w.kelurahan_desa) AS nama_kelurahan_desa,

                COUNT(*) FILTER (
                    WHERE d.indikator IN ('partisipasi_sekolah', 'lama_sekolah')
                ) AS pendidikan,

                COUNT(*) FILTER (
                    WHERE d.indikator IN ('morbiditas', 'nutrisi_balita')
                ) AS kesehatan,

                COUNT(*) FILTER (
                    WHERE d.indikator IN (
                        'kepadatan_hunian',
                        'kondisi_hunian',
                        'air_minum',
                        'sanitasi'
                    )
                ) AS perumahan,

                COUNT(*) FILTER (
                    WHERE d.indikator IN (
                        'sumber_penerangan',
                        'bahan_bakar_masak',
                        'kepemilikan_aset'
                    )
                ) AS ekonomi,

                COUNT(*) FILTER (
                    WHERE d.indikator IN (
                        'partisipasi_sekolah',
                        'lama_sekolah',
                        'morbiditas',
                        'nutrisi_balita',
                        'kepadatan_hunian',
                        'kondisi_hunian',
                        'air_minum',
                        'sanitasi',
                        'sumber_penerangan',
                        'bahan_bakar_masak',
                        'kepemilikan_aset'
                    )
                ) AS total_deprivasi

            FROM dtsen.data_deprivasi d

            JOIN dtsen.dataset_feature f
                ON TRIM(d.id_keluarga) = TRIM(f.id_keluarga)

            LEFT JOIN dtsen.ref_wilayah w
                ON TRIM(f.kode_kelurahan_desa) = TRIM(w.kode_kelurahan_desa)

            WHERE d.status = 1
            $wilayahWhere

            GROUP BY
                $kodeWilayahField

            ORDER BY nama_wilayah
        """.trimIndent()

        dataSource.connection.use { conn ->
            conn.prepareStatement(sql).use { stmt ->
                params.forEachIndexed { index, value ->
                    stmt.setString(index + 1, value.toString())
                }

                stmt.executeQuery().use { rs ->
                    val result = mutableListOf<DimensionAggRow>()

                    while (rs.next()) {
                        result.add(mapDimensionAggRow(level, rs))
                    }

                    return result
                }
            }
        }
    }

    private fun mapDimensionAggRow(
        level: String,
        rs: ResultSet
    ): DimensionAggRow {
        val kodeWilayah = rs.getString("kode_wilayah") ?: ""

        return DimensionAggRow(
            level = level,

            kodeWilayah = kodeWilayah,
            namaWilayah = rs.getString("nama_wilayah") ?: "-",

            kodeProvinsi = if (level == "provinsi") kodeWilayah else "",
            kodeKabkot = if (level == "kabupaten") kodeWilayah else "",
            kodeKecamatan = if (level == "kecamatan") kodeWilayah else "",
            kodeKelurahanDesa = if (level == "desa") kodeWilayah else "",

            namaProvinsi = rs.getString("nama_provinsi") ?: "-",
            namaKabkot = rs.getString("nama_kabkot") ?: "-",
            namaKecamatan = rs.getString("nama_kecamatan"),
            namaKelurahanDesa = rs.getString("nama_kelurahan_desa"),

            pendidikan = rs.getLong("pendidikan"),
            kesehatan = rs.getLong("kesehatan"),
            perumahan = rs.getLong("perumahan"),
            ekonomi = rs.getLong("ekonomi"),
            totalDeprivasi = rs.getLong("total_deprivasi")
        )
    }

    data class DimensionAggRow(
        val level: String,

        val kodeWilayah: String,
        val namaWilayah: String,

        val kodeProvinsi: String,
        val kodeKabkot: String,
        val kodeKecamatan: String?,
        val kodeKelurahanDesa: String?,

        val namaProvinsi: String,
        val namaKabkot: String,
        val namaKecamatan: String?,
        val namaKelurahanDesa: String?,

        val pendidikan: Long,
        val kesehatan: Long,
        val perumahan: Long,
        val ekonomi: Long,
        val totalDeprivasi: Long
    )
}