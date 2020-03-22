package model

import kotlinx.serialization.Serializable
import kotlinx.serialization.modules.SerializersModule


interface Message

@Serializable
class IntMessage(val value:Int):Message

@Serializable
class StringMessage(val value:String):Message

val module = SerializersModule {
    polymorphic(Message::class) {
        IntMessage::class with IntMessage.serializer()
        StringMessage::class with StringMessage.serializer()
    }
}