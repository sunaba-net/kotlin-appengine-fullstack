@file:JsModule("firebase/app")

package firebase.app

import firebase.Firestore
import firebase.auth.Auth


external interface App {
    val name: String
    val options: dynamic
}

