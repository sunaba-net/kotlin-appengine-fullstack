package net.sunaba.serialization

import javax.lang.model.element.VariableElement

interface SerialProperty {
    val element: VariableElement
    val serialName: String
    val name: String
}