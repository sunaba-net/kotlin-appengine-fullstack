package net.sunaba

import com.auth0.jwt.algorithms.Algorithm
import com.google.api.client.http.javanet.NetHttpTransport
import com.google.api.client.json.jackson2.JacksonFactory
import com.google.api.services.appengine.v1.Appengine
import com.google.api.services.cloudresourcemanager.CloudResourceManager
import com.google.auth.http.HttpCredentialsAdapter
import com.google.auth.oauth2.GoogleCredentials
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
import kotlin.streams.toList

fun isOwner(email: String?) = getOwners("ktor-sunaba").contains("user:${email}")

fun getOwners(projectId: String): List<String> {
    val resource = CloudResourceManager.Builder(NetHttpTransport(), JacksonFactory.getDefaultInstance()
            , HttpCredentialsAdapter(GoogleCredentials.getApplicationDefault())).build()

    return resource.projects().getIamPolicy(projectId, null).execute().bindings
            .filter { it.role == "roles/owner" }.flatMap { it.members }
}

/**
 * メールアドレスがキーで、ロールのSetがバリューのマップを返す
 * @param projectId プロジェクトID
 * @param userOnly プレフィックスが"user:"で始まるアカウントのみを対象とする
 * @return メールアドレスがキーで、ロールのSetがバリューのマップを返す
 */
fun getRoles(projectId: String, userOnly: Boolean = true): Map<String, Set<String>> {
    val resource = CloudResourceManager.Builder(NetHttpTransport(), JacksonFactory.getDefaultInstance()
            , HttpCredentialsAdapter(GoogleCredentials.getApplicationDefault())).build()


    return resource.projects().getIamPolicy(projectId, null).execute().bindings
            .flatMap { binding -> binding.members.stream().filter { !userOnly || it.startsWith("user:") }.map { it!! to binding.role!! }.toList() }
            .groupBy({ it.first }, { it.second }).map { it.key to it.value.toSet() }.toMap()
}


fun main() {

    val appenigne = Appengine.Builder(NetHttpTransport(), JacksonFactory.getDefaultInstance()
            , HttpCredentialsAdapter(GoogleCredentials.getApplicationDefault())).build()

    val app = appenigne.apps().get("ktor-sunaba").execute()

    println(app.iap.oauth2ClientId)
    println(app.iap.oauth2ClientSecret)

    SecretManagerServiceClient.create().use { client ->

        client.listSecrets("projects/ktor-sunaba").iterateAll().forEach {
            println(it)
        }

    }
}

fun Application.module() {
    install(ContentNegotiation) {
        json(JsonConfiguration.Stable)
        serialization(ContentType.Application.Cbor, Cbor())
    }
    install(AppEngineDeferred) {
//        projectId = "ktor-sunaba"
//        this.region = "asia-northeast1"
    }

    //get secretKey from secret manager
    val secretKey = SecretManagerServiceClient.create().use {
        it.accessSecretVersion(SecretVersionName.of("ktor-sunaba", "session-user-secret", "latest")).payload.data.toStringUtf8()
    }

    val googleSignIn = easyGoogleSignInConfig {
        clientId = "509057577460-efnp64l74ech7bmbs44oerb67mtkishc.apps.googleusercontent.com"
        onLoginAction = { jwt ->
            val email = jwt.payload.claims["email"]!!.asString()
            this.sessions.set(User(jwt.payload.subject, email, isOwner(email)))
            true
        }
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

    install(Authentication) {
        register(googleSignIn)
        provider("admin") {
            pipeline.intercept(AuthenticationPipeline.CheckAuthentication) {
                if (true != this.call.sessions.get<User>()?.admin) {
                    call.respondRedirect(googleSignIn.loginPath + "?continue=" + URLEncoder.encode(call.request.path()))
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
        installEasyGoogleSignIn(googleSignIn)
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
