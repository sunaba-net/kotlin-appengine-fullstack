
import com.soywiz.korio.async.async
import com.soywiz.korio.net.ws.WebSocketClient
import kotlinx.coroutines.Dispatchers
import kotlinx.serialization.PolymorphicSerializer
import vue.Vue
import vue.VueComponent
import vue.VueComponentConfig
import vue.VueData
import websocket.AutoWebSocketClient


class ChatModel {

    val websocket:AutoWebSocketClient = AutoWebSocketClient("ws://localhost:8081/hoge")

}

open class Chat:VueComponent<ChatModel>({

    template = """
        <span>
        {{websocket}}
        </span>
    """.trimIndent()
    data = {ChatModel()}

    val hoge = 123
}) {


}

fun <T> register(name:String, configure:VueComponentConfig<T>.()->Unit) {

}

fun hoge() {
    register<String>("") {
        template = ""
        data = {""}
    }
}

open class ButtonCounter : VueComponent<ButtonCounter.CountModel>({
    template = """
        <button v-on:click="${ButtonCounter::onClick.name}">
            You clicked me {{ count }} times.
        </button>
    """
    data = { CountModel() }
}) {
    class CountModel(var count: Int = 0)

    open fun onClick() {
        data.count += 1
    }
}

class ReverseButton : VueComponent<VueData<String>>(
        {
            template = """<button v-on:click="${ReverseButton::reverseMessage.name}">{{data}}</button>"""
            data = { VueData("reverse message") }
        }
) {
    fun reverseMessage() {
        data.data = data.data.reversed()
    }
}

class ButtonCounter2 : ButtonCounter() {
    init {
    }

    override fun onClick() {
        data.count += 100

        async(Dispatchers.Default) {
            webSocketClient.send("Hello")
        }

    }
}

lateinit var webSocketClient: AutoWebSocketClient


suspend fun main() {
    webSocketClient = AutoWebSocketClient("ws://localhost:8081/chat", DEBUG = true).apply { connect() }
//    webSocketClient.onStringMessage {
//        PolymorphicSerializer<User>(User::class)
//    }
//    webSocketClient.onError.add {
//        println(it)
//    }
//    webSocketClient.onClose.add {
//        println("onClose")
//    }
//    val client = HttpClient()
//    val json = Json(JsonConfiguration.Stable)
//    val user1 = json.parse(User.serializer(), client.readString("/json/user"))
//    println(user1)


    VueComponent("button-counter", ButtonCounter())
    VueComponent("button-counter2", ButtonCounter2())
    VueComponent("reverse-button", ReverseButton())
    VueComponent("chat", Chat())
    Vue(object {
        val el = "#components-demo"
    })


}