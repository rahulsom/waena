package com.github.rahulsom.waena

import com.dorongold.gradle.tasktree.TaskTreePlugin
import io.github.gradlenexus.publishplugin.NexusPublishExtension
import io.github.gradlenexus.publishplugin.NexusPublishPlugin
import nebula.plugin.contacts.ContactsPlugin
import nebula.plugin.release.ReleasePlugin
import org.gradle.api.GradleException
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.internal.provider.DefaultProviderFactory
import org.gradle.api.provider.Property
import org.gradle.api.provider.Provider
import org.gradle.kotlin.dsl.getByType
import org.gradle.plugins.signing.SigningPlugin
import java.net.URI
import java.time.Duration

class WaenaRootPlugin : Plugin<Project> {
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
        sonatype {
          nexusUrl.set(buildUriProvider(waenaExtension.useCentralPortal, false))
          snapshotRepositoryUrl.set(buildUriProvider(waenaExtension.useCentralPortal, true))
          if (rootProject.hasProperty("sonatypeUsername")) {
            username.convention(rootProject.property("sonatypeUsername") as String)
          }
          if (rootProject.hasProperty("sonatypePassword")) {
            password.convention(rootProject.property("sonatypePassword") as String)
          }
        }
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

  fun buildUriProvider(useCentralPortal: Property<Boolean>, isSnapshot: Boolean): Provider<URI> {
    return DefaultProviderFactory().provider({
      val input = Pair(useCentralPortal.get(), isSnapshot)
      val retval = when (input) {
        Pair(true, true) -> URI("https://central.sonatype.com/repository/maven-snapshots/")
        Pair(true, false) -> URI("https://s01.sonatype.org/service/local/")
        Pair(false, true) -> URI("https://oss.sonatype.org/content/repositories/snapshots/")
        Pair(false, false) -> URI("https://oss.sonatype.org/service/local/")
        else -> throw IllegalStateException("Invalid combination of useCentralPortal and isSnapshot")
      }
      retval
    })
  }

}
