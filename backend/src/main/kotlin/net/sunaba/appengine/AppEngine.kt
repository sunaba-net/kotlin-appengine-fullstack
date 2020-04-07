package net.sunaba.appengine

import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.request.headers
import kotlinx.coroutines.*
import kotlin.coroutines.CoroutineContext


object AppEngine : CoroutineScope {

    private val job = SupervisorJob()

    override val coroutineContext: CoroutineContext
        get() = Dispatchers.IO + job

    /**
     *  インスタンスIDが取得できたらサービス環境と判断する
     */
    val isServiceEnv: Boolean by lazy { Env.GAE_INSTANCE.value.isNotEmpty() }

    val isLocalEnv: Boolean = !isServiceEnv

    //https://cloud.google.com/appengine/docs/standard/java11/runtime
    enum class Env(val key: String) {
        GAE_APPLICATION("GAE_APPLICATION"),
        GAE_DEPLOYMENT_ID("GAE_DEPLOYMENT_ID"),
        GAE_ENV("GAE_ENV"),
        GAE_INSTANCE("GAE_INSTANCE"),
        GAE_MEMORY_MB("GAE_MEMORY_MB"),
        GAE_RUNTIME("GAE_RUNTIME"),
        GAE_SERVICE("GAE_SERVICE"),
        GAE_VERSION("GAE_VERSION"),
        GOOGLE_CLOUD_PROJECT("GOOGLE_CLOUD_PROJECT"),
        NODE_ENV("NODE_ENV"),
        PORT("PORT");

        val value: String by lazy {
            System.getenv(key) ?: ""
        }
    }

    /**
     * @see https://cloud.google.com/compute/docs/storing-retrieving-metadata?hl=ja
     */
    enum class MetaPath(val path: String) {
        NUMERIC_PROJECT_ID("/computeMetadata/v1/project/numeric-project-id"),
        PROJECT_ID("/computeMetadata/v1/project/project-id"),
        ZONE("/computeMetadata/v1/instance/zone"),
        SERVICE_ACCOUNT_DEFAULT_ALIASES("/computeMetadata/v1/instance/service-accounts/default/aliases"),
        SERVICE_ACCOUNT_DEFAULT("/computeMetadata/v1/instance/service-accounts/default/"),
        SERVICE_ACCOUNT_DEFAULT_SCOPES("/computeMetadata/v1/instance/service-accounts/default/scopes");

        val value:String by lazy {
            metaData[this]?:""
        }
    }

    val metaData: Map<MetaPath, String> by lazy {
        runBlocking {
            val client = HttpClient()
            MetaPath.values().map {
                it to async {
                    client.get<String>("http://metadata.google.internal${it.path}") {
                        headers {
                            append("Metadata-Flavor", "Google")
                        }
                    }
                }
            }.map { it.first to it.second.await() }.toMap()
        }
    }

    val currentRegion: String? by lazy {
        metaData[MetaPath.ZONE]?.let {
            it.split("/").last().let { it.substring(0, it.lastIndexOf('-')) }
        }
    }
}