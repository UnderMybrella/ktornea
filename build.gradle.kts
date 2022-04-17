import dev.brella.kornea.gradle.mavenBrella

buildscript {
    dependencies {
        classpath("org.jetbrains.kotlinx:atomicfu-gradle-plugin:0.17.1")
    }
}

plugins {
    kotlin("multiplatform") version "1.6.20" apply false
    kotlin("jvm") version "1.6.20" apply false
    kotlin("plugin.serialization") version "1.6.20" apply false

    id("dev.brella.kornea") version "1.0.5"
}

allprojects {
    group = "dev.brella"

    repositories {
        mavenCentral()
        mavenBrella()
    }
}

configure(subprojects) {
    apply(plugin = "maven-publish")

    group = "dev.brella"

    configure<PublishingExtension> {
        repositories {
            maven(url = "${rootProject.buildDir}/repo")
        }
    }
}