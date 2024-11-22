@file:Suppress("ConstructorParameterNaming")

package org.gradle.toolchains.foojay

import com.google.gson.Gson
import org.gradle.platform.Architecture
import org.gradle.platform.OperatingSystem
import java.net.URI

val architectures32Bit = setOf("x32", "i386", "x86")
val architectures64Bit = setOf("x64", "x86_64", "amd64", "ia64")
val architecturesArm64Bit = setOf("aarch64", "arm64")

val handledArchiveTypes = setOf("tar", "tar.gz", "tgz", "zip")

fun parsePackages(json: String): List<Package> {
    return Gson().fromJson(json, PackagesResult::class.java).result
}

fun match(packages: List<Package>, architecture: Architecture): Package? {
    val candidates = packages
        .filter { p -> matches(p, architecture) }   // we filter out packages not matching the architecture the build is running on
        .filter { p -> hasHandledArchiveType(p) }   // Gradle can handle only certain archive types
        .sortedWith(compareBy(Package::package_type, Package::lib_c_type)) // prefer JDKs over JREs & prefer "glibc" over "musl"
    return candidates.firstOrNull()
}

fun OperatingSystem.toApiValue(): String =
    when (this) {
        OperatingSystem.LINUX -> "linux"
        OperatingSystem.UNIX -> "linux"
        OperatingSystem.WINDOWS -> "windows"
        OperatingSystem.MAC_OS -> "macos"
        OperatingSystem.SOLARIS -> "solaris"
        OperatingSystem.FREE_BSD -> "linux"
    }

private fun matches(p: Package, architecture: Architecture): Boolean =
    when (architecture) {
        Architecture.X86 -> p.architecture in architectures32Bit
        Architecture.X86_64 -> p.architecture in architectures64Bit
        Architecture.AARCH64 -> p.architecture in architecturesArm64Bit
                || (p.operating_system == OperatingSystem.MAC_OS.toApiValue() && p.architecture in architectures64Bit)
    }

private fun hasHandledArchiveType(p: Package): Boolean {
    return p.archive_type in handledArchiveTypes
}

data class Package(
        val archive_type: String,
        val distribution: String,
        val jdk_version: Int,
        val distribution_version: String,
        val operating_system: String,
        val architecture: String,
        val package_type: String,
        val lib_c_type: String,
        val links: Links
)

data class Links(
    val pkg_download_redirect: URI,
    val pkg_info_uri: URI?
)

private data class PackagesResult(
    val result: List<Package>
)
