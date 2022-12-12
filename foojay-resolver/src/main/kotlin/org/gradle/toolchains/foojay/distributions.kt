package org.gradle.toolchains.foojay

import com.google.gson.Gson
import org.gradle.jvm.toolchain.JavaLanguageVersion
import org.gradle.jvm.toolchain.JvmImplementation
import org.gradle.jvm.toolchain.JvmVendorSpec
import org.gradle.jvm.toolchain.internal.DefaultJvmVendorSpec.any

val vendorAliases = mapOf(
    any() to "Temurin",
    JvmVendorSpec.AZUL to "Zulu",
    JvmVendorSpec.ADOPTOPENJDK to "AOJ",
    JvmVendorSpec.AMAZON to "Corretto",
    JvmVendorSpec.BELLSOFT to "Liberica",
    JvmVendorSpec.IBM to "Semeru",
    JvmVendorSpec.IBM_SEMERU to "Semeru",
    JvmVendorSpec.ORACLE to "Oracle OpenJDK",
    JvmVendorSpec.SAP to "SAP Machine",
)

val j9Aliases = mapOf(
    JvmVendorSpec.ADOPTOPENJDK to "AOJ OpenJ9",
    JvmVendorSpec.IBM to "Semeru",
    JvmVendorSpec.IBM_SEMERU to "Semeru",
    any() to "Semeru",
)

fun match(
        distributions: List<Distribution>,
        vendor: JvmVendorSpec,
        implementation: JvmImplementation,
        version: JavaLanguageVersion
): Distribution? {
    if (JvmImplementation.J9 == implementation) {
        return distributions.firstOrNull { it.name == j9Aliases[vendor] }
    }
    if (JvmVendorSpec.GRAAL_VM == vendor) {
        return match(distributions, JvmVendorSpec.matching("Graal VM CE " + version.asInt()))
    }
    return match(distributions, vendor)
}

fun match(distributions: List<Distribution>, vendor: JvmVendorSpec): Distribution? {
    val exactMatchByAlias = distributions.firstOrNull { it.name == vendorAliases[vendor] }
    if (exactMatchByAlias != null) return exactMatchByAlias

    return distributions.firstOrNull { distribution ->
        vendor.matches(distribution.name) || distribution.synonyms.find { vendor.matches(it) } != null
    }
}

fun parseDistributions(json: String): List<Distribution> {
    return Gson().fromJson(json, DistributionsResult::class.java).result
}

data class Distribution(
    val name: String,
    val api_parameter: String,
    val synonyms: List<String>,
    val versions: List<String>
)

private data class DistributionsResult(
    val result: List<Distribution>
)
