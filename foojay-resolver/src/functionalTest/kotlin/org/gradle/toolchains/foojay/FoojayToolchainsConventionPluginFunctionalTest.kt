package org.gradle.toolchains.foojay

import kotlin.test.Test
import kotlin.test.assertTrue

class FoojayToolchainsConventionPluginFunctionalTest: AbstractFoojayToolchainsPluginFunctionalTest() {

    @Test
    fun `can use convention plugin`() {
        val settings = """
            plugins {
                id("org.gradle.toolchains.foojay-resolver-convention")
            }
        """.trimIndent()

        val result = runner(settings).build()

        assertTrue("Installed toolchain from https://api.foojay.io/disco/" in result.output)
    }

    @Test
    fun `generates useful error for unsupported Gradle versions`() {
        val settings = """
            plugins {
                id("org.gradle.toolchains.foojay-resolver-convention")
            }
        """.trimIndent()

        val result = runner(settings)
                .withGradleVersion("7.5")
                .buildAndFail()

        assertTrue("FoojayToolchainsPlugin needs Gradle version 7.6 or higher" in result.output)
    }

}