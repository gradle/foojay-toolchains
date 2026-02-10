package org.gradle.toolchains.foojay

import org.gradle.jvm.toolchain.JavaLanguageVersion.of
import org.gradle.jvm.toolchain.JvmImplementation
import org.gradle.jvm.toolchain.JvmImplementation.J9
import org.gradle.jvm.toolchain.JvmImplementation.VENDOR_SPECIFIC
import org.gradle.jvm.toolchain.JvmVendorSpec
import org.gradle.jvm.toolchain.JvmVendorSpec.*
import org.gradle.jvm.toolchain.internal.DefaultJvmVendorSpec.any
import org.gradle.platform.Architecture
import org.gradle.platform.OperatingSystem
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import org.junit.jupiter.params.provider.ValueSource
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

@Suppress("UnstableApiUsage")
class FoojayApiTest {

    private val api = FoojayApi()

    @ParameterizedTest(name = "javaVersion: {0}, vendor: {1}, isJ9: {2}, os: {3}, arch: {4}")
    @MethodSource("getData")
    fun `download URI provided correctly`(
        javaVersion: Int,
        vendor: JvmVendorSpec,
        isJ9: Boolean,
        os: OperatingSystem,
        arch: Architecture
    ) = assertDownloadUri(javaVersion, vendor, isJ9, false, os, arch)

    companion object {

        private
        val IBM_SEMERU_VENDOR = try {
            JvmVendorSpec::class.java.getDeclaredField("IBM_SEMERU").get(null) as JvmVendorSpec
        } catch (_: Exception) {
            null
        }

        @Suppress("DEPRECATION")
        @JvmStatic
        fun getData(): List<Arguments> {
            val tmpList = mutableListOf(
                Arguments.of(8, any(), false, OperatingSystem.MAC_OS, Architecture.X86_64),
                Arguments.of(8, any(), false, OperatingSystem.MAC_OS, Architecture.AARCH64),
                Arguments.of(17, any(), false, OperatingSystem.MAC_OS, Architecture.X86_64),
                Arguments.of(17, any(), false, OperatingSystem.MAC_OS, Architecture.AARCH64),
                Arguments.of(17, ADOPTIUM, false, OperatingSystem.MAC_OS, Architecture.AARCH64),
                Arguments.of(17, GRAAL_VM, false, OperatingSystem.MAC_OS, Architecture.AARCH64),
                Arguments.of(21, any(), true, OperatingSystem.MAC_OS, Architecture.X86_64),
                Arguments.of(21, IBM, true, OperatingSystem.MAC_OS, Architecture.X86_64),

                Arguments.of(17, GRAAL_VM, false, OperatingSystem.LINUX, Architecture.X86_64),
                Arguments.of(17, any(), false, OperatingSystem.LINUX, Architecture.X86_64),
                Arguments.of(17, any(), true, OperatingSystem.LINUX, Architecture.X86_64),
                Arguments.of(21, GRAAL_VM, false, OperatingSystem.LINUX, Architecture.X86_64),

                Arguments.of(8, any(), false, OperatingSystem.WINDOWS, Architecture.X86_64),
                Arguments.of(8, GRAAL_VM, false, OperatingSystem.WINDOWS, Architecture.X86_64),
                Arguments.of(17, any(), false, OperatingSystem.WINDOWS, Architecture.X86_64),
                Arguments.of(17, GRAAL_VM, false, OperatingSystem.WINDOWS, Architecture.X86_64),
            )
            IBM_SEMERU_VENDOR?.let { tmpList.add(Arguments.of(21, it, true, OperatingSystem.MAC_OS, Architecture.X86_64)) }
            return tmpList.toList()
        }
    }

    @ParameterizedTest(name = "J9 implementation influences vendor resolution (Java {0})")
    @ValueSource(ints = [8, 11, 16])
    fun `J9 implementation influences vendor resolution`(version: Int) {
        assertMatchedDistributions(any(), J9, version, "Semeru", "AOJ OpenJ9")

        assertMatchedDistributions(ADOPTOPENJDK, J9, version, "AOJ OpenJ9")
        assertMatchedDistributions(IBM, J9, version, "Semeru")
        IBM_SEMERU_VENDOR?.let { assertMatchedDistributions(it, J9, version, "Semeru") }

        assertMatchedDistributions(ADOPTIUM, J9, version)
        assertMatchedDistributions(AZUL, J9, version)
        assertMatchedDistributions(AMAZON, J9, version)
        assertMatchedDistributions(BELLSOFT, J9, version)
        assertMatchedDistributions(MICROSOFT, J9, version)
        assertMatchedDistributions(ORACLE, J9, version)
        assertMatchedDistributions(SAP, J9, version)
        assertMatchedDistributions(APPLE, J9, version)
        assertMatchedDistributions(GRAAL_VM, J9, version)
        assertMatchedDistributions(HEWLETT_PACKARD, J9, version)
    }

