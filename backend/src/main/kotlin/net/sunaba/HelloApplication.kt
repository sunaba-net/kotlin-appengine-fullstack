package net.sunaba

import io.ktor.application.Application
import io.ktor.application.call
import io.ktor.application.install
import io.ktor.features.CORS
import io.ktor.features.ContentNegotiation
import io.ktor.features.StatusPages
import io.ktor.http.ContentType
import io.ktor.http.content.default
import io.ktor.http.content.files
import io.ktor.http.content.static
import io.ktor.response.respond
import io.ktor.response.respondText
import io.ktor.routing.get
import io.ktor.routing.routing
import io.ktor.serialization.DefaultJsonConfiguration
import io.ktor.serialization.serialization
import kotlinx.serialization.json.Json
import net.sunaba.appengine.AppEngine
import net.sunaba.appengine.AppEngineDeferred
import net.sunaba.appengine.HelloDeferred
import net.sunaba.appengine.deferred
import java.io.PrintWriter
import java.io.StringWriter

fun Application.module() {

    install(ContentNegotiation) {
        serialization(contentType = ContentType.Application.Json
                , json = Json(DefaultJsonConfiguration.copy(prettyPrint = true)))
    }
    install(AppEngineDeferred) {
        idTokenVerification = true
//        projectId = "ktor-sunaba"
//        this.region = "asia-northeast1"

    }

    //ローカルで実行時はfrontendからのCORSを有効化する
    if (AppEngine.isLocalEnv) install(CORS) { host("localhost:8080") }
    install(StatusPages) {
        exception<Throwable> { cause ->
            StringWriter().use {
                PrintWriter(it).use {
                    cause.printStackTrace(it)
                }
                val stackTrace = it.buffer.toString()
                call.respondText("<h1>Internal Error</h1><pre>${stackTrace}</pre>", ContentType.Text.Html)
            }
            throw cause
        }
    }

    routing {

        static {
            val staticPath = if (AppEngine.isLocalEnv) "build/staged-app/web" else "web"
            files("$staticPath")
            default("$staticPath/index.html")
        }

        get("/hello") {
            call.respondText { "hello" }
        }

        get("props") {
            call.respond(System.getProperties().map { it.key.toString() to it.value.toString() }.toMap())
        }

        get("env") {
            call.respond(System.getenv().map { it.key.toString() to it.value.toString() }.toMap())
        }

        get("gae_env") {
            call.respond(AppEngine.Env.values().map { it.key to it.value }.toMap())
        }

        get("gae_meta") {
            call.respond(AppEngine.metaData.map { it.key.path to it.value }.toMap())
        }

        get("/tasks/add") {
            deferred(HelloDeferred("Kotlin World"))
        }
    }
}
