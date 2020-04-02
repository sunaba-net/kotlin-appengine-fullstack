@file:JsModule("firebaseui")
@file:JsQualifier("auth")

package firebaseui.auth

import firebase.auth.Auth

external class AuthUI(auth:Auth) {
    fun start(container:String, uiConfig:dynamic)
}