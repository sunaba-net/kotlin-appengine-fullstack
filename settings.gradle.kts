rootProject.name = "kotlin-appengine-fullstack"
include("backend")


pluginManagement {
    repositories {
        gradlePluginPortal()
        google()
    }
    resolutionStrategy {
        eachPlugin {
            if (requested.id.id == "com.google.cloud.tools.appengine") {
                useModule("com.google.cloud.tools:appengine-gradle-plugin:${requested.version}")
            }
        }
    }
}

include("frontend")
include("model")
include("serial-module-generator")
include("cs-model-generator")
