package org.gradle.disco.spec

import com.google.gson.Gson
import org.gradle.jvm.toolchain.JvmVendorSpec

val aliases = mapOf(
    JvmVendorSpec.AZUL to "Zulu",
    JvmVendorSpec.ADOPTOPENJDK to "AOJ",
    JvmVendorSpec.AMAZON to "Corretto",
    JvmVendorSpec.BELLSOFT to "Liberica",
    JvmVendorSpec.IBM_SEMERU to "Semeru",
    JvmVendorSpec.ORACLE to "Oracle",
    JvmVendorSpec.SAP to "SAP Machine",
)

fun matchingDistribution(
    distributions: List<Distribution>, vendor: JvmVendorSpec
): Distribution? {
    val exactMathByAlias = distributions.firstOrNull { it.name == aliases[vendor] }
    if (exactMathByAlias != null) return exactMathByAlias;

    return distributions.firstOrNull { distribution ->
        vendor.matches(distribution.name) || distribution.synonyms.find { vendor.matches(it) } != null
    }
}


fun parseDistributions(json: String): List<Distribution> {
    return Gson().fromJson(json, DistributionResult::class.java).result
}

data class Distribution(
    val name: String, val synonyms: List<String>
)

private data class DistributionResult(
    val result: List<Distribution>
)
