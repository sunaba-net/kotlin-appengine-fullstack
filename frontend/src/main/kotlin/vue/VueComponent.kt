package  vue

class VueData<T>(var data:T)

class VueComponentConfig<T>() {
    var template: String = ""
    lateinit var data: (() -> T)
}

abstract class VueComponent<T : Any>(val config: VueComponentConfig<T>.() -> Unit) {
    inline val data: T
        get() = js("this.\$data") as T
}

inline fun <reified T:Any> VueComponent(name:String, component:VueComponent<T>) {
    Vue.component(name, component.toVueObject())
}

inline fun <reified T : Any> VueComponent<T>.toVueObject(): Any {
    val config = VueComponentConfig<T>().apply(config)
    return object {
        val data: () -> T = config.data
        val template = config.template
        val methods = this@toVueObject
    }
}
