package backend.dto

import kotlinx.serialization.Serializable

@Serializable
data class ConcentDto(
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

    val nilai: Double,
    val kategori: String,

    val jumlahTerpilih: Long,
    val totalPembanding: Long,
    val proporsiWilayah: Double,
    val proporsiKeseluruhan: Double,

    val reference: String = "nasional",
    val jumlahAcuanTerpilih: Long = 0,
    val totalAcuanPembanding: Long = 0
)