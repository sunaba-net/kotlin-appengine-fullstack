repositories {
    mavenCentral()
}

dependencies {
    implementation(kotlin("stdlib-jdk8"))
    implementation(kotlin("reflect"))
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-runtime:0.20.0")
    implementation(project(":model"))
}

tasks {
    compileKotlin {
        kotlinOptions.jvmTarget = "1.8"
    }
    compileTestKotlin {
        kotlinOptions.jvmTarget = "1.8"
    }

    create("generate", JavaExec::class) {
        dependsOn("build")
        classpath = sourceSets["test"].runtimeClasspath
        args = listOf("$projectDir/hoge")
        main = "modelgen.CsModelGenKt"
    }
}