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
    implementation(ktor("auth"))
    implementation(ktor("auth-jwt"))
    implementation(ktor("websockets"))
    implementation(ktor("serialization"))
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-cbor:0.20.0")

    implementation(project(":model"))

    testImplementation(kotlin("test"))
    testImplementation(kotlin("test-junit"))

    implementation("com.google.apis:google-api-services-cloudresourcemanager:v1-rev572-1.25.0")
    implementation("com.google.cloud:google-cloud-secretmanager:1.0.0")
    implementation("com.google.cloud:google-cloud-tasks:1.29.1")
}

tasks {
    compileKotlin {
        kotlinOptions.jvmTarget = "1.8"
    }
    compileTestKotlin {
        kotlinOptions.jvmTarget = "1.8"
    }
    test {
        useJUnit {
        }
    }
    appengineStage {
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
    val switchStandard = create("switchStandard") {
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

    val switchFlex = create("switchFlex") {
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
    create("appengineDeployStandard") {
        group = "deploy"
        dependsOn(switchStandard, appengineDeploy)
        appengineStage.get().mustRunAfter(switchStandard)
    }

    create("appengineDeployFlex") {
        group = "deploy"
        dependsOn(switchFlex, appengineDeploy)
        appengineStage.get().mustRunAfter(switchFlex)
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
