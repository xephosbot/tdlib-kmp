import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTarget

plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.android.kmp.library)
    id("de.undercouch.download")
    `maven-publish`
}

val tdlibVersion = project.property("tdlib.version") as String
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
    android {
        namespace = "org.example.tdlib"
        compileSdk = libs.versions.compileSdk.get().toInt()
        minSdk = libs.versions.minSdk.get().toInt()
    }

    jvm()
    iosArm64()
    iosSimulatorArm64()
    macosX64()
    macosArm64()
    linuxX64()
    linuxArm64()

    // -----------------------------------------------------------------
    // cinterop: bind TDLib C headers & link static libraries
    // -----------------------------------------------------------------
    val nativeLibDirs = mapOf(
        "iosArm64"            to "ios-arm64",
        "iosSimulatorArm64"   to "ios-arm64-simulator",
        "macosArm64"          to "macos-arm64",
        "macosX64"            to "macos-x86_64",
        "linuxX64"            to "linux-x86_64",
        "linuxArm64"          to "linux-arm64",
    )

    targets.withType<KotlinNativeTarget> {
        val libDir = nativeLibDirs[name] ?: return@withType
        compilations.getByName("main") {
            cinterops {
                val tdjson by creating {
                    defFile(project.file("src/nativeInterop/cinterop/tdjson.def"))
                    compilerOpts("-I${project.file("libs/$libDir/include")}")
                    extraOpts("-libraryPath", project.file("libs/$libDir/lib").absolutePath)
                }
            }
        }
    }

    sourceSets {

    }
}

// ---------------------------------------------------------------------------
// Wire download tasks as dependencies of compilation & cinterop
// ---------------------------------------------------------------------------
tasks.matching { it.name == "compileKotlinJvm" }.configureEach {
    dependsOn(jniExtractTask)
}

tasks.matching { it.name == "compileKotlinIosArm64" }.configureEach {
    dependsOn(iosArm64ExtractTask)
}

tasks.matching { it.name == "compileKotlinIosSimulatorArm64" }.configureEach {
    dependsOn(iosSimArm64ExtractTask)
}

tasks.matching { it.name == "compileKotlinMacosArm64" }.configureEach {
    dependsOn(macosArm64ExtractTask)
}

tasks.matching { it.name == "compileKotlinMacosX64" }.configureEach {
    dependsOn(macosX64ExtractTask)
}

tasks.matching { it.name == "compileKotlinLinuxX64" }.configureEach {
    dependsOn(linuxX64ExtractTask)
}

tasks.matching { it.name == "compileKotlinLinuxArm64" }.configureEach {
    dependsOn(linuxArm64ExtractTask)
}

// cinterop tasks must run after native artifacts are extracted
mapOf(
    "cinteropTdjsonIosArm64"          to iosArm64ExtractTask,
    "cinteropTdjsonIosSimulatorArm64" to iosSimArm64ExtractTask,
    "cinteropTdjsonMacosArm64"        to macosArm64ExtractTask,
    "cinteropTdjsonMacosX64"          to macosX64ExtractTask,
    "cinteropTdjsonLinuxX64"          to linuxX64ExtractTask,
    "cinteropTdjsonLinuxArm64"        to linuxArm64ExtractTask,
).forEach { (cinteropTask, extractTask) ->
    tasks.matching { it.name == cinteropTask }.configureEach {
        dependsOn(extractTask)
    }
}
