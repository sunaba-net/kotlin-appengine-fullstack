package net.sunaba

import com.auth0.jwt.algorithms.Algorithm
import io.ktor.application.Application
import io.ktor.application.call
import io.ktor.application.install
import io.ktor.auth.Authentication
import io.ktor.auth.authenticate
import io.ktor.auth.principal
import io.ktor.features.HttpsRedirect
import io.ktor.features.XForwardedHeaderSupport
import io.ktor.response.respondText
import io.ktor.routing.get
import io.ktor.routing.route
import io.ktor.routing.routing
import net.sunaba.appengine.AppEngine
import net.sunaba.auth.*

fun Application.login_test_module() {

    if (AppEngine.isServiceEnv) {
        install(XForwardedHeaderSupport)
        install(HttpsRedirect)
    }

    val loginConfig = easyGoogleSignInConfig {
        clientId = "509057577460-efnp64l74ech7bmbs44oerb67mtkishc.apps.googleusercontent.com"
        algorithm = Algorithm.HMAC256("my-special-secret-key")
        loginPath = "/login"
        exchangeTokenPath = "/exchange_token"
        secureCookie = AppEngine.isServiceEnv
        validate { credential ->
//            val email = credential.payload.claims.get("email")?.asString()
//            email in arrayOf("test@example.com")
            true
        }

    }

    install(Authentication) {
        register(loginConfig)
    }

    routing {
        installEasyGoogleSignIn(loginConfig)
        authenticate {
            route("login_required_zone") {
                get("index") {
                    val email = call.principal<EasyLoginPrincipal>()!!.email
                    call.respondText("Your email: ${email}")
                }
            }
        }
    }
}