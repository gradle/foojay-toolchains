package org.gradle.toolchains.foojay

import org.gradle.api.initialization.Settings
import org.gradle.kotlin.dsl.jvm

@Suppress("unused")
abstract class FoojayToolchainsConventionPlugin: AbstractFoojayToolchainPlugin() {

    override fun apply(settings: Settings) {
        settings.plugins.apply(FoojayToolchainsPlugin::class.java)

        settings.toolchainManagement {
            jvm {
                javaRepositories {
                    repository("foojay") {
                        resolverClass.set(FoojayToolchainResolver::class.java)
                    }
                }
            }
        }
    }

}
