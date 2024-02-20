package org.gradle.toolchains.foojay

import org.gradle.api.GradleException
import java.io.BufferedReader
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import java.util.concurrent.TimeUnit

internal interface Api {
    fun fetchDistributions(params: Map<String, String>): String
    fun fetchPackages(params: Map<String, String>): String
}

internal class FoojayApi : Api {
    private val CONNECT_TIMEOUT = TimeUnit.SECONDS.toMillis(10).toInt()
    private val READ_TIMEOUT = TimeUnit.SECONDS.toMillis(20).toInt()

    private val SCHEMA = "https"
    private val ENDPOINT_ROOT = "api.foojay.io/disco/v3.0"
    private val DISTRIBUTIONS_ENDPOINT = "$ENDPOINT_ROOT/distributions"
    private val PACKAGES_ENDPOINT = "$ENDPOINT_ROOT/packages"

    override fun fetchDistributions(params: Map<String, String>): String =
        createConnection(DISTRIBUTIONS_ENDPOINT, params).use { readResponse(this) }

    override fun fetchPackages(params: Map<String, String>): String =
        createConnection(PACKAGES_ENDPOINT, params).use { readResponse(this) }

    private fun createConnection(endpoint: String, params: Map<String, String>): HttpURLConnection {
        val url = URL("$SCHEMA://$endpoint?${toParameterString(params)}")
        val con = url.openConnection() as HttpURLConnection
        con.connectTimeout = CONNECT_TIMEOUT
        con.readTimeout = READ_TIMEOUT
        return con
    }

    private fun toParameterString(params: Map<String, String>): String {
        return params.entries.joinToString("&") {
            "${URLEncoder.encode(it.key, StandardCharsets.UTF_8.name())}=${
                URLEncoder.encode(
                    it.value,
                    StandardCharsets.UTF_8.name()
                )
            }"
        }
    }

    private fun readResponse(con: HttpURLConnection): String {
        val status = con.responseCode
        if (status != HttpURLConnection.HTTP_OK) {
            throw GradleException("Requesting vendor list failed: ${readContent(con.errorStream)}")
        }
        return readContent(con.inputStream)
    }

    private fun readContent(stream: InputStream): String = stream.bufferedReader().use(BufferedReader::readText)
}

private fun <T> HttpURLConnection.use(block: HttpURLConnection.() -> T): T {
    val result = block()
    disconnect()
    return result
}