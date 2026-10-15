package backend.service

import backend.dto.RegionOptionDto
import backend.repo.RegionRepo

class RegionService(private val regionRepo: RegionRepo) {

    fun getProvinsi(): List<RegionOptionDto> {
        return regionRepo.getProvinsi()
    }

    fun getKabupaten(kodeProvinsi: String): List<RegionOptionDto> {
        return regionRepo.getKabupaten(kodeProvinsi)
    }

    fun getKecamatan(kodeKabkot: String): List<RegionOptionDto> {
        return regionRepo.getKecamatan(kodeKabkot)
    }

    fun getDesa(kodeKecamatan: String): List<RegionOptionDto> {
        return regionRepo.getDesa(kodeKecamatan)
    }
}