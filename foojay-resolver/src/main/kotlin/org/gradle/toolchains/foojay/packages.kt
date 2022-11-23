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
        .sortedBy { p -> p.package_type } // prefer JDKs over JREs
    return candidates.firstOrNull()
}

fun map(os: OperatingSystem): String =
    when (os) {
        OperatingSystem.LINUX -> "linux"
        OperatingSystem.UNIX -> "linux"
        OperatingSystem.WINDOWS -> "windows"
        OperatingSystem.MAC_OS -> "macos"
        OperatingSystem.SOLARIS -> "solaris"
        OperatingSystem.FREE_BSD -> "linux"
    }

private fun matches(p: Package, architecture: Architecture): Boolean =
    when (architecture) {
        Architecture.X86 -> architectures32Bit.contains(p.architecture)
        Architecture.X86_64 -> architectures64Bit.contains(p.architecture)
        Architecture.AARCH64 -> architecturesArm64Bit.contains(p.architecture)
    }

private fun hasHandledArchiveType(p: Package): Boolean {
    return handledArchiveTypes.contains(p.archive_type)
}

data class Package(
        val archive_type: String,
        val distribution: String,
        val major_version: Int,
        val operating_system: String,
        val architecture: String,
        val package_type: String,
        val links: Links,
)

data class Links(
    val pkg_download_redirect: URI,
    val pkg_info_uri: URI?,
)

private data class PackagesResult(
    val result: List<Package>
)