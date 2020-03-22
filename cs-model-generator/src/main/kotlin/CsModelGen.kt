package modelgen

import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerialDescriptor
import kotlinx.serialization.SerialName
import kotlinx.serialization.modules.EmptyModule
import kotlinx.serialization.modules.SerialModule
import model.Message
import model.module
import java.io.File
import java.util.*
import kotlin.reflect.KClass
import kotlin.reflect.KType
import kotlin.reflect.full.findAnnotation
import kotlin.reflect.full.memberProperties
import kotlin.reflect.full.superclasses


class CsModelGen(val context: SerialModule = EmptyModule, var dstDir: File = File(".")) {

    internal val kotlin.reflect.KProperty<*>.csField: String
        get() = "${returnType.csTypeName} ${name} {get; set;}"

    fun generatePolymorphic(clazz: KClass<*>, defaultImpl: KClass<*>? = null) {
        val polyMap = context.javaClass.declaredFields.find { it.name == "polyBase2Serializers" }!!.let {
            it.isAccessible = true
            it.get(context)
        } as Map<KClass<*>, Map<KClass<*>, KSerializer<*>>>

        val serializerMap = polyMap[clazz]

        //generate interfaces
        serializerMap!!.forEach { subClass, s ->
            this.generate(subClass, s)
        }

        val outFile = File(dstDir, clazz.qualifiedName!!.replace(".", File.separator) + ".cs")
        outFile.parentFile.mkdirs()
        outFile.printWriter(Charsets.UTF_8).use {
            val modelName = clazz.simpleName
            val alreadyDeclaredProperties = clazz.superclasses.flatMap { it.memberProperties.map { it.name } }

            it.println("""
                    using System;
                    using System.Collections.Generic;
                    using PeterO.Cbor;
                    using Piisu.CBOR;
                """.trimIndent())
            it.println("namespace models.simple {")
            val inheritClasses = clazz.supertypes.filter { it.classifier != Any::class }.map { (it.classifier as KClass<*>).qualifiedName }
                    .let {
                        if (it.isEmpty()) "" else it.joinToString(", ", prefix = ": ")
                    }
            it.println("interface ${modelName}${inheritClasses} {")
            it.println(clazz.memberProperties
                    .filter { !alreadyDeclaredProperties.contains(it.name) }
                    .map { "${it.csField}" }.joinToString("\n    ", prefix = "    "))
            it.println()
            it.println("}")


            val converterName = "${modelName}Converter"
            it.println()
            it.println("class ${converterName}: ICBORToFromConverter<${modelName}> {")
            it.println("    public static readonly ${converterName} Instance = new ${converterName}();")
            it.println("    public ${modelName} FromCBORObject(CBORObject obj) {")
            it.println("        switch(obj[0].AsString()) {")
            serializerMap.forEach { subClass, serializer ->
                if (defaultImpl == subClass) {
                    it.println("        default:")
                } else {
                    it.println("        case \"${serializer.descriptor.serialName}\":")
                }
                it.println("            return ${subClass.qualifiedName}Converter.Instance.FromCBORObject(obj[1]);")
            }
            it.println("        }")
            it.println("        return null;")
            it.println("    }")

            it.println("    public CBORObject ToCBORObject(${modelName} model) {")
            it.println("        switch(model) {")
            serializerMap.forEach { subClass, serializer ->
                it.println("        case ${subClass.qualifiedName} v:")
                it.println("            return CBORObject.NewArray().Add(\"${serializer.descriptor.serialName}\")")
                it.println("                .Add(${subClass.qualifiedName}Converter.Instance.ToCBORObject(v));")
            }
            it.println("        }")
            it.println("        return null;")
            it.println("    }")
            it.println("}")
            it.println("}")
        }
    }


    inline fun <reified T : Any> generate(serializer: KSerializer<T>) {
        generate(T::class, serializer)
    }


