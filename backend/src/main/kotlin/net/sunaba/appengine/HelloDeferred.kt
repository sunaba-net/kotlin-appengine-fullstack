package net.sunaba.appengine

import java.io.Serializable

class HelloDeferred(val name:String):DeferredTask {

    override fun run() {
        println("Hello ${name}!!")
    }
}