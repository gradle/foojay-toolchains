package org.gradle.toolchains.foojay

import org.gradle.internal.impldep.com.google.gson.Gson
import org.gradle.util.GradleVersion
import java.net.URL


internal
object GradleTestVersions {

    private val smallestVersionOfInterest = GradleVersion.version("7.6")

    internal
    fun getVersions(): List<String> {
        val releasedVersions = getReleasedVersions()

        val testVersions = keepOnlyLatestMinor(releasedVersions)
            .toMutableList()
        testVersions.add(getLatestNightlyVersion())

        return testVersions
            .sortedByDescending { it.majorVersion }
            .map { it.gradleVersion.version }
            .toList()
    }

    private fun getLatestNightlyVersion(): VersionInfo {
        val jsonText = URL("https://services.gradle.org/versions/nightly").readText()
        return VersionInfo(Gson().fromJson(jsonText, VersionJsonBlock::class.java).version)
    }

    private fun getReleasedVersions(): List<VersionInfo> {
        val jsonText = URL("https://services.gradle.org/versions/all").readText()
        val allVersions = Gson().fromJson(jsonText, Array<VersionJsonBlock>::class.java)
            .asSequence()
            .filter { !it.snapshot }
            .filter { !it.nightly }
            .filter { it.rcFor.isBlank() }
            .filter { it.milestoneFor.isBlank() }
            .map { VersionInfo(it.version) }
            .filter { it.gradleVersion >= smallestVersionOfInterest }
            .toList()
        return allVersions
    }

    private fun keepOnlyLatestMinor(versions: List<VersionInfo>): MutableList<VersionInfo> {
        val filteredVersions = versions
            .sortedWith(compareBy<VersionInfo> { it.majorVersion }.thenBy { it.gradleVersion })
            .toMutableList()
        var i = 0
        while (i < filteredVersions.size - 1) {
            if (filteredVersions[i].majorVersion == filteredVersions[i + 1].majorVersion) {
                filteredVersions.removeAt(i)
            } else {
                i++
            }
        }
        return filteredVersions
    }
}

fun main() {
    val versions = GradleTestVersions.getVersions()
    println("versions = $versions")
}

@Suppress("MagicNumber")
private class VersionInfo(val version: String) {
    val gradleVersion: GradleVersion = GradleVersion.version(version)
    val majorVersion: GradleVersion = GradleVersion.version(getMajorVersion(version))
    private fun getMajorVersion(version: String): String {
        val majorVersionParts = version.split("\\.".toRegex())
        return when (majorVersionParts.size) {
            2 -> version
            3 -> "${majorVersionParts[0]}.${majorVersionParts[1]}"
            else -> error("Unexpected version number: $version")
        }
    }

    override fun toString(): String = version

}

data class VersionJsonBlock(
    val version: String,
    val snapshot: Boolean,
    val nightly: Boolean,
    val rcFor: String,
    val milestoneFor: String,
)
