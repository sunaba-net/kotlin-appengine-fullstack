package net.sunaba.auth

import com.auth0.jwk.GuavaCachedJwkProvider
import com.auth0.jwk.UrlJwkProvider
import com.auth0.jwt.JWT
import com.auth0.jwt.JWTCreator
import com.auth0.jwt.algorithms.Algorithm
import com.auth0.jwt.interfaces.DecodedJWT
import io.ktor.application.ApplicationCall
import io.ktor.application.call
import io.ktor.auth.*
import io.ktor.auth.jwt.JWTCredential
import io.ktor.auth.jwt.JWTPrincipal
import io.ktor.auth.jwt.jwt
import io.ktor.features.origin
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.request.path
import io.ktor.response.respond
import io.ktor.response.respondRedirect
import io.ktor.response.respondText
import io.ktor.routing.Routing
import io.ktor.routing.get
import io.ktor.routing.post
import io.ktor.sessions.CookieConfiguration
import io.ktor.sessions.SessionSerializer
import io.ktor.sessions.Sessions
import io.ktor.sessions.cookie
import io.ktor.util.date.GMTDate
import kotlinx.serialization.Serializable
import net.sunaba.appengine.AppEngine
import java.net.URL
import java.net.URLEncoder
import java.util.*

/**
 * https://developers.google.com/identity/sign-in/web?hl=ja
 */
class EasyGoogleSignInProvider(config: Configuration) : AuthenticationProvider(config) {
    class Configuration internal constructor(name: String?) : AuthenticationProvider.Configuration(name) {

        lateinit var clientId: String
        lateinit var algorithm: Algorithm

        var expireMinute: Int = 60 * 3
        internal fun build(): EasyGoogleSignInProvider = EasyGoogleSignInProvider(this)

        var issure: String = "easy-google-login"

        val googleAuthenticationName: String
            get() = "google-auth-${name}"

        var secureCookie: Boolean = true
        internal val cookieName: String
            get() = "${name}-jwt"

        var loginPath: String = "/__login"
        var exchangeTokenPath: String = "/__exchangeToken"

        internal var authenticationFunction: AuthenticationFunction<JWTCredential> = { credentials ->
            JWTPrincipal(credentials.payload)
        }

        fun validate(validate: suspend ApplicationCall.(credential: JWTCredential) -> Boolean) {
            authenticationFunction = { credentials ->
                if (validate.invoke(this, credentials)) {
                    JWTPrincipal(credentials.payload)
                } else null
            }
        }

        internal var customClaimFunction: suspend CustomClaimer.(firebasePrincipal: JWTPrincipal) -> Unit = { }
        fun customClaim(claiming: suspend CustomClaimer.(firebasePrincipal: JWTPrincipal) -> Unit) {
            customClaimFunction = claiming
        }
    }
}

fun easyGoogleSignInConfig(name: String?=null, configure: EasyGoogleSignInProvider.Configuration.() -> Unit): EasyGoogleSignInProvider.Configuration = EasyGoogleSignInProvider.Configuration(name).apply(configure)

private val EasyGoogleSignInKey: Any = "EasyGoogleLoginKey"


fun Authentication.Configuration.register(
        config: EasyGoogleSignInProvider.Configuration
) {
    val provider = config.build()
    provider.pipeline.intercept(AuthenticationPipeline.CheckAuthentication) { context ->
        fun challenge(cause: String) =
                context.challenge(EasyGoogleSignInKey, AuthenticationFailedCause.Error(cause)) {
                    //config.challengeFunction.invoke(this)
                    if (!it.completed && call.response.status() != null) {
                        it.complete()
                    }
                    call.response.cookies.appendExpired(config.cookieName)
                    call.respondRedirect("${config.loginPath}?continue=" + URLEncoder.encode(call.request.path(), "UTF-8"))
                }

        val token = call.request.cookies[config.cookieName]
        if (!token.isNullOrBlank()) {
            try {
                val verifier = JWT.require(config.algorithm).withIssuer(config.issure)
                //config.verificationFunction.invoke(verifier, call.request)
                context.principal(EasyLoginPrincipal(verifier.build().verify(token)))
                return@intercept
            } catch (ex: Throwable) {
                ex.printStackTrace()
                challenge("Failed to verify")
            }
        } else {
            challenge("Token is Empty")
        }
    }

    register(provider)

    val jwkIssuer = "accounts.google.com"
    val audience = config.clientId
    val jwkProvider = UrlJwkProvider(URL("https://www.googleapis.com/oauth2/v3/certs"))
    val cachedJwkProvider = GuavaCachedJwkProvider(jwkProvider)
    jwt(config.googleAuthenticationName) {
        verifier(cachedJwkProvider, jwkIssuer) {
            withAudience(audience)
        }
        validate(config.authenticationFunction)
    }
}

