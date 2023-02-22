package org.gradle.toolchains.foojay

import org.gradle.jvm.toolchain.JavaToolchainDownload
import org.gradle.jvm.toolchain.JavaToolchainRequest
import org.gradle.jvm.toolchain.JavaToolchainResolver
import java.util.*

abstract class FoojayToolchainResolver: JavaToolchainResolver {

    private val api: FoojayApi = FoojayApi()

    override fun resolve(request: JavaToolchainRequest): Optional<JavaToolchainDownload> {
        val spec = request.javaToolchainSpec
        val platform = request.buildPlatform
        val links = api.toLinks(
            spec.languageVersion.get(),
            spec.vendor.get(),
            spec.implementation.get(),
            platform.operatingSystem,
            platform.architecture
        )
        val uri = api.toUri(links)
        return Optional.ofNullable(uri).map(JavaToolchainDownload::fromUri)
    }
}