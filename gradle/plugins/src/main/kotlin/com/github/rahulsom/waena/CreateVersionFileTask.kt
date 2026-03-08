package com.github.rahulsom.waena

import org.gradle.api.DefaultTask
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.TaskAction

abstract class CreateVersionFileTask : DefaultTask() {

    @get:Input
    abstract val version: Property<String>

    @get:OutputDirectory
    abstract val outputDir: DirectoryProperty

    @TaskAction
    fun create() {
        val dir = outputDir.get().asFile
        if (!dir.exists()) {
            dir.mkdirs()
        }
        val versionFile = dir.resolve("waena-version.properties")
        versionFile.writeText("waena.version=${version.get()}")
    }
}
