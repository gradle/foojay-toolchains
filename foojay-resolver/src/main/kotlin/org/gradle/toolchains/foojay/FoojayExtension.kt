package org.gradle.toolchains.foojay

import org.gradle.api.Action
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.Property
import javax.inject.Inject

@Suppress("UnusedPrivateProperty")
abstract class ProxyConfig @Inject constructor(objects: ObjectFactory) {
    abstract val autoDetect: Property<Boolean>

    init {
        autoDetect.convention(false)
    }
}

abstract class FoojayExtension @Inject constructor(objectFactory: ObjectFactory) {

    val proxy: ProxyConfig = objectFactory.newInstance(ProxyConfig::class.java)

    fun proxy(action: Action<ProxyConfig>) {
        action.execute(proxy)
    }
}
