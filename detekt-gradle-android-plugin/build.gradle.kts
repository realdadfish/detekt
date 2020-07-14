plugins {
    `java-gradle-plugin`
    id("com.gradle.plugin-publish") version "0.11.0"
}

repositories {
    google()
}

dependencies {
    implementation(kotlin("gradle-plugin"))
    implementation(kotlin("gradle-plugin-api"))
    api(project(":detekt-gradle-plugin"))
    compileOnly("com.android.tools.build:gradle:4.0.0")

    testImplementation(project(":detekt-test-utils"))
}

gradlePlugin {
    plugins {
        register("detektAndroidPlugin") {
            id = "io.gitlab.arturbosch.detekt"
            implementationClass = "io.gitlab.arturbosch.detekt.DetektAndroidPlugin"
        }
    }
}

tasks.validatePlugins {
    enableStricterValidation.set(true)
}

pluginBundle {
    website = "https://arturbosch.github.io/detekt"
    vcsUrl = "https://github.com/detekt/detekt"
    description = "Static code analysis for Kotlin"
    tags = listOf("kotlin", "detekt", "code-analysis", "linter", "codesmells", "android")

    (plugins) {
        "detektAndroidPlugin" {
            id = "io.gitlab.arturbosch.detekt.android"
            displayName = "Static code analysis for Kotlin on Android"
        }
    }
}