    fun generate(clazz: KClass<*>, serializer: KSerializer<*>) {

        //SerialNameから実際のプロパティ名を逆引きする
        val propertyMap = clazz.memberProperties.map {
            (it.findAnnotation<SerialName>()?.value ?: it.name) to it
        }.toMap()

        val desc = serializer.descriptor

        val properties = (0 until desc.elementsCount).map {
            val serialName = desc.getElementName(it)
            val prop = propertyMap[serialName]!!
            PropertyInfo(clazz, prop.name, serialName, prop.returnType)
        }

        val outFile = File(dstDir, clazz.qualifiedName!!.replace(".", File.separator) + ".cs")
        outFile.parentFile.mkdirs()
        //親クラスに実装済みのプロパティ一覧
        val alreadyDeclaredProperties = clazz.superclasses
                .filter { !it.isAbstract }
                .flatMap { it.memberProperties.map { it.name } }

        val modelName = clazz.simpleName
        outFile.printWriter(Charsets.UTF_8).use {
            it.println("""
                using System;
                using System.Collections.Generic;
                using PeterO.Cbor;
                using Piisu.CBOR;
            """.trimIndent())
            it.println("namespace models.simple {")
            val inheritClasses = clazz.supertypes.filter { it.classifier != Any::class }.map { (it.classifier as KClass<*>).qualifiedName }
                    .let {
                        if (it.isEmpty()) "" else it.joinToString(", ", prefix = ": ")
                    }
            it.println("class ${modelName}${inheritClasses} {")
            it.println(properties.filter { !alreadyDeclaredProperties.contains(it.name) }.map { "${it.csField}" }.joinToString("\n    ", prefix = "    "))
            it.println()
            it.println("    public override string ToString() {")
            it.println("        return $\"" + properties.map { "${it.name}:{${it.name}}" }.joinToString() + "\";")
            it.println("    }")
            it.println("}")


            val converterName = "${modelName}Converter"
            it.println()
            it.println("class ${converterName}: ICBORToFromConverter<${modelName}> {")
            it.println("    public static readonly ${converterName} Instance = new ${converterName}();")

            it.println("    public ${modelName} FromCBORObject(CBORObject obj) => new ${modelName} {")
            it.println(properties.map { it.readOperation }
                    .joinToString(",\n        ", prefix = "        "))
            it.println("    };")

            it.println("    public CBORObject ToCBORObject(${modelName} model) {")
            it.println("        CBORObject obj = CBORObject.NewMap();")

            it.println(properties.map { it.writeOperation }
                    .joinToString("\n        ", prefix = "        "))
            it.println("        return obj;")
            it.println("    }")
            it.println("}")
            it.println("}")
        }
    }

    class PropertyInfo<T : Any>(val declaredClass: KClass<T>
                                , val name: String
                                , val serialName: String
                                , val type: KType) {

        val writeOperation: String
            get() = "obj.Add(\"${serialName}\", ${type.cborConverter}.ToCBORObject(model.$name));"
        val readOperation: String
            get() = "${name} = ${type.cborConverter}.FromCBORObject(obj[\"${serialName}\"])"//type.genReadOperation(name, serialName)
        val csField: String
            get() = "public ${type.csTypeName} ${name} {set; get;} "
    }

    class EntityInfo<T : Any>(
            val clazz: KClass<T>, val serializer: KSerializer<T>
            , val properties: List<PropertyInfo<T>>
            , val descriptor: SerialDescriptor = serializer.descriptor) {

        val simpleName: String
            get() = clazz.simpleName!!

        val serialName: String
            get() = descriptor.serialName

        val csTypeName: String
            get() = clazz.qualifiedName!!
    }

}


val KType.csTypeName: String
    get() = when (classifier) {
        //Primitive
        Boolean::class -> "bool"
        Byte::class -> "byte"
        Char::class -> "char"
        Short::class -> "short"
        Int::class -> "int"
        Long::class -> "long"
        Float::class -> "float"
        Double::class -> "double"
        String::class -> "string"

        //PrimitiveArray
        BooleanArray::class -> "bool[]"
        ByteArray::class -> "byte[]"
        CharArray::class -> "char[]"
        ShortArray::class -> "short[]"
        IntArray::class -> "int[]"
        LongArray::class -> "long[]"
        FloatArray::class -> "float[]"
        DoubleArray::class -> "double[]"

        //Structure
        List::class -> "List<${arguments[0].type!!.csTypeName}>"
        Map::class -> "Dictionary<${arguments[0].type!!.csTypeName}, ${arguments[1].type!!.csTypeName}>"

        Date::class -> "DateTime"
        //Class
        is KClass<*> -> (classifier as KClass<*>).qualifiedName!!

        //Class
        else -> TODO("Unknown type ${this}")
    }

val KType.cborConverter: String
    get() = when (classifier) {
        //Primitive
        Boolean::class,
        Byte::class,
        Char::class,
        Short::class,
        Int::class,
        Long::class,
        Float::class,
        Double::class,
        String::class,

            //PrimitiveArray
        BooleanArray::class,
        ByteArray::class,
        CharArray::class,
        ShortArray::class,
        IntArray::class,
        LongArray::class,
        FloatArray::class,
        DoubleArray::class -> "PrimitiveConverter<${csTypeName}>.Instance"

        //Structure
        List::class -> arguments[0].type!!.let { "new ListConverter<${it.csTypeName}>(${it.cborConverter})" }
        Map::class -> "Dictionary<${arguments[0].type!!.csTypeName}, ${arguments[1].type!!.csTypeName}>"

        Date::class -> "DateTimeConverter.Instance"
        //Class
        is KClass<*> -> (classifier as KClass<*>).qualifiedName!! + "Converter.Instance"

        //Class
        else -> TODO("Unknown type ${this}")
    }

@OptIn(ExperimentalStdlibApi::class)
@ExperimentalStdlibApi
fun main(args: Array<String>) {
    val modelGen = CsModelGen(module, File(args[0]!!))

    modelGen.generatePolymorphic(Message::class)
}