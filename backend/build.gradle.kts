group = "org.example"
version = "1.0-SNAPSHOT"



repositories {
    mavenCentral()
}

val ktorVersion = "1.3.0"

dependencies {
    implementation(kotlin("stdlib-jdk8"))

    fun ktor(module: String, ver: String = ktorVersion) = "io.ktor:ktor-$module:$ver"
    implementation(ktor("server-core"))
    implementation(ktor("server-netty"))
    implementation(ktor("client-cio"))
    implementation(ktor("serialization"))

    implementation("com.google.cloud:google-cloud-tasks:1.28.2")

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
    }
}
