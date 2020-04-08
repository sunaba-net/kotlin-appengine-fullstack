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
import java.util.*
import kotlin.random.Random

private const val SALT_LENGTH: Int = 8
private const val HEADER_AUTHORIZATION = "X-Deferred-Authorization"
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
//                val iam = Iam.Builder(NetHttpTransport(), JacksonFactory.getDefaultInstance()
//                        , HttpCredentialsAdapter(GoogleCredentials.getApplicationDefault())).build()
//                val name = "projects/{PROJECT_ID}/serviceAccounts/{ACCOUNT}"
//                iam.projects().serviceAccounts().signBlob()
                (GoogleCredentials.getApplicationDefault() as ComputeEngineCredentials).sign(byteArray)
            }
        }

        private var _region: String = GCLOUD_REGION
        var taskSigner: Signer = APPLICATION_DEFAULT_CREDENTIAL_SIGNER

        var region: String
            get() = if (_region == GCLOUD_REGION) AppEngine.currentRegion!! else _region
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

                    val body = call.request.receiveChannel().toByteArray()
                    val sign = config.taskSigner.invoke(body)
                    val base64Sign = Base64.getEncoder().encodeToString(sign)
                    if (base64Sign != call.request.headers.get(HEADER_AUTHORIZATION)) {
                        call.respond(HttpStatusCode.Unauthorized, "Invalid Signature")
                        return@post
                    }
                    val deferredTask = ByteArrayInputStream(body).use {
                        it.skip(SALT_LENGTH.toLong()) //salt
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
    // 仮に失敗してもリトライされる
    if (true != headers["X-AppEngine-TaskETA"]?.let { Math.abs(it.split(".").first().toLong() - scheduleTime.seconds) < 5 }) {
        return false
    }
    return true
}

fun Application.deferred(task: DeferredTask, queue: String = "default", builder: AppEngineRouting.Builder.() -> Unit = {}): Task {
    val feature = feature(AppEngineDeferred)
    val config = feature.config
    val path = config.path

    return CloudTasksClient.create().use { client ->
        val salt = Random.nextBytes(SALT_LENGTH)
        val body = ByteArrayOutputStream().use { it ->
            it.write(salt)
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
                .putHeaders(HEADER_AUTHORIZATION, base64Sign)
                .setHttpMethod(HttpMethod.POST)
                .setAppEngineRouting(AppEngineRouting.newBuilder().apply(builder))

        val taskBuilder = Task.newBuilder().setAppEngineHttpRequest(requestBuilder.build())
        client.createTask(queuePath, taskBuilder.build())
    }
}


