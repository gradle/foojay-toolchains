@file:Suppress("UnstableApiUsage")

package org.gradle.toolchains.foojay

import com.google.gson.Gson
import org.gradle.jvm.toolchain.JavaLanguageVersion
import org.gradle.jvm.toolchain.JvmImplementation
import org.gradle.jvm.toolchain.JvmVendorSpec
import org.gradle.jvm.toolchain.internal.DefaultJvmVendorSpec.any
import java.net.URI

val vendorAliases = createVendorAliases()

val distributionOrderOfPreference = listOf("Temurin", "AOJ")

val j9Aliases = createJ9AliasesMap()

private fun createVendorAliases(): Map<JvmVendorSpec?, String> {
    val tmpMap = mutableMapOf(
        JvmVendorSpec.ADOPTIUM to "Temurin",
        JvmVendorSpec.ADOPTOPENJDK to "AOJ",
        JvmVendorSpec.AMAZON to "Corretto",
        JvmVendorSpec.AZUL to "Zulu",
        JvmVendorSpec.BELLSOFT to "Liberica",
        JvmVendorSpec.IBM to "Semeru",
        JvmVendorSpec.ORACLE to "Oracle OpenJDK",
        JvmVendorSpec.SAP to "SAP Machine"
    )
    try {
        val ibmSemeru = JvmVendorSpec::class.java.getDeclaredField("IBM_SEMERU")
        tmpMap.put(ibmSemeru.get(null) as JvmVendorSpec?, "Semeru")
    } catch (_: NoSuchFieldException) {
        // Ignore - removed in Gradle 9
    }
    return tmpMap.toMap()
}

private fun createJ9AliasesMap(): Map<JvmVendorSpec?, String> {
    val tmpMap = mutableMapOf(
        JvmVendorSpec.IBM to "Semeru",
        JvmVendorSpec.ADOPTOPENJDK to "AOJ OpenJ9"
    )
    try {
        val ibmSemeru = JvmVendorSpec::class.java.getDeclaredField("IBM_SEMERU")
        tmpMap.put(ibmSemeru.get(null) as JvmVendorSpec?, "Semeru")
    } catch (_: NoSuchFieldException) {
        // Ignore - removed in Gradle 9
    }
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
