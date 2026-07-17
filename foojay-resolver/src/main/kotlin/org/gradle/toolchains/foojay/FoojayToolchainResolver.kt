package org.gradle.toolchains.foojay

import org.gradle.api.provider.Property
import org.gradle.jvm.toolchain.JavaToolchainDownload
import org.gradle.jvm.toolchain.JavaToolchainRequest
import org.gradle.jvm.toolchain.JavaToolchainResolver
import org.gradle.jvm.toolchain.JavaToolchainSpec
import org.gradle.util.GradleVersion
import java.util.*

@Suppress("UnstableApiUsage")
abstract class FoojayToolchainResolver : JavaToolchainResolver {

    private val apiService = FoojayApiServiceBridge.getService()
        ?: throw RuntimeException("failed to retrieve FoojayApiService instance")

    override fun resolve(request: JavaToolchainRequest): Optional<JavaToolchainDownload> {
        val spec = request.javaToolchainSpec
        val nativeImageCapable = if (GradleVersion.current().baseVersion >= GradleVersion.version("8.14")) {
            extractNativeImageCapability(spec)
        } else {
            false
        }
        val platform = request.buildPlatform
        val links = apiService.api.toPackage(
            spec.languageVersion.get(),
            spec.vendor.get(),
            spec.implementation.get(),
            nativeImageCapable,
            platform.operatingSystem,
            platform.architecture
        )?.links
        return Optional.ofNullable(links?.pkg_download_redirect).map(JavaToolchainDownload::fromUri)
    }

    @Suppress("UNCHECKED_CAST")
    private fun extractNativeImageCapability(spec: JavaToolchainSpec): Boolean {
        val result = spec.javaClass.getMethod("getNativeImageCapable").invoke(spec) as Property<Boolean>
        return result.getOrElse(false)
    }
}
