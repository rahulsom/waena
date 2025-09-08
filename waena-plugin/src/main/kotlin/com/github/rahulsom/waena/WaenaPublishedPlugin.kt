package com.github.rahulsom.waena

import nebula.plugin.contacts.Contact
import nebula.plugin.contacts.ContactsExtension
import nebula.plugin.info.scm.ScmInfoPlugin
import nebula.plugin.release.ReleasePlugin
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.plugins.JavaBasePlugin
import org.gradle.api.plugins.JavaPluginExtension
import org.gradle.api.publish.PublishingExtension
import org.gradle.api.publish.maven.MavenPublication
import org.gradle.api.publish.maven.plugins.MavenPublishPlugin
import org.gradle.api.tasks.bundling.AbstractArchiveTask
import org.gradle.kotlin.dsl.delegateClosureOf
import org.gradle.kotlin.dsl.findByType
import org.gradle.kotlin.dsl.getByType
import org.gradle.plugins.signing.SigningExtension
import org.gradle.plugins.signing.SigningPlugin
import java.util.*
import nebula.plugin.publishing.maven.MavenPublishPlugin as NebulaMavenPublishPlugin

class WaenaPublishedPlugin : Plugin<Project> {
  override fun apply(target: Project) {
    target.plugins.apply(SigningPlugin::class.java)
    target.plugins.apply(ReleasePlugin::class.java)
    target.plugins.apply(NebulaMavenPublishPlugin::class.java)
    target.plugins.withType(JavaBasePlugin::class.java) {
      val javaPluginExtension = target.extensions.getByType(JavaPluginExtension::class.java)
      javaPluginExtension.withJavadocJar()
      javaPluginExtension.withSourcesJar()
    }

    val hasSigningKey = target.hasProperty("signing.keyId")
        || target.findProperty("signingKey") != null
        || target.findProperty("signingKeyB64") != null

    if (hasSigningKey) {
      signProject(target)
    }

    val publishingExtension = target.extensions.findByType<PublishingExtension>()!!
    val rootProject = target.rootProject
    val waenaExtension = rootProject.extensions.getByType(WaenaExtension::class.java)

    publishingExtension.repositories.maven {
      name = "local"
      val rootBuildDir = target.rootProject.layout.buildDirectory.get()
      val releasesRepoUrl = "$rootBuildDir/repos/releases"
      val snapshotsRepoUrl = "$rootBuildDir/repos/snapshots"
      val repoUrl = if (target.version.toString().endsWith("SNAPSHOT")) snapshotsRepoUrl else releasesRepoUrl
      url = target.file(repoUrl).toURI()
    }

    configurePom(target, waenaExtension)

    if (target.rootProject == target) {
      target.rootProject.tasks.findByPath("release")?.dependsOn(":publish")
      target.rootProject.tasks.getByPath("closeSonatypeStagingRepository").mustRunAfter(":publish")
      target.rootProject.tasks.findByPath("jreleaserDeploy")?.mustRunAfter(":publish")
    } else {
      target.rootProject.tasks.findByPath("release")?.dependsOn(":${target.name}:publish")
      target.rootProject.tasks.getByPath("closeSonatypeStagingRepository").mustRunAfter(":${target.name}:publish")
      target.rootProject.tasks.findByPath("jreleaserDeploy")?.mustRunAfter(":${target.name}:publish")
    }

    target.tasks.withType(AbstractArchiveTask::class.java).configureEach {
      isPreserveFileTimestamps = false
      isReproducibleFileOrder = true
    }
  }

  private fun signProject(project: Project) {
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

  data class HostedRepo(val host: String, val repo: Repo)
  data class Repo(val owner: String, val name: String) {
    override fun toString(): String {
      return "$owner/$name"
    }
  }

  private fun configurePom(project: Project, waenaExtension: WaenaExtension) {
    val repoKey = getHostedRepoInfo(project)

    project.afterEvaluate {
      val contactsExtension = project.extensions.getByType<ContactsExtension>()
      if (contactsExtension.people.isEmpty()) {
        contactsExtension.addPerson("${repoKey.repo.owner}@noreply.github.com", delegateClosureOf<Contact> {
          moniker(repoKey.repo.owner)
          roles("owner")
          github("https://${repoKey.host}/${repoKey.repo.owner}")
        })
      }
    }

    project.plugins.withType(MavenPublishPlugin::class.java).configureEach {
      val publishing = project.extensions.getByType<PublishingExtension>()
      publishing.publications.withType(MavenPublication::class.java).configureEach {
        pom {
          name.set("${project.group}:${project.name}")
          description.set(name)
          url.set("https://github.com/${repoKey.repo}")
          licenses {
            license {
              name.set(waenaExtension.license.get().license)
              url.set(waenaExtension.license.get().url)
            }
          }
          scm {
            connection.set("scm:git:https://github.com/${repoKey.repo}")
            developerConnection.set("scm:git:ssh://github.com/${repoKey.repo}.git")
            url.set("https://github.com/${repoKey.repo}")
          }
          issueManagement {
            this.system.set("github")
            this.url.set("https://github.com/${repoKey.repo}/issues")
          }
        }
      }
    }
  }

  fun getHostedRepoInfo(project: Project, origin: String? = null): HostedRepo {
    val scmInfoPlugin = project.rootProject.plugins.getAt(ScmInfoPlugin::class.java)
    val originUrl = origin ?: scmInfoPlugin.findProvider(project).calculateOrigin(project)

    val hostedRepoRegex =
      Regex("^(?:https?://|git://|git@)(?<host>[^/:]+)[:/](?<owner>[^/]+)/(?<repo>[^/]+?)(?:.git)?$")
    val matchResult = hostedRepoRegex.matchEntire(originUrl)
    val (host, repo) = matchResult?.let {
      val host = it.groups["host"]?.value ?: ""
      val owner = it.groups["owner"]?.value ?: ""
      val repoName = it.groups["repo"]?.value?.removeSuffix(".git") ?: ""
      host to Repo(owner, repoName)
    } ?: ("unknown" to Repo("rahulsom", "nothing"))
    return HostedRepo(host, repo)
  }

}
