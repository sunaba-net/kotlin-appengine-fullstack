group = "net.sunaba"
version = "1.0-SNAPSHOT"

plugins {
    kotlin("kapt")
}

dependencies {
    implementation(kotlin("stdlib-jdk8"))

    implementation("com.google.auto.service:auto-service-annotations:1.0-rc6")
    kapt("com.google.auto.service:auto-service:1.0-rc6")

    implementation("org.jetbrains.kotlinx:kotlinx-serialization-runtime:0.20.0")

    implementation("com.squareup:kotlinpoet:1.5.0")


    //for testing
    testImplementation(kotlin("reflect"))
    testImplementation(kotlin("test"))
    testImplementation(kotlin("test-junit"))

//    implementation(files(org.gradle.internal.jvm.Jvm.current().getToolsJar()))
}

//println(org.gradle.internal.jvm.Jvm.current().getToolsJar())


tasks {
    compileKotlin {
        kotlinOptions.jvmTarget = "1.8"
    }
    compileTestKotlin {
        kotlinOptions.jvmTarget = "1.8"
    }
}