package org.gradle.toolchains.foojay

import org.gradle.jvm.toolchain.JavaToolchainDownload
import org.gradle.jvm.toolchain.JavaToolchainRequest
import org.gradle.jvm.toolchain.JavaToolchainResolver
import org.gradle.platform.Architecture
import org.gradle.platform.OperatingSystem
import java.util.*

abstract class FoojayToolchainResolver: JavaToolchainResolver {

    private val api: FoojayApi = FoojayApi()

    override fun resolve(request: JavaToolchainRequest): Optional<JavaToolchainDownload> {
        val spec = request.javaToolchainSpec
        val platform = request.buildPlatform
        val links = api.toPackage(
            spec.languageVersion.get(),
            spec.vendor.get(),
            spec.implementation.get(),
            platform.operatingSystem,
            platform.architecture
        )?.links
        var uri = api.toUri(links)

        // Also try x64 on M-series Macs
        if(uri == null && platform.operatingSystem == OperatingSystem.MAC_OS && platform.architecture == Architecture.AARCH64) {
            uri = api.toUri(api.toPackage(
                spec.languageVersion.get(),
                spec.vendor.get(),
                spec.implementation.get(),
                platform.operatingSystem,
                Architecture.X86_64
            )?.links)
        }

        return Optional.ofNullable(uri).map(JavaToolchainDownload::fromUri)
    }
}
