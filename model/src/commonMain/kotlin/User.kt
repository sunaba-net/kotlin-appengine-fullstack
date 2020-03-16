package model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

interface Message


@Serializable
data class IntMessage(val intValue:Int, val intValues:IntArray):Message

@Serializable
data class StringMessage(val stringValue:String):Message

@Serializable
data class User(val id:Int=123, @SerialName("myname") val name:String="name") {
    @SerialName("myna;me")
    val hoge = 123
}

@Serializable
data class Messages(val messages:List<Message> = listOf(IntMessage(1, intArrayOf(1,23)), StringMessage("Hello")))
