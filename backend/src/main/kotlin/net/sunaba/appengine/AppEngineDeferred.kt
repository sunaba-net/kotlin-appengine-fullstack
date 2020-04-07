package net.sunaba.appengine

import com.google.auth.oauth2.ComputeEngineCredentials
import com.google.auth.oauth2.GoogleCredentials
import com.google.cloud.tasks.v2.*
import com.google.protobuf.ByteString
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
import io.ktor.util.toByteArray
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.ObjectInputStream
import java.io.ObjectOutputStream
import java.net.HttpURLConnection
import java.net.URL
import java.net.UnknownHostException
import java.util.*

private fun getMetaData(path: String, defaultValue: String): String {
    val conn = URL("http://metadata.google.internal${path}").openConnection() as HttpURLConnection
    conn.requestMethod = "POST"
    conn.setRequestProperty("Metadata-Flavor", "Google")
    return try {
        conn.inputStream.use {
            it.reader(Charsets.UTF_8).readText().trim().let {
                it.split("/").last().let { it.substring(0, it.lastIndexOf('-')) }
            }
        }
    } catch (ex: UnknownHostException) {
        defaultValue
    }
}

class AppEngineDeferred(internal val config: Configuration) {

    class Configuration(var executePath: String = "/queue/__deferred__"
                        , var projectId: String = System.getenv("GOOGLE_CLOUD_PROJECT") ?: ""
    ) {
        companion object {
            // Acquisition of metadata is costly, so delay acquisition
            const val GCLOUD_REGION: String = "__GCLOUD_REGION__"
        }

        private val gcloudRegion: String by lazy { getMetaData("/computeMetadata/v1/instance/zone", "") }

        private var _region: String = GCLOUD_REGION

        var region: String
            get() = if (_region == GCLOUD_REGION) gcloudRegion else _region
            set(value) {
                _region = value
            }
    }

    companion object Feature : ApplicationFeature<Application, Configuration, AppEngineDeferred> {
        override val key = AttributeKey<AppEngineDeferred>("AppEngine Deferred")

        override fun install(pipeline: Application, configure: Configuration.() -> Unit): AppEngineDeferred {
            val config = Configuration().apply(configure)
            val feature = AppEngineDeferred(config)
            pipeline.routing {
                post(config.executePath) {
                    println("appengine deferred")
                    val body = call.request.receiveChannel().toByteArray()

                    val taskId = call.request.headers.get("X-AppEngine-TaskName")
                    val queue = call.request.headers.get("X-AppEngine-QueueName")
                    if (taskId.isNullOrEmpty() || queue.isNullOrEmpty()) {
                        call.respond(HttpStatusCode.Unauthorized, "Invalid TaskName")
                        return@post
                    }

                    val taskName = TaskName.format(config.projectId, config.region, queue, taskId)
                    val task = CloudTasksClient.create().use { it.getTask(taskName) }
                    println("getTask: ${task}")
                    if (task == null || !task.verify(call.request.headers)) {
                        println("Invalid Task: ${taskName}")
                        call.respond(HttpStatusCode.Unauthorized, "Invalid Task")
                        return@post
                    }

                    val sign = sign(body)
                    if (sign != call.request.headers.get(HEADER_SIGNATURE)) {
                        println("Invalid Signature: ${taskName}")
                        call.respond(HttpStatusCode.Unauthorized, "Invalid Signature")
                        return@post
                    }
                    val deferredTask = ByteArrayInputStream(body).use {
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

private fun Task.verify(headers:Headers):Boolean {
    println("X-AppEngine-TaskRetryCount: ${headers["X-AppEngine-TaskRetryCount"]}, ${this.dispatchCount}")
    println("X-AppEngine-TaskExecutionCount: ${headers["X-AppEngine-TaskExecutionCount"]}, ${this.responseCount}")
    println("X-AppEngine-TaskETA:${headers["X-AppEngine-TaskETA"]}, ${this.scheduleTime}") // 誤差がある

    if (dispatchCount.toString() != headers["X-AppEngine-TaskRetryCount"])  {
        return false;
    }
    if (responseCount.toString() != headers["X-AppEngine-TaskRetryCount"])  {
        return false;
    }

    return true
}

private const val HEADER_SIGNATURE = "X-DEFERRED-SIGNATURE"

private fun sign(body: ByteArray): String {
    val sign: ByteArray = (GoogleCredentials.getApplicationDefault() as ComputeEngineCredentials)
            .sign(body)
    return Base64.getEncoder().encodeToString(sign)
}


fun Application.deferred(task: DeferredTask, queue: String = "default"): Task {
    val feature = feature(AppEngineDeferred)
    val config = feature.config
    val path = config.executePath

    return CloudTasksClient.create().use {
        val body = ByteArrayOutputStream().use {
            ObjectOutputStream(it).use {
                it.writeObject(task)
            }
            it.toByteArray()
        }
        val sign = sign(body)
        val queuePath = QueueName.of(config.projectId, config.region, queue)
        val requestBuilder = AppEngineHttpRequest.newBuilder()
                .setRelativeUri(path)
                .setBody(ByteString.copyFrom(body))
                .putHeaders(HEADER_SIGNATURE, sign)
                .setHttpMethod(HttpMethod.POST)
        val task = Task.newBuilder().setAppEngineHttpRequest(requestBuilder.build())
        it.createTask(queuePath, task.build())
    }
}


