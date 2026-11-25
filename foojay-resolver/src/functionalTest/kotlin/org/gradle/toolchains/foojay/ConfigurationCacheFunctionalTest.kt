package org.gradle.toolchains.foojay

import org.gradle.testkit.runner.TaskOutcome
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ConfigurationCacheFunctionalTest : AbstractFoojayToolchainsPluginFunctionalTest() {

    @ParameterizedTest
    @ValueSource(strings = ["org.gradle.toolchains.foojay-resolver", "org.gradle.toolchains.foojay-resolver-convention"])
    fun `reuses configuration cache`(pluginId: String) {
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
        """.trimIndent() + "\n" + repositorySettings
        val buildScript = """
            plugins {
                java
            }

            java {
                toolchain {
                    languageVersion.set(JavaLanguageVersion.of(JavaVersion.current().majorVersion))
                }
            }
        """
        val runner = runner(settings, buildScript)
        runner.withArguments(runner.arguments + listOf(
            "--configuration-cache",
            "--configuration-cache-problems=fail",
            "-Dorg.gradle.java.installations.paths=" + System.getProperty("java.home")
        ))
        val first = runner.build()
        assertProvisioningSuccessful(first)
        assertTrue("Configuration cache entry stored." in first.output)

        val second = runner.build()
        assertTrue("Configuration cache entry reused." in second.output)
        assertEquals(TaskOutcome.UP_TO_DATE, second.task(":compileJava")?.outcome)
    }
}
