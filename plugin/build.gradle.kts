plugins {
    `kotlin-dsl`
}

repositories {
    mavenCentral()
    gradlePluginPortal()
}

dependencies {
    implementation("com.netflix.nebula:nebula-release-plugin:15.3.1")
    implementation("com.netflix.nebula:nebula-publishing-plugin:17.3.2")
    implementation("com.netflix.nebula:gradle-contacts-plugin:5.1.0")
    implementation("com.netflix.nebula:gradle-info-plugin:9.3.0")
    implementation("io.codearte.gradle.nexus:gradle-nexus-staging-plugin:0.22.0")
    implementation("de.marcphilipp.gradle:nexus-publish-plugin:0.4.0")

    testImplementation("org.jetbrains.kotlin:kotlin-test")
    testImplementation("org.jetbrains.kotlin:kotlin-test-junit")
}

gradlePlugin {
    val waenaRoot by plugins.creating {
        id = "com.github.rahulsom.waena.root"
        implementationClass = "com.github.rahulsom.waena.WaenaRootPlugin"
    }
    val waenaPublished by plugins.creating {
        id = "com.github.rahulsom.waena.published"
        implementationClass = "com.github.rahulsom.waena.WaenaPublishedPlugin"
    }
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
