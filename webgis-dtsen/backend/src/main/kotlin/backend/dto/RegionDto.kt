package backend.dto

import kotlinx.serialization.Serializable

@Serializable
data class RegionOptionDto(
    val kode: String,
    val nama: String
)