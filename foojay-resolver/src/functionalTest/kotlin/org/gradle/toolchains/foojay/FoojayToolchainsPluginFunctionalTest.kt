package org.gradle.toolchains.foojay

import kotlin.test.Test
import kotlin.test.assertTrue

class FoojayToolchainsPluginFunctionalTest: AbstractFoojayToolchainsPluginFunctionalTest() {

    @Test
    fun `can use base plugin`() {
        val settings = """
            plugins {
                id("org.gradle.toolchains.foojay-resolver") version "$pluginVersion"
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

        val result = runner(settings).build()

        assertTrue(result.output.contains("Installed toolchain from https://api.foojay.io/disco/"))
    }

    @Test
    fun `generates useful error for unsupported Gradle versions`() {
        val settings = """
            plugins {
                id("org.gradle.toolchains.foojay-resolver") version "$pluginVersion"
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

        val result = runner(settings)
                .withGradleVersion("7.5")
                .buildAndFail()

        assertTrue(result.output.contains("FoojayToolchainsPlugin needs Gradle version 7.6 or higher"))
    }
}