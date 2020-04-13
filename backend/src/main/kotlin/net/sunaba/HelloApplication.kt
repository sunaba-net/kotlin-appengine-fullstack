package net.sunaba

import com.auth0.jwt.algorithms.Algorithm
import com.google.api.client.json.jackson2.JacksonFactory
import com.google.api.services.cloudresourcemanager.CloudResourceManager
import com.google.auth.http.HttpCredentialsAdapter
import com.google.auth.oauth2.GoogleCredentials
import com.google.cloud.secretmanager.v1.SecretManagerServiceClient
import com.google.cloud.secretmanager.v1.SecretVersionName
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import com.google.firebase.cloud.FirestoreClient
import io.ktor.application.Application
import io.ktor.application.application
import io.ktor.application.call
import io.ktor.application.install
import io.ktor.auth.Authentication
import io.ktor.auth.AuthenticationPipeline
import io.ktor.auth.authenticate
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
import io.ktor.sessions.*
import io.ktor.util.toMap
import io.ktor.websocket.WebSockets
import io.ktor.websocket.webSocket
import kotlinx.serialization.cbor.Cbor
import kotlinx.serialization.json.JsonConfiguration
import net.sunaba.appengine.AppEngine
import net.sunaba.appengine.AppEngineDeferred
import net.sunaba.appengine.deferred
import net.sunaba.auth.*
import net.sunaba.gogleapis.SharedHttpTransportFactory
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
        provider("admin") {
            pipeline.intercept(AuthenticationPipeline.RequestAuthentication) {
                if (true != this.call.sessions.get<User>()?.admin) {
                    call.respondRedirect(googleSignIn.path + "?continue=" + URLEncoder.encode(call.request.path(), "UTF-8"))
                    subject.challenge.complete()
                }
            }
        }
        //@see https://cloud.google.com/appengine/docs/standard/java11/scheduling-jobs-with-cron-yaml
        provider("cron") {
            pipeline.intercept(AuthenticationPipeline.RequestAuthentication) {
                if (!"true".equals(call.request.headers["X-Appengine-Cron"])) {
                    call.respond(HttpStatusCode.Unauthorized)
                    subject.challenge.complete()
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

fun main() {

    val res = CloudResourceManager.Builder(SharedHttpTransportFactory.sharedInstance, JacksonFactory.getDefaultInstance()
            , HttpCredentialsAdapter(GoogleCredentials.getApplicationDefault())).build()


    val options: FirebaseOptions = FirebaseOptions.builder()
            .setCredentials(GoogleCredentials.getApplicationDefault())
            .setProjectId("ktor-sunaba")
            .setHttpTransport(SharedHttpTransportFactory.sharedInstance)
            .build()

    val app = FirebaseApp.initializeApp(options)
    println(app)

    val firestore = FirestoreClient.getFirestore()

    firestore.collection("users")
            .add(User("123", "test@gmail.com", false)).get()


//    println(FirebaseAuth.getInstance().listUsers(null).values)
//    val firestore = FirestoreClient.getFirestore()
//    val addUser= firestore.collection("users").add(User("123", "test@gmail.com", false))
//
//    while (!addUser.isDone) {
//        println("waiting...")
//        Thread.sleep(1000)
//    }

}