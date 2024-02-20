package org.gradle.toolchains.foojay

import org.gradle.jvm.toolchain.JavaToolchainDownload
import org.gradle.jvm.toolchain.JavaToolchainRequest
import org.gradle.jvm.toolchain.JavaToolchainResolver
import java.util.Optional

abstract class FoojayToolchainResolver : JavaToolchainResolver {

    private val service: FoojayService = FoojayService(FoojayApi())

    override fun resolve(request: JavaToolchainRequest): Optional<JavaToolchainDownload> {
        val spec = request.javaToolchainSpec
        val platform = request.buildPlatform
        val matchingDownloadUri = service.findMatchingDownloadUri(
            spec.languageVersion.get(),
            spec.vendor.get(),
            spec.implementation.get(),
            platform.operatingSystem,
            platform.architecture
        )
        return Optional.ofNullable(matchingDownloadUri).map(JavaToolchainDownload::fromUri)
    }
}