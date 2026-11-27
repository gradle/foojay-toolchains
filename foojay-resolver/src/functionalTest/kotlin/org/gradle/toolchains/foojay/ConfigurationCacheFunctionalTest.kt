package org.gradle.toolchains.foojay

import org.gradle.testkit.runner.TaskOutcome
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ConfigurationCacheFunctionalTest : AbstractFoojayToolchainsPluginFunctionalTest() {

    @ParameterizedTest
    @CsvSource(
        "org.gradle.toolchains.foojay-resolver, false",
        "org.gradle.toolchains.foojay-resolver-convention, false",
        "org.gradle.toolchains.foojay-resolver, true",
        "org.gradle.toolchains.foojay-resolver-convention, true"
    )
    fun `reuses configuration cache`(pluginId: String, isolatedProjects: Boolean) {
        val repositorySettings = if (pluginId == "org.gradle.toolchains.foojay-resolver") {
            """
                toolchainManagement {
                    jvm {
                        javaRepositories {
                            repository("foojay") {
                                resolverClass.set(org.gradle.toolchains.foojay.FoojayToolchainResolver::class.java)
                            }
                        }
                    }
                }
            """.trimIndent()
        } else {
            ""
        }
        val settings = """
            plugins {
                id("$pluginId")
            }

            include("app", "core")
        """.trimIndent() + "\n" + repositorySettings
        writeSubprojects()

        val runner = runnerForTask(settings, ":app:compileJava")
        val mode = if (isolatedProjects) "-Dorg.gradle.unsafe.isolated-projects=true" else "--configuration-cache"
        runner.withArguments(runner.arguments + listOf(mode, "--configuration-cache-problems=fail"))

        val first = runner.build()
        assertEquals(TaskOutcome.SUCCESS, first.task(":core:compileJava")?.outcome)
        assertEquals(TaskOutcome.SUCCESS, first.task(":app:compileJava")?.outcome)
        assertTrue("Configuration cache entry stored." in first.output)

        val second = runner.build()
        assertTrue("Configuration cache entry reused." in second.output)
        assertEquals(TaskOutcome.UP_TO_DATE, second.task(":core:compileJava")?.outcome)
        assertEquals(TaskOutcome.UP_TO_DATE, second.task(":app:compileJava")?.outcome)
    }

    private fun writeSubprojects() {
        val buildScript = """
            plugins {
                java
            }

            java {
                toolchain {
                    languageVersion.set(JavaLanguageVersion.of(${getDifferentJavaVersion()}))
                }
            }
        """.trimIndent()
        val coreDir = projectDir.resolve("core")
        val appDir = projectDir.resolve("app")
        val coreSources = coreDir.resolve("src/main/java")
        val appSources = appDir.resolve("src/main/java")
        coreSources.mkdirs()
        appSources.mkdirs()
        coreSources.resolve("Core.java").writeText("public class Core {}")
        appSources.resolve("App.java").writeText("public class App extends Core {}")
        coreDir.resolve("build.gradle.kts").writeText(buildScript)
        val appDependencies = """

            dependencies {
                implementation(project(":core"))
            }
        """.trimIndent()
        appDir.resolve("build.gradle.kts").writeText(buildScript + "\n\n" + appDependencies)
    }
}
