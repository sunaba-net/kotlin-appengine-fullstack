package net.sunaba

import com.auth0.jwt.algorithms.Algorithm
import com.google.api.client.http.javanet.NetHttpTransport
import com.google.api.client.json.jackson2.JacksonFactory
import com.google.api.services.cloudresourcemanager.CloudResourceManager
import com.google.auth.http.HttpCredentialsAdapter
import com.google.auth.oauth2.GoogleCredentials
import io.ktor.application.Application
import io.ktor.application.call
import io.ktor.application.install
import io.ktor.auth.Authentication
import io.ktor.auth.authenticate
import io.ktor.features.*
import io.ktor.http.ContentType
import io.ktor.http.cio.websocket.CloseReason
import io.ktor.http.cio.websocket.Frame
import io.ktor.http.cio.websocket.close
import io.ktor.http.cio.websocket.readText
import io.ktor.http.content.default
import io.ktor.http.content.files
import io.ktor.http.content.static
import io.ktor.response.respond
import io.ktor.response.respondText
import io.ktor.routing.get
import io.ktor.routing.route
import io.ktor.routing.routing
import io.ktor.serialization.json
import io.ktor.serialization.serialization
import io.ktor.websocket.WebSocketServerSession
import io.ktor.websocket.WebSockets
import io.ktor.websocket.webSocket
import kotlinx.serialization.PolymorphicSerializer
import kotlinx.serialization.cbor.Cbor
import kotlinx.serialization.json.JsonConfiguration
import model.IntMessage
import model.Message
import model.StringMessage
import net.sunaba.appengine.AppEngine
import net.sunaba.appengine.AppEngineDeferred
import net.sunaba.appengine.HelloDeferred
import net.sunaba.appengine.deferred
import net.sunaba.auth.easyGoogleSignInConfig
import net.sunaba.auth.installEasyGoogleSignIn
import net.sunaba.auth.register
import java.io.PrintWriter
import java.io.StringWriter

fun isOwner(email: String?) = getOwners("ktor-sunaba").contains("user:${email}")

fun getOwners(projectId: String): List<String> {
    val resource = CloudResourceManager.Builder(NetHttpTransport(), JacksonFactory.getDefaultInstance()
            , HttpCredentialsAdapter(GoogleCredentials.getApplicationDefault())).build()

    return resource.projects().getIamPolicy(projectId, null).execute().bindings
            .filter { it.role == "roles/owner" }.flatMap { it.members }
}


fun Application.module() {



    install(ContentNegotiation) {
        json(JsonConfiguration.Stable)
        serialization(ContentType.Application.Cbor, Cbor())
    }
    install(AppEngineDeferred) {
        idTokenVerification = true
//        projectId = "ktor-sunaba"
//        this.region = "asia-northeast1"
    }

    install(WebSockets)

    val googleSignIn = easyGoogleSignInConfig("admin") {
        clientId = "509057577460-efnp64l74ech7bmbs44oerb67mtkishc.apps.googleusercontent.com"
        algorithm = Algorithm.HMAC256("my-special-secret-key")
        secureCookie = AppEngine.isServiceEnv
        validate { credential ->
            isOwner(credential.payload.getClaim("email")?.asString())
        }
    }

    install(Authentication) {
        register(googleSignIn)
    }

    //ローカルで実行時はfrontendからのCORSを有効化する
    if (AppEngine.isServiceEnv) {
        install(XForwardedHeaderSupport)
        install(HttpsRedirect)
    } else {
        install(CORS) {
            host("*")
        }
    }

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

    val sessions = arrayListOf<WebSocketServerSession>()

    routing {

        installEasyGoogleSignIn(googleSignIn)

        authenticate(googleSignIn.name) {
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
                    call.respondText(deferred(HelloDeferred("Kotlin World")).name)
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

        webSocket("/chat") {
            println("session start")
            sessions.add(this)
            for (frame in incoming) {
                when (frame) {
                    is Frame.Text -> {
                        val text = frame.readText()

//                        sessions.forEach {
//                            it.outgoing.send(Frame.Text("YOU SAID: $text"))
//                        }

                        if (text.equals("bye", ignoreCase = true)) {
                            close(CloseReason(CloseReason.Codes.NORMAL, "Client said BYE"))
                        }
                    }
                }
            }
            sessions.remove(this)
            println("session end")
        }

        get("/chat_server") {
//            val appengine = Appengine.Builder(NetHttpTransport(), JacksonFactory.getDefaultInstance()
//                    , HttpCredentialsAdapter(GoogleCredentials.getApplicationDefault())).build()
//            val service = appengine.apps().services().get(AppEngine.Env.GOOGLE_CLOUD_PROJECT.name, "chat")
//                    .execute()
//            appengine.apps().services().versions().list("", "").execute()
        }

        get("/hello") {
            call.respondText { "hello" }

            val cbor = Cbor(context = model.module)
            val serializer = PolymorphicSerializer(Message::class)
            val msg = when ((Math.random() * 2).toInt()) {
                0 -> IntMessage(123)
                else -> StringMessage("Hello Kotlin")
            }
            val frame = Frame.Binary(true, cbor.dump(serializer, msg))

            sessions.forEach {
                it.outgoing.send(frame)
            }
        }
    }
}