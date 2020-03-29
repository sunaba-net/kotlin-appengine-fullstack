group = "net.sunaba"
version = "1.0-SNAPSHOT"

plugins {
    val kotlinVersion = "1.3.71"

    kotlin("jvm") version kotlinVersion apply false
    id("com.google.cloud.tools.appengine") version "2.2.0" apply false

    kotlin("js") version kotlinVersion apply false
    kotlin("plugin.serialization") version kotlinVersion apply false

    kotlin("multiplatform") version kotlinVersion apply false

    kotlin("kapt") version kotlinVersion apply false
}

subprojects {
    fun kotlin(module: String) = "org.jetbrains.kotlin.$module"
    when (name) {
        "backend" -> apply {
            plugin(kotlin("jvm"))
            plugin("com.google.cloud.tools.appengine")
            plugin("org.gradle.application")
            plugin(kotlin("plugin.serialization"))
        }
        "react-hands-on",
        "frontend" -> apply {
            plugin(kotlin("js"))
            plugin(kotlin("plugin.serialization"))
        }
        "model"->apply {
            plugin(kotlin("multiplatform"))
            plugin(kotlin("plugin.serialization"))
        }
        "serial-module-generator" -> apply {
            plugin(kotlin("jvm"))
        }
        "cs-model-generator" -> apply {
            plugin(kotlin("jvm"))
        }
    }
}

allprojects {
    version = "1.0.0-SNAPSHOT"
    group = "net.sunaba"
    group = "org.example"
    version = "1.0-SNAPSHOT"

    repositories {
        jcenter()
        mavenCentral()
    }
}