package org.gradle.toolchains.foojay

import org.gradle.api.provider.Provider


object FoojayApiServiceBridge {
    private var serviceProvider: Provider<FoojayApiService>? = null

    fun init(serviceProvider: Provider<FoojayApiService>) {
        this.serviceProvider = serviceProvider
    }

    fun getService(): FoojayApiService? {
        return serviceProvider?.orNull
    }
}