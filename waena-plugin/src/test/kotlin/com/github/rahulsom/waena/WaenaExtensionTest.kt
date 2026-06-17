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
    @Suppress("DEPRECATION")
    assertThat(extension.publishMode.get()).isEqualTo(WaenaExtension.PublishMode.Central)
  }

  @Test
  fun `can set license`() {
    val project = ProjectBuilder.builder().build()
    val extension = WaenaExtension(project)
    extension.license.set(WaenaExtension.License.MIT)
    assertThat(extension.license.get()).isEqualTo(WaenaExtension.License.MIT)
  }

  @Test
  fun `can set publish modes`() {
    val project = ProjectBuilder.builder().build()
    val extension = WaenaExtension(project)
    extension.publishModes.set(setOf(WaenaExtension.PublishMode.Central, WaenaExtension.PublishMode.GitHub))
    assertThat(extension.publishModes.get()).containsExactlyInAnyOrder(
      WaenaExtension.PublishMode.Central,
      WaenaExtension.PublishMode.GitHub,
    )
  }

  @Test
  @Suppress("DEPRECATION")
  fun `can set publish mode (deprecated)`() {
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
    assertThat(extensionConfig["publishModes"] as List<*>).containsExactly(WaenaExtension.PublishMode.Central.toString())
  }
}
