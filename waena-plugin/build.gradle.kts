import com.adarshr.gradle.testlogger.theme.ThemeType

plugins {
  `kotlin-dsl`
  alias(libs.plugins.gradlePublish)
  alias(libs.plugins.testLogger)
}

repositories {
  mavenCentral()
  gradlePluginPortal()
}

dependencies {
  implementation(libs.nebulaRelease)
  implementation(libs.nebulaPublish)
  implementation(libs.nebulaContacts)
  implementation(libs.gradleInfo)
  implementation(libs.nexusPublish)
  implementation(libs.taskTree)
  implementation(libs.jreleaser)
  implementation(libs.nebulaInfo)

  testImplementation(libs.bundles.junit)
  testImplementation(libs.assertJ)
  testImplementation(libs.jgit)
  testImplementation(libs.mockitoCore)
  testImplementation(libs.mockitoKotlin)
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

testlogger {
  theme = if (System.getProperty("idea.active") == "true") ThemeType.PLAIN_PARALLEL else ThemeType.MOCHA_PARALLEL
  slowThreshold = 5000

  showPassed = true
  showSkipped = false
  showFailed = true
}