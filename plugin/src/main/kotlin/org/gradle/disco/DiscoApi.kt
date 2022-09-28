package org.gradle.disco

import org.gradle.api.GradleException
import org.gradle.disco.spec.Distribution
import org.gradle.disco.spec.match
import org.gradle.disco.spec.parseDistributions
import org.gradle.jvm.toolchain.JvmImplementation
import org.gradle.jvm.toolchain.JvmVendorSpec
import java.io.BufferedReader
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.nio.charset.StandardCharsets.UTF_8


class DiscoApi {

    val TIMEOUT = 5000 //todo: what value to use? add retries, but how many?

    val SCHEMA = "https"

    val ENDPOINT_ROOT = "api.foojay.io/disco/v3.0"

    val DISTRIBUTIONS_ENDPOIT = "$ENDPOINT_ROOT/distributions"

    val distributions = mutableListOf<Distribution>()

    fun match(vendor: JvmVendorSpec, implementation: JvmImplementation): Distribution? {
        fetchDistributionsIfMissing()
        return match(distributions, vendor, implementation)
    }

    private fun fetchDistributionsIfMissing() {
        if (distributions.isEmpty()) {
            val con = createConnection(
                DISTRIBUTIONS_ENDPOIT,
                mapOf("include_versions" to "true", "include_synonyms" to "true")
            )
            val json = readResponse(con)
            con.disconnect()

            distributions.addAll(parseDistributions(json))
        }
    }

    //todo: sort on package type, prefer jdk
    //todo: sort on archive type, filter out what we don't handle

    private fun createConnection(endpoint: String, parameters: Map<String, String>): HttpURLConnection {
        val url = URL("$SCHEMA://$endpoint?${toParameterString(parameters)}")
        val con = url.openConnection() as HttpURLConnection
        con.setRequestProperty("Content-Type", "application/json");
        con.requestMethod = "GET"
        con.connectTimeout = TIMEOUT;
        con.readTimeout = TIMEOUT;
        return con
    }

    private fun toParameterString(params: Map<String, String>): String {
        val result = StringBuilder()
        for (param in params) {
            result.append(URLEncoder.encode(param.key, UTF_8))
            result.append("=")
            result.append(URLEncoder.encode(param.value, UTF_8))
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