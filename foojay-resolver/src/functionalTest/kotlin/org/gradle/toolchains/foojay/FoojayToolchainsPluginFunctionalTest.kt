package org.gradle.toolchains.foojay

import org.gradle.testkit.runner.GradleRunner
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.io.TempDir
import java.io.File
import kotlin.test.Test
import kotlin.test.assertTrue

class FoojayToolchainsPluginFunctionalTest {

    @field:TempDir
    lateinit var projectDir: File

    @field:TempDir
    lateinit var homeDir: File

    private val pluginVersion = "0.1"

    private val buildFile by lazy { projectDir.resolve("build.gradle.kts") }
    private val settingsFile by lazy { projectDir.resolve("settings.gradle.kts") }
    private val propertiesFile by lazy { projectDir.resolve("gradle.properties") }

    @BeforeEach
    internal fun setUp() {
        propertiesFile.writeText("""
            org.gradle.java.installations.auto-detect=false
            org.gradle.java.installations.auto-download=true
        """.trimIndent())
    }

    @Test
    fun `can use basic plugin`() {
        // Set up the test build
        settingsFile.writeText("""
            plugins {
                id("org.gradle.toolchains.foojay-resolver") version "${pluginVersion}"
            }
            
            toolchainManagement {
                jvm { 
                    javaRepositories {
                        repository("foojay") { 
                            resolverClass.set(org.gradle.toolchains.foojay.FoojayToolchainResolver::class.java)
                        }
                    }
                }
            }
        """.trimIndent())
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