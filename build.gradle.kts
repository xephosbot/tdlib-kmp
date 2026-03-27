@file:OptIn(ExperimentalKotlinGradlePluginApi::class)

import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi

plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.android.kmp.library)
    id("de.undercouch.download")
    `maven-publish`
}

val tdlibVersion = project.property("tdlib.version") as String
group = "org.xephosbot"
version = tdlibVersion

// ---------------------------------------------------------------------------
// TDLib native artifact download & extraction (Skiko-style lazy approach)
// ---------------------------------------------------------------------------
val tdlibDeps = TdlibDependencies(project, tdlibVersion)

// JNI for current host (used by JVM target at test/run time)
val jniExtractTask = tdlibDeps.jniForHost()

// Native targets — register downloads (tasks execute only when depended upon)
val iosArm64ExtractTask      = tdlibDeps.nativeFor(TdlibOS.IOS, TdlibArch.Arm64)
val iosSimArm64ExtractTask   = tdlibDeps.nativeFor(TdlibOS.IOS, TdlibArch.Arm64, isSimulator = true)
val macosArm64ExtractTask    = tdlibDeps.nativeFor(TdlibOS.MacOS, TdlibArch.Arm64)
val macosX64ExtractTask      = tdlibDeps.nativeFor(TdlibOS.MacOS, TdlibArch.X64)
val linuxX64ExtractTask      = tdlibDeps.nativeFor(TdlibOS.Linux, TdlibArch.X64)
val linuxArm64ExtractTask    = tdlibDeps.nativeFor(TdlibOS.Linux, TdlibArch.Arm64)

// Android ABIs
val androidArm64ExtractTask  = tdlibDeps.androidFor("arm64-v8a")

// Convenience: download everything
tasks.register("downloadAllTdlib") {
    group = "tdlib"
    description = "Download and extract all TDLib native artifacts"
    dependsOn(tasks.matching { it.name.startsWith("extractTdlib") })
}

// ---------------------------------------------------------------------------
// Kotlin Multiplatform configuration
// ---------------------------------------------------------------------------
kotlin {
    applyHierarchyTemplate(tdlibSourceSetHierarchyTemplate)

    android {
        namespace = "org.xephosbot.tdlib"
        compileSdk = libs.versions.compileSdk.get().toInt()
        minSdk = libs.versions.minSdk.get().toInt()

        publishLibraryVariants("release")
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

// ---------------------------------------------------------------------------
// TdlibProjectContext — central build configuration
// ---------------------------------------------------------------------------
val ctx = TdlibProjectContext(project, kotlin, tdlibDeps, tdlibVersion)

// ---------------------------------------------------------------------------
// Native targets: cinterop + static linking
// ---------------------------------------------------------------------------
data class NativeTargetDef(
    val name: String,
    val os: TdlibOS,
    val arch: TdlibArch,
    val extractTask: String,
    val isSimulator: Boolean = false,
)

val nativeTargets = listOf(
    NativeTargetDef("iosArm64",          TdlibOS.IOS,   TdlibArch.Arm64, iosArm64ExtractTask),
    NativeTargetDef("iosSimulatorArm64", TdlibOS.IOS,   TdlibArch.Arm64, iosSimArm64ExtractTask, isSimulator = true),
    NativeTargetDef("macosArm64",        TdlibOS.MacOS, TdlibArch.Arm64, macosArm64ExtractTask),
    NativeTargetDef("macosX64",          TdlibOS.MacOS, TdlibArch.X64,   macosX64ExtractTask),
    NativeTargetDef("linuxX64",          TdlibOS.Linux, TdlibArch.X64,   linuxX64ExtractTask),
    NativeTargetDef("linuxArm64",        TdlibOS.Linux, TdlibArch.Arm64, linuxArm64ExtractTask),
)

nativeTargets.forEach { def ->
    val target = kotlin.targets.getByName(def.name) as org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTarget
    ctx.configureNativeTarget(def.os, def.arch, target, def.isSimulator)
    ctx.wireNativeExtractTask(def.extractTask, target)
}

// ---------------------------------------------------------------------------
// JVM target: JNI shared library loading
// ---------------------------------------------------------------------------
ctx.configureJvmTarget(jniExtractTask)

// ---------------------------------------------------------------------------
// Android target: wire extract tasks
// ---------------------------------------------------------------------------
ctx.configureAndroidTarget(mapOf("arm64-v8a" to androidArm64ExtractTask))

// ---------------------------------------------------------------------------
// Maven publish — per-platform artifacts
// ---------------------------------------------------------------------------
publishing {
    publications.withType<MavenPublication> {
        groupId = "org.xephosbot"
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
