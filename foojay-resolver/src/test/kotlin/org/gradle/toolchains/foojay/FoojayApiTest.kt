package org.gradle.toolchains.foojay

import org.gradle.jvm.toolchain.JavaLanguageVersion
import org.gradle.jvm.toolchain.JvmImplementation
import org.gradle.jvm.toolchain.JvmVendorSpec
import org.gradle.jvm.toolchain.internal.DefaultJvmVendorSpec
import org.gradle.platform.Architecture
import org.gradle.platform.OperatingSystem
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
            11, JvmVendorSpec.ADOPTIUM, false, OperatingSystem.MAC_OS, Architecture.AARCH64
        ) // OpenJDK11U-jdk_aarch64_mac_hotspot_11.0.17_8.tar.gz

        assertDownloadUri(
            "https://api.foojay.io/disco/v3.0/ids/ab6e7111c1a2cd7bf06de9be70ea0304/redirect",
            16, DefaultJvmVendorSpec.any(), true, OperatingSystem.LINUX, Architecture.X86_64
        ) // ibm-semeru-open-jdk_x64_linux_16.0.2_7_openj9-0.27.0.tar.gz

        assertDownloadUri(
            "https://api.foojay.io/disco/v3.0/ids/5b31509900ab21f4cd92dbc454b3c7e2/redirect",
            16, DefaultJvmVendorSpec.any(), true, OperatingSystem.MAC_OS, Architecture.X86_64
        ) // ibm-semeru-open-jdk_x64_mac_16.0.2_7_openj9-0.27.0.tar.gz
    }

    @Test
    fun `J9 implementation influences vendor resolution`() {
        assertEquals("Semeru", api.match(DefaultJvmVendorSpec.any(), JvmImplementation.J9)?.name)

        assertEquals("AOJ OpenJ9", api.match(JvmVendorSpec.ADOPTOPENJDK, JvmImplementation.J9)?.name)
        assertEquals("Semeru", api.match(JvmVendorSpec.IBM_SEMERU, JvmImplementation.J9)?.name)

        assertNull(api.match(JvmVendorSpec.ADOPTIUM, JvmImplementation.J9)?.name)
        assertNull(api.match(JvmVendorSpec.AZUL, JvmImplementation.J9)?.name)
        assertNull(api.match(JvmVendorSpec.AMAZON, JvmImplementation.J9)?.name)
        assertNull(api.match(JvmVendorSpec.BELLSOFT, JvmImplementation.J9)?.name)
        assertNull(api.match(JvmVendorSpec.MICROSOFT, JvmImplementation.J9)?.name)
        assertNull(api.match(JvmVendorSpec.ORACLE, JvmImplementation.J9)?.name)
        assertNull(api.match(JvmVendorSpec.SAP, JvmImplementation.J9)?.name)

        assertNull(api.match(JvmVendorSpec.APPLE, JvmImplementation.J9)?.name)
        assertNull(api.match(JvmVendorSpec.GRAAL_VM, JvmImplementation.J9)?.name)
        assertNull(api.match(JvmVendorSpec.HEWLETT_PACKARD, JvmImplementation.J9)?.name)
        assertNull(api.match(JvmVendorSpec.IBM, JvmImplementation.J9)?.name)
    }

    @Test
    fun `vendor specific implementation does not influences vendor resolution`() {
        assertEquals("Temurin", api.match(DefaultJvmVendorSpec.any(), JvmImplementation.VENDOR_SPECIFIC)?.name)

        assertEquals("AOJ", api.match(JvmVendorSpec.ADOPTOPENJDK, JvmImplementation.VENDOR_SPECIFIC)?.name)
        assertEquals("Semeru", api.match(JvmVendorSpec.IBM_SEMERU, JvmImplementation.VENDOR_SPECIFIC)?.name)

        assertEquals("Temurin", api.match(JvmVendorSpec.ADOPTIUM, JvmImplementation.VENDOR_SPECIFIC)?.name)
        assertEquals("Zulu", api.match(JvmVendorSpec.AZUL, JvmImplementation.VENDOR_SPECIFIC)?.name)
        assertEquals("Corretto", api.match(JvmVendorSpec.AMAZON, JvmImplementation.VENDOR_SPECIFIC)?.name)
        assertEquals("Liberica", api.match(JvmVendorSpec.BELLSOFT, JvmImplementation.VENDOR_SPECIFIC)?.name)
        assertEquals("Microsoft", api.match(JvmVendorSpec.MICROSOFT, JvmImplementation.VENDOR_SPECIFIC)?.name)
        assertEquals("Oracle OpenJDK", api.match(JvmVendorSpec.ORACLE, JvmImplementation.VENDOR_SPECIFIC)?.name)
        assertEquals("SAP Machine", api.match(JvmVendorSpec.SAP, JvmImplementation.VENDOR_SPECIFIC)?.name)

        assertNull(api.match(JvmVendorSpec.APPLE, JvmImplementation.VENDOR_SPECIFIC)?.name)
        assertNull(api.match(JvmVendorSpec.GRAAL_VM, JvmImplementation.VENDOR_SPECIFIC)?.name)
        assertNull(api.match(JvmVendorSpec.HEWLETT_PACKARD, JvmImplementation.VENDOR_SPECIFIC)?.name)
        assertNull(api.match(JvmVendorSpec.IBM, JvmImplementation.VENDOR_SPECIFIC)?.name)
    }

    @Test
    fun `can resolve arbitrary vendors`() {
        assertEquals("ZuluPrime", api.match(vendorSpec("zuluprime"), JvmImplementation.VENDOR_SPECIFIC)?.name)
        assertEquals("ZuluPrime", api.match(vendorSpec("zUluprIme"), JvmImplementation.VENDOR_SPECIFIC)?.name)
        assertEquals("JetBrains", api.match(vendorSpec("JetBrains"), JvmImplementation.VENDOR_SPECIFIC)?.name)
    }

    @Test
    fun `can match GraalVM`() {
        assertEquals("Graal VM CE 8", api.match(vendorSpec("GraalVMCE8"), JvmImplementation.VENDOR_SPECIFIC)?.name)
        assertEquals("Graal VM CE 11", api.match(vendorSpec("GraalVMCE11"), JvmImplementation.VENDOR_SPECIFIC)?.name)
    }

    @Test
    fun `can pick the right package`() {
        val p = api.match("temurin", JavaLanguageVersion.of(11), OperatingSystem.LINUX, Architecture.X86_64)
        assertNotNull(p)
        assertEquals("tar.gz", p.archive_type)
        assertEquals("temurin", p.distribution)
        assertEquals(11, p.major_version)
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
                JavaLanguageVersion.of(javaVersion),
            vendor,
            if (isJ9) JvmImplementation.J9 else JvmImplementation.VENDOR_SPECIFIC,
            os,
            arch
        )
        assertEquals(expected, uri.toString())
    }

    private fun vendorSpec(vendorName: String): JvmVendorSpec = JvmVendorSpec.matching(vendorName)

}