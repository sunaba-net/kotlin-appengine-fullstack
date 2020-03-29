import org.jetbrains.kotlin.gradle.targets.js.webpack.KotlinWebpackConfig

repositories {
    maven("https://kotlin.bintray.com/kotlin-js-wrappers/")
}

val ktorVersion = "1.3.1"

dependencies {
    implementation(kotlin("stdlib-js"))

    val react_version = "16.13.0"
    val kotlin_react_version = "${react_version}-pre.93-kotlin-1.3.70"

    implementation("org.jetbrains:kotlin-react:${kotlin_react_version}")
    implementation("org.jetbrains:kotlin-react-dom:${kotlin_react_version}")
    implementation(npm("react", react_version))
    implementation(npm("react-dom", react_version))

    //Kotlin Styled (chapter 3)
    implementation("org.jetbrains:kotlin-styled:1.0.0-pre.94-kotlin-1.3.70")
    implementation(npm("styled-components"))
    implementation(npm("inline-style-prefixer"))

    //Video Player (chapter 7)
    implementation(npm("react-player"))

    //Share Buttons (chapter 7)
    implementation(npm("react-share"))
}

kotlin {
    target {
        browser {
            useCommonJs()
            runTask {
            }
        }
    }
}
