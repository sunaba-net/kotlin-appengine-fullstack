package net.sunaba.google.api

import com.google.api.client.googleapis.batch.BatchRequest
import com.google.api.client.googleapis.batch.json.JsonBatchCallback
import com.google.api.client.googleapis.json.GoogleJsonError
import com.google.api.client.googleapis.services.json.AbstractGoogleJsonClientRequest
import com.google.api.client.http.HttpHeaders

/**
 *
 *
 *
 *
 *
 */

interface ApiResult<T> {
    fun onSuccess(action: (t: T, responseHeaders: HttpHeaders?) -> Unit)
    fun onFailure(action: (e: GoogleJsonError?, responseHeaders: HttpHeaders?) -> Unit)
}

fun <T> AbstractGoogleJsonClientRequest<T>.queue(batch: BatchRequest, result: ApiResult<T>.() -> Unit): Unit {
    this.queue(batch, object : JsonBatchCallback<T>() {
        override fun onSuccess(t: T, responseHeaders: HttpHeaders?) {
            object: ApiResult<T> {
                override fun onSuccess(action: (t: T, responseHeaders: HttpHeaders?) -> Unit) {
                    action.invoke(t, responseHeaders)
                }
                override fun onFailure(action: (e: GoogleJsonError?, responseHeaders: HttpHeaders?) -> Unit) {
                }
            }.apply(result)
        }

        override fun onFailure(e: GoogleJsonError?, responseHeaders: HttpHeaders?) {
            object: ApiResult<T> {
                override fun onSuccess(action: (t: T, responseHeaders: HttpHeaders?) -> Unit) {
                }
                override fun onFailure(action: (e: GoogleJsonError?, responseHeaders: HttpHeaders?) -> Unit) {
                    action.invoke(e,responseHeaders)
                }
            }.apply(result)
        }
    })
}