    @ParameterizedTest(name = "vendor specific implementation does not influence vendor resolution (Java {0})")
    @ValueSource(ints = [8, 11, 16])
    fun `vendor specific implementation does not influence vendor resolution`(version: Int) {
        assertMatchedDistributions(any(), VENDOR_SPECIFIC, version,
                "Temurin", "AOJ",
                "Zulu", "Trava", "Semeru certified", "Semeru", "SAP Machine", "Red Hat", "Oracle OpenJDK",
                "Oracle", "OpenLogic", "OJDKBuild", "Microsoft", "Mandrel", "Liberica Native", "Liberica", "Kona",
                "JetBrains", "GraalVM Community", "GraalVM CE $version", "GraalVM", "Gluon GraalVM", "Dragonwell",
                "Corretto", "Bi Sheng", "AOJ OpenJ9"
        )

        assertMatchedDistributions(ADOPTOPENJDK, VENDOR_SPECIFIC, version, "AOJ")
        assertMatchedDistributions(IBM, VENDOR_SPECIFIC, version, "Semeru")
        IBM_SEMERU_VENDOR?.let { assertMatchedDistributions(it, VENDOR_SPECIFIC, version, "Semeru") }

        assertMatchedDistributions(ADOPTIUM, VENDOR_SPECIFIC, version, "Temurin")
        assertMatchedDistributions(AZUL, VENDOR_SPECIFIC, version, "Zulu")
        assertMatchedDistributions(AMAZON, VENDOR_SPECIFIC, version, "Corretto")
        assertMatchedDistributions(BELLSOFT, VENDOR_SPECIFIC, version, "Liberica")
        assertMatchedDistributions(MICROSOFT, VENDOR_SPECIFIC, version, "Microsoft")
        assertMatchedDistributions(ORACLE, VENDOR_SPECIFIC, version, "Oracle OpenJDK")
        assertMatchedDistributions(SAP, VENDOR_SPECIFIC, version, "SAP Machine")

        assertMatchedDistributions(GRAAL_VM, VENDOR_SPECIFIC, version, "GraalVM Community", "GraalVM CE $version")

        assertMatchedDistributions(APPLE, VENDOR_SPECIFIC, version)
        assertMatchedDistributions(HEWLETT_PACKARD, VENDOR_SPECIFIC, version)
    }

    private fun assertMatchedDistributions(
        vendor: JvmVendorSpec,
        implementation: JvmImplementation,
        version: Int,
        vararg expectedDistributions: String
    ) {
        assertEquals(
                listOf(*expectedDistributions),
                api.match(vendor, implementation, of(version), false).map { it.name },
                "Mismatch in matching distributions for vendor: $vendor, implementation: $implementation, version: $version"
        )
    }

    @ParameterizedTest(name = "can resolve arbitrary vendors (Java {0})")
    @ValueSource(ints = [8, 11, 16])
    fun `can resolve arbitrary vendors`(version: Int) {
        assertEquals("Liberica Native", api.match(vendorSpec("liberica native"), VENDOR_SPECIFIC, of(version), false).firstOrNull()?.name)
        assertEquals("Liberica Native", api.match(vendorSpec("lIbeRica nAtive"), VENDOR_SPECIFIC, of(version), false).firstOrNull()?.name)
        assertEquals("JetBrains", api.match(vendorSpec("JetBrains"), VENDOR_SPECIFIC, of(version), false).firstOrNull()?.name)
    }

    @Test
    fun `can pick the right package`() {
        val p = api.match("temurin", of(11), OperatingSystem.LINUX, Architecture.X86_64)
        assertNotNull(p)
        assertEquals("tar.gz", p.archive_type)
        assertEquals("temurin", p.distribution)
        assertEquals(11, p.jdk_version)
        assertTrue(Regex("11.\\d+.\\d+").matches(p.distribution_version))
        assertEquals("linux", p.operating_system)
        assertEquals("x64", p.architecture)
        assertEquals("jdk", p.package_type)
    }

