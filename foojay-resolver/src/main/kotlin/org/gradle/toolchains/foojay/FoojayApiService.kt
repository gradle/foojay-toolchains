package org.gradle.toolchains.foojay

import org.gradle.api.provider.Property
import org.gradle.api.services.BuildService
import org.gradle.api.services.BuildServiceParameters

interface FoojayApiParameters : BuildServiceParameters {
    val detectProxy: Property<Boolean>

    fun toConfigs(): FoojayApiConfig {
        return FoojayApiConfig().apply {
            proxy.autoDetect = detectProxy.get()
        }
    }
}

abstract class FoojayApiService : BuildService<FoojayApiParameters> {
    val api = FoojayApi(parameters.toConfigs())
}
