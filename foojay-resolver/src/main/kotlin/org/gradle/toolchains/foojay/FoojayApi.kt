package org.gradle.toolchains.foojay

import org.gradle.api.GradleException
import org.gradle.jvm.toolchain.JavaLanguageVersion
import org.gradle.jvm.toolchain.JvmImplementation
import org.gradle.jvm.toolchain.JvmVendorSpec
import org.gradle.platform.Architecture
import org.gradle.platform.OperatingSystem
import java.io.BufferedReader
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.URI
import java.net.URL
import java.net.URLEncoder
import java.nio.charset.StandardCharsets.UTF_8
import java.util.concurrent.TimeUnit.SECONDS


@Suppress("UnstableApiUsage")
class FoojayApi {

    companion object {
        val CONNECT_TIMEOUT = SECONDS.toMillis(10).toInt()
        val READ_TIMEOUT = SECONDS.toMillis(20).toInt()

        const val SCHEMA = "https"

        private const val ENDPOINT_ROOT = "api.foojay.io/disco/v3.0"
        const val DISTRIBUTIONS_ENDPOINT = "$ENDPOINT_ROOT/distributions"
        const val PACKAGES_ENDPOINT = "$ENDPOINT_ROOT/packages"
    }

    private val distributions = mutableListOf<Distribution>()

    fun toUri(links: Links?): URI? = links?.pkg_download_redirect

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
            val con = createConnection(
                DISTRIBUTIONS_ENDPOINT,
                mapOf("include_versions" to "true", "include_synonyms" to "true")
            )
            val json = readResponse(con)
            con.disconnect()

            distributions.addAll(parseDistributions(json))
        }
    }

    internal fun match(distributionName: String, version: JavaLanguageVersion, operatingSystem: OperatingSystem, architecture: Architecture): Package? {
        val versionApiKey = when {
            distributionName.startsWith("graalvm_community") -> "version"
            distributionName == "graalvm" -> "version"
            else -> "jdk_version"
        }

        val con = createConnection(
            PACKAGES_ENDPOINT,
            mapOf(
                versionApiKey to "$version",
                "distro" to distributionName,
                "operating_system" to operatingSystem.toApiValue(),
                "latest" to "available",
                "directly_downloadable" to "true"
            )
        )
        val json = readResponse(con)
        con.disconnect()

        val packages = parsePackages(json)
        return match(packages, architecture)
    }

    private fun createConnection(endpoint: String, parameters: Map<String, String>): HttpURLConnection {
        val url = URL("$SCHEMA://$endpoint?${toParameterString(parameters)}")
        val con = url.openConnection() as HttpURLConnection
        con.setRequestProperty("Content-Type", "application/json")
        con.requestMethod = "GET"
        con.connectTimeout = CONNECT_TIMEOUT
        con.readTimeout = READ_TIMEOUT
        return con
    }

    private fun toParameterString(params: Map<String, String>): String {
        return params.entries.joinToString("&") {
            "${URLEncoder.encode(it.key, UTF_8.name())}=${URLEncoder.encode(it.value, UTF_8.name())}"
        }
    }

    private fun readResponse(con: HttpURLConnection): String {
        val status = con.responseCode
        if (status != HttpURLConnection.HTTP_OK) {
            throw GradleException("Requesting vendor list failed: ${readContent(con.errorStream)}")
        }
        return readContent(con.inputStream)
    }

    private fun readContent(stream: InputStream) = stream.bufferedReader().use(BufferedReader::readText)
}
