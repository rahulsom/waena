package com.github.rahulsom.waena

import com.fasterxml.jackson.databind.ObjectMapper
import org.assertj.core.api.Assertions.assertThat
import org.gradle.testfixtures.ProjectBuilder
import org.junit.jupiter.api.Test

class WaenaExtensionTest {
  @Test
  fun `can create extension`() {
    val project = ProjectBuilder.builder().build()
    val extension = WaenaExtension(project)
    assertThat(extension.license.get()).isEqualTo(WaenaExtension.License.Apache2)
    assertThat(extension.publishMode.get()).isEqualTo(WaenaExtension.PublishMode.OSS)
  }

  @Test
  fun `can set license`() {
    val project = ProjectBuilder.builder().build()
    val extension = WaenaExtension(project)
    extension.license.set(WaenaExtension.License.MIT)
    assertThat(extension.license.get()).isEqualTo(WaenaExtension.License.MIT)
  }

  @Test
  fun `can set publish mode`() {
    val project = ProjectBuilder.builder().build()
    val extension = WaenaExtension(project)
    extension.publishMode.set(WaenaExtension.PublishMode.Central)
    assertThat(extension.publishMode.get()).isEqualTo(WaenaExtension.PublishMode.Central)
  }

  @Test
  fun `can read extension as json`() {
    val project = ProjectBuilder.builder().build()
    val extension = WaenaExtension(project)
    val extensionConfig = ObjectMapper().readValue(extension.toJson(), Map::class.java)
    assertThat(extensionConfig["license"]).isEqualTo(WaenaExtension.License.Apache2.toString())
    assertThat(extensionConfig["publishMode"]).isEqualTo(WaenaExtension.PublishMode.OSS.toString())
  }
}