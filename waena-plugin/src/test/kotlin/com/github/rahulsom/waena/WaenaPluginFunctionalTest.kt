package com.github.rahulsom.waena

import com.github.rahulsom.waena.WaenaRootPlugin.Companion.CENTRAL
import org.assertj.core.api.SoftAssertions
import org.eclipse.jgit.api.Git
import org.eclipse.jgit.transport.URIish
import org.gradle.testkit.runner.GradleRunner
import org.intellij.lang.annotations.Language
import org.junit.jupiter.api.io.TempDir
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.Arguments.arguments
import org.junit.jupiter.params.provider.MethodSource
import java.io.File
import java.util.stream.Stream

/**
 * A simple functional test for the 'com.github.rahulsom.waena.greeting' plugin.
 */
class WaenaPluginFunctionalTest {

  companion object {
    private const val SNAPSHOT = "snapshot"
    private const val FINAL = "final"

    private const val SONATYPE_1_PUBLISH = "publishNebulaPublicationToSonatypeRepository"
    private const val SONATYPE_2_CLOSE = "closeSonatypeStagingRepository"
    private const val LOCAL_PUBLISH = "publishNebulaPublicationToLocalRepository"
    private const val SONATYPE_3_RELEASE = "releaseSonatypeStagingRepository"

    private val ALL_TASKS = setOf(LOCAL_PUBLISH, SONATYPE_1_PUBLISH, SONATYPE_2_CLOSE, SONATYPE_3_RELEASE)
    private val PUBLISH_TASKS = setOf(LOCAL_PUBLISH, SONATYPE_1_PUBLISH)

    fun buildGradleScript(waenaConfig: String = ""): String {
      // language=groovy
      return """
            import com.github.rahulsom.waena.WaenaExtension
            import groovy.json.JsonBuilder
            plugins {
                id('com.github.rahulsom.waena.root')
                id('com.github.rahulsom.waena.published')
            }
            $waenaConfig
            task('showconfig') {
              doLast {
                println(new JsonBuilder([
                  nexusUrl: nexusPublishing.repositories.getByName('sonatype').nexusUrl.get().toString(),
                  snapshotRepositoryUrl: nexusPublishing.repositories.getByName('sonatype').snapshotRepositoryUrl.get().toString()
                ]))
                println(project.extensions.getByType(WaenaExtension).toJson())
              }
            }
        """
    }

    @JvmStatic
    fun testParameters(): Stream<Arguments> {
      val gradleVersion = listOf(null, "9.0.0", "9.1.0", "9.2.1")
      val configUrlPairs = listOf(
        Pair(null, CENTRAL),
        Pair("Central", CENTRAL),
      )
      val taskTasksPairs = listOf(
        Pair(SNAPSHOT, PUBLISH_TASKS),
        Pair(FINAL, ALL_TASKS)
      )
      return gradleVersion.flatMap { gv ->
        configUrlPairs.flatMap { (publishMode, urls) ->
          taskTasksPairs.map { (task, tasks) ->
            arguments(publishMode, task, tasks, urls, gv)
          }
        }
      }.stream()
    }
  }

  @ParameterizedTest(name = "gradle({4}) publishMode({0}) task({1})")
  @MethodSource("testParameters")
  fun testWaenaConfiguration(
    testName: String?,
    task: String,
    containsTasks: Set<String>,
    urls: Pair<String, String>,
    gradleVersion: String?,
    @TempDir projectDir: File
  ) {
    val waenaConfig = testName?.let { "waena { publishMode.set(WaenaExtension.PublishMode.$it) }" }
    runTest(waenaConfig ?: "", task, containsTasks, urls, gradleVersion, projectDir)
  }

  fun runTest(
    @Language("kotlin") waenaConfig: String,
    task: String,
    containsTasks: Set<String>,
    urls: Pair<String, String>,
    gradleVersion: String?,
    @TempDir projectDir: File
  ) {
    val doesNotContainTasks = ALL_TASKS - containsTasks
    val softly = SoftAssertions()
    Git.init().setDirectory(projectDir).call()
    val git = Git.open(projectDir)
    git.repository.config.setString("user", null, "name", "John Doe")
    git.repository.config.setString("user", null, "email", "john.doe@example.com")
    projectDir.mkdirs()
    projectDir.resolve("settings.gradle").writeText("")
    projectDir.resolve("build.gradle").writeText(buildGradleScript(waenaConfig))
    projectDir.resolve(".gitignore").writeText(
      """
            build/
            .gradle/
        """.trimIndent()
    )
    git.add().addFilepattern(".").call()
    git.commit().setMessage("Initial commit").call()
    git.remoteAdd().setName("origin").setUri(URIish("https://github.com/rahulsom/nothing.git")).call()

    val taskTree = getTaskTree(task, projectDir, gradleVersion)
    val showConfigResult = showConfig(projectDir, gradleVersion)

    println(showConfigResult + "\n" + taskTree)

    // Verify the result
    containsTasks.forEach { t ->
      softly.assertThat(taskTree).contains(t)
    }
    doesNotContainTasks.forEach { t ->
      softly.assertThat(taskTree).doesNotContain(t)
    }

    // Verify the result
    softly.assertThat(showConfigResult!!.split("\n").first { it.contains("snapshotRepositoryUrl") })
      .isEqualTo("""{"nexusUrl":"${urls.second}","snapshotRepositoryUrl":"${urls.first}"}""")
    softly.assertAll()
  }

  private fun getTaskTree(task: String, projectDir: File, gradleVersion: String?): String? {
    val runner = baseRunner(projectDir, gradleVersion)
    runner.withArguments(task, "taskTree")
    val result = runner.build().output
    return result
  }

  private fun baseRunner(projectDir: File, gradleVersion: String?): GradleRunner {
    val runner = GradleRunner.create().forwardOutput().withPluginClasspath().withProjectDir(projectDir)
    if (gradleVersion != null) {
      runner.withGradleVersion(gradleVersion)
    }
    return runner
  }

  private fun showConfig(projectDir: File, gradleVersion: String?): String? {
    val showConfigRunner = baseRunner(projectDir, gradleVersion)
    showConfigRunner.withArguments("showconfig")
    val showConfigResult = showConfigRunner.build().output
    return showConfigResult
  }
}
