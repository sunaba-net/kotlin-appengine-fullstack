repositories {
    maven("https://dl.bintray.com/korlibs/korlibs/")
    maven("https://kotlin.bintray.com/kotlin-js-wrappers/")
}

val ktorVersion = "1.3.2"

dependencies {
    implementation(kotlin("stdlib-js"))
    testImplementation(kotlin("test-js"))

    implementation("io.ktor:ktor-client-js:$ktorVersion")

    implementation("org.jetbrains.kotlinx:kotlinx-serialization-runtime-js:0.20.0")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-cbor-js:0.20.0")
    implementation("com.soywiz.korlibs.korio:korio:1.10.0")

    implementation("org.jetbrains:kotlin-css-js:1.0.0-pre.94-kotlin-1.3.70")
    implementation(project(":model"))

//    api(npm("text-encoding"))
//    api(npm("bufferutil"))
//    api(npm("utf-8-validate"))
//    api(npm("abort-controller"))
//    api(npm("fs"))
    implementation(npm("text-encoding", "0.7.0"))
    implementation(npm("abort-controller", "3.0.0"))
}

kotlin {
    target {
        browser {
            dceTask {
                // 既知の問題：DCEによって必要なコードが消えてしまうのを避ける
                // 将来的には不要になるかも
                //see https://kotlinlang.org/docs/reference/javascript-dce.html#known-issue-dce-and-ktor
                keep("ktor-ktor-io.\$\$importsForInline\$\$.ktor-ktor-io.io.ktor.utils.io")
            }
            useCommonJs()
            runTask {
            }
            testTask {
                useKarma {
                    useChromeHeadless()
                }
            }
        }
    }
    sourceSets["main"].dependencies {
        implementation(npm("vue"))
        implementation(npm("firebase"))
        implementation(npm("firebaseui"))
    }
}
