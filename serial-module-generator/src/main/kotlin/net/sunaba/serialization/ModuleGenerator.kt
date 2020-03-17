package net.sunaba.serialization

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.io.File
import java.io.PrintWriter
import javax.annotation.processing.*
import javax.lang.model.SourceVersion
import javax.lang.model.element.*
import javax.lang.model.type.*
import javax.tools.Diagnostic
import kotlin.collections.HashMap
import kotlin.collections.HashSet

@SupportedSourceVersion(SourceVersion.RELEASE_8)
@SupportedOptions("module.gen.output")
@com.google.auto.service.AutoService(Processor::class)
class ModuleGenerator : AbstractProcessor() {

    private val polyMap = hashMapOf<String, HashSet<String>>()

    private val serialClassImplRespository: HashMap<TypeElement, SerialClass> = hashMapOf()


    override fun getCompletions(element: Element?, annotation: AnnotationMirror?, member: ExecutableElement?, userText: String?): MutableIterable<Completion> {
        return super.getCompletions(element, annotation, member, userText)
    }

    override fun process(annotations: MutableSet<out TypeElement>, roundEnv: RoundEnvironment): Boolean {
        val kaptKotlinGeneratedDir = processingEnv.options[KAPT_KOTLIN_GENERATED_OPTION_NAME] ?: run {
            processingEnv.messager.printMessage(Diagnostic.Kind.ERROR, "Can't find the target directory for generated Kotlin files.")
            return false
        }
        if (roundEnv.processingOver()) {
            generateAutoModule(kaptKotlinGeneratedDir)

            serialClassImplRespository.values.forEach {
                CsCborGenerator.generate(it)
            }

        } else {
            roundEnv.getElementsAnnotatedWith(Serializable::class.java).forEach {
                process(it as TypeElement)
            }
        }

        return false
    }

    fun generateAutoModule(outputDir: String) {
        val dir = processingEnv.options.get("serializers.output") ?: outputDir

        File(dir, "AutoModule.kt").writer(Charsets.UTF_8).use {
            PrintWriter(it).use { w ->
                w.println("package automodule")
                w.println("import kotlinx.serialization.modules.SerializersModule")
                w.println("val AutoModule = SerializersModule{")
                polyMap.forEach {
                    w.println("    polymorphic(${it.key}::class) {")
                    w.println(it.value.map { "addSubclass(${it}::class, ${it}.serializer())" }
                            .joinToString("\n        ", prefix = "        "))
                    w.println("    }")
                }
                w.println("}")
            }
        }
    }


    inner class SerialPropertyImpl(override val element: VariableElement, override val serialName: String) : SerialProperty {
        override val name: String = element.simpleName!!.toString()
    }


    inner class SerialClassImpl(override val element: TypeElement) : SerialClass {
        override val isClass: Boolean = element.kind == ElementKind.CLASS
        override val isInterface: Boolean = element.kind == ElementKind.INTERFACE

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

        override val qualifiedName: String = element.qualifiedName!!.toString()
        override val serialName: String by lazy {
            element.getAnnotation(SerialName::class.java)?.value ?: qualifiedName
        }

        override val hasSerializer: Boolean by lazy {
            element.getAnnotation(Serializable::class.java) != null
        }

        override val properties: List<SerialProperty> by lazy {
            element.enclosedElements
                    .filter { it is VariableElement }
                    .filter { !"Companion".equals(it.simpleName.toString()) }
                    .map {
                        SerialPropertyImpl(it as VariableElement, serialPropertyNameMap
                                .getOrDefault(it.simpleName.toString(), it.simpleName.toString()))
                    }
        }

        override val declaredProperties: List<SerialProperty> by lazy {
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

        override val superclass: SerialClass? by lazy {
            if (isInterface) {
                null
            } else {
                element.superclass.let {
                    if (it.isJavaLangObject) null else process(processingEnv.typeUtils.asElement(it) as TypeElement)
                }
            }
        }

        override val interfaces: List<SerialClass> by lazy {
            element.interfaces.map { process(processingEnv.typeUtils.asElement(it) as TypeElement) }
        }

        override val allSuperClassInterfaces: Set<SerialClass> by lazy {
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

    fun process(typeElement: TypeElement): SerialClass = serialClassImplRespository.getOrPut(typeElement) {
        SerialClassImpl(typeElement).also {
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

    val TypeMirror.isJavaLangObject
        get() = this is DeclaredType && (this.asElement() as TypeElement).isJavaLangObject

    val TypeElement.isJavaLangObject
        get() = kind == ElementKind.CLASS && superclass is NoType

    companion object {
        const val KAPT_KOTLIN_GENERATED_OPTION_NAME = "kapt.kotlin.generated"
    }

}
