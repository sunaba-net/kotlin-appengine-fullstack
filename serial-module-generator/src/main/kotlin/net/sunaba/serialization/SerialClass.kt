package net.sunaba.serialization

import javax.lang.model.element.TypeElement

interface SerialClass {
    val element:TypeElement
    val isClass: Boolean
    val isInterface: Boolean
    val qualifiedName: String
    val serialName: String
    val hasSerializer: Boolean
    val properties: List<SerialProperty>
    val declaredProperties: List<SerialProperty>
    val superclass: SerialClass?
    val interfaces: List<SerialClass>
    val allSuperClassInterfaces: Set<SerialClass>
}