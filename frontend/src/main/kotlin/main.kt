import com.soywiz.korio.net.http.HttpClient
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonConfiguration
import model.User
import vue.Vue
import vue.VueComponent
import vue.VueData

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
    }
}

suspend fun main() {

    val client = HttpClient()
    val json = Json(JsonConfiguration.Stable)
    val user1 = json.parse(User.serializer(), client.readString("http://localhost:8081/json/user"))
    println(user1)

    VueComponent("button-counter", ButtonCounter())
    VueComponent("button-counter2", ButtonCounter2())
    VueComponent("reverse-button", ReverseButton())
    Vue(object {
        val el = "#components-demo"
    })


}