fun Routing.installEasyGoogleSignIn(config: EasyGoogleSignInProvider.Configuration) {

    authenticate(config.googleAuthenticationName) {
        //exchange
        post(config.exchangeTokenPath) {
            val jwtPrincipal = call.authentication.principal<JWTPrincipal>()
            val email = call.authentication.principal<JWTPrincipal>()?.payload?.getClaim("email")?.asString()
            val expire = Date().time + config.expireMinute * 60 * 1000
            val builder = JWT.create()
                    .withIssuer(config.issure)
                    .withClaim("email", email)
                    .withExpiresAt(Date(expire))
            config.customClaimFunction.invoke(CustomClaimer(builder), jwtPrincipal!!)

            if (config.secureCookie && !call.request.origin.scheme.equals("https", true)) {
                call.response.cookies.append(config.cookieName, ""
                        , expires = GMTDate(expire), path = "/", httpOnly = true, secure = config.secureCookie)
                call.respond(HttpStatusCode.Unauthorized)
            } else {
                val customToken = builder.sign(config.algorithm)
                call.response.cookies.append(config.cookieName, customToken
                        , expires = GMTDate(expire), path = "/", httpOnly = true, secure = config.secureCookie)
                call.respond(HttpStatusCode.OK)
            }
        }
    }

    get(config.loginPath) {
        call.respondText("""<html lang="en">
<head>
    <meta content="width=device-width, initial-scale=1, minimum-scale=1" name="viewport">
    <meta name="google-signin-scope" content="email">
    <meta name="google-signin-client_id" content="${config.clientId}">
    <script src="https://apis.google.com/js/platform.js" async defer></script>
    <style>.abcRioButton {margin:auto;}
    </style>
</head>
<body>
<div class="g-signin2" data-onsuccess="onSignIn" data-theme="dark"></div>
<div id="status-text">test</div>
<script>
    function onSignIn(googleUser) {
        // The ID token you need to pass to your backend:
        var id_token = googleUser.getAuthResponse().id_token;
        var xhr = new XMLHttpRequest();
        xhr.open('POST', '${config.exchangeTokenPath}');
        xhr.setRequestHeader('Authorization', 'Bearer ' + id_token)
        xhr.onload = function () {
            var auth2 = gapi.auth2.getAuthInstance();
            auth2.signOut().then(function () {
                if (xhr.status == 200) {
                    var searchParams = new URLSearchParams(window.location.search)
                    var continueUrl = searchParams.get("continue")
                    if (continueUrl) {
                        window.location = continueUrl
                    }
                }
            });
        };
        xhr.send();
    }
</script>
</body>
</html>""", ContentType.Text.Html)
    }
}


class CustomClaimer(private val builder: JWTCreator.Builder) {
    fun withClaim(name: String, value: Boolean): CustomClaimer = apply { builder.withClaim(name, value) }
    fun withClaim(name: String, value: Int): CustomClaimer = apply { builder.withClaim(name, value) }
    fun withClaim(name: String, value: Long): CustomClaimer = apply { builder.withClaim(name, value) }
    fun withClaim(name: String, value: Double): CustomClaimer = apply { builder.withClaim(name, value) }
    fun withClaim(name: String, value: String): CustomClaimer = apply { builder.withClaim(name, value) }
    fun withClaim(name: String, value: Date): CustomClaimer = apply { builder.withClaim(name, value) }
    fun withArrayClaim(name: String, items: Array<String>): CustomClaimer = apply { builder.withArrayClaim(name, items) }
    fun withArrayClaim(name: String, items: Array<Int>): CustomClaimer = apply { builder.withArrayClaim(name, items) }
    fun withArrayClaim(name: String, items: Array<Long>): CustomClaimer = apply { builder.withArrayClaim(name, items) }
}

class EasyLoginPrincipal(val jwt: DecodedJWT) : Principal

val EasyLoginPrincipal.email
    get() = jwt.claims.get("email")!!.asString()

@Serializable
data class User(val id: String, val email: String, var admin: Boolean = false)

class UserSessionSerializer(val algorithm: Algorithm, val issuer: String = "App Issuer", val maxAgeInSeconds: Long = 60 * 60) : SessionSerializer<User> {
    override fun deserialize(text: String): User {
        val jwt = JWT.require(algorithm)
                .withIssuer(issuer)
                .build()
                .verify(text)

        fun str(key: String) = jwt.claims[key]!!.asString()
        fun bool(key: String) = jwt.claims[key]!!.asBoolean()
        return User(jwt.subject, str("email"), bool("admin"))
    }

    override fun serialize(user: User): String {
        val now = Date()
        return JWT.create().withIssuer(issuer)
                .withIssuer(issuer)
                .withIssuedAt(now)
                .withExpiresAt(Date(now.time + maxAgeInSeconds * 1000))
                .withSubject(user.id)
                .withClaim("email", user.email)
                .withClaim("admin", user.admin)
                .sign(algorithm)
    }
}

fun Sessions.Configuration.installEasyLogin(config: EasyGoogleSignInProvider.Configuration) {
    cookie<User>(config.cookieName) {
        cookie.maxAgeInSeconds = 60 * 60
        cookie.httpOnly = true
        cookie.secure = AppEngine.isServiceEnv
        cookie.path = "/"
        serializer = UserSessionSerializer(config.algorithm, maxAgeInSeconds = cookie.maxAgeInSeconds)
    }
}