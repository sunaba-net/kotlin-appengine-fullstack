package net.sunaba.serialization

import com.squareup.kotlinpoet.FileSpec
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.io.File
import java.util.*
import javax.annotation.processing.*
import javax.lang.model.element.*
import javax.lang.model.type.*
import javax.tools.StandardLocation
import kotlin.collections.HashMap
import kotlin.collections.HashSet
import kotlin.math.min


@SupportedOptions("module.gen.output")
@com.google.auto.service.AutoService(Processor::class)
class ModuleGenerator : AbstractProcessor() {

    val polyMap = hashMapOf<String, HashSet<String>>()

    override fun getCompletions(element: Element?, annotation: AnnotationMirror?, member: ExecutableElement?, userText: String?): MutableIterable<Completion> {
        return super.getCompletions(element, annotation, member, userText)
    }

    override fun process(annotations: MutableSet<out TypeElement>, roundEnv: RoundEnvironment): Boolean {
        if (roundEnv.processingOver()) {

            polyMap.forEach {
                println("    polymorphic(${it.key}::class) {")
                println(it.value.map { "addSubclass(${it}::class, ${it}.serializer())" }
                        .joinToString("\n        ", prefix = "        "))
                println("    }")
            }

            println(processingEnv.options)

            processingEnv.options.get("serializers.output")?.let { File(it, "Dummy.kt").writer(Charsets.UTF_8) }
                    ?: processingEnv.filer.createResource(StandardLocation.SOURCE_OUTPUT, "dummy", "Dummy.kt").openWriter().use {
                        it.write("Hello")
                    }

        } else {
        }

        roundEnv.getElementsAnnotatedWith(Serializable::class.java).forEach {
            process(it as TypeElement)
        }
        return false
    }

    private val serialClassRespository: HashMap<TypeElement, SerialClass> = hashMapOf()

    inner class SerialProperty(element: VariableElement, val serialName: String) {
        val name: String = element.simpleName!!.toString()
    }

    inner class SerialClass(element: TypeElement) {
        private val isClass: Boolean = element.kind == ElementKind.CLASS
        private val isInterface: Boolean = element.kind == ElementKind.INTERFACE

        /**
         * SerialNameアノテーションが付いているプロパティのプロパティ名とシリアル名のmap
         */
        private val serialPropertyNameMap: Map<String, String> by lazy {
            val serialAnnotationSuffix = "\$annotations";
            element.enclosedElements
                    .map { it to it.getAnnotation(SerialName::class.java) }
                    .filter { it.first.simpleName.endsWith(serialAnnotationSuffix) && it.second != null }
                    .map { it.first.simpleName.substring(0, it.first.simpleName.length - serialAnnotationSuffix.length) to it.second.value }
                    .toMap()
        }

        val qualifiedName: String = element.qualifiedName!!.toString()
        val serialName: String by lazy {
            element.getAnnotation(SerialName::class.java)?.value ?: qualifiedName
        }

        val hasSerializer: Boolean by lazy {
            element.getAnnotation(Serializable::class.java) != null
        }

        val properties: List<SerialProperty> by lazy {
            element.enclosedElements
                    .filter { it is VariableElement }
                    .filter { !"Companion".equals(it.simpleName.toString()) }
                    .map {
                        SerialProperty(it as VariableElement, serialPropertyNameMap
                                .getOrDefault(it.simpleName.toString(), it.simpleName.toString()))
                    }
        }

        val declaredProperties: List<SerialProperty> by lazy {
            if (isInterface) {
                interfaces.flatMap { it.properties.map { it.name } }.let { names ->
                    properties.filter { !names.contains(it.name) }
                }
            } else {
                if (superclass == null) {
                    properties
                } else {
                    superclass!!.properties.map { it.name }.let { names ->
                        properties.filter { !names.contains(it.name) }
                    }
                }
            }
        }

        val superclass: SerialClass? by lazy {
            if (isInterface) {
                null
            } else {
                element.superclass.let {
                    if (it.isJavaLangObject) null else process(processingEnv.typeUtils.asElement(it) as TypeElement)
                }
            }
        }

        val interfaces: List<SerialClass> by lazy {
            element.interfaces.map { process(processingEnv.typeUtils.asElement(it) as TypeElement) }
        }

        val allSuperClassInterfaces: Set<SerialClass> by lazy {
            hashSetOf<SerialClass>().apply {
                superclass?.let {
                    add(it)
                    addAll(it.allSuperClassInterfaces)
                }
                addAll(interfaces)
                interfaces.forEach {
                    addAll(it.allSuperClassInterfaces)
                }
            }
        }
    }

    fun process(typeElement: TypeElement): SerialClass = serialClassRespository.getOrPut(typeElement) {
        return SerialClass(typeElement).also {
            if (it.hasSerializer) {
                it.allSuperClassInterfaces.forEach { base ->
                    polyMap.getOrPut(base.qualifiedName, { hashSetOf() }).add(it.qualifiedName)
                }
            }
        }
    }

    override fun getSupportedAnnotationTypes(): MutableSet<String> {
        return mutableSetOf(Serializable::class.java.name)
    }

    val TypeMirror.csType: String
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
            TypeKind.ARRAY -> (this as ArrayType).csType
            TypeKind.DECLARED -> (this as DeclaredType).csType
            else -> {
                "unknown " + this.toString()
            }
        }
    val ArrayType.csType: String
        get() = this.componentType.csType + "[]"

    val DeclaredType.csType: String
        get() {
            val element = this.asElement() as TypeElement
            return when (element.qualifiedName.toString()) {
                java.lang.String::class.java.name -> "string"
                java.util.List::class.java.name -> "List<" + this.typeArguments.joinToString { it.csType } + ">"
                java.util.Map::class.java.name -> "Map<" + this.typeArguments.joinToString { it.csType } + ">"
                else -> this.toString()
            }
        }

    val TypeMirror.isJavaLangObject
        get() = this is DeclaredType && (this.asElement() as TypeElement).isJavaLangObject

    val TypeElement.isJavaLangObject
        get() = kind == ElementKind.CLASS && superclass is NoType

}
