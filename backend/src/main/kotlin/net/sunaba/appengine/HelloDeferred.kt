package net.sunaba.appengine

import io.ktor.application.ApplicationCall

class HelloDeferred(val name: String) : DeferredTask {
    override fun run(call: ApplicationCall) {
        println("Hello ${name}!!")
    }
}