package com.github.rahulsom.waena

import com.dorongold.gradle.tasktree.TaskTreePlugin
import com.github.rahulsom.waena.WaenaExtension.PublishMode
import io.github.gradlenexus.publishplugin.NexusPublishExtension
import io.github.gradlenexus.publishplugin.NexusPublishPlugin
import nebula.plugin.contacts.ContactsPlugin
import nebula.plugin.release.ReleasePlugin
import org.gradle.api.GradleException
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.internal.provider.DefaultProvider
import org.gradle.kotlin.dsl.getByType
import org.gradle.plugins.signing.SigningPlugin
import org.jreleaser.gradle.plugin.JReleaserExtension
import org.jreleaser.gradle.plugin.JReleaserPlugin
import org.jreleaser.model.Active
import java.time.Duration

class WaenaRootPlugin : Plugin<Project> {

  companion object {
    val CENTRAL = Pair("https://central.sonatype.com/repository/maven-snapshots/", "https://central.sonatype.com/api/v1/publisher")
    val OSS = Pair("https://oss.sonatype.org/content/repositories/snapshots/", "https://oss.sonatype.org/service/local/")
    val S01 = Pair("https://s01.oss.sonatype.org/content/repositories/snapshots/", "https://s01.oss.sonatype.org/service/local/")
  }

  override fun apply(target: Project) {
    if (target.rootProject != target) {
      throw GradleException("WaenaRoot can only be applied to a root project")
    }
    configureRootProject(target)
  }

  private fun configureRootProject(rootProject: Project) {
    rootProject.plugins.apply(SigningPlugin::class.java)
    rootProject.plugins.apply(ReleasePlugin::class.java)
    rootProject.plugins.apply(TaskTreePlugin::class.java)
    val waenaExtension = rootProject.extensions.create("waena", WaenaExtension::class.java, rootProject)

    rootProject.allprojects.forEach { target ->
      target.plugins.apply(ContactsPlugin::class.java)
    }

    configureNexusPublishPlugin(rootProject, waenaExtension)

    rootProject.afterEvaluate {
      if (!useNexusPublishPlugin(rootProject)) {
        configureJReleaser(rootProject)
      }
    }
  }

  private fun Project.configureJReleaser(rootProject: Project) {
    val isSnapshot = rootProject.version.toString().endsWith("-SNAPSHOT")
    rootProject.tasks.getByPath(":initializeSonatypeStagingRepository").enabled = isSnapshot
    rootProject.tasks.getByPath(":closeSonatypeStagingRepository").enabled = isSnapshot
    rootProject.tasks.getByPath(":releaseSonatypeStagingRepository").enabled = isSnapshot
    rootProject.subprojects.forEach { project ->
      project.afterEvaluate {
        project.tasks.findByName("publishNebulaPublicationToSonatypeRepository")?.enabled = isSnapshot
      }
    }
    rootProject.plugins.apply(JReleaserPlugin::class.java)
    val jreleaser = rootProject.extensions.getByType<JReleaserExtension>()
    if (!isSnapshot) {
      jreleaser.project.description.set("Waena Bundle")
      jreleaser.project.copyright.set("2025")
      jreleaser.deploy {
        maven {
          mavenCentral {
            create("sonatype") {
              active.set(Active.ALWAYS)
              url.set(CENTRAL.second)
              stagingRepository("build/repos/releases")
              username.set(rootProject.property("sonatypeUsername") as String)
              password.set(rootProject.property("sonatypePassword") as String)
              sign.set(false)
            }
          }
        }
      }
    }

    project.file("build/jreleaser").mkdirs()

    listOf("candidate", "final").forEach {
      rootProject.tasks.findByPath(it)?.let { r ->
        rootProject.tasks.findByPath("jreleaserDeploy")?.let { c ->
          r.dependsOn(c)

        }
      }
    }
  }

  private fun configureNexusPublishPlugin(rootProject: Project, waenaExtension: WaenaExtension) {
    rootProject.plugins.apply(NexusPublishPlugin::class.java)

    val nexusPublishExtension = rootProject.extensions.getByType<NexusPublishExtension>()
    val sonatypeRepository = nexusPublishExtension.repositories.find { it.name == "sonatype" } ?: nexusPublishExtension.repositories.create("sonatype")
    sonatypeRepository.apply {
      nexusUrl.unsetConvention().convention(provideUrls(waenaExtension).map { rootProject.uri(it.second) })
      snapshotRepositoryUrl.unsetConvention().convention(provideUrls(waenaExtension).map { rootProject.uri(it.first) })
      if (rootProject.hasProperty("sonatypeUsername")) {
        username.unsetConvention().convention(rootProject.property("sonatypeUsername") as String)
      }
      if (rootProject.hasProperty("sonatypePassword")) {
        password.unsetConvention().convention(rootProject.property("sonatypePassword") as String)
      }
    }

    nexusPublishExtension.connectTimeout.unsetConvention().convention(Duration.ofMinutes(3))
    nexusPublishExtension.clientTimeout.unsetConvention().convention(Duration.ofMinutes(3))
    nexusPublishExtension.transitionCheckOptions.delayBetween.unsetConvention().convention(Duration.ofSeconds(30))

    listOf("candidate", "final").forEach {
      rootProject.tasks.findByPath(it)?.let { r ->
        rootProject.tasks.findByPath("closeAndReleaseSonatypeStagingRepository")?.let { c ->
          r.dependsOn(c)
        }
      }
    }
  }

  private fun useNexusPublishPlugin(rootProject: Project): Boolean {
    val publishMode = rootProject.extensions.getByType<WaenaExtension>().publishMode.get()
    return publishMode != PublishMode.Central
  }

  fun provideUrls(extension: WaenaExtension) = DefaultProvider({
    when (extension.publishMode.get()) {
      PublishMode.OSS -> OSS
      PublishMode.Central -> CENTRAL
      PublishMode.S01 -> S01
    }
  })

}
