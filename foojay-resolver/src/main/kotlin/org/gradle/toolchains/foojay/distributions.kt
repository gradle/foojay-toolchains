package org.gradle.toolchains.foojay

import com.google.gson.Gson
import org.gradle.jvm.toolchain.JavaLanguageVersion
import org.gradle.jvm.toolchain.JvmImplementation
import org.gradle.jvm.toolchain.JvmVendorSpec
import org.gradle.jvm.toolchain.internal.DefaultJvmVendorSpec.any

@Suppress("DEPRECATION")
val vendorAliases = mapOf(
    JvmVendorSpec.ADOPTIUM to "Temurin",
    JvmVendorSpec.ADOPTOPENJDK to "AOJ",
    JvmVendorSpec.AMAZON to "Corretto",
    JvmVendorSpec.AZUL to "Zulu",
    JvmVendorSpec.BELLSOFT to "Liberica",
    JvmVendorSpec.IBM to "Semeru",
    JvmVendorSpec.IBM_SEMERU to "Semeru",
    JvmVendorSpec.ORACLE to "Oracle OpenJDK",
    JvmVendorSpec.SAP to "SAP Machine"
)

val distributionOrderOfPreference = listOf("Temurin", "AOJ")

@Suppress("DEPRECATION")
val j9Aliases = mapOf(
    JvmVendorSpec.IBM to "Semeru",
    JvmVendorSpec.IBM_SEMERU to "Semeru",
    JvmVendorSpec.ADOPTOPENJDK to "AOJ OpenJ9"
)

fun match(
    distributions: List<Distribution>,
    vendor: JvmVendorSpec,
    implementation: JvmImplementation,
    version: JavaLanguageVersion
): List<Distribution> = when {
    JvmImplementation.J9 == implementation -> matchForJ9(distributions, vendor)
    JvmVendorSpec.GRAAL_VM == vendor -> match(distributions, JvmVendorSpec.matching("GraalVM CE " + version.asInt()), version)
    else -> match(distributions, vendor, version)
}

private fun matchForJ9(distributions: List<Distribution>, vendor: JvmVendorSpec) =
    if (vendor == any()) {
        distributions
            .filter { it.name in j9Aliases.values }
            .sortedBy { j9Aliases.values.indexOf(it.name) }
    } else {
        distributions.filter { it.name == j9Aliases[vendor] }
    }

private fun match(distributions: List<Distribution>, vendor: JvmVendorSpec, version: JavaLanguageVersion): List<Distribution> {
    if (vendor == any()) {
        return allDistributionsPrecededByWellKnownOnes(distributions, version)
    }

    return findByMatchingAliases(distributions, vendor) ?: findByMatchingNamesAndSynonyms(distributions, vendor)
}

private fun allDistributionsPrecededByWellKnownOnes(distributions: List<Distribution>, version: JavaLanguageVersion): List<Distribution> =
    distributions
        .filter { distribution ->
            when {
                distribution.name.startsWith("GraalVM CE") -> distribution.name == "GraalVM CE " + version.asInt()
                else -> true
            }
        }
        .sortedBy {
            //put our preferences first, preserver Foojay order otherwise
            val indexOf = distributionOrderOfPreference.indexOf(it.name)
            when {
                indexOf < 0 -> distributionOrderOfPreference.size
                else -> indexOf
            }
        }

private fun findByMatchingAliases(distributions: List<Distribution>, vendor: JvmVendorSpec): List<Distribution>? =
    distributions.find { it.name == vendorAliases[vendor] }?.let {
        listOf(it)
    }

private fun findByMatchingNamesAndSynonyms(distributions: List<Distribution>, vendor: JvmVendorSpec) =
    distributions.filter { distribution ->
        vendor.matches(distribution.name) || distribution.synonyms.find { vendor.matches(it) } != null
    }

fun parseDistributions(json: String): List<Distribution> {
    return Gson().fromJson(json, DistributionsResult::class.java).result
}

/**
 * The data class for the result objects as returned by [FoojayApi.DISTRIBUTIONS_ENDPOINT].
 */
data class Distribution(
    /**
     * The distribution (vendor) name, e.g. "Temurin", "Oracle OpenJDK", "JetBrains", "GraalVM", ...
     */
    val name: String,

    /**
     * The name to use as part of the path when requesting distribution-specific details, see
     * https://github.com/foojayio/discoapi#endpoint-distributions
     */
    val api_parameter: String,

    /**
     * A list of alterative names / spellings for the distribution.
     */
    val synonyms: List<String>,

    /**
     * The version strings available for this distribution.
     */
    val versions: List<String>
)

private data class DistributionsResult(
    val result: List<Distribution>
)
