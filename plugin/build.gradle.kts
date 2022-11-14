
plugins {
    `kotlin-dsl`
    id("com.gradle.plugin-publish") version "1.1.0"
}

group = "org.gradle.disco"
version = "0.1"

repositories {
    mavenCentral()
}

dependencies {
    implementation("com.google.code.gson:gson:2.9.1")
}

gradlePlugin {
    vcsUrl.set("https://github.com/gradle/disco-toolchains")
    website.set("https://github.com/gradle/disco-toolchains")
    val discoToolchains by plugins.creating {
        id = "org.gradle.disco-toolchains"
        implementationClass = "org.gradle.disco.DiscoToolchainsPlugin"
        displayName = "Disco API Toolchains Provisioner by Gradle"
        description = "Toolchains provisioner using the Disco Foojay API for resolving Java runtimes - developed by Gradle"
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
