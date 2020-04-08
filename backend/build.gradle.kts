plugins {
    application
}

val ktorVersion = "1.3.2"

dependencies {
    implementation(kotlin("stdlib-jdk8"))

    fun ktor(module: String, ver: String = ktorVersion) = "io.ktor:ktor-$module:$ver"
    implementation(ktor("server-core"))
    implementation(ktor("server-netty"))
    implementation(ktor("client-cio"))
    implementation(ktor("serialization"))
    implementation(ktor("auth"))
    implementation(ktor("auth-jwt"))
    implementation(ktor("websockets"))

    implementation("com.google.cloud:google-cloud-tasks:1.29.0")
    implementation("com.google.cloud:google-cloud-storage:1.105.0") {
        exclude("androidx.annotation")
    }
    implementation(project(":model"))
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-cbor:0.20.0")


    implementation("com.google.apis:google-api-services-appengine:v1-rev20200215-1.30.9")
    implementation("com.google.apis:google-api-services-cloudresourcemanager:v1-rev572-1.25.0")

    implementation("com.google.cloud:google-cloud-secretmanager:1.0.0")
//
//    implementation("com.google.apis:google-api-services-iam:v1-rev20200221-1.30.9")
//    implementation("org.shredzone.acme4j:acme4j-client:2.9")
//    implementation("org.shredzone.acme4j:acme4j-utils:2.9")

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
    dependsOn(":react-hands-on:browserDistribution")
    doLast {
        copy {
            from(project.configurations.runtimeClasspath)
            into(File(stagingExtension.stagingDirectory, "libs"))
        }
        copy {
            from(File(rootDir, "frontend/build/distributions"))
            into(File(stagingExtension.stagingDirectory, "web"))
        }
        copy {
            from(File(rootDir, "react-hands-on/build/distributions"))
            into(File(stagingExtension.stagingDirectory, "web/react"))
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