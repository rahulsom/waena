package com.github.rahulsom.waena

import com.dorongold.gradle.tasktree.TaskTreePlugin
import de.marcphilipp.gradle.nexus.NexusPublishExtension
import de.marcphilipp.gradle.nexus.NexusPublishPlugin
import io.codearte.gradle.nexus.NexusStagingExtension
import io.codearte.gradle.nexus.NexusStagingPlugin
import nebula.plugin.release.ReleasePlugin
import org.gradle.api.GradleException
import org.gradle.api.Plugin
import org.gradle.api.Project
import java.time.Duration
import nebula.plugin.contacts.ContactsPlugin
import org.gradle.kotlin.dsl.getByType

class WaenaRootPlugin : Plugin<Project> {
  override fun apply(target: Project) {
    if (target.rootProject != target) {
      throw GradleException("WaenaRoot can only be applied to a root project")
    }
    configureRootProject(target)
  }

  private fun configureRootProject(rootProject: Project) {
    rootProject.plugins.apply("signing")
    rootProject.plugins.apply(ReleasePlugin::class.java)
    rootProject.plugins.apply(NexusStagingPlugin::class.java)
    rootProject.plugins.apply(TaskTreePlugin::class.java)

    rootProject.extensions.create("waena", WaenaExtension::class.java, rootProject)

    rootProject.allprojects.forEach { target ->
      target.plugins.apply(NexusPublishPlugin::class.java)
      target.plugins.apply(ContactsPlugin::class.java)

      target.extensions.getByType<NexusPublishExtension>().apply {
        repositories {
          sonatype()
        }
        connectTimeout.set(Duration.ofMinutes(3))
        clientTimeout.set(Duration.ofMinutes(3))
      }

    }

    rootProject.extensions.getByType<NexusStagingExtension>().apply {
      username = rootProject.findProperty("sonatypeUsername") as String?
      password = rootProject.findProperty("sonatypePassword") as String?
      repositoryDescription = "Release ${rootProject.group} ${rootProject.version}"
      delayBetweenRetriesInMillis = 30_000
    }

    listOf("candidate", "final").forEach {
      rootProject.tasks.findByPath(it)?.dependsOn("closeAndReleaseRepository")
    }
  }

}
