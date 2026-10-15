package backend.dto

import kotlinx.serialization.Serializable

@Serializable
data class SummaryDto(
    val totalKeluarga: Long,
    val totalRekomendasi: Long,
    val keluargaDesil15: Long,
    val keluargaDesil610: Long,
    val rekomendasiDesil15: Long,
    val rekomendasiDesil610: Long,
    val programTotalsD15: ProgramTotalsDto,
    val programTotalsD610: ProgramTotalsDto
)

@Serializable
data class ProgramTotalsDto(
    val pip: Long,
    val pkh: Long,
    val pbi_jk: Long,
    val atensi: Long,
    val sembako: Long,
    val blt: Long,
    val bsps: Long
)