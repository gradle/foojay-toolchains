package org.gradle.toolchains.foojay

import org.gradle.api.GradleException
import org.gradle.jvm.toolchain.JavaLanguageVersion
import org.gradle.jvm.toolchain.JvmImplementation
import org.gradle.jvm.toolchain.JvmVendorSpec
import org.gradle.platform.Architecture
import org.gradle.platform.OperatingSystem
import java.net.URI
import java.net.URLEncoder
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.nio.charset.StandardCharsets.UTF_8
import java.time.Duration

@Suppress("MagicNumber")
private val CONNECT_TIMEOUT = Duration.ofSeconds(10)
@Suppress("MagicNumber")
private val READ_TIMEOUT = Duration.ofSeconds(20)

private const val SCHEMA = "https"

private const val ENDPOINT_ROOT = "api.foojay.io/disco/v3.0"
private const val DISTRIBUTIONS_ENDPOINT = "$ENDPOINT_ROOT/distributions"
private const val PACKAGES_ENDPOINT = "$ENDPOINT_ROOT/packages"

@Suppress("UnstableApiUsage")
class FoojayApi {

    private val distributions = mutableListOf<Distribution>()

    private val httpClient = HttpClient.newBuilder()
        .connectTimeout(CONNECT_TIMEOUT)
        .build()

    @Suppress("LongParameterList")
    fun toPackage(
        version: JavaLanguageVersion,
        vendor: JvmVendorSpec,
        implementation: JvmImplementation,
        nativeImageCapable: Boolean,
        operatingSystem: OperatingSystem,
        architecture: Architecture
    ): Package? {
        val distributions = match(vendor, implementation, version, nativeImageCapable)
        return distributions.asSequence().mapNotNull { distribution ->
            match(distribution.api_parameter, version, operatingSystem, architecture)
        }.firstOrNull()
    }

    internal fun match(vendor: JvmVendorSpec, implementation: JvmImplementation, version: JavaLanguageVersion, nativeImageCapable: Boolean): List<Distribution> {
        fetchDistributionsIfMissing()
        return match(distributions, vendor, implementation, version, nativeImageCapable)
    }

    private fun fetchDistributionsIfMissing() {
        if (distributions.isEmpty()) {
            val json = downloadVendorList(
                DISTRIBUTIONS_ENDPOINT,
                mapOf("include_versions" to "true", "include_synonyms" to "true")
            )
            distributions.addAll(parseDistributions(json))
        }
    }

    internal fun match(distributionName: String, version: JavaLanguageVersion, operatingSystem: OperatingSystem, architecture: Architecture): Package? {
        val versionApiKey = when {
            distributionName.startsWith("graalvm_community") -> "version"
            distributionName == "graalvm" -> "version"
            else -> "jdk_version"
        }
        val json = downloadVendorList(
            PACKAGES_ENDPOINT,
            mapOf(
                versionApiKey to "$version",
                "distro" to distributionName,
                "operating_system" to operatingSystem.toApiValue(),
                "latest" to "available",
                "directly_downloadable" to "true"
            )
        )
        val packages = parsePackages(json)
        return match(packages, architecture)
    }

    @Suppress("MagicNumber")
    private fun downloadVendorList(
        endpoint: String,
        params: Map<String, String>
    ): String {
        val uri = URI.create("$SCHEMA://$endpoint?${toParameterString(params)}")
        val request = HttpRequest.newBuilder()
            .uri(uri)
            .header("Content-Type", "application/json")
            .timeout(READ_TIMEOUT)
            .GET()
            .build()
        val response = httpClient.send(request, HttpResponse.BodyHandlers.ofString())
        if (response.statusCode() != 200) {
            throw GradleException("Requesting vendor list failed: ${response.body()}")
        }
        return response.body()
    }

    private fun toParameterString(params: Map<String, String>): String {
        return params.entries.joinToString("&") {
            "${URLEncoder.encode(it.key, UTF_8.name())}=${URLEncoder.encode(it.value, UTF_8.name())}"
        }
    }
}
