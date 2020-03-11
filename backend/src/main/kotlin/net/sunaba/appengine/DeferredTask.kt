package net.sunaba.appengine

import io.ktor.application.ApplicationCall
import java.io.Serializable

interface DeferredTask : Serializable {
    fun run(call: ApplicationCall)
}