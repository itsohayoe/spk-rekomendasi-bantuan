package backend.util

class FilterBuilder {

    data class QueryParts(
        val whereClause: String,
        val params: List<Any>
    )

    // =========================
    // FEATURE (dataset_feature)
    // =========================
    fun buildFeatureFilter(
        kodeProvinsi: String?,
        kodeKabkot: String?,
        kodeKecamatan: String?,
        kodeKelurahanDesa: String?,
        desil: List<Int>,
        alias: String = "f"
    ): QueryParts {

        val conditions = mutableListOf<String>()
        val params = mutableListOf<Any>()

        if (!kodeProvinsi.isNullOrBlank()) {
            conditions.add("TRIM($alias.kode_provinsi) = ?")
            params.add(kodeProvinsi.trim())
        }

        if (!kodeKabkot.isNullOrBlank()) {
            conditions.add("TRIM($alias.kode_kabupaten_kota) = ?")
            params.add(kodeKabkot.trim())
        }

        if (!kodeKecamatan.isNullOrBlank()) {
            conditions.add("TRIM($alias.kode_kecamatan) = ?")
            params.add(kodeKecamatan.trim())
        }

        if (!kodeKelurahanDesa.isNullOrBlank()) {
            conditions.add("TRIM($alias.kode_kelurahan_desa) = ?")
            params.add(kodeKelurahanDesa.trim())
        }

        if (desil.isNotEmpty()) {

            val placeholders = desil.joinToString(",") { "?" }

            conditions.add(
                "CAST($alias.desil_nasional AS INTEGER) IN ($placeholders)"
            )

            params.addAll(desil)
        }

        val whereClause =
            if (conditions.isEmpty()) {
                ""
            } else {
                "WHERE " + conditions.joinToString(" AND ")
            }

        return QueryParts(whereClause, params)
    }

    // =========================
    // REKOMENDASI (data_rekomendasi)
    // =========================
    fun buildRecomFilter(
        kodeProvinsi: String?,
        kodeKabkot: String?,
        kodeKecamatan: String?,
        kodeKelurahanDesa: String?,
        desil: List<Int>,
        prioritas: List<String>,
        alias: String = "r"
    ): QueryParts {

        val conditions = mutableListOf<String>()
        val params = mutableListOf<Any>()

        conditions.add("$alias.status = 1")

        if (!kodeProvinsi.isNullOrBlank()) {
            conditions.add("TRIM($alias.kode_provinsi) = ?")
            params.add(kodeProvinsi.trim())
        }

        if (!kodeKabkot.isNullOrBlank()) {
            conditions.add("TRIM($alias.kode_kabupaten_kota) = ?")
            params.add(kodeKabkot.trim())
        }

        if (!kodeKecamatan.isNullOrBlank()) {
            conditions.add("TRIM($alias.kode_kecamatan) = ?")
            params.add(kodeKecamatan.trim())
        }

        if (!kodeKelurahanDesa.isNullOrBlank()) {
            conditions.add("TRIM($alias.kode_kelurahan_desa) = ?")
            params.add(kodeKelurahanDesa.trim())
        }

        // DESIL SENGAJA TIDAK DIFILTER DI WHERE
        // karena summary card kiri/kanan ditangani dinamis di SummaryService

        if (prioritas.isNotEmpty()) {

            val mappedPrioritas = prioritas.map {

                when (it.trim().lowercase()) {

                    "sangat tinggi" -> 1
                    "tinggi" -> 2
                    "sedang" -> 3
                    "rendah" -> 4
                    "sangat rendah" -> 5

                    else -> it.toIntOrNull() ?: 0
                }
            }

            val placeholders = mappedPrioritas.joinToString(",") { "?" }

            conditions.add("CAST($alias.prioritas AS INTEGER) IN ($placeholders)")

            params.addAll(mappedPrioritas)
        }

        val whereClause =
            if (conditions.isEmpty()) {
                ""
            } else {
                "WHERE " + conditions.joinToString(" AND ")
            }

        return QueryParts(whereClause, params)
    }
}