import vue.Vue
import vue.VueComponent

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

class ButtonCounter2:ButtonCounter() {
    override fun onClick() {
        data.count +=10
    }
}

fun main() {
    VueComponent("button-counter", ButtonCounter())
    VueComponent("button-counter2", ButtonCounter2())
    Vue(object {
        val el = "#components-demo"
    })
}