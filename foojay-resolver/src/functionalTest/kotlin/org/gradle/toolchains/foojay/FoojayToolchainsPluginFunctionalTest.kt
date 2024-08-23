package org.gradle.toolchains.foojay

import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import kotlin.test.Test
import kotlin.test.assertTrue

class FoojayToolchainsPluginFunctionalTest: AbstractFoojayToolchainsPluginFunctionalTest() {

    @ParameterizedTest(name = "gradle version: {0}")
    @MethodSource("getGradleTestVersions")
    fun `can use base plugin`(gradleVersion: String) {
        val settings = """
            plugins {
                id("org.gradle.toolchains.foojay-resolver")
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
        """.trimIndent()

        val buildScript = """
            plugins {
                java
            }
            
            java {
                toolchain {
                    languageVersion.set(JavaLanguageVersion.of(${getDifferentJavaVersion()}))
                }
            }
        """
        val result = runner(settings, buildScript)
            .withGradleVersion(gradleVersion)
            .build()
        assertProvisioningSuccessful(result)
    }

    @Test
    fun `generates useful error for unsupported Gradle versions`() {
        val settings = """
            plugins {
                id("org.gradle.toolchains.foojay-resolver")
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
        """.trimIndent()

        val buildScript = """
            plugins {
                java
            }
            
            java {
                toolchain {
                    languageVersion.set(JavaLanguageVersion.of(${getDifferentJavaVersion()}))
                }
            }
        """
        val result = runner(settings, buildScript)
            .withGradleVersion("7.5")
            .buildAndFail()

        assertTrue("FoojayToolchainsPlugin needs Gradle version 7.6 or higher" in result.output)
    }

    @ParameterizedTest(name = "gradle version: {0}")
    @MethodSource("getGradleTestVersions")
    fun `provides meaningful error when applied as a project plugin`(gradleVersion: String) {
        val settings = ""

        val buildScript = """
            plugins {
                java
                id("org.gradle.toolchains.foojay-resolver")
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
            
            java {
                toolchain {
                    languageVersion.set(JavaLanguageVersion.of(${getDifferentJavaVersion()}))
                }
            }
        """
        val result = runner(settings, buildScript)
            .withGradleVersion(gradleVersion)
            .buildAndFail()

        assertTrue("> Failed to apply plugin 'org.gradle.toolchains.foojay-resolver'.\n" +
                "   > Settings plugins must be applied in the settings script." in result.output)
    }
}
