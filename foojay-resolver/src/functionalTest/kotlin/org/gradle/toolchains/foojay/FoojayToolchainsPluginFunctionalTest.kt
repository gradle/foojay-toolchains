package org.gradle.toolchains.foojay

import kotlin.test.Test

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
        runTest(settings)
    }
}