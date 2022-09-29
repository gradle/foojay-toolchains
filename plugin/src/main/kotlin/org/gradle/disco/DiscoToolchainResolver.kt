package org.gradle.disco

import org.gradle.jvm.toolchain.JavaToolchainDownload
import org.gradle.jvm.toolchain.JavaToolchainRequest
import org.gradle.jvm.toolchain.JavaToolchainResolver
import java.util.*

abstract class DiscoToolchainResolver: JavaToolchainResolver {

    private val api: DiscoApi = DiscoApi()

    override fun resolve(request: JavaToolchainRequest): Optional<JavaToolchainDownload> {
        val spec = request.javaToolchainSpec
        val platform = request.buildPlatform
        val uri = api.toUri(
            spec.languageVersion.get(),
            spec.vendor.get(),
            spec.implementation.get(),
            platform.operatingSystem,
            platform.architecture
        )
        return Optional.ofNullable(uri).map(JavaToolchainDownload::fromUri)
    }
}