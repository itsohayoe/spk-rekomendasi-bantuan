package backend.repo

import backend.dto.RegionOptionDto
import javax.sql.DataSource

class RegionRepo(private val dataSource: DataSource) {

    fun getProvinsi(): List<RegionOptionDto> {
        val sql = """
            SELECT DISTINCT
                kode_provinsi AS kode,
                provinsi AS nama
            FROM dtsen.ref_wilayah
            ORDER BY provinsi
        """.trimIndent()

        return queryOptions(sql)
    }

    fun getKabupaten(kodeProvinsi: String): List<RegionOptionDto> {
        val sql = """
            SELECT DISTINCT
                kode_kabupaten_kota AS kode,
                kabupaten_kota AS nama
            FROM dtsen.ref_wilayah
            WHERE kode_provinsi = ?
            ORDER BY kabupaten_kota
        """.trimIndent()

        return queryOptions(sql, listOf(kodeProvinsi))
    }

    fun getKecamatan(kodeKabkot: String): List<RegionOptionDto> {
        val sql = """
            SELECT DISTINCT
                kode_kecamatan AS kode,
                kecamatan AS nama
            FROM dtsen.ref_wilayah
            WHERE kode_kabupaten_kota = ?
            ORDER BY kecamatan
        """.trimIndent()

        return queryOptions(sql, listOf(kodeKabkot))
    }

    fun getDesa(kodeKecamatan: String): List<RegionOptionDto> {
        val sql = """
            SELECT DISTINCT
                kode_kelurahan_desa AS kode,
                kelurahan_desa AS nama
            FROM dtsen.ref_wilayah
            WHERE kode_kecamatan = ?
            ORDER BY kelurahan_desa
        """.trimIndent()

        return queryOptions(sql, listOf(kodeKecamatan))
    }

    private fun queryOptions(
        sql: String,
        params: List<String> = emptyList()
    ): List<RegionOptionDto> {
        dataSource.connection.use { conn ->
            conn.prepareStatement(sql).use { stmt ->
                params.forEachIndexed { index, value ->
                    stmt.setString(index + 1, value)
                }

                stmt.executeQuery().use { rs ->
                    val result = mutableListOf<RegionOptionDto>()

                    while (rs.next()) {
                        result.add(
                            RegionOptionDto(
                                kode = rs.getString("kode"),
                                nama = rs.getString("nama")
                            )
                        )
                    }

                    return result
                }
            }
        }
    }
}