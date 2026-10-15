package backend.dto

import kotlinx.serialization.Serializable

@Serializable
data class RecomDto(
    val idRekomendasi: Long,
    val idKeluarga: String,

    val kodeProvinsi: String?,
    val kodeKabkot: String?,
    val kodeKecamatan: String?,
    val kodeKelurahanDesa: String?,

    val namaProvinsi: String?,
    val namaKabkot: String?,
    val namaKecamatan: String?,
    val namaKelurahanDesa: String?,

    val desil: Int?,
    val desilLabel: String,

    val program: String,

    val status: Int?,
    val statusProgram: String,

    val skor: Double?,

    val prioritasKode: Int?,
    val prioritas: String,

    val asesmenKode: Int?,
    val catatanAsesmen: String,

    val isComplete: Int?,
    val kelengkapanData: String,

    val indikatorDeprivasi: List<String>
)

@Serializable
data class PagedRecomDto(
    val items: List<RecomDto>,
    val page: Int,
    val size: Int,
    val totalItems: Long,
    val totalPages: Long
)