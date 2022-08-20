package com.github.rahulsom.waena

import de.marcphilipp.gradle.nexus.NexusPublishPlugin
import nebula.plugin.release.ReleasePlugin
import nebula.plugin.info.InfoPlugin
import nebula.plugin.info.scm.ScmInfoPlugin
import nebula.plugin.publishing.publications.JavadocJarPlugin
import nebula.plugin.publishing.publications.SourceJarPlugin
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.publish.PublishingExtension
import org.gradle.api.publish.maven.MavenPublication
import org.gradle.api.publish.maven.plugins.MavenPublishPlugin
import org.gradle.kotlin.dsl.findByType
import org.gradle.kotlin.dsl.getByType
import org.gradle.plugins.signing.SigningExtension
import java.util.*
import nebula.plugin.publishing.maven.MavenPublishPlugin as NebulaMavenPublishPlugin

class WaenaPublishedPlugin : Plugin<Project> {
  override fun apply(target: Project) {
    target.plugins.apply(NexusPublishPlugin::class.java)
    target.plugins.apply("signing")
    target.plugins.apply(ReleasePlugin::class.java)
    target.plugins.apply(NebulaMavenPublishPlugin::class.java)
    target.plugins.apply(InfoPlugin::class.java)
    target.plugins.apply(JavadocJarPlugin::class.java)
    target.plugins.apply(SourceJarPlugin::class.java)

    val hasSigningKey = target.hasProperty("signing.keyId")
        || target.findProperty("signingKey") != null
        || target.findProperty("signingKeyB64") != null

    if (hasSigningKey) {
      signProject(target)
    }

    target.extensions.findByType<PublishingExtension>()?.apply {
      repositories {
        maven {
          name = "local"
          val releasesRepoUrl = "${target.rootProject.buildDir}/repos/releases"
          val snapshotsRepoUrl = "${target.rootProject.buildDir}/repos/snapshots"
          val repoUrl = if (target.version.toString().endsWith("SNAPSHOT")) snapshotsRepoUrl else releasesRepoUrl
          url = target.file(repoUrl).toURI()
        }
      }
    }

    val waenaExtension = target.rootProject.extensions.getByType(WaenaExtension::class.java)
    configurePom(target, waenaExtension)

    if (target.rootProject == target) {
      target.rootProject.tasks.findByPath("release")?.dependsOn(":publish")
      target.rootProject.tasks.getByPath("closeRepository").mustRunAfter(":publish")
    } else {
      target.rootProject.tasks.findByPath("release")?.dependsOn(":${target.name}:publish")
      target.rootProject.tasks.getByPath("closeRepository").mustRunAfter(":${target.name}:publish")
    }
  }

  fun signProject(project: Project) {
    project.extensions.findByType<SigningExtension>()?.apply {
      val signingKeyId = project.findProperty("signingKeyId") as String?
      val signingKeyRaw = project.findProperty("signingKey") as String?
      val signingKeyB64 = project.findProperty("signingKeyB64") as String?

      val signingKey = signingKeyRaw ?: signingKeyB64?.let { String(Base64.getDecoder().decode(it)) }
      val signingPassword = project.findProperty("signingPassword") as String?

      if (signingKeyId != null) {
        useInMemoryPgpKeys(signingKeyId, signingKey, signingPassword)
      } else if (signingKey != null) {
        useInMemoryPgpKeys(signingKey, signingPassword!!)
      }
      sign(project.extensions.getByType<PublishingExtension>().publications.getByName("nebula"))
    }
  }

  private fun configurePom(project: Project, waenaExtension: WaenaExtension) {
    val repoKey = getGithubRepoKey(project)

    project.plugins.withType(MavenPublishPlugin::class.java).forEach {
      val publishing = project.extensions.getByType<PublishingExtension>()
      publishing.publications.withType(MavenPublication::class.java).forEach { mavenPublication ->
        mavenPublication.pom {
          name.set("${project.group}:${project.name}")
          description.set(name)
          url.set("https://github.com/$repoKey")
          licenses {
            license {
              name.set(waenaExtension.license.get().license)
              url.set(waenaExtension.license.get().url)
            }
          }
          scm {
            connection.set("scm:git:https://github.com/$repoKey")
            developerConnection.set("scm:git:ssh://github.com/$repoKey.git")
            url.set("https://github.com/$repoKey")
          }
          issueManagement {
            this.system.set("github")
            this.url.set("https://github.com/$repoKey/issues")
          }
        }
      }
    }
  }

  private fun getGithubRepoKey(project: Project): String {
    val scmInfoPlugin = project.plugins.getAt(ScmInfoPlugin::class.java)
    val origin = scmInfoPlugin.findProvider(project).calculateOrigin(project)

    val matchingRegex = listOf(
      Regex("https://github.com/([^/]+)/([^/]+)"),
      Regex("git@github.com:([^/]+)/([^/]+)\\.git"),
      Regex("https://github.com/([^/]+)/([^/]+)\\.git"),
      Regex("git://github.com/([^/]+)/([^/]+)\\.git")
      ).find { origin.matches(it) }
    val message = matchingRegex?.matchEntire(origin)
    val repoKey = message?.let { it.groupValues[1] + "/" + it.groupValues[2] } ?: "rahulsom/nothing"
    return repoKey
  }

}
