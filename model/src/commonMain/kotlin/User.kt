package model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.modules.SerializersModule

interface Message

interface IIntMessage:Message {
    val intValue:Int
}

@Serializable
data class IntMessage(override val intValue:Int, val intValues:IntArray):IIntMessage

@Serializable
data class StringMessage(val stringValue:String):Message

@Serializable
data class User(val id:Int=123, @SerialName("myname") val name:String="name") {
    @SerialName("myna;me")
    val hoge = 123
}

@Serializable
data class Messages(val messages:List<Message> = listOf(IntMessage(1, intArrayOf(1,23)), StringMessage("Hello")))


val AutoSerialModule = SerializersModule {
    polymorphic(model.Message::class) {
        addSubclass(model.StringMessage::class, model.StringMessage.serializer())
        addSubclass(model.IntMessage::class, model.IntMessage.serializer())
    }
    polymorphic(model.IIntMessage::class) {
        addSubclass(model.IntMessage::class, model.IntMessage.serializer())
    }
}