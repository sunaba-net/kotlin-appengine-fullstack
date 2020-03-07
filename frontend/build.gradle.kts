
group = "net.sunaba"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

val ktorVersion = "1.3.0"

dependencies {
    implementation(kotlin("stdlib-js"))
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
    }
}