package org.gradle.toolchains.foojay

import org.gradle.testkit.runner.GradleRunner
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.io.TempDir
import java.io.File
import kotlin.test.assertTrue

abstract class AbstractFoojayToolchainsPluginFunctionalTest {

    @field:TempDir
    protected lateinit var projectDir: File

    @field:TempDir
    protected lateinit var homeDir: File

    protected val pluginVersion = "0.2"

    private val settingsFile by lazy { projectDir.resolve("settings.gradle.kts") }
    private val propertiesFile by lazy { projectDir.resolve("gradle.properties") }
    private val buildFile by lazy { projectDir.resolve("build.gradle.kts") }

    @BeforeEach
    internal fun setUp() {
        propertiesFile.writeText("""
            org.gradle.java.installations.auto-detect=false
            org.gradle.java.installations.auto-download=true
        """.trimIndent())
    }

    protected fun runTest(settings: String) {
        settingsFile.writeText(settings)
        buildFile.writeText("""
                plugins {
                    java
                }
                
                java {
                    toolchain {
                        languageVersion.set(JavaLanguageVersion.of(${getDifferentJavaVersion()}))
                    }
                }
            """.trimIndent())

        // Run the build
        val runner = GradleRunner.create()
        runner.forwardOutput()
        runner.withPluginClasspath()
        runner.withArguments(listOf("--info", "-g", homeDir.absolutePath, "compileJava"))
        runner.withProjectDir(projectDir)
        val result = runner.build()

        // Verify the result
        assertTrue(result.output.contains("Installed toolchain from https://api.foojay.io/disco/"))
    }

    private fun getDifferentJavaVersion() = when {
        System.getProperty("java.version").startsWith("11.") -> "16"
        else -> "11"
    }
}