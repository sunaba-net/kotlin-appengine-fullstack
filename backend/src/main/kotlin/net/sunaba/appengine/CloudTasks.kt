package net.sunaba.appengine

import com.google.cloud.tasks.v2.*
import com.google.protobuf.ByteString
import io.ktor.application.Application
import io.ktor.application.ApplicationFeature
import io.ktor.application.call
import io.ktor.http.HttpStatusCode
import io.ktor.request.header
import io.ktor.response.respond
import io.ktor.routing.post
import io.ktor.routing.routing
import io.ktor.util.AttributeKey
import io.ktor.utils.io.jvm.javaio.toInputStream
import java.io.ByteArrayOutputStream
import java.io.ObjectInputStream
import java.io.ObjectOutput
import java.io.ObjectOutputStream

class CloudTasks(internal val config: Configuration) {

    class Configuration

    companion object Feature : ApplicationFeature<Application, Configuration, CloudTasks> {
        override val key: AttributeKey<CloudTasks>
            get() = AttributeKey("Cloud Tasks")

        override fun install(pipeline: Application, configure: Configuration.() -> Unit): CloudTasks {

            val configuration = Configuration().apply(configure)
            val feature = CloudTasks(configuration)

            pipeline.routing {
                post("/_ah/__deferred__") {
                    val deferredTask = call.request.receiveChannel().toInputStream().use {
                        ObjectInputStream(it).use {
                            it.readObject()
                        }
                    } as DeferredTask
                    deferredTask.run()
                    call.respond(HttpStatusCode.OK)
                }
            }
            return feature;
        }

        fun addTask(task: DeferredTask, queue: String = "default") =
                CloudTasksClient.create().use {
                    val body = ByteArrayOutputStream().use {
                        ObjectOutputStream(it).use {
                            it.writeObject(task)
                        }
                        it.toByteArray()
                    }

                    val queuePath = QueueName.of(AppEngine.Env.GOOGLE_CLOUD_PROJECT.value
                            , AppEngine.currentRegion, queue)
                    val task = Task.newBuilder().setAppEngineHttpRequest(AppEngineHttpRequest.newBuilder()
                            .setRelativeUri("/_ah/__deferred__")
                            .setBody(ByteString.copyFrom(body))
                            .setHttpMethod(HttpMethod.POST).build())
                    it.createTask(queuePath, task.build())
                }
    }
}


