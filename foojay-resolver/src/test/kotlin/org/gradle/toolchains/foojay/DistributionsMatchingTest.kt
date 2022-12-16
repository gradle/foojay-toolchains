package org.gradle.toolchains.foojay

import org.gradle.jvm.toolchain.JavaLanguageVersion
import org.gradle.jvm.toolchain.JvmImplementation
import org.gradle.jvm.toolchain.JvmImplementation.J9
import org.gradle.jvm.toolchain.JvmImplementation.VENDOR_SPECIFIC
import org.gradle.jvm.toolchain.JvmVendorSpec
import org.gradle.jvm.toolchain.JvmVendorSpec.*
import org.gradle.jvm.toolchain.internal.DefaultJvmVendorSpec
import org.gradle.jvm.toolchain.internal.DefaultJvmVendorSpec.any
import kotlin.test.assertEquals
import kotlin.test.Test

class DistributionsMatchingTest {

    private val SEMERU = Distribution("Semeru", "", listOf(), listOf())
    private val OPENJ9 = Distribution("AOJ OpenJ9", "", listOf(), listOf())
    private val GRAALVM = Distribution("Graal VM CE 17", "", listOf(), listOf())
    private val GRAALVM_SYNONYM = Distribution("Some non Graal matching name", "", listOf("Graal VM CE 17"), listOf())
    private val TEMURIN = Distribution("Temurin", "", listOf(), listOf())
    private val TEMURIN_SYNONYM = Distribution("Some not matching name", "", listOf("Temurin"), listOf())
    private val SAP = Distribution("SAP Machine", "", listOf(), listOf())

    @Test
    fun `matches J9 vendor distribution by alias`() {
        val distributionToMatch = SEMERU
        val distributionToNotMatch = OPENJ9
        val distributions = listOf(distributionToMatch, distributionToNotMatch)
        val actualMatch = match(distributions, IBM, J9, JavaLanguageVersion.of(17))
        val expectedMatch = listOf(distributionToMatch)

        assertEquals(expectedMatch, actualMatch)
    }

    @Test
    fun `matches GraalVM vendor distribution by name`() {
        val distributionToMatch = GRAALVM
        val distributionToNotMatch = OPENJ9
        val distributions = listOf(distributionToMatch, distributionToNotMatch)
        val actualMatch = match(distributions, GRAAL_VM, VENDOR_SPECIFIC, JavaLanguageVersion.of(17))
        val expectedMatch = listOf(distributionToMatch)

        assertEquals(expectedMatch, actualMatch)
    }

    @Test
    fun `matches GraalVM vendor distribution by synonym`() {
        val distributionToMatch = GRAALVM_SYNONYM
        val distributionToNotMatch = OPENJ9
        val distributions = listOf(distributionToMatch, distributionToNotMatch)
        val actualMatch = match(distributions, GRAAL_VM, VENDOR_SPECIFIC, JavaLanguageVersion.of(17))
        val expectedMatch = listOf(distributionToMatch)

        assertEquals(expectedMatch, actualMatch)
    }


    @Test
    fun `matches particular vendor distribution by name`() {
        val distributionToMatch = TEMURIN
        val distributionToNotMatch = SAP
        val distributions = listOf(distributionToMatch, distributionToNotMatch)
        val actualMatch = match(distributions, ADOPTIUM, VENDOR_SPECIFIC, JavaLanguageVersion.of(17))
        val expectedMatch = listOf(distributionToMatch)

        assertEquals(expectedMatch, actualMatch)
    }

    @Test
    fun `matches particular vendor distribution by synonym`() {
        val distributionToMatch = TEMURIN_SYNONYM
        val distributionToNotMatch = SAP
        val distributions = listOf(distributionToMatch, distributionToNotMatch)
        val actualMatch = match(distributions, ADOPTIUM, VENDOR_SPECIFIC, JavaLanguageVersion.of(17))
        val expectedMatch = listOf(distributionToMatch)

        assertEquals(expectedMatch, actualMatch)
    }

    @Test
    fun `matches any vendor distribution by alias`() {
        val firstDistributionToMatch = TEMURIN
        val secondDistributionToMatch = SAP
        val distributionToNotMatch = OPENJ9
        val distributions = listOf(firstDistributionToMatch, secondDistributionToMatch, distributionToNotMatch)
        val actualMatch = match(distributions, any(), VENDOR_SPECIFIC, JavaLanguageVersion.of(17))
        val expectedMatch = listOf(firstDistributionToMatch, secondDistributionToMatch)

        assertEquals(expectedMatch, actualMatch)
    }

}