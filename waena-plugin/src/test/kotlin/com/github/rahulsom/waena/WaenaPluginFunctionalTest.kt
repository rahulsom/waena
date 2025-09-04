package com.github.rahulsom.waena

import org.assertj.core.api.SoftAssertions
import org.eclipse.jgit.api.Git
import org.eclipse.jgit.transport.URIish
import org.gradle.testkit.runner.GradleRunner
import org.intellij.lang.annotations.Language
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File

/**
 * A simple functional test for the 'com.github.rahulsom.waena.greeting' plugin.
 */
class WaenaPluginFunctionalTest {

  companion object {
    fun buildGradleScript(waenaConfig: String = ""): String {
      // language=groovy
      return """
            import com.github.rahulsom.waena.WaenaExtension.PublishMode
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
  }

  @Test
  fun `snapshot without customization`(@TempDir projectDir: File) {
    runTest(
      "",
      "snapshot",
      setOf("publishNebulaPublicationToSonatypeRepository"),
      setOf<String>("closeSonatypeStagingRepository"),
      WaenaRootPlugin.CENTRAL,
      projectDir
    )
  }

  @Test
  fun `final without customization`(@TempDir projectDir: File) {
    runTest(
      "",
      "final",
      setOf(
        "publishNebulaPublicationToLocalRepository",
        "publishNebulaPublicationToSonatypeRepository",
        "closeSonatypeStagingRepository",
        "releaseSonatypeStagingRepository"
      ),
      setOf(),
      WaenaRootPlugin.CENTRAL,
      projectDir
    )
  }

  @Test
  fun `snapshot for OSS`(@TempDir projectDir: File) {
    runTest(
      "waena { publishMode.set(PublishMode.OSS) } ",
      "snapshot",
      setOf("publishNebulaPublicationToLocalRepository", "publishNebulaPublicationToSonatypeRepository"),
      setOf("closeSonatypeStagingRepository", "releaseSonatypeStagingRepository"),
      WaenaRootPlugin.OSS,
      projectDir
    )
  }

  @Test
  fun `final for OSS`(@TempDir projectDir: File) {
    runTest(
      "waena { publishMode.set(PublishMode.OSS) } ",
      "final",
      setOf(
        "publishNebulaPublicationToLocalRepository",
        "publishNebulaPublicationToSonatypeRepository",
        "closeSonatypeStagingRepository",
        "releaseSonatypeStagingRepository"
      ),
      setOf(),
      WaenaRootPlugin.OSS,
      projectDir
    )
  }

  @Test
  @Disabled
  fun `snapshot for Central`(@TempDir projectDir: File) {
    runTest(
      "waena { publishMode.set(PublishMode.Central) } ",
      "snapshot",
      setOf("publishNebulaPublicationToLocalRepository", "publishNebulaPublicationToSonatypeRepository"),
      setOf("closeSonatypeStagingRepository", "releaseSonatypeStagingRepository"),
      WaenaRootPlugin.CENTRAL,
      projectDir
    )
  }

  @Disabled
  @Test
  fun `final for Central`(@TempDir projectDir: File) {
    runTest(
      "waena { publishMode.set(PublishMode.Central) } ",
      "final",
      setOf("publishNebulaPublicationToLocalRepository"),
      setOf("publishNebulaPublicationToSonatypeRepository", "closeSonatypeStagingRepository", "releaseSonatypeStagingRepository"),
      WaenaRootPlugin.CENTRAL,
      projectDir
    )
  }

  @Test
  fun `snapshot for S01`(@TempDir projectDir: File) {
    runTest(
      "waena { publishMode.set(PublishMode.S01) } ",
      "snapshot",
      setOf("publishNebulaPublicationToLocalRepository", "publishNebulaPublicationToSonatypeRepository"),
      setOf("closeSonatypeStagingRepository", "releaseSonatypeStagingRepository"),
      WaenaRootPlugin.S01,
      projectDir
    )
  }

  @Test
  fun `final for S01`(@TempDir projectDir: File) {
    runTest(
      "waena { publishMode.set(PublishMode.S01) } ",
      "final",
      setOf(
        "publishNebulaPublicationToLocalRepository",
        "publishNebulaPublicationToSonatypeRepository",
        "closeSonatypeStagingRepository",
        "releaseSonatypeStagingRepository"
      ),
      setOf(),
      WaenaRootPlugin.S01,
      projectDir
    )
  }

  fun runTest(
    @Language("kotlin") waenaConfig: String,
    task: String,
    containsTasks: Set<String>,
    doesNotContainTasks: Set<String>,
    urls: Pair<String, String>,
    @TempDir projectDir: File
  ) {
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

    val taskTree = getTaskTree(task, projectDir)
    val showConfigResult = showConfig(projectDir)

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

  private fun getTaskTree(task: String, projectDir: File): String? {
    val runner = baseRunner(projectDir)
    runner.withArguments(task, "taskTree")
    val result = runner.build().output
    return result
  }

  private fun baseRunner(projectDir: File): GradleRunner = GradleRunner.create().forwardOutput().withPluginClasspath().withProjectDir(projectDir)

  private fun showConfig(projectDir: File): String? {
    val showConfigRunner = baseRunner(projectDir)
    showConfigRunner.withArguments("showconfig")
    val showConfigResult = showConfigRunner.build().output
    return showConfigResult
  }
}
