@file:JsModule("firebase")

import firebase.Firestore
import firebase.app.App
import firebase.auth.Auth

external interface Firebase : App {
    fun initializeApp(options: dynamic, name: String? = definedExternally): App
    val apps: Array<App>
    val SDK_VERSION: String

    fun auth(app: App? = definedExternally): Auth
    fun firestore(app: App? = definedExternally): Firestore
}

@JsName("default")
external val firebase: Firebase


external interface FirebaseError {
    val code: String
    val message: String
    val name: String
    val stack: String?
}