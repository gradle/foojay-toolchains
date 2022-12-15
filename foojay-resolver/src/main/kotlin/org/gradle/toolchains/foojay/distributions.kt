package org.gradle.toolchains.foojay

import com.google.gson.Gson
import org.gradle.jvm.toolchain.JavaLanguageVersion
import org.gradle.jvm.toolchain.JvmImplementation
import org.gradle.jvm.toolchain.JvmVendorSpec
import org.gradle.jvm.toolchain.internal.DefaultJvmVendorSpec.any

val vendorAliases = mapOf(
    JvmVendorSpec.ADOPTIUM to "Temurin",
    JvmVendorSpec.ADOPTOPENJDK to "AOJ",
    JvmVendorSpec.AMAZON to "Corretto",
    JvmVendorSpec.AZUL to "Zulu",
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
): List<Distribution> {
    if (JvmImplementation.J9 == implementation) {
        return distributions.filter { it.name == j9Aliases[vendor] }
    }
    if (JvmVendorSpec.GRAAL_VM == vendor) {
        return match(distributions, JvmVendorSpec.matching("Graal VM CE " + version.asInt()))
    }
    return match(distributions, vendor)
}

fun match(distributions: List<Distribution>, vendor: JvmVendorSpec): List<Distribution> {
    val aliases = vendorAliases
    if (vendor == any()) {
        return distributions
            .filter { it.name in aliases.values }
            .sortedBy { aliases.values.indexOf(it.name) }
    }

    val exactMatchByAlias = distributions.firstOrNull { it.name == aliases[vendor] }
    if (exactMatchByAlias != null) return listOf(exactMatchByAlias)

    return distributions.filter { distribution ->
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
