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

        println("${cls.classOrInterface} ${cls.qualifiedName}"
                + (if (superClassAndInterfaces.isNotEmpty()) ": " else "")
                + superClassAndInterfaces.joinToString { it.qualifiedName } + " {")

        cls.declaredProperties.forEach {
            println("    ${it.element.asType().csTypeName} ${it.name} {get; set;}")
        }
        println("}")

        val converterName = "${cls.qualifiedName}Converter"
        println("class ${converterName}: ICBORToFromConverter<${cls.simpleName}> {")
        println("    public static readonly ${converterName} Instance = new ${converterName}();")
        println("    public ${cls.simpleName} FromCBORObject(CBORObject obj) => new ${cls.simpleName} {")
        println(cls.properties.map { "${it.name} = ${it.element.asType().cborConverter}.FromCBORObject(obj[${it.serialName}])" }
                .joinToString(",\n        ", prefix = "        "))
        println("    }")


        println("    public CBORObject ToCBORObject(${cls.simpleName} model) {")
        println("        return CBORObject.NewMap();")
        println(cls.properties.map { ".Add(\"${it.serialName}\", ${it.element.asType().cborConverter}.ToCBORObject(name))" }
                .joinToString("\n            ", prefix = "            ", postfix = ";"))
        println("    }")

        println("}")

    }

    val SerialClass.classOrInterface
        get() = if (isClass) "class" else "interface"

    val SerialClass.simpleName
        get() = qualifiedName.split(".").last()

    val SerialClass.packageName
        get() = qualifiedName.split(".").dropLast(1).joinToString(".")


    val TypeMirror.cborConverter: String
        get() = when (this.kind) {
            TypeKind.BOOLEAN,
            TypeKind.BYTE,
            TypeKind.SHORT,
            TypeKind.INT,
            TypeKind.LONG,
            TypeKind.CHAR,
            TypeKind.FLOAT,
            TypeKind.DOUBLE -> "PrimitiveConverter.Instance"
            TypeKind.ARRAY -> (this as ArrayType).cborConverter
            TypeKind.DECLARED -> (this as DeclaredType).cborConverter
            else -> {
                "unknown " + this.toString()
            }
        }

    val ArrayType.cborConverter: String
        get() = this.componentType.csTypeName + "[]"

    val DeclaredType.cborConverter: String
        get() {
            val element = this.asElement() as TypeElement
            return when (element.qualifiedName.toString()) {
                java.lang.String::class.java.name -> "PrimitiveConverter.Instance"
                java.util.List::class.java.name -> typeArguments[0].let { "new ListConverter<${it.csTypeName}>(${it.cborConverter})" }
                java.util.Map::class.java.name -> "Map<" + this.typeArguments.joinToString { it.csTypeName } + ">"
                java.util.Date::class.java.name -> "DateTimeConverter.Instance"
                else -> "${csTypeName}Converter.Instance"
            }
        }

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
            TypeKind.ARRAY -> (this as ArrayType).csTypeName
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
                java.util.Date::class.java.name -> "DateTime"
                else -> this.toString()
            }
        }

}