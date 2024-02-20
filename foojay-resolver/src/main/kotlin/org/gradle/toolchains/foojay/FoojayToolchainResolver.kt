package org.gradle.toolchains.foojay

import org.gradle.api.logging.Logging
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
        if (matchingDownloadUri.isFailure) logWarning()
        return Optional.ofNullable(matchingDownloadUri.getOrThrow()).map(JavaToolchainDownload::fromUri)
    }

    private fun logWarning() {
        Logging.getLogger(FoojayToolchainResolver::class.java).warn("Failed to resolve Java toolchain")
    }
}