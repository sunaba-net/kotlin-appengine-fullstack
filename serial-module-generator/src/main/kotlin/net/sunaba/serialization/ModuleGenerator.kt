package net.sunaba.serialization

import com.squareup.kotlinpoet.MemberName
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import javax.annotation.processing.*
import javax.lang.model.element.*
import javax.lang.model.type.*

@SupportedOptions("module.gen.output")
@com.google.auto.service.AutoService(Processor::class)
class ModuleGenerator : AbstractProcessor() {

    val moduleMap = hashMapOf<String, List<String>>()

    override fun getCompletions(element: Element?, annotation: AnnotationMirror?, member: ExecutableElement?, userText: String?): MutableIterable<Completion> {
        return super.getCompletions(element, annotation, member, userText)
    }

    override fun process(annotations: MutableSet<out TypeElement>, roundEnv: RoundEnvironment): Boolean {
        if (roundEnv.processingOver()) {
            println("===========processingOver")
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
        assert(typeElement.kind.let { it==ElementKind.CLASS ||it==ElementKind.INTERFACE })

        if (typeElement.kind == ElementKind.CLASS && typeElement.superclass is NoType) {
            //java.lang.Object
            return;
        }
        if (processed.contains(typeElement)) {
            return
        }
        processed.add(typeElement)

        typeElement.interfaces.forEach { process(processingEnv.typeUtils.asElement(it) as TypeElement) }

        process(processingEnv.typeUtils.asElement(typeElement.superclass) as TypeElement)
        val superClassProperties = processingEnv.typeUtils.asElement(typeElement.superclass).enclosedElements
                .filter { it is VariableElement }
                .map { it.simpleName }

        val serialNames = typeElement.enclosedElements.filter { it.getAnnotation(SerialName::class.java) != null }
        //println(serialName.map { it.asType() })


        println(typeElement.qualifiedName.toString() + ":" + typeElement.interfaces)
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


}