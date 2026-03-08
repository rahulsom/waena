package com.github.rahulsom.waena

import org.assertj.core.api.Assertions.assertThat
import org.gradle.testfixtures.ProjectBuilder
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File

class CreateVersionFileTaskTest {

    @TempDir
    lateinit var tempDir: File

    @Test
    fun `task creates version file`() {
        val project = ProjectBuilder.builder().withProjectDir(tempDir).build()
        val task = project.tasks.create("createVersionFile", CreateVersionFileTask::class.java)

        val outputDir = File(tempDir, "build/generated")
        task.version.set("1.2.3")
        task.outputDir.set(outputDir)

        task.create()

        val versionFile = File(outputDir, "waena-version.properties")
        assertThat(versionFile).exists()
        assertThat(versionFile.readText()).isEqualTo("waena.version=1.2.3")
    }
}
