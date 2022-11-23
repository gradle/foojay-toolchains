package org.gradle.toolchains.foojay

import kotlin.test.Test

class FoojayToolchainsConventionPluginFunctionalTest: AbstractFoojayToolchainsPluginFunctionalTest() {

    //todo: add this option to the documentation

    @Test
    fun `can use convention plugin`() {
        val settings = """
            plugins {
                id("org.gradle.toolchains.foojay-resolver-convention") version "$pluginVersion"
            }
        """.trimIndent()
        runTest(settings)
    }

}