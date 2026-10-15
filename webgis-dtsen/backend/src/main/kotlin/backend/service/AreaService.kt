package backend.service

import backend.dto.AreaDto
import backend.repo.AreaRepo

class AreaService(private val areaRepo: AreaRepo) {

    fun getAreas(
        level: String,
        kodeProvinsi: String?,
        kodeKabkot: String?,
        kodeKecamatan: String?,
        kodeKelurahanDesa: String?,
        desil: List<Int>,
        prioritas: List<String>,
        asesmen: List<String>
    ): List<AreaDto> {
        return areaRepo.getAreaAggregation(
            level = level,
            kodeProvinsi = kodeProvinsi,
            kodeKabkot = kodeKabkot,
            kodeKecamatan = kodeKecamatan,
            kodeKelurahanDesa = kodeKelurahanDesa,
            desil = desil,
            prioritas = prioritas,
            asesmen = asesmen
        )
    }
}