package backend.repo

import backend.dto.PagedRecomDto
import backend.dto.RecomDto
import backend.util.LabelMapper
import java.sql.PreparedStatement
import java.sql.ResultSet
import javax.sql.DataSource
import kotlin.math.ceil

class RecomRepo(
    private val dataSource: DataSource
) {

    fun getFamilyRecommendations(
        kodeProvinsi: String?,
        kodeKabkot: String?,
        kodeKecamatan: String?,
        kodeKelurahanDesa: String?,
        keyword: String?,
        searchBy: String?,
        wilayahLevel: String?,
        desil: List<Int>,
        program: List<String>,
        status: List<String>,
        prioritas: List<String>,
        asesmen: List<String>,
        statusData: List<String>,
        page: Int,
        size: Int
    ): PagedRecomDto {
        val safePage = page.coerceAtLeast(1)
        val safeSize = size.coerceIn(10, 500)
        val offset = (safePage - 1) * safeSize

        val queryParts = buildWhereClause(
            kodeProvinsi = kodeProvinsi,
            kodeKabkot = kodeKabkot,
            kodeKecamatan = kodeKecamatan,
            kodeKelurahanDesa = kodeKelurahanDesa,
            keyword = keyword,
            searchBy = searchBy,
            wilayahLevel = wilayahLevel,
            desil = desil,
            program = program,
            status = status,
            prioritas = prioritas,
            asesmen = asesmen,
            statusData = statusData
        )

        val totalItems = countRecommendations(queryParts)

        val sql = """
            SELECT
                r.id_rekomendasi,
                r.id_keluarga,

                TRIM(r.kode_provinsi) AS kode_provinsi,
                w.provinsi,

                TRIM(r.kode_kabupaten_kota) AS kode_kabupaten_kota,
                w.kabupaten_kota,

                TRIM(r.kode_kecamatan) AS kode_kecamatan,
                w.kecamatan,

                TRIM(r.kode_kelurahan_desa) AS kode_kelurahan_desa,
                w.kelurahan_desa,

                r.desil_nasional,
                TRIM(r.program) AS program,
                r.status,
                r.skor,
                r.prioritas,
                r.asesmen,
                r.is_complete,
                r.indikator_deprivasi
            FROM dtsen.data_rekomendasi r
            LEFT JOIN dtsen.ref_wilayah w
                ON r.kode_kelurahan_desa = w.kode_kelurahan_desa
            ${queryParts.whereClause}
            ORDER BY
                r.skor DESC NULLS LAST,
                TRIM(r.kode_provinsi),
                TRIM(r.kode_kabupaten_kota),
                r.id_keluarga,
                TRIM(r.program)
            LIMIT ?
            OFFSET ?
        """.trimIndent()

        dataSource.connection.use { conn ->
            conn.prepareStatement(sql).use { stmt ->
                setParams(stmt, queryParts.params)

                val limitIndex = queryParts.params.size + 1
                val offsetIndex = queryParts.params.size + 2

                stmt.setInt(limitIndex, safeSize)
                stmt.setInt(offsetIndex, offset)

                stmt.executeQuery().use { rs ->
                    val items = mutableListOf<RecomDto>()

                    while (rs.next()) {
                        val desilValue = getNullableInt(rs, "desil_nasional")
                        val statusValue = getNullableInt(rs, "status")
                        val prioritasValue = getNullableInt(rs, "prioritas")
                        val asesmenValue = getNullableInt(rs, "asesmen")
                        val isCompleteValue = getNullableInt(rs, "is_complete")
                        val indikatorValue = rs.getString("indikator_deprivasi")

                        items.add(
                            RecomDto(
                                idRekomendasi = rs.getLong("id_rekomendasi"),
                                idKeluarga = rs.getString("id_keluarga"),

                                kodeProvinsi = rs.getString("kode_provinsi"),
                                kodeKabkot = rs.getString("kode_kabupaten_kota"),
                                kodeKecamatan = rs.getString("kode_kecamatan"),
                                kodeKelurahanDesa = rs.getString("kode_kelurahan_desa"),

                                namaProvinsi = rs.getString("provinsi"),
                                namaKabkot = rs.getString("kabupaten_kota"),
                                namaKecamatan = rs.getString("kecamatan"),
                                namaKelurahanDesa = rs.getString("kelurahan_desa"),

                                desil = desilValue,
                                desilLabel = LabelMapper.desilLabel(desilValue),

                                program = formatProgram(rs.getString("program")),

                                status = statusValue,
                                statusProgram = LabelMapper.statusProgramLabel(statusValue),

                                skor = getNullableDouble(rs, "skor"),

                                prioritasKode = prioritasValue,
                                prioritas = LabelMapper.prioritasLabel(prioritasValue),

                                asesmenKode = asesmenValue,
                                catatanAsesmen = LabelMapper.asesmenLabel(asesmenValue),

                                isComplete = isCompleteValue,
                                kelengkapanData = LabelMapper.statusDataLabel(isCompleteValue),

                                indikatorDeprivasi = splitIndikatorDeprivasi(
                                    LabelMapper.indikatorDeprivasiLabel(indikatorValue)
                                )
                            )
                        )
                    }

                    val totalPages = if (totalItems == 0L) {
                        0L
                    } else {
                        ceil(totalItems.toDouble() / safeSize.toDouble()).toLong()
                    }

                    return PagedRecomDto(
                        items = items,
                        page = safePage,
                        size = safeSize,
                        totalItems = totalItems,
                        totalPages = totalPages
                    )
                }
            }
        }
    }

    private fun countRecommendations(queryParts: QueryParts): Long {
        val sql = """
            SELECT COUNT(*) AS total
            FROM dtsen.data_rekomendasi r
            LEFT JOIN dtsen.ref_wilayah w
                ON r.kode_kelurahan_desa = w.kode_kelurahan_desa
            ${queryParts.whereClause}
        """.trimIndent()

        dataSource.connection.use { conn ->
            conn.prepareStatement(sql).use { stmt ->
                setParams(stmt, queryParts.params)

                stmt.executeQuery().use { rs ->
                    rs.next()
                    return rs.getLong("total")
                }
            }
        }
    }

    private fun buildWhereClause(
        kodeProvinsi: String?,
        kodeKabkot: String?,
        kodeKecamatan: String?,
        kodeKelurahanDesa: String?,
        keyword: String?,
        searchBy: String?,
        wilayahLevel: String?,
        desil: List<Int>,
        program: List<String>,
        status: List<String>,
        prioritas: List<String>,
        asesmen: List<String>,
        statusData: List<String>
    ): QueryParts {
        val conditions = mutableListOf<String>()
        val params = mutableListOf<Any>()

        if (!kodeProvinsi.isNullOrBlank()) {
            conditions.add("TRIM(r.kode_provinsi) = ?")
            params.add(kodeProvinsi.trim())
        }

        if (!kodeKabkot.isNullOrBlank()) {
            conditions.add("TRIM(r.kode_kabupaten_kota) = ?")
            params.add(kodeKabkot.trim())
        }

        if (!kodeKecamatan.isNullOrBlank()) {
            conditions.add("TRIM(r.kode_kecamatan) = ?")
            params.add(kodeKecamatan.trim())
        }

        if (!kodeKelurahanDesa.isNullOrBlank()) {
            conditions.add("TRIM(r.kode_kelurahan_desa) = ?")
            params.add(kodeKelurahanDesa.trim())
        }

        if (!keyword.isNullOrBlank()) {
            val keywordValue = "%${keyword.trim().lowercase()}%"

            val wilayahColumn = when (wilayahLevel?.trim()?.lowercase()) {
                "provinsi" -> "w.provinsi"
                "kecamatan" -> "w.kecamatan"
                "desa", "kelurahan", "kelurahan_desa" -> "w.kelurahan_desa"
                else -> "w.kabupaten_kota"
            }

            conditions.add(
                """
                (
                    LOWER(r.id_keluarga) LIKE ?
                    OR LOWER($wilayahColumn) LIKE ?
                )
                """.trimIndent()
            )

            params.add(keywordValue)
            params.add(keywordValue)
        }

        if (desil.isNotEmpty()) {
            val placeholders = desil.joinToString(",") { "?" }
            conditions.add("r.desil_nasional IN ($placeholders)")
            params.addAll(desil)
        }

        if (program.isNotEmpty()) {
            val placeholders = program.joinToString(",") { "?" }
            conditions.add("UPPER(TRIM(r.program)) IN ($placeholders)")
            params.addAll(program.map { it.trim().uppercase() })
        }

        val statusConditions = buildStatusConditions(status)
        if (statusConditions.isNotEmpty()) {
            conditions.add(statusConditions)
        }

        val prioritasConditions = buildPrioritasConditions(prioritas)
        if (prioritasConditions.isNotEmpty()) {
            conditions.add(prioritasConditions.condition)
            params.addAll(prioritasConditions.params)
        }

        val asesmenConditions = buildAsesmenConditions(asesmen)
        if (asesmenConditions.isNotEmpty()) {
            conditions.add(asesmenConditions.condition)
            params.addAll(asesmenConditions.params)
        }

        val statusDataConditions = buildStatusDataConditions(statusData)
        if (statusDataConditions.isNotEmpty()) {
            conditions.add(statusDataConditions.condition)
            params.addAll(statusDataConditions.params)
        }

        val whereClause = if (conditions.isEmpty()) {
            ""
        } else {
            "WHERE ${conditions.joinToString(" AND ")}"
        }

        return QueryParts(
            whereClause = whereClause,
            params = params
        )
    }

    private fun buildStatusConditions(status: List<String>): String {
        if (status.isEmpty()) return ""

        val normalized = status.map { it.trim().lowercase() }
        val statusConditions = mutableListOf<String>()

        if (normalized.any { it == "direkomendasikan" || it == "1" }) {
            statusConditions.add("r.status = 1")
        }

        if (normalized.any { it == "dipertimbangkan" || it == "null" }) {
            statusConditions.add("r.status IS NULL")
        }

        if (normalized.any { it == "tidak direkomendasikan" || it == "0" }) {
            statusConditions.add("r.status = 0")
        }

        return if (statusConditions.isEmpty()) {
            ""
        } else {
            statusConditions.joinToString(" OR ", prefix = "(", postfix = ")")
        }
    }

    private fun buildPrioritasConditions(prioritas: List<String>): FilterCondition {
        if (prioritas.isEmpty()) return FilterCondition.empty()

        val codes = mutableListOf<Int>()
        var includeNull = false

        prioritas.forEach { item ->
            when (item.trim().lowercase()) {
                "sangat tinggi", "1" -> codes.add(1)
                "tinggi", "2" -> codes.add(2)
                "sedang", "3" -> codes.add(3)
                "rendah", "4" -> codes.add(4)
                "sangat rendah", "5" -> codes.add(5)
                "belum_ditetapkan", "belum ditetapkan", "tidak ditetapkan", "null" -> includeNull = true
            }
        }

        if (codes.isEmpty() && !includeNull) return FilterCondition.empty()

        val parts = mutableListOf<String>()
        val params = mutableListOf<Any>()

        if (codes.isNotEmpty()) {
            val placeholders = codes.joinToString(",") { "?" }
            parts.add("r.prioritas IN ($placeholders)")
            params.addAll(codes)
        }

        if (includeNull) {
            parts.add("r.prioritas IS NULL")
        }

        return FilterCondition(
            condition = parts.joinToString(" OR ", prefix = "(", postfix = ")"),
            params = params
        )
    }

    private fun buildAsesmenConditions(asesmen: List<String>): FilterCondition {
        if (asesmen.isEmpty()) return FilterCondition.empty()

        val codes = mutableListOf<Int>()
        var includeNull = false

        asesmen.forEach { item ->
            when (item.trim().lowercase()) {
                "perlu", "perlu asesmen", "1" -> codes.add(1)
                "tidak_perlu", "tidak perlu", "0" -> codes.add(0)
                "belum_ditetapkan", "belum ditetapkan", "tidak ditetapkan", "null" -> includeNull = true
            }
        }

        if (codes.isEmpty() && !includeNull) return FilterCondition.empty()

        val parts = mutableListOf<String>()
        val params = mutableListOf<Any>()

        if (codes.isNotEmpty()) {
            val placeholders = codes.joinToString(",") { "?" }
            parts.add("r.asesmen IN ($placeholders)")
            params.addAll(codes)
        }

        if (includeNull) {
            parts.add("r.asesmen IS NULL")
        }

        return FilterCondition(
            condition = parts.joinToString(" OR ", prefix = "(", postfix = ")"),
            params = params
        )
    }

    private fun buildStatusDataConditions(statusData: List<String>): FilterCondition {
        if (statusData.isEmpty()) return FilterCondition.empty()

        val codes = mutableListOf<Int>()

        statusData.forEach { item ->
            when (item.trim().lowercase()) {
                "lengkap", "data lengkap", "1" -> codes.add(1)
                "tidak_lengkap", "tidak lengkap", "0" -> codes.add(0)
            }
        }

        if (codes.isEmpty()) return FilterCondition.empty()

        val placeholders = codes.joinToString(",") { "?" }

        return FilterCondition(
            condition = "r.is_complete IN ($placeholders)",
            params = codes
        )
    }

    private fun setParams(
        stmt: PreparedStatement,
        params: List<Any>
    ) {
        params.forEachIndexed { index, value ->
            when (value) {
                is Int -> stmt.setInt(index + 1, value)
                is Long -> stmt.setLong(index + 1, value)
                is Double -> stmt.setDouble(index + 1, value)
                else -> stmt.setString(index + 1, value.toString())
            }
        }
    }

    private fun getNullableInt(rs: ResultSet, column: String): Int? {
        val value = rs.getObject(column) ?: return null
        return (value as Number).toInt()
    }

    private fun getNullableDouble(rs: ResultSet, column: String): Double? {
        val value = rs.getObject(column) ?: return null
        return (value as Number).toDouble()
    }

    private fun formatProgram(program: String): String {
        return program.trim().uppercase()
    }

    private fun splitIndikatorDeprivasi(value: String): List<String> {
        if (value.isBlank() || value == "-") return emptyList()

        return value
            .split(",")
            .map { it.trim() }
            .filter { it.isNotBlank() }
    }

    private data class QueryParts(
        val whereClause: String,
        val params: List<Any>
    )

    private data class FilterCondition(
        val condition: String,
        val params: List<Any>
    ) {
        fun isNotEmpty(): Boolean {
            return condition.isNotBlank()
        }

        companion object {
            fun empty(): FilterCondition {
                return FilterCondition("", emptyList())
            }
        }
    }
}