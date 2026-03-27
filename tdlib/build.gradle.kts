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

val tdlibDeps = TdlibDependencies(project, tdlibVersion)

val jniExtractTask = tdlibDeps.jniForHost()

val iosArm64ExtractTask = tdlibDeps.nativeFor(TdlibOS.IOS, TdlibArch.Arm64)
val iosSimArm64ExtractTask = tdlibDeps.nativeFor(TdlibOS.IOS, TdlibArch.Arm64, isSimulator = true)
val macosArm64ExtractTask = tdlibDeps.nativeFor(TdlibOS.MacOS, TdlibArch.Arm64)
val macosX64ExtractTask = tdlibDeps.nativeFor(TdlibOS.MacOS, TdlibArch.X64)
val linuxX64ExtractTask = tdlibDeps.nativeFor(TdlibOS.Linux, TdlibArch.X64)
val linuxArm64ExtractTask = tdlibDeps.nativeFor(TdlibOS.Linux, TdlibArch.Arm64)

val androidArm64ExtractTask = tdlibDeps.androidFor("arm64-v8a")
val androidArm32ExtractTask = tdlibDeps.androidFor("armeabi-v7a")
val androidX86ExtractTask = tdlibDeps.androidFor("x86")
val androidX86_64ExtractTask = tdlibDeps.androidFor("x86_64")

kotlin {
    applyHierarchyTemplate(tdlibSourceSetHierarchyTemplate)

    android {
        namespace = "com.xbot.tdlib"
        compileSdk {
            version = release(libs.versions.android.compilesdk.get().toInt()) {
                minorApiLevel = 1
            }
        }
        minSdk = libs.versions.android.minsdk.get().toInt()
    }

    jvm()
    iosArm64()
    iosSimulatorArm64()
    macosX64()
    macosArm64()
    linuxX64()
    linuxArm64()

    sourceSets {
        commonTest.dependencies {
            implementation(kotlin("test"))
        }
    }
}

val ctx = TdlibProjectContext(project, kotlin, tdlibDeps, tdlibVersion)

data class NativeTargetDef(
    val name: String,
    val os: TdlibOS,
    val arch: TdlibArch,
    val extractTask: String,
    val isSimulator: Boolean = false,
)

val nativeTargets = listOf(
    NativeTargetDef("iosArm64", TdlibOS.IOS, TdlibArch.Arm64, iosArm64ExtractTask),
    NativeTargetDef("iosSimulatorArm64", TdlibOS.IOS, TdlibArch.Arm64, iosSimArm64ExtractTask, isSimulator = true),
    NativeTargetDef("macosArm64", TdlibOS.MacOS, TdlibArch.Arm64, macosArm64ExtractTask),
    NativeTargetDef("macosX64", TdlibOS.MacOS, TdlibArch.X64,   macosX64ExtractTask),
    NativeTargetDef("linuxX64", TdlibOS.Linux, TdlibArch.X64,   linuxX64ExtractTask),
    NativeTargetDef("linuxArm64", TdlibOS.Linux, TdlibArch.Arm64, linuxArm64ExtractTask),
)

nativeTargets.forEach { def ->
    val target = kotlin.targets.getByName(def.name) as org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTarget
    ctx.configureNativeTarget(def.os, def.arch, target, def.isSimulator)
    ctx.wireNativeExtractTask(def.extractTask, target)
}

ctx.configureJvmTarget(jniExtractTask)

ctx.configureAndroidTarget(mapOf(
    "arm64-v8a" to androidArm64ExtractTask,
    "armeabi-v7a" to androidArm32ExtractTask,
    "x86" to androidX86ExtractTask,
    "x86_64" to androidX86_64ExtractTask,
))

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
