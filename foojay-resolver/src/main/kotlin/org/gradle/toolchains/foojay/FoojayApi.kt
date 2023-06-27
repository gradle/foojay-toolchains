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


class FoojayApi {

    val CONNECT_TIMEOUT = SECONDS.toMillis(10).toInt()
    val READ_TIMEOUT = SECONDS.toMillis(20).toInt()

    val SCHEMA = "https"

    val ENDPOINT_ROOT = "api.foojay.io/disco/v3.0"
    val DISTRIBUTIONS_ENDPOINT = "$ENDPOINT_ROOT/distributions"
    val PACKAGES_ENDPOINT = "$ENDPOINT_ROOT/packages"

    val distributions = mutableListOf<Distribution>()

    fun toUri(links: Links?): URI? = links?.pkg_download_redirect

    fun toLinks(
            version: JavaLanguageVersion,
            vendor: JvmVendorSpec,
            implementation: JvmImplementation,
            operatingSystem: OperatingSystem,
            architecture: Architecture
    ): Links? {
        val distributions = match(vendor, implementation, version)
        return distributions.asSequence().mapNotNull { distribution ->
            val downloadPackage = match(distribution.api_parameter, version, operatingSystem, architecture)
            downloadPackage?.links
        }.firstOrNull()
    }

    internal fun match(vendor: JvmVendorSpec, implementation: JvmImplementation, version: JavaLanguageVersion): List<Distribution> {
        fetchDistributionsIfMissing()
        return match(distributions, vendor, implementation, version)
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
        // Old GraalVM releases are special in that the JVM version they target is part of the distribution name instead
        // of the release version. For these releases "jdk_version" instead of "version" must be used as they key.
        val versionApiKey = when {
            distributionName.startsWith("graalvm_ce") -> "jdk_version"
            else -> "version"
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
        val result = StringBuilder()
        for (param in params) {
            result.append(URLEncoder.encode(param.key, UTF_8.name()))
            result.append("=")
            result.append(URLEncoder.encode(param.value, UTF_8.name()))
            result.append("&")
        }
        if (params.isNotEmpty()) result.delete(result.length - 1, result.length)
        return result.toString()
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
