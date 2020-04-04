@file:JsModule("firebaseui")
@file:JsQualifier("auth")

package firebaseui.auth

import firebase.auth.Auth
import firebase.auth.AuthCredential
import firebase.auth.AuthProvider
import firebase.auth.UserCredential

external class AuthUI(auth:Auth) {
    fun start(container:String, uiConfig:dynamic)
}

external class AnonymousAuthProvider: AuthProvider {
    companion object {
        val PROVIDER_ID:String
    }
}

external interface AuthResult:UserCredential