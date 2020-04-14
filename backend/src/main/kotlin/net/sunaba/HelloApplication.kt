package net.sunaba


import com.auth0.jwt.algorithms.Algorithm
import com.google.cloud.secretmanager.v1.SecretManagerServiceClient
import com.google.cloud.secretmanager.v1.SecretVersionName
import io.ktor.application.Application
import io.ktor.application.application
import io.ktor.application.call
import io.ktor.application.install
import io.ktor.auth.*
import io.ktor.features.*
import io.ktor.http.ContentType
import io.ktor.http.CookieEncoding
import io.ktor.http.HttpStatusCode
import io.ktor.http.cio.websocket.CloseReason
import io.ktor.http.cio.websocket.Frame
import io.ktor.http.cio.websocket.close
import io.ktor.http.cio.websocket.readText
import io.ktor.http.content.default
import io.ktor.http.content.files
import io.ktor.http.content.static
import io.ktor.request.path
import io.ktor.response.respond
import io.ktor.response.respondRedirect
import io.ktor.response.respondText
import io.ktor.routing.Routing
import io.ktor.routing.get
import io.ktor.routing.route
import io.ktor.routing.routing
import io.ktor.serialization.json
import io.ktor.serialization.serialization
import io.ktor.sessions.Sessions
import io.ktor.sessions.cookie
import io.ktor.sessions.sessions
import io.ktor.sessions.set
import io.ktor.util.toMap
import io.ktor.websocket.WebSockets
import io.ktor.websocket.webSocket
import kotlinx.serialization.cbor.Cbor
import kotlinx.serialization.json.JsonConfiguration
import net.sunaba.appengine.AppEngine
import net.sunaba.appengine.AppEngineDeferred
import net.sunaba.appengine.deferred
import net.sunaba.auth.*
import java.io.PrintWriter
import java.io.StringWriter
import java.net.URLEncoder

data class AdminPrincipal(val user: User) : Principal

fun Application.module() {
    val gcpProjectId = if (AppEngine.isServiceEnv) AppEngine.Env.GOOGLE_CLOUD_PROJECT.value else "ktor-sunaba"

    install(ContentNegotiation) {
        json(JsonConfiguration.Stable)
        serialization(ContentType.Application.Cbor, Cbor())
    }
    install(AppEngineDeferred) {}

    val secretKey = if (AppEngine.isLocalEnv) "my-special-secret-key" else SecretManagerServiceClient.create().use {
        //get secretKey from secret manager
        it.accessSecretVersion(SecretVersionName.of(gcpProjectId, "session-user-secret", "latest")).payload.data.toStringUtf8()
    }

    install(Sessions) {
        cookie<User>("app-user-session") {
            cookie.maxAgeInSeconds = 60 * 60
            cookie.httpOnly = true
            cookie.secure = AppEngine.isServiceEnv
            cookie.path = "/"
            cookie.encoding = CookieEncoding.RAW
            serializer = UserSessionSerializer(Algorithm.HMAC256(secretKey), maxAgeInSeconds = cookie.maxAgeInSeconds)
        }
    }

    val googleSignIn = googleSignInConfig("509057577460-efnp64l74ech7bmbs44oerb67mtkishc.apps.googleusercontent.com") {
        onLoginAction = { jwt ->
            val email = jwt.payload.claims["email"]!!.asString()
            sessions.set(User(jwt.payload.subject, email, AppEngine.isOwner(gcpProjectId, email)))
            true
        }
    }

    install(Authentication) {
        register(googleSignIn)
        session<User>("admin") {
            validate {
                if (it.admin) AdminPrincipal(it) else null
            }
            challenge {
                call.respondRedirect(googleSignIn.path + "?continue=" + URLEncoder.encode(call.request.path(), "UTF-8"))
            }
        }
        //@see https://cloud.google.com/appengine/docs/standard/java11/scheduling-jobs-with-cron-yaml
        provider("cron") {
            pipeline.intercept(AuthenticationPipeline.RequestAuthentication) {
                if ("true" != call.request.headers["X-Appengine-Cron"]) {
                    subject.challenge("AppEngineCron", AuthenticationFailedCause.NoCredentials) {
                        call.respond(HttpStatusCode.Unauthorized)
                        subject.complete()
                    }
                }
            }
        }
    }

    //ローカルで実行時はfrontendからのCORSを有効化する
    if (AppEngine.isServiceEnv) {
        install(XForwardedHeaderSupport)
        install(HttpsRedirect) {
            exclude { it.request.headers.contains("X-AppEngine-TaskName") }
        }
    } else {
        install(CORS) {
            host("*")
        }
    }

    installStatusPageFeature()

    install(WebSockets)

    routing {
        installLogin(googleSignIn)

        routeAdmin()

        authenticate("cron") {
            get("/cron/test") {
                println(call.request.headers.toMap())
                call.respondText("test")
            }
        }



        webSocket("echo") {
            for (frame in incoming) {
                when (frame) {
                    is Frame.Text -> {
                        val text = frame.readText()
                        outgoing.send(Frame.Text("YOU SAID: $text"))
                        if (text.equals("bye", ignoreCase = true)) {
                            close(CloseReason(CloseReason.Codes.NORMAL, "Client said BYE"))
                        }
                    }
                }
            }
        }

        get("hello") {
            call.respondText("test3")
        }

        static {
            val staticPath = if (AppEngine.isLocalEnv) "build/staged-app/web" else "web"
            files("$staticPath")
            default("$staticPath/index.html")
        }
    }
}

private fun Application.installStatusPageFeature() {
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
}

private fun Routing.routeAdmin() = authenticate("admin") {
    route("admin") {

        get("user") {
            call.respond((call.authentication.principal as AdminPrincipal).user.id)
        }

        get("props") {
            call.respond(java.lang.System.getProperties().map { it.key.toString() to it.value.toString() }.toMap())
        }

        get("env") {
            call.respond(java.lang.System.getenv().map { it.key.toString() to it.value.toString() }.toMap())
        }

        get("gae_env") {
            call.respond(net.sunaba.appengine.AppEngine.Env.values().map { it.key to it.value }.toMap())
        }

        get("gae_meta") {
            call.respond(net.sunaba.appengine.AppEngine.metaData.map { it.key.path to it.value }.toMap())
        }

        get("/tasks/add") {
            call.respondText(application.deferred(net.sunaba.appengine.TestRetry(3)).name)

        }

        get("/tasks/add2") {
            call.respondText(application.deferred(net.sunaba.appengine.TestRetry(3)
                    , scheduleTime = java.util.Date(java.util.Date().time + 60 * 1000)).name)
        }
    }
}
