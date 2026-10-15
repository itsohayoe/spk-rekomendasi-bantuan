package backend.route

import backend.service.ConcentService
import io.ktor.server.application.call
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get

fun Route.concentRoute(concentService: ConcentService) {
    get("/concentration") {
        val level = call.request.queryParameters["level"] ?: "kabupaten"

        val kodeProvinsi = call.request.queryParameters["kode_provinsi"]
        val kodeKabkot = call.request.queryParameters["kode_kabkot"]
        val kodeKecamatan = call.request.queryParameters["kode_kecamatan"]
        val kodeKelurahanDesa = call.request.queryParameters["kode_kelurahan_desa"]

        val type = call.request.queryParameters["type"] ?: "bantuan"
        val program = call.request.queryParameters["program"] ?: "pbi_jk"
        val dimension = call.request.queryParameters["dimension"] ?: "kesehatan"
        val reference =
            call.request.queryParameters["reference"]
                ?: call.request.queryParameters["acuan"]
                ?: "nasional"

        call.respond(
            concentService.getConcentration(
                level = level,
                kodeProvinsi = kodeProvinsi,
                kodeKabkot = kodeKabkot,
                kodeKecamatan = kodeKecamatan,
                kodeKelurahanDesa = kodeKelurahanDesa,
                type = type,
                program = program,
                dimension = dimension,
                reference = reference
            )
        )
    }
}