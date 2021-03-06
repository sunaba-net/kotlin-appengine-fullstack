package net.sunaba.appengine


import com.google.cloud.tasks.v2.*
import com.google.protobuf.ByteString
import com.google.protobuf.Timestamp
import io.ktor.application.Application
import io.ktor.application.ApplicationFeature
import io.ktor.application.call
import io.ktor.application.feature
import io.ktor.http.Headers
import io.ktor.http.HttpStatusCode
import io.ktor.response.respond
import io.ktor.routing.post
import io.ktor.routing.routing
import io.ktor.util.AttributeKey
import io.ktor.utils.io.jvm.javaio.toInputStream
import java.io.ByteArrayOutputStream
import java.io.ObjectInputStream
import java.io.ObjectOutputStream
import java.util.*
import kotlin.random.Random


private const val TOKEN_BYTES: Int = 64
private const val HEADER_TOKEN = "X-Deferred-Token"

class AppEngineDeferred(internal val config: Configuration) {

    class Configuration(var path: String = "/queue/__deferred__"
                        , var projectId: String = System.getenv("GOOGLE_CLOUD_PROJECT") ?: ""
    ) {
        companion object {
            // Acquisition of getCurrentLocation is costly, so delay acquisition
            const val PROJECT_LOCATION: String = "__PROJECT_LOCATION__"
            private val CLIENT_INSTANCE: CloudTasksClient by lazy {
                CloudTasksClient.create()
            }
        }

        var location: String = PROJECT_LOCATION
            get() = if (field == PROJECT_LOCATION) {
                AppEngine.currentLocation!!.let {
                    //https://cloud.google.com/tasks/docs/tutorial-gcf
                    // Note that two locations, called europe-west and us-central in App Engine commands, are called, respectively, europe-west1 and us-central1 in Cloud Tasks commands.
                    when (AppEngine.currentLocation!!) {
                        "europe-west" -> "europe-west1"
                        "us-central" -> "us-central1"
                        else-> it
                    }
                }
            } else {field}

        var cloudTasksClientProvider: () -> CloudTasksClient = {
            CLIENT_INSTANCE
        }
    }

    companion object Feature : ApplicationFeature<Application, Configuration, AppEngineDeferred> {
        override val key = AttributeKey<AppEngineDeferred>("AppEngine Deferred")

        override fun install(pipeline: Application, configure: Configuration.() -> Unit): AppEngineDeferred {
            val config = Configuration().apply(configure)
            val feature = AppEngineDeferred(config)
            pipeline.routing {
                post(config.path) {
                    val taskId = call.request.headers["X-AppEngine-TaskName"]
                    val queue = call.request.headers["X-AppEngine-QueueName"]
                    if (taskId.isNullOrEmpty() || queue.isNullOrEmpty()) {
                        call.respond(HttpStatusCode.Unauthorized, "Invalid TaskName")
                        return@post
                    }

                    val taskName = TaskName.format(config.projectId, config.location, queue, taskId)
                    val basicTaskRequest = GetTaskRequest.newBuilder().setName(taskName).setResponseView(Task.View.BASIC).build()
                    val task = config.cloudTasksClientProvider().getTask(basicTaskRequest)
                    if (task == null || !task.verify(call.request.headers)) {
                        call.respond(HttpStatusCode.Unauthorized, "Invalid Task")
                        return@post
                    }

                    val deferredTask = call.request.receiveChannel().toInputStream().use {
                        ObjectInputStream(it).use {
                            it.readObject()
                        }
                    } as DeferredTask
                    deferredTask.run(call)
                    call.respond(HttpStatusCode.OK)
                }
            }
            return feature;
        }
    }
}

private fun Task.verify(headers: Headers): Boolean {
    val taskToken = appEngineHttpRequest.getHeadersOrThrow(HEADER_TOKEN)!!
    val requestHeaderToken = headers[HEADER_TOKEN]!!
    return taskToken == requestHeaderToken
}

val IMMEDIATE: Date = Date(0L)

private fun Date.toTimestamp(): Timestamp = Timestamp.newBuilder()
        .setSeconds(time / 1000)
        .setNanos(1000 * (time % 1000).toInt()).build()


fun Application.deferred(task: DeferredTask, queue: String = "default", service: String = "default"
                         , scheduleTime: Date = IMMEDIATE): Task {
    val feature = feature(AppEngineDeferred)
    val config = feature.config
    val path = config.path


    val body = ByteArrayOutputStream().use { it ->
        ObjectOutputStream(it).use {
            it.writeObject(task)
        }
        it.toByteArray()
    }
    val random = Base64.getEncoder().encodeToString(Random.nextBytes(TOKEN_BYTES))
    val queuePath = QueueName.of(config.projectId, config.location, queue)
    val requestBuilder = AppEngineHttpRequest.newBuilder()
            .setRelativeUri(path)
            .setBody(ByteString.copyFrom(body))
            .putHeaders(HEADER_TOKEN, random)
            .setHttpMethod(HttpMethod.POST)
            .setAppEngineRouting(AppEngineRouting.newBuilder().setService(service))

    val taskBuilder = Task.newBuilder()
            .setAppEngineHttpRequest(requestBuilder.build())
    if (scheduleTime != IMMEDIATE) {
        taskBuilder.setScheduleTime(scheduleTime.toTimestamp())
    }

    return config.cloudTasksClientProvider().createTask(queuePath, taskBuilder.build())
}