package net.sunaba.appengine

import io.ktor.application.ApplicationCall
import io.ktor.request.ApplicationRequest
import io.ktor.request.header
import java.io.Serializable

interface DeferredTask : Serializable {
    suspend fun run(call: ApplicationCall)
}