plugins {
    id("com.gradle.develocity") version "3.17"
    id("org.gradle.toolchains.foojay-resolver-convention") version "0.9.0"
}

develocity {
    buildScan {
        termsOfUseUrl = "https://gradle.com/terms-of-service"
        termsOfUseAgree = "yes"
        // TODO: workaround for https://github.com/gradle/gradle/issues/22879.
        val isCI = providers.environmentVariable("CI").isPresent
        publishing.onlyIf { isCI }
    }
}

dependencyResolutionManagement {
    repositories {
        mavenCentral()
    }
}

rootProject.name = "foojay-toolchains"
include("foojay-resolver")
