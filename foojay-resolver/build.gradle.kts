@file:Suppress("UNUSED_VARIABLE")

import java.io.FileNotFoundException

plugins {
    `kotlin-dsl`
    signing
    id("com.gradle.plugin-publish") version "1.1.0"
}

group = "org.gradle.toolchains"
val pluginVersion = property("pluginVersion") ?: throw GradleException("`pluginVersion` missing in gradle.properties!")
version = pluginVersion

repositories {
    mavenCentral()
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(8))
    }
}

dependencies {
    implementation("com.google.code.gson:gson:2.9.1")
}

gradlePlugin {
    vcsUrl.set("https://github.com/gradle/disco-toolchains")
    website.set("https://github.com/gradle/disco-toolchains")

    val discoToolchains by plugins.creating {
        id = "org.gradle.toolchains.foojay-resolver"
        implementationClass = "org.gradle.toolchains.foojay.FoojayToolchainsPlugin"
        displayName = "Foojay Disco API Toolchains Resolver"
        description = "Toolchains resolver using the Foojay Disco API for resolving Java runtimes."
        tags.set(listOf("gradle", "toolchains"))
    }

    val discoToolchainsConvenience by plugins.creating {
        id = "org.gradle.toolchains.foojay-resolver-convention"
        implementationClass = "org.gradle.toolchains.foojay.FoojayToolchainsConventionPlugin"
        displayName = "Foojay Disco API Toolchains Resolver Convention"
        description = "Toolchains resolver using the Foojay Disco API for resolving Java runtimes. Automatically configures toolchain management."
        tags.set(listOf("gradle", "toolchains"))
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

tasks.named<Task>("check") {
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
        if (releaseNotes.isBlank()) {
            throw IllegalArgumentException("Release notes file $releaseNotesFile.absolutePath is empty")
        }
        gradlePlugin.plugins["discoToolchains"].description = releaseNotes
        gradlePlugin.plugins["discoToolchainsConvenience"].description = releaseNotes
    }
}

tasks.publishPlugins {
    dependsOn(readReleaseNotes)
}