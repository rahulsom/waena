package com.github.rahulsom.waena

import nebula.plugin.info.InfoPlugin
import nebula.plugin.publishing.maven.MavenPublishPlugin
import nebula.plugin.release.ReleasePlugin
import org.assertj.core.api.Assertions.assertThat
import org.gradle.plugins.signing.SigningPlugin
import org.gradle.testfixtures.ProjectBuilder
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource

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

  @ParameterizedTest
  @CsvSource(
    "'https://github.com/rahulsom/waena.git', 'rahulsom/waena'",
    "'git://github.com/rahulsom/waena.git', 'rahulsom/waena'",
    "'git@github.com:rahulsom/waena.git', 'rahulsom/waena'",
    "'git@github.com:rahulsom/svg-builder', 'rahulsom/svg-builder'",
    "'https://github.com/rahulsom/waena', 'rahulsom/waena'",
    "'https://github.com/owner/repo.git', 'owner/repo'",
    "'git://github.com/owner/repo.git', 'owner/repo'",
    "'git://github.com/owner/repo', 'owner/repo'",
    "'git@github.com:owner/repo.git', 'owner/repo'",
    "'git@github.com:owner/repo', 'owner/repo'",
    "'https://github.com/owner/repo', 'owner/repo'"
  )
  fun `regex patterns match various GitHub URL formats correctly`(origin: String, expected: String) {
    val githubRegex = Regex("""^(?:https://github\.com/|git://github\.com/|git@github\.com:)(?<owner>[^/]+)/(?<repo>[^/]+?)(?:\.git)?$""")
    val matchResult = githubRegex.matchEntire(origin)
    val repoKey = matchResult?.let {
      val owner = it.groups["owner"]?.value ?: ""
      val repo = it.groups["repo"]?.value?.removeSuffix(".git") ?: ""
      "$owner/$repo"
    } ?: "rahulsom/nothing"

    assertThat(repoKey).isEqualTo(expected)
  }

  @Test
  fun `regex patterns return fallback for non-GitHub URLs`() {
    val origin = "https://bitbucket.org/owner/repo.git"

    val githubRegex = Regex("""^(?:https://github\.com/|git://github\.com/|git@github\.com:)(?<owner>[^/]+)/(?<repo>[^/]+?)(?:\.git)?$""")
    val matchResult = githubRegex.matchEntire(origin)
    val repoKey = matchResult?.let {
      val owner = it.groups["owner"]?.value ?: ""
      val repo = it.groups["repo"]?.value?.removeSuffix(".git") ?: ""
      "$owner/$repo"
    } ?: "rahulsom/nothing"

    assertThat(repoKey).isEqualTo("rahulsom/nothing")
  }
}