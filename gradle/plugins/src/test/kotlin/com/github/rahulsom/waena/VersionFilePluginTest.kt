package com.github.rahulsom.waena

import org.assertj.core.api.Assertions.assertThat
import org.gradle.testfixtures.ProjectBuilder
import org.junit.jupiter.api.Test

class VersionFilePluginTest {

    @Test
    fun `plugin registers task`() {
        val project = ProjectBuilder.builder().build()
        project.plugins.apply("com.github.rahulsom.waena.version-file")

        val task = project.tasks.findByName("createVersionFile")
        assertThat(task).isNotNull()
        assertThat(task).isInstanceOf(CreateVersionFileTask::class.java)
    }

    @Test
    fun `plugin configures task conventions`() {
        val project = ProjectBuilder.builder().build()
        project.version = "1.0.0"
        project.plugins.apply("com.github.rahulsom.waena.version-file")

        val task = project.tasks.getByName("createVersionFile") as CreateVersionFileTask
        assertThat(task.version.get()).isEqualTo("1.0.0")
        assertThat(task.outputDir.get().asFile.path).contains("generated-src/main/resources")
    }
}
