plugins {
    id("com.gradle.develocity") version "4.2"
    id("io.github.gradle.gradle-enterprise-conventions-plugin").version("0.10.3")
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}

dependencyResolutionManagement {
    repositories {
        mavenCentral()
    }
}

rootProject.name = "foojay-toolchains"
include("foojay-resolver")
