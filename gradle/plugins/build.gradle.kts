plugins {
    `kotlin-dsl`
    `java-gradle-plugin`
}

repositories {
    mavenCentral()
}

dependencies {
    testImplementation(libs.bundles.junit)
    testImplementation(libs.assertJ)
}

gradlePlugin {
    plugins {
        create("versionFile") {
            id = "com.github.rahulsom.waena.version-file"
            implementationClass = "com.github.rahulsom.waena.VersionFilePlugin"
        }
    }
}

tasks.withType<Test> {
    useJUnitPlatform()
}
