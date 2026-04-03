@file:OptIn(ExperimentalKotlinGradlePluginApi::class)

import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi

plugins {
    alias(libs.plugins.android.multiplatform.library)
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.gradle.download.task)
    id("tdlib-convention")
    `maven-publish`
}

val tdlibVersion = project.property("tdlib.version") as String
group = "io.xbot"
version = tdlibVersion

tdlib {
    version.set(tdlibVersion)
    jvm()
    android()
    native {
        iosArm64()
        iosSimulatorArm64()
        macosArm64()
        macosX64()
        linuxX64()
        linuxArm64()
    }
}

kotlin {
    android {
        namespace = "io.xbot.tdlib"
        compileSdk {
            version = release(libs.versions.android.compilesdk.get().toInt()) {
                minorApiLevel = 1
            }
        }
        minSdk = libs.versions.android.minsdk.get().toInt()
        withDeviceTestBuilder {
            sourceSetTreeName = "test"
        }.configure {
            instrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        }
    }
    jvm()
    iosArm64(); iosSimulatorArm64()
    macosX64(); macosArm64()
    linuxX64(); linuxArm64()

    sourceSets {
        commonTest.dependencies { implementation(kotlin("test")) }
        getByName("androidDeviceTest") {
            dependencies {
                implementation(libs.androidx.test.runner)
                implementation(libs.androidx.test.ext.junit)
            }
        }
    }
}

publishing {
    publications.withType<MavenPublication> {
        groupId = "io.xbot"
        artifactId = "tdlib-kmp-${name}"
        version = tdlibVersion
        pom {
            name.set("tdlib-kmp")
            description.set("TDLib Kotlin Multiplatform library")
            url.set("https://github.com/xephosbot/tdlib-kmp")
            licenses {
                license {
                    name.set("Boost Software License 1.0")
                    url.set("https://www.boost.org/LICENSE_1_0.txt")
                }
            }
        }
    }
}
