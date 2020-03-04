package net.sunaba

import io.ktor.application.Application
import io.ktor.application.call
import io.ktor.application.install
import io.ktor.http.content.default
import io.ktor.http.content.files
import io.ktor.http.content.static
import io.ktor.response.respondText
import io.ktor.routing.get
import io.ktor.routing.routing
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty

fun Application.module() {

    routing {
        static {
            files("web")
            default("web/index.html")
        }
        get("/hello") {
            call.respondText { "hello" }
        }
    }

}

fun main() {
    val server = embeddedServer(Netty, 8081) {
        module()
    }
    server.start()
}