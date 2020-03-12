import org.jetbrains.kotlin.gradle.targets.js.webpack.KotlinWebpackConfig

group = "net.sunaba"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
    mavenLocal()
    maven("https://dl.bintray.com/korlibs/korlibs/")
}

val ktorVersion = "1.3.1"

dependencies {
    implementation(kotlin("stdlib-js"))
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-runtime-js:0.20.0")
    implementation("com.soywiz.korlibs.korio:korio:1.10.0")
    implementation(project(":model"))
}

kotlin {
    target {
        browser {
            useCommonJs()
            runTask {
            }
        }
    }
    sourceSets["main"].dependencies {
        implementation(npm("vue"))
    }
}