    @Test
    fun `macos arm is mapped to x64 when arm isn't available`() {
        // RISC architecture is preferred when available
        val p1 = assertDownloadUri(17, AZUL, false, false,OperatingSystem.MAC_OS, Architecture.AARCH64)
        assertEquals("aarch64", p1.architecture)

        // X86 architecture is provided when RISC is not available
        val p2 = assertDownloadUri(7, AZUL, false, false, OperatingSystem.MAC_OS, Architecture.AARCH64)
        assertEquals("x64", p2.architecture)
    }

    @Test
    fun `can pick a native image capable package`() {
        assertDownloadUri(21, any(), false, true, OperatingSystem.MAC_OS, Architecture.AARCH64)
    }

    @Test
    fun `can pick graalvm package`() {
        assertDownloadUri(21, matching("GraalVM"), false, true, OperatingSystem.MAC_OS, Architecture.AARCH64)
    }

    @Suppress("LongParameterList")
    private fun assertDownloadUri(
            javaVersion: Int,
            vendor: JvmVendorSpec,
            isJ9: Boolean,
            nativeImageCapable: Boolean,
            os: OperatingSystem,
            arch: Architecture
    ): Package {
        val actual = api.toPackage(
            of(javaVersion),
            vendor,
            if (isJ9) J9 else VENDOR_SPECIFIC,
            nativeImageCapable,
            os,
            arch
        )
        assertNotNull(actual)
        assertNotNull(actual.links.pkg_download_redirect)
        assertJavaVersion(javaVersion, actual)
        assertDistribution(vendor, actual)
        assertNativeImageCapable(nativeImageCapable, vendor, actual)
        assertOperatingSystem(os, actual)
        assertArchitecture(os, arch, actual)
        return actual
    }

    private fun assertJavaVersion(javaVersion: Int, actual: Package) {
        val actualValue = actual.jdk_version
        assertEquals(javaVersion, actualValue,
            "Expected Java version ($javaVersion) doesn't match actual one ($actualValue),  ${moreDetailsAt(actual)}"
        )
    }

    private fun assertDistribution(vendor: JvmVendorSpec, actual: Package) {
        var expectedValue = vendor.toString().replace("_", "").lowercase()
        expectedValue = when (expectedValue) {
            "ibm" -> "semeru"
            "azul zulu" -> "zulu"
            else -> expectedValue
        }

        val actualValue = actual.distribution

        assertTrue(vendor.matches(actualValue) || actualValue.startsWith(expectedValue),
            "Expected vendor spec ($expectedValue) doesn't match actual distribution (${actualValue}), ${moreDetailsAt(actual)}"
        )
    }

    private fun assertNativeImageCapable(nativeImageCapable: Boolean, vendor: JvmVendorSpec, actual: Package) {
        if (nativeImageCapable) {
            // TODO this is not a great test, but the package does not carry the native-image / Graal capability information anymore
            if (vendor == any()) {
                assertTrue(actual.distribution.contains("mandrel"),
                    "Expected vendor to contain 'mandrel' when native image capable, got ${actual.distribution}")
            } else {
                assertDistribution(vendor, actual)
            }
        }
    }

    private fun assertOperatingSystem(os: OperatingSystem, actual: Package) {
        val expectedValue = os.toString().replace("_", "").lowercase()
        val actualValue = actual.operating_system
        assertEquals(expectedValue, actualValue,
            "Expected operating system ($expectedValue) doesn't match actual one ($actualValue),  ${moreDetailsAt(actual)}"
        )
    }

    private fun assertArchitecture(os: OperatingSystem, arch: Architecture, actual: Package) {
        val expectedValues = when (arch) {
            Architecture.X86 -> architectures32Bit
            Architecture.X86_64 -> architectures64Bit
            Architecture.AARCH64 ->
                if (os == OperatingSystem.MAC_OS) architecturesArm64Bit + architectures64Bit
                else architecturesArm64Bit
        }
        val actualValue = actual.architecture
        assertTrue(expectedValues.contains(actualValue),
            "Expected architecture (${arch}) doesn't match actual one ($actualValue),  ${moreDetailsAt(actual)}"
        )
    }

    private fun moreDetailsAt(actual: Package?) = "for more details see ${actual?.links?.pkg_info_uri}"

    private fun vendorSpec(vendorName: String): JvmVendorSpec = matching(vendorName)

}

