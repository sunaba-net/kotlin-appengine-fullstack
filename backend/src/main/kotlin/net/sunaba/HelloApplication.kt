package net.sunaba

import com.auth0.jwt.algorithms.Algorithm
import com.google.cloud.secretmanager.v1.SecretManagerServiceClient
import com.google.cloud.secretmanager.v1.SecretVersionName
import io.ktor.application.Application
import io.ktor.application.call
import io.ktor.application.install
import io.ktor.auth.Authentication
import io.ktor.auth.AuthenticationPipeline
import io.ktor.auth.authenticate
import io.ktor.features.*
import io.ktor.http.ContentType
import io.ktor.http.content.default
import io.ktor.http.content.files
import io.ktor.http.content.static
import io.ktor.request.path
import io.ktor.response.respond
import io.ktor.response.respondRedirect
import io.ktor.response.respondText
import io.ktor.routing.get
import io.ktor.routing.route
import io.ktor.routing.routing
import io.ktor.serialization.json
import io.ktor.serialization.serialization
import io.ktor.sessions.*
import kotlinx.serialization.cbor.Cbor
import kotlinx.serialization.json.JsonConfiguration
import net.sunaba.appengine.AppEngine
import net.sunaba.appengine.AppEngineDeferred
import net.sunaba.appengine.TestRetry
import net.sunaba.appengine.deferred
import net.sunaba.auth.*
import java.io.PrintWriter
import java.io.StringWriter
import java.net.URLEncoder

fun Application.module() {
    val gcpProjectId = if (AppEngine.isServiceEnv) AppEngine.Env.GOOGLE_CLOUD_PROJECT.value else "ktor-sunaba"

    install(ContentNegotiation) {
        json(JsonConfiguration.Stable)
        serialization(ContentType.Application.Cbor, Cbor())
    }
    install(AppEngineDeferred) {}

    //get secretKey from secret manager
    val secretKey = SecretManagerServiceClient.create().use {
        it.accessSecretVersion(SecretVersionName.of(gcpProjectId, "session-user-secret", "latest")).payload.data.toStringUtf8()
    }

    install(Sessions) {
        cookie<User>("app-user-session") {
            cookie.maxAgeInSeconds = 60 * 60
            cookie.httpOnly = true
            cookie.secure = AppEngine.isServiceEnv
            cookie.path = "/"
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
        provider("admin") {
            pipeline.intercept(AuthenticationPipeline.CheckAuthentication) {
                if (true != this.call.sessions.get<User>()?.admin) {
                    call.respondRedirect(googleSignIn.path + "?continue=" + URLEncoder.encode(call.request.path(), "UTF-8"))
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

    routing {
        installLogin(googleSignIn)

        authenticate("admin") {
            route("admin") {

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
                    call.respondText(deferred(TestRetry(3)).name)
                }
            }
        }

        get("localScheme") {
            call.respondText(call.request.local.scheme)
        }

        get("originScheme") {
            call.respondText(call.request.origin.scheme)
        }

        get("uris") {
            call.respond(mapOf<String, Any>(
                    "origin.scheme" to call.request.origin.scheme,
                    "origin.host" to call.request.origin.host,
                    "origin.uri" to call.request.origin.uri,
                    "origin.remoteHost" to call.request.origin.remoteHost,
                    "local.scheme" to call.request.local.scheme,
                    "local.host" to call.request.local.host,
                    "local.uri" to call.request.local.uri,
                    "local.remoteHost" to call.request.local.remoteHost
            ))
        }

        get("headers") {
            call.respond(call.request.headers.entries().map {
                it.key to it.value
            }.toMap())
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
