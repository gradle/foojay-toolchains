plugins {
    id("com.gradle.develocity") version "4.4.3"
    id("io.github.gradle.develocity-conventions-plugin") version "0.15.0"
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}

dependencyResolutionManagement {
    repositories {
        mavenCentral()
    }
}

rootProject.name = "foojay-toolchains"
include("foojay-resolver")
