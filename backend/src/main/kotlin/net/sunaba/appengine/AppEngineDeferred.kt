package net.sunaba.appengine

import com.google.cloud.tasks.v2.*
import com.google.protobuf.ByteString
import io.ktor.application.Application
import io.ktor.application.ApplicationFeature
import io.ktor.application.call
import io.ktor.application.feature
import io.ktor.http.HttpStatusCode
import io.ktor.response.respond
import io.ktor.routing.post
import io.ktor.routing.routing
import io.ktor.util.AttributeKey
import io.ktor.utils.io.jvm.javaio.toInputStream
import java.io.ByteArrayOutputStream
import java.io.ObjectInputStream
import java.io.ObjectOutputStream
import java.net.HttpURLConnection
import java.net.URL
import java.net.UnknownHostException

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

    /**
     * @property executePath
     * @property idTokenVerification
     * @see https://cloud.google.com/compute/docs/instances/verifying-instance-identity
     */
    class Configuration(var executePath: String = "/queue/__deferred__"
                        , var idTokenVerification: Boolean = true
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
            val configuration = Configuration().apply(configure)
            val feature = AppEngineDeferred(configuration)
            pipeline.routing {
                post(configuration.executePath) {
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
        val queuePath = QueueName.of(config.projectId, config.region, queue)
        val requestBuilder = AppEngineHttpRequest.newBuilder()
                .setRelativeUri(path)
                .setBody(ByteString.copyFrom(body))
                .setHttpMethod(HttpMethod.POST)

        val task = Task.newBuilder().setAppEngineHttpRequest(requestBuilder.build())
        it.createTask(queuePath, task.build())
    }
}


