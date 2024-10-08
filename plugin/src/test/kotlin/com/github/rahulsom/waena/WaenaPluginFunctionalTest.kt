/*
 * This Kotlin source file was generated by the Gradle 'init' task.
 */
package com.github.rahulsom.waena

import org.assertj.core.api.Assertions.assertThat
import org.eclipse.jgit.api.Git
import org.eclipse.jgit.transport.URIish
import org.gradle.testkit.runner.GradleRunner
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File

/**
 * A simple functional test for the 'com.github.rahulsom.waena.greeting' plugin.
 */
class WaenaPluginFunctionalTest {

  @Nested
  inner class Simple {

    @Test
    fun `can run task`(@TempDir projectDir: File) {
      // Setup the test build
      Git.init().setDirectory(projectDir).call()
      val git = Git.open(projectDir)
      git.repository.config.setString("user", null, "name", "John Doe")
      git.repository.config.setString("user", null, "email", "john.doe@example.com")
      projectDir.mkdirs()
      projectDir.resolve("settings.gradle").writeText("")
      projectDir.resolve("build.gradle").writeText(
        // language=groovy
        """
            plugins {
                id('com.github.rahulsom.waena.root')
                id('com.github.rahulsom.waena.published')
            }
            
            task('showconfig') {
              doLast {
                println(nexusPublishing.repositories.getByName('sonatype').nexusUrl.get())
              }
            }
        """
      )
      projectDir.resolve(".gitignore").writeText(
        """
            build/
            .gradle/
        """.trimIndent()
      )
      git.add().addFilepattern(".").call()
      git.commit().setMessage("Initial commit").call()
      git.remoteAdd().setName("origin").setUri(URIish("https://github.com/rahulsom/nothing.git")).call()

      // Run the build
      val runner = GradleRunner.create()
      runner.forwardOutput()
      runner.withPluginClasspath()
      runner.withArguments("final", "taskTree")
      runner.withProjectDir(projectDir)
      val result = runner.build()

      // Verify the result
      assertThat(result.output).contains("publishNebulaPublicationToSonatypeRepository")

      // Run showconfig
      val showConfigRunner = GradleRunner.create()
      showConfigRunner.forwardOutput()
      showConfigRunner.withPluginClasspath()
      showConfigRunner.withArguments("showconfig")
      showConfigRunner.withProjectDir(projectDir)
      val showConfigResult = showConfigRunner.build()

      // Verify the result
      assertThat(showConfigResult.output).contains("https://oss.sonatype.org/service/local/")
    }
  }

  @Nested
  inner class WithCentral {

    @Test
    fun `can run task`(@TempDir projectDir: File) {
      // Setup the test build
      Git.init().setDirectory(projectDir).call()
      val git = Git.open(projectDir)
      git.repository.config.setString("user", null, "name", "John Doe")
      git.repository.config.setString("user", null, "email", "john.doe@example.com")
      projectDir.mkdirs()
      projectDir.resolve("settings.gradle").writeText("")
      projectDir.resolve("build.gradle").writeText(
        // language=groovy
        """
            plugins {
                id('com.github.rahulsom.waena.root')
                id('com.github.rahulsom.waena.published')
            }
            
            waena {
                useCentralPortal.set(true)
            }
            
            task('showconfig') {
              doLast {
                println(nexusPublishing.repositories.getByName('sonatype').nexusUrl.get())
              }
            }
        """
      )
      projectDir.resolve(".gitignore").writeText(
        """
            build/
            .gradle/
        """.trimIndent()
      )
      git.add().addFilepattern(".").call()
      git.commit().setMessage("Initial commit").call()
      git.remoteAdd().setName("origin").setUri(URIish("https://github.com/rahulsom/nothing.git")).call()

      // Run the build
      val runner = GradleRunner.create()
      runner.forwardOutput()
      runner.withPluginClasspath()
      runner.withArguments("final", "taskTree")
      runner.withProjectDir(projectDir)
      val result = runner.build()

      // Verify the result
      assertThat(result.output).contains("publishNebulaPublicationToSonatypeRepository")


      // Run showconfig
      val showConfigRunner = GradleRunner.create()
      showConfigRunner.forwardOutput()
      showConfigRunner.withPluginClasspath()
      showConfigRunner.withArguments("showconfig")
      showConfigRunner.withProjectDir(projectDir)
      val showConfigResult = showConfigRunner.build()

      // Verify the result
      assertThat(showConfigResult.output).contains("https://s01.oss.sonatype.org/service/local/")

    }
  }
}
