
plugins {
    `java-gradle-plugin`

    id("org.jetbrains.kotlin.jvm") version "1.7.10"

    `maven-publish`
}

group = "org.gradle.disco"
version = "0.1"

repositories {
    mavenCentral()
}

dependencies {
    implementation("com.google.code.gson:gson:2.9.1")

    testImplementation("org.jetbrains.kotlin:kotlin-test-junit5")
}

gradlePlugin {
    val greeting by plugins.creating {
        id = "org.gradle.disco-toolchains"
        implementationClass = "org.gradle.disco.DiscoToolchainsPlugin"
    }
}

publishing {
    repositories {
        maven {
            url = uri(layout.projectDirectory.dir("repo"))
        }
    }
}

// Add a source set for the functional test suite
val functionalTestSourceSet = sourceSets.create("functionalTest") {
}

configurations["functionalTestImplementation"].extendsFrom(configurations["testImplementation"])

// Add a task to run the functional tests
val functionalTest by tasks.registering(Test::class) {
    testClassesDirs = functionalTestSourceSet.output.classesDirs
    classpath = functionalTestSourceSet.runtimeClasspath
    useJUnitPlatform()
}

gradlePlugin.testSourceSets(functionalTestSourceSet)

tasks.named<Task>("check") {
    // Run the functional tests as part of `check`
    dependsOn(functionalTest)
}

tasks.named<Test>("test") {
    // Use JUnit Jupiter for unit tests.
    useJUnitPlatform()
}
