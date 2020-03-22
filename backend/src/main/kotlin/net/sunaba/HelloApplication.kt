package net.sunaba

import io.ktor.application.Application
import io.ktor.application.call
import io.ktor.application.install
import io.ktor.features.CORS
import io.ktor.features.ContentNegotiation
import io.ktor.features.StatusPages
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
import io.ktor.routing.routing
import io.ktor.serialization.json
import io.ktor.serialization.serialization
import io.ktor.websocket.WebSocketServerSession
import io.ktor.websocket.WebSockets
import io.ktor.websocket.webSocket
import kotlinx.serialization.PolymorphicSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.cbor.Cbor
import kotlinx.serialization.json.JsonConfiguration
import model.IntMessage
import model.Message
import net.sunaba.appengine.AppEngine
import net.sunaba.appengine.AppEngineDeferred
import net.sunaba.appengine.HelloDeferred
import net.sunaba.appengine.deferred
import java.io.PrintWriter
import java.io.StringWriter

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

    //ローカルで実行時はfrontendからのCORSを有効化する
    if (AppEngine.isLocalEnv) {
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

        static {
            val staticPath = if (AppEngine.isLocalEnv) "build/staged-app/web" else "web"
            files("$staticPath")
            default("$staticPath/index.html")
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
            val frame = Frame.Binary(true, cbor.dump(serializer, IntMessage(123)))
            sessions.forEach {
                it.outgoing.send(frame)
            }
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
            call.respondText(deferred(HelloDeferred("Kotlin World")).name)
        }
    }
}
