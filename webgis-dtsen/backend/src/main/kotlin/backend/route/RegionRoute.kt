package backend.route

import backend.service.RegionService
import io.ktor.server.application.call
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.route

fun Route.regionRoute(regionService: RegionService) {
    route("/regions") {
        get("/provinsi") {
            call.respond(regionService.getProvinsi())
        }

        get("/kabupaten") {
            val kodeProvinsi = call.request.queryParameters["kode_provinsi"]

            if (kodeProvinsi.isNullOrBlank()) {
                call.respond(emptyList<Any>())
                return@get
            }

            call.respond(regionService.getKabupaten(kodeProvinsi))
        }

        get("/kecamatan") {
            val kodeKabkot = call.request.queryParameters["kode_kabkot"]

            if (kodeKabkot.isNullOrBlank()) {
                call.respond(emptyList<Any>())
                return@get
            }

            call.respond(regionService.getKecamatan(kodeKabkot))
        }

        get("/desa") {
            val kodeKecamatan = call.request.queryParameters["kode_kecamatan"]

            if (kodeKecamatan.isNullOrBlank()) {
                call.respond(emptyList<Any>())
                return@get
            }

            call.respond(regionService.getDesa(kodeKecamatan))
        }
    }
}