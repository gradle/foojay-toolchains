@file:Suppress("UnstableApiUsage")

package org.gradle.toolchains.foojay

import com.google.gson.Gson
import org.gradle.jvm.toolchain.JavaLanguageVersion
import org.gradle.jvm.toolchain.JvmImplementation
import org.gradle.jvm.toolchain.JvmVendorSpec
import org.gradle.jvm.toolchain.JvmVendorSpec.ADOPTIUM
import org.gradle.jvm.toolchain.JvmVendorSpec.ADOPTOPENJDK
import org.gradle.jvm.toolchain.JvmVendorSpec.AMAZON
import org.gradle.jvm.toolchain.JvmVendorSpec.AZUL
import org.gradle.jvm.toolchain.JvmVendorSpec.BELLSOFT
import org.gradle.jvm.toolchain.JvmVendorSpec.IBM
import org.gradle.jvm.toolchain.JvmVendorSpec.ORACLE
import org.gradle.jvm.toolchain.JvmVendorSpec.SAP
import org.gradle.jvm.toolchain.internal.DefaultJvmVendorSpec.any
import java.net.URI

val vendorAliases: Map<JvmVendorSpec, String>
    get() {
        val tmpMap = mutableMapOf(
            ADOPTIUM to "Temurin",
            ADOPTOPENJDK to "AOJ",
            AMAZON to "Corretto",
            AZUL to "Zulu",
            BELLSOFT to "Liberica",
            IBM to "Semeru",
            ORACLE to "Oracle OpenJDK",
            SAP to "SAP Machine"
        )
        addPossiblyUndefinedVendor("IBM_SEMERU", "Semeru", tmpMap)
        return tmpMap.toMap()
    }

val distributionOrderOfPreference = listOf("Temurin", "AOJ")

val j9Aliases: Map<JvmVendorSpec, String>
    get() {
        val tmpMap = mutableMapOf(
            IBM to "Semeru",
            ADOPTOPENJDK to "AOJ OpenJ9"
        )
        addPossiblyUndefinedVendor("IBM_SEMERU", "Semeru", tmpMap)
        return tmpMap.toMap()
    }

/**
 * Given a list of [distributions], return those that match the provided [vendor] and JVM [implementation]. The Java
 * language [version] is only used to remove wrong GraalVM distributions; no general version filtering is done here.
 */
@Suppress("ReturnCount")
fun match(
    distributions: List<Distribution>,
    vendor: JvmVendorSpec,
    implementation: JvmImplementation,
    version: JavaLanguageVersion,
    nativeImageCapable: Boolean
): List<Distribution> {
    // Start by filtering based on the native image criteria.
    // If it is defined, we only keep the distributions that have `build_of_graalvm` set to true.
    val filteredDistributions = distributions.filter { !nativeImageCapable || it.build_of_graalvm }

    // Specific filter when J9 is requested
    if (implementation == JvmImplementation.J9) return matchForJ9(filteredDistributions, vendor)

    // Return early if an explicit non-GraalVM distribution is requested.
    if (vendor != JvmVendorSpec.GRAAL_VM && vendor != any()) return match(filteredDistributions, vendor)

    // Remove GraalVM distributions that target the wrong Java language version.
    val graalVmCeVendor = JvmVendorSpec.matching("GraalVM CE $version")
    val distributionsWithoutWrongGraalVm = filteredDistributions.filter { (name) ->
        when {
            // Naming scheme for old GraalVM community releases: The Java language version is part of the name.
            name.startsWith("GraalVM CE") -> graalVmCeVendor.matches(name)

            else -> true
        }
    }

    if (vendor == any()) return allDistributionsPrecededByWellKnownOnes(distributionsWithoutWrongGraalVm)

    // As Gradle has no means to distinguish between Community and Oracle distributions of GraalVM (see
    // https://github.com/gradle/gradle/issues/25521), disregard Oracle GraalVM distributions for now by only matching
    // "GraalVM Community" and "GraalVM CE".
    val graalVmVendor = JvmVendorSpec.matching("GraalVM C")

    return match(distributionsWithoutWrongGraalVm, graalVmVendor)
}

private fun matchForJ9(distributions: List<Distribution>, vendor: JvmVendorSpec) =
    if (vendor == any()) {
        distributions
            .filter { it.name in j9Aliases.values }
            .sortedBy { j9Aliases.values.indexOf(it.name) }
    } else {
        distributions.filter { it.name == j9Aliases[vendor] }
    }

private fun match(distributions: List<Distribution>, vendor: JvmVendorSpec): List<Distribution> =
    findByMatchingAliases(distributions, vendor) ?: findByMatchingNamesAndSynonyms(distributions, vendor)

private fun allDistributionsPrecededByWellKnownOnes(distributions: List<Distribution>): List<Distribution> =
    distributions.sortedBy {
        // Put our preferences first, preserve Foojay order otherwise.
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
        vendor.matches(distribution.name) || distribution.synonyms.any { vendor.matches(it) }
    }

fun parseDistributions(json: String): List<Distribution> {
    return Gson().fromJson(json, DistributionsResult::class.java).result
}


private fun addPossiblyUndefinedVendor(
    vendorFieldName: String,
    vendorAlias: String,
    map: MutableMap<JvmVendorSpec, String>
) {
    try {
        val vendorField = JvmVendorSpec::class.java.getDeclaredField(vendorFieldName)
        map.put(vendorField.get(null) as JvmVendorSpec, vendorAlias)
    } catch (_: Exception) {
        // Ignore - removed in the Gradle version currently running the build where the plugin is applied.
    }
}

/**
 * The data class for the result objects as returned by [FoojayApi.DISTRIBUTIONS_ENDPOINT].
 */
@Suppress("ConstructorParameterNaming")
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
     * A flag to indicate whether the distribution is still maintained or not.
     */
    val maintained: Boolean,

    /**
     * A flag to indicate whether this is an OpenJDK (re-)distribution.
     */
    val build_of_openjdk: Boolean,

    /**
     * A flag to indicate whether this is a GraalVM (re-)distribution.
     */
    val build_of_graalvm: Boolean,

    /**
     * The URI of the offical homepage of the ditribution.
     */
    val official_uri: URI,

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
