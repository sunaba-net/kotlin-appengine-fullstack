group = "org.example"
version = "1.0-SNAPSHOT"



repositories {
    mavenCentral()
}

val ktorVersion = "1.3.1"

dependencies {
    implementation(kotlin("stdlib-jdk8"))

    fun ktor(module: String, ver: String = ktorVersion) = "io.ktor:ktor-$module:$ver"
    implementation(ktor("server-core"))
    implementation(ktor("server-netty"))
    implementation(ktor("client-cio"))
    implementation(ktor("serialization"))
    implementation(ktor("websockets"))

    implementation("com.google.cloud:google-cloud-tasks:1.28.2")
    implementation("com.google.cloud:google-cloud-storage:1.105.0") {
        exclude("androidx.annotation")
    }
    implementation("com.google.apis:google-api-services-appengine:v1-rev20200215-1.30.9")

    implementation(project(":model"))

    implementation("org.jetbrains.kotlinx:kotlinx-serialization-runtime:0.20.0")
}

tasks {
    compileKotlin {
        kotlinOptions.jvmTarget = "1.8"
    }
    compileTestKotlin {
        kotlinOptions.jvmTarget = "1.8"
    }
}

appengine {
    deploy {
        projectId = "ktor-sunaba"
        version = "GCLOUD_CONFIG"
    }
}

application {
    mainClassName = "io.ktor.server.netty.EngineMain"
}

tasks.appengineStage {
    dependsOn(":frontend:browserDistribution")
    doLast {
        copy {
            from(project.configurations.runtimeClasspath)
            into(File(stagingExtension.stagingDirectory, "libs"))
        }
        copy {
            from(File(rootDir, "frontend/build/distributions"))
            into(File(stagingExtension.stagingDirectory, "web"))
        }
        File(stagingExtension.stagingDirectory, "app.yaml").let {f->
            f.writeText(f.readText().replace("{{mainClassName}}", application.mainClassName))
        }
        File(stagingExtension.stagingDirectory, "DockerFile").let {f->
            if (f.exists()) {
                f.writeText(f.readText().replace("{{mainClassName}}", application.mainClassName))
            }
        }
    }
}

val appengineDir = File(projectDir, "src/main/appengine")
tasks.create("switchStandard") {
    group = "deploy"
    doLast {
        copy {
            from(appengineDir) {
                include("app_standard.yaml")
                rename { "app.yaml" }
            }
            into(appengineDir)
        }
    }
}
tasks.create("switchFlex") {
    group = "deploy"
    doLast {
        copy {
            from(appengineDir) {
                include("app_flex.yaml")
                rename { "app.yaml" }
            }
            into(appengineDir)
        }
    }
}

tasks.create("appengineDeployStandard") {
    group = "deploy"
    dependsOn("switchStandard", "appengineDeploy")
    tasks["appengineStage"].mustRunAfter("switchStandard")
}

tasks.create("appengineDeployFlex") {
    group = "deploy"
    dependsOn("switchFlex", "appengineDeploy")
    tasks["appengineStage"].mustRunAfter("switchFlex")
}