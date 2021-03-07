buildscript {
    dependencies {
        classpath("org.jetbrains.kotlinx:atomicfu-gradle-plugin:0.15.1")
    }
}

plugins {
    kotlin("multiplatform") version "1.4.30" apply false
    kotlin("jvm") version "1.4.30" apply false
    kotlin("plugin.serialization") version "1.4.30" apply false
}

group = "dev.brella"