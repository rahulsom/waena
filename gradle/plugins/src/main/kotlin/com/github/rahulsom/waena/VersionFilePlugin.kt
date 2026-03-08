package com.github.rahulsom.waena

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.register

class VersionFilePlugin : Plugin<Project> {
    override fun apply(project: Project) {
        val projectVersion = project.provider { project.version.toString() }
        val projectBuildDir = project.layout.buildDirectory
        project.tasks.register<CreateVersionFileTask>("createVersionFile") {
            version.convention(projectVersion)
            outputDir.convention(projectBuildDir.dir("generated-src/main/resources"))
        }
    }
}
