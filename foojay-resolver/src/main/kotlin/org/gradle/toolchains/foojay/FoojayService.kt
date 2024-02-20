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
    ): URI?
}

internal class FoojayService(
    private val api: Api
) : Service {

    private val distributions = mutableListOf<Distribution>()

    override fun findMatchingDownloadUri(
        version: JavaLanguageVersion,
        vendor: JvmVendorSpec,
        implementation: JvmImplementation,
        operatingSystem: OperatingSystem,
        architecture: Architecture
    ): URI? = findMatchingDistributions(vendor, implementation, version)
        .asSequence()
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

    @VisibleForTesting()
    internal fun findMatchingDistributions(
        vendor: JvmVendorSpec,
        implementation: JvmImplementation,
        version: JavaLanguageVersion
    ): List<Distribution> {
        fetchDistributionsIfMissing()
        return match(distributions, vendor, implementation, version)
    }

    private fun fetchDistributionsIfMissing() {
        if (distributions.isEmpty()) {
            val distributionJson = api.fetchDistributions(
                mapOf("include_versions" to "true", "include_synonyms" to "true")
            )

            distributions.addAll(parseDistributions(distributionJson))
        }
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

        val packagesJson = api.fetchPackages(
            mapOf(
                versionApiKey to "$version",
                "distro" to distributionName,
                "operating_system" to operatingSystem.toApiValue(),
                "latest" to "available",
                "directly_downloadable" to "true"
            )
        )

        val packages = parsePackages(packagesJson)
        return match(packages, architecture)
    }
}