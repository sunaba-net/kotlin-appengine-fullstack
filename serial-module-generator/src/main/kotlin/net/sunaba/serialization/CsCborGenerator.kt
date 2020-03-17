package net.sunaba.serialization

import javax.lang.model.element.TypeElement
import javax.lang.model.type.ArrayType
import javax.lang.model.type.DeclaredType
import javax.lang.model.type.TypeKind
import javax.lang.model.type.TypeMirror

/**
 * C# 用のモデルジェネレータ
 */
object CsCborGenerator {
    fun generate(cls: SerialClass) {

        println("============================")

        val superClassAndInterfaces = if (cls.superclass != null) {
            listOf(cls.superclass!!, *cls.interfaces.toTypedArray())
        } else cls.interfaces

        println("(${cls.classOrInterface} ${cls.qualifiedName}"
                + (if (superClassAndInterfaces.isNotEmpty()) ": " else "")
                + superClassAndInterfaces.joinToString { it.qualifiedName } + " {")

        cls.declaredProperties.forEach {
            println("    ${it.element.asType().csTypeName} ${it.name};")
        }
        println("}")

        println("class ${cls.qualifiedName}Converter: ICBORToFromConverter<${cls.qualifiedName}> {")

        cls.declaredProperties.forEach {
            println("    ${it.element.asType().csTypeName} ${it.name};")
        }
        println("}")

    }

    val SerialClass.classOrInterface
        get() = if (isClass) "class" else "interface"

    val SerialClass.simpleName
        get() = qualifiedName.split(".").last()

    val SerialClass.packageName
        get() = qualifiedName.split(".").dropLast(1).joinToString(".")

    val TypeMirror.csTypeName: String
        get() = when (this.kind) {
            TypeKind.BOOLEAN -> "bool"
            TypeKind.BYTE -> "byte"
            TypeKind.SHORT -> "short"
            TypeKind.INT -> "int"
            TypeKind.LONG -> "long"
            TypeKind.CHAR -> "char"
            TypeKind.FLOAT -> "float"
            TypeKind.DOUBLE -> "double"
            TypeKind.VOID -> "void"
            TypeKind.ARRAY -> (this as ArrayType).componentType.csTypeName + "[]"
            TypeKind.DECLARED -> (this as DeclaredType).csTypeName
            else -> {
                "unknown " + this.toString()
            }
        }
    val ArrayType.csTypeName: String
        get() = this.componentType.csTypeName + "[]"

    val DeclaredType.csTypeName: String
        get() {
            val element = this.asElement() as TypeElement
            return when (element.qualifiedName.toString()) {
                java.lang.String::class.java.name -> "string"
                java.util.List::class.java.name -> "List<" + this.typeArguments.joinToString { it.csTypeName } + ">"
                java.util.Map::class.java.name -> "Map<" + this.typeArguments.joinToString { it.csTypeName } + ">"
                else -> this.toString()
            }
        }

}