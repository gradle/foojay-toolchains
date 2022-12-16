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

class FoojayApiTest {

    private val api = FoojayApi()

    @Test
    fun `download URI provided correctly`() {
        assertDownloadUri(
            "https://api.foojay.io/disco/v3.0/ids/06d328bdf96f725498761110afa5ddaa/redirect",
            8, any(), false, OperatingSystem.MAC_OS, Architecture.AARCH64
        ) // amazon-corretto-8.352.08.1-macosx-aarch64.tar.gz

        assertDownloadUri(
            "https://api.foojay.io/disco/v3.0/ids/2b5dc4d917750eba32eb1acf62cec901/redirect",
            11, ADOPTIUM, false, OperatingSystem.MAC_OS, Architecture.AARCH64
        ) // OpenJDK11U-jdk_aarch64_mac_hotspot_11.0.17_8.tar.gz

        assertDownloadUri(
            "https://api.foojay.io/disco/v3.0/ids/9927d38888344a93f3803dfd0366a6e3/redirect",
            11, GRAAL_VM, false, OperatingSystem.MAC_OS, Architecture.AARCH64
        ) // graalvm-ce-java11-darwin-aarch64-22.3.0.tar.gz

        assertDownloadUri(
                "https://api.foojay.io/disco/v3.0/ids/5b31509900ab21f4cd92dbc454b3c7e2/redirect",
                16, any(), true, OperatingSystem.MAC_OS, Architecture.X86_64
        ) // ibm-semeru-open-jdk_x64_mac_16.0.2_7_openj9-0.27.0.tar.gz

        assertDownloadUri(
                "https://api.foojay.io/disco/v3.0/ids/5b31509900ab21f4cd92dbc454b3c7e2/redirect",
                16, IBM, true, OperatingSystem.MAC_OS, Architecture.X86_64
        ) // ibm-semeru-open-jdk_x64_mac_16.0.2_7_openj9-0.27.0.tar.gz

        assertDownloadUri(
                "https://api.foojay.io/disco/v3.0/ids/5b31509900ab21f4cd92dbc454b3c7e2/redirect",
                16, IBM_SEMERU, true, OperatingSystem.MAC_OS, Architecture.X86_64
        ) // ibm-semeru-open-jdk_x64_mac_16.0.2_7_openj9-0.27.0.tar.gz

        assertDownloadUri(
            "https://api.foojay.io/disco/v3.0/ids/871fe4e17cc2d5625fc5ca5f4027affd/redirect",
            16, GRAAL_VM, false, OperatingSystem.LINUX, Architecture.X86_64
        ) // graalvm-ce-java16-linux-amd64-21.2.0.tar.gz

        assertDownloadUri(
                "https://api.foojay.io/disco/v3.0/ids/09fd457b8a0a388f54ccf62049add79e/redirect",
                16, any(), false, OperatingSystem.LINUX, Architecture.X86_64
        ) // OpenJDK16U-jdk_x64_linux_hotspot_16.0.2_7.tar.gz

        assertDownloadUri(
                "https://api.foojay.io/disco/v3.0/ids/ab6e7111c1a2cd7bf06de9be70ea0304/redirect",
                16, any(), true, OperatingSystem.LINUX, Architecture.X86_64
        ) // ibm-semeru-open-jdk_x64_linux_16.0.2_7_openj9-0.27.0.tar.gz

        assertDownloadUri(
                "https://api.foojay.io/disco/v3.0/ids/74bf5fb0d06e512f88356eb8fe45f67f/redirect",
                8, GRAAL_VM, false, OperatingSystem.WINDOWS, Architecture.X86_64
        ) // graalvm-ce-java8-windows-amd64-21.3.1.zip
    }

    @ParameterizedTest(name = "J9 implementation influences vendor resolution (Java {0})")
    @ValueSource(ints = [8, 11, 16])
    fun `J9 implementation influences vendor resolution`(version: Int) {
        assertMatchedDistributions(any(), J9, version, "Semeru", "AOJ OpenJ9")

        assertMatchedDistributions(ADOPTOPENJDK, J9, version, "AOJ OpenJ9")
        assertMatchedDistributions(IBM, J9, version, "Semeru")
        assertMatchedDistributions(IBM_SEMERU, J9, version, "Semeru")

        assertTrue(api.match(ADOPTIUM, J9, of(version)).isEmpty())
        assertTrue(api.match(AZUL, J9, of(version)).isEmpty())
        assertTrue(api.match(AMAZON, J9, of(version)).isEmpty())
        assertTrue(api.match(BELLSOFT, J9, of(version)).isEmpty())
        assertTrue(api.match(MICROSOFT, J9, of(version)).isEmpty())
        assertTrue(api.match(ORACLE, J9, of(version)).isEmpty())
        assertTrue(api.match(SAP, J9, of(version)).isEmpty())

        assertTrue(api.match(APPLE, J9, of(version)).isEmpty())
        assertTrue(api.match(GRAAL_VM, J9, of(version)).isEmpty())
        assertTrue(api.match(HEWLETT_PACKARD, J9, of(version)).isEmpty())
    }

