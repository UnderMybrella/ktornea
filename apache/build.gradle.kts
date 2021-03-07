plugins {
    kotlin("jvm")
}

apply(plugin = "kotlinx-atomicfu")
apply(plugin = "maven-publish")

version = "1.0.0-alpha"

repositories {
    mavenCentral()
    maven(url = "https://maven.brella.dev")
    maven(url = "https://kotlin.bintray.com/ktor")
}

dependencies {
    implementation(project(":ktornea-utils"))

    implementation("io.ktor:ktor-client-core:1.5.0")
    implementation("io.ktor:ktor-client-serialization:1.5.0")

    implementation("dev.brella:kornea-io:5.2.0-alpha")

    implementation("org.jetbrains.kotlinx:atomicfu:0.15.1")

    implementation("io.ktor:ktor-client-apache:1.5.0")
}

configure<kotlinx.atomicfu.plugin.gradle.AtomicFUPluginExtension> {
    dependenciesVersion = null
}

configure<PublishingExtension> {
    repositories {
        maven(url = "${rootProject.buildDir}/repo")
    }
}