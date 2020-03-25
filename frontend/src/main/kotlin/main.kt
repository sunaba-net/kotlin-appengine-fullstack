import vue.Vue
import vue.VueComponent
import vue.VueData
import websocket.AutoWebSocketClient


class ButtonCounter : VueComponent<ButtonCounter.CountModel>({
    template = """
        <button v-on:click="onClick" v-bind:style="buttonStyle()">
            You clicked me {{ count }} times.
        </button>
    """
    data = { CountModel() }
}) {
    data class CountModel(var count: Int = 0)

    fun onClick() {
        data.count += 1
    }

    fun buttonStyle() = object {
        val color = arrayOf("red", "green", "blue")[data.count % 3]
        val fontSize = (20 + data.count).toString() + "px"
    }
}

external object require {
    fun resolve(module: String): String
}

external fun require(module: String): dynamic

inline operator fun require.invoke(module: String) = asDynamic()(module)

suspend fun main() {

    VueComponent("button-counter", ButtonCounter())
    Vue(object {
        val el = "#components-demo"
    })
}
