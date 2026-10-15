package backend

import backend.config.DbConfig
import backend.repo.AreaRepo
import backend.repo.ConcentRepo
import backend.repo.RecomRepo
import backend.repo.RegionRepo
import backend.route.areaRoute
import backend.route.concentRoute
import backend.route.recomRoute
import backend.route.regionRoute
import backend.route.summaryRoute
import backend.service.AreaService
import backend.service.ConcentService
import backend.service.RecomService
import backend.service.RegionService
import backend.service.SummaryService
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.application.Application
import io.ktor.server.application.call
import io.ktor.server.application.install
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.ktor.server.plugins.cors.routing.CORS
import io.ktor.server.response.respondText
import io.ktor.server.routing.get
import io.ktor.server.routing.route
import io.ktor.server.routing.routing

fun Application.module() {
    install(ContentNegotiation) {
        json()
    }

    install(CORS) {
        allowHost("localhost:5173")
        allowHost("localhost:5174")

        allowHost("webgis-dtsen-nasional.vercel.app")
        allowHost("webgis-dtsen-nasional-92c25t61.vercel.app")

        allowHeader(HttpHeaders.ContentType)
        allowMethod(HttpMethod.Get)
    }

    val dataSource = DbConfig.dataSource

    val areaRepo = AreaRepo(dataSource)
    val concentRepo = ConcentRepo(areaRepo, dataSource)
    val recomRepo = RecomRepo(dataSource)
    val regionRepo = RegionRepo(dataSource)

    val summaryService = SummaryService(dataSource)
    val areaService = AreaService(areaRepo)
    val recomService = RecomService(recomRepo)
    val concentService = ConcentService(concentRepo)
    val regionService = RegionService(regionRepo)

    routing {
        get("/") {
            call.respondText("Backend DTSEN aktif")
        }

        route("/api") {
            summaryRoute(summaryService)
            areaRoute(areaService)
            recomRoute(recomService)
            concentRoute(concentService)
            regionRoute(regionService)
        }
    }
}