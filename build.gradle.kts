group = "net.sunaba"
version = "1.0-SNAPSHOT"

plugins {
    val kotlinVersion = "1.3.70"

    kotlin("jvm") version kotlinVersion apply false
    id("com.google.cloud.tools.appengine") version "2.2.0" apply false
    application

    kotlin("js") version kotlinVersion apply false
    kotlin("plugin.serialization") version kotlinVersion apply false
}

subprojects {
    fun kotlin(module: String) = "org.jetbrains.kotlin.$module"
    when (name) {
        "backend" -> apply {
            plugin(kotlin("jvm"))
            plugin("com.google.cloud.tools.appengine")
            plugin("org.gradle.application")
        }
        "frontend" -> apply {
            plugin(kotlin("js"))
            plugin(kotlin("plugin.serialization"))
        }
    }


}
