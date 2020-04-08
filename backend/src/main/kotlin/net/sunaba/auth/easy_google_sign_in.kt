package net.sunaba.auth

import com.auth0.jwk.GuavaCachedJwkProvider
import com.auth0.jwk.UrlJwkProvider
import io.ktor.application.ApplicationCall
import io.ktor.application.call
import io.ktor.auth.Authentication
import io.ktor.auth.AuthenticationFunction
import io.ktor.auth.authenticate
import io.ktor.auth.authentication
import io.ktor.auth.jwt.JWTCredential
import io.ktor.auth.jwt.JWTPrincipal
import io.ktor.auth.jwt.jwt
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.response.respond
import io.ktor.response.respondText
import io.ktor.routing.Routing
import io.ktor.routing.get
import io.ktor.routing.post
import java.net.URL

class Configuration internal constructor() {
    lateinit var clientId: String
    val googleAuthenticationName: String
        get() = "google-auth"
    var loginPath: String = "/__login"
    internal var onLoginAction: ApplicationCall.(jwt:JWTPrincipal) -> Boolean = {true}
}

fun easyGoogleSignInConfig(configure: Configuration.() -> Unit): Configuration = Configuration().apply(configure)

fun Authentication.Configuration.register(config: Configuration) {
    val jwkIssuer = "accounts.google.com"
    val audience = config.clientId
    val jwkProvider = UrlJwkProvider(URL("https://www.googleapis.com/oauth2/v3/certs"))
    val cachedJwkProvider = GuavaCachedJwkProvider(jwkProvider)
    jwt(config.googleAuthenticationName) {
        verifier(cachedJwkProvider, jwkIssuer) {
            withAudience(audience)
        }
        validate { jwtCredential -> JWTPrincipal(jwtCredential.payload) }
    }
}

fun Routing.installEasyGoogleSignIn(config: Configuration) {

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
<script>
    function onSignIn(googleUser) {
        // The ID token you need to pass to your backend:
        var id_token = googleUser.getAuthResponse().id_token;
        var xhr = new XMLHttpRequest();
        xhr.open('POST', '${config.loginPath}');
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

    authenticate(config.googleAuthenticationName) {
        post(config.loginPath) {
            val jwtPrincipal = call.authentication.principal<JWTPrincipal>()!!
            config.onLoginAction.invoke(this.call, jwtPrincipal)
            call.respond(HttpStatusCode.OK)
        }
    }

}

