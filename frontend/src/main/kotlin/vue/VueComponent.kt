package  vue

import kotlinx.css.CSSBuilder

class VueData<T>(var data: T)

class Styles {

    val classes = js("{}")

    fun String.style(css: CSSBuilder.() -> Unit) {
        classes[this] = CSSBuilder().apply(css).toString()
    }
}

class VueComponentConfig<T>() {
    var template: String = ""
    var data: (() -> T) = { js("{}") as T }
    val styles:Styles = Styles()
    fun styles(style: Styles.() -> Unit) = styles.apply(style)
}

abstract class VueComponent<T : Any>(val config: VueComponentConfig<T>.() -> Unit) {
    inline val data: T
        get() = js("this.\$data") as T
}

fun <T:Any> VueComponent<T>.style(builder: CSSBuilder.()->Unit) = CSSBuilder().apply(builder).toString()

inline fun <reified T : Any> VueComponent(name: String, component: VueComponent<T>) {
    Vue.component(name, component.toVueObject())
}

external object Object {
    fun <T, R : T> assign(dest: R, vararg src: T): R
}

inline fun <reified T : Any> VueComponent<T>.toVueObject(): Any {
    val config = VueComponentConfig<T>().apply(config)
    println(JSON.stringify(config.styles))
    return object {
        val data: () -> T = {Object.assign(config.data.invoke(), config.styles.classes)}
        val template = config.template
        val methods = this@toVueObject
    }
}
