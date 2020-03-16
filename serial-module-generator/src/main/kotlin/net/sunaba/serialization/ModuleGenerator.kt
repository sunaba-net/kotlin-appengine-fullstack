package net.sunaba.serialization

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import javax.annotation.processing.*
import javax.lang.model.element.*
import javax.lang.model.type.*

@SupportedOptions("module.gen.output")
@com.google.auto.service.AutoService(Processor::class)
class ModuleGenerator : AbstractProcessor() {

    val moduleMap = hashMapOf<String, HashSet<String>>()

    override fun getCompletions(element: Element?, annotation: AnnotationMirror?, member: ExecutableElement?, userText: String?): MutableIterable<Completion> {
        return super.getCompletions(element, annotation, member, userText)
    }

    override fun process(annotations: MutableSet<out TypeElement>, roundEnv: RoundEnvironment): Boolean {
        if (roundEnv.processingOver()) {
            println("===========processingOver")

            println("val module = SerializersModule {")
            moduleMap.forEach {
                println("    polymorphic(${it.key}::class) {")
                println(it.value.map { "addSubclass(${it}::class, ${it}.serializer())" }
                        .joinToString("\n        ", prefix = "        "))
                println("    }")
            }
            println("}")
            println(moduleMap)
        } else {
            println("===========processingNotOver")
        }

        roundEnv.getElementsAnnotatedWith(Serializable::class.java).forEach {
            process(it as TypeElement)
        }
        println(processingEnv.options.entries)

        return false
    }

    val processed = hashSetOf<TypeElement>()

    fun process(typeElement: TypeElement) {

        val isKindClass = (typeElement.kind == ElementKind.CLASS)
        val isKindInterface = (typeElement.kind == ElementKind.INTERFACE)

        if (isKindClass && typeElement.superclass is NoType) {
            //java.lang.Object
            return;
        }
        if (processed.contains(typeElement)) {
            return
        }
        processed.add(typeElement)

        if (typeElement.getAnnotation(Serializable::class.java) != null) {
            typeElement.baseElements.forEach {
                moduleMap.getOrPut(it.toString() ,{ hashSetOf()}).add(typeElement.toString())
            }
        }



        typeElement.interfaces.forEach { process(processingEnv.typeUtils.asElement(it) as TypeElement) }

        val superClassProperties = if (isKindClass) {
            process(processingEnv.typeUtils.asElement(typeElement.superclass) as TypeElement)
            processingEnv.typeUtils.asElement(typeElement.superclass).enclosedElements
        } else {
            typeElement.interfaces.flatMap { processingEnv.typeUtils.asElement(it).enclosedElements }
        }.filter { it is VariableElement }
                .map { it.simpleName }

        val serialNames = typeElement.enclosedElements.filter { it.getAnnotation(SerialName::class.java) != null }
        //println(serialName.map { it.asType() })


        println(typeElement.qualifiedName.toString() + ":" + typeElement.interfaces)
        println(typeElement.baseElements)
        typeElement.enclosedElements.stream()
                .filter { it is VariableElement }
                .filter { !superClassProperties.contains(it.simpleName) }
                .filter { !"Companion".equals(it.simpleName.toString()) }
                .map { it as VariableElement }
                .forEach {
                    val serialName = it.getAnnotation(SerialName::class.java)
                    if (serialName != null) {
                        println(serialName)
                    }
                    println("    ${it.asType()} ${it.simpleName} = ${it.constantValue} (${it.asType().csType})")
                }

        println()
//            println(typeElement.interfaces)
//
//            if (typeElement.superclass is NoType) {
//                println("noType")
//            }
//            println(typeElement.superclass)
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
            TypeKind.INT -> "int"
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
                else -> this.toString()
            }
        }

    val baseElementCache = hashMapOf<TypeElement, Set<TypeElement>>()

    /**
     *
     */
    val TypeElement.baseElements: Set<TypeElement>
        get() = baseElementCache.getOrPut(this) {
            val hashSet = hashSetOf<TypeElement>()
            if (superclass.kind != TypeKind.NONE && !superclass.isJavaLangObject) {
                val s = processingEnv.typeUtils.asElement(superclass) as TypeElement
                hashSet.add(s)
                hashSet.addAll(s.baseElements)
            }
            interfaces.forEach {
                if (!(it is  NoType)) {
                    val i = processingEnv.typeUtils.asElement(it) as TypeElement
                    hashSet.add(i)
                    hashSet.addAll(i.baseElements)
                }

            }
            return hashSet
        }


    val TypeMirror.isJavaLangObject
        get() = this is DeclaredType && (this.asElement() as TypeElement).isJavaLangObject

    val TypeElement.isJavaLangObject
        get() = kind == ElementKind.CLASS && superclass is NoType
}