import org.jetbrains.intellij.platform.gradle.TestFrameworkType

plugins {
    kotlin("jvm") version "2.2.20"
    id("org.jetbrains.intellij.platform") version "2.19.0"
}

group = "dev.projectpins"
version = "0.3.7"

repositories {
    mavenCentral()
    intellijPlatform { defaultRepositories() }
}

dependencies {
    intellijPlatform {
        val localIde = providers.gradleProperty("localIdePath")
        if (localIde.isPresent) local(localIde.get()) else webstorm("2025.3.1.1")
        testFramework(TestFrameworkType.Platform)
        pluginVerifier()
    }
    testImplementation("junit:junit:4.13.2")
}

kotlin {
    jvmToolchain(21)
    compilerOptions { jvmDefault = org.jetbrains.kotlin.gradle.dsl.JvmDefaultMode.NO_COMPATIBILITY }
}

intellijPlatform {
    pluginConfiguration {
        name = "Project Pins"
        ideaVersion { sinceBuild = "253"; untilBuild = "253.*" }
        changeNotes = "Compact Project View pin row with adaptive badges, native edit/remove popup, duplicate prevention, optional file opening and double-click scroll-to-top."
    }
    publishing { token = providers.environmentVariable("JETBRAINS_MARKETPLACE_TOKEN") }
    pluginVerification {
        ides {
            val localIde = providers.gradleProperty("localIdePath")
            if (localIde.isPresent) local(localIde.get()) else create(org.jetbrains.intellij.platform.gradle.IntelliJPlatformType.WebStorm, "2025.3.1.1")
        }
    }
}

tasks.test { maxHeapSize = "2g" }
