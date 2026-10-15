package backend.service

import backend.dto.AreaDto
import backend.dto.ConcentDto
import backend.repo.ConcentRepo
import kotlin.math.round

class ConcentService(
    private val concentRepo: ConcentRepo
) {

    fun getConcentration(
        level: String,
        kodeProvinsi: String?,
        kodeKabkot: String?,
        kodeKecamatan: String?,
        kodeKelurahanDesa: String?,
        type: String,
        program: String,
        dimension: String,
        reference: String
    ): List<ConcentDto> {
        val normalizedType = type.trim().lowercase()
        val normalizedReference = normalizeReference(level, reference)

        return if (normalizedType == "dimensi" || normalizedType == "dimension") {
            calculateDimensionLq(
                level = level,
                kodeProvinsi = kodeProvinsi,
                kodeKabkot = kodeKabkot,
                kodeKecamatan = kodeKecamatan,
                kodeKelurahanDesa = kodeKelurahanDesa,
                dimension = dimension,
                reference = normalizedReference
            )
        } else {
            calculateProgramLq(
                level = level,
                kodeProvinsi = kodeProvinsi,
                kodeKabkot = kodeKabkot,
                kodeKecamatan = kodeKecamatan,
                kodeKelurahanDesa = kodeKelurahanDesa,
                program = program,
                reference = normalizedReference
            )
        }
    }

    private fun calculateProgramLq(
        level: String,
        kodeProvinsi: String?,
        kodeKabkot: String?,
        kodeKecamatan: String?,
        kodeKelurahanDesa: String?,
        program: String,
        reference: String
    ): List<ConcentDto> {
        val areas = concentRepo.getBaseAreaData(
            level = level,
            kodeProvinsi = kodeProvinsi,
            kodeKabkot = kodeKabkot,
            kodeKecamatan = kodeKecamatan,
            kodeKelurahanDesa = kodeKelurahanDesa
        )

        val programKey = normalizeKey(program)

        val groups = areas.groupBy { area ->
            getReferenceKey(
                kodeWilayah = area.kodeWilayah,
                reference = reference
            )
        }

        return areas.map { area ->
            val referenceRows =
                groups[getReferenceKey(area.kodeWilayah, reference)].orEmpty()

            val totalProgramAcuan =
                referenceRows.sumOf { it.bantuan[programKey] ?: 0L }

            val totalRecomAcuan =
                referenceRows.sumOf { it.totalRekomendasi }

            val totalProgramArea = area.bantuan[programKey] ?: 0L
            val totalRecomArea = area.totalRekomendasi

            val proporsiWilayah = percentage(totalProgramArea, totalRecomArea)
            val proporsiKeseluruhan = percentage(totalProgramAcuan, totalRecomAcuan)

            val lq =
                if (
                    totalProgramArea == 0L ||
                    totalRecomArea == 0L ||
                    totalProgramAcuan == 0L ||
                    totalRecomAcuan == 0L
                ) {
                    0.0
                } else {
                    (totalProgramArea.toDouble() / totalRecomArea.toDouble()) /
                        (totalProgramAcuan.toDouble() / totalRecomAcuan.toDouble())
                }

            buildDtoFromArea(
                area = area,
                level = level,
                nilai = round(lq * 100.0) / 100.0,
                kategori = getKategori(lq),
                jumlahTerpilih = totalProgramArea,
                totalPembanding = totalRecomArea,
                proporsiWilayah = proporsiWilayah,
                proporsiKeseluruhan = proporsiKeseluruhan,
                reference = reference,
                jumlahAcuanTerpilih = totalProgramAcuan,
                totalAcuanPembanding = totalRecomAcuan
            )
        }
    }

    private fun calculateDimensionLq(
        level: String,
        kodeProvinsi: String?,
        kodeKabkot: String?,
        kodeKecamatan: String?,
        kodeKelurahanDesa: String?,
        dimension: String,
        reference: String
    ): List<ConcentDto> {
        val rows = concentRepo.getDimensionBaseData(
            level = level,
            kodeProvinsi = kodeProvinsi,
            kodeKabkot = kodeKabkot,
            kodeKecamatan = kodeKecamatan,
            kodeKelurahanDesa = kodeKelurahanDesa
        )

        val dimensionKey = normalizeDimension(dimension)

        val groups = rows.groupBy { row ->
            getReferenceKey(
                kodeWilayah = row.kodeWilayah,
                reference = reference
            )
        }

        return rows.map { row ->
            val referenceRows =
                groups[getReferenceKey(row.kodeWilayah, reference)].orEmpty()

            val totalDimensionAcuan =
                referenceRows.sumOf { getDimensionValue(it, dimensionKey) }

            val totalAllDimensionAcuan =
                referenceRows.sumOf { it.totalDeprivasi }

            val totalDimensionArea = getDimensionValue(row, dimensionKey)
            val totalAreaDimension = row.totalDeprivasi

            val proporsiWilayah = percentage(totalDimensionArea, totalAreaDimension)
            val proporsiKeseluruhan = percentage(totalDimensionAcuan, totalAllDimensionAcuan)

            val lq =
                if (
                    totalDimensionArea == 0L ||
                    totalAreaDimension == 0L ||
                    totalDimensionAcuan == 0L ||
                    totalAllDimensionAcuan == 0L
                ) {
                    0.0
                } else {
                    (totalDimensionArea.toDouble() / totalAreaDimension.toDouble()) /
                        (totalDimensionAcuan.toDouble() / totalAllDimensionAcuan.toDouble())
                }

            buildDtoFromDimensionRow(
                row = row,
                nilai = round(lq * 100.0) / 100.0,
                kategori = getKategori(lq),
                jumlahTerpilih = totalDimensionArea,
                totalPembanding = totalAreaDimension,
                proporsiWilayah = proporsiWilayah,
                proporsiKeseluruhan = proporsiKeseluruhan,
                reference = reference,
                jumlahAcuanTerpilih = totalDimensionAcuan,
                totalAcuanPembanding = totalAllDimensionAcuan
            )
        }
    }

    private fun getDimensionValue(
        row: ConcentRepo.DimensionAggRow,
        dimensionKey: String
    ): Long {
        return when (dimensionKey) {
            "pendidikan", "education" -> row.pendidikan
            "kesehatan", "health" -> row.kesehatan
            "perumahan", "housing" -> row.perumahan
            "ekonomi", "economy" -> row.ekonomi
            else -> 0L
        }
    }

    private fun buildDtoFromArea(
        area: AreaDto,
        level: String,
        nilai: Double,
        kategori: String,
        jumlahTerpilih: Long,
        totalPembanding: Long,
        proporsiWilayah: Double,
        proporsiKeseluruhan: Double,
        reference: String,
        jumlahAcuanTerpilih: Long,
        totalAcuanPembanding: Long
    ): ConcentDto {
        return ConcentDto(
            level = level,

            kodeWilayah = area.kodeWilayah,
            namaWilayah = area.namaWilayah,

            kodeProvinsi = area.kodeProvinsi,
            kodeKabkot = area.kodeKabkot,
            kodeKecamatan = area.kodeKecamatan,
            kodeKelurahanDesa = area.kodeKelurahanDesa,

            namaProvinsi = area.namaProvinsi,
            namaKabkot = area.namaKabkot,
            namaKecamatan = area.namaKecamatan,
            namaKelurahanDesa = area.namaKelurahanDesa,

            nilai = nilai,
            kategori = kategori,

            jumlahTerpilih = jumlahTerpilih,
            totalPembanding = totalPembanding,
            proporsiWilayah = proporsiWilayah,
            proporsiKeseluruhan = proporsiKeseluruhan,

            reference = reference,
            jumlahAcuanTerpilih = jumlahAcuanTerpilih,
            totalAcuanPembanding = totalAcuanPembanding
        )
    }

    private fun buildDtoFromDimensionRow(
        row: ConcentRepo.DimensionAggRow,
        nilai: Double,
        kategori: String,
        jumlahTerpilih: Long,
        totalPembanding: Long,
        proporsiWilayah: Double,
        proporsiKeseluruhan: Double,
        reference: String,
        jumlahAcuanTerpilih: Long,
        totalAcuanPembanding: Long
    ): ConcentDto {
        return ConcentDto(
            level = row.level,

            kodeWilayah = row.kodeWilayah,
            namaWilayah = row.namaWilayah,

            kodeProvinsi = row.kodeProvinsi,
            kodeKabkot = row.kodeKabkot,
            kodeKecamatan = row.kodeKecamatan,
            kodeKelurahanDesa = row.kodeKelurahanDesa,

            namaProvinsi = row.namaProvinsi,
            namaKabkot = row.namaKabkot,
            namaKecamatan = row.namaKecamatan,
            namaKelurahanDesa = row.namaKelurahanDesa,

            nilai = nilai,
            kategori = kategori,

            jumlahTerpilih = jumlahTerpilih,
            totalPembanding = totalPembanding,
            proporsiWilayah = proporsiWilayah,
            proporsiKeseluruhan = proporsiKeseluruhan,

            reference = reference,
            jumlahAcuanTerpilih = jumlahAcuanTerpilih,
            totalAcuanPembanding = totalAcuanPembanding
        )
    }

    private fun percentage(part: Long, total: Long): Double {
        if (total == 0L) return 0.0
        return round((part.toDouble() / total.toDouble()) * 10000.0) / 100.0
    }

    private fun normalizeKey(value: String): String {
        return value
            .trim()
            .lowercase()
            .replace(" ", "_")
            .replace("-", "_")
    }

    private fun normalizeDimension(value: String): String {
        return when (normalizeKey(value)) {
            "education" -> "pendidikan"
            "health" -> "kesehatan"
            "housing" -> "perumahan"
            "economy" -> "ekonomi"
            else -> normalizeKey(value)
        }
    }

    private fun normalizeReference(level: String, reference: String): String {
        val normalized = normalizeKey(reference)

        val allowed =
            when (normalizeKey(level)) {
                "provinsi" -> setOf("nasional")
                "kabupaten" -> setOf("provinsi", "nasional")
                "kecamatan" -> setOf("kabupaten", "provinsi", "nasional")
                "desa" -> setOf("kecamatan", "kabupaten", "provinsi", "nasional")
                else -> setOf("nasional")
            }

        return if (normalized in allowed) normalized else "nasional"
    }

    private fun getReferenceKey(
        kodeWilayah: String,
        reference: String
    ): String {
        val kode = normalizeKodeWilayah(kodeWilayah)

        return when (reference) {
            "kecamatan" -> kode.take(7)
            "kabupaten" -> kode.take(4)
            "provinsi" -> kode.take(2)
            else -> "nasional"
        }
    }

    private fun normalizeKodeWilayah(value: String): String {
        return value
            .trim()
            .replace(".", "")
    }

    private fun getKategori(value: Double): String {
        return when {
            value >= 1.25 -> "Tinggi"
            value >= 1.05 -> "Cukup Tinggi"
            value >= 0.95 -> "Sedang"
            value >= 0.75 -> "Cukup Rendah"
            else -> "Rendah"
        }
    }
}