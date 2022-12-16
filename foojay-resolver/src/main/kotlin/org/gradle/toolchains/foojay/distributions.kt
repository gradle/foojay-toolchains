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

val reversedWellKnownDistributionNames = vendorAliases.values.reversed()

val j9Aliases = mapOf(
    JvmVendorSpec.IBM to "Semeru",
    JvmVendorSpec.IBM_SEMERU to "Semeru",
    JvmVendorSpec.ADOPTOPENJDK to "AOJ OpenJ9",
)

fun match(
    distributions: List<Distribution>,
    vendor: JvmVendorSpec,
    implementation: JvmImplementation,
    version: JavaLanguageVersion
): List<Distribution> = when {
    JvmImplementation.J9 == implementation -> {
        if (vendor == any()) {
            distributions
                .filter { j9Aliases.values.contains(it.name) }
                .sortedBy { j9Aliases.values.indexOf(it.name) }
        } else {
            distributions.filter { it.name == j9Aliases[vendor] }
        }
    }

    JvmVendorSpec.GRAAL_VM == vendor -> match(distributions, JvmVendorSpec.matching("Graal VM CE " + version.asInt()))
    else -> match(distributions, vendor)
}

private fun match(distributions: List<Distribution>, vendor: JvmVendorSpec): List<Distribution> {
    if (vendor == any()) {
        return allDistributionsPrecededByWellKnownOnes(distributions)
    }

    return findExactMatch(distributions, vendor) ?: findAllMatchingDistributions(distributions, vendor)
}

private fun allDistributionsPrecededByWellKnownOnes(distributions: List<Distribution>): List<Distribution> =
    distributions
        .sortedByDescending { reversedWellKnownDistributionNames.indexOf(it.name) }

private fun findExactMatch(distributions: List<Distribution>, vendor: JvmVendorSpec): List<Distribution>? =
    distributions.firstOrNull { it.name == vendorAliases[vendor] }?.let {
        listOf(it)
    }

private fun findAllMatchingDistributions(distributions: List<Distribution>, vendor: JvmVendorSpec) =
    distributions.filter { distribution ->
        vendor.matches(distribution.name) || distribution.synonyms.find { vendor.matches(it) } != null
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
