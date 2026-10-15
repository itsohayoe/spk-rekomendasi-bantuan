package backend.repo

import backend.dto.AreaDto
import backend.dto.DimensionIndicatorDto
import java.sql.ResultSet
import javax.sql.DataSource

class AreaRepo(
    private val dataSource: DataSource
) {

    private val dimensionIndicators = mapOf(
        "pendidikan" to setOf(
            "partisipasi_sekolah",
            "lama_sekolah"
        ),

        "kesehatan" to setOf(
            "morbiditas",
            "nutrisi_balita"
        ),

        "perumahan" to setOf(
            "kepadatan_hunian",
            "kondisi_hunian",
            "air_minum",
            "sanitasi"
        ),

        "ekonomi" to setOf(
            "sumber_penerangan",
            "bahan_bakar_masak",
            "kepemilikan_aset"
        )
    )

    private val programIndicators = mapOf(
        "pip" to setOf(
            "partisipasi_sekolah",
            "lama_sekolah"
        ),

        "pkh" to setOf(
            "partisipasi_sekolah",
            "lama_sekolah",
            "morbiditas",
            "nutrisi_balita"
        ),

        "pbi_jk" to setOf(
            "morbiditas",
            "nutrisi_balita"
        ),

        "atensi" to setOf(
            "morbiditas",
            "nutrisi_balita"
        ),

        "blt" to setOf(
            "morbiditas",
            "nutrisi_balita",
            "sumber_penerangan",
            "bahan_bakar_masak",
            "kepemilikan_aset"
        ),

        "sembako" to setOf(
            "sumber_penerangan",
            "bahan_bakar_masak",
            "kepemilikan_aset"
        ),

        "bsps" to setOf(
            "kepadatan_hunian",
            "kondisi_hunian",
            "air_minum",
            "sanitasi"
        )
    )

    fun getAreaAggregation(
        level: String,
        kodeProvinsi: String?,
        kodeKabkot: String?,
        kodeKecamatan: String?,
        kodeKelurahanDesa: String?,
        desil: List<Int>,
        prioritas: List<String> = emptyList(),
        asesmen: List<String> = emptyList()
    ): List<AreaDto> {

        val featureFilter = buildFeatureFilterOnlyWilayah(
            kodeProvinsi = kodeProvinsi,
            kodeKabkot = kodeKabkot,
            kodeKecamatan = kodeKecamatan,
            kodeKelurahanDesa = kodeKelurahanDesa
        )

        val recomFilter = buildRecommendationSubqueryFilter(
            desil = desil,
            prioritas = prioritas,
            asesmen = asesmen
        )

        val kodeWilayahField =
            when (level) {
                "provinsi" -> "f.kode_provinsi"
                "kabupaten" -> "f.kode_kabupaten_kota"
                "kecamatan" -> "f.kode_kecamatan"
                "desa" -> "f.kode_kelurahan_desa"
                else -> "f.kode_kabupaten_kota"
            }

        val namaWilayahField =
            when (level) {
                "provinsi" -> "MAX(w.provinsi)"
                "kabupaten" -> "MAX(w.kabupaten_kota)"
                "kecamatan" -> "MAX(w.kecamatan)"
                "desa" -> "MAX(w.kelurahan_desa)"
                else -> "MAX(w.kabupaten_kota)"
            }

        val sql = """
            WITH f_base AS (
                SELECT DISTINCT
                    TRIM(f.id_keluarga) AS id_keluarga,
                    TRIM(f.kode_provinsi) AS kode_provinsi,
                    TRIM(f.kode_kabupaten_kota) AS kode_kabupaten_kota,
                    TRIM(f.kode_kecamatan) AS kode_kecamatan,
                    TRIM(f.kode_kelurahan_desa) AS kode_kelurahan_desa
                FROM dtsen.dataset_feature f
                ${featureFilter.whereClause}
            ),

            r_agg AS (
                SELECT
                    TRIM(r.id_keluarga) AS id_keluarga,

                    COUNT(r.id_rekomendasi) AS total_rekomendasi,

                    COUNT(r.id_rekomendasi) FILTER (
                        WHERE TRIM(r.program) = 'PIP'
                    ) AS pip,

                    COUNT(r.id_rekomendasi) FILTER (
                        WHERE TRIM(r.program) = 'PKH'
                    ) AS pkh,

                    COUNT(r.id_rekomendasi) FILTER (
                        WHERE TRIM(r.program) = 'PBI JK'
                    ) AS pbi_jk,

                    COUNT(r.id_rekomendasi) FILTER (
                        WHERE TRIM(r.program) = 'ATENSI'
                    ) AS atensi,

                    COUNT(r.id_rekomendasi) FILTER (
                        WHERE TRIM(r.program) = 'SEMBAKO'
                    ) AS sembako,

                    COUNT(r.id_rekomendasi) FILTER (
                        WHERE TRIM(r.program) = 'BLT'
                    ) AS blt,

                    COUNT(r.id_rekomendasi) FILTER (
                        WHERE TRIM(r.program) = 'BSPS'
                    ) AS bsps

                FROM dtsen.data_rekomendasi r
                ${recomFilter.whereClause}
                GROUP BY TRIM(r.id_keluarga)
            )

            SELECT
                $kodeWilayahField AS kode_wilayah,
                $namaWilayahField AS nama_wilayah,

                MAX(w.provinsi) AS nama_provinsi,
                MAX(w.kabupaten_kota) AS nama_kabkot,
                MAX(w.kecamatan) AS nama_kecamatan,
                MAX(w.kelurahan_desa) AS nama_kelurahan_desa,

                COUNT(r.id_keluarga) AS total_keluarga,

                COUNT(DISTINCT f.id_keluarga) AS total_keluarga_wilayah,

                COALESCE(SUM(r.total_rekomendasi), 0) AS total_rekomendasi,

                CASE
                    WHEN COUNT(DISTINCT f.id_keluarga) = 0 THEN 0
                    ELSE
                        COUNT(r.id_keluarga)::numeric
                        / COUNT(DISTINCT f.id_keluarga)::numeric
                        * 100
                END AS persentase_keluarga_membutuhkan,

                COALESCE(SUM(r.pip), 0) AS pip,
                COALESCE(SUM(r.pkh), 0) AS pkh,
                COALESCE(SUM(r.pbi_jk), 0) AS pbi_jk,
                COALESCE(SUM(r.atensi), 0) AS atensi,
                COALESCE(SUM(r.sembako), 0) AS sembako,
                COALESCE(SUM(r.blt), 0) AS blt,
                COALESCE(SUM(r.bsps), 0) AS bsps

            FROM f_base f

            LEFT JOIN r_agg r
                ON f.id_keluarga = r.id_keluarga

            LEFT JOIN dtsen.ref_wilayah w
                ON f.kode_kelurahan_desa = TRIM(w.kode_kelurahan_desa)

            GROUP BY
                $kodeWilayahField

            ORDER BY nama_wilayah
        """.trimIndent()

        val allParams = featureFilter.params + recomFilter.params

        dataSource.connection.use { conn ->
            conn.prepareStatement(sql).use { stmt ->

                allParams.forEachIndexed { index, value ->
                    when (value) {
                        is Int -> stmt.setInt(index + 1, value)
                        else -> stmt.setString(index + 1, value.toString())
                    }
                }

                stmt.executeQuery().use { rs ->
                    val result = mutableListOf<AreaDto>()

                    while (rs.next()) {
                        result.add(mapArea(level, rs))
                    }

                    val indicatorRows = getIndicatorAggregation(
                        level = level,
                        featureFilter = featureFilter
                    )

                    return attachIndicatorDetails(
                        areas = result,
                        indicatorRows = indicatorRows
                    )
                }
            }
        }
    }

    private fun buildFeatureFilterOnlyWilayah(
        kodeProvinsi: String?,
        kodeKabkot: String?,
        kodeKecamatan: String?,
        kodeKelurahanDesa: String?
    ): QueryParts {
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

        val whereClause =
            if (conditions.isEmpty()) {
                ""
            } else {
                "WHERE ${conditions.joinToString(" AND ")}"
            }

        return QueryParts(
            whereClause = whereClause,
            params = params
        )
    }

    private fun buildRecommendationSubqueryFilter(
        desil: List<Int>,
        prioritas: List<String>,
        asesmen: List<String>
    ): QueryParts {
        val conditions = mutableListOf<String>()
        val params = mutableListOf<Any>()

        conditions.add("r.status = 1")

        if (desil.isNotEmpty()) {
            val placeholders = desil.joinToString(",") { "?" }
            conditions.add("CAST(r.desil_nasional AS INTEGER) IN ($placeholders)")
            params.addAll(desil)
        }

        val prioritasCodes = prioritas.mapNotNull { value ->
            when (value.trim().lowercase()) {
                "sangat tinggi", "1" -> 1
                "tinggi", "2" -> 2
                "sedang", "3" -> 3
                "rendah", "4" -> 4
                "sangat rendah", "5" -> 5
                else -> value.toIntOrNull()
            }
        }

        if (prioritasCodes.isNotEmpty()) {
            val placeholders = prioritasCodes.joinToString(",") { "?" }
            conditions.add("CAST(r.prioritas AS INTEGER) IN ($placeholders)")
            params.addAll(prioritasCodes)
        }

        val asesmenCodes = asesmen.mapNotNull { value ->
            when (value.trim().lowercase()) {
                "perlu", "perlu asesmen", "1" -> 1
                "tidak_perlu", "tidak perlu", "0" -> 0
                else -> value.toIntOrNull()
            }
        }

        if (asesmenCodes.isNotEmpty()) {
            val placeholders = asesmenCodes.joinToString(",") { "?" }
            conditions.add("CAST(r.asesmen AS INTEGER) IN ($placeholders)")
            params.addAll(asesmenCodes)
        }

        return QueryParts(
            whereClause = "WHERE ${conditions.joinToString(" AND ")}",
            params = params
        )
    }

        private fun getIndicatorAggregation(
            level: String,
            featureFilter: QueryParts
        ): List<IndicatorAggRow> {
            val kodeWilayahField =
                when (level) {
                    "provinsi" -> "f.kode_provinsi"
                    "kabupaten" -> "f.kode_kabupaten_kota"
                    "kecamatan" -> "f.kode_kecamatan"
                    "desa" -> "f.kode_kelurahan_desa"
                    else -> "f.kode_kabupaten_kota"
                }

            val sql = """
                WITH f_base AS (
                    SELECT DISTINCT
                        TRIM(f.id_keluarga) AS id_keluarga,
                        TRIM(f.kode_provinsi) AS kode_provinsi,
                        TRIM(f.kode_kabupaten_kota) AS kode_kabupaten_kota,
                        TRIM(f.kode_kecamatan) AS kode_kecamatan,
                        TRIM(f.kode_kelurahan_desa) AS kode_kelurahan_desa
                    FROM dtsen.dataset_feature f
                    ${featureFilter.whereClause}
                )

                SELECT
                    $kodeWilayahField AS kode_wilayah,
                    LOWER(TRIM(d.indikator)) AS indikator,
                    COUNT(*) AS jumlah
                FROM f_base f
                JOIN dtsen.data_deprivasi d
                    ON TRIM(d.id_keluarga) = f.id_keluarga
                WHERE CAST(d.status AS INTEGER) = 1
                GROUP BY
                    $kodeWilayahField,
                    LOWER(TRIM(d.indikator))
            """.trimIndent()

            dataSource.connection.use { conn ->
                conn.prepareStatement(sql).use { stmt ->

                    featureFilter.params.forEachIndexed { index, value ->
                        stmt.setString(index + 1, value.toString())
                    }

                    stmt.executeQuery().use { rs ->
                        val result = mutableListOf<IndicatorAggRow>()

                        while (rs.next()) {
                            result.add(
                                IndicatorAggRow(
                                    kodeWilayah = rs.getString("kode_wilayah") ?: "",
                                    indikator = rs.getString("indikator") ?: "",
                                    jumlah = rs.getLong("jumlah")
                                )
                            )
                        }

                        return result
                    }
                }
            }
        }

        private fun attachIndicatorDetails(
            areas: List<AreaDto>,
            indicatorRows: List<IndicatorAggRow>
        ): List<AreaDto> {
            val rowsByWilayah = indicatorRows.groupBy { it.kodeWilayah }

            return areas.map { area ->
                val rows = rowsByWilayah[area.kodeWilayah].orEmpty()

                area.copy(
                    deprivasiCount = buildDeprivasiCount(rows),
                    bantuanIndikator = buildBantuanIndikator(rows)
                )
            }
        }

        private fun buildDeprivasiCount(
            rows: List<IndicatorAggRow>
        ): Map<String, DimensionIndicatorDto> {
            return dimensionIndicators.mapValues { (_, indicators) ->
                val indicatorMap =
                    rows
                        .filter { row -> row.indikator in indicators }
                        .groupBy { row -> row.indikator }
                        .mapValues { (_, values) ->
                            values.sumOf { it.jumlah }
                        }

                DimensionIndicatorDto(
                    total = indicatorMap.values.sum(),
                    indikator = indicatorMap
                )
            }
        }

        private fun buildBantuanIndikator(
            rows: List<IndicatorAggRow>
        ): Map<String, Map<String, Long>> {
            return programIndicators.mapValues { (_, indicators) ->
                rows
                    .filter { row -> row.indikator in indicators }
                    .groupBy { row -> row.indikator }
                    .mapValues { (_, values) ->
                        values.sumOf { it.jumlah }
                    }
            }
        }

    private fun mapArea(
        level: String,
        rs: ResultSet
    ): AreaDto {
        val kodeWilayah = rs.getString("kode_wilayah") ?: ""

        val kodeProvinsi =
            if (level == "provinsi") kodeWilayah else ""

        val kodeKabkot =
            if (level == "kabupaten") kodeWilayah else ""

        val kodeKecamatan =
            if (level == "kecamatan") kodeWilayah else ""

        val kodeKelurahanDesa =
            if (level == "desa") kodeWilayah else ""

        return AreaDto(
            level = level,

            kodeWilayah = kodeWilayah,
            namaWilayah = rs.getString("nama_wilayah"),

            kodeProvinsi = kodeProvinsi,
            kodeKabkot = kodeKabkot,
            kodeKecamatan = kodeKecamatan,
            kodeKelurahanDesa = kodeKelurahanDesa,

            namaProvinsi = rs.getString("nama_provinsi"),
            namaKabkot = rs.getString("nama_kabkot"),
            namaKecamatan = rs.getString("nama_kecamatan"),
            namaKelurahanDesa = rs.getString("nama_kelurahan_desa"),

            totalKeluarga = rs.getLong("total_keluarga"),
            totalKeluargaWilayah = rs.getLong("total_keluarga_wilayah"),
            totalRekomendasi = rs.getLong("total_rekomendasi"),
            persentaseKeluargaMembutuhkan = rs.getDouble("persentase_keluarga_membutuhkan"),

            bantuan = mapOf(
                "pip" to rs.getLong("pip"),
                "pkh" to rs.getLong("pkh"),
                "pbi_jk" to rs.getLong("pbi_jk"),
                "atensi" to rs.getLong("atensi"),
                "sembako" to rs.getLong("sembako"),
                "blt" to rs.getLong("blt"),
                "bsps" to rs.getLong("bsps")
            )
        )
    }

    private data class QueryParts(
        val whereClause: String,
        val params: List<Any>
    )

    private data class IndicatorAggRow(
        val kodeWilayah: String,
        val indikator: String,
        val jumlah: Long
    )
}