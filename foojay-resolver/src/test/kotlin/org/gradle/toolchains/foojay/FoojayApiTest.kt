package org.gradle.toolchains.foojay

import org.gradle.jvm.toolchain.JavaLanguageVersion.of
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
import kotlin.test.assertNull

class FoojayApiTest {

    private val api = FoojayApi()

    @Test
    fun `download URI provided correctly`() {
        assertDownloadUri(
            "https://api.foojay.io/disco/v3.0/ids/2b5dc4d917750eba32eb1acf62cec901/redirect",
            11, ADOPTIUM, false, OperatingSystem.MAC_OS, Architecture.AARCH64
        ) // OpenJDK11U-jdk_aarch64_mac_hotspot_11.0.17_8.tar.gz

        assertDownloadUri(
            "https://api.foojay.io/disco/v3.0/ids/ab6e7111c1a2cd7bf06de9be70ea0304/redirect",
            16, any(), true, OperatingSystem.LINUX, Architecture.X86_64
        ) // ibm-semeru-open-jdk_x64_linux_16.0.2_7_openj9-0.27.0.tar.gz

        assertDownloadUri(
            "https://api.foojay.io/disco/v3.0/ids/5b31509900ab21f4cd92dbc454b3c7e2/redirect",
            16, any(), true, OperatingSystem.MAC_OS, Architecture.X86_64
        ) // ibm-semeru-open-jdk_x64_mac_16.0.2_7_openj9-0.27.0.tar.gz

        assertDownloadUri(
                "https://api.foojay.io/disco/v3.0/ids/74bf5fb0d06e512f88356eb8fe45f67f/redirect",
                8, GRAAL_VM, false, OperatingSystem.WINDOWS, Architecture.X86_64
        ) // graalvm-ce-java8-windows-amd64-21.3.1.zip

        assertDownloadUri(
            "https://api.foojay.io/disco/v3.0/ids/9927d38888344a93f3803dfd0366a6e3/redirect",
            11, GRAAL_VM, false, OperatingSystem.MAC_OS, Architecture.AARCH64
        ) // graalvm-ce-java11-darwin-aarch64-22.3.0.tar.gz

        assertDownloadUri(
            "https://api.foojay.io/disco/v3.0/ids/871fe4e17cc2d5625fc5ca5f4027affd/redirect",
            16, GRAAL_VM, false, OperatingSystem.LINUX, Architecture.X86_64
        ) // graalvm-ce-java16-linux-amd64-21.2.0.tar.gz

        assertDownloadUri(
                "https://api.foojay.io/disco/v3.0/ids/09fd457b8a0a388f54ccf62049add79e/redirect",
                16, any(), false, OperatingSystem.LINUX, Architecture.X86_64
        ) // OpenJDK16U-jdk_x64_linux_hotspot_16.0.2_7.tar.gz
    }

    @ParameterizedTest(name = "J9 implementation influences vendor resolution (Java {0})")
    @ValueSource(ints = [8, 11, 16])
    fun `J9 implementation influences vendor resolution`(version: Int) {
        assertEquals("Semeru", api.match(any(), J9, of(version))?.name)

        assertEquals("AOJ OpenJ9", api.match(ADOPTOPENJDK, J9, of(version))?.name)
        assertEquals("Semeru", api.match(IBM, J9, of(version))?.name)
        assertEquals("Semeru", api.match(IBM_SEMERU, J9, of(version))?.name)

        assertNull(api.match(ADOPTIUM, J9, of(version))?.name)
        assertNull(api.match(AZUL, J9, of(version))?.name)
        assertNull(api.match(AMAZON, J9, of(version))?.name)
        assertNull(api.match(BELLSOFT, J9, of(version))?.name)
        assertNull(api.match(MICROSOFT, J9, of(version))?.name)
        assertNull(api.match(ORACLE, J9, of(version))?.name)
        assertNull(api.match(SAP, J9, of(version))?.name)

        assertNull(api.match(APPLE, J9, of(version))?.name)
        assertNull(api.match(GRAAL_VM, J9, of(version))?.name)
        assertNull(api.match(HEWLETT_PACKARD, J9, of(version))?.name)
    }

    @ParameterizedTest(name = "vendor specific implementation does not influences vendor resolution (Java {0})")
    @ValueSource(ints = [8, 11, 16])
    fun `vendor specific implementation does not influences vendor resolution`(version: Int) {
        assertEquals("Temurin", api.match(any(), VENDOR_SPECIFIC, of(version))?.name)

        assertEquals("AOJ", api.match(ADOPTOPENJDK, VENDOR_SPECIFIC, of(version))?.name)
        assertEquals("Semeru", api.match(IBM, VENDOR_SPECIFIC, of(version))?.name)
        assertEquals("Semeru", api.match(IBM_SEMERU, VENDOR_SPECIFIC, of(version))?.name)

        assertEquals("Temurin", api.match(ADOPTIUM, VENDOR_SPECIFIC, of(version))?.name)
        assertEquals("Zulu", api.match(AZUL, VENDOR_SPECIFIC, of(version))?.name)
        assertEquals("Corretto", api.match(AMAZON, VENDOR_SPECIFIC, of(version))?.name)
        assertEquals("Liberica", api.match(BELLSOFT, VENDOR_SPECIFIC, of(version))?.name)
        assertEquals("Microsoft", api.match(MICROSOFT, VENDOR_SPECIFIC, of(version))?.name)
        assertEquals("Oracle OpenJDK", api.match(ORACLE, VENDOR_SPECIFIC, of(version))?.name)
        assertEquals("SAP Machine", api.match(SAP, VENDOR_SPECIFIC, of(version))?.name)

        assertEquals("Graal VM CE $version", api.match(GRAAL_VM, VENDOR_SPECIFIC, of(version))?.name)

        assertNull(api.match(APPLE, VENDOR_SPECIFIC, of(version))?.name)
        assertNull(api.match(HEWLETT_PACKARD, VENDOR_SPECIFIC, of(version))?.name)
    }

    @ParameterizedTest(name = "can resolve arbitrary vendors (Java {0})")
    @ValueSource(ints = [8, 11, 16])
    fun `can resolve arbitrary vendors`(version: Int) {
        assertEquals("ZuluPrime", api.match(vendorSpec("zuluprime"), VENDOR_SPECIFIC, of(version))?.name)
        assertEquals("ZuluPrime", api.match(vendorSpec("zUluprIme"), VENDOR_SPECIFIC, of(version))?.name)
        assertEquals("JetBrains", api.match(vendorSpec("JetBrains"), VENDOR_SPECIFIC, of(version))?.name)
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