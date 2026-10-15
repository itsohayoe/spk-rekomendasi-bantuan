package backend.route

import backend.service.RecomService
import io.ktor.server.application.call
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get

fun Route.recomRoute(recomService: RecomService) {
    get("/recommendations") {
        val kodeProvinsi = call.request.queryParameters["kode_provinsi"]
        val kodeKabkot = call.request.queryParameters["kode_kabkot"]
        val kodeKecamatan = call.request.queryParameters["kode_kecamatan"]
        val kodeKelurahanDesa = call.request.queryParameters["kode_kelurahan_desa"]

        val keyword = call.request.queryParameters["keyword"]

        val searchBy =
            call.request.queryParameters["search_by"]
                ?: call.request.queryParameters["searchBy"]
                ?: "wilayah"

        val wilayahLevel =
            call.request.queryParameters["wilayah_level"]
                ?: call.request.queryParameters["wilayahLevel"]
                ?: "kabupaten"

        val page = call.request.queryParameters["page"]?.toIntOrNull() ?: 1
        val size = call.request.queryParameters["size"]?.toIntOrNull() ?: 50

        val desil = call.request.queryParameters.getAll("desil")
            ?.mapNotNull { value -> value.toIntOrNull() }
            ?: emptyList()

        val program = call.request.queryParameters.getAll("program")
            ?: emptyList()

        val status = call.request.queryParameters.getAll("status")
            ?: emptyList()

        val prioritas = call.request.queryParameters.getAll("prioritas")
            ?: emptyList()

        val asesmen = call.request.queryParameters.getAll("asesmen")
            ?: emptyList()

        val statusData = call.request.queryParameters.getAll("statusData")
            ?: emptyList()

        call.respond(
            recomService.getRecommendations(
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
        )
    }
}