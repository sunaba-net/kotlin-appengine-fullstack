package net.sunaba.appengine

import io.ktor.application.ApplicationCall
import io.ktor.http.HttpStatusCode
import io.ktor.response.respond
import java.lang.Exception

class TestRetry(val retryCount: Int) : DeferredTask {
    override suspend fun run(call: ApplicationCall) {

        val count = call.request.headers["X-AppEngine-TaskRetryCount"]!!.toInt()

        println("RETRY COUNT:${count}")

        if (count < this.retryCount) {
            call.respond(HttpStatusCode.InternalServerError, "InternalServerError")
            throw Exception("ERROR")
        }
    }
}