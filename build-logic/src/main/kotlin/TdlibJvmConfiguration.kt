import com.android.build.api.dsl.KotlinMultiplatformAndroidLibraryTarget
import com.android.build.api.variant.KotlinMultiplatformAndroidComponentsExtension
import org.gradle.api.tasks.Copy
import org.gradle.kotlin.dsl.register
import org.jetbrains.kotlin.gradle.targets.jvm.KotlinJvmTarget

private val JNI_TARGETS = listOf(
    TdlibTarget(TdlibOS.Windows, TdlibArch.X64),
    TdlibTarget(TdlibOS.Windows, TdlibArch.Arm64),
    TdlibTarget(TdlibOS.Linux, TdlibArch.X64),
    TdlibTarget(TdlibOS.Linux, TdlibArch.Arm64),
    TdlibTarget(TdlibOS.MacOS, TdlibArch.X64),
    TdlibTarget(TdlibOS.MacOS, TdlibArch.Arm64),
)
private val ANDROID_ABIS = listOf("arm64-v8a", "armeabi-v7a", "x86", "x86_64")

/**
 * JVM & Android target configuration for TDLib.
 *
 * Analogous to `JvmTasksConfiguration.kt` in JetBrains/skiko.
 *
 * JVM target:
 *   Packages every supported desktop JNI shared library into a fat JVM JAR
 *   under `natives/{os}_{arch}/`.
 *   At runtime `TdLibLoader` extracts the binary for the current host from
 *   the classpath.
 *
 * Android target:
 *   The shared `.so` files go into `jniLibs/{abi}/` automatically via
 *   the Android Gradle Plugin, so we only need to copy the extracted
 *   artifacts to the right place.
 */

/**
 * Registers staging tasks that bundle all supported desktop JNI shared
 * libraries as classpath resources for the JVM target.
 *
 * The fat JAR contains entries like `natives/{os}_{arch}/libtdjsonjava.{ext}`.
 */
fun TdlibProjectContext.configureJvmTarget(target: KotlinJvmTarget) = with(project) {
    val jniResourcesDir = layout.buildDirectory.dir("jvmJniResources")

    val stageTasks = JNI_TARGETS.map { tdTarget ->
        val artifactTask = tdlibDeps.jniFor(tdTarget.os, tdTarget.arch)
        // Libs are extracted to the persistent prebuilds directory
        val jniLibsDir = rootProject.rootDir.resolve("prebuilds/natives/${tdTarget.jniLocalDir()}")
        // JAR resource path: natives/linux_64, natives/macos_arm64, etc.
        val resourceLayout = tdTarget.jvmResourceDir()
        val suffix = tdTarget.jniLocalDir()
            .split("-")
            .joinToString("") { it.replaceFirstChar(Char::titlecase) }
            
        tasks.register<Copy>("stageJvmJni$suffix") {
            group = "tdlib"
            description = "Stage JNI library in classpath resource layout for run/test"
            dependsOn(artifactTask)
            // Copy lib/ contents directly — no extra lib/ nesting in the JAR
            from(jniLibsDir.resolve("lib")) {
                into(resourceLayout)
            }
            into(jniResourcesDir)
        }
    }

    // Add staged resources so both run/test and published JAR include the library.
    kotlin.sourceSets.getByName("jvmMain") {
        resources.srcDir(jniResourcesDir)
    }

    tasks.matching { it.name == "compileKotlinJvm" || it.name == "jvmProcessResources" }.configureEach {
        stageTasks.forEach { dependsOn(it) }
    }
}

/**
 * Configures Android target: copies `.so` files into the proper `{abi}/` directory.
 */
fun TdlibProjectContext.configureAndroidTarget(target: KotlinMultiplatformAndroidLibraryTarget) = with(project) {
    val jniLibsBaseDir = layout.buildDirectory.dir("androidJniLibs")

    // Per-ABI copy tasks: prebuilds/natives/android-{abi}/*.so → build/androidJniLibs/{abi}/*.so
    val copyTasks = ANDROID_ABIS.map { abi ->
        val suffix = abi.split("-").joinToString("") { it.replaceFirstChar(Char::titlecase) }
        val artifactTask = tdlibDeps.androidFor(abi)
        
        tasks.register<Copy>("copyAndroidJniLibs$suffix") {
            group = "tdlib"
            description = "Copy $abi .so libs for Android AAR"
            dependsOn(artifactTask)
            from(rootProject.rootDir.resolve("prebuilds/natives/android-$abi")) { include("*.so") }
            into(jniLibsBaseDir.map { it.dir(abi) })
        }.name
    }

    // Register assembled jniLibs directory with AGP via the Variant API.
    extensions.getByType(KotlinMultiplatformAndroidComponentsExtension::class.java)
        .onVariants { variant ->
            variant.sources.jniLibs?.addStaticSourceDirectory(jniLibsBaseDir.get().asFile.absolutePath)
        }

    // Wire copy tasks before AGP merges JNI libraries.
    tasks.configureEach {
        if (name.startsWith("merge") && name.endsWith("JniLibFolders")) {
            copyTasks.forEach { dependsOn(it) }
        }
    }
}
