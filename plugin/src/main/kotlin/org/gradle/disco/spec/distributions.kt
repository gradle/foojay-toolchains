package org.gradle.disco.spec

import com.google.gson.Gson
import org.gradle.jvm.toolchain.JvmImplementation
import org.gradle.jvm.toolchain.JvmVendorSpec
import org.gradle.jvm.toolchain.internal.DefaultJvmVendorSpec.any

val vendorAliases = mapOf(
    any() to "Temurin",
    JvmVendorSpec.AZUL to "Zulu",
    JvmVendorSpec.ADOPTOPENJDK to "AOJ",
    JvmVendorSpec.AMAZON to "Corretto",
    JvmVendorSpec.BELLSOFT to "Liberica",
    JvmVendorSpec.IBM_SEMERU to "Semeru",
    JvmVendorSpec.ORACLE to "Oracle",
    JvmVendorSpec.SAP to "SAP Machine",
)

val j9Aliases = mapOf(
    JvmVendorSpec.ADOPTOPENJDK to "AOJ OpenJ9",
    JvmVendorSpec.IBM_SEMERU to "Semeru",
    any() to "Semeru",
)

fun match(distributions: List<Distribution>, vendor: JvmVendorSpec, implementation: JvmImplementation): Distribution? {
    if (JvmImplementation.J9 == implementation) {
        return distributions.firstOrNull { it.name == j9Aliases[vendor] }
    }
    return match(distributions, vendor)
}

fun match(distributions: List<Distribution>, vendor: JvmVendorSpec): Distribution? {
    val exactMathByAlias = distributions.firstOrNull { it.name == vendorAliases[vendor] }
    if (exactMathByAlias != null) return exactMathByAlias

    return distributions.firstOrNull { distribution ->
        vendor.matches(distribution.name) || distribution.synonyms.find { vendor.matches(it) } != null
    }
}

fun parseDistributions(json: String): List<Distribution> {
    return Gson().fromJson(json, DistributionsResult::class.java).result
}

data class Distribution(
    val name: String, val synonyms: List<String>, val versions: List<String>
)

private data class DistributionsResult(
    val result: List<Distribution>
)
