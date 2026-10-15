package backend.route

import backend.service.AreaService
import io.ktor.server.application.call
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get

fun Route.areaRoute(areaService: AreaService) {
    get("/areas") {
        val level = call.request.queryParameters["level"] ?: "kabupaten"

        val kodeProvinsi = call.request.queryParameters["kode_provinsi"]
        val kodeKabkot = call.request.queryParameters["kode_kabkot"]
        val kodeKecamatan = call.request.queryParameters["kode_kecamatan"]
        val kodeKelurahanDesa = call.request.queryParameters["kode_kelurahan_desa"]

        val desil = call.request.queryParameters.getAll("desil")
            ?.mapNotNull { value -> value.toIntOrNull() }
            ?: emptyList()

        val prioritas = call.request.queryParameters.getAll("prioritas")
            ?: emptyList()

        val asesmen = call.request.queryParameters.getAll("asesmen")
            ?: emptyList()

        call.respond(
            areaService.getAreas(
                level = level,
                kodeProvinsi = kodeProvinsi,
                kodeKabkot = kodeKabkot,
                kodeKecamatan = kodeKecamatan,
                kodeKelurahanDesa = kodeKelurahanDesa,
                desil = desil,
                prioritas = prioritas,
                asesmen = asesmen
            )
        )
    }
}