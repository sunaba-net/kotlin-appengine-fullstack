repositories {
    maven("https://dl.bintray.com/korlibs/korlibs/")
    maven("https://kotlin.bintray.com/kotlin-js-wrappers/")
}

val ktorVersion = "1.3.1"

dependencies {
    implementation(kotlin("stdlib-js"))
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-runtime-js:0.20.0")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-cbor-js:0.20.0")
    implementation("com.soywiz.korlibs.korio:korio:1.10.0")

    implementation("org.jetbrains:kotlin-css-js:1.0.0-pre.94-kotlin-1.3.70")
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
