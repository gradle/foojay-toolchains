package org.gradle.toolchains.foojay

import org.gradle.api.provider.Property
import org.gradle.jvm.toolchain.JavaToolchainDownload
import org.gradle.jvm.toolchain.JavaToolchainRequest
import org.gradle.jvm.toolchain.JavaToolchainResolver
import org.gradle.jvm.toolchain.JavaToolchainSpec
import org.gradle.util.GradleVersion
import java.util.*

abstract class FoojayToolchainResolver : JavaToolchainResolver {

    private val api: FoojayApi = FoojayApi()

    override fun resolve(request: JavaToolchainRequest): Optional<JavaToolchainDownload> {
        val spec = request.javaToolchainSpec
        val nativeImageCapable = if (GradleVersion.current().baseVersion >= GradleVersion.version("8.14")) {
            extractNativeImageCapability(spec)
        } else {
            false
        }
        val platform = request.buildPlatform
        val links = api.toPackage(
            spec.languageVersion.get(),
            spec.vendor.get(),
            spec.implementation.get(),
            nativeImageCapable,
            platform.operatingSystem,
            platform.architecture
        )?.links
        val uri = api.toUri(links)
        return Optional.ofNullable(uri).map(JavaToolchainDownload::fromUri)
    }

    @Suppress("UNCHECKED_CAST")
    private fun extractNativeImageCapability(spec: JavaToolchainSpec): Boolean {
        val result = spec.javaClass.getMethod("getNativeImageCapable").invoke(spec) as Property<Boolean>
        return result.getOrElse(false)
    }
}
