package net.sunaba.appengine

import java.io.Serializable

interface TaskInformation {
    val queueName:String
    val taskName:String
    val taskRetryCount:Int
    val taskExecutionCount:Int
    val taskEta:Int
    val previousResponse:Int?
    val retryReason:String?
    val failFast:String?
}

interface DeferredTask:Serializable {
    fun run()
}