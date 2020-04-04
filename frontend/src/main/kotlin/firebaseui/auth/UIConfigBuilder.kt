package firebaseui.auth

class UIConfigBuilder {
    private val obj = js("{}")

    class CallbackBuilder {
        private val obj = js("{}")
        fun signInSuccessWithAuthResult(action: (authResult: AuthResult, redirectUrl: String?) -> Unit) {
            obj.signInSuccessWithAuthResult = action
        }
        fun signInFailure(action: (error: dynamic) -> Unit) {
            obj.signInFailure = action
        }
        fun uiShown(action: () -> Unit) {
            obj.uiShown = action
        }
        fun build() = obj
    }

    fun callbacks(builder: CallbackBuilder.() -> Unit) {
        obj.callbacks = CallbackBuilder().apply(builder).build()
    }
    var signInOptions: Array<dynamic> = arrayOf()
        set(value) {
            obj.signInOptions = value
            field = value
        }

    var signInFlow:SignInFlow = SignInFlow.REDIRECT
        set(value) {
            obj.signInFlow = value.value
            field = value
        }

    var tosUrl:String = ""
        set(value) {
            obj.tosUrl = value
            field = value
        }
    var privacyPolicyUrl:String = ""
        set(value) {
            obj.privacyPolicyUrl = value
            field = value
        }

    fun build() = obj
}

enum class SignInFlow(val value:String) {
    REDIRECT("redirect"), POPUP("popup")
}

fun uiConfig(configure: UIConfigBuilder.() -> Unit) = UIConfigBuilder().apply(configure).build()


