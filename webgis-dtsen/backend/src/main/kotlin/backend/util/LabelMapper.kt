package backend.util

object LabelMapper {

    fun desilLabel(value: Int?): String {
        return value?.let { "Desil $it" } ?: "Belum Diketahui"
    }

    fun statusProgramLabel(value: Int?): String {
        return when (value) {
            1 -> "Direkomendasikan"
            0 -> "Tidak Direkomendasikan"
            null -> "Dipertimbangkan"
            else -> "Dipertimbangkan"
        }
    }

    fun prioritasLabel(value: Int?): String {
        return when (value) {
            1 -> "Sangat Tinggi"
            2 -> "Tinggi"
            3 -> "Sedang"
            4 -> "Rendah"
            5 -> "Sangat Rendah"
            null -> "Belum Ditetapkan"
            else -> "Belum Ditetapkan"
        }
    }

    fun asesmenLabel(value: Int?): String {
        return when (value) {
            1 -> "Perlu Asesmen"
            0 -> "Tidak Perlu"
            null -> "Belum Ditetapkan"
            else -> "Belum Ditetapkan"
        }
    }

    fun statusDataLabel(value: Int?): String {
        return when (value) {
            1 -> "Data Lengkap"
            0 -> "Tidak Lengkap"
            null -> "Tidak Lengkap"
            else -> "Tidak Lengkap"
        }
    }

    fun indikatorDeprivasiLabel(value: String?): String {
        return if (value.isNullOrBlank()) "-" else value
    }
}