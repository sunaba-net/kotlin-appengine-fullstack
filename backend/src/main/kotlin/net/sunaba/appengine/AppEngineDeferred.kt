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

private const val HEADER_AUTHORIZATIOn = "X-Deferred-Authorization"
typealias Signer = (taskPayload: ByteArray) -> ByteArray

class AppEngineDeferred(internal val config: Configuration) {

    class Configuration(var path: String = "/queue/__deferred__"
                        , var projectId: String = System.getenv("GOOGLE_CLOUD_PROJECT") ?: ""
    ) {
        companion object {
            // Acquisition of metadata is costly, so delay acquisition
            const val GCLOUD_REGION: String = "__GCLOUD_REGION__"

            /**
             * 使用するにはサービス「サービス アカウント トークン作成者」のRoleが必要
             *
             */
            val APPLICATION_DEFAULT_CREDENTIAL_SIGNER: Signer = { byteArray ->
                (GoogleCredentials.getApplicationDefault() as ComputeEngineCredentials).sign(byteArray)
            }
        }

        private val gcloudRegion: String by lazy { getMetaData("/computeMetadata/v1/instance/zone", "") }
        private var _region: String = GCLOUD_REGION
        var taskSigner: Signer = APPLICATION_DEFAULT_CREDENTIAL_SIGNER

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
                post(config.path) {
                    val body = call.request.receiveChannel().toByteArray()

                    val taskId = call.request.headers["X-AppEngine-TaskName"]
                    val queue = call.request.headers["X-AppEngine-QueueName"]
                    if (taskId.isNullOrEmpty() || queue.isNullOrEmpty()) {
                        call.respond(HttpStatusCode.Unauthorized, "Invalid TaskName")
                        return@post
                    }

                    val taskName = TaskName.format(config.projectId, config.region, queue, taskId)
                    val task = CloudTasksClient.create().use { it.getTask(taskName) }
                    if (task == null || !task.verify(call.request.headers)) {
                        call.respond(HttpStatusCode.Unauthorized, "Invalid Task")
                        return@post
                    }

                    val sign = config.taskSigner.invoke(body)
                    val base64Sign = Base64.getEncoder().encodeToString(sign)
                    if (base64Sign != call.request.headers.get(HEADER_AUTHORIZATIOn)) {
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

private fun Task.verify(headers: Headers): Boolean {
    if (true != headers["X-AppEngine-TaskRetryCount"]?.let { dispatchCount == it.toInt() }) {
        return false
    }
    if (true != headers["X-AppEngine-TaskExecutionCount"]?.let { responseCount == it.toInt() }) {
        return false
    }

    // headers["X-AppEngine-TaskETA"]は"1586219797.7147498"のような形式で取得できるが、Task#scheduleTimeとは誤差がある。
    // 何回か計測したところ、数ナノ秒程度の誤差だが余裕を持って5秒はOKとする
    if (true != headers["X-AppEngine-TaskETA"]?.let { Math.abs(it.split(".").first().toLong() - scheduleTime.seconds) < 5 }) {
        return false
    }
    return true
}

fun Application.deferred(task: DeferredTask, queue: String = "default", builder: AppEngineRouting.Builder.() -> Unit = {}): Task {
    val feature = feature(AppEngineDeferred)
    val config = feature.config
    val path = config.path

    return CloudTasksClient.create().use {
        val body = ByteArrayOutputStream().use {
            ObjectOutputStream(it).use {
                it.writeObject(task)
            }
            it.toByteArray()
        }
        val sign = config.taskSigner.invoke(body)
        val base64Sign = Base64.getEncoder().encodeToString(sign)
        val queuePath = QueueName.of(config.projectId, config.region, queue)
        val requestBuilder = AppEngineHttpRequest.newBuilder()
                .setRelativeUri(path)
                .setBody(ByteString.copyFrom(body))
                .putHeaders(HEADER_AUTHORIZATIOn, base64Sign)
                .setHttpMethod(HttpMethod.POST)
                .setAppEngineRouting(AppEngineRouting.newBuilder().apply(builder))

        val task = Task.newBuilder().setAppEngineHttpRequest(requestBuilder.build())
        it.createTask(queuePath, task.build())
    }
}


