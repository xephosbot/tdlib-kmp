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

    sourceSets {

    }
}

// ---------------------------------------------------------------------------
// Wire download tasks as dependencies of compilation
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
