package com.github.rahulsom.waena

import com.dorongold.gradle.tasktree.TaskTreePlugin
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
import java.time.Duration


class WaenaRootPlugin : Plugin<Project> {

  companion object {
    val CENTRAL = Pair("https://central.sonatype.com/repository/maven-snapshots/", "https://central.sonatype.com/service/local/")
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
    rootProject.plugins.apply(NexusPublishPlugin::class.java)
    rootProject.plugins.apply(TaskTreePlugin::class.java)

    val waenaExtension = rootProject.extensions.create("waena", WaenaExtension::class.java, rootProject)

    rootProject.allprojects.forEach { target ->
      target.plugins.apply(ContactsPlugin::class.java)
    }

    val nexusPublishExtension = rootProject.extensions.getByType<NexusPublishExtension>()

    nexusPublishExtension.apply {
      repositories {
        val s = repositories.find { it.name == "sonatype" } ?: repositories.create("sonatype")
        s.apply {
          nexusUrl.unsetConvention().convention(provideUrls(waenaExtension).map { rootProject.uri(it.second) })
          snapshotRepositoryUrl.unsetConvention().convention(provideUrls(waenaExtension).map { rootProject.uri(it.first) })
          if (rootProject.hasProperty("sonatypeUsername")) {
            username.unsetConvention().convention(rootProject.property("sonatypeUsername") as String)
          }
          if (rootProject.hasProperty("sonatypePassword")) {
            password.unsetConvention().convention(rootProject.property("sonatypePassword") as String)
          }
        }
      }

      connectTimeout.unsetConvention().convention(Duration.ofMinutes(3))
      clientTimeout.unsetConvention().convention(Duration.ofMinutes(3))
      transitionCheckOptions.delayBetween.unsetConvention().convention(Duration.ofSeconds(30))
    }

    listOf("candidate", "final").forEach {
      rootProject.tasks.findByPath(it)?.dependsOn("closeAndReleaseSonatypeStagingRepository")
    }

  }

  fun provideUrls(extension: WaenaExtension) = DefaultProvider({
    when (extension.repositoryConfig.get()) {
      WaenaExtension.PublishMode.OSS -> OSS
      WaenaExtension.PublishMode.Central -> CENTRAL
      WaenaExtension.PublishMode.S01 -> S01
    }
  })

}
