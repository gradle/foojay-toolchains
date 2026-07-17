package org.gradle.toolchains.foojay

import org.gradle.internal.classpath.Instrumented.systemProperty
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import java.io.PrintWriter
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

    @ParameterizedTest(name = "gradle version: {0}")
    @MethodSource("getGradleTestVersions")
    fun `can use base plugin with proxy`(gradleVersion: String) {
        val settings = """
            plugins {
                id("org.gradle.toolchains.foojay-resolver")
            }
            
            // This is the only syntax compatible with version 7.6 to current (9.8).
            configure<org.gradle.toolchains.foojay.FoojayExtension> {
                proxy {
                    autoDetect.set(true)
                }
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
        val arguments = listOf(
            "-Dhttps.proxyHost=127.0.0.1",
            "-Dhttps.proxyPort=$proxyPort",
            "-Dhttps.nonProxyHosts=''",
            // To make sure Gradle does not pick up local JDK installations.
            "-Porg.gradle.java.installations.auto-detect=false"
        )
        val result = runner(settings, buildScript, arguments)
            .withGradleVersion(gradleVersion)
            .build()
        assertProvisioningSuccessful(result)
        assertTrue(proxyInterceptorCount.get() > 0, "Traffic bypassed the proxy.")
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
