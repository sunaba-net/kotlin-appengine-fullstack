@file:JsModule("firebase")
@file:JsQualifier("auth")

package firebase.auth

import firebase.app.App
import kotlin.js.Promise

external class Auth {
    val app: App
    val currentUser: User?
}

open external class UserInfo {
    val displayName: String?
    val email: String?
    val phoneNumber: String?
    val photoURL: String?
    val providerId: String
    val uid: String
}

external class User : UserInfo {
    val emailVerified: Boolean
    val isAnonymous: Boolean
    val metadata: UserMetadata
    val providerData: Array<UserInfo>
    val refreshToken: String
    val tenantId: String?

    fun delete(): Promise<Unit>
    fun getIdToken(forceRefresh: Boolean = definedExternally): Promise<String>
    fun getIdTokenResult(forceRefresh: Boolean = definedExternally): Promise<IdTokenResult>
    fun linkWithCredential(credential: AuthCredential): Promise<UserCredential>
}

external class UserMetadata {}

external class IdTokenResult {
    val authTime: String
    val claims: dynamic
    val expirationTime: String
    val issuedAtTime: String
    val signInProvider: String
    val token: String
}

open external class AuthCredential {
    val providerId: String
    val signInMethod: String
}

external class UserCredential

open external class AuthProvider {
    val providerId:String
}
external class EmailAuthProvider:AuthProvider {
    companion object {
        val EMAIL_LINK_SIGN_IN_METHOD:String
        val EMAIL_PASSWORD_SIGN_IN_METHOD:String
        val PROVIDER_ID:String
        fun credential(email:String, password:String):AuthCredential
        fun credentialWithLink(email: String, emailLink: String):AuthCredential
    }
}
external class FacebookAuthProvider:AuthProvider {
    companion object {
        val FACEBOOK_SIGN_IN_METHOD:String
        val PROVIDER_ID:String
        fun credential(token:String):OAuthCredential
    }
    fun addScope(scope:String):AuthProvider
    fun setCustomParameters(customOAuthParameters:dynamic):AuthProvider
}

external class GoogleAuthProvider:AuthProvider {
    companion object {
        val GOOGLE_SIGN_IN_METHOD:String
        val PROVIDER_ID:String
        fun credential(idToken :String, accessToken:String?):OAuthCredential
    }
    fun addScope(scope:String):AuthProvider
    fun setCustomParameters(customOAuthParameters:dynamic):AuthProvider
}

external class OAuthCredential:AuthCredential {
    val accessToken:String

    companion object {
        fun fromJSON(json:dynamic):AuthCredential
    }
}