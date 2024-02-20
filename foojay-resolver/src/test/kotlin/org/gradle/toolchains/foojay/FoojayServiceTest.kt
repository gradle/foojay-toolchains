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
import org.junit.jupiter.params.provider.ValueSource
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class FoojayServiceTest {

    private val service = FoojayService(FoojayApi())

    @Test
    fun `download URI provided correctly`() {
        assertDownloadUri(
            javaVersion = 8,
            vendor = any(),
            isJ9 = false,
            os = OperatingSystem.MAC_OS,
            arch = Architecture.AARCH64
        ) // zulu8.X.X.X-ca-fx-jdk8.X.XXX-macosx_aarch64.tar.gz

        assertDownloadUri(
            javaVersion = 11,
            vendor = ADOPTIUM,
            isJ9 = false,
            os = OperatingSystem.MAC_OS,
            arch = Architecture.AARCH64
        ) // OpenJDK11U-jdk_aarch64_mac_hotspot_11.X.XX.tar.gz

        assertDownloadUri(
            javaVersion = 11,
            vendor = GRAAL_VM,
            isJ9 = false,
            os = OperatingSystem.MAC_OS,
            arch = Architecture.AARCH64
        ) // graalvm-ce-java11-darwin-aarch64-22.X.X.tar.gz

        assertDownloadUri(
            javaVersion = 16,
            vendor = any(),
            isJ9 = true,
            os = OperatingSystem.MAC_OS,
            arch = Architecture.X86_64
        ) // ibm-semeru-open-jdk_x64_mac_16.X.X_openj9-0.27.0.tar.gz

        assertDownloadUri(
            javaVersion = 16,
            vendor = IBM,
            isJ9 = true,
            os = OperatingSystem.MAC_OS,
            arch = Architecture.X86_64
        ) // ibm-semeru-open-jdk_x64_mac_16.X.X_openj9-0.27.0.tar.gz

        @Suppress("DEPRECATION")
        assertDownloadUri(
            javaVersion = 16,
            vendor = IBM_SEMERU,
            isJ9 = true,
            os = OperatingSystem.MAC_OS,
            arch = Architecture.X86_64
        ) // ibm-semeru-open-jdk_x64_mac_16.X.X_openj9-0.27.0.tar.gz

        assertDownloadUri(
            javaVersion = 16,
            vendor = GRAAL_VM,
            isJ9 = false,
            os = OperatingSystem.LINUX,
            arch = Architecture.X86_64
        ) // graalvm-ce-java16-linux-amd64-21.X.X.tar.gz

        assertDownloadUri(
            javaVersion = 16,
            vendor = any(),
            isJ9 = false,
            os = OperatingSystem.LINUX,
            arch = Architecture.X86_64
        ) // OpenJDK16U-jdk_x64_linux_hotspot_16.X.X.tar.gz

        assertDownloadUri(
            javaVersion = 16,
            vendor = any(),
            isJ9 = true,
            os = OperatingSystem.LINUX,
            arch = Architecture.X86_64
        ) // ibm-semeru-open-jdk_x64_linux_16.X.X_openj9-0.27.0.tar.gz

        assertDownloadUri(
            javaVersion = 8,
            vendor = GRAAL_VM,
            isJ9 = false,
            os = OperatingSystem.WINDOWS,
            arch = Architecture.X86_64
        ) // graalvm-ce-java8-windows-amd64-21.X.X.zip

        assertDownloadUri(
            javaVersion = 20,
            vendor = GRAAL_VM,
            isJ9 = false,
            os = OperatingSystem.LINUX,
            arch = Architecture.X86_64
        ) // graalvm-community-jdk-20.0.1_linux-x64_bin.tar.gz

        assertDownloadUri(
            expected = Regex(""),
            javaVersion = 1000,
            vendor = any(),
            isJ9 = false,
            os = OperatingSystem.LINUX,
            arch = Architecture.X86_64
        ) // No match because Java 1000 is not beeing released soon
    }

    @ParameterizedTest(name = "J9 implementation influences vendor resolution (Java {0})")
    @ValueSource(ints = [8, 11, 16])
    fun `J9 implementation influences vendor resolution`(version: Int) {
        assertMatchedDistributions(any(), J9, version, "Semeru", "AOJ OpenJ9")

        assertMatchedDistributions(ADOPTOPENJDK, J9, version, "AOJ OpenJ9")
        assertMatchedDistributions(IBM, J9, version, "Semeru")
        @Suppress("DEPRECATION")
        assertMatchedDistributions(IBM_SEMERU, J9, version, "Semeru")

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
                "ZuluPrime", "Zulu", "Trava", "Semeru certified", "Semeru", "SAP Machine", "Red Hat", "Oracle OpenJDK",
                "Oracle", "OpenLogic", "OJDKBuild", "Microsoft", "Mandrel", "Liberica Native", "Liberica", "Kona",
                "JetBrains", "GraalVM Community", "GraalVM CE $version", "GraalVM", "Gluon GraalVM", "Dragonwell",
                "Debian", "Corretto", "Bi Sheng", "AOJ OpenJ9"
        )

        assertMatchedDistributions(ADOPTOPENJDK, VENDOR_SPECIFIC, version, "AOJ")
        assertMatchedDistributions(IBM, VENDOR_SPECIFIC, version, "Semeru")
        @Suppress("DEPRECATION")
        assertMatchedDistributions(IBM_SEMERU, VENDOR_SPECIFIC, version, "Semeru")

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

    private fun assertMatchedDistributions(vendor: JvmVendorSpec, implementation: JvmImplementation, version: Int, vararg expectedDistributions: String) {
        assertEquals(
                listOf(*expectedDistributions),
                service.findMatchingDistributions(vendor, implementation, of(version)).getOrThrow().map { it.name },
                "Mismatch in matching distributions for vendor: $vendor, implementation: $implementation, version: $version"
        )
    }

    @ParameterizedTest(name = "can resolve arbitrary vendors (Java {0})")
    @ValueSource(ints = [8, 11, 16])
    fun `can resolve arbitrary vendors`(version: Int) {
        assertEquals("ZuluPrime", service.findMatchingDistributions(vendorSpec("zuluprime"), VENDOR_SPECIFIC, of(version)).getOrThrow().firstOrNull()?.name)
        assertEquals("ZuluPrime", service.findMatchingDistributions(vendorSpec("zUluprIme"), VENDOR_SPECIFIC, of(version)).getOrThrow().firstOrNull()?.name)
        assertEquals("JetBrains", service.findMatchingDistributions(vendorSpec("JetBrains"), VENDOR_SPECIFIC, of(version)).getOrThrow().firstOrNull()?.name)
    }

    @Test
    fun `can pick the right package`() {
        val p = service.findMatchingPackage("temurin", of(11), OperatingSystem.LINUX, Architecture.X86_64)
        assertNotNull(p)
        assertEquals("tar.gz", p.archive_type)
        assertEquals("temurin", p.distribution)
        assertEquals(11, p.jdk_version)
        assertTrue(Regex("11.\\d+.\\d+").matches(p.distribution_version))
        assertEquals("linux", p.operating_system)
        assertEquals("x64", p.architecture)
        assertEquals("jdk", p.package_type)
    }

    private fun assertDownloadUri(
            expected: Regex = Regex("""https://api.foojay.io/disco/v3.0/ids/[a-z0-9]{32}/redirect"""),
            javaVersion: Int,
            vendor: JvmVendorSpec,
            isJ9: Boolean,
            os: OperatingSystem,
            arch: Architecture
    ) {
        val matchingUri = service.findMatchingDownloadUri(
                of(javaVersion),
                vendor,
                if (isJ9) J9 else VENDOR_SPECIFIC,
                os,
                arch
        )
        val uriString = matchingUri.map { it?.toString() }.getOrNull() ?: ""
        assertTrue(expected.matches(uriString), "Expected URI differs from actual, got $uriString")
    }

    private fun vendorSpec(vendorName: String): JvmVendorSpec = matching(vendorName)

}
