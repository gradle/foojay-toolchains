package org.gradle.toolchains.foojay

import kotlin.test.Test
import kotlin.test.assertTrue

class FoojayToolchainsPluginFunctionalTest: AbstractFoojayToolchainsPluginFunctionalTest() {

    @Test
    fun `can use base plugin`() {
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
        val result = runner(settings, buildScript).build()

        assertTrue("Installed toolchain from https://api.foojay.io/disco/" in result.output)
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

    @Test
    fun `provides meaningful error when applied as a project plugin`() {
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
        val result = runner(settings, buildScript).buildAndFail()

        assertTrue("> Failed to apply plugin 'org.gradle.toolchains.foojay-resolver'.\n" +
                "   > Settings plugins must be applied in the settings script." in result.output)
    }
}