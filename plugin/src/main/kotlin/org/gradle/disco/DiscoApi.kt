package org.gradle.disco

import org.gradle.api.GradleException
import org.gradle.disco.spec.matchingDistribution
import org.gradle.disco.spec.parseDistributions
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

    fun resolveVendor(vendor: JvmVendorSpec): String? {
        val con = createConnection(DISTRIBUTIONS_ENDPOIT, mapOf("include_versions" to "false", "include_synonyms" to "true"))
        val json = readResponse(con)
        con.disconnect()

        val distributions = parseDistributions(json) //todo: should we cash the result for further calls? if so, invalidate when?
        return matchingDistribution(distributions, vendor)?.name
    }

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