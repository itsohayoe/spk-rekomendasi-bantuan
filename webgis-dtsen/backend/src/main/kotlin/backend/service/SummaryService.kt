package backend.service

import backend.dto.ProgramTotalsDto
import backend.dto.SummaryDto
import backend.util.FilterBuilder
import javax.sql.DataSource

class SummaryService(
    private val dataSource: DataSource
) {

    private val filterBuilder = FilterBuilder()

    fun getSummary(
        kodeProvinsi: String?,
        kodeKabkot: String?,
        kodeKecamatan: String?,
        kodeKelurahanDesa: String?,
        desil: List<Int>,
        prioritas: List<String>
    ): SummaryDto {

        val recomFilter = filterBuilder.buildRecomFilter(
            kodeProvinsi,
            kodeKabkot,
            kodeKecamatan,
            kodeKelurahanDesa,
            emptyList(),
            prioritas,
            "r"
        )

        val leftFiltered = desil.filter { it in 1..5 }

        val rightFiltered = desil.filter { it in 6..10 }

        val leftCondition =
            when {

                desil.isEmpty() ->
                    "CAST(desil AS INTEGER) BETWEEN 1 AND 5"

                leftFiltered.isEmpty() ->
                    "FALSE"

                else ->
                    "CAST(desil AS INTEGER) IN (${leftFiltered.joinToString(",")})"
            }

        val rightCondition =
            when {

                desil.isEmpty() ->
                    "CAST(desil AS INTEGER) BETWEEN 6 AND 10"

                rightFiltered.isEmpty() ->
                    "FALSE"

                else ->
                    "CAST(desil AS INTEGER) IN (${rightFiltered.joinToString(",")})"
            }

        val sql = """
            WITH base AS (
                SELECT
                    r.id_keluarga,
                    CAST(r.desil_nasional AS INTEGER) AS desil,
                    r.program
                FROM dtsen.data_rekomendasi r
                ${recomFilter.whereClause}
            )

            SELECT

            (
                COUNT(DISTINCT CASE
                    WHEN $leftCondition
                    THEN id_keluarga
                END)

                +

                COUNT(DISTINCT CASE
                    WHEN $rightCondition
                    THEN id_keluarga
                END)
            ) AS total_keluarga,

            (
                COUNT(*) FILTER (
                    WHERE $leftCondition
                )

                +

                COUNT(*) FILTER (
                    WHERE $rightCondition
                )
            ) AS total_rekomendasi,

            COUNT(DISTINCT CASE
                WHEN $leftCondition
                THEN id_keluarga
            END) AS keluarga_desil_15,

            COUNT(DISTINCT CASE
                WHEN $rightCondition
                THEN id_keluarga
            END) AS keluarga_desil_610,

            COUNT(*) FILTER (
                WHERE $leftCondition
            ) AS rekomendasi_desil_15,

            COUNT(*) FILTER (
                WHERE $rightCondition
            ) AS rekomendasi_desil_610,

            -- LEFT CARD

            SUM(
                CASE
                    WHEN $leftCondition
                    AND program = 'PIP'
                    THEN 1
                    ELSE 0
                END
            ) AS d15_pip,

            SUM(
                CASE
                    WHEN $leftCondition
                    AND program = 'PKH'
                    THEN 1
                    ELSE 0
                END
            ) AS d15_pkh,

            SUM(
                CASE
                    WHEN $leftCondition
                    AND program = 'PBI JK'
                    THEN 1
                    ELSE 0
                END
            ) AS d15_pbi_jk,

            SUM(
                CASE
                    WHEN $leftCondition
                    AND program = 'ATENSI'
                    THEN 1
                    ELSE 0
                END
            ) AS d15_atensi,

            SUM(
                CASE
                    WHEN $leftCondition
                    AND program = 'SEMBAKO'
                    THEN 1
                    ELSE 0
                END
            ) AS d15_sembako,

            SUM(
                CASE
                    WHEN $leftCondition
                    AND program = 'BLT'
                    THEN 1
                    ELSE 0
                END
            ) AS d15_blt,

            SUM(
                CASE
                    WHEN $leftCondition
                    AND program = 'BSPS'
                    THEN 1
                    ELSE 0
                END
            ) AS d15_bsps,

            -- RIGHT CARD

            SUM(
                CASE
                    WHEN $rightCondition
                    AND program = 'PIP'
                    THEN 1
                    ELSE 0
                END
            ) AS d610_pip,

            SUM(
                CASE
                    WHEN $rightCondition
                    AND program = 'PKH'
                    THEN 1
                    ELSE 0
                END
            ) AS d610_pkh,

            SUM(
                CASE
                    WHEN $rightCondition
                    AND program = 'PBI JK'
                    THEN 1
                    ELSE 0
                END
            ) AS d610_pbi_jk,

            SUM(
                CASE
                    WHEN $rightCondition
                    AND program = 'ATENSI'
                    THEN 1
                    ELSE 0
                END
            ) AS d610_atensi,

            SUM(
                CASE
                    WHEN $rightCondition
                    AND program = 'SEMBAKO'
                    THEN 1
                    ELSE 0
                END
            ) AS d610_sembako,

            SUM(
                CASE
                    WHEN $rightCondition
                    AND program = 'BLT'
                    THEN 1
                    ELSE 0
                END
            ) AS d610_blt,

            SUM(
                CASE
                    WHEN $rightCondition
                    AND program = 'BSPS'
                    THEN 1
                    ELSE 0
                END
            ) AS d610_bsps

            FROM base
        """.trimIndent()

        dataSource.connection.use { conn ->

            conn.prepareStatement(sql).use { stmt ->

                recomFilter.params.forEachIndexed { index, value ->

                    when (value) {

                        is Int ->
                            stmt.setInt(index + 1, value)

                        else ->
                            stmt.setString(index + 1, value.toString())
                    }
                }

                stmt.executeQuery().use { rs ->

                    rs.next()

                    return SummaryDto(

                        totalKeluarga = rs.getLong("total_keluarga"),

                        totalRekomendasi = rs.getLong("total_rekomendasi"),

                        keluargaDesil15 = rs.getLong("keluarga_desil_15"),

                        keluargaDesil610 = rs.getLong("keluarga_desil_610"),

                        rekomendasiDesil15 = rs.getLong("rekomendasi_desil_15"),

                        rekomendasiDesil610 = rs.getLong("rekomendasi_desil_610"),

                        programTotalsD15 = ProgramTotalsDto(
                            pip = rs.getLong("d15_pip"),
                            pkh = rs.getLong("d15_pkh"),
                            pbi_jk = rs.getLong("d15_pbi_jk"),
                            atensi = rs.getLong("d15_atensi"),
                            sembako = rs.getLong("d15_sembako"),
                            blt = rs.getLong("d15_blt"),
                            bsps = rs.getLong("d15_bsps")
                        ),

                        programTotalsD610 = ProgramTotalsDto(
                            pip = rs.getLong("d610_pip"),
                            pkh = rs.getLong("d610_pkh"),
                            pbi_jk = rs.getLong("d610_pbi_jk"),
                            atensi = rs.getLong("d610_atensi"),
                            sembako = rs.getLong("d610_sembako"),
                            blt = rs.getLong("d610_blt"),
                            bsps = rs.getLong("d610_bsps")
                        )
                    )
                }
            }
        }
    }
}