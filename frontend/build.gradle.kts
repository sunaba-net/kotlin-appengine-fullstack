
group = "net.sunaba"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
    maven("https://dl.bintray.com/korlibs/korlibs/")
}

val ktorVersion = "1.3.1"

dependencies {
    implementation(kotlin("stdlib-js"))
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-runtime-js:0.14.0")
    implementation("com.soywiz.korlibs.korio:korio:1.10.0")
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