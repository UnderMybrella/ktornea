import dev.brella.kornea.gradle.kotlinxSerialisationModules
import dev.brella.kornea.gradle.versioned

plugins {
    kotlin("multiplatform")
    kotlin("plugin.serialization")
}

apply(plugin = "maven-publish")

group = "dev.brella"
version = "1.0.0-alpha"

repositories {
    mavenCentral()
    maven(url = "https://maven.brella.dev")
//    maven(url = "https://kotlin.bintray.com/ktor")
}

kotlin {
    jvm {
        compilations.all {
            kotlinOptions.jvmTarget = "1.8"
        }
    }
    js(IR) {
        browser {
            testTask {
                useKarma {
                    useChromeHeadless()
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
                kotlinxSerialisationModules {
                    implementation(core())
                }

                api(project(":ktornea-http"))
                implementation(versioned("dev.brella:kornea-serialisation-core", "kornea-serialisation-core"))
            }
        }
        val jvmMain by getting {
            dependencies {
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