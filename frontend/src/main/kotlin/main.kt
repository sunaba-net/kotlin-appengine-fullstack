import kotlinx.css.*

import vue.Vue
import vue.VueComponent
import vue.style


class ButtonCounter : VueComponent<ButtonCounter.CountModel>({
    template = """
        <button v-on:click="onClick" v-bind:style="buttonStyle()">
            You clicked me {{ count }} times.
        </button>
    """
    data = { CountModel() }
    styles {
        "test".style {
            color = Color.red
            fontSize = 200.px
        }
    }
}) {
    data class CountModel(var count: Int = 0)

    fun onClick() {
        data.count += 1
    }

    fun buttonStyle() = style {
        color = arrayOf(Color.red, Color("#00FF00"), rgb(0, 0, 255))[data.count % 3]
        fontSize = (20 + data.count).px
    }
}




fun main() {
    VueComponent("button-counter", ButtonCounter())
    Vue(object {
        val el = "#components-demo"
    })
}
