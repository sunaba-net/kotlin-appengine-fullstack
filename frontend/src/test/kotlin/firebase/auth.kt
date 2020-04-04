package firebase

import firebase.auth.EmailAuthProvider
import firebase.auth.FacebookAuthProvider
import firebase.auth.GoogleAuthProvider
import firebaseui.auth.AnonymousAuthProvider
import kotlin.test.Test
import kotlin.test.assertEquals

class ProviderTest {
    @Test
    fun providerId() {
        assertEquals("google.com", GoogleAuthProvider.PROVIDER_ID)
        assertEquals("facebook.com", FacebookAuthProvider.PROVIDER_ID)
        assertEquals("password", EmailAuthProvider.PROVIDER_ID)
        assertEquals("anonymous", AnonymousAuthProvider.PROVIDER_ID)
    }
}