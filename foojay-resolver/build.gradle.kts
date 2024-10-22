@file:Suppress("UnusedPrivateProperty", "UnstableApiUsage")

import java.io.FileNotFoundException

plugins {
    `kotlin-dsl`
    signing
    id("com.gradle.plugin-publish") version "1.2.1"
    id("io.gitlab.arturbosch.detekt") version "1.23.6"
}

group = "org.gradle.toolchains"
val pluginVersion = property("pluginVersion") ?: throw GradleException("`pluginVersion` missing in gradle.properties!")
version = pluginVersion

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(8)
    }
}

detekt {
    buildUponDefaultConfig = true // preconfigure defaults
    config.setFrom(project.rootProject.file("gradle/detekt.yml"))

    // also check the project build file
    source.from(project.buildFile)
}

dependencies {
    implementation("com.google.code.gson:gson:2.10.1")
}

gradlePlugin {
    vcsUrl = "https://github.com/gradle/foojay-toolchains"
    website = "https://github.com/gradle/foojay-toolchains"

    val discoToolchains by plugins.creating {
        id = "org.gradle.toolchains.foojay-resolver"
        implementationClass = "org.gradle.toolchains.foojay.FoojayToolchainsPlugin"
        displayName = "Foojay Disco API Toolchains Resolver"
        description = "Toolchains resolver using the Foojay Disco API for resolving Java runtimes."
        tags = listOf("gradle", "toolchains")
    }

    val discoToolchainsConvenience by plugins.creating {
        id = "org.gradle.toolchains.foojay-resolver-convention"
        implementationClass = "org.gradle.toolchains.foojay.FoojayToolchainsConventionPlugin"
        displayName = "Foojay Disco API Toolchains Resolver Convention"
        description = "Toolchains resolver using the Foojay Disco API for resolving Java runtimes. Automatically configures toolchain management."
        tags = listOf("gradle", "toolchains")
    }

}

publishing {
    repositories {
        maven {
            url = uri(layout.projectDirectory.dir("repo"))
        }
    }
}

signing {
    useInMemoryPgpKeys(
            project.providers.environmentVariable("PGP_SIGNING_KEY").orNull,
            project.providers.environmentVariable("PGP_SIGNING_KEY_PASSPHRASE").orNull
    )
}

testing {
    suites {
        val functionalTest by registering(JvmTestSuite::class) {
            dependencies {
                implementation("org.jetbrains.kotlin:kotlin-test-junit5")
            }
        }
        val test by getting(JvmTestSuite::class) {
            useJUnitJupiter()
            dependencies {
                implementation("org.jetbrains.kotlin:kotlin-test-junit5")
            }
        }
    }
}

gradlePlugin.testSourceSets(sourceSets.getAt("functionalTest"))

tasks.check {
    // Run the functional tests as part of `check`
    dependsOn(testing.suites.named("functionalTest"))
}

val readReleaseNotes by tasks.registering {
    description = "Ensure we've got some release notes handy"
    doLast {
        val releaseNotesFile = file("release-notes-$version.txt")
        if (!releaseNotesFile.exists()) {
            throw FileNotFoundException("Couldn't find release notes file $releaseNotesFile.absolutePath")
        }
        val releaseNotes = releaseNotesFile.readText().trim()
        require(releaseNotes.isBlank()) { "Release notes file $releaseNotesFile.absolutePath is empty" }
        gradlePlugin.plugins["discoToolchains"].description = releaseNotes
        gradlePlugin.plugins["discoToolchainsConvenience"].description = releaseNotes
    }
}

tasks.publishPlugins {
    dependsOn(readReleaseNotes)
}

tasks.check {
    dependsOn(tasks.named("detektTest"), tasks.named("detektFunctionalTest"))
}
