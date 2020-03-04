plugins {
    id("org.jetbrains.kotlin.js") version "1.3.70"
}

group = "net.sunaba"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    implementation(kotlin("stdlib-js"))
}

kotlin {
    target {
        browser {
            useCommonJs()
        }
    }
    sourceSets["main"].dependencies {
        implementation(npm("vue"))
    }
}