import dev.brella.kornea.gradle.defineVersions
import dev.brella.kornea.gradle.mavenBrella

buildscript {
    dependencies {
        classpath("org.jetbrains.kotlinx:atomicfu-gradle-plugin:0.17.3")
    }
}

plugins {
    kotlin("multiplatform") version "1.8.20" apply false
    kotlin("jvm") version "1.8.20" apply false
    kotlin("plugin.serialization") version "1.8.20" apply false

    id("dev.brella.kornea") version "2.1.0"
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

defineVersions {
    ktor("2.2.4")
    korneaErrors("4.0.1-alpha")
    korneaIO("5.5.1-alpha")

    "kornea-serialisation-core" .. "2.1.1-alpha"
}