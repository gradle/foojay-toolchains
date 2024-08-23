package org.gradle.toolchains.foojay

import org.gradle.testkit.runner.BuildResult
import org.gradle.testkit.runner.GradleRunner
import org.gradle.testkit.runner.TaskOutcome
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.io.TempDir
import java.io.File
import kotlin.test.assertTrue

abstract class AbstractFoojayToolchainsPluginFunctionalTest {

    @field:TempDir
    protected lateinit var projectDir: File

    @field:TempDir
    protected lateinit var homeDir: File

    private val settingsFile by lazy { projectDir.resolve("settings.gradle.kts") }
    private val propertiesFile by lazy { projectDir.resolve("gradle.properties") }
    private val buildFile by lazy { projectDir.resolve("build.gradle.kts") }
    private val sourceFolder by lazy { projectDir.resolve("src/main/java/") }

    @BeforeEach
    internal fun setUp() {
        propertiesFile.writeText("""
            org.gradle.java.installations.auto-detect=false
            org.gradle.java.installations.auto-download=true
        """.trimIndent())
    }

    protected fun runner(settings: String, buildScript: String): GradleRunner {
        settingsFile.writeText(settings)
        buildFile.writeText(buildScript.trimIndent())

        sourceFolder.mkdirs()
        val sourceFile = File(sourceFolder, "Java.java")
        sourceFile.writeText("""
            public class Java {
                public static void main(String[] args) {
                    System.out.println();
                }
            }
        """.trimIndent())

        return GradleRunner.create()
            .forwardOutput()
            .withPluginClasspath()
            .withArguments(listOf("--info", "-g", homeDir.absolutePath, "compileJava"))
            .withProjectDir(projectDir)
    }

    protected fun getDifferentJavaVersion() = when {
        System.getProperty("java.version").startsWith("11.") -> "16"
        else -> "11"
    }

    protected fun assertProvisioningSuccessful(buildResult: BuildResult) {
        val successfulTasks = buildResult.tasks(TaskOutcome.SUCCESS)
        assertTrue(":compileJava" in successfulTasks.map { it.path })
    }

    protected companion object {
        @JvmStatic
        @Suppress("MagicNumber")
        fun getGradleTestVersions(): List<String> {
            val versions = GradleTestVersions.getVersions()
            val latestVersions = versions.take(3)
            val oldestVersion = versions.takeLast(1)
            return latestVersions + oldestVersion // compromise, testing takes too long with all versions
        }
    }
}
