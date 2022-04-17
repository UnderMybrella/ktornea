import dev.brella.kornea.gradle.korneaIO
import dev.brella.kornea.gradle.mavenBrella
import dev.brella.kornea.gradle.projectFrom

plugins {
    kotlin("multiplatform")
    id("dev.brella.kornea")
}

apply(plugin = "kotlinx-atomicfu")
apply(plugin = "maven-publish")

group = "dev.brella"
version = "1.0.0-alpha"

repositories {
    mavenCentral()
    mavenBrella()
//    maven(url = "https://kotlin.bintray.com/ktor")
}

kotlin {
    jvm {
        compilations.all {
            kotlinOptions.jvmTarget = "1.8"
        }
        testRuns["test"].executionTask.configure {
            useJUnit()
        }
    }
    js(BOTH) {
        browser {
            testTask {
                useKarma {
                    useChromeHeadless()
                    webpackConfig.cssSupport.enabled = true
                }
            }
        }
    }
//    val hostOs = System.getProperty("os.name")
//    val isMingwX64 = hostOs.startsWith("Windows")
//    val nativeTarget = when {
//        hostOs == "Mac OS X" -> macosX64("native")
//        hostOs == "Linux" -> linuxX64("native")
//        isMingwX64 -> mingwX64("native")
//        else -> throw GradleException("Host OS is not supported in Kotlin/Native.")
//    }

    sourceSets {
        val commonMain by getting {
            dependencies {
//                api(project)
//                api(projectFrom("client", "core"))
                api(projectFrom("ktornea", "client", "core"))
                implementation("org.jetbrains.kotlinx:atomicfu:0.17.1")

                implementation(korneaIO("5.5.1-alpha"))
            }
        }
        val commonTest by getting {
            dependencies {
                implementation(kotlin("test-common"))
                implementation(kotlin("test-annotations-common"))
            }
        }
        val jvmMain by getting {
            dependencies {
            }
        }
        val jvmTest by getting {
            dependencies {
                implementation(kotlin("test-junit"))
            }
        }
        val jsMain by getting
        val jsTest by getting {
            dependencies {
                implementation(kotlin("test-js"))
            }
        }

        all {
            languageSettings.apply {
                optIn("kotlin.RequiresOptIn")
                explicitApi()
            }
        }
//        val nativeMain by getting
//        val nativeTest by getting
    }
}

configure<kotlinx.atomicfu.plugin.gradle.AtomicFUPluginExtension> {
    dependenciesVersion = null
}