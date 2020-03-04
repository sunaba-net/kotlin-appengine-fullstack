@file:JsModule("vue/dist/vue.esm.js")

package vue

@JsName("default")
external class Vue(options:dynamic) {
    companion object {
        fun component(name:String, options:dynamic):Any
    }
}

