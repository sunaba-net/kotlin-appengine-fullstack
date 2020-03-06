plugins {
    id("org.jetbrains.kotlin.js") version "1.3.70"
    kotlin("plugin.serialization") version "1.3.70"
}

group = "net.sunaba"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

val ktorVersion = "1.3.0"

dependencies {
    implementation(kotlin("stdlib-js"))
    implementation("io.ktor:ktor-client-js:$ktorVersion")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-runtime-js:0.14.0")
}

kotlin {
    target {
        browser {
            useCommonJs()
        }
    }
    sourceSets["main"].dependencies {
        implementation(npm("vue"))

        implementation(npm("text-encoding"))
        implementation(npm("abort-controller"))
        implementation(npm("utf-8-validate"))
    }
}