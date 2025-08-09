package com.github.rahulsom.waena

import nebula.plugin.info.InfoPlugin
import nebula.plugin.publishing.maven.MavenPublishPlugin
import nebula.plugin.release.ReleasePlugin
import org.assertj.core.api.Assertions.assertThat
import org.gradle.plugins.signing.SigningPlugin
import org.gradle.testfixtures.ProjectBuilder
import org.junit.jupiter.api.Test

class WaenaPublishedPluginTest {
  @Test
  fun `plugin registers task`() {
    val rootProject = ProjectBuilder.builder().build()
    rootProject.plugins.apply(WaenaRootPlugin::class.java)

    val submodule = ProjectBuilder.builder().withParent(rootProject).build()
    submodule.plugins.apply(WaenaPublishedPlugin::class.java)

    // Verify the result
    val installedPlugins = submodule.plugins
    assertThat(installedPlugins.find { it is WaenaRootPlugin }).isNull()
    assertThat(installedPlugins.find { it is WaenaPublishedPlugin }).isNotNull()
    assertThat(installedPlugins.find { it is SigningPlugin }).isNotNull()
    assertThat(installedPlugins.find { it is ReleasePlugin }).isNotNull()
    assertThat(installedPlugins.find { it is MavenPublishPlugin }).isNotNull()

    val rootPlugins = rootProject.plugins
    assertThat(rootPlugins.find { it is InfoPlugin }).isNotNull()
  }
}