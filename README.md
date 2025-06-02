# Waena

[![GitHub commits since latest release](https://img.shields.io/github/commits-since/rahulsom/waena/latest?style=for-the-badge)](https://github.com/rahulsom/waena/releases/new)

Gradle plugins for publishing to Maven Central

## Setup

There are 2 plugins that make up this project.

### Waena Root

[![Gradle Plugin Portal](https://img.shields.io/gradle-plugin-portal/v/com.github.rahulsom.waena.root?style=for-the-badge)](https://plugins.gradle.org/plugin/com.github.rahulsom.waena.root)

This needs to be applied only to the root project.

```groovy
plugins {
  id("com.github.rahulsom.waena.root").version("<VERSION>")
}

allprojects {
  group = 'TODO'
}

contacts {
    validateEmails = true
    'todo@noreply.github.com' {
        moniker("To Do")
        roles("owner")
        github("https://github.com/todo")
    }
}
```

### Waena Published

[![Gradle Plugin Portal](https://img.shields.io/gradle-plugin-portal/v/com.github.rahulsom.waena.published?style=for-the-badge)](https://plugins.gradle.org/plugin/com.github.rahulsom.waena.published)

This needs to be applied to each module in the project that needs to be published.

```groovy
plugins {
  id("com.github.rahulsom.waena.published").version("<VERSION>")
}

description = "TODO"

// Optional (to customize license)
waena {
    license.set(WaenaExtension.License.Apache2)
    // By default, publishMode is set to OSS. You can set it to S01 or Central
    // if your maven central is setup for something other than oss.sonatype.org
    publishMode.set(WaenaExtension.PublishMode.Central)
}
```

## Usage

### Environment Variables

Set these 4 environment variables

```shell
export ORG_GRADLE_PROJECT_sonatypeUsername=???
export ORG_GRADLE_PROJECT_sonatypePassword=???
export ORG_GRADLE_PROJECT_signingKey=???
export ORG_GRADLE_PROJECT_signingPassword=???
```

### Publishing snapshots

```shell
./gradlew snapshot
```

### Publishing releases

```shell
git tag v1.2.3 # Any semver compatible version prefixed by `v`
./gradlew -Prelease.useLastTag=true build final --stacktrace
```

## Implementation Details

Under the hood, this uses several other plugins and ties them all together such that you can release to maven central easily

* [nebula.release](https://plugins.gradle.org/plugin/nebula.release)
* [nebula.maven-publish](https://plugins.gradle.org/plugin/nebula.maven-publish)
* [nebula.contacts](https://plugins.gradle.org/plugin/nebula.contacts)
* [nebula.info](https://plugins.gradle.org/plugin/nebula.info)
* [nexus-staging](https://plugins.gradle.org/plugin/io.codearte.nexus-staging)
* [nexus-publish](https://plugins.gradle.org/plugin/de.marcphilipp.nexus-publish)
