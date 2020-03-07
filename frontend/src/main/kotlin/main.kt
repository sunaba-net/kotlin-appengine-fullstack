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
    VueComponent("button-counter", ButtonCounter())
    VueComponent("button-counter2", ButtonCounter2())
    VueComponent("reverse-button", ReverseButton())
    Vue(object {
        val el = "#components-demo"
    })
}