pluginManagement {
    repositories {
        maven {
            name = "Maven Central (Google Mirror)"
            url = uri("https://maven-central.storage-download.googleapis.com/maven2/")
        }
        gradlePluginPortal()
    }
}

plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "0.8.0"
}

rootProject.name = "com.fcfb.arceus"
