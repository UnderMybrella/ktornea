import dev.brella.kornea.gradle.versioned

plugins {
    kotlin("multiplatform")
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
                api(versioned("io.ktor:ktor-http", "ktor"))
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