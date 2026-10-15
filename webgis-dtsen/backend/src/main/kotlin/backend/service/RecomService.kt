package backend.service

import backend.dto.PagedRecomDto
import backend.repo.RecomRepo

class RecomService(private val recomRepo: RecomRepo) {

    fun getRecommendations(
        kodeProvinsi: String?,
        kodeKabkot: String?,
        kodeKecamatan: String?,
        kodeKelurahanDesa: String?,
        keyword: String?,
        searchBy: String?,
        wilayahLevel: String?,
        desil: List<Int>,
        program: List<String>,
        status: List<String>,
        prioritas: List<String>,
        asesmen: List<String>,
        statusData: List<String>,
        page: Int,
        size: Int
    ): PagedRecomDto {
        return recomRepo.getFamilyRecommendations(
            kodeProvinsi = kodeProvinsi,
            kodeKabkot = kodeKabkot,
            kodeKecamatan = kodeKecamatan,
            kodeKelurahanDesa = kodeKelurahanDesa,
            keyword = keyword,
            searchBy = searchBy,
            wilayahLevel = wilayahLevel,
            desil = desil,
            program = program,
            status = status,
            prioritas = prioritas,
            asesmen = asesmen,
            statusData = statusData,
            page = page,
            size = size
        )
    }
}