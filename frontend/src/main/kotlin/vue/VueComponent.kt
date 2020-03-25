package  vue

class VueData<T>(var data: T)

class VueComponentConfig<T>() {
    var template: String = ""
    var data: (() -> T) = { js("{}") as T }
    var styles = js("{}")
}

abstract class VueComponent<T : Any>(val config: VueComponentConfig<T>.() -> Unit) {
    inline val data: T
        get() = js("this.\$data") as T
}

inline fun <reified T : Any> VueComponent(name: String, component: VueComponent<T>) {
    Vue.component(name, component.toVueObject())
}

external object Object {
    fun <T, R : T> assign(dest: R, vararg src: T): R
}

inline fun <reified T : Any> VueComponent<T>.toVueObject(): Any {
    val config = VueComponentConfig<T>().apply(config)

    return object {
        val data: () -> T = {Object.assign(config.data.invoke(), config.styles)}
        val template = config.template
        val methods = this@toVueObject
    }
}
