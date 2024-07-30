plugins {
  `kotlin-dsl`
  id("com.gradle.plugin-publish") version "1.2.1"
}

repositories {
  mavenCentral()
  gradlePluginPortal()
}

dependencies {
  implementation("com.netflix.nebula:nebula-release-plugin:19.0.10")
  implementation("com.netflix.nebula:nebula-publishing-plugin:21.0.0")
  implementation("com.netflix.nebula:gradle-contacts-plugin:7.0.1")
  implementation("com.netflix.nebula:gradle-info-plugin:13.1.2")
  implementation("io.github.gradle-nexus:publish-plugin:2.0.0")
  implementation("com.dorongold.plugins:task-tree:4.0.0")

  testImplementation("org.junit.jupiter:junit-jupiter-api:5.10.3")
  testImplementation("org.assertj:assertj-core:3.26.3")
  testImplementation("org.eclipse.jgit:org.eclipse.jgit:5.+")
  testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:5.10.3")
}

gradlePlugin {
  val waenaRoot by plugins.creating {
    id = "com.github.rahulsom.waena.root"
    implementationClass = "com.github.rahulsom.waena.WaenaRootPlugin"
    displayName = "Waena Plugin for Root Modules"
    description = "Configures the root project to publish to Maven Central."
    tags.set(mutableSetOf("publishing", "mavencentral"))
  }
  val waenaPublished by plugins.creating {
    id = "com.github.rahulsom.waena.published"
    implementationClass = "com.github.rahulsom.waena.WaenaPublishedPlugin"
    displayName = "Waena Plugin for Published Modules"
    description = "Marks a module as one to be published to Maven Central."
    tags.set(mutableSetOf("publishing", "mavencentral"))
  }
  website.set("https://github.com/rahulsom/waena")
  vcsUrl.set("https://github.com/rahulsom/waena.git")
}

rootProject.tasks.getByName("final").dependsOn(project.tasks.getByName("publishPlugins"))

tasks.withType<Test>() {
  useJUnitPlatform()
}
