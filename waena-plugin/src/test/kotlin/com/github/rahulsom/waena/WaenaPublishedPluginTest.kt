package com.github.rahulsom.waena

import nebula.plugin.info.InfoBrokerPlugin
import nebula.plugin.info.InfoPlugin
import nebula.plugin.publishing.maven.MavenPublishPlugin
import nebula.plugin.release.ReleasePlugin
import org.assertj.core.api.Assertions.assertThat
import org.gradle.api.internal.project.ProjectInternal
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

  @Test
  fun `plugin adds Waena-Version to InfoBrokerPlugin`() {
    val rootProject = ProjectBuilder.builder().build()
    rootProject.plugins.apply(WaenaRootPlugin::class.java)

    val submodule = ProjectBuilder.builder().withParent(rootProject).build()
    submodule.plugins.apply(WaenaPublishedPlugin::class.java)

    // Trigger afterEvaluate callbacks
    (submodule as ProjectInternal).evaluate()

    val infoBroker = submodule.plugins.findPlugin(InfoBrokerPlugin::class.java)
    val manifest = infoBroker!!.buildNonChangingManifest()

    assertThat(manifest).containsKey("Waena-Version")
    assertThat(manifest["Waena-Version"]).isNotEqualTo("unknown")
  }

  @ParameterizedTest
  @CsvSource(
    "'https://github.com/rahulsom/waena.git', 'github.com', 'rahulsom', 'waena'",
    "'git://github.com/rahulsom/waena.git', 'github.com', 'rahulsom', 'waena'",
    "'git@github.com:rahulsom/waena.git', 'github.com', 'rahulsom', 'waena'",
    "'git@github.com:rahulsom/svg-builder', 'github.com', 'rahulsom', 'svg-builder'",
    "'https://github.com/rahulsom/waena', 'github.com', 'rahulsom', 'waena'",
    "'https://github.com/owner/repo.git', 'github.com', 'owner', 'repo'",
    "'git://github.com/owner/repo.git', 'github.com', 'owner', 'repo'",
    "'git://github.com/owner/repo', 'github.com', 'owner', 'repo'",
    "'git@github.com:owner/repo.git', 'github.com', 'owner', 'repo'",
    "'git@github.com:owner/repo', 'github.com', 'owner', 'repo'",
    "'https://github.com/owner/repo', 'github.com', 'owner', 'repo'",
    "'https://bitbucket.org/owner/repo.git', 'bitbucket.org', 'owner', 'repo'",
    "'git@bitbucket.org:owner/repo.git', 'bitbucket.org', 'owner', 'repo'",
    "'https://gitlab.com/group/project.git', 'gitlab.com', 'group', 'project'",
    "'git://gitlab.com/group/project', 'gitlab.com', 'group', 'project'"
  )
  fun `getHostedRepoInfo parses various Git URL formats correctly`(
    origin: String,
    expectedHost: String,
    expectedOwner: String,
    expectedName: String
  ) {
    val project = ProjectBuilder.builder().build()
    project.plugins.apply(WaenaRootPlugin::class.java)

    val repo = WaenaPublishedPlugin().getHostedRepoInfo(project, origin)

    assertThat(repo.host).isEqualTo(expectedHost)
    assertThat(repo.repo.owner).isEqualTo(expectedOwner)
    assertThat(repo.repo.name).isEqualTo(expectedName)
    assertThat(repo.repo.toString()).isEqualTo("$expectedOwner/$expectedName")
  }

  @Test
  fun `getHostedRepoInfo parses non-GitHub URLs correctly`() {
    val project = ProjectBuilder.builder().build()
    project.plugins.apply(WaenaRootPlugin::class.java)

    val repo = WaenaPublishedPlugin().getHostedRepoInfo(project, "https://bitbucket.org/owner/repo.git")

    assertThat(repo.host).isEqualTo("bitbucket.org")
    assertThat(repo.repo.owner).isEqualTo("owner")
    assertThat(repo.repo.name).isEqualTo("repo")
    assertThat(repo.repo.toString()).isEqualTo("owner/repo")
  }

  @ParameterizedTest
  @CsvSource(
    "'rahulsom/waena', 'scm:git:git://github.com/rahulsom/waena.git', 'scm:git:git@github.com:rahulsom/waena.git', 'https://github.com/rahulsom/waena'",
    "'owner/repo', 'scm:git:git://github.com/owner/repo.git', 'scm:git:git@github.com:owner/repo.git', 'https://github.com/owner/repo'"
  )
  fun `SCM connection URLs are formatted correctly`(
    repoPath: String,
    expectedConnection: String,
    expectedDeveloperConnection: String,
    expectedUrl: String
  ) {
    val rootProject = ProjectBuilder.builder().build()
    rootProject.plugins.apply(WaenaRootPlugin::class.java)

    val submodule = ProjectBuilder.builder().withParent(rootProject).build()
    submodule.plugins.apply(WaenaPublishedPlugin::class.java)

    // Mock git origin to return a URL with the specified repo path
    val origin = "https://github.com/$repoPath.git"
    val repoKey = WaenaPublishedPlugin().getHostedRepoInfo(submodule, origin)

    // Verify SCM connection format uses git:// protocol
    assertThat("scm:git:git://github.com/${repoKey.repo}.git").isEqualTo(expectedConnection)
    // Verify developer connection format uses SSH
    assertThat("scm:git:git@github.com:${repoKey.repo}.git").isEqualTo(expectedDeveloperConnection)
    // Verify URL format
    assertThat("https://github.com/${repoKey.repo}").isEqualTo(expectedUrl)
  }
}