    @ParameterizedTest(name = "vendor specific implementation does not influence vendor resolution (Java {0})")
    @ValueSource(ints = [8, 11, 16])
    fun `vendor specific implementation does not influence vendor resolution`(version: Int) {
        assertMatchesDistributionsContainInOrder(any(), VENDOR_SPECIFIC, version, "Temurin", "AOJ", "Corretto", "Zulu", "Liberica", "Semeru", "Oracle OpenJDK", "SAP Machine")

        assertMatchedDistributions(ADOPTOPENJDK, VENDOR_SPECIFIC, version, "AOJ")
        assertMatchedDistributions(IBM, VENDOR_SPECIFIC, version, "Semeru")
        assertMatchedDistributions(IBM_SEMERU, VENDOR_SPECIFIC, version, "Semeru")

        assertMatchedDistributions(ADOPTIUM, VENDOR_SPECIFIC, version, "Temurin")
        assertMatchedDistributions(AZUL, VENDOR_SPECIFIC, version, "Zulu")
        assertMatchedDistributions(AMAZON, VENDOR_SPECIFIC, version, "Corretto")
        assertMatchedDistributions(BELLSOFT, VENDOR_SPECIFIC, version, "Liberica")
        assertMatchedDistributions(MICROSOFT, VENDOR_SPECIFIC, version, "Microsoft")
        assertMatchedDistributions(ORACLE, VENDOR_SPECIFIC, version, "Oracle OpenJDK")
        assertMatchedDistributions(SAP, VENDOR_SPECIFIC, version, "SAP Machine")

        assertMatchedDistributions(GRAAL_VM, VENDOR_SPECIFIC, version, "Graal VM CE $version")

        assertMatchedDistributions(APPLE, VENDOR_SPECIFIC, version)
        assertMatchedDistributions(HEWLETT_PACKARD, VENDOR_SPECIFIC, version)
    }

    private fun assertMatchedDistributions(vendor: JvmVendorSpec, implementation: JvmImplementation, version: Int, vararg expectedDistributions: String) {
        assertEquals(
                listOf(*expectedDistributions),
                api.match(vendor, implementation, of(version)).map { it.name },
                "Mismatch in matching distributions for vendor: $vendor, implementation: $implementation, version: $version"
        )
    }

    private fun assertMatchesDistributionsContainInOrder(vendor: JvmVendorSpec, implementation: JvmImplementation, version: Int, vararg expectedDistributions: String) {
        val matches = api.match(vendor, implementation, of(version)).map { it.name }
        assertEquals(
            listOf(*expectedDistributions), matches.subList(0, expectedDistributions.size),
                "Mismatch in matching distributions for vendor: $vendor, implementation: $implementation, version: $version"
        )
    }

    @ParameterizedTest(name = "can resolve arbitrary vendors (Java {0})")
    @ValueSource(ints = [8, 11, 16])
    fun `can resolve arbitrary vendors`(version: Int) {
        assertEquals("ZuluPrime", api.match(vendorSpec("zuluprime"), VENDOR_SPECIFIC, of(version)).firstOrNull()?.name)
        assertEquals("ZuluPrime", api.match(vendorSpec("zUluprIme"), VENDOR_SPECIFIC, of(version)).firstOrNull()?.name)
        assertEquals("JetBrains", api.match(vendorSpec("JetBrains"), VENDOR_SPECIFIC, of(version)).firstOrNull()?.name)
    }

    @Test
    fun `can pick the right package`() {
        val p = api.match("temurin", of(11), OperatingSystem.LINUX, Architecture.X86_64)
        assertNotNull(p)
        assertEquals("tar.gz", p.archive_type)
        assertEquals("temurin", p.distribution)
        assertEquals(11, p.jdk_version)
        assertEquals("11.0.17", p.distribution_version)
        assertEquals("linux", p.operating_system)
        assertEquals("x64", p.architecture)
        assertEquals("jdk", p.package_type)
    }

    private fun assertDownloadUri(
            expected: String,
            javaVersion: Int,
            vendor: JvmVendorSpec,
            isJ9: Boolean,
            os: OperatingSystem,
            arch: Architecture
    ) {
        val uri = api.toUri(
                of(javaVersion),
            vendor,
            if (isJ9) J9 else VENDOR_SPECIFIC,
            os,
            arch
        )
        assertEquals(expected, uri.toString())
    }

    private fun vendorSpec(vendorName: String): JvmVendorSpec = matching(vendorName)

}
