package net.sunaba.auth

import com.auth0.jwk.GuavaCachedJwkProvider
import com.auth0.jwk.UrlJwkProvider
import com.auth0.jwt.interfaces.Payload
import io.ktor.application.ApplicationCall
import io.ktor.auth.Authentication
import io.ktor.auth.AuthenticationContext
import io.ktor.auth.AuthenticationFunction
import io.ktor.auth.jwt.JWTCredential
import io.ktor.auth.jwt.JWTPrincipal
import io.ktor.auth.jwt.jwt
import java.net.URL

internal const val JWK_PROVIDER_URL = "https://www.googleapis.com/service_accounts/v1/jwk/securetoken@system.gserviceaccount.com"

class FirebaseJwtConfiguration(val projectId: String, val name: String?) {

    internal var authenticationFunction: AuthenticationFunction<JWTCredential> = { credentials ->
        JWTPrincipal(credentials.payload)
    }

    /**
     * 追加のバリデーションを行いたい場合に設定する
     *
     * @param validate
     */
    fun validate(validate: suspend ApplicationCall.(credential:JWTCredential) -> Boolean) {
        authenticationFunction = { credentials ->
            if (validate.invoke(this, credentials)) {
                JWTPrincipal(credentials.payload)
            } else null
        }
    }
}

fun Authentication.Configuration.firebaseJwt(
        name: String? = null,
        projectId: String,
        configure: FirebaseJwtConfiguration.() -> Unit
) {
    val config = FirebaseJwtConfiguration(projectId, name).apply(configure)
    val jwkIssuer = "https://securetoken.google.com/${config.projectId}"
    val audience = config.projectId
    val jwkProvider = UrlJwkProvider(URL(JWK_PROVIDER_URL))
    val cachedJwkProvider = GuavaCachedJwkProvider(jwkProvider)
    jwt(config.name) {
        verifier(cachedJwkProvider, jwkIssuer) {
            withAudience(audience)
        }
        validate(config.authenticationFunction)
    }
}

val Payload.firebaseUserId: String?
    get() = this.subject

val AuthenticationContext.firebaseUserId: String?
    get() = this.principal<JWTPrincipal>()?.payload?.firebaseUserId