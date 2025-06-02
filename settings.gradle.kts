plugins {
    id("com.gradle.develocity") version "4.0.2"
}

develocity {
    buildScan {
        termsOfUseUrl.set("https://gradle.com/terms-of-service")
        termsOfUseAgree.set("yes")
        buildScanPublished {
            file("build").mkdirs()
            file("build/gradle-scan.md").writeText(
                """Gradle Build Scan - [`${this.buildScanId}`](${this.buildScanUri})"""
            )
        }
    }
}

rootProject.name = "waena"
include("waena-plugin")
