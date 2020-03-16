import org.jetbrains.kotlin.kapt3.base.Kapt

plugins {
    kotlin("kapt")
}


kotlin {
    jvm()
    js {
        browser
    }
    /* Targets configuration omitted.
    *  To find out how to configure the targets, please follow the link:
    *  https://kotlinlang.org/docs/reference/building-mpp-with-gradle.html#setting-up-targets */

    sourceSets {

        val serialization_version = "0.20.0"
        val commonMain by getting {
            dependencies {
                implementation(kotlin("stdlib-common"))
                implementation("org.jetbrains.kotlinx:kotlinx-serialization-runtime-common:$serialization_version")

                // https://stackoverflow.com/questions/59321848/using-kapt-with-multiplatform-subproject
                configurations.get("kapt").dependencies.add(project(":serial-module-generator"))
            }
        }
        //kaptで追加されたソースをソースパスに追加
//        this["commonMain"]
//                .kotlin.srcDir("$projectDir/build/generated/source/kapt/main")

        val commonTest by getting {
            dependencies {
                implementation(kotlin("test-common"))
                implementation(kotlin("test-annotations-common"))
            }
        }

        val jvmMain by getting {
            dependencies {
                implementation(kotlin("stdlib-jdk8"))
                implementation("org.jetbrains.kotlinx:kotlinx-serialization-runtime:$serialization_version")
            }
        }

        val jsMain by getting {
            dependencies {
                implementation(kotlin("stdlib-js"))
                implementation("org.jetbrains.kotlinx:kotlinx-serialization-runtime-js:$serialization_version")
            }
        }
    }
}

kapt {
    arguments {
        arg("serializers.output", "$projectDir/src/commonMain")
    }
}