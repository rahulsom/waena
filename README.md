# Waena

Gradle plugins for publishing to Maven Central

## Setup

There are 2 plugins that make up this project.

### Waena Root

This needs to be applied only to the root project.

```groovy
plugins {
  id("com.github.rahulsom.waena.root").version("0.3.0")
}

contacts {
    validateEmails = true
    'rahulsom@noreply.github.com' {
        moniker("Rahul Somasunderam")
        roles("owner")
        github("https://github.com/rahulsom")
    }
}
```

### Waena Published

This needs to be applied to each module in the project that needs to be published.

```groovy
plugins {
  id("com.github.rahulsom.waena.published").version("0.3.0")
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
