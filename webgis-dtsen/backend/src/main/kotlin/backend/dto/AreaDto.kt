package backend.dto

import kotlinx.serialization.Serializable

@Serializable
data class DimensionIndicatorDto(
    val total: Long = 0,
    val indikator: Map<String, Long> = emptyMap()
)

@Serializable
data class AreaDto(
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

    val totalKeluarga: Long,
    val totalKeluargaWilayah: Long,
    val totalRekomendasi: Long,
    val persentaseKeluargaMembutuhkan: Double,

    val bantuan: Map<String, Long>,

    val deprivasiCount: Map<String, DimensionIndicatorDto> = emptyMap(),
    val bantuanIndikator: Map<String, Map<String, Long>> = emptyMap()
)