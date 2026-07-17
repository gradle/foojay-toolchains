package org.gradle.toolchains.foojay

import io.netty.handler.codec.http.HttpObject
import io.netty.handler.codec.http.HttpRequest
import io.netty.handler.codec.http.HttpResponse
import org.gradle.testkit.runner.BuildResult
import org.gradle.testkit.runner.GradleRunner
import org.gradle.testkit.runner.TaskOutcome
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.io.TempDir
import org.littleshoot.proxy.HttpFilters
import org.littleshoot.proxy.HttpFiltersAdapter
import org.littleshoot.proxy.HttpFiltersSourceAdapter
import org.littleshoot.proxy.HttpProxyServer
import org.littleshoot.proxy.impl.DefaultHttpProxyServer
import java.io.File
import java.util.concurrent.atomic.AtomicInteger
import kotlin.test.assertTrue

abstract class AbstractFoojayToolchainsPluginFunctionalTest {

    @field:TempDir
    protected lateinit var projectDir: File

    @field:TempDir
    protected lateinit var homeDir: File

    protected lateinit var proxyServer: HttpProxyServer
    protected var proxyPort: Int = 0
    protected val proxyInterceptorCount = AtomicInteger(0)

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
        // Start proxy on a random available port
        proxyInterceptorCount.set(0)
        proxyServer = DefaultHttpProxyServer.bootstrap()
            .withPort(0)
            .withFiltersSource(object : HttpFiltersSourceAdapter() {
                override fun filterRequest(originalRequest: HttpRequest): HttpFilters {
                    return object : HttpFiltersAdapter(originalRequest) {
                        override fun clientToProxyRequest(httpObject: HttpObject): HttpResponse? {
                            // Increment whenever the proxy intercepts a request frame
                            if (httpObject is HttpRequest) {
                                System.err.println("Captured traffic in proxy!")
                                proxyInterceptorCount.incrementAndGet()
                            }
                            return null // Continue normal routing
                        }
                    }
                }
            })
            .start()
        proxyPort = proxyServer.listenAddress.port
    }

    @AfterEach
    internal fun tearDownProxy() {
        proxyServer.stop()
    }

    protected fun runner(
        settings: String,
        buildScript: String,
        extraArguments: List<String> = emptyList()
    ): GradleRunner {

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
            .withArguments(listOf("--info", "-g", homeDir.absolutePath, "compileJava") + extraArguments)
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
