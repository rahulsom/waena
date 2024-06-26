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
  implementation("com.netflix.nebula:gradle-info-plugin:13.1.1")
  implementation("io.codearte.gradle.nexus:gradle-nexus-staging-plugin:0.30.0")
  implementation("de.marcphilipp.gradle:nexus-publish-plugin:0.4.0")
  implementation("gradle.plugin.com.dorongold.plugins:task-tree:1.5")

  testImplementation("org.jetbrains.kotlin:kotlin-test")
  testImplementation("org.jetbrains.kotlin:kotlin-test-junit")
}

gradlePlugin {
  val waenaRoot by plugins.creating {
    id = "com.github.rahulsom.waena.root"
    implementationClass = "com.github.rahulsom.waena.WaenaRootPlugin"
    displayName = "Waena Plugin for Root Modules"
    description = "Configures the root project to publish to Maven Central."
  }
  val waenaPublished by plugins.creating {
    id = "com.github.rahulsom.waena.published"
    implementationClass = "com.github.rahulsom.waena.WaenaPublishedPlugin"
    displayName = "Waena Plugin for Published Modules"
    description = "Marks a module as one to be published to Maven Central."
  }
  website.set("https://github.com/rahulsom/waena")
  vcsUrl.set("https://github.com/rahulsom/waena.git")
}

val functionalTestSourceSet = sourceSets.create("functionalTest") {
}

gradlePlugin.testSourceSets(functionalTestSourceSet)
configurations["functionalTestImplementation"].extendsFrom(configurations["testImplementation"])

val functionalTest by tasks.registering(Test::class) {
  testClassesDirs = functionalTestSourceSet.output.classesDirs
  classpath = functionalTestSourceSet.runtimeClasspath
}

tasks.check {
  dependsOn(functionalTest)
}

rootProject.tasks.getByName("final").dependsOn(project.tasks.getByName("publishPlugins"))
