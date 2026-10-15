package backend.route

import backend.service.SummaryService
import io.ktor.server.application.call
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get

fun Route.summaryRoute(summaryService: SummaryService) {
    get("/summary") {
        val kodeProvinsi = call.request.queryParameters["kode_provinsi"]
        val kodeKabkot = call.request.queryParameters["kode_kabkot"]
        val kodeKecamatan = call.request.queryParameters["kode_kecamatan"]
        val kodeKelurahanDesa = call.request.queryParameters["kode_kelurahan_desa"]

        val desil = call.request.queryParameters.getAll("desil")
            ?.mapNotNull { value -> value.toIntOrNull() }
            ?: emptyList()

        val prioritas = call.request.queryParameters.getAll("prioritas")
            ?: emptyList()

        call.respond(
            summaryService.getSummary(
                kodeProvinsi = kodeProvinsi,
                kodeKabkot = kodeKabkot,
                kodeKecamatan = kodeKecamatan,
                kodeKelurahanDesa = kodeKelurahanDesa,
                desil = desil,
                prioritas = prioritas
            )
        )
    }
}