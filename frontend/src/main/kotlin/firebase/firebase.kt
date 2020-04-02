@file:JsModule("firebase")

import firebase.app.App

external interface Firebase: App {
    fun initializeApp(options: dynamic, name: String? = definedExternally): App
    val apps: Array<App>
    val SDK_VERSION: String
}

@JsName("default")
external val firebase:Firebase

