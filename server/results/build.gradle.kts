import dev.brella.kornea.gradle.korneaErrorsModule
import dev.brella.kornea.gradle.projectFrom
import dev.brella.kornea.gradle.versioned

plugins {
    kotlin("multiplatform")
    id("dev.brella.kornea")
}

apply(plugin = "maven-publish")

group = "dev.brella"
version = "1.0.2-indev"

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
        testRuns["test"].executionTask.configure {
            useJUnit()
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
                api(projectFrom("ktornea", "server", "core"))

                implementation(korneaErrorsModule())
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