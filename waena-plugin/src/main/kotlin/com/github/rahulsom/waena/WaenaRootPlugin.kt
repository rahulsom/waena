package com.github.rahulsom.waena

import com.dorongold.gradle.tasktree.TaskTreePlugin
import nebula.plugin.contacts.ContactsPlugin
import nebula.plugin.info.InfoPlugin
import nebula.plugin.release.ReleasePlugin
import org.gradle.api.GradleException
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.internal.provider.DefaultProvider
import org.gradle.api.publish.plugins.PublishingPlugin
import org.gradle.language.base.plugins.LifecycleBasePlugin
import org.gradle.kotlin.dsl.getByType
import org.gradle.plugins.signing.SigningPlugin
import org.jreleaser.gradle.plugin.JReleaserExtension
import org.jreleaser.gradle.plugin.JReleaserPlugin
import org.jreleaser.gradle.plugin.tasks.AbstractJReleaserTask
import org.jreleaser.model.Active
import kotlin.math.max

class WaenaRootPlugin : Plugin<Project> {

  companion object {
    val CENTRAL = Pair("https://central.sonatype.com/repository/maven-snapshots/", "https://central.sonatype.com/api/v1/publisher")
    const val CONFIGURE_REMOTE_PUBLISHING_PROPERTY = "waenaConfigureRemotePublishing"
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
    rootProject.plugins.apply(InfoPlugin::class.java)
    rootProject.extensions.create("waena", WaenaExtension::class.java, rootProject)
    val configureRemotePublishing = shouldConfigureRemotePublishing(rootProject)

    rootProject.allprojects.forEach { target ->
      target.plugins.apply(ContactsPlugin::class.java)
    }

    if (configureRemotePublishing) {
      configureJReleaser(rootProject)
    } else {
      rootProject.logger.info(
        "Skipping remote publishing configuration. " +
            "Set -P$CONFIGURE_REMOTE_PUBLISHING_PROPERTY=true to force-enable it."
      )
    }

    enforceVerificationBeforePublishing(rootProject)
  }

  private fun enforceVerificationBeforePublishing(rootProject: Project) {
    rootProject.gradle.projectsEvaluated {
      val verificationTasks = rootProject.allprojects.flatMap { project ->
        project.tasks.matching { task ->
          task.group.equals(LifecycleBasePlugin.VERIFICATION_GROUP, ignoreCase = true)
        }
      }
      rootProject.allprojects.forEach { project ->
        project.tasks.matching { task ->
          task.group.equals(PublishingPlugin.PUBLISH_TASK_GROUP, ignoreCase = true)
        }.configureEach {
          mustRunAfter(verificationTasks)
        }
      }
    }
  }

  private fun configureJReleaser(rootProject: Project) {
    rootProject.plugins.apply(JReleaserPlugin::class.java)
    val jreleaser = rootProject.extensions.getByType<JReleaserExtension>()

    jreleaser.project.description.set("Waena Bundle")
    jreleaser.project.copyright.set("2025")
    jreleaser.deploy {
      maven {
        mavenCentral {
          create("sonatype") {
            active.set(Active.ALWAYS)
            url.set(CENTRAL.second)
            stagingRepository("build/repos/releases")
            if (rootProject.hasProperty("sonatypeUsername")) {
              username.set(rootProject.property("sonatypeUsername") as String)
              password.set(rootProject.property("sonatypePassword") as String)
            }
            sign.set(false)
            retryDelay.set(centralRetryDelay(rootProject))
          }
        }
      }
    }

    val jreleaserOutputDir = rootProject.layout.buildDirectory.dir("jreleaser")
    rootProject.tasks.findByName("jreleaserDeploy")?.doFirst {
      jreleaserOutputDir.get().asFile.mkdirs()
    }

    listOf("candidate", "final").forEach {
      rootProject.tasks.findByPath(it)?.let { r ->
        rootProject.tasks.findByPath("jreleaserDeploy")?.let { c ->
          r.dependsOn(c)
        }
      }
    }
  }

  private fun shouldConfigureRemotePublishing(rootProject: Project): Boolean {
    rootProject.findProperty(CONFIGURE_REMOTE_PUBLISHING_PROPERTY)?.toString()?.let { configuredValue ->
      configuredValue.toBooleanStrictOrNull()?.let { return it }
      rootProject.logger.warn(
        "Ignoring invalid value '$configuredValue' for $CONFIGURE_REMOTE_PUBLISHING_PROPERTY. " +
            "Expected true or false."
      )
    }

    if (rootProject.gradle.parent != null) {
      return false
    }

    val requestedTasks = rootProject.gradle.startParameter.taskNames
    if (requestedTasks.isEmpty()) {
      return true
    }
    return requestedTasks
      .map { taskPath -> taskPath.substringAfterLast(':') }
      .any { taskName ->
        taskName.equals("snapshot", ignoreCase = true)
            || taskName.equals("candidate", ignoreCase = true)
            || taskName.equals("final", ignoreCase = true)
            || taskName.contains("publish", ignoreCase = true)
            || taskName.contains("release", ignoreCase = true)
            || taskName.contains("sonatype", ignoreCase = true)
            || taskName.contains("jreleaser", ignoreCase = true)
      }
  }

  fun centralRetryDelay(rootProject: Project) = DefaultProvider({
    max(30, rootProject.subprojects.size * 15)
  })

}
