package org.gradle.toolchains.foojay

import org.gradle.jvm.toolchain.JavaLanguageVersion
import org.gradle.jvm.toolchain.JvmImplementation
import org.gradle.jvm.toolchain.JvmVendorSpec
import org.gradle.platform.Architecture
import org.gradle.platform.OperatingSystem
import org.jetbrains.annotations.VisibleForTesting
import java.net.URI

internal interface Service {
    fun findMatchingDownloadUri(
        version: JavaLanguageVersion,
        vendor: JvmVendorSpec,
        implementation: JvmImplementation,
        operatingSystem: OperatingSystem,
        architecture: Architecture
    ): Result<URI?>
}

internal class FoojayService(
    private val api: Api
) : Service {

    private val cachedDistributions = mutableListOf<Distribution>()

    override fun findMatchingDownloadUri(
        version: JavaLanguageVersion,
        vendor: JvmVendorSpec,
        implementation: JvmImplementation,
        operatingSystem: OperatingSystem,
        architecture: Architecture
    ): Result<URI?> = findMatchingDistributions(vendor, implementation, version)
        .map {
            it.asSequence()
                .mapNotNull { distribution ->
                    val matchingPackage = findMatchingPackage(
                        distribution.api_parameter,
                        version,
                        operatingSystem,
                        architecture
                    )
                    matchingPackage?.links
                }
                .firstOrNull()
                ?.pkg_download_redirect
        }

    @VisibleForTesting()
    internal fun findMatchingDistributions(
        vendor: JvmVendorSpec,
        implementation: JvmImplementation,
        version: JavaLanguageVersion
    ): Result<List<Distribution>> {
        val distributions = fetchDistributionsIfMissing()
        if (distributions.isFailure) return distributions
        return Result.success(match(distributions.getOrThrow(), vendor, implementation, version))
    }

    private fun fetchDistributionsIfMissing(): Result<List<Distribution>> {
        if (cachedDistributions.isEmpty()) {
            val distributionResult = api.fetchDistributions(
                mapOf("include_versions" to "true", "include_synonyms" to "true")
            )

            if (distributionResult.isFailure) return Result.failure(distributionResult.exceptionOrNull()!!)
            cachedDistributions.addAll(parseDistributions(distributionResult.getOrThrow()))
        }

        return Result.success(cachedDistributions)
    }

    @VisibleForTesting
    internal fun findMatchingPackage(
        distributionName: String,
        version: JavaLanguageVersion,
        operatingSystem: OperatingSystem,
        architecture: Architecture
    ): Package? {
        val versionApiKey = when {
            distributionName.startsWith("graalvm_community") -> "version"
            else -> "jdk_version"
        }

        val packagesResult = api.fetchPackages(
            mapOf(
                versionApiKey to "$version",
                "distro" to distributionName,
                "operating_system" to operatingSystem.toApiValue(),
                "latest" to "available",
                "directly_downloadable" to "true"
            )
        )

        if (packagesResult.isFailure) return null
        return match(parsePackages(packagesResult.getOrThrow()), architecture)
    }
}