package com.github.rahulsom.waena

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.SoftAssertions
import org.eclipse.jgit.api.Git
import org.eclipse.jgit.transport.URIish
import org.gradle.testkit.runner.GradleRunner
import org.intellij.lang.annotations.Language
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.Arguments.arguments
import org.junit.jupiter.params.provider.MethodSource
import java.io.File
import java.util.jar.JarFile
import java.util.stream.Stream

class WaenaPluginFunctionalTest {

  companion object {
    private const val SNAPSHOT = "snapshot"
    private const val FINAL = "final"
    private const val CANDIDATE = "candidate"

    private const val LOCAL_PUBLISH = "publishNebulaPublicationToLocalRepository"
    private const val SONATYPE_PUBLISH = "publishNebulaPublicationToSonatypeRepository"
    private const val JRELEASER_DEPLOY = "jreleaserDeploy"

    private val RELEASE_TASKS = setOf(LOCAL_PUBLISH, JRELEASER_DEPLOY)
    private val SNAPSHOT_TASKS = setOf(LOCAL_PUBLISH, SONATYPE_PUBLISH)

    fun buildGradleScript(waenaConfig: String = ""): String {
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
              publishMode: project.extensions.getByType(WaenaExtension).publishMode.get().toString()
            ]))
            println(project.extensions.getByType(WaenaExtension).toJson())
          }
        }
        """
    }

    @JvmStatic
    fun testParameters(): Stream<Arguments> {
      val gradleVersion = listOf(null, "9.1.0", "9.2.1", "9.4.0")
      val publishModes = listOf(null, "Central")
      val taskTasksPairs = listOf(
        Pair(SNAPSHOT, SNAPSHOT_TASKS),
        Pair(CANDIDATE, RELEASE_TASKS),
        Pair(FINAL, RELEASE_TASKS)
      )
      return gradleVersion.flatMap { gv ->
        publishModes.flatMap { publishMode ->
          taskTasksPairs.map { (task, tasks) ->
            arguments(publishMode, task, tasks, gv)
          }
        }
      }.stream()
    }
  }

  @ParameterizedTest(name = "gradle({3}) publishMode({0}) task({1})")
  @MethodSource("testParameters")
  fun testWaenaConfiguration(
    testName: String?,
    task: String,
    containsTasks: Set<String>,
    gradleVersion: String?,
    @TempDir projectDir: File
  ) {
    val waenaConfig = testName?.let { "waena { publishMode.set(WaenaExtension.PublishMode.$it) }" }
    runTest(waenaConfig ?: "", task, containsTasks, gradleVersion, projectDir)
  }

  fun runTest(
    @Language("kotlin") waenaConfig: String,
    task: String,
    containsTasks: Set<String>,
    gradleVersion: String?,
    @TempDir projectDir: File
  ) {
    val allPossibleTasks = setOf(LOCAL_PUBLISH, SONATYPE_PUBLISH, JRELEASER_DEPLOY)
    val doesNotContainTasks = allPossibleTasks - containsTasks
    val softly = SoftAssertions()
    Git.init().setDirectory(projectDir).call()
    val git = Git.open(projectDir)
    git.repository.config.setString("user", null, "name", "John Doe")
    git.repository.config.setString("user", null, "email", "john.doe@example.com")
    git.repository.config.setBoolean("commit", null, "gpgsign", false)
    git.repository.config.setBoolean("tag", null, "gpgsign", false)
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

    containsTasks.forEach { t ->
      softly.assertThat(taskTree).contains(t)
    }
    doesNotContainTasks.forEach { t ->
      softly.assertThat(taskTree).doesNotContain(t)
    }

    softly.assertThat(showConfigResult).contains("\"publishMode\":\"Central\"")
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
    showConfigRunner.withArguments("-P${WaenaRootPlugin.CONFIGURE_REMOTE_PUBLISHING_PROPERTY}=true", "showconfig")
    val showConfigResult = showConfigRunner.build().output
    return showConfigResult
  }

  private fun initMultiModuleProject(projectDir: File): Git {
    Git.init().setDirectory(projectDir).call()
    val git = Git.open(projectDir)
    git.repository.config.setString("user", null, "name", "John Doe")
    git.repository.config.setString("user", null, "email", "john.doe@example.com")
    git.repository.config.setBoolean("commit", null, "gpgsign", false)
    git.repository.config.setBoolean("tag", null, "gpgsign", false)

    projectDir.resolve("settings.gradle").writeText(
      """
      rootProject.name = 'multi-test'
      include 'sub-a', 'sub-b'
      """.trimIndent()
    )
    projectDir.resolve("build.gradle").writeText(
      """
      plugins {
        id('com.github.rahulsom.waena.root')
      }
      """.trimIndent()
    )

    listOf("sub-a", "sub-b").forEach { sub ->
      val subDir = projectDir.resolve(sub)
      subDir.mkdirs()
      subDir.resolve("build.gradle").writeText(
        """
        plugins {
          id('java')
          id('com.github.rahulsom.waena.published')
        }
        group = 'com.example'
        """.trimIndent()
      )
      subDir.resolve("src/main/java").mkdirs()
      subDir.resolve("src/main/java/Dummy.java").writeText("public class Dummy {}")
      subDir.resolve("src/test/java").mkdirs()
      subDir.resolve("src/test/java/DummyTest.java").writeText(
        """
        import org.junit.jupiter.api.Test;
        class DummyTest {
          @Test void dummy() {}
        }
        """.trimIndent()
      )
    }

    projectDir.resolve(".gitignore").writeText("build/\n.gradle/\n")
    git.add().addFilepattern(".").call()
    git.commit().setMessage("Initial commit").call()
    git.remoteAdd().setName("origin").setUri(URIish("https://github.com/rahulsom/nothing.git")).call()
    return git
  }

  private fun extractTaskOrder(dryRunOutput: String): List<String> {
    return dryRunOutput.lines()
      .filter { it.startsWith("> Task ") || it.startsWith(":") }
      .map { line ->
        line.removePrefix("> Task ").split(" ")[0]
      }
  }

  @Test
  fun `all verification tasks run before any publish task in multi-module build`(@TempDir projectDir: File) {
    initMultiModuleProject(projectDir)

    val result = baseRunner(projectDir, null)
      .withArguments("check", "publishToMavenLocal", "publishNebulaPublicationToLocalRepository", "--dry-run")
      .build()

    val tasks = extractTaskOrder(result.output)

    val verificationTasks = tasks.filter { it.contains("test") || it.contains("Test") || it.contains("check") || it.contains("Check") }
    val publishTasks = tasks.filter { it.contains("publish", ignoreCase = true) && !it.contains("generate", ignoreCase = true) }

    assertThat(verificationTasks).isNotEmpty()
    assertThat(publishTasks).isNotEmpty()

    val lastVerificationIndex = verificationTasks.maxOf { tasks.indexOf(it) }
    val firstPublishIndex = publishTasks.minOf { tasks.indexOf(it) }

    assertThat(lastVerificationIndex)
      .describedAs(
        "Last verification task (${tasks[lastVerificationIndex]}) at index $lastVerificationIndex " +
            "should be before first publish task (${tasks[firstPublishIndex]}) at index $firstPublishIndex"
      )
      .isLessThan(firstPublishIndex)
  }

  @Test
  fun `publish tasks still run when a verification task is excluded`(@TempDir projectDir: File) {
    initMultiModuleProject(projectDir)

    val result = baseRunner(projectDir, null)
      .withArguments("check", "publishNebulaPublicationToLocalRepository", "-x", ":sub-a:check", "--dry-run")
      .build()

    val tasks = extractTaskOrder(result.output)

    assertThat(tasks).anyMatch { it.contains("publishNebulaPublicationToLocalRepository") }
    assertThat(tasks).noneMatch { it == ":sub-a:check" }

    val verificationTasks = tasks.filter { it.contains("test") || it.contains("Test") || it.contains("check") || it.contains("Check") }
    val publishTasks = tasks.filter { it.contains("publish", ignoreCase = true) && !it.contains("generate", ignoreCase = true) }

    if (verificationTasks.isNotEmpty() && publishTasks.isNotEmpty()) {
      val lastVerificationIndex = verificationTasks.maxOf { tasks.indexOf(it) }
      val firstPublishIndex = publishTasks.minOf { tasks.indexOf(it) }
      assertThat(lastVerificationIndex)
        .describedAs(
          "Remaining verification tasks should still precede publish tasks"
        )
        .isLessThan(firstPublishIndex)
    }
  }

  @Test
  fun `built JAR contains Waena-Version manifest entry`(@TempDir projectDir: File) {
    Git.init().setDirectory(projectDir).call()
    val git = Git.open(projectDir)
    git.repository.config.setString("user", null, "name", "John Doe")
    git.repository.config.setString("user", null, "email", "john.doe@example.com")
    git.repository.config.setBoolean("commit", null, "gpgsign", false)
    git.repository.config.setBoolean("tag", null, "gpgsign", false)

    projectDir.resolve("settings.gradle").writeText("rootProject.name = 'test-project'")
    projectDir.resolve("build.gradle").writeText(
      """
      plugins {
        id('java')
        id('com.github.rahulsom.waena.root')
        id('com.github.rahulsom.waena.published')
      }
      group = 'com.example'
      version = '1.0.0'
      """.trimIndent()
    )

    projectDir.resolve("src/main/java").mkdirs()
    projectDir.resolve("src/main/java/Example.java").writeText("public class Example {}")

    projectDir.resolve(".gitignore").writeText("build/\n.gradle/\n")
    git.add().addFilepattern(".").call()
    git.commit().setMessage("Initial commit").call()
    git.remoteAdd().setName("origin").setUri(URIish("https://github.com/rahulsom/nothing.git")).call()

    GradleRunner.create()
      .withPluginClasspath()
      .withProjectDir(projectDir)
      .withArguments("jar")
      .forwardOutput()
      .build()

    val jarFile = projectDir.resolve("build/libs/test-project-1.0.0.jar")
    JarFile(jarFile).use { jar ->
      val manifest = jar.manifest
      assertThat(manifest.mainAttributes.getValue("Waena-Version")).isNotNull().isNotEqualTo("unknown")
    }
  }
}
