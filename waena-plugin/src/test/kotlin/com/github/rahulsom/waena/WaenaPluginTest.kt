package com.github.rahulsom.waena

import org.assertj.core.api.Assertions.assertThat
import org.gradle.api.plugins.JavaLibraryPlugin
import org.gradle.testfixtures.ProjectBuilder
import org.junit.jupiter.api.Test

class WaenaPluginTest {
  @Test
  fun `plugin registers task`() {
    val project = ProjectBuilder.builder().build()
    project.plugins.apply(JavaLibraryPlugin::class.java)
    project.plugins.apply(WaenaRootPlugin::class.java)
    project.plugins.apply(WaenaPublishedPlugin::class.java)

    assertThat(project.tasks.names).contains("jreleaserDeploy")
  }
}
