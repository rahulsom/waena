package com.github.rahulsom.waena

import com.dorongold.gradle.tasktree.TaskTreePlugin
import io.github.gradlenexus.publishplugin.NexusPublishExtension
import io.github.gradlenexus.publishplugin.NexusPublishPlugin
import nebula.plugin.contacts.ContactsPlugin
import nebula.plugin.release.ReleasePlugin
import org.gradle.api.GradleException
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.getByType
import java.time.Duration

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
    rootProject.plugins.apply(NexusPublishPlugin::class.java)
    rootProject.plugins.apply(TaskTreePlugin::class.java)

    rootProject.extensions.create("waena", WaenaExtension::class.java, rootProject)

    rootProject.allprojects.forEach { target ->
      target.plugins.apply(ContactsPlugin::class.java)
    }

    rootProject.extensions.getByType<NexusPublishExtension>().apply {
      repositories {
        sonatype()
      }

      connectTimeout.set(Duration.ofMinutes(3))
      clientTimeout.set(Duration.ofMinutes(3))

      transitionCheckOptions {
        delayBetween.set(Duration.ofSeconds(30))
      }
    }

    listOf("candidate", "final").forEach {
      rootProject.tasks.findByPath(it)?.dependsOn("closeAndReleaseSonatypeStagingRepository")
    }
  }

}
